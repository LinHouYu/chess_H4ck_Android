package com.linhouyu.chess_h4ck.core.engine

import com.linhouyu.chess_h4ck.core.model.PieceColor
import com.linhouyu.chess_h4ck.core.model.PieceType
import com.linhouyu.chess_h4ck.core.model.Square
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class StockfishEngine(private var callback: EngineCallback? = null) {

    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var currentJob: Job? = null

    var latestBestMove: String? = null
    var latestFromSquare: Square? = null
    var latestToSquare: Square? = null
    var latestEvalStr: String = "就绪"

    fun setCallback(cb: EngineCallback?) {
        this.callback = cb
    }

    fun requestAnalysis(fen: String, isBottomTurn: Boolean, depth: Int = 4, timeLimitMs: Long = 400L) {
        currentJob?.cancel()
        currentJob = engineScope.launch {
            try {
                val (bestMove, evalStr) = analyzeFen(fen, isBottomTurn, depth, timeLimitMs)
                latestBestMove = bestMove
                latestEvalStr = evalStr

                if (bestMove != null && bestMove.length >= 4) {
                    latestFromSquare = Square.fromAlgebraic(bestMove.substring(0, 2))
                    latestToSquare = Square.fromAlgebraic(bestMove.substring(2, 4))
                } else {
                    latestFromSquare = null
                    latestToSquare = null
                }

                notifyCallback {
                    callback?.onAnalysisResult(
                        if (isBottomTurn) bestMove else null,
                        evalStr,
                        if (isBottomTurn) latestFromSquare else null,
                        if (isBottomTurn) latestToSquare else null
                    )
                }
            } catch (e: CancellationException) {
                // Ignore normal job cancel
            } catch (e: Exception) {
                notifyCallback {
                    callback?.onAnalysisResult(null, "自定义局面", null, null)
                }
            }
        }
    }

    private suspend fun notifyCallback(action: () -> Unit) {
        try {
            withContext(Dispatchers.Main) {
                action()
            }
        } catch (t: Throwable) {
            action()
        }
    }

    fun analyzeFenSync(
        fen: String,
        isBottomTurn: Boolean = true,
        depth: Int = 4,
        timeLimitMs: Long = 400L
    ): Pair<String?, String> {
        return analyzeFen(fen, isBottomTurn, depth, timeLimitMs)
    }

    fun stop() {
        currentJob?.cancel()
        engineScope.cancel()
    }

    fun analyzeFen(
        fen: String,
        isBottomTurn: Boolean,
        maxDepth: Int,
        timeLimitMs: Long
    ): Pair<String?, String> {
        val board = FastBoard()
        if (!board.loadFen(fen)) {
            return Pair(null, "自定义局面")
        }

        val startTime = System.currentTimeMillis()
        val isWhiteTurn = board.turn == FastBoard.WHITE

        // Minimax Alpha-Beta search with PST (Piece-Square Tables)
        val bestMove = searchBestMove(board, maxDepth, startTime, timeLimitMs)
        val evalScore = evaluatePosition(board)

        val evalFormatted = if (isWhiteTurn) {
            String.format("%+.2f", evalScore / 100.0)
        } else {
            String.format("%+.2f", -evalScore / 100.0)
        }

        return Pair(bestMove, evalFormatted)
    }

    private fun searchBestMove(
        board: FastBoard,
        depth: Int,
        startTime: Long,
        timeLimitMs: Long
    ): String? {
        val moves = board.generateLegalMoves()
        if (moves.isEmpty()) return null

        var bestMove: Long? = null
        val isMaximizing = board.turn == FastBoard.WHITE
        var bestScore = if (isMaximizing) -100000 else 100000
        var alpha = -100000
        var beta = 100000

        // Move sorting: captures first
        moves.sortByDescending { move ->
            val captured = FastBoard.getCaptured(move)
            if (captured != 0) 1000 + abs(captured) else 0
        }

        for (m in moves) {
            if (System.currentTimeMillis() - startTime > timeLimitMs) break
            board.makeMove(m)
            val score = alphaBeta(board, depth - 1, alpha, beta, !isMaximizing, startTime, timeLimitMs)
            board.undoMove(m)

            if (isMaximizing) {
                if (score > bestScore) {
                    bestScore = score
                    bestMove = m
                }
                alpha = max(alpha, score)
            } else {
                if (score < bestScore) {
                    bestScore = score
                    bestMove = m
                }
                beta = min(beta, score)
            }
            if (beta <= alpha) break
        }

        return if (bestMove != null) FastBoard.moveToString(bestMove) else FastBoard.moveToString(moves[0])
    }

    private fun alphaBeta(
        board: FastBoard,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        startTime: Long,
        timeLimitMs: Long
    ): Int {
        if (depth <= 0 || System.currentTimeMillis() - startTime > timeLimitMs) {
            return evaluatePosition(board)
        }

        val moves = board.generateLegalMoves()
        if (moves.isEmpty()) {
            return if (board.isInCheck(board.turn)) (if (isMaximizing) -50000 + depth else 50000 - depth) else 0
        }

        var curAlpha = alpha
        var curBeta = beta

        if (isMaximizing) {
            var maxEval = -100000
            for (m in moves) {
                board.makeMove(m)
                val eval = alphaBeta(board, depth - 1, curAlpha, curBeta, false, startTime, timeLimitMs)
                board.undoMove(m)
                maxEval = max(maxEval, eval)
                curAlpha = max(curAlpha, eval)
                if (curBeta <= curAlpha) break
            }
            return maxEval
        } else {
            var minEval = 100000
            for (m in moves) {
                board.makeMove(m)
                val eval = alphaBeta(board, depth - 1, curAlpha, curBeta, true, startTime, timeLimitMs)
                board.undoMove(m)
                minEval = min(minEval, eval)
                curBeta = min(curBeta, eval)
                if (curBeta <= curAlpha) break
            }
            return minEval
        }
    }

    private fun evaluatePosition(board: FastBoard): Int {
        var score = 0
        val pieceValues = intArrayOf(0, 100, 320, 330, 500, 900, 20000)

        // Piece square tables
        for (sq in 0 until 64) {
            val p = board.squares[sq]
            if (p == 0) continue
            val pieceType = abs(p)
            val isWhite = p > 0
            val valBase = pieceValues[pieceType]
            val pstBonus = getPstBonus(pieceType, sq, isWhite)
            val totalPieceVal = valBase + pstBonus

            if (isWhite) score += totalPieceVal else score -= totalPieceVal
        }
        return score
    }

    private fun getPstBonus(type: Int, sq: Int, isWhite: Boolean): Int {
        val r = if (isWhite) sq / 8 else 7 - (sq / 8)
        val f = sq % 8
        val centerDist = 3.5 - max(abs(f - 3.5), abs(r - 3.5))
        return when (type) {
            1 -> (r * 10 + (centerDist * 5).toInt()) // Pawn
            2 -> ((centerDist * 15).toInt())          // Knight
            3 -> ((centerDist * 10).toInt())          // Bishop
            4 -> (if (r == 6) 20 else 0)             // Rook
            5 -> ((centerDist * 5).toInt())           // Queen
            6 -> (-((centerDist * 10).toInt()))       // King
            else -> 0
        }
    }

    // High performance lightweight chess board representation
    private class FastBoard {
        companion object {
            const val WHITE = 1
            const val BLACK = -1

            const val P = 1; const val N = 2; const val B = 3; const val R = 4; const val Q = 5; const val K = 6

            fun encodeMove(from: Int, to: Int, captured: Int = 0, promo: Int = 0): Long {
                return (from.toLong() and 0x3F) or
                        ((to.toLong() and 0x3F) shl 6) or
                        (((captured + 8).toLong() and 0xF) shl 12) or
                        ((promo.toLong() and 0x7) shl 16)
            }

            fun getFrom(m: Long): Int = (m and 0x3F).toInt()
            fun getTo(m: Long): Int = ((m shr 6) and 0x3F).toInt()
            fun getCaptured(m: Long): Int = (((m shr 12) and 0xF).toInt() - 8)
            fun getPromo(m: Long): Int = ((m shr 16) and 0x7).toInt()

            fun moveToString(m: Long): String {
                val from = getFrom(m)
                val to = getTo(m)
                val promo = getPromo(m)
                val f1 = 'a' + (from % 8)
                val r1 = '1' + (from / 8)
                val f2 = 'a' + (to % 8)
                val r2 = '1' + (to / 8)
                val pStr = when (promo) {
                    Q -> "q"; R -> "r"; B -> "b"; N -> "n"; else -> ""
                }
                return "$f1$r1$f2$r2$pStr"
            }
        }

        val squares = IntArray(64)
        var turn = WHITE

        fun loadFen(fen: String): Boolean {
            squares.fill(0)
            val parts = fen.trim().split(" ")
            if (parts.isEmpty()) return false
            val rows = parts[0].split("/")
            if (rows.size != 8) return false

            for (r in 7 downTo 0) {
                val rowStr = rows[7 - r]
                var f = 0
                for (c in rowStr) {
                    if (c.isDigit()) {
                        f += c.digitToInt()
                    } else {
                        val pieceVal = when (c) {
                            'P' -> P; 'N' -> N; 'B' -> B; 'R' -> R; 'Q' -> Q; 'K' -> K
                            'p' -> -P; 'n' -> -N; 'b' -> -B; 'r' -> -R; 'q' -> -Q; 'k' -> -K
                            else -> 0
                        }
                        if (f in 0..7) {
                            squares[r * 8 + f] = pieceVal
                        }
                        f++
                    }
                }
            }
            turn = if (parts.size > 1 && parts[1] == "b") BLACK else WHITE
            return true
        }

        fun makeMove(m: Long) {
            val from = getFrom(m)
            val to = getTo(m)
            val promo = getPromo(m)
            var p = squares[from]
            if (promo != 0) {
                p = if (p > 0) promo else -promo
            }
            squares[from] = 0
            squares[to] = p
            turn = -turn
        }

        fun undoMove(m: Long) {
            val from = getFrom(m)
            val to = getTo(m)
            val captured = getCaptured(m)
            val promo = getPromo(m)
            var p = squares[to]
            if (promo != 0) {
                p = if (p > 0) P else -P
            }
            squares[from] = p
            squares[to] = captured
            turn = -turn
        }

        fun isInCheck(color: Int): Boolean {
            var kingSq = -1
            val targetK = if (color == WHITE) K else -K
            for (i in 0 until 64) {
                if (squares[i] == targetK) {
                    kingSq = i
                    break
                }
            }
            if (kingSq == -1) return false
            return isSquareAttacked(kingSq, -color)
        }

        fun isSquareAttacked(sq: Int, byColor: Int): Boolean {
            val r = sq / 8
            val f = sq % 8

            // Pawn attacks
            val pawnDir = if (byColor == WHITE) -1 else 1
            val pR = r + pawnDir
            if (pR in 0..7) {
                val targetP = if (byColor == WHITE) P else -P
                if (f > 0 && squares[pR * 8 + (f - 1)] == targetP) return true
                if (f < 7 && squares[pR * 8 + (f + 1)] == targetP) return true
            }

            // Knight attacks
            val kOffsets = arrayOf(
                intArrayOf(-2, -1), intArrayOf(-2, 1), intArrayOf(-1, -2), intArrayOf(-1, 2),
                intArrayOf(1, -2), intArrayOf(1, 2), intArrayOf(2, -1), intArrayOf(2, 1)
            )
            val targetN = if (byColor == WHITE) N else -N
            for (off in kOffsets) {
                val nr = r + off[0]
                val nf = f + off[1]
                if (nr in 0..7 && nf in 0..7 && squares[nr * 8 + nf] == targetN) return true
            }

            // Sliding pieces (Rook / Bishop / Queen)
            val straightDirs = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))
            val diagDirs = arrayOf(intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1))

            val targetR = if (byColor == WHITE) R else -R
            val targetB = if (byColor == WHITE) B else -B
            val targetQ = if (byColor == WHITE) Q else -Q

            for (d in straightDirs) {
                var cr = r + d[0]
                var cf = f + d[1]
                while (cr in 0..7 && cf in 0..7) {
                    val p = squares[cr * 8 + cf]
                    if (p != 0) {
                        if (p == targetR || p == targetQ) return true
                        break
                    }
                    cr += d[0]
                    cf += d[1]
                }
            }

            for (d in diagDirs) {
                var cr = r + d[0]
                var cf = f + d[1]
                while (cr in 0..7 && cf in 0..7) {
                    val p = squares[cr * 8 + cf]
                    if (p != 0) {
                        if (p == targetB || p == targetQ) return true
                        break
                    }
                    cr += d[0]
                    cf += d[1]
                }
            }

            // King adjacent attacks
            val targetK = if (byColor == WHITE) K else -K
            for (dr in -1..1) {
                for (df in -1..1) {
                    if (dr == 0 && df == 0) continue
                    val kr = r + dr
                    val kf = f + df
                    if (kr in 0..7 && kf in 0..7 && squares[kr * 8 + kf] == targetK) return true
                }
            }

            return false
        }

        fun generateLegalMoves(): MutableList<Long> {
            val moves = mutableListOf<Long>()
            for (sq in 0 until 64) {
                val p = squares[sq]
                if (p == 0 || (p > 0 && turn != WHITE) || (p < 0 && turn != BLACK)) continue
                val type = abs(p)
                when (type) {
                    P -> generatePawnMoves(sq, moves)
                    N -> generateKnightMoves(sq, moves)
                    B -> generateSlidingMoves(sq, arrayOf(intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1)), moves)
                    R -> generateSlidingMoves(sq, arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1)), moves)
                    Q -> generateSlidingMoves(sq, arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1), intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1)), moves)
                    K -> generateKingMoves(sq, moves)
                }
            }
            return moves
        }

        private fun generatePawnMoves(sq: Int, out: MutableList<Long>) {
            val r = sq / 8
            val f = sq % 8
            val dir = if (turn == WHITE) 1 else -1
            val startRank = if (turn == WHITE) 1 else 6
            val promoRank = if (turn == WHITE) 7 else 0

            val forwardR = r + dir
            if (forwardR in 0..7) {
                val fSq = forwardR * 8 + f
                if (squares[fSq] == 0) {
                    if (forwardR == promoRank) {
                        out.add(encodeMove(sq, fSq, 0, Q))
                        out.add(encodeMove(sq, fSq, 0, N))
                    } else {
                        out.add(encodeMove(sq, fSq, 0))
                        // Double push
                        if (r == startRank) {
                            val f2Sq = (r + dir * 2) * 8 + f
                            if (squares[f2Sq] == 0) out.add(encodeMove(sq, f2Sq, 0))
                        }
                    }
                }
            }

            // Captures
            for (df in arrayOf(-1, 1)) {
                val cf = f + df
                if (cf in 0..7 && forwardR in 0..7) {
                    val capSq = forwardR * 8 + cf
                    val capPiece = squares[capSq]
                    if (capPiece != 0 && (capPiece > 0) != (turn == WHITE)) {
                        if (forwardR == promoRank) {
                            out.add(encodeMove(sq, capSq, capPiece, Q))
                            out.add(encodeMove(sq, capSq, capPiece, N))
                        } else {
                            out.add(encodeMove(sq, capSq, capPiece))
                        }
                    }
                }
            }
        }

        private fun generateKnightMoves(sq: Int, out: MutableList<Long>) {
            val r = sq / 8
            val f = sq % 8
            val kOffsets = arrayOf(
                intArrayOf(-2, -1), intArrayOf(-2, 1), intArrayOf(-1, -2), intArrayOf(-1, 2),
                intArrayOf(1, -2), intArrayOf(1, 2), intArrayOf(2, -1), intArrayOf(2, 1)
            )
            for (off in kOffsets) {
                val nr = r + off[0]
                val nf = f + off[1]
                if (nr in 0..7 && nf in 0..7) {
                    val targetSq = nr * 8 + nf
                    val cap = squares[targetSq]
                    if (cap == 0 || (cap > 0) != (turn == WHITE)) {
                        out.add(encodeMove(sq, targetSq, cap))
                    }
                }
            }
        }

        private fun generateSlidingMoves(sq: Int, dirs: Array<IntArray>, out: MutableList<Long>) {
            val r = sq / 8
            val f = sq % 8
            for (d in dirs) {
                var cr = r + d[0]
                var cf = f + d[1]
                while (cr in 0..7 && cf in 0..7) {
                    val targetSq = cr * 8 + cf
                    val cap = squares[targetSq]
                    if (cap == 0) {
                        out.add(encodeMove(sq, targetSq, 0))
                    } else {
                        if ((cap > 0) != (turn == WHITE)) {
                            out.add(encodeMove(sq, targetSq, cap))
                        }
                        break
                    }
                    cr += d[0]
                    cf += d[1]
                }
            }
        }

        private fun generateKingMoves(sq: Int, out: MutableList<Long>) {
            val r = sq / 8
            val f = sq % 8
            for (dr in -1..1) {
                for (df in -1..1) {
                    if (dr == 0 && df == 0) continue
                    val nr = r + dr
                    val nf = f + df
                    if (nr in 0..7 && nf in 0..7) {
                        val targetSq = nr * 8 + nf
                        val cap = squares[targetSq]
                        if (cap == 0 || (cap > 0) != (turn == WHITE)) {
                            out.add(encodeMove(sq, targetSq, cap))
                        }
                    }
                }
            }
        }
    }
}
