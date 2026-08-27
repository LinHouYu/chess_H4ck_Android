package com.linhouyu.chess_h4ck.core.engine

import com.linhouyu.chess_h4ck.core.model.Square

interface EngineCallback {
    fun onAnalysisResult(
        bestMoveUci: String?,
        evalStr: String,
        fromSquare: Square?,
        toSquare: Square?
    )
}
