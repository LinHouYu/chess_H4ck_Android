package com.linhouyu.chess_h4ck.ui.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.widget.ImageView
import com.linhouyu.chess_h4ck.R
import com.linhouyu.chess_h4ck.core.model.Square
import com.linhouyu.chess_h4ck.core.state.BoardStateManager
import com.linhouyu.chess_h4ck.util.CoordinateUtils

class ChessboardOverlay(
    private val context: Context,
    val boardState: BoardStateManager,
    val onUserMove: (Square, Square) -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootView: View? = null
    lateinit var chessboardView: ChessboardView
        private set

    var windowX: Int = 100
        private set
    var windowY: Int = 300
        private set
    var boardSizePx: Int = 720
        private set

    var isVisible: Boolean = true
        private set

    private var layoutParams: WindowManager.LayoutParams? = null

    fun show() {
        if (rootView != null) return

        val displayMetrics = context.resources.displayMetrics
        boardSizePx = (displayMetrics.widthPixels * 0.88f).toInt()
        windowX = (displayMetrics.widthPixels - boardSizePx) / 2
        windowY = (displayMetrics.heightPixels - boardSizePx) / 2

        val themedContext = ContextThemeWrapper(context, R.style.Theme_Chess_H4ck)
        val inflater = LayoutInflater.from(themedContext)
        val view = inflater.inflate(R.layout.overlay_chessboard, null)
        rootView = view

        chessboardView = view.findViewById(R.id.chessboardView)
        chessboardView.boardState = boardState
        chessboardView.onMoveListener = onUserMove

        val dragHeader = view.findViewById<View>(R.id.dragHeader)
        val resizeGrip = view.findViewById<ImageView>(R.id.resizeGrip)

        val headerHeightPx = (36 * displayMetrics.density).toInt()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            boardSizePx,
            boardSizePx + headerHeightPx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = windowX
            y = windowY
        }
        layoutParams = params

        // Drag to move floating window
        dragHeader.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowX = params.x
                        windowY = params.y
                        try {
                            windowManager.updateViewLayout(view, params)
                        } catch (e: Exception) {}
                        return true
                    }
                }
                return false
            }
        })

        // Resize Grip at bottom right to freely scale chessboard
        resizeGrip.setOnTouchListener(object : View.OnTouchListener {
            private var initialSize = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialSize = boardSizePx
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY
                        val delta = maxOf(deltaX, deltaY)
                        val newSize = (initialSize + delta).toInt()
                            .coerceIn(360, displayMetrics.widthPixels)

                        boardSizePx = newSize
                        params.width = newSize
                        params.height = newSize + headerHeightPx
                        try {
                            windowManager.updateViewLayout(view, params)
                        } catch (e: Exception) {}
                        chessboardView.postInvalidate()
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleVisibility(): Boolean {
        isVisible = !isVisible
        rootView?.visibility = if (isVisible) View.VISIBLE else View.GONE
        return isVisible
    }

    fun getSquareScreenCenter(sq: Square): Pair<Float, Float> {
        val headerHeightPx = (36 * context.resources.displayMetrics.density).toInt()
        val boardLeft = windowX
        val boardTop = windowY + headerHeightPx // offset header
        return CoordinateUtils.squareToScreenCoords(
            sq,
            boardLeft,
            boardTop,
            boardSizePx,
            boardSizePx,
            chessboardView.isFlipped
        )
    }

    fun toggleFlip(): Boolean {
        chessboardView.isFlipped = !chessboardView.isFlipped
        return chessboardView.isFlipped
    }

    fun toggleTouchThrough(): Boolean {
        chessboardView.isTouchThrough = !chessboardView.isTouchThrough
        val params = layoutParams
        val view = rootView
        if (params != null && view != null) {
            if (chessboardView.isTouchThrough) {
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            } else {
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            }
            try {
                windowManager.updateViewLayout(view, params)
            } catch (e: Exception) {}
        }
        return chessboardView.isTouchThrough
    }

    fun setTouchThroughWindowFlag(enable: Boolean) {
        val params = layoutParams ?: return
        val view = rootView ?: return
        if (enable) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            if (!chessboardView.isTouchThrough) {
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            }
        }
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {}
    }

    fun redraw() {
        chessboardView.postInvalidate()
    }

    fun hide() {
        rootView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
            rootView = null
        }
    }
}
