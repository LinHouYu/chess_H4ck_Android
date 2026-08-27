package com.linhouyu.chess_h4ck.ui.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.linhouyu.chess_h4ck.R

class StatusBarOverlay(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootView: View? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var tvTurn: TextView? = null
    private var tvMode: TextView? = null
    private var tvAiMove: TextView? = null
    private var tvEval: TextView? = null
    private var tvActionLog: TextView? = null

    fun show() {
        if (rootView != null) return

        val themedContext = ContextThemeWrapper(context, R.style.Theme_Chess_H4ck)
        val inflater = LayoutInflater.from(themedContext)
        val view = inflater.inflate(R.layout.overlay_status_bar, null)
        rootView = view

        tvTurn = view.findViewById(R.id.tvTurn)
        tvMode = view.findViewById(R.id.tvMode)
        tvAiMove = view.findViewById(R.id.tvAiMove)
        tvEval = view.findViewById(R.id.tvEval)
        tvActionLog = view.findViewById(R.id.tvActionLog)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 0
        }

        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateTurn(turnName: String, isBottomTurn: Boolean) {
        mainHandler.post {
            val prefix = if (turnName.contains("白棋")) "♙ " else "♟ "
            tvTurn?.text = "${prefix}回合: $turnName"
            tvTurn?.setTextColor(if (isBottomTurn) Color.WHITE else Color.parseColor("#94a3b8"))
        }
    }

    fun updateMode(isSandbox: Boolean) {
        mainHandler.post {
            if (isSandbox) {
                tvMode?.text = "[自由沙盒]"
                tvMode?.setTextColor(Color.parseColor("#facc15"))
            } else {
                tvMode?.text = "[对局模式]"
                tvMode?.setTextColor(Color.parseColor("#38bdf8"))
            }
        }
    }

    fun updateAiAnalysis(bestMove: String?, evalStr: String, isBottomTurn: Boolean) {
        mainHandler.post {
            tvEval?.text = "评分: $evalStr"
            if (!isBottomTurn) {
                tvAiMove?.text = "AI: 等待对手落子..."
                tvAiMove?.setTextColor(Color.parseColor("#94a3b8"))
            } else if (bestMove != null) {
                val moveDisplay = if (bestMove.length >= 4) {
                    "${bestMove.substring(0, 2)} -> ${bestMove.substring(2, 4)}"
                } else bestMove
                tvAiMove?.text = "AI推荐: $moveDisplay"
                tvAiMove?.setTextColor(Color.parseColor("#4ade80"))
            } else {
                tvAiMove?.text = "AI: 思考中..."
                tvAiMove?.setTextColor(Color.parseColor("#94a3b8"))
            }
        }
    }

    fun flashAction(actionDesc: String) {
        mainHandler.post {
            tvActionLog?.text = actionDesc
            tvActionLog?.setTextColor(Color.parseColor("#38bdf8"))
            mainHandler.postDelayed({
                tvActionLog?.setTextColor(Color.parseColor("#64748b"))
            }, 2500)
        }
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
