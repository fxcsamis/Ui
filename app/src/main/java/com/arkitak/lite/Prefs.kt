package com.arkitak.lite

import android.content.Context

/** Small settings store. The API key stays on this phone (private app storage) and is only sent to Google's Gemini API. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("arkitak_lite", Context.MODE_PRIVATE)

    var apiKey: String
        get() = sp.getString("api_key", "") ?: ""
        set(value) { sp.edit().putString("api_key", value.trim()).apply() }

    /** Gemini Live model. Always stored with the "models/" prefix the Live API wants. */
    var liveModel: String
        get() = sp.getString("live_model", DEFAULT_LIVE_MODEL) ?: DEFAULT_LIVE_MODEL
        set(value) {
            val v = value.trim().ifEmpty { DEFAULT_LIVE_MODEL }
            sp.edit().putString("live_model", if (v.startsWith("models/")) v else "models/$v").apply()
        }

    /** Gemini prebuilt voice name (e.g. Puck, Kore). */
    var voice: String
        get() = sp.getString("voice", DEFAULT_VOICE) ?: DEFAULT_VOICE
        set(value) { sp.edit().putString("voice", value.trim().ifEmpty { DEFAULT_VOICE }).apply() }

    /** "auto", "en" or "bn" */
    var lang: String
        get() = sp.getString("lang", "auto") ?: "auto"
        set(value) { sp.edit().putString("lang", value).apply() }

    /**
     * Mic always on: the microphone and the live session keep running when the app is not on screen
     * (a small "listening" notification is shown, as Android requires), until the user stops it.
     */
    var alwaysOn: Boolean
        get() = true // the microphone is always on; it can no longer be switched off from a setting
        set(@Suppress("UNUSED_PARAMETER") value) {}

    companion object {
        // Same Live models, voices and default voice as the main ARKITAK app (GeminiConstants).
        const val DEFAULT_LIVE_MODEL = "models/gemini-3.1-flash-live-preview"
        const val DEFAULT_VOICE = "Puck"
        val LIVE_MODELS = listOf("models/gemini-3.1-flash-live-preview", "models/gemini-3.8-live")
        val VOICES = listOf("Puck", "Aoede", "Charon", "Kore", "Fenrir", "Leda", "Orus", "Zephyr")
    }
}
