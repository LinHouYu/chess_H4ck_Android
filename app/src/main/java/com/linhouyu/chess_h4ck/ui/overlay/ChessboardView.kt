package com.linhouyu.chess_h4ck.ui.overlay

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.linhouyu.chess_h4ck.core.model.Square
import com.linhouyu.chess_h4ck.core.state.BoardStateManager
import com.linhouyu.chess_h4ck.util.CoordinateUtils
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class ChessboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var boardState: BoardStateManager? = null
        set(value) {
            field = value
            postInvalidate()
        }

    var onMoveListener: ((Square, Square) -> Unit)? = null

    var isFlipped: Boolean = false
        set(value) {
            field = value
            postInvalidate()
        }

    var isTouchThrough: Boolean = false
        set(value) {
            field = value
            postInvalidate()
        }

    // AI recommendation overlay
    var aiFromSquare: Square? = null
    var aiToSquare: Square? = null

    private var selectedSquare: Square? = null
    private var draggedSquare: Square? = null
    private var dragCurrentX: Float = 0f
    private var dragCurrentY: Float = 0f
    private var isDragging: Boolean = false

    private var touchDownX: Float = 0f
    private var touchDownY: Float = 0f
    private var hasMovedSignificantly: Boolean = false

    // Paints
    private val darkSquarePaint = Paint().apply {
        color = Color.parseColor("#44111115")
        style = Paint.Style.FILL
    }
    private val lightSquarePaint = Paint().apply {
        color = Color.parseColor("#33222228")
        style = Paint.Style.FILL
    }
    private val gridLinePaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val coordPaint = Paint().apply {
        color = Color.parseColor("#88a1a1aa")
        textSize = 22f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val piecePaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }
    private val pieceShadowPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }
    private val selectedPaint = Paint().apply {
        color = Color.parseColor("#f59e0b")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val selectedFillPaint = Paint().apply {
        color = Color.parseColor("#55f59e0b")
        style = Paint.Style.FILL
    }
    private val aiFromPaint = Paint().apply {
        color = Color.parseColor("#22c55e")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val aiToPaint = Paint().apply {
        color = Color.parseColor("#3b82f6")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val arrowPaint = Paint().apply {
        color = Color.parseColor("#ef4444")
        strokeWidth = 7f
        style = Paint.Style.FILL_AND_STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        if (size <= 0) return
        val sqSize = size / 8.0f

        // 1. Draw Checkered Squares
        for (r in 0..7) {
            for (f in 0..7) {
                val isLight = (f + r) % 2 != 0
                val left = f * sqSize
                val top = r * sqSize
                canvas.drawRect(left, top, left + sqSize, top + sqSize, if (isLight) lightSquarePaint else darkSquarePaint)
            }
        }

        // 2. Draw Solid Pure Black Grid Lines
        for (i in 0..8) {
            val pos = i * sqSize
            canvas.drawLine(pos, 0f, pos, size, gridLinePaint)
            canvas.drawLine(0f, pos, size, pos, gridLinePaint)
        }

        // 3. Draw Board Coordinate Labels (a-h, 1-8)
        coordPaint.textSize = sqSize * 0.18f
        for (i in 0..7) {
            val fileChar = if (!isFlipped) ('a' + i) else ('h' - i)
            val rankChar = if (!isFlipped) ('8' - i) else ('1' + i)
            // Files on bottom
            canvas.drawText("$fileChar", i * sqSize + 4f, size - 4f, coordPaint)
            // Ranks on right
            canvas.drawText("$rankChar", size - coordPaint.textSize - 2f, i * sqSize + coordPaint.textSize + 2f, coordPaint)
        }

        val state = boardState

        // 4. Draw AI Best Move Highlight & Arrow
        if (aiFromSquare != null && aiToSquare != null) {
            drawSquareHighlight(canvas, aiFromSquare!!, sqSize, aiFromPaint, null)
            drawSquareHighlight(canvas, aiToSquare!!, sqSize, aiToPaint, null)
            drawAiArrow(canvas, aiFromSquare!!, aiToSquare!!, sqSize)
        }

        // 5. Draw Selected Square
        if (selectedSquare != null && selectedSquare != draggedSquare) {
            drawSquareHighlight(canvas, selectedSquare!!, sqSize, selectedPaint, selectedFillPaint)
        }

        // 6. Draw Pieces
        val fontSize = sqSize * 0.72f
        piecePaint.textSize = fontSize
        pieceShadowPaint.textSize = fontSize

        if (state != null) {
            for (r in 0..7) {
                for (f in 0..7) {
                    val sq = Square(f, r)
                    val piece = state.getPiece(sq) ?: continue

                    // If piece is currently dragged, skip drawing it on the cell
                    if (isDragging && draggedSquare == sq) continue

                    val col = if (!isFlipped) f else 7 - f
                    val row = if (!isFlipped) 7 - r else r

                    val cx = (col + 0.5f) * sqSize
                    val cy = (row + 0.5f) * sqSize + (fontSize * 0.35f)

                    drawPieceUnicode(canvas, piece.unicode, piece.isWhite, cx, cy)
                }
            }

            // 7. Draw Dragged Piece following finger
            if (isDragging && draggedSquare != null) {
                val piece = state.getPiece(draggedSquare!!)
                if (piece != null) {
                    drawPieceUnicode(canvas, piece.unicode, piece.isWhite, dragCurrentX, dragCurrentY + (fontSize * 0.35f))
                }
            }
        }
    }

    private fun drawSquareHighlight(canvas: Canvas, sq: Square, sqSize: Float, strokePaint: Paint, fillPaint: Paint?) {
        val col = if (!isFlipped) sq.file else 7 - sq.file
        val row = if (!isFlipped) 7 - sq.rank else sq.rank
        val left = col * sqSize + 4f
        val top = row * sqSize + 4f
        val right = left + sqSize - 8f
        val bottom = top + sqSize - 8f
        if (fillPaint != null) {
            canvas.drawRect(left, top, right, bottom, fillPaint)
        }
        canvas.drawRect(left, top, right, bottom, strokePaint)
    }

    private fun drawPieceUnicode(canvas: Canvas, unicode: String, isWhite: Boolean, cx: Float, cy: Float) {
        // High contrast shadow
        pieceShadowPaint.color = if (isWhite) Color.BLACK else Color.WHITE
        for (dx in -2..2) {
            for (dy in -2..2) {
                if (dx != 0 || dy != 0) {
                    canvas.drawText(unicode, cx + dx, cy + dy, pieceShadowPaint)
                }
            }
        }

        piecePaint.color = if (isWhite) Color.WHITE else Color.parseColor("#0f172a")
        canvas.drawText(unicode, cx, cy, piecePaint)
    }

    private fun drawAiArrow(canvas: Canvas, from: Square, to: Square, sqSize: Float) {
        val fromCol = if (!isFlipped) from.file else 7 - from.file
        val fromRow = if (!isFlipped) 7 - from.rank else from.rank
        val toCol = if (!isFlipped) to.file else 7 - to.file
        val toRow = if (!isFlipped) 7 - to.rank else to.rank

        val x1 = (fromCol + 0.5f) * sqSize
        val y1 = (fromRow + 0.5f) * sqSize
        val x2 = (toCol + 0.5f) * sqSize
        val y2 = (toRow + 0.5f) * sqSize

        val dx = x2 - x1
        val dy = y2 - y1
        val dist = hypot(dx, dy)
        if (dist <= 0) return

        val shortenedDist = maxOf(0f, dist - 16f)
        val ax2 = x1 + (dx / dist) * shortenedDist
        val ay2 = y1 + (dy / dist) * shortenedDist

        // Draw line body
        canvas.drawLine(x1, y1, ax2, ay2, arrowPaint)

        // Draw Arrowhead
        val angle = atan2(dy, dx)
        val arrowHeadLength = sqSize * 0.28f
        val arrowAngle = Math.PI / 6.0

        val path = Path().apply {
            moveTo(ax2, ay2)
            lineTo(
                (ax2 - arrowHeadLength * cos(angle - arrowAngle)).toFloat(),
                (ay2 - arrowHeadLength * sin(angle - arrowAngle)).toFloat()
            )
            lineTo(
                (ax2 - arrowHeadLength * cos(angle + arrowAngle)).toFloat(),
                (ay2 - arrowHeadLength * sin(angle + arrowAngle)).toFloat()
            )
            close()
        }
        canvas.drawPath(path, arrowPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isTouchThrough) return false

        val sq = CoordinateUtils.pixelToSquare(event.x, event.y, minOf(width, height).toFloat(), isFlipped)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                hasMovedSignificantly = false

                if (sq == null) {
                    selectedSquare = null
                    isDragging = false
                    draggedSquare = null
                    postInvalidate()
                    return true
                }

                val currentPiece = boardState?.getPiece(sq)

                // 1. If a square is already selected (Click-to-Move / Capture)
                if (selectedSquare != null) {
                    val from = selectedSquare!!
                    val fromPiece = boardState?.getPiece(from)

                    // Clicked same square -> Deselect
                    if (sq == from) {
                        selectedSquare = null
                        isDragging = false
                        draggedSquare = null
                        postInvalidate()
                        return true
                    }

                    // Clicked friendly piece and NOT in sandbox mode -> Switch selection
                    val isSameColor = fromPiece != null && currentPiece != null && fromPiece.color == currentPiece.color
                    if (isSameColor && boardState?.sandboxMode != true) {
                        selectedSquare = sq
                        draggedSquare = sq
                        dragCurrentX = event.x
                        dragCurrentY = event.y
                        isDragging = true
                        postInvalidate()
                        return true
                    } else {
                        // Execute Move / Capture
                        selectedSquare = null
                        draggedSquare = null
                        isDragging = false
                        onMoveListener?.invoke(from, sq)
                        postInvalidate()
                        return true
                    }
                }

                // 2. No square previously selected -> Start selection / dragging
                if (currentPiece != null) {
                    selectedSquare = sq
                    draggedSquare = sq
                    dragCurrentX = event.x
                    dragCurrentY = event.y
                    isDragging = true
                } else {
                    selectedSquare = null
                }
                postInvalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = event.x - touchDownX
                    val dy = event.y - touchDownY
                    if (dx * dx + dy * dy > 36) {
                        hasMovedSignificantly = true
                    }
                    dragCurrentX = event.x
                    dragCurrentY = event.y
                    postInvalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging && draggedSquare != null) {
                    val from = draggedSquare!!
                    if (hasMovedSignificantly && sq != null && sq != from) {
                        // Drag-and-drop completed!
                        selectedSquare = null
                        draggedSquare = null
                        isDragging = false
                        onMoveListener?.invoke(from, sq)
                        postInvalidate()
                        return true
                    }
                }
                // Tap release -> keep selectedSquare so next tap can complete move
                isDragging = false
                draggedSquare = null
                postInvalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun setAiRecommendation(from: Square?, to: Square?) {
        post {
            this.aiFromSquare = from
            this.aiToSquare = to
            invalidate()
        }
    }
}
