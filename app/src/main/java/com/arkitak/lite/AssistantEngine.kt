package com.arkitak.lite

import com.arkitak.lite.wake.DiagLog
import com.arkitak.lite.wake.WakeEngineManager
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
        fun onReply() {}                  // the assistant's reply text changed: read engine.replyHistory()
        fun onPersona(persona: String) {} // the active persona changed
    }

    data class Reply(val persona: String, val text: String)

    companion object {
        @Volatile private var instance: AssistantEngine? = null
        fun get(context: Context): AssistantEngine =
            instance ?: synchronized(this) {
                instance ?: AssistantEngine(context.applicationContext).also { instance = it }
            }

        private const val VOICE_LEVEL = 0.15f      // mic level that counts as "the user is talking"
        private const val MAX_PRE_CONNECT = 60     // ~1.2 s of audio kept while the connection is still opening
        private const val FILLER_AFTER_MS = 1400L  // answer slower than this after you finished talking: Olivia says "hmm, darao..."
        private const val FILLER_MIN_FRAMES = 25   // ~0.5 s of real speech, so a cough or a click does not trigger it
    }

    private val main = Handler(Looper.getMainLooper())
    private val prefs = Prefs(ctx)
    private val listeners = CopyOnWriteArrayList<Listener>()

    @Volatile var state: String = OrbView.STATE_IDLE
        private set

    /** True while a conversation is running (microphone open, Gemini session open or reconnecting). */
    @Volatile var active: Boolean = false
        private set

    /** Both personas stay connected, so switching between them has no connection delay. */
    private val sessions = java.util.concurrent.ConcurrentHashMap<String, GeminiLive>()
    @Volatile var persona: String = prefs.persona
        private set
    private val live: GeminiLive? get() = sessions[persona]
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
    private val fillerBanks = Personas.ALL.associateWith { FillerBank(ctx, prefs, it) }
    private val fillers: FillerBank get() = fillerBanks.getValue(persona)
    private val ring = java.util.ArrayDeque<Pair<Long, ByteArray>>() // the last ~10 s of microphone audio, to replay a sentence to the persona you just addressed
    @Volatile private var chunkSeq = 0L
    @Volatile private var utterStartSeq = 0L
    private val inputBuf = StringBuilder()
    private val replies = ArrayList<Reply>()
    private val replyBuf = StringBuilder()
    private var replyOpen = false
    @Volatile private var utteranceFrames = 0
    @Volatile private var lastActivityAt = 0L // wake mode: when the conversation last did anything (talking, thinking, speaking)
    private var fillerPlayed = false
    @Volatile private var awaitingAnswer = false
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
        lastActivityAt = SystemClock.elapsedRealtime() // wake mode: the silence timeout counts from the last state change
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
                state == OrbView.STATE_SPEAKING -> speaker?.flush() // its callback returns the orb to idle
                state == OrbView.STATE_THINKING -> { userSpoke = false; awaitingAnswer = false; setState(OrbView.STATE_IDLE) }
                else -> {} // the mic stays on
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

        persona = prefs.persona
        sessions.values.forEach { try { it.close() } catch (_: Exception) {} }
        sessions.clear()
        for (id in listOf(persona, Personas.other(persona))) sessions[id] = newSession(id).also { it.connect() }
        inputBuf.setLength(0)
        closeReply()

        // Wake mode: the wake engine lets go of the microphone before the conversation opens it (never two AudioRecords at once).
        DiagLog.safe("wake: session started") { WakeEngineManager.onSessionStarted() }
        lastActivityAt = SystemClock.elapsedRealtime()

        val m = MicStream { chunk, level -> onMicChunk(chunk, level) }
        if (!m.start()) {
            postMessage("Could not open the microphone. Is another app using it?")
            stopNow()
            return
        }
        mic = m

        if (prefs.alwaysOn) LiveMicService.start(ctx)
        setState(OrbView.STATE_IDLE) // the orb is calm until you actually talk
        main.removeCallbacks(ticker)
        main.postDelayed(ticker, 200)
    }

    private fun stopNow() {
        active = false
        main.removeCallbacks(ticker)
        main.removeCallbacks(endSpeaking)
        main.removeCallbacks(speakTick)
        fillerBanks.values.forEach { it.cancel() }
        awaitingAnswer = false
        fillerPlayed = false
        utteranceFrames = 0
        try { mic?.stop() } catch (_: Exception) {}
        sessions.values.forEach { try { it.close() } catch (_: Exception) {} }
        try { speaker?.release() } catch (_: Exception) {}
        mic = null
        sessions.clear()
        speaker = null
        preConnect.clear()
        LiveMicService.stop(ctx)
        emitLevel(0f)
        setState(OrbView.STATE_IDLE)
        // Wake mode: the microphone is free again, so the wake engine listens for the name.
        DiagLog.safe("wake: session ended") { WakeEngineManager.onSessionEnded() }
    }

    /**
     * Wake word or manual trigger. Main thread. [personaId] is the name that was said (null = keep the current persona).
     * Returns true only if THIS call started the conversation; a conversation that was already running is not ours.
     */
    fun startFromWake(personaId: String?, source: String): Boolean {
        val id = personaId?.let { Personas.valid(it) }
        DiagLog.log("WAKE", "start from '$source' persona=${id ?: "(unchanged)"} alreadyActive=$active")
        if (active) {
            if (id != null && id != persona) switchNow(id, replay = false)
            lastActivityAt = SystemClock.elapsedRealtime()
            return false
        }
        if (id != null) { persona = id; prefs.persona = id }
        startNow()
        lastActivityAt = SystemClock.elapsedRealtime()
        return active
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
            utteranceFrames++
            if (userFrames >= 5) {
                heardUserAt = now
                if (!userSpoke) { userSpoke = true; utterStartSeq = max(0L, chunkSeq - 30); main.post { onUserStartedTalking() } }
            }
        } else if (userFrames > 0) {
            userFrames--
        }

        val seq = ++chunkSeq
        synchronized(ring) { // same lock as a persona switch, so replayed and live audio never interleave
            ring.addLast(Pair(seq, chunk))
            while (ring.size > 500) ring.removeFirst()
            val l = live
            if (l != null && l.isReady()) {
                while (true) l.sendAudio(preConnect.poll() ?: break)
                l.sendAudio(chunk)
            } else if (preConnect.size < MAX_PRE_CONNECT) {
                preConnect.offer(chunk) // connection still opening: keep the first words instead of losing them
            }
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

    /**
     * Orb states: IDLE = calm (nobody is talking), LISTENING = only while you are really talking, THINKING = you finished and
     * Gemini has not started answering, SPEAKING = until the speaker has played the last word.
     */
    private val ticker = object : Runnable {
        override fun run() {
            if (!active) return
            val now = SystemClock.elapsedRealtime()
            if (prefs.wakeEnabled && state == OrbView.STATE_IDLE && now - lastActivityAt > prefs.silenceSeconds * 1000L) {
                // Wake mode: nobody talked for the silence timeout. Sleep; the wake engine takes over again.
                DiagLog.log("WAKE", "silence timeout (${prefs.silenceSeconds} s): conversation ends, wake engine resumes")
                postMessage("Back to waiting for the wake word")
                stopNow()
                return
            }
            if (state == OrbView.STATE_LISTENING && userSpoke && now - heardUserAt > 700L) {
                userSpoke = false
                thinkingSince = now
                setState(OrbView.STATE_THINKING)
            } else if (state == OrbView.STATE_LISTENING && !userSpoke) {
                setState(OrbView.STATE_IDLE)
            } else if (state == OrbView.STATE_THINKING) {
                if (!fillerPlayed && now - thinkingSince > FILLER_AFTER_MS && utteranceFrames >= FILLER_MIN_FRAMES && speaker?.isPlaying() != true) {
                    playFiller()
                }
                if (now - thinkingSince > 30_000L) { // no answer at all: back to calm
                    utteranceFrames = 0
                    awaitingAnswer = false
                    setState(OrbView.STATE_IDLE)
                }
            }
            main.postDelayed(this, 200)
        }
    }

    private fun onUserStartedTalking() {
        if (!active) return
        lastActivityAt = SystemClock.elapsedRealtime()
        main.removeCallbacks(endSpeaking)
        fillerPlayed = false
        awaitingAnswer = false
        inputBuf.setLength(0)
        closeReply()
        if (state != OrbView.STATE_LISTENING) setState(OrbView.STATE_LISTENING)
    }

    /** Gemini is slow: say "hmm, darao, ektu dekhi..." in the same voice, then the real answer follows. */
    private fun playFiller() {
        fillerPlayed = true
        val clip = fillers.pick() ?: return
        val spk = speaker ?: return
        awaitingAnswer = true
        spk.enqueue(clip)
        spk.turnComplete()
    }

    private fun onSpeakerPlaying(playing: Boolean) {
        if (!active) return
        main.removeCallbacks(endSpeaking)
        if (playing) {
            userSpoke = false
            utteranceFrames = 0
            setState(OrbView.STATE_SPEAKING)
            main.removeCallbacks(speakTick)
            main.post(speakTick)
        } else {
            lastPlaybackEnd = SystemClock.elapsedRealtime()
            main.postDelayed(endSpeaking, 350L) // short grace: more audio may follow a tiny gap
        }
    }

    private val endSpeaking = Runnable {
        if (!active) return@Runnable
        if (speaker?.isPlaying() == true) return@Runnable // more audio arrived, still speaking
        emitLevel(0f)
        if (state == OrbView.STATE_SPEAKING) {
            if (awaitingAnswer) { thinkingSince = SystemClock.elapsedRealtime(); setState(OrbView.STATE_THINKING) } // only the stalling clip played
            else setState(OrbView.STATE_IDLE)
        }
    }

    /** While speaking: feed the orb the loudness of what is being heard right now (lips move in time with the voice). */
    private val speakTick = object : Runnable {
        override fun run() {
            val spk = speaker
            if (!active || spk == null || state != OrbView.STATE_SPEAKING) return
            emitLevel(spk.currentLevel())
            main.postDelayed(this, 50)
        }
    }

    // ---------------------------------------------------------------- Gemini Live callbacks (OkHttp threads)

    private fun newSession(id: String) = GeminiLive(
        prefs.apiKey, GeminiLive.normalizeModel(Personas.model(prefs, id)),
        Personas.voice(prefs, id), Personas.prompt(prefs.lang, id), SessionListener(id)
    )

    private inner class SessionListener(private val id: String) : GeminiLive.Listener {
        private fun mine() = id == persona

        override fun onReady() {
            if (!mine()) return
            main.post {
                if (!active || !mine()) return@post
                setState(OrbView.STATE_IDLE)
                // record the stalling clips once, in the background: this persona first, the other one later
                main.postDelayed({ if (active) fillerBanks[persona]?.ensure() }, 3000L)
                main.postDelayed({ if (active) fillerBanks[Personas.other(persona)]?.ensure() }, 45_000L)
            }
        }

        override fun onStatus(text: String) {
            if (!mine()) return
            main.post {
                if (!active || !mine()) return@post
                setState(OrbView.STATE_IDLE)
                if (text != lastStatus) { lastStatus = text; postMessage(text) }
            }
        }

        override fun onAudio(pcm24k: ByteArray) {
            if (!mine()) return
            awaitingAnswer = false
            speaker?.enqueue(pcm24k)
        }

        override fun onInterrupted() {
            if (!mine()) return
            speaker?.flush()
            main.post { closeReply() }
        }

        override fun onTurnComplete() {
            if (!mine()) return
            speaker?.turnComplete()
            awaitingAnswer = false
            main.postDelayed({ closeReply() }, 600L) // the last words of the transcript can arrive just after
            main.post {
                if (active && state == OrbView.STATE_THINKING && speaker?.isPlaying() != true) setState(OrbView.STATE_IDLE)
            }
        }

        override fun onToolCall(id: String, name: String, args: JSONObject) {
            if (!mine()) return
            if (name == "set_floating_avatar") {
                val on = args.optBoolean("on", true)
                main.post { listeners.forEach { it.onFloatingCommand(on) } }
                sessions[this.id]?.sendToolResult(id, name, JSONObject().put("result", "ok"))
            } else if (name == "open_wake_screen") {
                val enable = args.optBoolean("enable", true)
                main.post {
                    DiagLog.safe("open wake screen") {
                        ctx.startActivity(
                            android.content.Intent(ctx, com.arkitak.lite.wake.WakeSettingsActivity::class.java)
                                .putExtra(com.arkitak.lite.wake.WakeSettingsActivity.EXTRA_AUTO_ENABLE, enable)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
                sessions[this.id]?.sendToolResult(id, name, JSONObject().put("result", "ok"))
            } else {
                sessions[this.id]?.sendToolResult(id, name, JSONObject().put("error", "unknown function"))
            }
        }

        override fun onInputText(text: String) {
            if (!mine()) return
            main.post {
                if (!active || !mine()) return@post
                inputBuf.append(text)
                val named = Personas.detect(inputBuf.toString())
                if (named != null && named != persona) switchNow(named, replay = true)
            }
        }

        override fun onOutputText(text: String) {
            if (!mine()) return
            main.post { if (active && mine()) addReplyText(text) }
        }

        override fun onFatal(message: String) {
            main.post {
                if (!active) return@post
                if (mine()) { postMessage(message); stopNow() }
                else postMessage("${Personas.name(id)}: $message") // the other persona is unavailable; this one keeps working
            }
        }
    }

    // ---------------------------------------------------------------- personas

    /** Make [id] the active persona now (menu / manual). Saying a name does the same through [switchNow]. */
    fun switchPersona(id: String) { main.post { switchNow(Personas.valid(id), replay = false) } }

    /**
     * Both sessions are already connected, so the switch itself costs nothing. With [replay], the sentence you are saying
     * (kept from its start in a small buffer) is sent to the new persona at once, so it answers that same sentence.
     */
    private fun switchNow(id: String, replay: Boolean) {
        if (!active || id == persona) return
        val oldId = persona
        main.removeCallbacks(endSpeaking)
        speaker?.flush()          // the old persona's voice stops
        awaitingAnswer = false
        fillerPlayed = false
        inputBuf.setLength(0)
        closeReply()
        synchronized(ring) {
            persona = id
            prefs.persona = id
            val target = sessions[id]
            if (replay && target != null && target.isReady()) {
                val from = utterStartSeq
                for (e in ring) if (e.first >= from) target.sendAudio(e.second)
            }
        }
        listeners.forEach { it.onPersona(id) }
        // The old session heard half a sentence: renew it quietly in the background so it is clean for the next switch.
        main.postDelayed({
            if (!active || persona == oldId) return@postDelayed
            try { sessions[oldId]?.close() } catch (_: Exception) {}
            sessions[oldId] = newSession(oldId).also { it.connect() }
        }, 300L)
    }

    // ---------------------------------------------------------------- reply text (for the popup next to the floating orb)

    private fun addReplyText(text: String) {
        replyBuf.append(text)
        val reply = Reply(persona, replyBuf.toString())
        if (replyOpen && replies.isNotEmpty()) replies[replies.lastIndex] = reply
        else { replies.add(reply); replyOpen = true }
        while (replies.size > 12) replies.removeAt(0)
        listeners.forEach { it.onReply() }
    }

    private fun closeReply() { replyOpen = false; replyBuf.setLength(0) }

    /** The latest replies, oldest first (main thread). The last one may still be growing. */
    fun replyHistory(): List<Reply> = ArrayList(replies)
}
