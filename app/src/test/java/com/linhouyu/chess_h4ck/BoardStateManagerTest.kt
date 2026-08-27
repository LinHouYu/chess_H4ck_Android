package com.linhouyu.chess_h4ck

import com.linhouyu.chess_h4ck.core.model.PieceColor
import com.linhouyu.chess_h4ck.core.model.PieceType
import com.linhouyu.chess_h4ck.core.model.Square
import com.linhouyu.chess_h4ck.core.state.BoardStateManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BoardStateManagerTest {

    private lateinit var manager: BoardStateManager

    @Before
    fun setUp() {
        manager = BoardStateManager()
    }

    @Test
    fun testInitialSetup() {
        // e2 pawn
        val e2 = manager.getPiece(Square(4, 1))
        assertNotNull(e2)
        assertEquals(PieceType.PAWN, e2!!.type)
        assertEquals(PieceColor.WHITE, e2.color)

        // e1 king
        val e1 = manager.getPiece(Square(4, 0))
        assertNotNull(e1)
        assertEquals(PieceType.KING, e1!!.type)
        assertEquals(PieceColor.WHITE, e1.color)

        // e8 king
        val e8 = manager.getPiece(Square(4, 7))
        assertNotNull(e8)
        assertEquals(PieceType.KING, e8!!.type)
        assertEquals(PieceColor.BLACK, e8.color)

        assertEquals(PieceColor.WHITE, manager.turn)
    }

    @Test
    fun testStandardMoveAndTurnSwitch() {
        val from = Square.fromAlgebraic("e2")!!
        val to = Square.fromAlgebraic("e4")!!

        val success = manager.movePiece(from, to, autoSwitchTurn = true)
        assertTrue(success)
        assertNull(manager.getPiece(from))
        assertNotNull(manager.getPiece(to))
        assertEquals(PieceType.PAWN, manager.getPiece(to)!!.type)
        assertEquals(PieceColor.BLACK, manager.turn)
    }

    @Test
    fun testUndo() {
        val from = Square.fromAlgebraic("e2")!!
        val to = Square.fromAlgebraic("e4")!!
        manager.movePiece(from, to, autoSwitchTurn = true)

        val undone = manager.undo()
        assertTrue(undone)
        assertNotNull(manager.getPiece(from))
        assertNull(manager.getPiece(to))
        assertEquals(PieceColor.WHITE, manager.turn)
    }

    @Test
    fun testSandboxUnconditionalCapture() {
        manager.sandboxMode = true
        // Move White e2 pawn to capture own e1 King! (Sandbox full authority)
        val from = Square.fromAlgebraic("e2")!!
        val to = Square.fromAlgebraic("e1")!!

        val success = manager.movePiece(from, to, autoSwitchTurn = false)
        assertTrue(success)
        assertNull(manager.getPiece(from))
        assertEquals(PieceType.PAWN, manager.getPiece(to)!!.type)
    }

    @Test
    fun testPawnPromotion() {
        // Clear board and place white pawn at e7
        for (r in 0..7) {
            for (f in 0..7) {
                manager.grid[Square(f, r)] = null
            }
        }
        val e7 = Square.fromAlgebraic("e7")!!
        val e8 = Square.fromAlgebraic("e8")!!
        manager.grid[e7] = com.linhouyu.chess_h4ck.core.model.Piece(PieceType.PAWN, PieceColor.WHITE)

        manager.movePiece(e7, e8)
        val promoted = manager.getPiece(e8)
        assertNotNull(promoted)
        assertEquals(PieceType.QUEEN, promoted!!.type)
        assertEquals(PieceColor.WHITE, promoted.color)
    }

    @Test
    fun testFenSerialization() {
        val fen = manager.getFen()
        assertTrue(fen.startsWith("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq"))
    }
}
