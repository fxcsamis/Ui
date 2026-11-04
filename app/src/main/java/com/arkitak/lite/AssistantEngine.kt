package com.arkitak.lite

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/**
 * The whole voice loop in one place: listen (SpeechRecognizer) -> think (Gemini) -> speak (TextToSpeech) -> listen again.
 * One instance lives for the whole app process, so the full-screen orb and the floating orb always show the same state.
 * Everything runs on the main thread except the network call.
 */
class AssistantEngine private constructor(private val ctx: Context) {

    interface Listener {
        fun onState(state: String) {}
        fun onLevel(level: Float) {}
        fun onMessage(message: String) {}
        fun onFloatingCommand(on: Boolean) {}
    }

    companion object {
        @Volatile private var instance: AssistantEngine? = null
        fun get(context: Context): AssistantEngine =
            instance ?: synchronized(this) {
                instance ?: AssistantEngine(context.applicationContext).also { instance = it }
            }
        private const val MAX_TURNS = 12
    }

    private val main = Handler(Looper.getMainLooper())
    private val prefs = Prefs(ctx)
    private val listeners = CopyOnWriteArrayList<Listener>()
    private val history = ArrayList<GeminiClient.Turn>()

    var state: String = OrbView.STATE_IDLE
        private set

    /** True while a conversation is running (listening, thinking or speaking). */
    var active: Boolean = false
        private set

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeech: String? = null
    private var token = 0
    private var emptyCount = 0
    private var wordPulse = 0f

    init {
        tts = TextToSpeech(ctx) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) { main.post { onSpeechFinished() } }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { main.post { onSpeechFinished() } }
                    override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) { wordPulse = 1f }
                })
                pendingSpeech?.let { pendingSpeech = null; main.post { speak(it) } }
            }
        }
    }

    // ---------------------------------------------------------------- listeners

    fun addListener(l: Listener) {
        listeners.addIfAbsent(l)
        l.onState(state)
    }

    fun removeListener(l: Listener) { listeners.remove(l) }

    private fun setState(s: String) {
        state = s
        listeners.forEach { it.onState(s) }
    }

    private fun emitLevel(level: Float) { listeners.forEach { it.onLevel(level) } }
    private fun postMessage(message: String) { listeners.forEach { it.onMessage(message) } }

    // ---------------------------------------------------------------- public controls

    fun hasMicPermission(): Boolean =
        ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /** Start a conversation (safe from any thread). */
    fun start() {
        main.post {
            if (prefs.apiKey.isBlank()) { postMessage("Add your Gemini API key first"); return@post }
            if (!hasMicPermission()) { postMessage("Microphone permission is needed"); return@post }
            if (active) return@post
            active = true
            emptyCount = 0
            listen()
        }
    }

    /** End the conversation and go quiet. */
    fun stop() {
        main.post { stopNow() }
    }

    /** Tap behaviour: idle -> start, speaking -> interrupt and listen, otherwise stop. */
    fun toggle() {
        main.post {
            when {
                !active -> start()
                state == OrbView.STATE_SPEAKING -> {
                    token++
                    tts?.stop()
                    stopPulse()
                    emptyCount = 0
                    listen()
                }
                else -> stopNow()
            }
        }
    }

    private fun stopNow() {
        active = false
        token++
        stopPulse()
        try { recognizer?.cancel() } catch (_: Exception) {}
        tts?.stop()
        setState(OrbView.STATE_IDLE)
    }

    // ---------------------------------------------------------------- listening

    private fun langTag(): String? = when (prefs.lang) {
        "en" -> "en-US"
        "bn" -> "bn-BD"
        else -> null
    }

    private fun listen() {
        if (!active) return
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            postMessage("Speech recognition is not available on this phone")
            stopNow()
            return
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(ctx).also { it.setRecognitionListener(recognitionListener) }
        }
        setState(OrbView.STATE_LISTENING)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
            langTag()?.let { putExtra(RecognizerIntent.EXTRA_LANGUAGE, it) }
        }
        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            postMessage("Could not start listening")
            stopNow()
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onRmsChanged(rmsdB: Float) {
            if (state == OrbView.STATE_LISTENING) emitLevel(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
        }

        override fun onResults(results: Bundle?) {
            if (!active || state != OrbView.STATE_LISTENING) return
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
            if (text.isEmpty()) handleEmpty() else onUserText(text)
        }

        override fun onError(error: Int) {
            if (!active || state != OrbView.STATE_LISTENING) return
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> handleEmpty()
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                    main.postDelayed({ if (active && state == OrbView.STATE_LISTENING) listen() }, 500)
                SpeechRecognizer.ERROR_CLIENT -> {}
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> { postMessage("Microphone permission is needed"); stopNow() }
                else -> { postMessage("Speech recognition error ($error)"); stopNow() }
            }
        }
    }

    /** Nothing heard: try once more, then go back to sleep. */
    private fun handleEmpty() {
        emptyCount++
        if (emptyCount >= 2) {
            stopNow()
        } else {
            main.postDelayed({ if (active && state == OrbView.STATE_LISTENING) listen() }, 250)
        }
    }

    // ---------------------------------------------------------------- thinking

    private fun onUserText(text: String) {
        emptyCount = 0
        if (handleLocalCommand(text)) return

        setState(OrbView.STATE_THINKING)
        history.add(GeminiClient.Turn("user", text))
        while (history.size > MAX_TURNS) history.removeAt(0)
        val snapshot = ArrayList(history)
        val myToken = ++token
        val key = prefs.apiKey
        val model = prefs.model

        Thread {
            var reply: String? = null
            var error: String? = null
            try {
                reply = GeminiClient.ask(key, model, snapshot)
            } catch (e: Exception) {
                error = e.message ?: "Network error"
            }
            val answer = reply
            val failure = error
            main.post {
                if (token != myToken || !active) return@post
                if (answer != null) {
                    history.add(GeminiClient.Turn("model", answer))
                    speak(answer)
                } else {
                    if (history.isNotEmpty()) history.removeAt(history.size - 1)
                    postMessage(failure ?: "Something went wrong")
                    stopNow()
                }
            }
        }.start()
    }

    /** "floating avatar on / off" is handled here, without asking Gemini. */
    private fun handleLocalCommand(text: String): Boolean {
        val t = text.lowercase(Locale.ROOT)
        val about = listOf("floating", "ফ্লোটিং", "overlay", "ওভারলে", "avatar", "অ্যাভাটার", "এভাটার", "ভাসমান")
        if (about.none { t.contains(it) }) return false
        val offWords = listOf(" off", "disable", "stop", "remove", "hide", "close", "বন্ধ", "অফ", "bondho", "band")
        val on = offWords.none { t.contains(it) }
        val bangla = text.any { it in 'ঀ'..'৿' }
        listeners.forEach { it.onFloatingCommand(on) }
        val reply = when {
            on && bangla -> "ঠিক আছে, ফ্লোটিং অ্যাভাটার চালু করছি।"
            on -> "Okay, turning on the floating avatar."
            bangla -> "ঠিক আছে, ফ্লোটিং অ্যাভাটার বন্ধ করছি।"
            else -> "Okay, turning off the floating avatar."
        }
        speak(reply)
        return true
    }

    // ---------------------------------------------------------------- speaking

    private fun speak(raw: String) {
        if (!active) return
        if (!ttsReady) {
            pendingSpeech = raw
            setState(OrbView.STATE_SPEAKING)
            return
        }
        val clean = raw.replace(Regex("[*#_`~>]"), "").trim().take(3000)
        if (clean.isEmpty()) { onSpeechFinished(); return }

        val hasBangla = clean.any { it in 'ঀ'..'৿' }
        val locale = when (prefs.lang) {
            "en" -> Locale.US
            "bn" -> Locale("bn", "BD")
            else -> if (hasBangla) Locale("bn", "BD") else Locale.getDefault()
        }
        tts?.setLanguage(locale)
        setState(OrbView.STATE_SPEAKING)
        startPulse()
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "arkitak")
    }

    private fun onSpeechFinished() {
        stopPulse()
        if (active && state == OrbView.STATE_SPEAKING) {
            emptyCount = 0
            listen()
        }
    }

    /** TTS does not expose its audio level, so the orb pulses with the words (word boundaries) over a soft base wave. */
    private val pulseTick = object : Runnable {
        override fun run() {
            if (state != OrbView.STATE_SPEAKING) return
            val base = 0.28f + 0.2f * abs(sin(System.currentTimeMillis() / 90.0)).toFloat()
            emitLevel(max(base, wordPulse * 0.85f))
            wordPulse *= 0.7f
            main.postDelayed(this, 70)
        }
    }

    private fun startPulse() { main.removeCallbacks(pulseTick); main.post(pulseTick) }
    private fun stopPulse() { main.removeCallbacks(pulseTick); wordPulse = 0f }
}
