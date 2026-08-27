package com.linhouyu.chess_h4ck.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

class ChessSnowFallingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val chessSymbols = arrayOf(
        "♔", "♕", "♖", "♗", "♘", "♙",
        "♚", "♛", "♜", "♝", "♞", "♟"
    )

    private val particleColors = intArrayOf(
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#E0F2FE"),
        Color.parseColor("#BAE6FD"),
        Color.parseColor("#7DD3FC"),
        Color.parseColor("#38BDF8")
    )

    private class ChessSnowParticle(
        var x: Float,
        var y: Float,
        var size: Float,
        var speedY: Float,
        var swayAmp: Float,
        var swayPhase: Float,
        var swaySpeed: Float,
        var rotation: Float,
        var rotationSpeed: Float,
        var alpha: Int,
        var color: Int,
        var symbol: String
    )

    private val particles = mutableListOf<ChessSnowParticle>()
    private val particleCount = 42
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private var isRunning = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            initParticles(w, h)
            if (!isRunning) {
                isRunning = true
                postInvalidateOnAnimation()
            }
        }
    }

    private fun initParticles(w: Int, h: Int) {
        particles.clear()
        for (i in 0 until particleCount) {
            particles.add(createParticle(w, h, randomInitialY = true))
        }
    }

    private fun createParticle(w: Int, h: Int, randomInitialY: Boolean): ChessSnowParticle {
        val density = resources.displayMetrics.density
        val size = (Random.nextFloat() * 18f + 14f) * density
        val initialY = if (randomInitialY) Random.nextFloat() * h else -size - Random.nextFloat() * 50f
        val initialX = Random.nextFloat() * w

        return ChessSnowParticle(
            x = initialX,
            y = initialY,
            size = size,
            speedY = (Random.nextFloat() * 1.8f + 1.2f) * density,
            swayAmp = (Random.nextFloat() * 1.4f + 0.6f) * density,
            swayPhase = Random.nextFloat() * Math.PI.toFloat() * 2f,
            swaySpeed = Random.nextFloat() * 0.03f + 0.015f,
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = (Random.nextFloat() - 0.5f) * 1.5f,
            alpha = (Random.nextFloat() * 120 + 90).toInt().coerceIn(70, 230),
            color = particleColors[Random.nextInt(particleColors.size)],
            symbol = chessSymbols[Random.nextInt(chessSymbols.size)]
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return

        for (p in particles) {
            p.swayPhase += p.swaySpeed
            p.x += sin(p.swayPhase) * p.swayAmp
            p.y += p.speedY
            p.rotation += p.rotationSpeed

            if (p.y > h + p.size) {
                val newParticle = createParticle(w, h, randomInitialY = false)
                p.x = newParticle.x
                p.y = newParticle.y
                p.size = newParticle.size
                p.speedY = newParticle.speedY
                p.swayAmp = newParticle.swayAmp
                p.swayPhase = newParticle.swayPhase
                p.swaySpeed = newParticle.swaySpeed
                p.rotation = newParticle.rotation
                p.rotationSpeed = newParticle.rotationSpeed
                p.alpha = newParticle.alpha
                p.color = newParticle.color
                p.symbol = newParticle.symbol
            }

            paint.textSize = p.size
            paint.color = p.color
            paint.alpha = p.alpha

            canvas.save()
            canvas.translate(p.x, p.y)
            canvas.rotate(p.rotation)
            canvas.drawText(p.symbol, 0f, p.size * 0.35f, paint)
            canvas.restore()
        }

        if (isRunning && isShown) {
            postInvalidateOnAnimation()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isRunning = true
        postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isRunning = false
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        isRunning = visibility == VISIBLE
        if (isRunning) {
            postInvalidateOnAnimation()
        }
    }
}
