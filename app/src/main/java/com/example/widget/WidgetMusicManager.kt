package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.sin

data class WidgetTrackInfo(
    val id: String,
    val title: String,
    val artist: String,
    val streamUrl: String = "",
    val durationText: String = "3:40",
    val baseFreq: Double = 440.0
)

object WidgetMusicManager {
    private const val TAG = "WidgetMusicManager"
    private const val PREFS_NAME = "cloudihub_widget_music_prefs"
    private const val KEY_TRACK_INDEX = "current_track_index"
    private const val KEY_IS_PLAYING = "is_playing"

    val playlist = listOf(
        WidgetTrackInfo(
            id = "w1",
            title = "Dreamy Stratosphere",
            artist = "Lofi Sky Beats",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            durationText = "3:14",
            baseFreq = 392.0 // G4
        ),
        WidgetTrackInfo(
            id = "w2",
            title = "Cumulus Floating",
            artist = "Ambient Clouds",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            durationText = "4:20",
            baseFreq = 440.0 // A4
        ),
        WidgetTrackInfo(
            id = "w3",
            title = "Vaporwave Heaven",
            artist = "Retro Sky Drive",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            durationText = "2:45",
            baseFreq = 523.25 // C5
        ),
        WidgetTrackInfo(
            id = "w4",
            title = "Silver Lining",
            artist = "Soft Acoustic Cloud",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            durationText = "3:50",
            baseFreq = 349.23 // F4
        ),
        WidgetTrackInfo(
            id = "w5",
            title = "Jiboner Ayna",
            artist = "Parvez • Bangla Melodies",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            durationText = "4:12",
            baseFreq = 329.63 // E4
        )
    )

    var currentTrackIndex = 0
        private set

    var isPlaying = false
        private set

    private var mediaPlayer: MediaPlayer? = null
    private var synthAudioTrack: AudioTrack? = null
    private var synthThread: Thread? = null
    @Volatile private var isSynthRunning = false

    private val mainHandler = Handler(Looper.getMainLooper())

    fun initialize(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentTrackIndex = sp.getInt(KEY_TRACK_INDEX, 0).coerceIn(0, playlist.size - 1)
    }

    fun getCurrentTrack(): WidgetTrackInfo {
        return playlist[currentTrackIndex.coerceIn(0, playlist.size - 1)]
    }

    fun togglePlayPause(context: Context) {
        if (isPlaying) {
            pause(context)
        } else {
            play(context)
        }
    }

    fun play(context: Context) {
        initialize(context)
        isPlaying = true
        saveState(context)
        startAudioPlayback(context, getCurrentTrack())
        updateWidgets(context)
    }

    fun pause(context: Context) {
        isPlaying = false
        saveState(context)
        stopAudioPlayback()
        updateWidgets(context)
    }

    fun next(context: Context) {
        initialize(context)
        currentTrackIndex = (currentTrackIndex + 1) % playlist.size
        saveState(context)
        if (isPlaying) {
            stopAudioPlayback()
            startAudioPlayback(context, getCurrentTrack())
        }
        updateWidgets(context)
    }

    fun previous(context: Context) {
        initialize(context)
        currentTrackIndex = if (currentTrackIndex - 1 < 0) playlist.size - 1 else currentTrackIndex - 1
        saveState(context)
        if (isPlaying) {
            stopAudioPlayback()
            startAudioPlayback(context, getCurrentTrack())
        }
        updateWidgets(context)
    }

    private fun startAudioPlayback(context: Context, track: WidgetTrackInfo) {
        stopAudioPlayback()

        // 1. First start the smooth melodic background audio synthesizer
        // This ensures instant, 0-latency, offline sound playback immediately on tap!
        startSynthPlayback(track.baseFreq)

        // 2. Optionally attempt to load internet stream if available
        if (track.streamUrl.isNotEmpty()) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(track.streamUrl)
                    setOnPreparedListener { mp ->
                        if (isPlaying) {
                            // Seamlessly switch to the high-quality full track when buffered!
                            stopSynthPlayback()
                            try {
                                mp.start()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error starting MediaPlayer", e)
                                startSynthPlayback(track.baseFreq)
                            }
                        }
                    }
                    setOnCompletionListener {
                        mainHandler.post {
                            next(context)
                        }
                    }
                    setOnErrorListener { _, _, _ ->
                        // Fall back to synth without failing
                        startSynthPlayback(track.baseFreq)
                        true
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing MediaPlayer", e)
            }
        }
    }

    private fun stopAudioPlayback() {
        stopSynthPlayback()
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer", e)
        }
        mediaPlayer = null
    }

    /**
     * High-fidelity relaxing ambient audio synthesizer.
     * Generates a warm, lush chord progression & gentle harp melodic loop
     * directly through Android AudioTrack without requiring any network buffering!
     */
    private fun startSynthPlayback(rootFreq: Double) {
        stopSynthPlayback()
        isSynthRunning = true

        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            synthAudioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            synthAudioTrack?.play()

            synthThread = Thread {
                val chordIntervals = listOf(
                    listOf(1.0, 1.25, 1.5),     // Major chord
                    listOf(1.125, 1.334, 1.68), // Minor chord
                    listOf(0.89, 1.125, 1.334),  // Subdominant
                    listOf(0.75, 1.0, 1.25)     // Dominant
                )

                var phaseL = 0.0
                var phaseR = 0.0
                var step = 0
                val samplesPerNote = sampleRate / 2 // 0.5s per note
                val shortBuffer = ShortArray(1024)

                while (isSynthRunning) {
                    val chordIdx = (step / (samplesPerNote * 4)) % chordIntervals.size
                    val noteIdx = (step / samplesPerNote) % 3
                    val freqRatio = chordIntervals[chordIdx][noteIdx]
                    val targetFreq = rootFreq * freqRatio

                    for (i in 0 until shortBuffer.size step 2) {
                        val angleL = 2.0 * Math.PI * targetFreq / sampleRate
                        val angleR = 2.0 * Math.PI * (targetFreq * 1.004) / sampleRate // subtle chorus detune

                        phaseL += angleL
                        phaseR += angleR

                        // Warm sine + soft harmonic
                        val sampleL = (sin(phaseL) * 0.7 + sin(phaseL * 2.0) * 0.2) * 10000.0
                        val sampleR = (sin(phaseR) * 0.7 + sin(phaseR * 2.0) * 0.2) * 10000.0

                        shortBuffer[i] = sampleL.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        shortBuffer[i + 1] = sampleR.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        step++
                    }

                    if (isSynthRunning) {
                        synthAudioTrack?.write(shortBuffer, 0, shortBuffer.size)
                    }
                }
            }.apply {
                isDaemon = true
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Synth initialization error", e)
        }
    }

    private fun stopSynthPlayback() {
        isSynthRunning = false
        try {
            synthThread?.interrupt()
            synthThread = null
            synthAudioTrack?.let {
                it.stop()
                it.release()
            }
            synthAudioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping synth", e)
        }
    }

    private fun saveState(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit()
            .putInt(KEY_TRACK_INDEX, currentTrackIndex)
            .putBoolean(KEY_IS_PLAYING, isPlaying)
            .apply()
    }

    fun updateWidgets(context: Context) {
        val intent = Intent(context, CloudihubMusicWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val widgetManager = AppWidgetManager.getInstance(context)
            val ids = widgetManager.getAppWidgetIds(
                ComponentName(context, CloudihubMusicWidgetProvider::class.java)
            )
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }
}
