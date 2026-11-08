package com.arkitak.lite.wake

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.arkitak.lite.AssistantEngine
import com.arkitak.lite.FloatingOrbService
import com.arkitak.lite.MainActivity
import com.arkitak.lite.Prefs
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Owns the three wake engines. Rules:
 *  - ONE engine is active at a time. Selecting another stops the old one first.
 *  - NO silent switching: if the chosen engine fails, the reason is kept in [lastFailure] and shown; nothing else is started for you.
 *  - The choice and the on/off state are saved in Prefs and restored when the app is shown ([restore]).
 *  - A real detection starts the conversation (the persona that was named wakes up). Manual triggers do too ([onTrigger]).
 * UI calls must come from the main thread; engines may call [onWake] / [onStatusChanged] from any thread.
 */
object WakeEngineManager : WakeSink {

    private const val TAG = "WAKE"
    private const val DETECT_WINDOW_MS = 15_000L
    private const val READY_WAIT_MS = 90_000L

    interface Listener { fun onWakeStateChanged() }

    private val main = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<Listener>()
    private var inited = false
    private lateinit var app: Context
    private lateinit var prefs: Prefs

    lateinit var vosk: VoskEngine
        private set
    lateinit var manual: ManualTriggerEngine
        private set

    val engines: List<WakeEngine> get() = listOf(vosk, manual)

    @Volatile var lastFailure: String? = null
        private set
    @Volatile private var sessionActive = false
    @Volatile private var ownedByTrigger = false

    @Synchronized
    fun init(ctx: Context) {
        if (inited) return
        app = ctx.applicationContext
        prefs = Prefs(app)
        vosk = VoskEngine(app, this)
        manual = ManualTriggerEngine(app, this)
        inited = true
        DiagLog.log(TAG, "manager ready; chosen=${prefs.wakeEngine} enabled=${prefs.wakeEnabled}")
    }

    fun addListener(l: Listener) { listeners.addIfAbsent(l) }
    fun removeListener(l: Listener) { listeners.remove(l) }
    private fun notifyChanged() { main.post { listeners.forEach { DiagLog.safe("wake listener") { it.onWakeStateChanged() } } } }

    fun engine(id: String): WakeEngine? = engines.firstOrNull { it.id == id }
    val selectedId: String get() = prefs.wakeEngine
    val isEnabled: Boolean get() = prefs.wakeEnabled

    /** For crash reports and diagnostics. */
    fun describeActive(): String {
        if (!inited) return "not initialised"
        val on = engines.filter { it.status().isActive }
        return if (on.isEmpty()) "none running" else on.joinToString { "${it.id} (${it.status().code})" }
    }

    // ------------------------------------------------------------------ choose / switch on and off

    /** Pick the engine. The old one is stopped first; the new one starts only if wake mode is on. */
    fun select(id: String) {
        if (engine(id) == null) return
        engines.filter { it.id != id }.forEach { e -> DiagLog.safe("stop ${e.id}") { e.stop() } }
        prefs.wakeEngine = id
        lastFailure = null
        if (prefs.wakeEnabled && !sessionActive) startSelected()
        notifyChanged()
    }

    /** Wake mode on or off. On: the chosen engine starts (once, from a visible screen). Off: every engine stops. */
    fun setEnabled(on: Boolean) {
        prefs.wakeEnabled = on
        lastFailure = null
        if (on) { if (!sessionActive) startSelected() } else engines.forEach { e -> DiagLog.safe("stop ${e.id}") { e.stop() } }
        notifyChanged()
    }

    private fun startSelected(): StartResult {
        val e = engine(prefs.wakeEngine) ?: return StartResult(false, "no such engine")
        val r = try { e.start() } catch (t: Throwable) {
            DiagLog.log(TAG, "${e.id}.start threw", t)
            StartResult(false, "${t.javaClass.simpleName}: ${t.message}")
        }
        if (!r.ok) { lastFailure = "${e.title}: ${r.message}"; DiagLog.log(TAG, "start failed: $lastFailure") }
        notifyChanged()
        return r
    }

    /**
     * Called when the app comes to the screen (a visible moment, so Android allows starting the microphone service).
     * Restores the saved state. It never starts anything from the background.
     */
    fun restore() {
        if (!inited || !prefs.wakeEnabled || sessionActive) return
        val e = engine(prefs.wakeEngine) ?: return
        if (e.status().isActive) return
        DiagLog.log(TAG, "restoring ${e.id}")
        startSelected()
    }

    // ------------------------------------------------------------------ the conversation and the microphone

    /** The conversation is about to open the microphone: every wake engine lets go of it first. Main thread. */
    fun onSessionStarted() {
        sessionActive = true
        engines.forEach { e -> DiagLog.safe("pause ${e.id}") { e.pauseForSession() } }
    }

    /** The conversation ended: listen for the name again, if wake mode is on. Main thread. */
    fun onSessionEnded() {
        sessionActive = false
        ownedByTrigger = false
        if (!prefs.wakeEnabled) return
        val e = engine(prefs.wakeEngine) ?: return
        DiagLog.safe("resume ${e.id}") { e.resumeAfterSession() }
        notifyChanged()
    }

    // ------------------------------------------------------------------ detections

    override fun onWake(engineId: String, persona: String?, heard: String) {
        main.post {
            DiagLog.safe("onWake") {
                DiagLog.log(TAG, "detection from $engineId: '$heard' persona=$persona")
                val t = test
                if (t != null && t.engineId == engineId) {
                    finishTest(t, "Detected", "heard: $heard", SystemClock.elapsedRealtime() - t.detectFrom)
                    return@safe
                }
                if (engineId != prefs.wakeEngine || !prefs.wakeEnabled) {
                    DiagLog.log(TAG, "ignored: $engineId is not the active engine")
                    return@safe
                }
                startConversation(persona, engineId)
            }
        }
    }

    /**
     * A manual trigger (assistant gesture, assist activity, tile, widget). Always starts the conversation, whichever engine is chosen.
     * Returns true if THIS call started it (so the caller may stop it later; a conversation that was already running is never ours).
     */
    fun onTrigger(source: String, openUi: Boolean = true): Boolean {
        DiagLog.log(TAG, "manual trigger: $source")
        val t = test
        if (t != null && t.engineId == "manual") {
            finishTest(t, "Detected", "triggered by: $source", SystemClock.elapsedRealtime() - t.detectFrom)
            return false
        }
        val owned = startConversation(null, "manual: $source", openUi)
        ownedByTrigger = owned
        return owned
    }

    /** The session UI of the default-assistant gesture was dismissed: end the conversation it started. */
    fun stopConversationFromTrigger() {
        if (ownedByTrigger) { ownedByTrigger = false; AssistantEngine.get(app).stop() }
    }

    private fun startConversation(persona: String?, source: String, openUi: Boolean = true): Boolean {
        val engine = AssistantEngine.get(app)
        val started = engine.startFromWake(persona, source)
        if (openUi && !FloatingOrbService.appVisible && Diagnostics.overlayOk(app)) {
            // Android lets an app that may draw over other apps open its screen from the background. Without that permission only the voice works.
            DiagLog.safe("open ui") {
                app.startActivity(Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            }
        }
        return started
    }

    override fun onStatusChanged(engineId: String) {
        main.post {
            DiagLog.safe("status changed") {
                val t = test
                if (t != null && t.engineId == engineId) checkTestProgress(t)
                notifyChanged()
            }
        }
    }

    // ------------------------------------------------------------------ real detection test

    private class PendingTest(val engineId: String, val cb: (TestResult) -> Unit, val startedAt: Long) {
        var detectFrom = 0L // when the engine became ready; latency and the 15 s window count from here
        var timeout: Runnable? = null
    }

    @Volatile private var test: PendingTest? = null
    val testRunning: Boolean get() = test != null
    val testingEngine: String? get() = test?.engineId

    /**
     * Starts the engine and waits for a REAL detection (15 s after it is ready). Reports Detected / Timed out / Unsupported / Failed
     * with the reason and the latency. The previously running engine is stopped for the test and restored after it.
     */
    fun runTest(id: String, cb: (TestResult) -> Unit) {
        val e = engine(id) ?: return
        if (test != null) { cb(TestResult(id, "Failed", "another test is still running", 0, System.currentTimeMillis())); return }
        if (sessionActive) {
            cb(result(id, "Failed", "a conversation is using the microphone; stop it first (tap the orb)", 0)); return
        }
        engines.forEach { x -> DiagLog.safe("stop ${x.id}") { x.stop() } }
        val r = try { e.start() } catch (t: Throwable) { StartResult(false, "${t.javaClass.simpleName}: ${t.message}") }
        val t = PendingTest(id, cb, SystemClock.elapsedRealtime())
        test = t
        DiagLog.log(TAG, "test started: $id (start ok=${r.ok} ${r.message})")
        notifyChanged()
        checkTestProgress(t)
        if (test === t) main.postDelayed(object : Runnable {
            override fun run() { if (test === t) { checkTestProgress(t); main.postDelayed(this, 250) } }
        }, 250)
    }

    private fun checkTestProgress(t: PendingTest) {
        if (test !== t) return
        val e = engine(t.engineId) ?: return
        val st = e.status()
        val now = SystemClock.elapsedRealtime()
        when {
            st.isBlocking -> {
                val outcome = if (st is EngineStatus.Unsupported || st is EngineStatus.Unenrolled || st is EngineStatus.HardwareUnavailable) "Unsupported" else "Failed"
                finishTest(t, outcome, st.code + ": " + st.detail + (if (st.fix.isNotEmpty()) "  FIX: ${st.fix}" else ""), 0)
            }
            st is EngineStatus.Running && t.detectFrom == 0L -> {
                t.detectFrom = now
                val to = Runnable { if (test === t) finishTest(t, "Timed out", "no detection in ${DETECT_WINDOW_MS / 1000} s. ${hintForTimeout(t.engineId)}", 0) }
                t.timeout = to
                main.postDelayed(to, DETECT_WINDOW_MS)
            }
            t.detectFrom == 0L && now - t.startedAt > READY_WAIT_MS ->
                finishTest(t, "Failed", "the engine did not become ready in ${READY_WAIT_MS / 1000} s (${st.code})", 0)
        }
    }

    private fun hintForTimeout(id: String) = when (id) {
        "vosk" -> "Say \"Hey Olivia\" or \"Hey ARKITAK\" clearly, close to the phone. Try sensitivity Zyada. Read the log in the report."
        else -> "Trigger it yourself: the Quick Settings tile, the widget, or the assistant gesture."
    }

    private fun finishTest(t: PendingTest, outcome: String, reason: String, latency: Long) {
        if (test !== t) return
        test = null
        t.timeout?.let { main.removeCallbacks(it) }
        engine(t.engineId)?.let { e -> DiagLog.safe("stop after test") { e.stop() } }
        val res = result(t.engineId, outcome, reason, latency)
        DiagLog.log(TAG, "test ${t.engineId}: ${res.line()}")
        if (outcome != "Detected") lastFailure = "${t.engineId} test: ${res.line()}"
        // put back what was running before the test
        if (prefs.wakeEnabled && !sessionActive) startSelected()
        notifyChanged()
        DiagLog.safe("test callback") { t.cb(res) }
    }

    private fun result(id: String, outcome: String, reason: String, latency: Long): TestResult {
        val r = TestResult(id, outcome, reason, latency, System.currentTimeMillis())
        prefs.setLastTest(id, r.encode())
        return r
    }
}
