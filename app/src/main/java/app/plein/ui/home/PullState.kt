package app.plein.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Ход оттягивания за новым кадром.
 *
 * Держит одно число — на сколько лист уехал вниз — и умеет вернуть его на
 * место. Возврат живёт здесь, а не в `onPreFling`: та корутина принадлежит
 * скроллу, и система обрывает её, стоило положить палец обратно посреди
 * анимации. Ход застывал на полпути, лист висел сдвинутым с мёртвым кругом, и
 * следующий жест начинался из ниоткуда. Область экрана переживает и обрыв
 * инерции, и новое касание.
 *
 * Второе, что чинится этим переносом: `onPreFling` больше не ждёт конца
 * анимации, а значит не задерживает инерцию списка на секунду.
 *
 * Значение читать отложенно — из `graphicsLayer` или лямбдой, а не в теле
 * композиции: иначе каждый кадр жеста пересобирает домашний экран целиком.
 */
class PullState(
    private val scope: CoroutineScope,
    private val limit: Float,
) {

    var value by mutableFloatStateOf(0f)
        private set

    private var homing: Job? = null

    /** Дотянут ли ход до срабатывания. */
    val reached: Boolean get() = value >= limit * PullPhysics.TRIGGER

    /** Палец тянет вниз: возврат, если он шёл, уступает пальцу. */
    fun drag(delta: Float) {
        stopHoming()
        value = PullPhysics.accumulate(value, delta, limit)
    }

    /**
     * Обратное движение: оттянутый лист сначала встаёт на место и только потом
     * отдаёт остаток списку. Возвращает, сколько взял себе (число со знаком).
     */
    fun giveBack(delta: Float): Float {
        stopHoming()
        val next = (value + delta).coerceAtLeast(0f)
        val used = next - value
        value = next
        return used
    }

    /**
     * Палец отпущен: лист уезжает обратно пружиной.
     *
     * @return дотянул ли жест до порога — тогда пора идти за кадром.
     */
    fun release(): Boolean {
        val hit = reached
        stopHoming()
        startHoming()
        return hit
    }

    /**
     * Страховка на случай, когда жест оборвался без отпускания.
     *
     * Инерция кончилась, а ход остался — вернуть. Уже идущий возврат не
     * трогаем: перезапуск с середины виден как рывок.
     */
    fun settle() {
        if (homing?.isActive == true) return
        startHoming()
    }

    private fun startHoming() {
        if (value <= 0f) return
        homing = scope.launch {
            animate(
                initialValue = value,
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessLow),
            ) { animated, _ -> value = animated }
        }
    }

    private fun stopHoming() {
        homing?.cancel()
        homing = null
    }
}
