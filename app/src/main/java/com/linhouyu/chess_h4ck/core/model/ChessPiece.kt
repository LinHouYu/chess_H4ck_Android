package com.linhouyu.chess_h4ck.core.model

enum class PieceColor {
    WHITE, BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(
    val symbolUpper: Char,
    val unicodeWhite: String,
    val unicodeBlack: String,
    val value: Int
) {
    PAWN('P', "♙", "♟", 100),
    KNIGHT('N', "♘", "♞", 320),
    BISHOP('B', "♗", "♝", 330),
    ROOK('R', "♖", "♜", 500),
    QUEEN('Q', "♕", "♛", 900),
    KING('K', "♔", "♚", 20000);

    companion object {
        fun fromChar(c: Char): PieceType? {
            val upper = c.uppercaseChar()
            return entries.find { it.symbolUpper == upper }
        }
    }
}

data class Piece(val type: PieceType, val color: PieceColor) {
    val symbol: Char
        get() = if (color == PieceColor.WHITE) type.symbolUpper else type.symbolUpper.lowercaseChar()

    val unicode: String
        get() = if (color == PieceColor.WHITE) type.unicodeWhite else type.unicodeBlack

    val isWhite: Boolean
        get() = color == PieceColor.WHITE

    companion object {
        fun fromChar(c: Char): Piece? {
            val type = PieceType.fromChar(c) ?: return null
            val color = if (c.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
            return Piece(type, color)
        }
    }
}
