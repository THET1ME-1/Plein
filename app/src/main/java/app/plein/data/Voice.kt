package app.plein.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import java.util.Locale

/**
 * Голосовой ввод.
 *
 * Своего распознавания у лаунчера нет и быть не должно: в системе стоит чужое
 * — Google, прошивочное или стороннее, — и оно спрашивает разрешение на
 * микрофон само. Наше дело позвать его и забрать текст.
 *
 * Когда распознавания нет вовсе (чистая AOSP-сборка без сервисов), зовём
 * голосового помощника: он хотя бы что-то ответит, а молчащая кнопка выглядит
 * поломкой.
 */
object Voice {

    fun recognizeIntent(context: Context): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(app.plein.R.string.voice_prompt))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

    /**
     * Есть ли кому слушать.
     *
     * Спрашиваем систему напрямую: на прошивках без сервисов Google интент
     * распознавания иногда разрешается в чужой экран, который речь не слышит,
     * и кнопка вела человека в настройки вместо микрофона.
     */
    fun available(context: Context): Boolean =
        android.speech.SpeechRecognizer.isRecognitionAvailable(context) &&
            recognizeIntent(context).resolveActivity(context.packageManager) != null

    /** Запасной ход: голосовой помощник системы. */
    fun assistantIntent(): Intent =
        Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Настоящий помощник, а не экран его выбора.
     *
     * `ACTION_VOICE_COMMAND` на телефоне без помощника разрешается в системные
     * настройки: кнопка формально работала, а человек попадал на страницу
     * «Цифровой помощник: Нет».
     */
    fun assistantAvailable(context: Context): Boolean {
        val target = context.packageManager.resolveActivity(assistantIntent(), 0) ?: return false
        val pkg = target.activityInfo?.packageName.orEmpty()
        return pkg.isNotEmpty() && !pkg.contains("settings", ignoreCase = true)
    }

    /** Первый распознанный вариант. Пусто — человек промолчал или отменил. */
    fun textOf(data: Intent?): String =
        data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            .orEmpty()

    private fun Intent.resolveActivity(manager: PackageManager) =
        manager.resolveActivity(this, 0)
}
