package com.linhouyu.chess_h4ck.util

import com.linhouyu.chess_h4ck.core.model.Square

object CoordinateUtils {

    /**
     * Converts a square to absolute screen pixel coordinate (centerX, centerY).
     * @param sq Target square (file 0-7, rank 0-7)
     * @param boardScreenX X position of chessboard on screen
     * @param boardScreenY Y position of chessboard on screen
     * @param boardWidth Pixel width of chessboard view
     * @param boardHeight Pixel height of chessboard view
     * @param isFlipped Whether board orientation is flipped (Black on bottom)
     */
    fun squareToScreenCoords(
        sq: Square,
        boardScreenX: Int,
        boardScreenY: Int,
        boardWidth: Int,
        boardHeight: Int,
        isFlipped: Boolean
    ): Pair<Float, Float> {
        val squareSize = minOf(boardWidth, boardHeight) / 8.0f

        val col = if (!isFlipped) sq.file else 7 - sq.file
        val row = if (!isFlipped) 7 - sq.rank else sq.rank

        val centerX = boardScreenX + (col + 0.5f) * squareSize
        val centerY = boardScreenY + (row + 0.5f) * squareSize

        return Pair(centerX, centerY)
    }

    /**
     * Converts canvas pixel coordinates inside chessboard view to Square.
     */
    fun pixelToSquare(
        px: Float,
        py: Float,
        boardSize: Float,
        isFlipped: Boolean
    ): Square? {
        val sqSize = boardSize / 8.0f
        if (sqSize <= 0f) return null

        val col = (px / sqSize).toInt()
        val row = (py / sqSize).toInt()

        if (col !in 0..7 || row !in 0..7) return null

        val file = if (!isFlipped) col else 7 - col
        val rank = if (!isFlipped) 7 - row else row

        return if (file in 0..7 && rank in 0..7) Square(file, rank) else null
    }
}
