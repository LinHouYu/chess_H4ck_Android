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
        val engine = StockfishEngine(context = null)
        assertNotNull(engine)

        engine.setCallback(object : EngineCallback {
            override fun onAnalysisResult(
                bestMoveUci: String?,
                evalStr: String,
                fromSquare: Square?,
                toSquare: Square?
            ) {
            }
        })

        val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        engine.requestAnalysis(startFen, isBottomTurn = true, depth = 3, timeLimitMs = 300)
        engine.stop()
    }
}
