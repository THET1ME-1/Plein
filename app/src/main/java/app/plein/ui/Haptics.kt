package app.plein.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Отклик в пальцы.
 *
 * Идём через `View.performHapticFeedback`, а не через `LocalHapticFeedback`:
 * системе видны все константы, включая тик и подтверждение, и она сама
 * уважает выключенную вибрацию в настройках телефона.
 *
 * Новые константы появились в Android 11 и 14, поэтому у каждой есть запасная
 * из тех, что были с самого начала: без этого на старых прошивках отклик
 * пропадал вовсе.
 */
class Haptics(private val view: View) {

    /** Короткий щелчок: страница сменилась, значок встал в новую клетку. */
    fun tick() = play(
        if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK
        else HapticFeedbackConstants.CLOCK_TICK
    )

    /** Долгое нажатие: открылось меню, взяли значок. */
    fun longPress() = play(HapticFeedbackConstants.LONG_PRESS)

    /** Дело сделано: значок отпущен, копия записана. */
    fun confirm() = play(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.VIRTUAL_KEY
    )

    /** Не вышло: файл не тот, действие отменено. */
    fun reject() = play(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT
        else HapticFeedbackConstants.LONG_PRESS
    )

    /** Тумблер и выбор в настройках. */
    fun toggle(on: Boolean) = play(
        when {
            Build.VERSION.SDK_INT >= 34 && on -> HapticFeedbackConstants.TOGGLE_ON
            Build.VERSION.SDK_INT >= 34 -> HapticFeedbackConstants.TOGGLE_OFF
            else -> HapticFeedbackConstants.CLOCK_TICK
        }
    )

    /** Жест дотянут до порога: отпустишь — приедет новый кадр. */
    fun threshold() = play(
        if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
        else HapticFeedbackConstants.CLOCK_TICK
    )

    private fun play(constant: Int) {
        view.performHapticFeedback(constant)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
