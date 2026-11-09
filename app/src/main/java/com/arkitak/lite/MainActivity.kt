package com.arkitak.lite

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import android.animation.ValueAnimator
import android.view.animation.OvershootInterpolator
import android.graphics.drawable.GradientDrawable
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * One black screen with the orb. Tap the orb to talk or stop, long-press it for settings.
 * First launch asks for the Gemini API key. "Floating avatar" keeps a small orb on top of other apps.
 */
class MainActivity : Activity() {

    private lateinit var orb: OrbView
    private lateinit var root: FrameLayout
    @Volatile private var duo: DuoOrbView? = null
    private lateinit var engine: AssistantEngine
    private lateinit var prefs: Prefs
    private var dialog: AlertDialog? = null
    private var afterMic: (() -> Unit)? = null
    private lateinit var scaleDetector: ScaleGestureDetector
    private var pinching = false
    private var orbScale = 1f
    private var springAnimator: ValueAnimator? = null

    private val REQ_MIC = 11
    private val REQ_OVERLAY = 12

    private val listener = object : AssistantEngine.Listener {
        override fun onState(state: String) { orb.setState(state); duo?.setState(state) }
        override fun onLevel(level: Float) { orb.pushAudio(level); duo?.pushAudio(level) }
        override fun onPersona(persona: String) { runOnUiThread { syncDuo(); duo?.setActive(persona) } }
        override fun onMessage(message: String) {
            runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show() }
        }
        override fun onFloatingCommand(on: Boolean) {
            if (on) runOnUiThread { enableFloating() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        engine = AssistantEngine.get(this)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 28) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }

        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        // The orb view now covers the whole screen so its light and the small dots spread over the full screen.
        orb = OrbView(this, fullField = true)
        root.addView(orb, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        // Test button (top right): tap it to show / hide the two-orb group animation (Olivia + ARKITAK).
        val density = resources.displayMetrics.density
        val groupBtn = TextView(this).apply {
            text = "Group"
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
            val h = (10 * density).toInt(); val v = (6 * density).toInt()
            setPadding(h * 2, v, h * 2, v)
            background = GradientDrawable().apply {
                setColor(0x33FFFFFF); cornerRadius = 24 * density; setStroke((1 * density).toInt().coerceAtLeast(1), 0x66FFFFFF)
            }
            setOnClickListener { engine.setGroup(!engine.groupMode) }
        }
        root.addView(groupBtn, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END
        ).apply { topMargin = (48 * density).toInt(); rightMargin = (16 * density).toInt() })
        setContentView(root)
        // App opens with the two orbs (Olivia + ARKITAK). The Group button above switches back to one orb.
        // Wait until the single orb has loaded, THEN divide it into two (loading 3 orb pages at the same time on a cold start left the screen empty).
        orb.whenLoaded { mainLoaded = true; if (duo != null || engine.groupMode) orb.hideSphere(true); startGroupOnce() }
        root.postDelayed({ startGroupOnce() }, 6000L) // safety: never stay without the two orbs if the load callback does not come

        // Pinch to make the orb smaller or bigger; let go and it bounces back to its normal size.
        scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                springAnimator?.cancel()
                return true
            }
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                orbScale = (orbScale * detector.scaleFactor).coerceIn(0.4f, 2.4f)
                orb.setOrbScale(orbScale)
                return true
            }
            override fun onScaleEnd(detector: ScaleGestureDetector) { springBack() }
        })

        orb.setTheme(OrbView.THEME_DARK)
        orb.onOrbTap = { onOrbTap() }
        orb.onOrbLongPress = { showMenu() }
        hideSystemBars()
    }

    override fun onStart() {
        super.onStart()
        FloatingOrbService.activityStarted()
    }

    override fun onResume() {
        super.onResume()
        orb.onResume()
        engine.addListener(listener)
        syncDuo()
        duo?.onResume() // the single orb keeps running behind the pair: it draws the dots around
        when {
            prefs.apiKey.isBlank() -> showKeyDialog(firstTime = true)
            !engine.hasMicPermission() -> requestMic()
            !engine.active -> engine.start()
        }
    }

    override fun onPause() {
        engine.removeListener(listener)
        orb.onPause()
        duo?.onPause()
        super.onPause()
    }

    override fun onStop() {
        FloatingOrbService.activityStopped()
        // Mic always on: leaving the app never closes the microphone; the conversation keeps running in the background.
        super.onStop()
    }

    override fun onDestroy() {
        dialog?.dismiss()
        duo?.destroy()
        duo = null
        orb.destroy()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    /** Group chat on (Olivia and ARKITAK together): the orb splits in two with a line between them. Off: they flow back into one. */
    private fun syncDuo() {
        val want = engine.groupMode
        val d = duo
        if (want && d == null) {
            val first = !autoGroupDone || !openedOnce
            openedOnce = true
            val nd = DuoOrbView(this, horizontal = true, stars = false, showLink = false) { p ->
                ParticleOrbHandle(this, if (p == Personas.ARKITAK) OrbView.THEME_DARK else OrbView.THEME_RED)
            }
            nd.onTap = { onOrbTap() }
            nd.onHold = { showMenu() }
            nd.setActive(engine.persona)
            nd.setState(engine.state)
            root.addView(nd, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            duo = nd
            // The big orb stays until both new orbs are ready, then it divides into two.
            // App just opened: the two orbs are simply there. Group button: the big orb divides into two.
            // The single orb stays behind (running) with only its sphere hidden, so the dots around are the orbs' own dots.
            nd.open(instant = first) { if (mainLoaded) orb.hideSphere(true) }
        } else if (!want && d != null) {
            orb.onResume()
            orb.hideSphere(false)
            duo = null
            d.close {
                root.removeView(d)
                d.destroy()
                if (engine.groupMode) syncDuo()
            }
        } else if (want && d != null) {
            d.setActive(engine.persona)
        }
    }

    private var autoGroupDone = false
    private var mainLoaded = false
    private var openedOnce = false
    private fun startGroupOnce() {
        runOnUiThread {
            if (autoGroupDone || isDestroyed) return@runOnUiThread
            autoGroupDone = true
            engine.setGroup(true)
        }
    }

    private fun springBack() {
        springAnimator?.cancel()
        springAnimator = ValueAnimator.ofFloat(orbScale, 1f).apply {
            duration = 500
            interpolator = OvershootInterpolator(1.6f)
            addUpdateListener { orbScale = it.animatedValue as Float; orb.setOrbScale(orbScale) }
            start()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(ev)
        if (ev.pointerCount > 1 || scaleDetector.isInProgress) {
            if (!pinching) {
                pinching = true
                // Cancel whatever the orb had started (drag, tap) so two fingers only resize.
                val cancel = MotionEvent.obtain(ev)
                cancel.action = MotionEvent.ACTION_CANCEL
                super.dispatchTouchEvent(cancel)
                cancel.recycle()
            }
            return true
        }
        if (pinching) {
            if (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL) {
                pinching = false
                springBack()
            }
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    }

    // ---------------------------------------------------------------- taps and menu

    private fun onOrbTap() {
        when {
            prefs.apiKey.isBlank() -> showKeyDialog(firstTime = true)
            !engine.hasMicPermission() -> requestMic()
            else -> engine.toggle()
        }
    }

    private fun showMenu() {
        if (dialog?.isShowing == true) return
        val floatingOn = FloatingOrbService.running
        val items = arrayOf(
            "Gemini settings (key, model, voice, language, mic always on)",
            if (floatingOn) "Floating avatar: ON (tap to turn off)" else "Floating avatar: OFF (tap to turn on)",
            "Choose avatar orb (glass designs)",
            "Voice: ${Personas.name(engine.persona)} (tap to switch to ${Personas.name(Personas.other(engine.persona))})",
            "Default assistant (for the swipe-up gesture)",
            if (engine.groupMode) "Group chat: ON (tap to turn off)" else "Group chat: talk to both (tap to turn on)",
            "Let Olivia and ARKITAK talk to each other"
        )
        dialog = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("ARKITAK Lite")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, GeminiSettingsActivity::class.java))
                    1 -> if (FloatingOrbService.running) FloatingOrbService.stop(this) else enableFloating()
                    2 -> enableFloating()
                    3 -> engine.switchPersona(Personas.other(engine.persona))
                    4 -> try { startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) } catch (_: Exception) {
                        Toast.makeText(this, "This phone has no screen for that setting.", Toast.LENGTH_LONG).show()
                    }
                    5 -> engine.setGroup(!engine.groupMode)
                    6 -> engine.startBanter()
                }
            }
            .show()
    }

    private fun showKeyDialog(firstTime: Boolean) {
        if (dialog?.isShowing == true) return
        val input = EditText(this).apply {
            hint = "AIza..."
            setText(prefs.apiKey)
            setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        val hint = TextView(this).apply {
            text = "Get a free key at aistudio.google.com/apikey. It is saved only on this phone and sent only to Google's Gemini API."
            setTextColor(Color.LTGRAY)
            textSize = 13f
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(hint)
            addView(input)
        }
        dialog = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(if (firstTime) "Welcome. Add your Gemini API key" else "Gemini API key")
            .setView(box)
            .setCancelable(!firstTime)
            .setPositiveButton("Save") { _, _ ->
                prefs.apiKey = input.text.toString()
                if (!firstTime) engine.restart()
                if (prefs.apiKey.isNotBlank()) {
                    if (engine.hasMicPermission()) engine.start() else requestMic()
                }
            }
            .apply { if (!firstTime) setNegativeButton("Cancel", null) }
            .show()
    }

    // ---------------------------------------------------------------- permissions

    private fun requestMic() {
        val perms = if (Build.VERSION.SDK_INT >= 33)
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        else arrayOf(Manifest.permission.RECORD_AUDIO)
        requestPermissions(perms, REQ_MIC)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_MIC) return
        if (engine.hasMicPermission()) {
            val next = afterMic
            afterMic = null
            if (next != null) next() else if (prefs.apiKey.isNotBlank()) engine.start()
        } else {
            afterMic = null
            Toast.makeText(this, "Microphone permission is needed to talk", Toast.LENGTH_LONG).show()
        }
    }

    // ---------------------------------------------------------------- floating avatar

    /** Mic permission first, then "display over other apps" (asked once), then start the floating orb. */
    private fun enableFloating() {
        if (!engine.hasMicPermission()) {
            afterMic = { enableFloating() }
            requestMic()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            if (dialog?.isShowing == true) return
            dialog = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Allow floating avatar")
                .setMessage(
                    "To keep the small orb on top of your other apps, Android needs your permission to " +
                    "\"Display over other apps\". On the next screen, turn it on, then come back."
                )
                .setPositiveButton("Open settings") { _, _ ->
                    startActivityForResult(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                        REQ_OVERLAY
                    )
                }
                .setNegativeButton("Not now", null)
                .show()
            return
        }
        openAvatarPicker()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) openAvatarPicker()
            else Toast.makeText(this, "Permission not granted, floating avatar is off", Toast.LENGTH_LONG).show()
        }
    }

    /** Permission is allowed: open the new screen with all the glass orb designs. Choosing one starts the floating avatar. */
    private fun openAvatarPicker() {
        startActivity(Intent(this, AvatarPickerActivity::class.java))
    }
}
