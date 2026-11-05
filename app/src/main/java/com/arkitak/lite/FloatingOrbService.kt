package com.arkitak.lite

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import kotlin.math.abs

/**
 * Small draggable orb that stays on top of other apps.
 * Tap = talk / stop, long-press = open the app, drag = move. It hides itself while the app is on screen.
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

    private val listener = object : AssistantEngine.Listener {
        override fun onState(state: String) { orb?.setState(state) }
        override fun onLevel(level: Float) { orb?.pushAudio(level) }
        override fun onMessage(message: String) {
            main.post { if (!appVisible) Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
        }
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
            .setContentText("Tap the orb to talk. Say \"floating avatar off\" to hide it.")
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
        val density = resources.displayMetrics.density
        val size = (120 * density).toInt()

        val orbView = GlassOrbView(applicationContext, picker = false, design = Prefs(this).avatarDesign)
        orbView.setState(engine.state)
        orb = orbView

        // Transparent layer on top of the WebView so dragging and tapping belong to the overlay, not the 3D sphere.
        val touchLayer = View(applicationContext)
        val box = FrameLayout(applicationContext)
        box.addView(orbView, FrameLayout.LayoutParams(size, size))
        box.addView(touchLayer, FrameLayout.LayoutParams(size, size))
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
            x = resources.displayMetrics.widthPixels - size - (12 * density).toInt()
            y = resources.displayMetrics.heightPixels / 3
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
                        val dm = resources.displayMetrics
                        params.x = (startX + dx).toInt().coerceIn(0, dm.widthPixels - size)
                        params.y = (startY + dy).toInt().coerceIn(0, dm.heightPixels - size)
                        try { wm.updateViewLayout(box, params) } catch (_: Exception) {}
                    }
                }
                MotionEvent.ACTION_UP -> {
                    main.removeCallbacks(longPress)
                    if (!moved && !longPressed) engine.toggle()
                }
                MotionEvent.ACTION_CANCEL -> main.removeCallbacks(longPress)
            }
            true
        }

        wm.addView(box, params)
    }

    private fun applyDesign() { orb?.setDesign(Prefs(this).avatarDesign) }

    private fun applyVisibility() {
        val box = container ?: return
        val hide = appVisible
        box.visibility = if (hide) View.GONE else View.VISIBLE
        if (hide) orb?.onPause() else orb?.onResume()
    }
}
