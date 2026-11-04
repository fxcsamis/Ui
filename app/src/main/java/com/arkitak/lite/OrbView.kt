package com.arkitak.lite

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

/**
 * Self-contained 3D particle orb (Three.js inside a transparent WebView).
 *
 * Everything the home screen of ARKITAK did with the orb lives here:
 *  - state  : idle / listening / thinking / speaking      -> setState()
 *  - theme  : light / dark / red / sunset / custom colour  -> setTheme()
 *  - audio  : live amplitude/bass/high (0..1) drives pulse -> pushAudio()
 *  - touch  : tap and long-press callbacks, drag still rotates the 3D sphere
 *  - life   : onResume()/onPause() stop the WebGL loop when hidden (saves battery)
 *
 * Calls made before the page finishes loading are remembered and applied on load.
 */
class OrbView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        const val STATE_IDLE = "idle"
        const val STATE_LISTENING = "listening"
        const val STATE_THINKING = "thinking"
        const val STATE_SPEAKING = "speaking"

        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_RED = "red"
        const val THEME_SUNSET = "sunset"
        const val THEME_CUSTOM = "custom"

        private val VALID_STATES = setOf(STATE_IDLE, STATE_LISTENING, STATE_THINKING, STATE_SPEAKING)
        private val VALID_THEMES = setOf(THEME_LIGHT, THEME_DARK, THEME_RED, THEME_SUNSET)
        private val HEX_COLOR = Regex("^#[0-9a-fA-F]{6}$")
        private const val AUDIO_MIN_INTERVAL_MS = 66L // ~15 pushes/second is plenty
    }

    /** Single tap on the orb (e.g. toggle mic). */
    var onOrbTap: (() -> Unit)? = null

    /** Long press on the orb (e.g. push-to-talk). */
    var onOrbLongPress: (() -> Unit)? = null

    private val webView = WebView(context)
    private var pageLoaded = false
    private var pendingState = STATE_IDLE
    private var pendingTheme = THEME_LIGHT
    private var pendingCustomHex: String? = null
    private var lastAudioPush = 0L

    init {
        clipChildren = false
        clipToPadding = false

        val density = resources.displayMetrics.density
        val haloSize = (260 * density).toInt()
        val halo = View(context).apply { setBackgroundResource(R.drawable.bg_orb_halo) }
        addView(halo, LayoutParams(haloSize, haloSize, Gravity.CENTER))

        addView(
            webView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER)
        )
        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun setupWebView() {
        webView.apply {
            setBackgroundColor(Color.TRANSPARENT)
            setLayerType(LAYER_TYPE_HARDWARE, null)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                @Suppress("DEPRECATION")
                allowFileAccessFromFileURLs = true
                @Suppress("DEPRECATION")
                allowUniversalAccessFromFileURLs = true
                cacheMode = WebSettings.LOAD_DEFAULT
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    pageLoaded = true
                    applyTheme()
                    applyState()
                }
            }

            // Tap/long-press are detected without blocking drag-to-rotate.
            val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    onOrbTap?.invoke()
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    onOrbLongPress?.invoke()
                }
            })
            setOnTouchListener { v, event ->
                detector.onTouchEvent(event)
                v.onTouchEvent(event)
            }

            loadUrl("file:///android_asset/orb.html?embedded=1")
        }
    }

    /** One of STATE_IDLE / STATE_LISTENING / STATE_THINKING / STATE_SPEAKING. */
    fun setState(state: String) {
        if (state !in VALID_STATES) return
        pendingState = state
        applyState()
    }

    /**
     * Theme: THEME_LIGHT / THEME_DARK / THEME_RED / THEME_SUNSET,
     * or THEME_CUSTOM with customHex like "#FF8800".
     */
    fun setTheme(theme: String, customHex: String? = null) {
        if (theme == THEME_CUSTOM) {
            if (customHex == null || !HEX_COLOR.matches(customHex)) return
            pendingTheme = THEME_CUSTOM
            pendingCustomHex = customHex
        } else {
            if (theme !in VALID_THEMES) return
            pendingTheme = theme
            pendingCustomHex = null
        }
        applyTheme()
    }

    /**
     * Feed live audio levels, each 0f..1f. Call from the mic loop (user speaking)
     * or from TTS playback (assistant speaking). Safe to call from any thread.
     * Without fresh values for ~220 ms the orb calms down on its own.
     */
    fun pushAudio(amplitude: Float, bass: Float = amplitude, high: Float = amplitude) {
        if (!pageLoaded) return
        val now = System.currentTimeMillis()
        if (now - lastAudioPush < AUDIO_MIN_INTERVAL_MS) return
        lastAudioPush = now
        val a = amplitude.coerceIn(0f, 1f)
        val b = bass.coerceIn(0f, 1f)
        val h = high.coerceIn(0f, 1f)
        post {
            webView.evaluateJavascript(
                "window.JarvisOrb && window.JarvisOrb.setExternalAudio($a, $b, $h);", null
            )
        }
    }

    /** Call from Activity/Fragment onResume(). */
    fun onResume() {
        webView.onResume()
        applyTheme()
        applyState()
    }

    /** Call from Activity/Fragment onPause(). Stops the animation to save battery. */
    fun onPause() {
        webView.onPause()
    }

    /** Call from onDestroy() to free the WebView. */
    fun destroy() {
        onOrbTap = null
        onOrbLongPress = null
        removeView(webView)
        webView.destroy()
    }

    private fun applyState() {
        if (!pageLoaded) return
        post {
            webView.evaluateJavascript(
                "window.JarvisOrb && window.JarvisOrb.setState('$pendingState');", null
            )
        }
    }

    private fun applyTheme() {
        if (!pageLoaded) return
        val js = if (pendingTheme == THEME_CUSTOM && pendingCustomHex != null) {
            "window.JarvisOrb && window.JarvisOrb.setBackground('$pendingCustomHex');"
        } else {
            "window.JarvisOrb && window.JarvisOrb.setTheme('$pendingTheme');"
        }
        post { webView.evaluateJavascript(js, null) }
    }
}
