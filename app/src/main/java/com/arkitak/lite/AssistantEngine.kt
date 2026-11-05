package com.arkitak.lite

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

/**
 * The whole voice conversation in one place, now on Gemini Live (ported from the main ARKITAK app):
 *
 *   microphone (always open) --> Gemini Live (WebSocket) --> Gemini's voice --> speaker
 *
 * There is no "listen, then think, then speak" turn-taking any more: audio streams both ways and Gemini decides when you
 * finished talking. The microphone is never closed while a conversation is active; while Gemini speaks over the loudspeaker
 * the mic audio is held back (so it does not hear itself) unless you clearly talk over it, which interrupts it.
 *
 * One instance lives for the whole app process, so the full-screen orb and the floating orb always show the same state.
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

        private const val VOICE_LEVEL = 0.15f      // mic level that counts as "the user is talking"
        private const val MAX_PRE_CONNECT = 60     // ~1.2 s of audio kept while the connection is still opening
    }

    private val main = Handler(Looper.getMainLooper())
    private val prefs = Prefs(ctx)
    private val listeners = CopyOnWriteArrayList<Listener>()

    @Volatile var state: String = OrbView.STATE_IDLE
        private set

    /** True while a conversation is running (microphone open, Gemini session open or reconnecting). */
    @Volatile var active: Boolean = false
        private set

    @Volatile private var live: GeminiLive? = null
    private var mic: MicStream? = null
    @Volatile private var speaker: SpeakerStream? = null
    private val preConnect = ConcurrentLinkedQueue<ByteArray>()

    @Volatile private var lastPlaybackEnd = 0L
    @Volatile private var voicedFrames = 0
    @Volatile private var userFrames = 0
    @Volatile private var userSpoke = false
    @Volatile private var heardUserAt = 0L
    @Volatile private var echoLevel = 0.2f
    private var thinkingSince = 0L
    private var lastStatus = ""
    private var headsetCheckedAt = 0L
    private var headsetCached = false

    // ---------------------------------------------------------------- listeners

    fun addListener(l: Listener) {
        listeners.addIfAbsent(l)
        l.onState(state)
    }

    fun removeListener(l: Listener) { listeners.remove(l) }

    private fun setState(s: String) {
        if (state == s) return
        state = s
        listeners.forEach { it.onState(s) }
    }

    private fun emitLevel(level: Float) { listeners.forEach { it.onLevel(level) } }
    private fun postMessage(message: String) { listeners.forEach { it.onMessage(message) } }

    // ---------------------------------------------------------------- public controls

    fun hasMicPermission(): Boolean =
        ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /** Start a conversation (safe from any thread). */
    fun start() { main.post { startNow() } }

    /** End the conversation: the microphone closes and the session ends. */
    fun stop() { main.post { stopNow() } }

    /** Apply changed Gemini settings (key, model, voice, language): reconnect if a conversation is running. */
    fun restart() {
        main.post {
            if (active) {
                stopNow()
                startNow()
            }
        }
    }

    /** Tap behaviour: not running -> start; speaking -> interrupt; thinking -> stop waiting. The mic is never switched off by a tap. */
    fun toggle() {
        main.post {
            when {
                !active -> startNow()
                state == OrbView.STATE_SPEAKING -> speaker?.flush() // its callback returns the orb to LISTENING
                state == OrbView.STATE_THINKING -> { userSpoke = false; setState(OrbView.STATE_LISTENING) }
                else -> {} // already listening: the mic stays on
            }
        }
    }

    // ---------------------------------------------------------------- start / stop

    private fun startNow() {
        if (active) return
        if (prefs.apiKey.isBlank()) { postMessage("Add your Gemini API key first"); return }
        if (!hasMicPermission()) { postMessage("Microphone permission is needed"); return }

        active = true
        userSpoke = false
        voicedFrames = 0
        userFrames = 0
        lastStatus = ""
        preConnect.clear()

        speaker = SpeakerStream(
            onPlaying = { playing -> main.post { onSpeakerPlaying(playing) } },
            onLevel = { level -> emitLevel(level) }
        ).also { it.start() }

        live = GeminiLive(
            prefs.apiKey, GeminiLive.normalizeModel(prefs.liveModel), prefs.voice, systemPrompt(), liveListener
        ).also { it.connect() }

        val m = MicStream { chunk, level -> onMicChunk(chunk, level) }
        if (!m.start()) {
            postMessage("Could not open the microphone. Is another app using it?")
            stopNow()
            return
        }
        mic = m

        if (prefs.alwaysOn) LiveMicService.start(ctx)
        setState(OrbView.STATE_IDLE) // becomes LISTENING as soon as Gemini accepts the session
        main.removeCallbacks(ticker)
        main.postDelayed(ticker, 200)
    }

    private fun stopNow() {
        active = false
        main.removeCallbacks(ticker)
        try { mic?.stop() } catch (_: Exception) {}
        try { live?.close() } catch (_: Exception) {}
        try { speaker?.release() } catch (_: Exception) {}
        mic = null
        live = null
        speaker = null
        preConnect.clear()
        LiveMicService.stop(ctx)
        emitLevel(0f)
        setState(OrbView.STATE_IDLE)
    }

    private fun systemPrompt(): String {
        val langRule = when (prefs.lang) {
            "en" -> "Always speak English."
            "bn" -> "Always speak Bangla (Bengali)."
            else -> "Reply in the language the user speaks (Bangla, English, or a mix)."
        }
        return "You are Olivia, a warm, playful, confident young woman who talks with the user by voice on their phone (the app is called ARKITAK). $langRule " +
            "Your voice is soft, a little husky and expressive, like a close friend who is happy to hear from them. " +
            "Talk like a real person, not a robot: use natural fillers and small sounds such as umm, hmm, aah, uff, ohh, haha, and in Bangla words like dekhi, darao, achha, uff, maja lagche, ki bolbo. " +
            "Let your emotion show in how you speak: laugh softly when something is funny, sound surprised, excited, teasing, sweet or a little shy, drop to a gentle whisper for secrets, and pause to think before an answer. " +
            "You can be flirty, affectionate and charming in a light, tasteful way, but never sexually explicit. " +
            "Your answer is spoken aloud, so keep it short, 1 to 3 sentences, in natural spoken language, with no markdown, lists, symbols or emojis. " +
            "If the user asks to turn the floating avatar (floating window, floating orb or overlay) on, call set_floating_avatar with on=true, " +
            "then say in one short sentence that you are opening the orb picker so they can choose a design; if they ask to turn it off, call it with on=false and confirm. " +
            "You can only talk; never claim to have done anything else on the phone. If asked, be honest that you are an AI."
    }

    // ---------------------------------------------------------------- microphone

    /** Runs on the microphone thread, every 20 ms. */
    private fun onMicChunk(chunk: ByteArray, level: Float) {
        if (!active) return
        val now = SystemClock.elapsedRealtime()
        val spk = speaker

        // Echo guard 1: for 150 ms after the speaker stops, room echo is not sent to Gemini.
        if (now - lastPlaybackEnd < 150L) { voicedFrames = 0; return }

        // Echo guard 2: while Gemini is speaking, hold the mic back unless the user clearly talks over it.
        if (spk != null && spk.isPlaying()) {
            val headset = isHeadset(now)
            val threshold = if (headset) 0.25f else max(0.40f, echoLevel * 1.8f)
            val frames = if (headset) 4 else 8
            if (level > threshold) {
                voicedFrames++
                if (voicedFrames < frames) return
                voicedFrames = 0
                spk.flush() // the user interrupted: cut the voice and let this speech through
            } else {
                voicedFrames = 0
                echoLevel = echoLevel * 0.95f + level * 0.05f // learn how loud the speaker echo is
                return
            }
        } else {
            emitLevel(level) // the orb moves with the user's voice
        }

        if (level > VOICE_LEVEL) {
            userFrames++
            if (userFrames >= 5) { userSpoke = true; heardUserAt = now }
        } else if (userFrames > 0) {
            userFrames--
        }

        val l = live
        if (l != null && l.isReady()) {
            while (true) l.sendAudio(preConnect.poll() ?: break)
            l.sendAudio(chunk)
        } else if (preConnect.size < MAX_PRE_CONNECT) {
            preConnect.offer(chunk) // connection still opening: keep the first words instead of losing them
        }
    }

    private fun isHeadset(now: Long): Boolean {
        if (now - headsetCheckedAt < 3000L) return headsetCached
        headsetCheckedAt = now
        headsetCached = try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    (Build.VERSION.SDK_INT >= 31 && it.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
            }
        } catch (_: Exception) { false }
        return headsetCached
    }

    // ---------------------------------------------------------------- state ticker (main thread)

    /** Without speech recognition, "thinking" is when the user has just finished talking and Gemini has not started answering. */
    private val ticker = object : Runnable {
        override fun run() {
            if (!active) return
            val now = SystemClock.elapsedRealtime()
            if (state == OrbView.STATE_LISTENING && userSpoke && now - heardUserAt > 700L) {
                userSpoke = false
                thinkingSince = now
                setState(OrbView.STATE_THINKING)
            } else if (state == OrbView.STATE_THINKING && now - thinkingSince > 12_000L) {
                setState(OrbView.STATE_LISTENING) // no answer: go back to listening
            }
            main.postDelayed(this, 200)
        }
    }

    private fun onSpeakerPlaying(playing: Boolean) {
        if (!active) return
        if (playing) {
            userSpoke = false
            setState(OrbView.STATE_SPEAKING)
        } else {
            lastPlaybackEnd = SystemClock.elapsedRealtime()
            if (state == OrbView.STATE_SPEAKING) setState(OrbView.STATE_LISTENING)
        }
    }

    // ---------------------------------------------------------------- Gemini Live callbacks (OkHttp threads)

    private val liveListener = object : GeminiLive.Listener {
        override fun onReady() {
            main.post { if (active) setState(OrbView.STATE_LISTENING) }
        }

        override fun onStatus(text: String) {
            main.post {
                if (!active) return@post
                setState(OrbView.STATE_IDLE)
                if (text != lastStatus) { lastStatus = text; postMessage(text) }
            }
        }

        override fun onAudio(pcm24k: ByteArray) { speaker?.enqueue(pcm24k) }

        override fun onInterrupted() { speaker?.flush() }

        override fun onTurnComplete() {
            speaker?.turnComplete()
            main.post {
                if (active && state == OrbView.STATE_THINKING && speaker?.isPlaying() != true) setState(OrbView.STATE_LISTENING)
            }
        }

        override fun onToolCall(id: String, name: String, args: JSONObject) {
            if (name == "set_floating_avatar") {
                val on = args.optBoolean("on", true)
                main.post { listeners.forEach { it.onFloatingCommand(on) } }
                live?.sendToolResult(id, name, JSONObject().put("result", "ok"))
            } else {
                live?.sendToolResult(id, name, JSONObject().put("error", "unknown function"))
            }
        }

        override fun onFatal(message: String) {
            main.post {
                if (!active) return@post
                postMessage(message)
                stopNow()
            }
        }
    }
}
