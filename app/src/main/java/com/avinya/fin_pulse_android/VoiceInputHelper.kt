package com.avinya.fin_pulse_android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class VoiceInputHelper(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onReadyForSpeechCallback: () -> Unit = {},
    private val onBeginningOfSpeechCallback: () -> Unit = {},
    private val onEndOfSpeechCallback: () -> Unit = {}
) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    companion object {
        private const val TAG = "VoiceInputHelper"
        
        fun isAvailable(context: Context): Boolean {
            return SpeechRecognizer.isRecognitionAvailable(context)
        }
    }
    
    fun startListening() {
        if (isListening) {
            Log.w(TAG, "Already listening")
            return
        }
        
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition is not available on this device")
            return
        }
        
        try {
            // Initialize speech recognizer
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "Ready for speech")
                    isListening = true
                    onReadyForSpeechCallback()
                }
                
                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "Beginning of speech")
                    onBeginningOfSpeechCallback()
                }
                
                override fun onRmsChanged(rmsdB: Float) {
                    // Audio level changed - can be used for visual feedback
                }
                
                override fun onBufferReceived(buffer: ByteArray?) {
                    // Audio buffer received
                }
                
                override fun onEndOfSpeech() {
                    Log.d(TAG, "End of speech")
                    isListening = false
                    onEndOfSpeechCallback()
                }
                
                override fun onError(error: Int) {
                    Log.e(TAG, "Speech recognition error: $error")
                    isListening = false
                    
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found. Please try again."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input. Please try again."
                        else -> "Unknown error occurred"
                    }
                    
                    onError(errorMessage)
                }
                
                override fun onResults(results: Bundle?) {
                    Log.d(TAG, "Got results")
                    isListening = false
                    
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val recognizedText = matches[0]
                        Log.d(TAG, "Recognized text: $recognizedText")
                        onResult(recognizedText)
                    } else {
                        onError("No speech recognized. Please try again.")
                    }
                }
                
                override fun onPartialResults(partialResults: Bundle?) {
                    // Partial results received - can be used for real-time feedback
                }
                
                override fun onEvent(eventType: Int, params: Bundle?) {
                    // Reserved for future use
                }
            })
            
            // Create recognition intent
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Say your expense (e.g., 'spent 100 on ice cream')"
                )
            }
            
            // Start listening
            speechRecognizer?.startListening(intent)
            Log.d(TAG, "Started listening")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            isListening = false
            onError("Failed to start voice recognition: ${e.message}")
        }
    }
    
    fun stopListening() {
        if (isListening) {
            try {
                speechRecognizer?.stopListening()
                Log.d(TAG, "Stopped listening")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech recognition", e)
            }
        }
        isListening = false
    }
    
    fun cancel() {
        try {
            speechRecognizer?.cancel()
            Log.d(TAG, "Cancelled speech recognition")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling speech recognition", e)
        }
        isListening = false
    }
    
    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            Log.d(TAG, "Destroyed speech recognizer")
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speech recognizer", e)
        }
        isListening = false
    }
    
    fun isCurrentlyListening(): Boolean {
        return isListening
    }
}
