package com.arkitak.lite

import android.content.Context

/** Small settings store. The API key stays on this phone (private app storage) and is only sent to Google's Gemini API. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("arkitak_lite", Context.MODE_PRIVATE)

    var apiKey: String
        get() = sp.getString("api_key", "") ?: ""
        set(value) { sp.edit().putString("api_key", value.trim()).apply() }

    var model: String
        // Gemini 2.5 models are retired, so a 2.5 name saved by an older version is ignored.
        get() = (sp.getString("model", DEFAULT_MODEL) ?: DEFAULT_MODEL).let { if (it.contains("2.5")) DEFAULT_MODEL else it }
        set(value) { sp.edit().putString("model", value.trim().ifEmpty { DEFAULT_MODEL }).apply() }

    /** "auto", "en" or "bn" */
    var lang: String
        get() = sp.getString("lang", "auto") ?: "auto"
        set(value) { sp.edit().putString("lang", value).apply() }

    /** One of the ids in [OrbDesigns]. */
    var orbDesign: String
        get() = sp.getString("orb_design", "nebula") ?: "nebula"
        set(value) { sp.edit().putString("orb_design", value).apply() }

    /** false = auto on/off like Google Assistant (default), true = mic never switches itself off. */
    var alwaysOn: Boolean
        get() = sp.getBoolean("always_on", false)
        set(value) { sp.edit().putBoolean("always_on", value).apply() }

    companion object {
        const val DEFAULT_MODEL = "gemini-3.6-flash"
    }
}
