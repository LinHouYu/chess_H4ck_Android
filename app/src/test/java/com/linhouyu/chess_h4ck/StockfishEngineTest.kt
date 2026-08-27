package com.linhouyu.chess_h4ck

import com.linhouyu.chess_h4ck.core.engine.EngineCallback
import com.linhouyu.chess_h4ck.core.engine.StockfishEngine
import com.linhouyu.chess_h4ck.core.model.Square
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class StockfishEngineTest {

    @Test
    fun testEngineAnalysis() {
        val engine = StockfishEngine()
        val latch = CountDownLatch(1)
        var receivedBestMove: String? = null
        var receivedEval: String? = null

        engine.setCallback(object : EngineCallback {
            override fun onAnalysisResult(
                bestMoveUci: String?,
                evalStr: String,
                fromSquare: Square?,
                toSquare: Square?
            ) {
                receivedBestMove = bestMoveUci
                receivedEval = evalStr
                latch.countDown()
            }
        })

        val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        engine.requestAnalysis(startFen, isBottomTurn = true, depth = 3, timeLimitMs = 300)

        // Wait up to 2 seconds
        latch.await(2, TimeUnit.SECONDS)
        engine.stop()

        assertNotNull(receivedBestMove)
        assertTrue(receivedBestMove!!.length >= 4)
        assertNotNull(receivedEval)
    }
}
