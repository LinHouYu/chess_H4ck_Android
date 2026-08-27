package com.linhouyu.chess_h4ck.core.state

import com.linhouyu.chess_h4ck.core.model.*

class BoardStateManager(
    var bottomColor: PieceColor = PieceColor.WHITE
) {
    var sandboxMode: Boolean = false
    var turn: PieceColor = PieceColor.WHITE
    val history = mutableListOf<String>()
    val redoStack = mutableListOf<String>()

    // 8x8 grid indexed by Square
    val grid = mutableMapOf<Square, Piece?>()

    init {
        reset()
    }

    fun reset() {
        saveState()
        grid.clear()
        // White pieces
        grid[Square(0, 0)] = Piece(PieceType.ROOK, PieceColor.WHITE)
        grid[Square(1, 0)] = Piece(PieceType.KNIGHT, PieceColor.WHITE)
        grid[Square(2, 0)] = Piece(PieceType.BISHOP, PieceColor.WHITE)
        grid[Square(3, 0)] = Piece(PieceType.QUEEN, PieceColor.WHITE)
        grid[Square(4, 0)] = Piece(PieceType.KING, PieceColor.WHITE)
        grid[Square(5, 0)] = Piece(PieceType.BISHOP, PieceColor.WHITE)
        grid[Square(6, 0)] = Piece(PieceType.KNIGHT, PieceColor.WHITE)
        grid[Square(7, 0)] = Piece(PieceType.ROOK, PieceColor.WHITE)
        for (f in 0..7) {
            grid[Square(f, 1)] = Piece(PieceType.PAWN, PieceColor.WHITE)
        }

        // Empty squares
        for (r in 2..5) {
            for (f in 0..7) {
                grid[Square(f, r)] = null
            }
        }

        // Black pieces
        for (f in 0..7) {
            grid[Square(f, 6)] = Piece(PieceType.PAWN, PieceColor.BLACK)
        }
        grid[Square(0, 7)] = Piece(PieceType.ROOK, PieceColor.BLACK)
        grid[Square(1, 7)] = Piece(PieceType.KNIGHT, PieceColor.BLACK)
        grid[Square(2, 7)] = Piece(PieceType.BISHOP, PieceColor.BLACK)
        grid[Square(3, 7)] = Piece(PieceType.QUEEN, PieceColor.BLACK)
        grid[Square(4, 7)] = Piece(PieceType.KING, PieceColor.BLACK)
        grid[Square(5, 7)] = Piece(PieceType.BISHOP, PieceColor.BLACK)
        grid[Square(6, 7)] = Piece(PieceType.KNIGHT, PieceColor.BLACK)
        grid[Square(7, 7)] = Piece(PieceType.ROOK, PieceColor.BLACK)

        turn = PieceColor.WHITE
    }

    fun saveState() {
        history.add(getFen())
        redoStack.clear()
    }

    fun getPiece(square: Square): Piece? = grid[square]
    fun getPiece(file: Int, rank: Int): Piece? = if (file in 0..7 && rank in 0..7) grid[Square(file, rank)] else null

    fun setPiece(square: Square, piece: Piece?) {
        saveState()
        grid[square] = piece
    }

    fun movePiece(from: Square, to: Square, autoSwitchTurn: Boolean = true): Boolean {
        if (from == to) return false
        val piece = grid[from] ?: return false

        saveState()

        // Handle pawn promotion if moving to last rank
        var destPiece = piece
        if (piece.type == PieceType.PAWN) {
            if (piece.color == PieceColor.WHITE && to.rank == 7) {
                destPiece = Piece(PieceType.QUEEN, PieceColor.WHITE)
            } else if (piece.color == PieceColor.BLACK && to.rank == 0) {
                destPiece = Piece(PieceType.QUEEN, PieceColor.BLACK)
            }
        }

        // Handle visual castling movement if king moves 2 squares horizontally
        if (piece.type == PieceType.KING) {
            if (piece.color == PieceColor.WHITE && from == Square(4, 0)) {
                if (to == Square(6, 0) && grid[Square(7, 0)]?.type == PieceType.ROOK) {
                    grid[Square(5, 0)] = grid[Square(7, 0)]
                    grid[Square(7, 0)] = null
                } else if (to == Square(2, 0) && grid[Square(0, 0)]?.type == PieceType.ROOK) {
                    grid[Square(3, 0)] = grid[Square(0, 0)]
                    grid[Square(0, 0)] = null
                }
            } else if (piece.color == PieceColor.BLACK && from == Square(4, 7)) {
                if (to == Square(6, 7) && grid[Square(7, 7)]?.type == PieceType.ROOK) {
                    grid[Square(5, 7)] = grid[Square(7, 7)]
                    grid[Square(7, 7)] = null
                } else if (to == Square(2, 7) && grid[Square(0, 7)]?.type == PieceType.ROOK) {
                    grid[Square(3, 7)] = grid[Square(0, 7)]
                    grid[Square(0, 7)] = null
                }
            }
        }

        // Move piece
        grid[from] = null
        grid[to] = destPiece

        if (autoSwitchTurn) {
            turn = turn.opposite()
        }

        return true
    }

    fun toggleTurn(): PieceColor {
        saveState()
        turn = turn.opposite()
        return turn
    }

    fun toggleSandbox(): Boolean {
        sandboxMode = !sandboxMode
        return sandboxMode
    }

    fun toggleBottomColor(): PieceColor {
        bottomColor = bottomColor.opposite()
        return bottomColor
    }

    fun undo(): Boolean {
        if (history.isEmpty()) return false
        val currentFen = getFen()
        redoStack.add(currentFen)
        val lastFen = history.removeAt(history.size - 1)
        return loadFen(lastFen)
    }

    fun isBottomTurn(): Boolean = turn == bottomColor

    fun getTurnName(): String {
        val colorName = if (turn == PieceColor.WHITE) "白棋" else "黑棋"
        val playerSide = if (isBottomTurn()) "我方/底部" else "对手/顶部"
        return "$colorName ($playerSide)"
    }

    fun getFen(): String {
        val rows = mutableListOf<String>()
        for (r in 7 downTo 0) {
            var emptyCount = 0
            val sb = StringBuilder()
            for (f in 0..7) {
                val piece = grid[Square(f, r)]
                if (piece != null) {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    sb.append(piece.symbol)
                } else {
                    emptyCount++
                }
            }
            if (emptyCount > 0) {
                sb.append(emptyCount)
            }
            rows.add(sb.toString())
        }

        val turnStr = if (turn == PieceColor.WHITE) "w" else "b"

        var castling = ""
        val wk = grid[Square(4, 0)] == Piece(PieceType.KING, PieceColor.WHITE)
        if (wk && grid[Square(7, 0)] == Piece(PieceType.ROOK, PieceColor.WHITE)) castling += "K"
        if (wk && grid[Square(0, 0)] == Piece(PieceType.ROOK, PieceColor.WHITE)) castling += "Q"
        val bk = grid[Square(4, 7)] == Piece(PieceType.KING, PieceColor.BLACK)
        if (bk && grid[Square(7, 7)] == Piece(PieceType.ROOK, PieceColor.BLACK)) castling += "k"
        if (bk && grid[Square(0, 7)] == Piece(PieceType.ROOK, PieceColor.BLACK)) castling += "q"
        if (castling.isEmpty()) castling = "-"

        return "${rows.joinToString("/")} $turnStr $castling - 0 1"
    }

    fun loadFen(fen: String): Boolean {
        try {
            val parts = fen.trim().split(" ")
            if (parts.isEmpty()) return false
            val boardPart = parts[0]
            val rows = boardPart.split("/")
            if (rows.size != 8) return false

            grid.clear()
            for (r in 7 downTo 0) {
                val rowIdx = 7 - r
                val rowStr = rows[rowIdx]
                var f = 0
                for (c in rowStr) {
                    if (c.isDigit()) {
                        val count = c.digitToInt()
                        for (i in 0 until count) {
                            if (f in 0..7) grid[Square(f, r)] = null
                            f++
                        }
                    } else {
                        val piece = Piece.fromChar(c)
                        if (f in 0..7) grid[Square(f, r)] = piece
                        f++
                    }
                }
            }

            if (parts.size > 1) {
                turn = if (parts[1] == "b") PieceColor.BLACK else PieceColor.WHITE
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
