package com.linhouyu.chess_h4ck.ui.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.linhouyu.chess_h4ck.R

class FloatingMenuOverlay(
    private val context: Context,
    private val callbacks: MenuCallbacks
) {
    interface MenuCallbacks {
        fun onReset()
        fun onToggleSandbox()
        fun onToggleTurn()
        fun onFlipBoard()
        fun onUndo()
        fun onAiMove()
        fun onToggleAutoPlay()
        fun onToggleBoardVisibility()
        fun onToggleTouchThrough()
        fun onClose()
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootView: View? = null
    private var isExpanded = false

    private var tvToggleBoard: TextView? = null

    fun show() {
        if (rootView != null) return

        val themedContext = ContextThemeWrapper(context, R.style.Theme_Chess_H4ck)
        val inflater = LayoutInflater.from(themedContext)
        val view = inflater.inflate(R.layout.overlay_floating_menu, null)
        rootView = view

        val layoutFabBubble = view.findViewById<View>(R.id.layoutFabBubble)
        val btnFabMain = view.findViewById<ImageView>(R.id.btnFabMain)
        val menuContainer = view.findViewById<LinearLayout>(R.id.menuContainer)
        val layoutMenuHeader = view.findViewById<LinearLayout>(R.id.layoutMenuHeader)
        val btnCollapseMenu = view.findViewById<ImageView>(R.id.btnCollapseMenu)

        val btnReset = view.findViewById<View>(R.id.btnReset)
        val btnSandbox = view.findViewById<View>(R.id.btnSandbox)
        val btnTurn = view.findViewById<View>(R.id.btnTurn)
        val btnFlip = view.findViewById<View>(R.id.btnFlip)
        val btnUndo = view.findViewById<View>(R.id.btnUndo)
        val btnAiMove = view.findViewById<View>(R.id.btnAiMove)
        val btnAutoPlay = view.findViewById<View>(R.id.btnAutoPlay)
        val btnToggleBoard = view.findViewById<View>(R.id.btnToggleBoard)
        tvToggleBoard = view.findViewById(R.id.tvToggleBoard)
        val btnTouchThrough = view.findViewById<View>(R.id.btnTouchThrough)
        val btnClose = view.findViewById<View>(R.id.btnClose)

        val displayMetrics = context.resources.displayMetrics
        val menuWidthPx = (340 * displayMetrics.density).toInt()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (displayMetrics.widthPixels - 70 * displayMetrics.density).toInt()
            y = (displayMetrics.heightPixels * 0.35f).toInt()
        }

        val expandMenu = {
            isExpanded = true
            layoutFabBubble.visibility = View.GONE
            menuContainer.visibility = View.VISIBLE

            if (params.x + menuWidthPx > displayMetrics.widthPixels) {
                params.x = maxOf(16, displayMetrics.widthPixels - menuWidthPx - (16 * displayMetrics.density).toInt())
            }
            if (params.x < 16) {
                params.x = 16
            }
            try {
                windowManager.updateViewLayout(view, params)
            } catch (e: Exception) {}
        }

        val collapseMenu = {
            isExpanded = false
            menuContainer.visibility = View.GONE
            layoutFabBubble.visibility = View.VISIBLE
            try {
                windowManager.updateViewLayout(view, params)
            } catch (e: Exception) {}
        }

        btnCollapseMenu.setOnClickListener {
            collapseMenu()
        }

        // 1. Draggable FAB Bubble (Collapsed State)
        btnFabMain.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isMoved = false

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isMoved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (dx * dx + dy * dy > 25) {
                            isMoved = true
                            params.x = (initialX + dx).toInt()
                            params.y = (initialY + dy).toInt()
                            try {
                                windowManager.updateViewLayout(view, params)
                            } catch (e: Exception) {}
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isMoved) {
                            expandMenu()
                        }
                        return true
                    }
                }
                return false
            }
        })

        // 2. Draggable Control Panel Header (Expanded State)
        layoutMenuHeader.setOnTouchListener(object : View.OnTouchListener {
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
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        params.x = (initialX + dx).toInt()
                        params.y = (initialY + dy).toInt()
                        try {
                            windowManager.updateViewLayout(view, params)
                        } catch (e: Exception) {}
                        return true
                    }
                }
                return false
            }
        })

        // Bind Action Buttons
        btnReset.setOnClickListener { callbacks.onReset() }
        btnSandbox.setOnClickListener { callbacks.onToggleSandbox() }
        btnTurn.setOnClickListener { callbacks.onToggleTurn() }
        btnFlip.setOnClickListener { callbacks.onFlipBoard() }
        btnUndo.setOnClickListener { callbacks.onUndo() }
        btnAiMove.setOnClickListener { callbacks.onAiMove() }
        btnAutoPlay.setOnClickListener { callbacks.onToggleAutoPlay() }
        btnToggleBoard.setOnClickListener { callbacks.onToggleBoardVisibility() }
        btnTouchThrough.setOnClickListener { callbacks.onToggleTouchThrough() }
        btnClose.setOnClickListener { callbacks.onClose() }

        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateBoardVisibilityButton(isVisible: Boolean) {
        tvToggleBoard?.text = if (isVisible) "隐藏棋盘" else "显示棋盘"
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
