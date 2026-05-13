package com.ben.inly.domain.util

import android.content.Context
import android.graphics.ColorSpace
import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException

class VoiceTaskRecognizer(private val context: Context) {
    // FIX: Explicitly ensuring this is org.vosk.Model
    private var model: Model? = null
    private var speechService: SpeechService? = null

    fun initModel(onComplete: (Boolean) -> Unit) {
        if (model != null) {
            onComplete(true)
            return
        }

        // "model" is the name of the folder in your assets directory
        StorageService.unpack(context, "model", "model",
            // FIX: Explicitly declared the types for the callback parameters
            { unpackedModel: Model ->
                this.model = unpackedModel
                Log.d("VoiceTask", "Vosk model unpacked successfully")
                onComplete(true)
            },
            { exception: IOException ->
                Log.e("VoiceTask", "Failed to unpack Vosk model", exception)
                onComplete(false)
            }
        )
    }

    fun startListening(
        onPartial: (String) -> Unit,
        onResult: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (model == null) {
            onError(IllegalStateException("Vosk model is not initialized yet"))
            return
        }

        try {
            val recognizer = Recognizer(model, 16000.0f)
            speechService = SpeechService(recognizer, 16000.0f)

            speechService?.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) {
                    hypothesis?.let {
                        val text = JSONObject(it).optString("partial", "")
                        if (text.isNotBlank()) onPartial(text)
                    }
                }

                override fun onResult(hypothesis: String?) {
                    hypothesis?.let {
                        val text = JSONObject(it).optString("text", "")
                        if (text.isNotBlank()) onResult(text)
                    }
                }

                override fun onFinalResult(hypothesis: String?) {
                    hypothesis?.let {
                        val text = JSONObject(it).optString("text", "")
                        if (text.isNotBlank()) onResult(text)
                    }
                }

                override fun onError(e: Exception?) {
                    e?.let { onError(it) }
                }

                override fun onTimeout() {
                    stopListening()
                }
            })
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun stopListening() {
        speechService?.stop()
        speechService = null
    }

    fun destroy() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
        model = null
    }
}