package app.plein.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Запрос за кадром: один живой за раз, новый перебивает начатый.
 *
 * Раньше начатый запрос глушил кнопку и жест до самого конца — до сорока пяти
 * секунд у мёртвого круга, если сеть молчала. Отпускать очередь по таймауту
 * человек не должен: потянул ещё раз — значит хочет другой кадр сейчас.
 *
 * Гасить индикатор имеет право только последний запрос. Перебитый уходит молча,
 * иначе его `finally` погасит круг уже начатой загрузке — так круг однажды и
 * гас на живом запросе.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackdropRequestTest {

    @Test
    fun `новый запрос перебивает начатый`() = runTest {
        val requests = BackdropRequest(CoroutineScope(coroutineContext + Job()))
        val first = CompletableDeferred<Unit>()
        var firstFinished = false
        var secondFinished = false

        requests.start {
            first.await()
            firstFinished = true
        }
        advanceUntilIdle()
        assertTrue("круг не зажёгся", requests.running)

        requests.start { secondFinished = true }
        advanceUntilIdle()

        assertFalse("перебитый запрос всё-таки дописал кадр", firstFinished)
        assertTrue("новый запрос не выполнился", secondFinished)
        assertFalse("круг остался гореть", requests.running)
    }

    @Test
    fun `перебитый запрос не гасит круг живому`() = runTest {
        val requests = BackdropRequest(CoroutineScope(coroutineContext + Job()))
        val second = CompletableDeferred<Unit>()

        requests.start { CompletableDeferred<Unit>().await() }
        advanceUntilIdle()

        requests.start { second.await() }
        advanceUntilIdle()
        assertTrue("круг погас на живом запросе", requests.running)

        second.complete(Unit)
        advanceUntilIdle()
        assertFalse(requests.running)
    }

    @Test
    fun `запросы идут по очереди, а не внахлёст`() = runTest {
        val requests = BackdropRequest(CoroutineScope(coroutineContext + Job()))
        var alive = 0
        var peak = 0

        repeat(3) {
            requests.start {
                alive++
                peak = maxOf(peak, alive)
                // Отмена прилетает сюда исключением, поэтому счётчик
                // уменьшается в finally, а не строкой ниже.
                try {
                    CompletableDeferred<Unit>().await()
                } finally {
                    alive--
                }
            }
            advanceUntilIdle()
        }

        assertEquals("два запроса шли одновременно", 1, peak)
    }
}
