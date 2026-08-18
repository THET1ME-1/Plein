package app.plein.baseline

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

/**
 * Что записываем в профиль.
 *
 * Путь человека в первые секунды: запуск, ожидание сетки, прокрутка списка,
 * переход между папками. Именно этот код Android потом переведёт заранее.
 */
class StartupProfile {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startupAndScroll() = rule.collect(
        packageName = "app.plein",
        // Без этого флага собирается только общий профиль, а startup-профиль
        // не пишется вовсе: R8 не узнает, какой код класть в начало dex.
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()

        // Ждём, пока появится сетка, и листаем её: прокрутка — вторая по
        // важности часть после запуска.
        device.wait(Until.hasObject(By.pkg("app.plein").depth(0)), 5_000)
        repeat(3) {
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight * 3 / 4,
                device.displayWidth / 2,
                device.displayHeight / 4,
                12,
            )
            device.waitForIdle()
        }

        // И листание страниц: у каждой папки своя раскладка.
        repeat(2) {
            device.swipe(
                device.displayWidth * 3 / 4,
                device.displayHeight / 2,
                device.displayWidth / 4,
                device.displayHeight / 2,
                10,
            )
            device.waitForIdle()
        }
    }
}
