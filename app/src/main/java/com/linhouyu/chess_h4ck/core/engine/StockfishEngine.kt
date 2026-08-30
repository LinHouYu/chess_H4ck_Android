package com.linhouyu.chess_h4ck.core.engine

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.linhouyu.chess_h4ck.core.config.SkillPreset
import com.linhouyu.chess_h4ck.core.model.Square
import java.io.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * StockfishEngine - 官方 Stockfish C++ 原生引擎跨进程通信 Wrapper
 *
 * 核心能力：
 * 1. 默认满血最高算力 (Skill Level 20, 4 Threads, 64MB Hash, Depth 22+)
 * 2. 动态读取并应用用户自定义棋力预设 (EnginePreferences)
 * 3. 严格敌我隔离：仅在接收到我方请求时计算，支持 stopAnalysis() 瞬间释放 CPU
 * 4. 优先加载 nativeLibraryDir (libstockfish.so)，完美突破 Android 10+ W^X 安全执行限制
 * 5. 全链路 UCI 通信日志与毫秒级 PV 实时推送
 */
class StockfishEngine(
    private val context: Context? = null,
    private var callback: EngineCallback? = null
) {
    companion object {
        private const val TAG = "StockfishEngine"
        private const val BINARY_NAME = "stockfish"
        private const val SO_NAME = "libstockfish.so"
    }

    private val mainHandler: Handler? by lazy {
        try {
            Handler(Looper.getMainLooper())
        } catch (t: Throwable) {
            null
        }
    }
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null

    private val isRunning = AtomicBoolean(false)

    var latestBestMove: String? = null
    var latestFromSquare: Square? = null
    var latestToSquare: Square? = null
    var latestEvalStr: String = "就绪"

    private var currentEvalScore: String = "0.00"

    init {
        executor.execute {
            initNativeProcess()
        }
    }

    fun setCallback(cb: EngineCallback?) {
        this.callback = cb
    }

    private fun isProcessAlive(p: Process?): Boolean {
        if (p == null) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            p.isAlive
        } else {
            try {
                p.exitValue()
                false
            } catch (e: IllegalThreadStateException) {
                true
            }
        }
    }

    /**
     * 二进制文件准备
     */
    private fun prepareBinary(): File? {
        if (context == null) {
            Log.w(TAG, "Context 为空，无法查找或解压 Stockfish 引擎")
            return null
        }

        // 策略 A: 系统原生库目录 (Android 10+ 官方允许 execve 路径)
        try {
            val nativeLibDir = context.applicationInfo.nativeLibraryDir
            if (!nativeLibDir.isNullOrEmpty()) {
                val soFile = File(nativeLibDir, SO_NAME)
                if (soFile.exists() && soFile.length() > 0) {
                    Log.i(TAG, "发现系统原生库路径 Stockfish: ${soFile.absolutePath}, 大小: ${soFile.length()} 字节")
                    return soFile
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "读取 nativeLibraryDir 失败: ${e.message}")
        }

        // 策略 B: 内部存储 filesDir
        val targetFile = File(context.filesDir, BINARY_NAME)
        if (targetFile.exists() && targetFile.length() > 0 && targetFile.canExecute()) {
            Log.i(TAG, "复用内部存储已存在的 Stockfish: ${targetFile.absolutePath}, 大小: ${targetFile.length()} 字节")
            return targetFile
        }

        // 策略 C: 从 assets 提取
        try {
            Log.i(TAG, "正在从 assets 释放 Stockfish 到: ${targetFile.absolutePath}")
            val inputStream = context.assets.open(BINARY_NAME)
            val tempFile = File(context.filesDir, "$BINARY_NAME.tmp")

            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(1024 * 128)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
                output.flush()
            }
            inputStream.close()

            if (targetFile.exists()) {
                targetFile.delete()
            }
            tempFile.renameTo(targetFile)

            targetFile.setExecutable(true, false)
            targetFile.setReadable(true, false)
            targetFile.setWritable(true, false)

            Log.i(TAG, "Stockfish assets 释放完毕，大小: ${targetFile.length()} 字节，执行权限: ${targetFile.canExecute()}")
            return targetFile
        } catch (e: Exception) {
            Log.e(TAG, "从 assets 提取 Stockfish 异常", e)
            return if (targetFile.exists()) targetFile else null
        }
    }

    /**
     * 启动原生子进程并配置用户选择的棋力
     */
    private fun initNativeProcess() {
        try {
            val binaryFile = prepareBinary() ?: run {
                Log.e(TAG, "无法准备 Stockfish 二进制文件，进程启动终止")
                return
            }

            Log.i(TAG, "正在启动 Stockfish 进程: ${binaryFile.absolutePath}")
            val pb = ProcessBuilder(binaryFile.absolutePath)
            pb.redirectErrorStream(true)
            val proc = pb.start()
            process = proc

            writer = BufferedWriter(OutputStreamWriter(proc.outputStream, Charsets.UTF_8))
            reader = BufferedReader(InputStreamReader(proc.inputStream, Charsets.UTF_8))
            isRunning.set(true)

            // 启动后台输出流监听线程
            startOutputReaderThread()

            // 发送 UCI 初始化指令
            sendCommandDirect("uci")

            // 应用当前保存的棋力配置 (默认最大算力)
            applyCurrentEngineOptions()

            sendCommandDirect("isready")
            Log.i(TAG, "Stockfish UCI 初始化与棋力配置完成")
        } catch (e: Exception) {
            Log.e(TAG, "启动 Stockfish 原生进程失败: ${e.message}", e)
        }
    }

    /**
     * 根据用户配置应用 UCI 棋力参数
     */
    fun applyCurrentEngineOptions() {
        val preset = SkillPreset.getSavedPreset(context)
        Log.i(TAG, "正在配置 Stockfish 棋力档位: ${preset.title}, Skill Level: ${preset.skillLevel}, Threads: ${preset.threads}, Hash: ${preset.hashMb}MB")

        sendCommandDirect("setoption name Threads value ${preset.threads}")
        sendCommandDirect("setoption name Hash value ${preset.hashMb}")
        sendCommandDirect("setoption name Skill Level value ${preset.skillLevel}")
        if (preset.limitStrength) {
            sendCommandDirect("setoption name UCI_LimitStrength value true")
            sendCommandDirect("setoption name UCI_Elo value ${preset.elo}")
        } else {
            sendCommandDirect("setoption name UCI_LimitStrength value false")
        }
    }

    /**
     * 后台守护线程监听引擎 stdout 输出流
     */
    private fun startOutputReaderThread() {
        Thread({
            val r = reader ?: return@Thread
            try {
                while (isRunning.get()) {
                    val currentLine = r.readLine() ?: break
                    val text = currentLine.trim()
                    if (text.isNotEmpty()) {
                        Log.d(TAG, "<<< 收到引擎反馈: $text")
                        parseUciOutput(text)
                    }
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    Log.e(TAG, "读取 Stockfish 管道输出异常: ${e.message}")
                }
            }
        }, "Stockfish-UCI-Reader").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * 解析引擎输出流 (包含 info 实时推演与 bestmove 终态决策)
     */
    private fun parseUciOutput(line: String) {
        if (line.isEmpty()) return

        // 解析局面评分: info depth 12 ... score cp 35 / score mate 2
        if (line.startsWith("info")) {
            val parts = line.split(" ")
            val scoreIndex = parts.indexOf("score")
            if (scoreIndex != -1 && scoreIndex + 2 < parts.size) {
                val type = parts[scoreIndex + 1]
                val value = parts[scoreIndex + 2].toIntOrNull()
                if (value != null) {
                    currentEvalScore = if (type == "cp") {
                        val scoreFormatted = value / 100.0
                        if (scoreFormatted >= 0) "+%.2f".format(scoreFormatted) else "%.2f".format(scoreFormatted)
                    } else if (type == "mate") {
                        "杀棋 #$value"
                    } else {
                        currentEvalScore
                    }
                    latestEvalStr = currentEvalScore
                }
            }

            // 实时解析中间搜索最佳线路 pv: info ... pv e2e4 e7e5 ...
            val pvIndex = parts.indexOf("pv")
            if (pvIndex != -1 && pvIndex + 1 < parts.size) {
                val interimMove = parts[pvIndex + 1]
                if (interimMove.length >= 4 && interimMove != "(none)") {
                    val fromSq = Square.fromAlgebraic(interimMove.substring(0, 2))
                    val toSq = Square.fromAlgebraic(interimMove.substring(2, 4))
                    latestBestMove = interimMove
                    latestFromSquare = fromSq
                    latestToSquare = toSq

                    // 实时派发推演成果给 UI
                    dispatchResult(interimMove, currentEvalScore, fromSq, toSq)
                }
            }
        }

        // 解析终态最佳着法: bestmove e2e4 [ponder e7e5]
        if (line.startsWith("bestmove")) {
            val parts = line.split(" ")
            if (parts.size >= 2) {
                val moveStr = parts[1]
                if (moveStr != "(none)" && moveStr.length >= 4) {
                    val fromStr = moveStr.substring(0, 2)
                    val toStr = moveStr.substring(2, 4)
                    val fromSq = Square.fromAlgebraic(fromStr)
                    val toSq = Square.fromAlgebraic(toStr)

                    latestBestMove = moveStr
                    latestFromSquare = fromSq
                    latestToSquare = toSq
                    latestEvalStr = currentEvalScore

                    dispatchResult(moveStr, currentEvalScore, fromSq, toSq)
                } else {
                    latestBestMove = null
                    latestFromSquare = null
                    latestToSquare = null
                    latestEvalStr = "无可用走法"
                    dispatchResult(null, latestEvalStr, null, null)
                }
            }
        }
    }

    private fun dispatchResult(
        bestMove: String?,
        evalStr: String,
        fromSquare: Square?,
        toSquare: Square?
    ) {
        val h = mainHandler
        if (h != null) {
            h.post {
                callback?.onAnalysisResult(bestMove, evalStr, fromSquare, toSquare)
            }
        } else {
            callback?.onAnalysisResult(bestMove, evalStr, fromSquare, toSquare)
        }
    }

    /**
     * 外部请求分析局面 (根据预设深度与时间进行深度搜索)
     */
    fun requestAnalysis(
        fen: String,
        isBottomTurn: Boolean = true,
        depth: Int? = null,
        timeLimitMs: Long? = null
    ) {
        val preset = SkillPreset.getSavedPreset(context)
        val actualDepth = depth ?: preset.depth
        val actualTimeLimit = timeLimitMs ?: preset.movetime

        executor.execute {
            if (process == null || !isRunning.get() || !isProcessAlive(process)) {
                Log.w(TAG, "检测到 Stockfish 未就绪，正在自动重启进程...")
                initNativeProcess()
            }

            try {
                sendCommandDirect("stop")
                sendCommandDirect("position fen $fen")
                sendCommandDirect("go depth $actualDepth movetime $actualTimeLimit")
            } catch (e: Exception) {
                Log.e(TAG, "发送分析指令异常: ${e.message}", e)
            }
        }
    }

    /**
     * 立即停止当前计算 (用于对手回合，完全不为敌人计算)
     */
    fun stopAnalysis() {
        executor.execute {
            if (process != null && isRunning.get() && isProcessAlive(process)) {
                sendCommandDirect("stop")
            }
        }
    }

    /**
     * 向引擎发送标准指令并记录发送日志
     */
    private fun sendCommandDirect(cmd: String) {
        val w = writer ?: return
        try {
            Log.d(TAG, ">>> 向引擎发送: $cmd")
            w.write(cmd)
            w.newLine()
            w.flush()
        } catch (e: Exception) {
            Log.e(TAG, "向 Stockfish 写入指令失败: $cmd, error: ${e.message}")
        }
    }

    /**
     * 停止引擎计算与安全销毁子进程
     */
    fun stop() {
        isRunning.set(false)
        executor.execute {
            try {
                sendCommandDirect("quit")
                writer?.close()
                reader?.close()
            } catch (e: Exception) {}
            try {
                process?.destroy()
            } catch (e: Exception) {}
            process = null
            writer = null
            reader = null
        }
        executor.shutdown()
    }
}
