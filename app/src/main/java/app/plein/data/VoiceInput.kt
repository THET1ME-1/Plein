package app.plein.data

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Голос без чужого экрана.
 *
 * Многие прошивки не объявляют активность `ACTION_RECOGNIZE_SPEECH`, зато
 * служба распознавания в системе есть. Тогда единственный путь — слушать
 * самим через `SpeechRecognizer` и рисовать своё окно: чужого просто нет.
 *
 * Живёт ровно столько, сколько открыт лист прослушивания, и обязательно
 * закрывается: распознаватель держит микрофон, пока его не отпустят.
 */
class VoiceInput(private val context: Context) {

    var listening by mutableStateOf(false)
        private set

    /** Громкость для живой полоски: −2…10 у большинства служб. */
    var level by mutableFloatStateOf(0f)
        private set

    var partial by mutableStateOf("")
        private set

    var error by mutableStateOf<String?>(null)
        private set

    private var recognizer: SpeechRecognizer? = null

    fun start(onResult: (String) -> Unit) {
        stop()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            error = "нет распознавания речи"
            return
        }

        val speech = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = speech
        listening = true
        partial = ""
        error = null

        speech.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) {
                level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() {
                listening = false
            }

            override fun onError(code: Int) {
                listening = false
                error = reasonOf(code)
            }

            override fun onResults(results: Bundle?) {
                listening = false
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) onResult(text) else error = "не расслышал"
            }

            override fun onPartialResults(results: Bundle?) {
                partial = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
            }

            override fun onEvent(type: Int, params: Bundle?) = Unit
        })

        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        runCatching { speech.startListening(intent) }
            .onFailure {
                listening = false
                error = "не вышло начать запись"
            }
    }

    fun stop() {
        recognizer?.let { speech ->
            runCatching { speech.cancel() }
            runCatching { speech.destroy() }
        }
        recognizer = null
        listening = false
        level = 0f
    }

    private fun reasonOf(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "микрофон не отдаёт звук"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "нет сети"
        SpeechRecognizer.ERROR_NO_MATCH -> "не расслышал"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "тишина"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "нет доступа к микрофону"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "распознавание занято"
        else -> "сбой распознавания"
    }

    companion object {
        /** Служба распознавания в системе: активность для интента не нужна. */
        fun available(context: Context): Boolean =
            SpeechRecognizer.isRecognitionAvailable(context)
    }
}
