package com.arkitak.lite.wake

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import com.arkitak.lite.MainActivity
import com.arkitak.lite.Personas
import com.arkitak.lite.Prefs
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * ENGINE B worker: a microphone foreground service that listens for "Hey Olivia" / "Hey ARKITAK" (or just the name) with Vosk.
 * One AudioRecord (VOICE_RECOGNITION, 16 kHz, mono, PCM16) on its own thread; the model is loaded on that thread too, never on the
 * main thread. The microphone indicator IS visible while this runs (that is the price of this fallback).
 *
 * The model is the HINDI small model, so the names are written in Devanagari in the grammar. All of these words were checked to exist
 * in the model's own word list: ओलिविया, आर्किटेक्ट, आर्कीटेक्ट, हे, हेय, हेलो. A grammar can only use words the model knows,
 * and the model never heard "ARKITAK", so "आर्किटेक्ट" (about "architect") is the closest sound. Expect misses and false hits: tune the
 * sensitivity on the Wake screen, and read the real result from the Test button.
 */
class VoskWakeService : Service() {

    companion object {
        private const val TAG = "VOSK"
        private const val CHANNEL = "wake_vosk"
        private const val NOTIF_ID = 31
        private const val ACTION_STOP = "com.arkitak.lite.STOP_VOSK_WAKE"
        private const val SAMPLE_RATE = 16000
        private const val COOLDOWN_MS = 3000L

        const val S_STOPPED = "STOPPED"
        const val S_LOADING = "LOADING"
        const val S_LISTENING = "LISTENING"
        const val S_PAUSED = "PAUSED"
        const val S_ERROR = "ERROR"

        @Volatile var state: String = S_STOPPED
        @Volatile var lastError: String? = null
        @Volatile var lastHeard: String = ""
        @Volatile var instance: VoskWakeService? = null

        private val OLIVIA_WORDS = listOf("ओलिविया")
        private val ARKITAK_WORDS = listOf("आर्किटेक्ट", "आर्कीटेक्ट")
        private val HEYS = listOf("हे", "हेय", "हेलो")

        /** Starts the foreground service. Android 14+ refuses a microphone foreground service from the background: report it, never hide it. */
        fun start(ctx: Context): StartResult {
            return try {
                ctx.startForegroundService(Intent(ctx, VoskWakeService::class.java))
                StartResult(true, "service starting")
            } catch (t: Throwable) {
                val blocked = t.javaClass.simpleName.contains("ForegroundServiceStartNotAllowed") || t is SecurityException || t is IllegalStateException
                val msg = (if (blocked) "Android blocked the microphone service from the background. Open the app and start it from the Wake screen. " else "") +
                    "${t.javaClass.simpleName}: ${t.message}"
                lastError = msg
                state = S_ERROR
                DiagLog.log(TAG, "start failed: $msg")
                StartResult(false, msg)
            }
        }

        fun stop(ctx: Context) {
            instance?.shutdown() ?: try { ctx.stopService(Intent(ctx, VoskWakeService::class.java)) } catch (_: Throwable) {}
            if (instance == null) state = S_STOPPED
        }
    }

    private val main = Handler(Looper.getMainLooper())
    private val lock = Any()
    private var worker: Thread? = null
    @Volatile private var wantListening = false
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var lastWakeAt = 0L
    private lateinit var prefs: Prefs

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            DiagLog.safe("vosk stop action") {
                main.post { WakeEngineManager.setEnabled(false) }
                shutdown()
            }
            return START_NOT_STICKY
        }
        try {
            val n = buildNotification()
            if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            else startForeground(NOTIF_ID, n)
        } catch (t: Throwable) {
            lastError = "startForeground failed: ${t.javaClass.simpleName}: ${t.message}"
            state = S_ERROR
            DiagLog.log(TAG, lastError!!, t)
            WakeEngineManager.onStatusChanged("vosk")
            stopSelf()
            return START_NOT_STICKY
        }
        DiagLog.safe("vosk begin") { beginListening() }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        DiagLog.safe("vosk onDestroy") { cleanup() }
        instance = null
        if (state != S_ERROR) state = S_STOPPED
        WakeEngineManager.onStatusChanged("vosk")
        super.onDestroy()
    }

    // ------------------------------------------------------------------ control (idempotent, synchronized)

    private fun beginListening() = synchronized(lock) {
        wantListening = true
        if (worker?.isAlive == true) return@synchronized
        lastError = null
        worker = Thread({ runWorker() }, "vosk-wake").also { it.isDaemon = true; it.start() }
    }

    /** A conversation needs the microphone: release it (the model stays loaded so resuming is instant). */
    fun pauseListening() {
        val t: Thread?
        synchronized(lock) {
            wantListening = false
            t = worker
        }
        try { t?.join(900) } catch (_: InterruptedException) {}
        if (state == S_LISTENING || state == S_LOADING) state = S_PAUSED
        DiagLog.log(TAG, "paused (microphone released)")
        WakeEngineManager.onStatusChanged("vosk")
    }

    fun resumeListening() {
        if (state == S_ERROR && recognizer == null) lastError = null
        DiagLog.log(TAG, "resuming")
        beginListening()
    }

    fun shutdown() {
        DiagLog.safe("vosk shutdown") { cleanup() }
        state = if (state == S_ERROR) S_ERROR else S_STOPPED
        try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (_: Throwable) {}
        stopSelf()
        WakeEngineManager.onStatusChanged("vosk")
    }

    private fun cleanup() {
        val t: Thread?
        synchronized(lock) {
            wantListening = false
            t = worker
        }
        try { t?.join(1200) } catch (_: InterruptedException) {}
        synchronized(lock) {
            worker = null
            try { recognizer?.close() } catch (_: Throwable) {}
            try { model?.close() } catch (_: Throwable) {}
            recognizer = null
            model = null
        }
    }

    // ------------------------------------------------------------------ the worker thread

    private fun runWorker() {
        try {
            if (recognizer == null) {
                state = S_LOADING
                WakeEngineManager.onStatusChanged("vosk")
                LibVosk.setLogLevel(LogLevel.WARNINGS)
                val dir = VoskModelStore.ensure(this)              // copy on first run (background thread)
                val m = Model(dir.absolutePath)                    // loading takes a few seconds (background thread)
                model = m
                recognizer = Recognizer(m, SAMPLE_RATE.toFloat(), grammar())
                DiagLog.log(TAG, "model loaded; grammar=${grammar()}")
            }
            recognizer?.reset()
            state = S_LISTENING
            WakeEngineManager.onStatusChanged("vosk")
            listenLoop()
            if (state == S_LISTENING) state = S_PAUSED
        } catch (t: Throwable) {
            lastError = "${t.javaClass.simpleName}: ${t.message}"
            state = S_ERROR
            DiagLog.log(TAG, "worker failed", t)
        }
        WakeEngineManager.onStatusChanged("vosk")
    }

    private fun grammar(): String {
        val names = OLIVIA_WORDS + ARKITAK_WORDS
        val phrases = ArrayList<String>()
        phrases.addAll(names)
        for (h in HEYS) for (n in names) phrases.add("$h $n")
        phrases.add("[unk]")
        return org.json.JSONArray(phrases).toString()
    }

    private fun threshold(): Int = when (prefs.voskSensitivity) { 0 -> 600; 2 -> 120; else -> 300 }

    private fun listenLoop() {
        val rec = recognizer ?: return
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val buf = ShortArray(max(minBuf / 2, 4000))
        var errors = 0
        var gateUntil = 0L
        var fedSomething = false

        while (wantListening) {
            var ar: AudioRecord? = null
            try {
                ar = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(minBuf, SAMPLE_RATE * 2)
                )
                if (ar.state != AudioRecord.STATE_INITIALIZED) throw IllegalStateException("AudioRecord not initialized (is another app using the microphone?)")
                ar.startRecording()
                if (ar.recordingState != AudioRecord.RECORDSTATE_RECORDING) throw IllegalStateException("AudioRecord did not start recording")
                errors = 0
                DiagLog.log(TAG, "microphone open (VOICE_RECOGNITION 16 kHz mono)")

                while (wantListening) {
                    val n = ar.read(buf, 0, buf.size)
                    if (n < 0) { DiagLog.log(TAG, "AudioRecord.read error $n"); break }
                    if (n == 0) { Thread.sleep(10); continue }
                    val now = SystemClock.elapsedRealtime()
                    // Sensitivity: a noise gate on the loudness (RMS). Quiet audio is not fed to the recognizer, which saves battery.
                    if (rms(buf, n) >= threshold()) gateUntil = now + 1000
                    if (now < gateUntil) {
                        val finished = rec.acceptWaveForm(buf, n)
                        fedSomething = true
                        val text = if (finished) parse(rec.result, "text") else parse(rec.partialResult, "partial")
                        if (text.isNotEmpty() && handleText(text, rec)) fedSomething = false
                    } else if (fedSomething) {
                        // the gate just closed: flush what was heard so a short name is not left hanging
                        val text = parse(rec.finalResult, "text")
                        fedSomething = false
                        if (text.isNotEmpty()) handleText(text, rec)
                        rec.reset()
                    }
                }
            } catch (e: InterruptedException) {
                break
            } catch (t: Throwable) {
                DiagLog.log(TAG, "microphone problem", t)
                errors++
                if (errors > 6) throw IllegalStateException("the microphone keeps failing: ${t.message}")
                try { Thread.sleep(min(8000L, 500L shl errors)) } catch (_: InterruptedException) { break }
            } finally {
                try { ar?.stop() } catch (_: Throwable) {}
                try { ar?.release() } catch (_: Throwable) {}
                DiagLog.log(TAG, "microphone released")
            }
        }
    }

    private fun rms(b: ShortArray, n: Int): Double {
        var s = 0.0
        for (i in 0 until n) { val v = b[i].toDouble(); s += v * v }
        return sqrt(s / max(1, n))
    }

    private fun parse(json: String, key: String): String = try {
        JSONObject(json).optString(key, "").trim()
    } catch (_: Throwable) { "" }

    /** True when it was a wake word (and the recognizer was reset). */
    private fun handleText(text: String, rec: Recognizer): Boolean {
        if (text == "[unk]") return false
        val words = text.split(" ").filter { it.isNotBlank() }
        val persona: String? = when {
            words.any { it in ARKITAK_WORDS } && words.any { it in OLIVIA_WORDS } ->
                if (words.indexOfFirst { it in ARKITAK_WORDS } > words.indexOfFirst { it in OLIVIA_WORDS }) Personas.ARKITAK else Personas.OLIVIA
            words.any { it in ARKITAK_WORDS } -> Personas.ARKITAK
            words.any { it in OLIVIA_WORDS } -> Personas.OLIVIA
            else -> null
        }
        if (persona == null) return false
        val now = SystemClock.elapsedRealtime()
        if (now - lastWakeAt < COOLDOWN_MS) return false
        lastWakeAt = now
        lastHeard = text
        rec.reset()
        DiagLog.log(TAG, "WAKE heard '$text' -> $persona")
        if (prefs.voskSound) DiagLog.safe("vosk tone") {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70).startTone(ToneGenerator.TONE_PROP_ACK, 120)
        }
        main.post { DiagLog.safe("vosk onWake") { WakeEngineManager.onWake("vosk", persona, text) } }
        return true
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Wake word", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getService(
            this, 3, Intent(this, VoskWakeService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val action = Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel), "Turn off", stop).build()
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Waiting for \"Olivia\" or \"ARKITAK\"")
            .setContentText("Wake word is on (Vosk). The microphone indicator is visible.")
            .setContentIntent(open)
            .addAction(action)
            .setOngoing(true)
            .build()
    }
}
