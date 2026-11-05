package com.arkitak.lite

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

private const val AUDIO_TAG = "LiveAudio"

/** 0..1 loudness of 16-bit little-endian mono PCM (same scale as the main ARKITAK app). */
internal fun pcmLevel(bytes: ByteArray, length: Int): Float {
    val samples = length / 2
    if (samples == 0) return 0f
    var sum = 0.0
    var i = 0
    while (i < length - 1) {
        val s = (bytes[i + 1].toInt() shl 8) or (bytes[i].toInt() and 0xFF)
        sum += (s * s).toDouble()
        i += 2
    }
    return (sqrt(sum / samples) / 3500.0).toFloat().coerceIn(0f, 1f)
}

/**
 * Microphone, 16 kHz mono, in 20 ms chunks. Uses the VOICE_COMMUNICATION source with the phone's echo canceller, noise
 * suppressor and gain control, so Gemini hears less of its own voice. If Android stops the microphone it is reopened.
 */
class MicStream(private val onChunk: (chunk: ByteArray, level: Float) -> Unit) {
    companion object {
        const val SAMPLE_RATE = 16000
        const val CHUNK_BYTES = 640 // 20 ms
    }

    private var record: AudioRecord? = null
    private var effects: List<android.media.audiofx.AudioEffect> = emptyList()
    private val running = AtomicBoolean(false)
    private var thread: Thread? = null

    @SuppressLint("MissingPermission")
    private fun open(): AudioRecord? {
        val min = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) return null
        val size = maxOf(min * 4, 32000)
        repeat(4) { attempt ->
            val r = try {
                AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size)
            } catch (e: Exception) { null }
            if (r != null && r.state == AudioRecord.STATE_INITIALIZED) {
                val list = ArrayList<android.media.audiofx.AudioEffect>()
                try { if (AcousticEchoCanceler.isAvailable()) AcousticEchoCanceler.create(r.audioSessionId)?.also { it.enabled = true }?.let { list.add(it) } } catch (_: Exception) {}
                try { if (NoiseSuppressor.isAvailable()) NoiseSuppressor.create(r.audioSessionId)?.also { it.enabled = true }?.let { list.add(it) } } catch (_: Exception) {}
                try { if (AutomaticGainControl.isAvailable()) AutomaticGainControl.create(r.audioSessionId)?.also { it.enabled = true }?.let { list.add(it) } } catch (_: Exception) {}
                effects = list
                r.startRecording()
                return r
            }
            r?.release()
            try { Thread.sleep((attempt + 1) * 80L) } catch (_: InterruptedException) {}
        }
        return null
    }

    private fun closeRecord() {
        effects.forEach { try { it.release() } catch (_: Exception) {} }
        effects = emptyList()
        try { record?.stop() } catch (_: Exception) {}
        try { record?.release() } catch (_: Exception) {}
        record = null
    }

    /** Returns false when the microphone could not be opened (permission missing, or another app is using it). */
    fun start(): Boolean {
        if (running.get()) return true
        record = open() ?: return false
        running.set(true)
        thread = Thread {
            try { android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO) } catch (_: Exception) {}
            val buf = ByteArray(CHUNK_BYTES)
            var errors = 0
            while (running.get()) {
                val r = record
                val n = if (r != null) r.read(buf, 0, buf.size) else -1
                if (n > 0) {
                    errors = 0
                    try { onChunk(buf.copyOf(n), pcmLevel(buf, n)) } catch (e: Exception) { Log.w(AUDIO_TAG, "chunk handler: ${e.message}") }
                } else {
                    errors++
                    try { Thread.sleep(40) } catch (_: InterruptedException) { break }
                    if (errors > 10 && running.get()) { // the microphone was taken away: reopen it
                        Log.w(AUDIO_TAG, "microphone read errors, reopening")
                        closeRecord()
                        try { Thread.sleep(400) } catch (_: InterruptedException) { break }
                        record = open()
                        errors = 0
                    }
                }
            }
        }.also { it.isDaemon = true; it.start() }
        return true
    }

    fun stop() {
        running.set(false)
        thread?.interrupt()
        thread = null
        closeRecord()
    }
}

/**
 * Plays Gemini's 24 kHz mono voice as it arrives. [flush] cuts the voice instantly (interruption). [onPlaying] fires when
 * speech starts and when it has really finished; [onLevel] reports loudness so the orb can pulse with the voice.
 */
class SpeakerStream(
    private val onPlaying: (Boolean) -> Unit,
    private val onLevel: (Float) -> Unit
) {
    companion object { const val SAMPLE_RATE = 24000 }

    private val queue = LinkedBlockingQueue<ByteArray>()
    private val running = AtomicBoolean(false)
    private val playing = AtomicBoolean(false)
    private val turnDone = AtomicBoolean(false)
    private val lock = Any()
    private var track: AudioTrack? = null
    private var thread: Thread? = null

    private fun newTrack(): AudioTrack {
        val min = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        return AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setBufferSizeInBytes(maxOf(min * 8, 96000))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    fun start() {
        if (running.getAndSet(true)) return
        synchronized(lock) { track = newTrack() }
        thread = Thread {
            try { android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO) } catch (_: Exception) {}
            var quiet = 0
            while (running.get()) {
                try {
                    val chunk = queue.poll(20, TimeUnit.MILLISECONDS)
                    if (chunk != null && chunk.isNotEmpty()) {
                        quiet = 0
                        if (!playing.getAndSet(true)) { turnDone.set(false); onPlaying(true) }
                        onLevel(pcmLevel(chunk, chunk.size))
                        write(chunk)
                    } else if (playing.get()) {
                        quiet++
                        // finished: Gemini said the turn is complete and the queue stayed empty ~160 ms, or no audio for ~800 ms
                        if ((turnDone.get() && quiet >= 8) || quiet >= 40) {
                            playing.set(false)
                            onLevel(0f)
                            onPlaying(false)
                        }
                    }
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.w(AUDIO_TAG, "playback: ${e.message}")
                }
            }
        }.also { it.isDaemon = true; it.priority = Thread.MAX_PRIORITY; it.start() }
    }

    private fun write(chunk: ByteArray) {
        synchronized(lock) {
            val t = track ?: return
            if (t.playState != AudioTrack.PLAYSTATE_PLAYING) try { t.play() } catch (_: Exception) {}
            t.write(chunk, 0, chunk.size)
        }
    }

    fun enqueue(pcm: ByteArray) { queue.offer(pcm) }
    fun turnComplete() { turnDone.set(true) }
    fun isPlaying(): Boolean = playing.get() || queue.isNotEmpty()

    /** Stops the voice right now and throws away what was not played yet. */
    fun flush() {
        queue.clear()
        turnDone.set(true)
        // Not under [lock]: the playback thread may be blocked inside write(), and pause()/flush() are what unblock it.
        val t = track
        try { t?.pause() } catch (_: Exception) {}
        try { t?.flush() } catch (_: Exception) {}
        if (playing.getAndSet(false)) { onLevel(0f); onPlaying(false) }
    }

    fun release() {
        running.set(false)
        thread?.interrupt()
        thread = null
        queue.clear()
        synchronized(lock) {
            try { track?.stop() } catch (_: Exception) {}
            try { track?.release() } catch (_: Exception) {}
            track = null
        }
        playing.set(false)
    }
}
