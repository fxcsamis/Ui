package com.arkitak.lite

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.LinkedList
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Short "hmm... darao, ektu dekhi..." clips in Olivia's own voice, played while Gemini is slow to answer (a long think,
 * a lookup), so there is never dead air. The clips are recorded once through a separate, throw-away Gemini Live session
 * (same voice, same emotion), saved in the app's private storage, and played locally afterwards: instant, no network.
 * If recording fails nothing breaks: there is just no stalling clip.
 */
class FillerBank(private val ctx: Context, private val prefs: Prefs) {

    companion object {
        private val LINES_BN = listOf(
            "হুমম… দাঁড়াও, একটু দেখি…",
            "উমম… একটু ভেবে নিই, দাঁড়াও…",
            "ওহ… আচ্ছা… দাঁড়াও, দেখছি…",
            "হুমম, এক সেকেন্ড… আমি একটু দেখে নিই…",
            "হিহি… দাঁড়াও না, একটু দেখি তো…"
        )
        private val LINES_EN = listOf(
            "Hmm… hold on, let me see…",
            "Umm… give me a second, let me think…",
            "Ohh… okay… let me check that…",
            "Hmm, one sec… let me look…",
            "Hehe… wait, let me see…"
        )
        private const val RECORDER_PROMPT =
            "You are Olivia, a warm, playful young woman with a soft, slightly husky, expressive voice. " +
            "Whenever a message starts with 'Say exactly this', speak only the text after it, aloud, with natural feeling " +
            "(a small breath, a soft hum or a little laugh where the text suggests one), and then stop. Never add any other words."
    }

    private val main = Handler(Looper.getMainLooper())
    private val clips = CopyOnWriteArrayList<ByteArray>()
    private var loadedKey = ""
    private var lastPick = -1
    @Volatile private var job: Job? = null

    private fun english() = prefs.lang == "en"
    private fun key() = "${prefs.voice}_${if (english()) "en" else "bn"}"
    private fun lines() = if (english()) LINES_EN else LINES_BN
    private fun dirFor(k: String) = File(ctx.filesDir, "fillers/$k").also { it.mkdirs() }

    private fun load() {
        val k = key()
        if (k == loadedKey) return
        loadedKey = k
        clips.clear()
        dirFor(k).listFiles { f -> f.name.endsWith(".pcm") }?.sortedBy { it.name }?.forEach { f ->
            try { val b = f.readBytes(); if (b.size > 6000) clips.add(b) } catch (_: Exception) {}
        }
    }

    /** A random clip (never the same one twice in a row), or null if none has been recorded yet. */
    fun pick(): ByteArray? {
        load()
        if (clips.isEmpty()) return null
        var i = (Math.random() * clips.size).toInt().coerceIn(0, clips.size - 1)
        if (clips.size > 1 && i == lastPick) i = (i + 1) % clips.size
        lastPick = i
        return clips[i]
    }

    /** Records the missing clips in the background (once per voice and language). */
    fun ensure() {
        load()
        if (job != null || prefs.apiKey.isBlank()) return
        val k = key()
        val have = dirFor(k).listFiles()?.map { it.name }?.toSet() ?: emptySet()
        val todo = LinkedList<Int>()
        lines().indices.forEach { if ("f$it.pcm" !in have) todo.add(it) }
        if (todo.isEmpty()) return
        val j = Job(k, lines(), todo)
        job = j
        j.begin()
    }

    fun cancel() { job?.finish() }

    private fun trim(pcm: ByteArray): ByteArray {
        val n = pcm.size / 2
        fun s(i: Int) = (pcm[2 * i + 1].toInt() shl 8) or (pcm[2 * i].toInt() and 0xFF)
        var a = 0
        while (a < n && abs(s(a)) < 250) a++
        var b = n - 1
        while (b > a && abs(s(b)) < 250) b--
        a = max(0, a - 1200)          // keep 50 ms before
        b = min(n - 1, b + 2400)      // and 100 ms after
        if (b <= a) return ByteArray(0)
        return pcm.copyOfRange(a * 2, (b + 1) * 2)
    }

    private inner class Job(val k: String, val lines: List<String>, val todo: LinkedList<Int>) : GeminiLive.Listener {
        private val buf = ByteArrayOutputStream()
        private var current = -1
        @Volatile private var waiting = false
        @Volatile private var live: GeminiLive? = null
        private val timeout = Runnable { finish() }

        fun begin() {
            val l = GeminiLive(prefs.apiKey, GeminiLive.normalizeModel(prefs.liveModel), prefs.voice, RECORDER_PROMPT, this)
            live = l
            l.connect()
            main.postDelayed(timeout, 90_000)
        }

        override fun onReady() { sendNext() }

        private fun sendNext() {
            val idx: Int? = todo.poll()
            if (idx == null) { finish(); return }
            synchronized(buf) { buf.reset() }
            current = idx
            waiting = true
            live?.sendText("Say exactly this: " + lines[idx])
        }

        override fun onAudio(pcm24k: ByteArray) {
            if (waiting) synchronized(buf) { buf.write(pcm24k) }
        }

        override fun onTurnComplete() {
            if (!waiting) return
            waiting = false
            val raw = synchronized(buf) { buf.toByteArray() }
            val pcm = trim(raw)
            if (pcm.size in 6000..(SpeakerStream.SAMPLE_RATE * 2 * 8)) {
                try { File(dirFor(k), "f$current.pcm").writeBytes(pcm) } catch (_: Exception) {}
                if (k == loadedKey) clips.add(pcm)
            }
            // The server may send "generation complete" and "turn complete" one after the other: wait before the next line.
            main.postDelayed({ if (job === this) sendNext() }, 700)
        }

        override fun onFatal(message: String) { finish() }

        fun finish() {
            main.removeCallbacks(timeout)
            try { live?.close() } catch (_: Exception) {}
            live = null
            if (job === this) job = null
        }
    }
}
