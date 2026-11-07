package com.arkitak.lite

import android.util.Base64
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Real-time voice conversation with Gemini Live over a WebSocket (the same BidiGenerateContent protocol the main ARKITAK app
 * uses): microphone audio (16 kHz PCM) goes up continuously, Gemini's spoken answer (24 kHz PCM) comes back as a stream,
 * and Gemini itself decides when you finished talking and can be interrupted.
 *
 * The session reconnects by itself (network drop, Google's periodic "go away") until [close] is called or the failure is
 * permanent (wrong API key, model not available), which is reported through [Listener.onFatal] instead of retrying forever.
 * Callbacks arrive on OkHttp threads.
 */
class GeminiLive(
    private val apiKey: String,
    private val model: String,
    private val voice: String,
    private val systemPrompt: String,
    private val listener: Listener
) {
    interface Listener {
        fun onStatus(text: String) {}
        fun onReady() {}
        fun onAudio(pcm24k: ByteArray) {}
        fun onInterrupted() {}
        fun onTurnComplete() {}
        fun onToolCall(id: String, name: String, args: JSONObject) {}
        fun onInputText(text: String) {}   // what the user said (speech-to-text), in pieces
        fun onOutputText(text: String) {}  // what the assistant says, in pieces
        fun onFatal(message: String) {}
    }

    companion object {
        private const val TAG = "GeminiLive"
        private const val WS_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"

        private val client: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)   // a live socket stays open; no read timeout
                .connectTimeout(8, TimeUnit.SECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .build()
        }

        /** Function Gemini may call when the user asks to show or hide the floating avatar. */
        private fun tools(): JSONArray = JSONArray().put(
            JSONObject().put(
                "functionDeclarations",
                JSONArray().put(
                    JSONObject()
                        .put("name", "set_floating_avatar")
                        .put("description", "Turns the small floating orb that stays on top of other apps on or off. When turned on, the app asks for the overlay permission if needed and opens a screen where the user picks a glass orb design. Call it when the user asks for the floating window / floating avatar / overlay.")
                        .put(
                            "parameters",
                            JSONObject().put("type", "OBJECT")
                                .put("properties", JSONObject().put("on", JSONObject().put("type", "BOOLEAN").put("description", "true to show it, false to hide it")))
                                .put("required", JSONArray().put("on"))
                        )
                )
                .put(
                    JSONObject()
                        .put("name", "open_wake_screen")
                        .put("description", "Opens the Wake word / wake detection screen of this app and, if enable is true, switches wake mode on so the app listens for the name (Hey Olivia / Hey ARKITAK) when idle. Call it when the user asks to turn on wake detection / wake word / hotword, or to open the wake or detection screen.")
                        .put(
                            "parameters",
                            JSONObject().put("type", "OBJECT")
                                .put("properties", JSONObject().put("enable", JSONObject().put("type", "BOOLEAN").put("description", "true to also switch wake mode on (default true)")))
                        )
                )
            )
        )

        /**
         * Connection test for the settings screen: opens a session, waits for Gemini to accept the setup and says exactly what
         * happened (works, wrong key, model not available, no internet). Blocks for up to ~10 s: call it off the main thread.
         */
        fun selfTest(apiKey: String, model: String, voice: String): String {
            if (apiKey.isBlank()) return "Enter the Gemini API key first."
            val latch = CountDownLatch(1)
            var result = "No answer from Google within 10 seconds. Check the internet connection."
            val session = GeminiLive(apiKey.trim(), normalizeModel(model), voice, "Test.", object : Listener {
                override fun onReady() { result = "OK. Gemini Live accepted the key, the model and the voice."; latch.countDown() }
                override fun onFatal(message: String) { result = message; latch.countDown() }
            })
            session.autoReconnect = false
            session.connect()
            latch.await(10, TimeUnit.SECONDS)
            session.close()
            return result
        }

        fun normalizeModel(m: String): String {
            val v = m.trim().ifEmpty { Prefs.DEFAULT_LIVE_MODEL }
            return if (v.startsWith("models/")) v else "models/$v"
        }
    }

    @Volatile private var socket: WebSocket? = null
    @Volatile private var ready = false
    private val closed = AtomicBoolean(false)
    private val generation = AtomicInteger(0) // every socket gets a number; callbacks of an old socket are ignored
    @Volatile private var autoReconnect = true
    @Volatile private var transcribe = true // speech-to-text of both sides; switched off by itself if the model refuses it
    @Volatile private var attempt = 0
    private val retryThread = java.util.concurrent.Executors.newSingleThreadScheduledExecutor()

    fun isReady(): Boolean = ready

    fun connect() {
        closed.set(false)
        open()
    }

    fun close() {
        closed.set(true)
        generation.incrementAndGet()
        ready = false
        try { socket?.close(1000, "bye") } catch (_: Exception) {}
        socket = null
        retryThread.shutdownNow()
    }

    private fun open() {
        if (closed.get()) return
        if (apiKey.isBlank()) { listener.onFatal("Gemini API key is missing."); return }
        listener.onStatus("Connecting...")
        ready = false
        val mine = generation.incrementAndGet()
        val request = Request.Builder().url("$WS_URL?key=$apiKey").build()
        val ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (mine != generation.get()) return
                sendSetup(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (mine == generation.get()) handle(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (mine == generation.get()) handle(bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (mine != generation.get()) return
                ready = false
                val r = reason.lowercase()
                when {
                    r.contains("api key") || r.contains("api_key") -> fatal("Google rejected the API key. Check it in Gemini settings.")
                    code == 1008 && transcribe -> { transcribe = false; retry("Retrying without transcripts") }
                    code == 1008 && (r.contains("not found") || r.contains("not supported") || r.contains("bidigeneratecontent")) ->
                        fatal("This model is not available for Live voice: ${reason.take(140)}. Pick another model in Gemini settings.")
                    code == 1008 -> fatal("Gemini refused the setup: ${reason.take(160)}")
                    else -> retry("Disconnected")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (mine != generation.get()) return
                ready = false
                val code = response?.code ?: 0
                val body = try { response?.body?.string().orEmpty() } catch (_: Exception) { "" }
                Log.w(TAG, "socket failure code=$code msg=${t.message}")
                when {
                    code == 400 || code == 401 || code == 403 || body.contains("API_KEY_INVALID", true) || body.contains("API key not valid", true) ->
                        fatal("Google rejected the API key (HTTP $code). Check it in Gemini settings.")
                    code == 429 -> retry("Too many requests, waiting")
                    else -> retry("No connection")
                }
            }
        })
        socket = ws
    }

    private fun fatal(message: String) {
        if (!closed.get()) listener.onFatal(message)
        if (!autoReconnect) return
        closed.set(true) // permanent: do not hammer Google with a key or model that will not work
        try { socket?.cancel() } catch (_: Exception) {}
    }

    private fun retry(status: String) {
        if (closed.get() || !autoReconnect) {
            if (!closed.get()) listener.onFatal("$status. Could not reach Gemini Live.")
            return
        }
        attempt++
        val delay = (1000L shl (attempt - 1).coerceAtMost(4)).coerceAtMost(15_000L) // 1s, 2s, 4s, 8s, 15s
        listener.onStatus("$status, reconnecting in ${delay / 1000}s...")
        try {
            retryThread.schedule(Runnable { open() }, delay, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
            // executor already shut down by close()
        }
    }

    private fun sendSetup(ws: WebSocket) {
        val setup = JSONObject().put(
            "setup",
            JSONObject().apply {
                put("model", model)
                // The server decides you stopped talking after ~0.45 s of silence.
                put("realtime_input_config", JSONObject().put("automatic_activity_detection", JSONObject().apply {
                    put("start_of_speech_sensitivity", "START_SENSITIVITY_HIGH")
                    put("end_of_speech_sensitivity", "END_SENSITIVITY_HIGH")
                    put("prefix_padding_ms", 40)
                    put("silence_duration_ms", 450)
                }))
                put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
                put("generation_config", JSONObject().apply {
                    put("response_modalities", JSONArray().put("AUDIO"))
                    put("speech_config", JSONObject().put("voice_config", JSONObject().put("prebuilt_voice_config", JSONObject().put("voice_name", voice))))
                    put("temperature", 0.6)
                    if (model.contains("2.5")) put("thinking_config", JSONObject().put("thinking_budget", 0))
                })
                put("tools", tools())
                if (transcribe) {
                    put("input_audio_transcription", JSONObject())
                    put("output_audio_transcription", JSONObject())
                }
            }
        )
        ws.send(setup.toString())
    }

    private fun handle(text: String) {
        try {
            val json = JSONObject(text)
            if (json.has("setupComplete")) {
                ready = true
                attempt = 0
                listener.onReady()
                return
            }
            if (json.has("goAway")) {
                // Google ends long sessions on purpose; open a fresh one right away.
                Log.i(TAG, "goAway received, reconnecting")
                ready = false
                try { socket?.close(1000, "renew") } catch (_: Exception) {}
                return
            }
            json.optJSONObject("error")?.let { e ->
                fatal("Gemini error: ${e.optString("message", "unknown").take(160)}")
                return
            }
            json.optJSONObject("toolCall")?.optJSONArray("functionCalls")?.let { calls ->
                for (i in 0 until calls.length()) {
                    val c = calls.getJSONObject(i)
                    val name = c.optString("name")
                    if (name.isNotEmpty()) listener.onToolCall(c.optString("id"), name, c.optJSONObject("args") ?: JSONObject())
                }
                return
            }
            val sc = json.optJSONObject("serverContent") ?: return
            sc.optJSONObject("inputTranscription")?.optString("text", "")?.let { if (it.isNotEmpty()) listener.onInputText(it) }
            sc.optJSONObject("outputTranscription")?.optString("text", "")?.let { if (it.isNotEmpty()) listener.onOutputText(it) }
            if (sc.optBoolean("interrupted", false)) listener.onInterrupted()
            val parts = sc.optJSONObject("modelTurn")?.optJSONArray("parts")
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val data = parts.getJSONObject(i).optJSONObject("inlineData")?.optString("data", "").orEmpty()
                    if (data.length > 8) {
                        val pcm = Base64.decode(data, Base64.DEFAULT)
                        val len = pcm.size and 1.inv() // 16-bit samples: keep an even number of bytes
                        if (len >= 16) listener.onAudio(if (len == pcm.size) pcm else pcm.copyOf(len))
                    }
                }
            }
            if (sc.optBoolean("turnComplete", false) || sc.optBoolean("generationComplete", false)) listener.onTurnComplete()
        } catch (e: Exception) {
            Log.w(TAG, "bad server message: ${e.message}")
        }
    }

    /** One chunk of 16 kHz 16-bit mono microphone audio. Ignored until the session is ready. */
    fun sendAudio(pcm16k: ByteArray) {
        if (!ready || pcm16k.isEmpty()) return
        val b64 = Base64.encodeToString(pcm16k, Base64.NO_WRAP)
        socket?.send("{\"realtimeInput\":{\"audio\":{\"mimeType\":\"audio/pcm;rate=16000\",\"data\":\"$b64\"}}}")
    }

    /** A line of text as the user's turn (used to have the voice record its stalling phrases). */
    fun sendText(text: String) {
        if (!ready || text.isBlank()) return
        socket?.send(JSONObject().put("realtimeInput", JSONObject().put("text", text)).toString())
    }

    fun sendToolResult(id: String, name: String, result: JSONObject) {
        val msg = JSONObject().put(
            "tool_response",
            JSONObject().put("function_responses", JSONArray().put(JSONObject().put("id", id).put("name", name).put("response", result)))
        )
        socket?.send(msg.toString())
    }
}
