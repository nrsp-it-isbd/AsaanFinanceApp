package com.assan.finance.ui.custom

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class VoiceVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class State {
        LISTENING, // Slow calm pulse
        HEARING,   // Reacts to volume
        THINKING,  // Fast pulse/spin
        SPEAKING   // Medium pulsing
    }

    private var currentState = State.LISTENING

    private val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E2E8F0") // Slate 200 (Light Gray)
        style = Paint.Style.FILL
    }
    
    private val outerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33E2E8F0") // Semi-transparent
        style = Paint.Style.FILL
    }

    private var baseRadius = 150f
    private var currentRadius = baseRadius
    private var currentGlowRadius = baseRadius * 1.2f

    private var pulseAnimator: ValueAnimator? = null
    
    // For hearing state
    private var targetRadius = baseRadius
    private var smoothedRadius = baseRadius

    init {
        startListeningAnimation()
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        baseRadius = Math.min(w, h) / 4f
        currentRadius = baseRadius
        currentGlowRadius = baseRadius * 1.2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f

        if (currentState == State.HEARING) {
            // Smoothly interpolate to target radius for fluid reaction
            smoothedRadius += (targetRadius - smoothedRadius) * 0.2f
            canvas.drawCircle(cx, cy, smoothedRadius * 1.3f, outerGlowPaint)
            canvas.drawCircle(cx, cy, smoothedRadius, orbPaint)
            postInvalidateDelayed(16) // ~60fps
        } else {
            canvas.drawCircle(cx, cy, currentGlowRadius, outerGlowPaint)
            canvas.drawCircle(cx, cy, currentRadius, orbPaint)
        }
    }

    fun setState(state: State) {
        if (currentState == state) return
        currentState = state
        
        pulseAnimator?.cancel()
        
        when (state) {
            State.LISTENING -> startListeningAnimation()
            State.HEARING -> {
                targetRadius = baseRadius
                smoothedRadius = baseRadius
                invalidate()
            }
            State.THINKING -> startThinkingAnimation()
            State.SPEAKING -> startSpeakingAnimation()
        }
    }

    fun updateAmplitude(amplitude: Float) {
        if (currentState != State.HEARING) return
        
        // Map amplitude (e.g., 0 to 100 or rmsdB) to radius expansion
        // Assuming amplitude is normalized roughly 0..1 or 0..100
        // We will just scale base radius up to 2x max
        val scale = 1f + (amplitude / 50f).coerceIn(0f, 1f) 
        targetRadius = baseRadius * scale
    }

    private fun startListeningAnimation() {
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.15f).apply {
            duration = 1500
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val scale = it.animatedValue as Float
                currentRadius = baseRadius
                currentGlowRadius = baseRadius * scale
                invalidate()
            }
            start()
        }
    }

    private fun startThinkingAnimation() {
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.3f).apply {
            duration = 600
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val scale = it.animatedValue as Float
                currentRadius = baseRadius * (1f + (scale - 1f) * 0.3f)
                currentGlowRadius = baseRadius * scale
                invalidate()
            }
            start()
        }
    }

    private fun startSpeakingAnimation() {
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.2f).apply {
            duration = 1000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val scale = it.animatedValue as Float
                currentRadius = baseRadius * (1f + (scale - 1f) * 0.5f)
                currentGlowRadius = baseRadius * scale
                invalidate()
            }
            start()
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
    }
}
