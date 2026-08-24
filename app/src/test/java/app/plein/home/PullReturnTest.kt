package app.plein.home

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.TestMonotonicFrameClock
import app.plein.ui.home.PullPhysics
import app.plein.ui.home.PullState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Возврат листа после оттягивания.
 *
 * Раньше возврат анимировался прямо в `onPreFling`, то есть в корутине
 * скролла. Стоило положить палец обратно, пока лист ехал, — система обрывала
 * инерцию, а вместе с ней и анимацию: ход застывал на полпути, лист висел
 * сдвинутым с мёртвым кругом, и следующий жест начинался из ниоткуда.
 * Возврат должен жить в области экрана и доходить до нуля при любом обрыве.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class PullReturnTest {

    private val limit = 130f * 3f

    /** Уверенный жест пальцем: столько шагов хватает, чтобы дойти до порога. */
    private fun PullState.pullToTrigger() {
        var travelled = 0f
        while (!reached && travelled < 2_000f) {
            drag(12f)
            travelled += 12f
        }
    }

    @Test
    fun `ход возвращается к нулю сам`() = runTest {
        withContext(TestMonotonicFrameClock(this)) {
            val screen = CoroutineScope(coroutineContext + Job())
            val pull = PullState(screen, limit)

            repeat(10) { pull.drag(12f) }
            assertTrue("палец тянул, а ход нулевой", pull.value > 0f)

            pull.release()
            advanceUntilIdle()
            assertEquals(0f, pull.value, 0.5f)
        }
    }

    @Test
    fun `оборванная инерция возврат не убивает`() = runTest {
        withContext(TestMonotonicFrameClock(this)) {
            val screen = CoroutineScope(coroutineContext + Job())
            val pull = PullState(screen, limit)
            val scroll = CoroutineScope(coroutineContext + Job())

            scroll.launch {
                repeat(10) { pull.drag(12f) }
                pull.release()
            }
            advanceTimeBy(32)
            // Палец лёг обратно: система обрывает инерцию списка.
            scroll.cancel()

            advanceUntilIdle()
            assertEquals("лист застыл оттянутым", 0f, pull.value, 0.5f)
        }
    }

    @Test
    fun `новый жест перебивает возврат и снова возвращается`() = runTest {
        withContext(TestMonotonicFrameClock(this)) {
            val screen = CoroutineScope(coroutineContext + Job())
            val pull = PullState(screen, limit)

            repeat(10) { pull.drag(12f) }
            pull.release()
            advanceTimeBy(48)

            // Второй заход: палец снова тянет, пока лист ещё едет назад.
            val onTheWayBack = pull.value
            repeat(10) { pull.drag(12f) }
            assertTrue("второй жест хода не набрал", pull.value > onTheWayBack)

            pull.release()
            advanceUntilIdle()
            assertEquals("второй возврат не доехал", 0f, pull.value, 0.5f)
        }
    }

    @Test
    fun `кадр заказывают только с порога`() = runTest {
        withContext(TestMonotonicFrameClock(this)) {
            val screen = CoroutineScope(coroutineContext + Job())
            val pull = PullState(screen, limit)

            repeat(5) { pull.drag(12f) }
            assertFalse("недотянутый жест заказал кадр", pull.release())
            advanceUntilIdle()

            pull.pullToTrigger()
            assertTrue("дотянутый жест кадр не заказал", pull.release())
            advanceUntilIdle()
            assertEquals(0f, pull.value, 0.5f)
        }
    }

    @Test
    fun `обратное движение отдаёт лист списку`() = runTest {
        withContext(TestMonotonicFrameClock(this)) {
            val screen = CoroutineScope(coroutineContext + Job())
            val pull = PullState(screen, limit)

            repeat(10) { pull.drag(12f) }
            val was = pull.value
            val used = pull.giveBack(-8f)
            assertEquals(-8f, used, 0.01f)
            assertEquals(was - 8f, pull.value, 0.01f)

            // Ниже нуля лист не уходит: остаток достаётся списку.
            val rest = pull.giveBack(-10_000f)
            assertEquals(0f, pull.value, 0.01f)
            assertTrue("отдали больше, чем было", rest > -10_000f)
        }
    }

    @Test
    fun `страховка возвращает застрявший ход`() = runTest {
        withContext(TestMonotonicFrameClock(this)) {
            val screen = CoroutineScope(coroutineContext + Job())
            val pull = PullState(screen, limit)

            // Жест оборвался так, что отпускания не пришло вовсе.
            repeat(10) { pull.drag(12f) }
            pull.settle()
            advanceUntilIdle()
            assertEquals(0f, pull.value, 0.5f)
        }
    }

    @Test
    fun `страховка не перебивает идущий возврат`() = runTest {
        withContext(TestMonotonicFrameClock(this)) {
            val screen = CoroutineScope(coroutineContext + Job())
            val pull = PullState(screen, limit)

            repeat(10) { pull.drag(12f) }
            pull.release()
            advanceTimeBy(48)
            val onTheWayBack = pull.value

            pull.settle()
            advanceTimeBy(16)
            assertTrue("возврат отскочил назад", pull.value <= onTheWayBack)

            advanceUntilIdle()
            assertEquals(0f, pull.value, 0.5f)
        }
    }

    @Test
    fun `порог считается от предела хода`() {
        val pull = PullState(CoroutineScope(Job()), limit)
        assertFalse(pull.reached)
        pull.pullToTrigger()
        assertTrue(pull.value >= limit * PullPhysics.TRIGGER)
    }
}
