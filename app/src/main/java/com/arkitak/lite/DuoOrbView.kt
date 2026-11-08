package com.arkitak.lite

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.os.SystemClock
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** One orb of the pair. Wraps either the particle orb (OrbView) or the glass orb (GlassOrbView) behind the same calls. */
interface OrbHandle {
    val view: View
    fun setState(state: String)
    fun pushAudio(level: Float)
    fun pulse(argb: Int)
    fun onResume()
    fun onPause()
    fun destroy()
}

/** The particle orb of the main screen, without its halo (two halos on top of each other look wrong). */
class ParticleOrbHandle(context: Context, theme: String) : OrbHandle {
    private val orb = OrbView(context, fullField = false, halo = false)
    init { orb.setTheme(theme) }
    override val view: View get() = orb
    override fun setState(state: String) = orb.setState(state)
    override fun pushAudio(level: Float) = orb.pushAudio(level)
    override fun pulse(argb: Int) {}
    override fun onResume() = orb.onResume()
    override fun onPause() = orb.onPause()
    override fun destroy() = orb.destroy()
}

/** The glass orb of the floating avatar. */
class GlassOrbHandle(context: Context, design: String) : OrbHandle {
    private val orb = GlassOrbView(context, picker = false, design = design)
    override val view: View get() = orb
    override fun setState(state: String) = orb.setState(state)
    override fun pushAudio(level: Float) = orb.pushAudio(level)
    override fun pulse(argb: Int) = orb.pulse(argb)
    override fun onResume() = orb.onResume()
    override fun onPause() = orb.onPause()
    override fun destroy() = orb.destroy()
}

/**
 * Olivia and ARKITAK talking to each other (group chat): one orb splits into two, Olivia on top and ARKITAK below, and a living
 * line grows between them. The line carries little lights from the one who speaks to the one who listens.
 * The same view is used on the main screen, in the assistant-gesture screen and in the floating avatar, so they all look the same.
 *
 * Calls to [setState], [setActive] and [pushAudio] are safe from any thread. [open] and [close] run on the main thread.
 */
class DuoOrbView(
    context: Context,
    make: (String) -> OrbHandle
) : FrameLayout(context) {

    /** Tap and long-press on the pair (the whole view takes the touches). */
    var onTap: (() -> Unit)? = null
    var onHold: (() -> Unit)? = null

    private val orbTop: OrbHandle = make(Personas.OLIVIA)
    private val orbBottom: OrbHandle = make(Personas.ARKITAK)
    private val link = LinkView(context)
    private val touch = View(context)

    @Volatile private var active = Personas.OLIVIA
    @Volatile private var state = OrbView.STATE_IDLE
    @Volatile private var level = 0f
    private var lastActive: String? = null
    private var split = 0f
    private var side = 0
    private var anim: ValueAnimator? = null

    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean { onTap?.invoke(); return true }
        override fun onLongPress(e: MotionEvent) { onHold?.invoke() }
    })

    init {
        clipChildren = false
        clipToPadding = false
        addView(link, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(orbTop.view, LayoutParams(1, 1, Gravity.CENTER))
        addView(orbBottom.view, LayoutParams(1, 1, Gravity.CENTER))
        addView(touch, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        touch.setOnTouchListener { _, e -> gestures.onTouchEvent(e); true }
        applySplit(0f)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        side = min(w.toFloat(), h / 2.24f).toInt().coerceAtLeast(1)
        resize(orbTop.view)
        resize(orbBottom.view)
        applySplit(split)
    }

    private fun resize(v: View) {
        val lp = v.layoutParams as LayoutParams
        lp.width = side
        lp.height = side
        v.layoutParams = lp
    }

    /** p = 0: both orbs on top of each other in the middle (hidden). p = 1: apart, line between them. */
    private fun applySplit(p: Float) {
        split = p
        val d = side * 0.62f * p
        orbTop.view.translationY = -d
        orbBottom.view.translationY = d
        val a = (p * 2.5f).coerceIn(0f, 1f)
        val sc = 1.12f - 0.12f * p
        for (v in arrayOf(orbTop.view, orbBottom.view)) {
            v.alpha = a
            v.scaleX = sc
            v.scaleY = sc
        }
        link.set(p, d, side)
    }

    // ---------------------------------------------------------------- animation

    fun open() = animateTo(1f, 700L, OvershootInterpolator(0.8f)) {}

    /** The two orbs flow back together, then [done] is called (remove and destroy the view there). */
    fun close(done: () -> Unit) = animateTo(0f, 450L, DecelerateInterpolator()) { done() }

    private fun animateTo(target: Float, ms: Long, interp: Interpolator, end: () -> Unit) {
        anim?.cancel()
        val v = ValueAnimator.ofFloat(split, target)
        v.duration = ms
        v.interpolator = interp
        v.addUpdateListener { applySplit(it.animatedValue as Float) }
        v.addListener(object : AnimatorListenerAdapter() {
            private var cancelled = false
            override fun onAnimationCancel(animation: Animator) { cancelled = true }
            override fun onAnimationEnd(animation: Animator) { if (!cancelled) end() }
        })
        anim = v
        v.start()
    }

    // ---------------------------------------------------------------- what the orbs show

    /** The persona that has the floor right now (speaking or thinking). */
    fun setActive(persona: String) {
        val p = Personas.valid(persona)
        val changed = lastActive != null && lastActive != p
        active = p
        lastActive = p
        applyStates()
        if (changed) (if (p == Personas.OLIVIA) orbTop else orbBottom).pulse(Personas.color(p))
    }

    fun setState(s: String) {
        state = s
        applyStates()
    }

    /** The one who speaks moves; the other one listens. When you speak, both listen. */
    private fun applyStates() {
        val act = if (active == Personas.OLIVIA) orbTop else orbBottom
        val oth = if (active == Personas.OLIVIA) orbBottom else orbTop
        when (state) {
            OrbView.STATE_SPEAKING -> { act.setState(OrbView.STATE_SPEAKING); oth.setState(OrbView.STATE_LISTENING) }
            OrbView.STATE_THINKING -> { act.setState(OrbView.STATE_THINKING); oth.setState(OrbView.STATE_IDLE) }
            OrbView.STATE_LISTENING -> { act.setState(OrbView.STATE_LISTENING); oth.setState(OrbView.STATE_LISTENING) }
            else -> { act.setState(OrbView.STATE_IDLE); oth.setState(OrbView.STATE_IDLE) }
        }
    }

    fun pushAudio(l: Float) {
        level = max(level, l.coerceIn(0f, 1f))
        val act = if (active == Personas.OLIVIA) orbTop else orbBottom
        val oth = if (active == Personas.OLIVIA) orbBottom else orbTop
        when (state) {
            OrbView.STATE_SPEAKING -> act.pushAudio(l)
            OrbView.STATE_LISTENING -> { act.pushAudio(l); oth.pushAudio(l) }
            else -> {}
        }
    }

    fun onResume() { orbTop.onResume(); orbBottom.onResume(); applyStates() }
    fun onPause() { orbTop.onPause(); orbBottom.onPause() }

    fun destroy() {
        anim?.cancel()
        onTap = null
        onHold = null
        orbTop.destroy()
        orbBottom.destroy()
    }

    // ---------------------------------------------------------------- the line between the two orbs

    private inner class LinkView(c: Context) : View(c) {
        private var progress = 0f
        private var d = 0f
        private var side = 0
        private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val core = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.WHITE }
        private val path = Path()
        private var shaderKey = ""

        fun set(p: Float, distance: Float, s: Int) {
            progress = p
            d = distance
            side = s
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val a = ((progress - 0.5f) * 2f).coerceIn(0f, 1f)
            if (a <= 0f || side == 0) return
            val density = resources.displayMetrics.density
            val cx = width / 2f
            val cy = height / 2f
            val r = side * 0.30f
            val y0 = cy - d + r
            val y1 = cy + d - r
            if (y1 - y0 < 4f) return

            val speaking = state == OrbView.STATE_SPEAKING
            val now = SystemClock.uptimeMillis()
            val lvl = level
            level = lvl * 0.92f

            val key = "$y0/$y1"
            if (key != shaderKey) {
                shaderKey = key
                val sh = LinearGradient(cx, y0, cx, y1, Personas.color(Personas.OLIVIA), Personas.color(Personas.ARKITAK), Shader.TileMode.CLAMP)
                glow.shader = sh
                core.shader = sh
            }
            glow.strokeWidth = 7f * density
            core.strokeWidth = 2f * density
            glow.alpha = (60 * a).toInt()
            core.alpha = (230 * a).toInt()

            val amp = density * (1.2f + (if (speaking) 9f * lvl else 0f))
            val phase = now * 0.006f * (if (speaking) 1.6f else 0.8f)
            val n = 28
            path.reset()
            for (i in 0..n) {
                val t = i / n.toFloat()
                val x = cx + amp * sin(phase - t * 9f) * sin(PI.toFloat() * t)
                val y = y0 + (y1 - y0) * t
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            canvas.drawPath(path, glow)
            canvas.drawPath(path, core)

            // small lights travel from the one who speaks to the one who listens
            val fromTop = active == Personas.OLIVIA
            val speed = if (speaking) 0.0007f else 0.00025f
            for (k in 0 until 3) {
                val u = (now * speed + k / 3f) % 1f
                val t = if (fromTop) u else 1f - u
                val x = cx + amp * sin(phase - t * 9f) * sin(PI.toFloat() * t)
                val y = y0 + (y1 - y0) * t
                val fade = sin(PI.toFloat() * u) * (if (speaking) 1f else 0.35f)
                dot.alpha = (255 * a * fade).toInt().coerceIn(0, 255)
                canvas.drawCircle(x, y, density * (if (speaking) 3f else 2f), dot)
            }
            postInvalidateOnAnimation()
        }
    }
}
