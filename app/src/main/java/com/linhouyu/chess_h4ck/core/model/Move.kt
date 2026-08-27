package com.linhouyu.chess_h4ck.core.model

data class Move(
    val from: Square,
    val to: Square,
    val promotion: PieceType? = null
) {
    fun toUci(): String {
        val promo = promotion?.symbolUpper?.lowercaseChar() ?: ""
        return "${from.toAlgebraic()}${to.toAlgebraic()}$promo"
    }

    override fun toString(): String = toUci()

    companion object {
        fun fromUci(uci: String): Move? {
            if (uci.length < 4) return null
            val from = Square.fromAlgebraic(uci.substring(0, 2)) ?: return null
            val to = Square.fromAlgebraic(uci.substring(2, 4)) ?: return null
            val promo = if (uci.length >= 5) PieceType.fromChar(uci[4]) else null
            return Move(from, to, promo)
        }
    }
}
