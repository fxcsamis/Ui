package com.arkitak.lite

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

/**
 * Glass orb with a face (assets/glass_orb.html, Three.js in a transparent WebView).
 * Same state/audio calls as OrbView, plus setDesign(). No halo view, so nothing is ever cut into a square.
 * [picker] = true: simulated voice and an optional auto demo of all states (used by the avatar picker).
 */
class GlassOrbView(
    context: Context,
    private val picker: Boolean = false,
    design: String = GlassDesigns.DEFAULT_ID
) : FrameLayout(context) {

    private val webView = WebView(context)
    private var pageLoaded = false
    private var pendingState = OrbView.STATE_IDLE
    private var pendingDesign = GlassDesigns.byId(design).id
    private var lastAudioPush = 0L

    init {
        addView(webView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.apply {
            setBackgroundColor(Color.TRANSPARENT)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    pageLoaded = true
                    applyDesign()
                    applyState()
                }
            }
            val mode = if (picker) "picker" else "avatar"
            loadUrl("file:///android_asset/glass_orb.html?mode=$mode&design=$pendingDesign&auto=${if (picker) 1 else 0}")
        }
    }

    fun setState(state: String) {
        pendingState = state
        applyState()
    }

    fun setDesign(id: String) {
        pendingDesign = GlassDesigns.byId(id).id // only known ids ever reach the page
        applyDesign()
    }

    /** Picker only: cycle idle, listening, thinking, speaking on its own. */
    fun setAuto(on: Boolean) {
        if (!pageLoaded) return
        post { webView.evaluateJavascript("window.JarvisOrb && window.JarvisOrb.setAuto($on);", null) }
    }

    /** A quick glow-and-swell in [argb], e.g. when the assistant switches persona. */
    fun pulse(argb: Int) {
        if (!pageLoaded) return
        val rgb = argb and 0xFFFFFF
        post { webView.evaluateJavascript("window.JarvisOrb && window.JarvisOrb.pulse($rgb);", null) }
    }

    fun pushAudio(level: Float) {
        if (!pageLoaded) return
        val now = System.currentTimeMillis()
        if (now - lastAudioPush < 40L) return
        lastAudioPush = now
        val a = level.coerceIn(0f, 1f)
        post { webView.evaluateJavascript("window.JarvisOrb && window.JarvisOrb.setExternalAudio($a);", null) }
    }

    fun onResume() { webView.onResume(); applyDesign(); applyState() }
    fun onPause() { webView.onPause() }

    fun destroy() {
        removeView(webView)
        webView.destroy()
    }

    private fun applyState() {
        if (!pageLoaded) return
        post { webView.evaluateJavascript("window.JarvisOrb && window.JarvisOrb.setState('$pendingState');", null) }
    }

    private fun applyDesign() {
        if (!pageLoaded) return
        post { webView.evaluateJavascript("window.JarvisOrb && window.JarvisOrb.setDesign('$pendingDesign');", null) }
    }
}
