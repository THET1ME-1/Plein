package app.plein.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

/**
 * Очередь из одного запроса за кадром.
 *
 * Пока кадр едет, второй запрос не начинается — иначе два ответа перебивают
 * друг друга и на экран попадает не тот, что заказан последним. Но и ждать
 * начатый запрос человек не обязан: потянул ещё раз — старый отменяется, новый
 * идёт. Раньше очередь держалась до победного, и при молчащей сети жест не
 * работал сорок пять секунд, до самого таймаута.
 *
 * Старый запрос дожидается смерти (`cancelAndJoin`) до того, как новый зажжёт
 * круг: без этого его `finally` гасил индикатор уже начатой загрузке.
 */
class BackdropRequest(private val scope: CoroutineScope) {

    /** Едет ли кадр прямо сейчас. */
    var running by mutableStateOf(false)
        private set

    private var job: Job? = null

    fun start(block: suspend () -> Unit) {
        val previous = job
        job = scope.launch {
            previous?.cancelAndJoin()
            running = true
            try {
                block()
            } finally {
                running = false
            }
        }
    }
}
