package com.example.data

import android.content.Context
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.util.Locale
import java.util.UUID

class AudioPlaybackManager(context: Context) {

    private val appContext = context.applicationContext

    // Media3 ExoPlayer for online audio streams
    private var exoPlayer: ExoPlayer? = null

    // Android Text-To-Speech fallback
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        exoPlayer = ExoPlayer.Builder(appContext).build()
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ttsReady = true
            }
        }
    }

    /**
     * Requirement 7:
     * If audio_url exists and is not blank, play it with Media3 ExoPlayer;
     * otherwise fallback to Android Text-to-Speech.
     */
    fun playAudio(
        audioUrl: String?,
        textFallback: String,
        isBritish: Boolean = false,
        playbackSpeed: Float = 1.0f
    ) {
        val cleanUrl = audioUrl?.trim().orEmpty()
        if (cleanUrl.isNotBlank() && (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://"))) {
            try {
                exoPlayer?.stop()
                exoPlayer?.clearMediaItems()
                val mediaItem = MediaItem.fromUri(Uri.parse(cleanUrl))
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.playbackParameters = androidx.media3.common.PlaybackParameters(playbackSpeed)
                exoPlayer?.prepare()
                exoPlayer?.play()
                return
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to TTS if ExoPlayer encounters an error
            }
        }

        // Fallback to Android TTS
        speakWithTts(textFallback, isBritish, playbackSpeed)
    }

    fun speakWithTts(text: String, isBritish: Boolean = false, playbackSpeed: Float = 1.0f) {
        if (!ttsReady || text.isBlank()) return
        tts?.language = if (isBritish) Locale.UK else Locale.US
        tts?.setSpeechRate(playbackSpeed)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun stop() {
        try {
            exoPlayer?.stop()
        } catch (_: Exception) {}
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    fun release() {
        stop()
        exoPlayer?.release()
        exoPlayer = null
        tts?.shutdown()
        tts = null
    }
}
