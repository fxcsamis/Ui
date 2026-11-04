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
        fun onOrbDesign(id: String) {}
    }

    companion object {
        @Volatile private var instance: AssistantEngine? = null
        fun get(context: Context): AssistantEngine =
            instance ?: synchronized(this) {
                instance ?: AssistantEngine(context.applicationContext).also { instance = it }
            }
        private const val MAX_TURNS = 12
        private const val FOLLOW_UP_WINDOW_MS = 20_000L
        private val NAME_REGEX = Regex("olivia|oliviya|olivea|ওলিভিয়া|অলিভিয়া|অলিভিয়া|ওলিভিয়া", RegexOption.IGNORE_CASE)
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
    private var lastInteraction = 0L

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

    /** Tap: idle -> start, speaking or thinking -> interrupt and listen, listening -> stop (unless the mic is set to always on). */
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
                state == OrbView.STATE_THINKING -> {
                    token++
                    if (history.isNotEmpty()) history.removeAt(history.size - 1)
                    listen()
                }
                else -> if (!prefs.alwaysOn) stopNow() // auto mode: tap while listening = done
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
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> { postMessage("Microphone permission is needed"); stopNow() }
                else -> if (prefs.alwaysOn) {
                    main.postDelayed({ if (active && state == OrbView.STATE_LISTENING) listen() }, 1200)
                } else {
                    postMessage("Speech recognition error ($error)")
                    stopNow()
                }
            }
        }
    }

    /** Nothing heard. Always-on: keep listening. Auto (like Google Assistant): one more try, then the mic goes off. */
    private fun handleEmpty() {
        emptyCount++
        if (!prefs.alwaysOn && emptyCount >= 2) {
            stopNow()
        } else {
            main.postDelayed({ if (active && state == OrbView.STATE_LISTENING) listen() }, 300)
        }
    }

    // ---------------------------------------------------------------- thinking

    private fun onUserText(text: String) {
        emptyCount = 0
        val now = System.currentTimeMillis()
        val heardName = NAME_REGEX.containsMatchIn(text)
        // Always-on mode behaves like a wake word: ignore talk that is not for Olivia unless a chat is going on.
        if (prefs.alwaysOn && !heardName && now - lastInteraction > FOLLOW_UP_WINDOW_MS) {
            main.postDelayed({ if (active && state == OrbView.STATE_LISTENING) listen() }, 200)
            return
        }
        lastInteraction = now
        if (heardName && NAME_REGEX.replace(text, "").trim(' ', ',', '.', '!', '?', '।').isEmpty()) {
            speak(if (text.any { it in '\u0980'..'\u09FF' }) "জি, বলুন।" else "Yes, I'm listening.")
            return
        }
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
                reply = GeminiClient.askWithFallback(key, model, snapshot) { working -> prefs.model = working }
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
                    if (prefs.alwaysOn) listen() else stopNow()
                }
            }
        }.start()
    }

    /** "floating avatar on / off" is handled here, without asking Gemini. */
    private fun handleLocalCommand(text: String): Boolean {
        val t = text.lowercase(Locale.ROOT)
        if (handleDesignCommand(text, t)) return true
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

    /** "change the orb", "I don't like this orb", "red orb" and so on. */
    private fun handleDesignCommand(text: String, t: String): Boolean {
        val orbWords = listOf("orb", "অর্ব", "অর্ভ", "design", "ডিজাইন", "colour", "color", "রং", "রঙ", "theme", "থিম", "avatar look")
        if (orbWords.none { t.contains(it) }) return false
        if (listOf("floating", "ফ্লোটিং", "overlay", "ওভারলে").any { t.contains(it) }) return false

        val named = OrbDesigns.all.firstOrNull { d -> d.words.any { t.contains(it) } }
        val changeWords = listOf(
            "change", "next", "another", "other", "different", "new", "switch", "don't like", "dont like", "not like",
            "বদল", "পাল্ট", "অন্য", "নতুন", "ভালো লাগ", "ভাল লাগ", "পছন্দ", "bodlao", "bodle", "onno", "bhalo lag", "valo lag", "pochondo"
        )
        val id = when {
            named != null -> named.id
            changeWords.any { t.contains(it) } -> OrbDesigns.next(prefs.orbDesign)
            else -> return false
        }
        setOrbDesign(id)
        val bangla = text.any { it in '\u0980'..'\u09FF' }
        speak(if (bangla) "ঠিক আছে, এই অর্বটা দেখুন।" else "Okay, how about this one?")
        return true
    }

    /** Save and show an orb design everywhere (full screen and floating). */
    fun setOrbDesign(id: String) {
        main.post {
            prefs.orbDesign = id
            listeners.forEach { it.onOrbDesign(id) }
        }
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
        lastInteraction = System.currentTimeMillis()
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
