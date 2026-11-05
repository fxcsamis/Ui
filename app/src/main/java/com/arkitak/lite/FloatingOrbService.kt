package com.arkitak.lite

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.max

/**
 * Small draggable orb that stays on top of other apps.
 * Tap = a reply popup slides out from the side of the orb (tap again and it slides back in), long-press = open the app,
 * drag = move. It hides itself while the app is on screen.
 */
class FloatingOrbService : Service() {

    companion object {
        @Volatile var running = false
        @Volatile var appVisible = false
        @Volatile private var visibleCount = 0

        /** Called from every ARKITAK screen's onStart/onStop, so the floating orb hides while any of them is open. */
        fun activityStarted() { visibleCount++; appVisible = true; refreshVisibility() }
        fun activityStopped() { visibleCount = maxOf(0, visibleCount - 1); appVisible = visibleCount > 0; refreshVisibility() }
        fun refreshDesign() { instance?.main?.post { instance?.applyDesign() } }
        @Volatile private var instance: FloatingOrbService? = null

        private const val ACTION_STOP = "com.arkitak.lite.STOP_FLOATING"
        private const val CHANNEL_ID = "floating_orb"
        private const val NOTIF_ID = 7

        fun start(context: Context) {
            context.startForegroundService(Intent(context, FloatingOrbService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingOrbService::class.java))
        }

        fun refreshVisibility() { instance?.applyVisibility() }
    }

    private val main = Handler(Looper.getMainLooper())
    private lateinit var engine: AssistantEngine
    private var windowManager: WindowManager? = null
    private var container: FrameLayout? = null
    private var orb: GlassOrbView? = null
    private lateinit var params: WindowManager.LayoutParams

    // reply popup (slides out of the orb)
    private var orbBox: FrameLayout? = null
    private var holder: FrameLayout? = null
    private var panel: LinearLayout? = null
    private var panelTitle: TextView? = null
    private var panelText: TextView? = null
    private var panelScroll: ScrollView? = null
    private var expanded = false
    private var collapsing = false
    private var panelOnLeft = false
    private var orbSize = 0
    private var panelW = 0
    private var panelH = 0
    private var overlap = 0
    private var orbInWinX = 0
    private var orbInWinY = 0

    private val listener = object : AssistantEngine.Listener {
        override fun onState(state: String) { orb?.setState(state) }
        override fun onLevel(level: Float) { orb?.pushAudio(level) }
        override fun onMessage(message: String) {
            main.post { if (!appVisible) Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
        }
        override fun onReply() { main.post { renderReplies() } }
        override fun onPersona(persona: String) { main.post { renderHeader(); orb?.pulse(Personas.color(persona)) } }
        override fun onFloatingCommand(on: Boolean) {
            if (!on) main.postDelayed({ stopSelf() }, 2500) // let the spoken confirmation finish first
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        engine = AssistantEngine.get(this)
        startInForeground()
        addOverlay()
        running = true
        instance = this
        engine.addListener(listener)
        renderHeader()
        renderReplies()
        applyVisibility()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        instance = null
        engine.removeListener(listener)
        try { container?.let { windowManager?.removeView(it) } } catch (_: Exception) {}
        orb?.destroy()
        container = null
        orb = null
        orbBox = null; holder = null; panel = null; panelTitle = null; panelText = null; panelScroll = null
        super.onDestroy()
    }

    private fun startInForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Floating avatar", NotificationManager.IMPORTANCE_LOW))

        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, FloatingOrbService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val turnOff = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel), "Turn off", stopIntent
        ).build()

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("ARKITAK floating avatar is on")
            .setContentText("Tap the orb to see replies. Say \"floating avatar off\" to hide it.")
            .setContentIntent(openApp)
            .addAction(turnOff)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun addOverlay() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm
        val dm = resources.displayMetrics
        val density = dm.density
        val size = (120 * density).toInt()
        orbSize = size
        overlap = (14 * density).toInt()
        panelW = minOf((250 * density).toInt(), dm.widthPixels - size - (8 * density).toInt())
        panelH = (180 * density).toInt()

        val orbView = GlassOrbView(applicationContext, picker = false, design = Prefs(this).avatarDesign)
        orbView.setState(engine.state)
        orb = orbView

        // Transparent layer on top of the WebView so dragging and tapping belong to the overlay, not the 3D sphere.
        val touchLayer = View(applicationContext)
        val ob = FrameLayout(applicationContext)
        ob.addView(orbView, FrameLayout.LayoutParams(size, size))
        ob.addView(touchLayer, FrameLayout.LayoutParams(size, size))
        orbBox = ob

        // The reply card. Its holder clips it, so when it slides it looks like it comes out of the orb itself.
        val title = TextView(applicationContext).apply { textSize = 13f; setPadding(0, 0, 0, (6 * density).toInt()) }
        val text = TextView(applicationContext).apply {
            textSize = 14f
            setTextColor(Color.WHITE)
            setLineSpacing(0f, 1.12f)
        }
        val scroll = ScrollView(applicationContext).apply {
            isVerticalScrollBarEnabled = false
            addView(text)
        }
        val card = LinearLayout(applicationContext).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (14 * density).toInt()
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                setColor(0xF0101018.toInt())
                cornerRadius = 22 * density
                setStroke((1 * density).toInt().coerceAtLeast(1), 0x33FFFFFF)
            }
            addView(title)
            addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        panel = card; panelTitle = title; panelText = text; panelScroll = scroll
        val hold = FrameLayout(applicationContext)
        hold.addView(card, FrameLayout.LayoutParams(panelW, panelH))
        hold.visibility = View.GONE
        holder = hold

        val box = FrameLayout(applicationContext)
        box.addView(hold, FrameLayout.LayoutParams(panelW, panelH, Gravity.START or Gravity.CENTER_VERTICAL))
        box.addView(ob, FrameLayout.LayoutParams(size, size, Gravity.TOP or Gravity.START)) // the orb is drawn on top
        container = box

        params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dm.widthPixels - size - (12 * density).toInt()
            y = dm.heightPixels / 3
        }

        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        var longPressed = false
        val slop = 10 * density
        val longPress = Runnable {
            longPressed = true
            val open = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try { startActivity(open) } catch (_: Exception) {}
        }

        touchLayer.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startX = params.x; startY = params.y
                    moved = false; longPressed = false
                    main.postDelayed(longPress, 700)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!moved && (abs(dx) > slop || abs(dy) > slop)) {
                        moved = true
                        main.removeCallbacks(longPress)
                    }
                    if (moved) {
                        params.x = (startX + dx).toInt().coerceIn(0, max(0, dm.widthPixels - params.width))
                        params.y = (startY + dy).toInt().coerceIn(0, max(0, dm.heightPixels - params.height))
                        try { wm.updateViewLayout(box, params) } catch (_: Exception) {}
                    }
                }
                MotionEvent.ACTION_UP -> {
                    main.removeCallbacks(longPress)
                    if (!moved && !longPressed) { if (expanded) collapse() else expand() }
                }
                MotionEvent.ACTION_CANCEL -> main.removeCallbacks(longPress)
            }
            true
        }

        wm.addView(box, params)
    }

    // ---------------------------------------------------------------- reply popup

    /** The popup slides out from the side of the orb that has room (the left if the orb is on the right half of the screen). */
    private fun expand() {
        val box = container ?: return
        val wm = windowManager ?: return
        val hold = holder ?: return
        val card = panel ?: return
        val ob = orbBox ?: return
        if (expanded || collapsing) return
        expanded = true
        val dm = resources.displayMetrics
        panelOnLeft = params.x + orbSize / 2 > dm.widthPixels / 2
        val winW = orbSize + panelW - overlap
        val winH = max(orbSize, panelH)
        orbInWinX = if (panelOnLeft) panelW - overlap else 0
        orbInWinY = (winH - orbSize) / 2
        val winX = (params.x - orbInWinX).coerceIn(0, max(0, dm.widthPixels - winW))
        val winY = (params.y - orbInWinY).coerceIn(0, max(0, dm.heightPixels - winH))
        (ob.layoutParams as FrameLayout.LayoutParams).apply { gravity = Gravity.START or Gravity.CENTER_VERTICAL; leftMargin = orbInWinX; topMargin = 0 }
        (hold.layoutParams as FrameLayout.LayoutParams).apply { gravity = Gravity.START or Gravity.CENTER_VERTICAL; leftMargin = if (panelOnLeft) 0 else orbSize - overlap }
        params.width = winW; params.height = winH; params.x = winX; params.y = winY
        ob.requestLayout(); hold.requestLayout()
        try { wm.updateViewLayout(box, params) } catch (_: Exception) {}
        renderHeader()
        renderReplies()
        val hidden = if (panelOnLeft) panelW.toFloat() else -panelW.toFloat()
        card.animate().cancel()
        card.translationX = hidden
        card.alpha = 0.4f
        hold.visibility = View.VISIBLE
        card.animate().translationX(0f).alpha(1f).setDuration(340).setInterpolator(DecelerateInterpolator(1.6f)).start()
        ob.animate().cancel()
        ob.scaleX = 0.94f; ob.scaleY = 0.94f
        ob.animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(OvershootInterpolator(2f)).start()
    }

    private fun collapse() {
        val card = panel ?: return
        if (!expanded || collapsing) return
        collapsing = true
        val hidden = if (panelOnLeft) panelW.toFloat() else -panelW.toFloat()
        card.animate().cancel()
        card.animate().translationX(hidden).alpha(0.4f).setDuration(240).setInterpolator(AccelerateInterpolator())
            .withEndAction { finishCollapse() }.start()
    }

    private fun finishCollapse() {
        val box = container
        val wm = windowManager
        val hold = holder
        val ob = orbBox
        if (box != null && wm != null && hold != null && ob != null) {
            hold.visibility = View.GONE
            (ob.layoutParams as FrameLayout.LayoutParams).apply { gravity = Gravity.TOP or Gravity.START; leftMargin = 0; topMargin = 0 }
            params.x += orbInWinX
            params.y += orbInWinY
            params.width = orbSize
            params.height = orbSize
            ob.requestLayout()
            try { wm.updateViewLayout(box, params) } catch (_: Exception) {}
        }
        expanded = false
        collapsing = false
    }

    private fun renderHeader() {
        val p = engine.persona
        panelTitle?.text = Personas.title(p)
        panelTitle?.setTextColor(Personas.color(p))
    }

    /** Newest reply in white, older ones dimmer; a small coloured name appears when the speaker changes. */
    private fun renderReplies() {
        val text = panelText ?: return
        val list = engine.replyHistory().takeLast(6)
        if (list.isEmpty()) {
            text.setTextColor(0x99FFFFFF.toInt())
            text.text = "Say something, my reply will appear here."
            return
        }
        text.setTextColor(Color.WHITE)
        val sb = SpannableStringBuilder()
        var prev: String? = null
        list.forEachIndexed { i, r ->
            if (i > 0) sb.append("\n\n")
            if (r.persona != prev) {
                val s0 = sb.length
                sb.append(Personas.name(r.persona)).append("\n")
                sb.setSpan(ForegroundColorSpan(Personas.color(r.persona)), s0, sb.length, 0)
                prev = r.persona
            }
            val s1 = sb.length
            sb.append(r.text.trim())
            sb.setSpan(ForegroundColorSpan(if (i == list.lastIndex) Color.WHITE else 0xAAFFFFFF.toInt()), s1, sb.length, 0)
        }
        text.text = sb
        panelScroll?.post { panelScroll?.fullScroll(View.FOCUS_DOWN) }
    }

    private fun applyDesign() { orb?.setDesign(Prefs(this).avatarDesign) }

    private fun applyVisibility() {
        val box = container ?: return
        val hide = appVisible
        box.visibility = if (hide) View.GONE else View.VISIBLE
        if (hide) orb?.onPause() else orb?.onResume()
    }
}
