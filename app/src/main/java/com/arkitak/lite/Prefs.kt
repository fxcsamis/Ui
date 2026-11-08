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

    /** Gemini prebuilt voice name (e.g. Aoede, Kore). New key, so the old default (Puck) does not stick. */
    var voice: String
        get() = sp.getString("voice_v2", DEFAULT_VOICE) ?: DEFAULT_VOICE
        set(value) { sp.edit().putString("voice_v2", value.trim().ifEmpty { DEFAULT_VOICE }).apply() }

    /** ARKITAK, "The Monster": deep male voice, and an optional own model (blank = same as Olivia's). */
    var arkitakVoice: String
        get() = sp.getString("voice_arkitak", DEFAULT_ARKITAK_VOICE) ?: DEFAULT_ARKITAK_VOICE
        set(value) { sp.edit().putString("voice_arkitak", value.trim().ifEmpty { DEFAULT_ARKITAK_VOICE }).apply() }

    var arkitakModel: String
        get() = sp.getString("model_arkitak", "") ?: ""
        set(value) { sp.edit().putString("model_arkitak", value.trim()).apply() }

    /** Which persona answers right now ("olivia" or "arkitak"); remembered between launches. */
    var persona: String
        get() = Personas.valid(sp.getString("persona", Personas.OLIVIA))
        set(value) { sp.edit().putString("persona", Personas.valid(value)).apply() }

    // ---- wake word (see the wake package) ----
    /** "vosk" or "manual": the one engine the user picked (an old saved "dsp" now means "vosk"). */
    var wakeEngine: String
        get() = if (sp.getString("wake_engine", "vosk") == "manual") "manual" else "vosk"
        set(value) { sp.edit().putString("wake_engine", if (value == "manual") "manual" else "vosk").apply() }

    /** Wake mode: the conversation sleeps after silence and the chosen engine listens for the name. */
    var wakeEnabled: Boolean
        get() = sp.getBoolean("wake_enabled", false)
        set(value) { sp.edit().putBoolean("wake_enabled", value).apply() }

    /** Vosk sensitivity: 0 = Kam (RMS 600), 1 = Medium (300), 2 = Zyada (120). */
    var voskSensitivity: Int
        get() = sp.getInt("vosk_sens", 1).coerceIn(0, 2)
        set(value) { sp.edit().putInt("vosk_sens", value.coerceIn(0, 2)).apply() }

    var voskSound: Boolean
        get() = sp.getBoolean("vosk_sound", true)
        set(value) { sp.edit().putBoolean("vosk_sound", value).apply() }

    /** Seconds of silence after which a wake-started conversation ends: 5, 8 or 15. */
    var silenceSeconds: Int
        get() = sp.getInt("silence_s", 8).let { if (it == 5 || it == 15) it else 8 }
        set(value) { sp.edit().putInt("silence_s", value).apply() }

    /** Free text typed in the diagnostics popup (e.g. whether the mic indicator was visible). */
    var micNote: String
        get() = sp.getString("mic_note", "") ?: ""
        set(value) { sp.edit().putString("mic_note", value).apply() }

    fun lastTest(engineId: String): String? = sp.getString("test_$engineId", null)
    fun setLastTest(engineId: String, encoded: String) { sp.edit().putString("test_$engineId", encoded).apply() }

    /** Glass orb used by the floating avatar (id from GlassDesigns). */
    var avatarDesign: String
        get() = GlassDesigns.byId(sp.getString("avatar_design", GlassDesigns.DEFAULT_ID) ?: GlassDesigns.DEFAULT_ID).id
        set(value) { sp.edit().putString("avatar_design", GlassDesigns.byId(value).id).apply() }

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
        const val DEFAULT_VOICE = "Aoede"
        const val DEFAULT_ARKITAK_VOICE = "Charon" // deep, clear, warm
        val LIVE_MODELS = listOf("models/gemini-3.1-flash-live-preview", "models/gemini-3.8-live")
        val VOICES = listOf("Aoede", "Leda", "Zephyr", "Kore", "Sulafat", "Despina", "Achernar", "Callirrhoe", "Puck", "Charon", "Fenrir", "Orus", "Algenib", "Alnilam", "Iapetus", "Umbriel")
    }
}
