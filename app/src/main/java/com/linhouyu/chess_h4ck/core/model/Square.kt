package com.linhouyu.chess_h4ck.core.model

data class Square(val file: Int, val rank: Int) {
    init {
        require(file in 0..7 && rank in 0..7) { "Square out of bounds: file=$file, rank=$rank" }
    }

    fun toAlgebraic(): String = "${('a' + file)}${rank + 1}"

    override fun toString(): String = toAlgebraic()

    companion object {
        fun fromAlgebraic(s: String): Square? {
            if (s.length < 2) return null
            val fileChar = s[0].lowercaseChar()
            val rankChar = s[1]
            val f = fileChar - 'a'
            val r = rankChar - '1'
            return if (f in 0..7 && r in 0..7) Square(f, r) else null
        }

        fun fromIndex(index: Int): Square {
            return Square(index % 8, index / 8)
        }
    }
}
