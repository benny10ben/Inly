package com.ben.inly.domain.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

/**
 * A utility class that handles recording and playing back voice notes.
 */
class AudioHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentFile: File? = null
    private var recordingStartTime = 0L

    /**
     * Starts recording audio from the device microphone.
     * This uses a high-fidelity AAC encoder (128 kbps, 44.1 kHz) to ensure the audio is
     * crisp and prevents the aggressive chopping that happens on lower settings.
     */
    fun startRecording() {
        val fileName = "voice_${UUID.randomUUID()}.m4a"
        currentFile = File(context.filesDir, fileName)

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)

            setOutputFile(currentFile!!.absolutePath)
            prepare()
            start()
        }
        recordingStartTime = System.currentTimeMillis()
    }

    /**
     * Stops the active recording and returns the file name along with its duration in seconds.
     * If the user cancels the recording, it deletes the temporary file to save storage.
     */
    fun stopRecording(cancel: Boolean = false): Pair<String, Int>? {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) { e.printStackTrace() }
        recorder = null

        if (cancel) {
            currentFile?.delete()
            return null
        }

        val durationSecs = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
        return currentFile?.name?.let { Pair(it, durationSecs) }
    }

    /**
     * Plays a saved voice note and triggers a callback when the audio finishes.
     */
    fun play(fileName: String, onCompletion: () -> Unit) {
        player?.release()
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return

        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            start()
            setOnCompletionListener {
                onCompletion()
                release()
                player = null
            }
        }
    }

    /**
     * Halts playback and releases the media player resources.
     */
    fun stopPlaying() {
        player?.apply {
            if (isPlaying) stop()
            release()
        }
        player = null
    }
}