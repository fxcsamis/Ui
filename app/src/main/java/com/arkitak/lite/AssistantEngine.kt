package com.arkitak.lite

import com.arkitak.lite.wake.DiagLog
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
        private const val GROUP_TURNS = 1          // group chat: after the first persona answers, the other one answers once
        private const val BANTER_TURNS = 6         // "talk to each other": up to 6 hand-overs, until you speak
        private const val ROUTE_HOLD_MS = 600L     // the first audio of a reply waits at most this long for the transcript of your words
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

    // ---- group chat: both personas take part, and can talk to each other ----
    @Volatile var groupMode = false
        private set
    private var bridgeBudget = 0
    private var bridgeFrom: String? = null
    private val userUtterance = StringBuilder() // what the user said in the current sentence (for the other persona)
    private var bridgesSinceUser = 0

    // ---- early hand-over: the other persona prepares its answer WHILE the first voice is still playing ----
    // Its audio and words are kept here (never played early) and released the moment the first voice ends: no waiting gap.
    private val prefetchLock = Any()
    @Volatile private var prefetchTo: String? = null
    private val prefetchAudio = ArrayList<ByteArray>()
    private val prefetchText = StringBuilder()
    private var prefetchDone = false

    // ---- routing: a name you say must reach the right persona BEFORE the wrong voice speaks ----
    private val routeLock = Any()
    private val heldAudio = ArrayList<ByteArray>()
    private var holding = false
    private var routeDecided = false
    @Volatile private var transcriptSeen = false   // some of this sentence was transcribed already
    @Volatile private var transcriptsWork = false  // transcripts ever arrived: only then is it worth waiting for them
    private val releaseHeld = Runnable { releaseHeldAudio() }
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
        dropHeldAudio()
        bridgeFrom = null
        cancelPrefetch()
        bridgeBudget = 0
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
    }

    /**
     * Started from outside the app (the assistant gesture). Main thread. [personaId] is the name that was said (null = keep the current persona).
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
        userUtterance.setLength(0)
        transcriptSeen = false
        synchronized(routeLock) { routeDecided = false }
        bridgeFrom = null
        cancelPrefetch()
        bridgesSinceUser = 0
        bridgeBudget = if (groupMode) GROUP_TURNS else 0 // you speak: any running hand-over chat stops
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
            // The first voice has said everything (its turn was complete before the early hand-over): the other one speaks NOW.
            prefetchTo?.let { completePrefetch(it) }
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
        maybeBridge()
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
            synchronized(prefetchLock) { if (id == prefetchTo) { prefetchAudio.add(pcm24k); return } }
            if (!mine()) return
            awaitingAnswer = false
            if (!holdForRouting(pcm24k)) speaker?.enqueue(pcm24k)
        }

        override fun onInterrupted() {
            if (!mine()) return
            speaker?.flush()
            main.post { closeReply() }
        }

        override fun onTurnComplete() {
            synchronized(prefetchLock) { if (id == prefetchTo) { prefetchDone = true; return } }
            if (!mine()) return
            speaker?.turnComplete()
            awaitingAnswer = false
            synchronized(routeLock) { routeDecided = false } // the next reply decides its route again
            main.postDelayed({ closeReply() }, 600L) // the last words of the transcript can arrive just after
            main.post {
                if (!active || !mine()) return@post
                if (groupMode && bridgeBudget > 0) bridgeFrom = id // the other persona answers once this voice has finished
                if (state == OrbView.STATE_THINKING && speaker?.isPlaying() != true) setState(OrbView.STATE_IDLE)
                if (bridgeFrom == id && speaker?.isPlaying() == true) {
                    // This voice is still playing: the other persona starts thinking now (after the last words of the transcript arrived).
                    main.postDelayed({ if (active) prefetchBridge() }, 700L)
                } else maybeBridge()
            }
        }

        override fun onToolCall(id: String, name: String, args: JSONObject) {
            if (!mine()) return
            if (name == "set_floating_avatar") {
                val on = args.optBoolean("on", true)
                main.post { listeners.forEach { it.onFloatingCommand(on) } }
                sessions[this.id]?.sendToolResult(id, name, JSONObject().put("result", "ok"))
            } else {
                sessions[this.id]?.sendToolResult(id, name, JSONObject().put("error", "unknown function"))
            }
        }

        override fun onInputText(text: String) {
            if (!mine()) return
            main.post {
                if (!active || !mine()) return@post
                transcriptSeen = true
                transcriptsWork = true
                inputBuf.append(text)
                userUtterance.append(text)
                val heard = inputBuf.toString()
                // 1) group chat / "talk to each other"
                when (Personas.detectGroup(heard)) {
                    Personas.GroupIntent.BANTER -> { setGroupNow(true); bridgeBudget = BANTER_TURNS }
                    Personas.GroupIntent.GROUP -> { setGroupNow(true); if (bridgeBudget < GROUP_TURNS) bridgeBudget = GROUP_TURNS }
                    else -> {}
                }
                // 2) who is addressed: in a group the FIRST name leads; alone, the last name wins
                val both = Personas.bothNamed(heard)
                val named = if (groupMode && both) Personas.firstNamed(heard) else Personas.detect(heard)
                if (named != null && !both && groupMode && Personas.detectGroup(heard) == Personas.GroupIntent.NONE) {
                    setGroupNow(false) // one name only: back to a private conversation with that persona
                }
                if (named != null && named != persona) switchNow(named, replay = true)
                else if (holding) { main.removeCallbacks(releaseHeld); releaseHeldAudio() } // right voice already: let it speak
            }
        }

        override fun onOutputText(text: String) {
            synchronized(prefetchLock) { if (id == prefetchTo) { prefetchText.append(text); return } }
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
        dropHeldAudio()           // audio the old persona had already started is thrown away, never played
        bridgeFrom = null
        cancelPrefetch()
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

    // ---------------------------------------------------------------- routing hold (a name must win before the wrong voice speaks)

    /** True = this audio was kept back (nothing is played yet). */
    private fun holdForRouting(pcm: ByteArray): Boolean {
        synchronized(routeLock) {
            if (routeDecided) return false
            if (transcriptSeen || !transcriptsWork) { routeDecided = true; return false } // nothing to wait for
            heldAudio.add(pcm)
            if (!holding) { holding = true; main.postDelayed(releaseHeld, ROUTE_HOLD_MS) }
            return true
        }
    }

    /** No name sent the sentence to the other persona: the held audio plays now. Main thread. */
    private fun releaseHeldAudio() {
        val list: List<ByteArray>
        synchronized(routeLock) {
            routeDecided = true
            holding = false
            list = ArrayList(heldAudio)
            heldAudio.clear()
        }
        for (c in list) speaker?.enqueue(c)
    }

    private fun dropHeldAudio() {
        main.removeCallbacks(releaseHeld)
        synchronized(routeLock) { heldAudio.clear(); holding = false; routeDecided = false }
    }

    // ---------------------------------------------------------------- group chat and the bridge between the two personas

    private fun setGroupNow(on: Boolean) {
        if (groupMode == on) return
        groupMode = on
        if (!on) { bridgeBudget = 0; bridgeFrom = null; cancelPrefetch() }
        DiagLog.log("GROUP", "group chat ${if (on) "ON" else "OFF"}")
        listeners.forEach { it.onPersona(persona) }
    }

    /** Menu / manual: both personas take part in the conversation. */
    fun setGroup(on: Boolean) { main.post { setGroupNow(on); if (on) bridgeBudget = GROUP_TURNS } }

    /** Menu / manual: the two personas talk to each other until you speak. */
    fun startBanter() {
        main.post {
            if (!active) { postMessage("Start the conversation first"); return@post }
            setGroupNow(true)
            bridgeBudget = BANTER_TURNS
            bridgesSinceUser = 0
            userUtterance.setLength(0)
            val other = Personas.other(persona)
            sessions[persona]?.sendText(
                "[Group chat] ${Personas.name(other)} is here with you now. Start a short, fun conversation with ${Personas.name(other)}: " +
                    "say one or two sentences to ${Personas.name(other)} directly, in your own voice."
            )
            DiagLog.log("GROUP", "banter started by ${persona}")
        }
    }

    /** The voice that was speaking has finished: pass the floor to the other persona, who hears what was said (as text). Main thread. */
    private fun maybeBridge() {
        prefetchTo?.let { completePrefetch(it); return }
        val from = bridgeFrom ?: return
        if (!active || !groupMode || bridgeBudget <= 0) { bridgeFrom = null; return }
        if (speaker?.isPlaying() == true || state == OrbView.STATE_LISTENING || state == OrbView.STATE_SPEAKING) return
        val to = Personas.other(from)
        val target = sessions[to]
        if (target == null || !target.isReady()) { bridgeFrom = null; postMessage("${Personas.name(to)} is not connected yet"); return }
        val said = replies.lastOrNull { it.persona == from }?.text?.trim().orEmpty()
        if (said.isEmpty()) { bridgeFrom = null; return } // no transcript: nothing to pass on
        val user = userUtterance.toString().trim()
        val firstAfterUser = bridgesSinceUser == 0 && user.isNotEmpty()
        bridgesSinceUser++
        bridgeFrom = null
        bridgeBudget--
        // The floor moves: the other persona is now the active one, so its voice is played and it hears the microphone.
        handOver(to)
        val msg = if (firstAfterUser) {
            "[Group chat] The user said: \"$user\". ${Personas.name(from)} just answered: \"$said\". Now it is your turn: answer the user in your own voice " +
                "and react to ${Personas.name(from)} naturally. Maximum 2 short sentences."
        } else {
            "[Group chat] ${Personas.name(from)} said to you: \"$said\". Reply to ${Personas.name(from)} directly, in your own voice and personality. " +
                "Maximum 2 short sentences. Do not repeat what was already said."
        }
        DiagLog.log("GROUP", "bridge ${from} -> $to (budget left $bridgeBudget)")
        target.sendText(msg)
    }

    private fun cancelPrefetch() {
        synchronized(prefetchLock) { prefetchTo = null; prefetchAudio.clear(); prefetchText.setLength(0); prefetchDone = false }
    }

    /**
     * The first voice is still playing but it has finished generating: send its words to the other persona now, so that the
     * other brain is already answering. Its audio waits in [prefetchAudio] and is released by [completePrefetch]. Main thread.
     */
    private fun prefetchBridge() {
        val from = bridgeFrom ?: return
        if (prefetchTo != null || !active || !groupMode || bridgeBudget <= 0) return
        if (speaker?.isPlaying() != true) return // nothing is playing any more: the normal hand-over does it at once
        val to = Personas.other(from)
        val target = sessions[to]
        if (target == null || !target.isReady()) return
        val said = replies.lastOrNull { it.persona == from }?.text?.trim().orEmpty()
        if (said.isEmpty()) return
        val user = userUtterance.toString().trim()
        val firstAfterUser = bridgesSinceUser == 0 && user.isNotEmpty()
        bridgesSinceUser++
        bridgeBudget--
        bridgeFrom = null
        synchronized(prefetchLock) { prefetchAudio.clear(); prefetchText.setLength(0); prefetchDone = false; prefetchTo = to }
        val msg = if (firstAfterUser) {
            "[Group chat] The user said: \"$user\". ${Personas.name(from)} just answered: \"$said\". Now it is your turn: answer the user in your own voice " +
                "and react to ${Personas.name(from)} naturally. Maximum 2 short sentences."
        } else {
            "[Group chat] ${Personas.name(from)} said to you: \"$said\". Reply to ${Personas.name(from)} directly, in your own voice and personality. " +
                "Maximum 2 short sentences. Do not repeat what was already said."
        }
        DiagLog.log("GROUP", "early hand-over ${from} -> $to (budget left $bridgeBudget)")
        target.sendText(msg)
    }

    /** The first voice has finished: the other persona takes the floor and its prepared audio plays at once. Main thread. */
    private fun completePrefetch(to: String) {
        if (prefetchTo != to) return
        if (!active || !groupMode || state == OrbView.STATE_LISTENING) { cancelPrefetch(); return }
        if (speaker?.isPlaying() == true) return // the first voice is not done yet
        handOver(to) // from now on this persona is the active one (its late audio is played normally)
        val audio: List<ByteArray>
        val text: String
        val done: Boolean
        synchronized(prefetchLock) {
            audio = ArrayList(prefetchAudio)
            text = prefetchText.toString()
            done = prefetchDone
            prefetchAudio.clear(); prefetchText.setLength(0); prefetchDone = false
            prefetchTo = null
        }
        DiagLog.log("GROUP", "early hand-over done: $to speaks now (${audio.size} audio chunks were ready, turn complete=$done)")
        for (c in audio) speaker?.enqueue(c)
        if (text.isNotEmpty()) addReplyText(text)
        if (done) {
            speaker?.turnComplete()
            main.postDelayed({ closeReply() }, 600L)
            if (groupMode && bridgeBudget > 0) bridgeFrom = to // and the first one answers next
        }
    }

    /** Like [switchNow] but nobody is cut off: the first voice has finished, so only the active persona changes. */
    private fun handOver(id: String) {
        if (id == persona) return
        main.removeCallbacks(endSpeaking)
        awaitingAnswer = false
        fillerPlayed = true // no stalling clip while the other persona prepares its reply
        inputBuf.setLength(0)
        closeReply()
        synchronized(ring) {
            persona = id
            prefs.persona = id
        }
        synchronized(routeLock) { routeDecided = true } // a text-driven reply has no user words to wait for
        listeners.forEach { it.onPersona(id) }
    }
}
