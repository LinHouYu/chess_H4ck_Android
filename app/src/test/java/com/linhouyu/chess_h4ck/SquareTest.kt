package com.linhouyu.chess_h4ck

import com.linhouyu.chess_h4ck.core.model.Square
import org.junit.Assert.*
import org.junit.Test

class SquareTest {

    @Test
    fun testSquareAlgebraic() {
        val e4 = Square.fromAlgebraic("e4")
        assertNotNull(e4)
        assertEquals(4, e4!!.file)
        assertEquals(3, e4.rank)
        assertEquals("e4", e4.toAlgebraic())

        val a1 = Square.fromAlgebraic("a1")
        assertNotNull(a1)
        assertEquals(0, a1!!.file)
        assertEquals(0, a1.rank)
        assertEquals("a1", a1.toAlgebraic())

        val h8 = Square.fromAlgebraic("h8")
        assertNotNull(h8)
        assertEquals(7, h8!!.file)
        assertEquals(7, h8.rank)
        assertEquals("h8", h8.toAlgebraic())
    }

    @Test
    fun testInvalidAlgebraic() {
        assertNull(Square.fromAlgebraic("i9"))
        assertNull(Square.fromAlgebraic("z0"))
        assertNull(Square.fromAlgebraic(""))
    }
}
