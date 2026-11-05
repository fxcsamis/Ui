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
 * speech starts and when it has REALLY finished (the audio the speaker has actually played, not just what was handed to it),
 * so the orb stays in "speaking" until the last word. [currentLevel] is the loudness of what is being heard right now,
 * so the lips move in time with the voice.
 */
class SpeakerStream(
    private val onPlaying: (Boolean) -> Unit,
    private val onLevel: (Float) -> Unit
) {
    companion object {
        const val SAMPLE_RATE = 24000
        private const val WINDOW_BYTES = 960 // 20 ms of 24 kHz 16-bit mono
    }

    private class Seg(val end: Long, val level: Float)

    private val queue = LinkedBlockingQueue<ByteArray>()
    private val running = AtomicBoolean(false)
    private val playing = AtomicBoolean(false)
    private val turnDone = AtomicBoolean(false)
    private val lock = Any()
    @Volatile private var track: AudioTrack? = null
    private var thread: Thread? = null

    // Frames handed to the AudioTrack vs frames it has played, and one loudness value per ~20 ms of audio.
    @Volatile private var framesWritten = 0L
    @Volatile private var headBase = 0L
    private val segs = ArrayList<Seg>()
    private var segFrames = 0L
    private var cursor = 0

    private fun newTrack(): AudioTrack {
        val min = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        return AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setBufferSizeInBytes(maxOf(min * 2, 24000)) // ~0.5 s: small, so what is written is close to what is heard
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private fun head(): Long {
        val t = track ?: return 0L
        return try { t.playbackHeadPosition.toLong() and 0xFFFFFFFFL } catch (_: Exception) { 0L }
    }

    private fun resetCounters() {
        headBase = head()
        framesWritten = 0L
        synchronized(segs) { segs.clear(); segFrames = 0L; cursor = 0 }
    }

    /** True when the speaker has played (almost) everything that was written to it. */
    private fun drained(): Boolean {
        val played = (head() - headBase) and 0xFFFFFFFFL
        return played + 480 >= framesWritten
    }

    private fun addLevels(chunk: ByteArray) {
        synchronized(segs) {
            var off = 0
            while (off < chunk.size) {
                val end = minOf(off + WINDOW_BYTES, chunk.size)
                var sum = 0.0
                var i = off
                while (i < end - 1) {
                    val v = (chunk[i + 1].toInt() shl 8) or (chunk[i].toInt() and 0xFF)
                    sum += (v * v).toDouble()
                    i += 2
                }
                val n = (end - off) / 2
                segFrames += n
                segs.add(Seg(segFrames, if (n > 0) (sqrt(sum / n) / 3500.0).toFloat().coerceIn(0f, 1f) else 0f))
                off = end
            }
        }
    }

    /** Loudness (0..1) of the audio that is coming out of the speaker right now. Safe to call from any thread. */
    fun currentLevel(): Float {
        if (!playing.get()) return 0f
        val played = (head() - headBase) and 0xFFFFFFFFL
        synchronized(segs) {
            while (cursor < segs.size && segs[cursor].end <= played) cursor++
            if (cursor >= segs.size) return 0f
            val a = segs[cursor].level
            val b = if (cursor + 1 < segs.size) segs[cursor + 1].level else a
            return (a + b) / 2f
        }
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
                        if (!playing.getAndSet(true)) { resetCounters(); onPlaying(true) }
                        addLevels(chunk)
                        write(chunk)
                    } else if (playing.get()) {
                        quiet++
                        // Finished only when the speaker has really played everything: the turn is complete and the queue stayed
                        // empty ~160 ms, or (stream stalled) no new audio for ~1.2 s. Hard stop after 10 s in case the track is stuck.
                        val done = quiet >= 8 && drained() && (turnDone.get() || quiet >= 60)
                        if (done || quiet >= 500) {
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
        var written = 0
        synchronized(lock) {
            val t = track ?: return
            if (t.playState != AudioTrack.PLAYSTATE_PLAYING) try { t.play() } catch (_: Exception) {}
            written = t.write(chunk, 0, chunk.size)
        }
        if (written > 0) framesWritten += written / 2
    }

    /** New audio means the turn is not over yet; [turnComplete] says no more is coming for this turn. */
    fun enqueue(pcm: ByteArray) { turnDone.set(false); queue.offer(pcm) }
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
        resetCounters()
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
