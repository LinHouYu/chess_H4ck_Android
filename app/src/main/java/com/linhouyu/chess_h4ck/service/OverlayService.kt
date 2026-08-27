package com.linhouyu.chess_h4ck.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.linhouyu.chess_h4ck.R
import com.linhouyu.chess_h4ck.core.engine.EngineCallback
import com.linhouyu.chess_h4ck.core.engine.StockfishEngine
import com.linhouyu.chess_h4ck.core.model.PieceColor
import com.linhouyu.chess_h4ck.core.model.Square
import com.linhouyu.chess_h4ck.core.state.BoardStateManager
import com.linhouyu.chess_h4ck.ui.overlay.ChessboardOverlay
import com.linhouyu.chess_h4ck.ui.overlay.FloatingMenuOverlay
import com.linhouyu.chess_h4ck.ui.overlay.StatusBarOverlay

class OverlayService : Service(), EngineCallback, FloatingMenuOverlay.MenuCallbacks {

    private lateinit var statusBarOverlay: StatusBarOverlay
    private lateinit var chessboardOverlay: ChessboardOverlay
    private lateinit var floatingMenuOverlay: FloatingMenuOverlay

    private val boardStateManager = BoardStateManager()
    private val engine = StockfishEngine()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isAutoPlay: Boolean = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        isRunning = true

        try {
            startForegroundServiceNotification()
        } catch (e: Throwable) {
            Log.e("OverlayService", "startForeground error", e)
        }

        try {
            engine.setCallback(this)

            statusBarOverlay = StatusBarOverlay(this)
            statusBarOverlay.show()

            chessboardOverlay = ChessboardOverlay(this, boardStateManager) { from, to ->
                onUserMovedPiece(from, to)
            }
            chessboardOverlay.show()

            floatingMenuOverlay = FloatingMenuOverlay(this, this)
            floatingMenuOverlay.show()

            updateStatusAndAnalyze("悬浮辅助已就绪")
        } catch (e: Throwable) {
            Log.e("OverlayService", "Overlay show error: ${e.message}", e)
            Toast.makeText(this, "创建悬浮窗失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val channelId = "chess_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chess Assistant Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("国际象棋悬浮辅助运行中")
            .setContentText("Stockfish 18 引擎已就绪")
            .setSmallIcon(R.drawable.app_logo)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1001, notification)
            }
        } catch (e: Throwable) {
            Log.e("OverlayService", "startForeground exception: ${e.message}", e)
            try {
                startForeground(1001, notification)
            } catch (t: Throwable) {
                Log.e("OverlayService", "fallback startForeground exception: ${t.message}", t)
            }
        }
    }

    private fun onUserMovedPiece(from: Square, to: Square) {
        boardStateManager.movePiece(from, to, autoSwitchTurn = true)
        chessboardOverlay.chessboardView.setAiRecommendation(null, null)
        chessboardOverlay.redraw()

        val action = "玩家移动: ${from.toAlgebraic()} -> ${to.toAlgebraic()}"
        updateStatusAndAnalyze(action)
    }

    private fun updateStatusAndAnalyze(actionDesc: String) {
        val turnName = boardStateManager.getTurnName()
        val isBottom = boardStateManager.isBottomTurn()
        statusBarOverlay.updateTurn(turnName, isBottom)
        statusBarOverlay.updateMode(boardStateManager.sandboxMode)
        statusBarOverlay.flashAction(actionDesc)

        // Trigger Stockfish Engine Analysis
        val fen = boardStateManager.getFen()
        engine.requestAnalysis(fen, isBottom)
    }

    override fun onAnalysisResult(
        bestMoveUci: String?,
        evalStr: String,
        fromSquare: Square?,
        toSquare: Square?
    ) {
        mainHandler.post {
            val isBottom = boardStateManager.isBottomTurn()
            statusBarOverlay.updateAiAnalysis(bestMoveUci, evalStr, isBottom)
            chessboardOverlay.chessboardView.setAiRecommendation(fromSquare, toSquare)

            if (isAutoPlay && isBottom && bestMoveUci != null && fromSquare != null && toSquare != null) {
                mainHandler.postDelayed({
                    if (isAutoPlay && boardStateManager.isBottomTurn()) {
                        performAiDirectMove(fromSquare, toSquare)
                    }
                }, 600)
            }
        }
    }

    private fun performAiDirectMove(from: Square, to: Square) {
        boardStateManager.movePiece(from, to, autoSwitchTurn = true)
        chessboardOverlay.chessboardView.setAiRecommendation(null, null)
        chessboardOverlay.redraw()

        val action = "AI走子: ${from.toAlgebraic()} -> ${to.toAlgebraic()}"
        updateStatusAndAnalyze(action)
    }

    // Floating Menu Callbacks
    override fun onReset() {
        boardStateManager.reset()
        val isFlipped = chessboardOverlay.chessboardView.isFlipped
        boardStateManager.bottomColor = if (isFlipped) PieceColor.BLACK else PieceColor.WHITE
        chessboardOverlay.chessboardView.setAiRecommendation(null, null)
        chessboardOverlay.redraw()
        updateStatusAndAnalyze("重置开局状态")
    }

    override fun onToggleSandbox() {
        val enabled = boardStateManager.toggleSandbox()
        statusBarOverlay.updateMode(enabled)
        val status = if (enabled) "沙盒模式: 已开启(自由摆子/吃子)" else "沙盒模式: 已关闭"
        statusBarOverlay.flashAction(status)
    }

    override fun onToggleTurn() {
        boardStateManager.toggleTurn()
        val turnName = boardStateManager.getTurnName()
        updateStatusAndAnalyze("已切换执棋方: $turnName")
    }

    override fun onFlipBoard() {
        val flipped = chessboardOverlay.toggleFlip()
        boardStateManager.bottomColor = if (flipped) PieceColor.BLACK else PieceColor.WHITE
        chessboardOverlay.chessboardView.setAiRecommendation(null, null)
        chessboardOverlay.redraw()
        val status = if (flipped) "视角翻转: AI执黑棋(底部)" else "视角还原: AI执白棋(底部)"
        statusBarOverlay.flashAction(status)
        updateStatusAndAnalyze(status)
    }

    override fun onUndo() {
        if (boardStateManager.undo()) {
            chessboardOverlay.chessboardView.setAiRecommendation(null, null)
            chessboardOverlay.redraw()
            updateStatusAndAnalyze("已撤销上一步")
        } else {
            statusBarOverlay.flashAction("无更多历史可撤销")
        }
    }

    override fun onAiMove() {
        val from = engine.latestFromSquare
        val to = engine.latestToSquare
        if (from != null && to != null) {
            performAiDirectMove(from, to)
        } else {
            statusBarOverlay.flashAction("AI计算中或未就绪")
        }
    }

    override fun onToggleAutoPlay() {
        isAutoPlay = !isAutoPlay
        val status = if (isAutoPlay) "自动代走: 已开启" else "自动代走: 已停止"
        statusBarOverlay.flashAction(status)

        if (isAutoPlay && boardStateManager.isBottomTurn()) {
            val from = engine.latestFromSquare
            val to = engine.latestToSquare
            if (from != null && to != null) {
                performAiDirectMove(from, to)
            }
        }
    }

    override fun onToggleBoardVisibility() {
        val isVisible = chessboardOverlay.toggleVisibility()
        floatingMenuOverlay.updateBoardVisibilityButton(isVisible)
        val status = if (isVisible) "棋盘: 已显示" else "棋盘: 已隐藏"
        statusBarOverlay.flashAction(status)
    }

    override fun onToggleTouchThrough() {
        val enabled = chessboardOverlay.toggleTouchThrough()
        val status = if (enabled) "触摸穿透: 已开启(直接操作底层)" else "触摸穿透: 已关闭(棋盘可挪子)"
        statusBarOverlay.flashAction(status)
    }

    override fun onClose() {
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isRunning = false

        try {
            statusBarOverlay.hide()
            chessboardOverlay.hide()
            floatingMenuOverlay.hide()
        } catch (e: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        var instance: OverlayService? = null
            private set
        var isRunning: Boolean = false
            private set
    }
}
