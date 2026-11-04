package com.arkitak.lite

import android.content.Context

/** Small settings store. The API key stays on this phone (private app storage) and is only sent to Google's Gemini API. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("arkitak_lite", Context.MODE_PRIVATE)

    var apiKey: String
        get() = sp.getString("api_key", "") ?: ""
        set(value) { sp.edit().putString("api_key", value.trim()).apply() }

    var model: String
        get() = sp.getString("model", DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) { sp.edit().putString("model", value.trim().ifEmpty { DEFAULT_MODEL }).apply() }

    /** "auto", "en" or "bn" */
    var lang: String
        get() = sp.getString("lang", "auto") ?: "auto"
        set(value) { sp.edit().putString("lang", value).apply() }

    /** false = auto on/off like Google Assistant (default), true = mic never switches itself off. */
    var alwaysOn: Boolean
        get() = sp.getBoolean("always_on", false)
        set(value) { sp.edit().putBoolean("always_on", value).apply() }

    companion object {
        const val DEFAULT_MODEL = "gemini-2.5-flash"
    }
}
