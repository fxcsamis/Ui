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
import android.graphics.RectF
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
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
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
    /** Runs the callback when the orb page has loaded (at once if it already has). */
    fun whenLoaded(cb: () -> Unit)
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
    override fun whenLoaded(cb: () -> Unit) = orb.whenLoaded(cb)
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
    override fun whenLoaded(cb: () -> Unit) = orb.whenLoaded(cb)
}

/**
 * Olivia and ARKITAK talking to each other (group chat): one orb splits into two and a living connection grows between them.
 *  - horizontal = true (main screen and assistant-gesture screen): ARKITAK (blue) on the left, Olivia (pink) on the right, and between
 *    them neon sound-wave bars with a microphone in the middle, on a field of stars. The speaker's side moves with its voice.
 *  - horizontal = false (small floating avatar): Olivia above, ARKITAK below, a line with travelling lights between them.
 * Calls to [setState], [setActive] and [pushAudio] are safe from any thread. [open] and [close] run on the main thread.
 */
class DuoOrbView(
    context: Context,
    private val horizontal: Boolean = false,
    private val stars: Boolean = false,
    private val showLink: Boolean = true,
    make: (String) -> OrbHandle
) : FrameLayout(context) {

    /** Tap and long-press on the pair (the whole view takes the touches). */
    var onTap: (() -> Unit)? = null
    var onHold: (() -> Unit)? = null

    // a = Olivia, b = ARKITAK
    private val orbOlivia: OrbHandle = make(Personas.OLIVIA)
    private val orbArkitak: OrbHandle = make(Personas.ARKITAK)
    private val link = LinkView(context)
    private val touch = View(context)

    @Volatile private var active = Personas.OLIVIA
    @Volatile private var state = OrbView.STATE_IDLE
    @Volatile private var level = 0f
    private var lastActive: String? = null
    private var split = 0f
    private var side = 0
    private var half = 0f // half the distance between the two orb centres when fully apart
    private var anim: ValueAnimator? = null
    private var started = false      // false while the two orb pages are still loading: nothing is shown yet
    private var cancelled = false
    private var destroyed = false

    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean { onTap?.invoke(); return true }
        override fun onLongPress(e: MotionEvent) { onHold?.invoke() }
    })

    init {
        clipChildren = false
        clipToPadding = false
        addView(link, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(orbOlivia.view, LayoutParams(1, 1, Gravity.CENTER))
        addView(orbArkitak.view, LayoutParams(1, 1, Gravity.CENTER))
        addView(touch, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        touch.setOnTouchListener { _, e -> gestures.onTouchEvent(e); true }
        orbOlivia.onResume() // a new WebView only draws after it was resumed
        orbArkitak.onResume()
        applySplit(0f)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (horizontal) {
            side = min(w * 0.40f, h * 0.9f).toInt().coerceAtLeast(1)
            half = w * 0.28f
        } else {
            side = min(w.toFloat(), h / 2.24f).toInt().coerceAtLeast(1)
            half = side * 0.62f
        }
        resize(orbOlivia.view)
        resize(orbArkitak.view)
        applySplit(split)
    }

    private fun resize(v: View) {
        val lp = v.layoutParams as LayoutParams
        lp.width = side
        lp.height = side
        v.layoutParams = lp
    }

    /** p = 0: both orbs on top of each other in the middle (hidden). p = 1: apart, connection between them. */
    private fun applySplit(p: Float) {
        split = p
        val d = half * p
        if (horizontal) { // ARKITAK on the left, Olivia on the right
            orbOlivia.view.translationX = d
            orbArkitak.view.translationX = -d
        } else { // Olivia above, ARKITAK below
            orbOlivia.view.translationY = -d
            orbArkitak.view.translationY = d
        }
        val a = if (started) (p * 6f).coerceIn(0f, 1f) else 0f
        val sc = 1.9f - 0.9f * p // the single big orb divides into two smaller ones
        for (v in arrayOf(orbOlivia.view, orbArkitak.view)) {
            v.alpha = a
            v.scaleX = sc
            v.scaleY = sc
        }
        link.set(p, d, side)
    }

    // ---------------------------------------------------------------- animation

    /**
     * Waits until both orb pages have loaded (so nothing pops in half-drawn), then calls [onVisible] (the caller fades out its single
     * orb there) and lets the big orb divide into two that glide apart. Gives up waiting after 7 s and shows them anyway.
     */
    fun open(instant: Boolean = false, onVisible: () -> Unit = {}) {
        var ready = 0
        val go = Runnable {
            if (cancelled || destroyed || started) return@Runnable
            started = true
            applySplit(0f)
            onVisible()
            if (instant) applySplit(1f) else animateTo(1f, 900L, OvershootInterpolator(0.6f)) {}
        }
        val oneLoaded: () -> Unit = {
            ready++
            if (ready == 2) postDelayed(go, 200L) // a short moment so that the first frame of both orbs is drawn
        }
        orbOlivia.whenLoaded(oneLoaded)
        orbArkitak.whenLoaded(oneLoaded)
        postDelayed(go, 7000L)
    }

    /** The two orbs flow back together, then [done] is called (remove and destroy the view there). */
    fun close(done: () -> Unit) {
        if (!started) { cancelled = true; done(); return }
        animateTo(0f, 600L, DecelerateInterpolator()) { done() }
    }

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
        if (changed) (if (p == Personas.OLIVIA) orbOlivia else orbArkitak).pulse(Personas.color(p))
    }

    fun setState(s: String) {
        state = s
        applyStates()
    }

    /** The one who speaks moves; the other one listens. When you speak, both listen. */
    private fun applyStates() {
        val act = if (active == Personas.OLIVIA) orbOlivia else orbArkitak
        val oth = if (active == Personas.OLIVIA) orbArkitak else orbOlivia
        when (state) {
            OrbView.STATE_SPEAKING -> { act.setState(OrbView.STATE_SPEAKING); oth.setState(OrbView.STATE_LISTENING) }
            OrbView.STATE_THINKING -> { act.setState(OrbView.STATE_THINKING); oth.setState(OrbView.STATE_IDLE) }
            OrbView.STATE_LISTENING -> { act.setState(OrbView.STATE_LISTENING); oth.setState(OrbView.STATE_LISTENING) }
            else -> { act.setState(OrbView.STATE_IDLE); oth.setState(OrbView.STATE_IDLE) }
        }
    }

    fun pushAudio(l: Float) {
        level = max(level, l.coerceIn(0f, 1f))
        val act = if (active == Personas.OLIVIA) orbOlivia else orbArkitak
        val oth = if (active == Personas.OLIVIA) orbArkitak else orbOlivia
        when (state) {
            OrbView.STATE_SPEAKING -> act.pushAudio(l)
            OrbView.STATE_LISTENING -> { act.pushAudio(l); oth.pushAudio(l) }
            else -> {}
        }
    }

    fun onResume() { orbOlivia.onResume(); orbArkitak.onResume(); applyStates() }
    fun onPause() { orbOlivia.onPause(); orbArkitak.onPause() }

    fun destroy() {
        destroyed = true
        anim?.cancel()
        onTap = null
        onHold = null
        orbOlivia.destroy()
        orbArkitak.destroy()
    }

    // ---------------------------------------------------------------- the connection and the stars (drawn behind the orbs)

    private inner class LinkView(c: Context) : View(c) {
        private var progress = 0f
        private var d = 0f
        private var side = 0
        private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val core = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.WHITE }
        private val path = Path()
        private val micRect = RectF()
        private var shaderKey = ""

        // stars (only when [stars] is true)
        private val starX = FloatArray(90)
        private val starY = FloatArray(90)
        private val starR = FloatArray(90)
        private val starPhase = FloatArray(90)
        private val starColor = IntArray(90)

        init {
            val rnd = Random(7)
            val palette = intArrayOf(0xFF4A8CFF.toInt(), 0xFF9B6BFF.toInt(), 0xFF35D0FF.toInt(), 0xFFFF6BD6.toInt())
            for (i in 0 until 90) {
                starX[i] = rnd.nextFloat()
                starY[i] = rnd.nextFloat()
                starR[i] = 0.5f + rnd.nextFloat() * 1.6f
                starPhase[i] = rnd.nextFloat() * 6.28f
                starColor[i] = palette[rnd.nextInt(palette.size)]
            }
        }

        fun set(p: Float, distance: Float, s: Int) {
            progress = p
            d = distance
            side = s
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            if (!showLink) return // no microphone, no bars, no separate stars: only the two orbs themselves
            val a = ((progress - 0.5f) * 2f).coerceIn(0f, 1f)
            if (a <= 0f || side == 0) return
            val density = resources.displayMetrics.density
            val now = SystemClock.uptimeMillis()
            val lvl = level
            level = lvl * 0.92f
            if (stars) drawStars(canvas, now, a, density)
            if (horizontal) drawWaves(canvas, now, a, density, lvl) else drawLine(canvas, now, a, density, lvl)
            postInvalidateOnAnimation()
        }

        private fun drawStars(canvas: Canvas, now: Long, a: Float, density: Float) {
            val w = width.toFloat()
            val h = height.toFloat()
            for (i in 0 until 90) {
                val tw = 0.35f + 0.65f * (0.5f + 0.5f * sin(now * 0.0015f + starPhase[i]))
                dot.color = starColor[i]
                dot.alpha = (255 * a * tw).toInt().coerceIn(0, 255)
                canvas.drawCircle(starX[i] * w, starY[i] * h, starR[i] * density, dot)
            }
            dot.color = Color.WHITE
        }

        /** Left: ARKITAK (blue) bars. Middle: microphone. Right: Olivia (pink) bars. The speaking side's bars dance with its voice. */
        private fun drawWaves(canvas: Canvas, now: Long, a: Float, density: Float, lvl: Float) {
            val cx = width / 2f
            val cy = height / 2f
            val r = side * 0.425f
            val g = 2f * d - 2f * r // free space between the two spheres
            if (g < 20f) return
            val blue = Personas.color(Personas.ARKITAK)
            val pink = Personas.color(Personas.OLIVIA)
            val micW = g * 0.26f
            val micH = min(g * 0.46f, side * 0.34f)

            // bars
            val profile = floatArrayOf(0.40f, 0.65f, 1.0f, 0.65f, 0.40f)
            val spaceFrom = micW / 2f + g * 0.05f
            val spaceTo = g / 2f - g * 0.06f
            val spacing = (spaceTo - spaceFrom) / 4f
            val speaking = state == OrbView.STATE_SPEAKING
            val thinking = state == OrbView.STATE_THINKING
            val userTalking = state == OrbView.STATE_LISTENING
            val arkitakSpeaks = active == Personas.ARKITAK
            for (sideSign in intArrayOf(-1, 1)) {
                val isArkitak = sideSign < 0
                val energy = when {
                    speaking -> if (isArkitak == arkitakSpeaks) 0.30f + 0.70f * lvl.coerceIn(0f, 1f) else 0.12f
                    userTalking -> 0.15f + 0.85f * lvl.coerceIn(0f, 1f)
                    thinking -> 0.18f + 0.10f * (0.5f + 0.5f * sin(now * 0.004f))
                    else -> 0.10f
                }
                val col = if (isArkitak) blue else pink
                for (i in 0 until 5) {
                    // i = 0 is the bar next to the microphone, i = 4 the outer one (outer bars are the shorter ones)
                    val x = cx + sideSign * (spaceFrom + i * spacing)
                    val wob = 0.55f + 0.45f * abs(sin(now * 0.008f + i * 0.9f + (if (isArkitak) 0f else 1.7f)))
                    val hh = (micH * 0.95f * profile[4 - i] * (0.18f + 0.82f * energy) * wob).coerceAtLeast(density * 3f)
                    glow.color = col
                    glow.alpha = (70 * a).toInt()
                    glow.strokeWidth = 7f * density
                    canvas.drawLine(x, cy - hh / 2f, x, cy + hh / 2f, glow)
                    core.shader = null
                    core.color = col
                    core.alpha = (255 * a).toInt()
                    core.strokeWidth = 2.4f * density
                    canvas.drawLine(x, cy - hh / 2f, x, cy + hh / 2f, core)
                }
            }

            // microphone
            val key = "$cx/$micW"
            val sh = LinearGradient(cx - micW / 2f, cy, cx + micW / 2f, cy, blue, pink, Shader.TileMode.CLAMP)
            if (key != shaderKey) shaderKey = key
            for (pass in 0..1) {
                val p = if (pass == 0) glow else core
                p.shader = sh
                p.strokeWidth = (if (pass == 0) 7f else 2.4f) * density
                p.alpha = ((if (pass == 0) 70 else 255) * a).toInt()
                val capW = micH * 0.34f
                val capH = micH * 0.52f
                val top = cy - micH * 0.5f
                micRect.set(cx - capW / 2f, top, cx + capW / 2f, top + capH)
                canvas.drawRoundRect(micRect, capW / 2f, capW / 2f, p)
                micRect.set(cx - capW * 0.85f, top + capH * 0.30f, cx + capW * 0.85f, top + capH * 0.30f + capW * 1.7f)
                canvas.drawArc(micRect, 0f, 180f, false, p)
                val stemTop = micRect.bottom
                val bottom = cy + micH * 0.5f
                canvas.drawLine(cx, stemTop, cx, bottom, p)
                canvas.drawLine(cx - capW * 0.55f, bottom, cx + capW * 0.55f, bottom, p)
            }
            glow.shader = null
            core.shader = null
        }

        /** Small floating avatar (vertical): a line with travelling lights from the speaker to the listener. */
        private fun drawLine(canvas: Canvas, now: Long, a: Float, density: Float, lvl: Float) {
            val cx = width / 2f
            val cy = height / 2f
            val r = side * 0.30f
            val y0 = cy - d + r
            val y1 = cy + d - r
            if (y1 - y0 < 4f) return
            val speaking = state == OrbView.STATE_SPEAKING

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
        }
    }
}
