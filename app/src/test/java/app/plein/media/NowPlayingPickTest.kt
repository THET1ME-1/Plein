package app.plein.media

import app.plein.data.NowPlaying
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Какой плеер показывать, когда их несколько.
 *
 * На телефоне разом живут музыка, подкаст и вкладка браузера с роликом.
 * Показываем то, что звучит; если молчат все — то, чем пользовались последним.
 */
class NowPlayingPickTest {

    private fun track(pkg: String, playing: Boolean, at: Long) =
        NowPlaying.Track(title = pkg, artist = "", art = null, playing = playing, packageName = pkg, activeAt = at)

    @Test
    fun `тишина без плееров`() {
        assertNull(NowPlaying.pick(emptyList()))
    }

    @Test
    fun `звучащий важнее молчащего`() {
        val found = NowPlaying.pick(listOf(track("подкаст", false, 900), track("музыка", true, 100)))
        assertEquals("музыка", found?.packageName)
    }

    @Test
    fun `из двух звучащих берём тот, что тронули позже`() {
        val found = NowPlaying.pick(listOf(track("музыка", true, 100), track("браузер", true, 500)))
        assertEquals("браузер", found?.packageName)
    }

    @Test
    fun `когда молчат все, остаётся последний`() {
        val found = NowPlaying.pick(listOf(track("музыка", false, 700), track("подкаст", false, 200)))
        assertEquals("музыка", found?.packageName)
    }
}
