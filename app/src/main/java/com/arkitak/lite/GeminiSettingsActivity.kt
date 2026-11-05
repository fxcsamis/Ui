package com.arkitak.lite

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/**
 * Gemini settings: API key, Live model, voice, language and "Mic always on", plus a real "Test connection" button
 * that opens a Gemini Live session with the values typed here (even before saving) and says exactly what happened.
 */
class GeminiSettingsActivity : Activity() {

    private val langCodes = listOf("auto", "en", "bn")
    private val langNames = listOf("Auto (same as I speak)", "English", "বাংলা")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        val dp = resources.displayMetrics.density
        fun px(v: Int) = (v * dp).toInt()

        fun label(text: String) = TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor("#9BA6D8"))
            textSize = 13f
            setPadding(0, px(16), 0, px(4))
        }

        fun styled(e: EditText) = e.apply {
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setSingleLine()
        }

        val title = TextView(this).apply {
            text = "Gemini settings"
            setTextColor(Color.WHITE)
            textSize = 22f
        }

        val key = styled(EditText(this)).apply {
            hint = "AIza..."
            setText(prefs.apiKey)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val keyHint = TextView(this).apply {
            text = "Free key at aistudio.google.com/apikey. Saved only on this phone, sent only to Google."
            setTextColor(Color.GRAY)
            textSize = 12f
        }

        val model = styled(EditText(this)).apply {
            hint = Prefs.DEFAULT_LIVE_MODEL
            setText(prefs.liveModel)
        }
        val modelHint = TextView(this).apply {
            text = "Suggested: " + Prefs.LIVE_MODELS.joinToString(", ") { it.removePrefix("models/") }
            setTextColor(Color.GRAY)
            textSize = 12f
        }

        val voice = Spinner(this).apply {
            adapter = ArrayAdapter(this@GeminiSettingsActivity, android.R.layout.simple_spinner_dropdown_item, Prefs.VOICES)
            setSelection(Prefs.VOICES.indexOf(prefs.voice).coerceAtLeast(0))
        }
        val arkVoice = Spinner(this).apply {
            adapter = ArrayAdapter(this@GeminiSettingsActivity, android.R.layout.simple_spinner_dropdown_item, Prefs.VOICES)
            setSelection(Prefs.VOICES.indexOf(prefs.arkitakVoice).coerceAtLeast(0))
        }
        val arkModel = styled(EditText(this)).apply {
            hint = "Same as the Live model above"
            setText(prefs.arkitakModel)
        }
        val lang = Spinner(this).apply {
            adapter = ArrayAdapter(this@GeminiSettingsActivity, android.R.layout.simple_spinner_dropdown_item, langNames)
            setSelection(langCodes.indexOf(prefs.lang).coerceAtLeast(0))
        }

        val alwaysHint = TextView(this).apply {
            text = "Mic always on: the microphone and the live conversation keep running when the app is closed or the screen is off (a small \"listening\" notification is shown)."
            setPadding(0, px(16), 0, 0)
            setTextColor(Color.GRAY)
            textSize = 12f
        }

        val result = TextView(this).apply {
            setTextColor(Color.parseColor("#E9DBFF"))
            textSize = 14f
            setPadding(0, px(16), 0, px(8))
        }

        fun selectedVoice() = Prefs.VOICES[voice.selectedItemPosition.coerceIn(0, Prefs.VOICES.lastIndex)]

        val test = Button(this).apply {
            text = "Test connection"
            setOnClickListener {
                val k = key.text.toString()
                val m = model.text.toString()
                val v = selectedVoice()
                result.text = "Testing... (up to 10 seconds)"
                isEnabled = false
                Thread {
                    val message = GeminiLive.selfTest(k, m, v)
                    runOnUiThread { result.text = message; isEnabled = true }
                }.start()
            }
        }

        val save = Button(this).apply {
            text = "Save"
            setOnClickListener {
                prefs.apiKey = key.text.toString()
                prefs.liveModel = model.text.toString()
                prefs.voice = selectedVoice()
                prefs.arkitakVoice = Prefs.VOICES[arkVoice.selectedItemPosition.coerceIn(0, Prefs.VOICES.lastIndex)]
                prefs.arkitakModel = arkModel.text.toString()
                prefs.lang = langCodes[lang.selectedItemPosition.coerceIn(0, langCodes.lastIndex)]
                AssistantEngine.get(this@GeminiSettingsActivity).restart() // apply right away if a conversation is running
                Toast.makeText(this@GeminiSettingsActivity, "Saved", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(20), px(40), px(20), px(24))
            gravity = Gravity.TOP
            addView(title)
            addView(label("Gemini API key")); addView(key); addView(keyHint)
            addView(label("Live model")); addView(model); addView(modelHint)
            addView(label("Olivia voice (The Lover)")); addView(voice)
            addView(label("ARKITAK voice (The Monster: deep, warm)")); addView(arkVoice)
            addView(label("ARKITAK model (optional)")); addView(arkModel)
            addView(label("Language")); addView(lang)
            addView(alwaysHint)
            addView(result)
            addView(test)
            addView(save)
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addView(box)
        })
    }
}
