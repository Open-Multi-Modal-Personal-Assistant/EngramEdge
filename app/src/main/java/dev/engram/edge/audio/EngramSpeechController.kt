package dev.engram.edge.audio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SpeechUiState(
    val isListening: Boolean = false,
    val partialTranscript: String = "",
    val ttsReady: Boolean = false,
    val preferOfflineRecognition: Boolean = true,
    val lastError: String? = null
)

/**
 * Replaces legacy OpenMMPA `STTSFunctions` Cloud Functions + `TTSEncodings.xlsx` with zero-latency
 * native Android 15 `SpeechRecognizer` (`EXTRA_PREFER_OFFLINE`) and system `TextToSpeech`.
 */
class EngramSpeechController(
    private val context: Context
) : AutoCloseable {

    companion object {
        private const val TAG = "EngramSpeech"

        fun errorToString(error: Int): String = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error (3)"
            SpeechRecognizer.ERROR_CLIENT -> "Client error (5)"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "RECORD_AUDIO permission missing (9)"
            SpeechRecognizer.ERROR_NETWORK -> "Network required (offline pack unavailable) (2)"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout (1)"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized (7)"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy (8)"
            SpeechRecognizer.ERROR_SERVER -> "Server/Engine error (offline pack unavailable) (4)"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout (no audio detected) (6)"
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported (12)"
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language pack unavailable (13)"
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Server disconnected (11)"
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests (10)"
            else -> "Speech recognition error ($error)"
        }
    }

    private val googleSearchService = ComponentName(
        "com.google.android.googlequicksearchbox",
        "com.google.android.voicesearch.serviceapi.GoogleRecognitionService"
    )

    private val googleTtsService = ComponentName(
        "com.google.android.tts",
        "com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService"
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    private val _state = MutableStateFlow(SpeechUiState())
    val state: StateFlow<SpeechUiState> = _state.asStateFlow()

    init {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                textToSpeech?.setSpeechRate(1.08f)
                _state.value = _state.value.copy(ttsReady = true)
                Log.i(TAG, "TextToSpeech initialized successfully")
            } else {
                Log.w(TAG, "TextToSpeech initialization failed with status $status")
            }
        }
    }

    private fun isServiceAvailable(component: ComponentName): Boolean {
        return runCatching {
            context.packageManager.getServiceInfo(component, 0)
            true
        }.getOrDefault(false)
    }

    /**
     * Must be invoked on the Main thread (e.g., from a Compose Push-to-Talk button handler).
     */
    fun startPushToTalk(
        preferOffline: Boolean = true,
        specificService: ComponentName? = null,
        onError: ((String) -> Unit)? = null,
        onFinalUtterance: (String) -> Unit
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { startPushToTalk(preferOffline, specificService, onError, onFinalUtterance) }
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            val err = "On-device SpeechRecognizer service unavailable on this ROM."
            Log.e(TAG, err)
            _state.value = _state.value.copy(
                isListening = false,
                lastError = err
            )
            onError?.invoke(err)
            return
        }

        stopListening()

        val serviceToUse = specificService ?: when {
            isServiceAvailable(googleSearchService) -> googleSearchService
            isServiceAvailable(googleTtsService) -> googleTtsService
            else -> null
        }

        Log.i(TAG, "Starting SpeechRecognizer (service=$serviceToUse, preferOffline=$preferOffline)")
        val recognizer = if (serviceToUse != null) {
            SpeechRecognizer.createSpeechRecognizer(context, serviceToUse)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
        speechRecognizer = recognizer

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
            if (preferOffline) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            runCatching { recognizer.triggerModelDownload(intent) }
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.i(TAG, "onReadyForSpeech: microphone open, ready")
                _state.value = _state.value.copy(
                    isListening = true,
                    partialTranscript = "Listening...",
                    preferOfflineRecognition = preferOffline,
                    lastError = null
                )
            }

            override fun onBeginningOfSpeech() {
                Log.i(TAG, "onBeginningOfSpeech: user began speaking")
                _state.value = _state.value.copy(partialTranscript = "Hearing speech...")
            }

            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                Log.i(TAG, "onEndOfSpeech: speech completed, transcribing...")
                _state.value = _state.value.copy(isListening = false)
            }

            override fun onError(error: Int) {
                val errStr = errorToString(error)
                Log.e(TAG, "onError: code=$error ($errStr), service=$serviceToUse, preferOffline=$preferOffline")
                _state.value = _state.value.copy(
                    isListening = false,
                    lastError = errStr
                )

                // Automatic fallback:
                // 1. If service was googleTtsService and language pack is missing, fallback to googleSearchService
                if (serviceToUse == googleTtsService && isServiceAvailable(googleSearchService)) {
                    Log.i(TAG, "TTS SODA language pack unavailable, falling back to Google Search Speech Service...")
                    mainHandler.post {
                        stopListening()
                        startPushToTalk(
                            preferOffline = false,
                            specificService = googleSearchService,
                            onError = onError,
                            onFinalUtterance = onFinalUtterance
                        )
                    }
                    return
                }

                // 2. If preferOffline failed, retry online
                if (preferOffline && (error == SpeechRecognizer.ERROR_SERVER || error == SpeechRecognizer.ERROR_NETWORK || error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)) {
                    Log.i(TAG, "Retrying speech recognition with online mode fallback...")
                    mainHandler.post {
                        stopListening()
                        startPushToTalk(
                            preferOffline = false,
                            specificService = specificService,
                            onError = onError,
                            onFinalUtterance = onFinalUtterance
                        )
                    }
                    return
                }

                mainHandler.post {
                    onError?.invoke(errStr)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = matches?.firstOrNull().orEmpty().trim()
                Log.i(TAG, "onResults: best='$best' (candidates=$matches)")
                _state.value = _state.value.copy(
                    isListening = false,
                    partialTranscript = best
                )
                mainHandler.post {
                    if (best.isNotEmpty()) {
                        onFinalUtterance(best)
                    } else {
                        onError?.invoke("No words recognized. Please speak closer to watch mic.")
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (partial.isNotBlank()) {
                    Log.d(TAG, "onPartialResults: $partial")
                    _state.value = _state.value.copy(partialTranscript = partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        recognizer.startListening(intent)
    }

    fun stopListening() {
        runCatching { speechRecognizer?.stopListening() }
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
        _state.value = _state.value.copy(isListening = false)
    }

    fun speakConciseReply(text: String) {
        if (!_state.value.ttsReady || text.isBlank()) return
        val cleaned = text
            .replace(Regex("""\[[^\]]*]"""), "")
            .replace("*", "")
            .trim()
            .take(260)
        textToSpeech?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, "engram-reply")
    }

    override fun close() {
        stopListening()
        runCatching {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
        textToSpeech = null
    }
}
