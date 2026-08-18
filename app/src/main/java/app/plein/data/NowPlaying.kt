package app.plein.data

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import app.plein.PleinNotifications

/**
 * Что играет на телефоне.
 *
 * Android отдаёт чужие сессии проигрывания только тому, кто читает
 * уведомления: у лаунчера для этого заведена пустая служба-ключ
 * `PleinNotifications`. Пока доступ не выдан, плитка так и говорит и ведёт в
 * системные настройки — молча пустовать она не должна.
 *
 * Управление идёт через `MediaController`: то же самое, что делают кнопки в
 * шторке, только не надо её открывать.
 */
object NowPlaying {

    data class Track(
        val title: String,
        val artist: String,
        val art: Bitmap?,
        val playing: Boolean,
        val packageName: String,
        val activeAt: Long = 0L,
    )

    /**
     * Какой плеер показывать, когда их несколько.
     *
     * На телефоне разом живут музыка, подкаст и вкладка браузера с роликом.
     * Показываем то, что звучит; если молчат все — то, чем пользовались
     * последним.
     */
    fun pick(tracks: List<Track>): Track? = tracks
        .sortedWith(compareByDescending<Track> { it.playing }.thenByDescending { it.activeAt })
        .firstOrNull()

    fun allowed(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}

/** Живое состояние плитки: что играет и чем этим управлять. */
class Playing(
    val track: NowPlaying.Track?,
    private val controller: MediaController?,
) {
    fun toggle() {
        val transport = controller?.transportControls ?: return
        if (track?.playing == true) transport.pause() else transport.play()
    }

    fun next() {
        controller?.transportControls?.skipToNext()
    }

    fun previous() {
        controller?.transportControls?.skipToPrevious()
    }
}

/**
 * Слушаем сессии, пока экран открыт.
 *
 * Своих таймеров нет: система сама зовёт обратно, когда сменился трек, нажали
 * паузу или появился новый плеер.
 */
@Composable
fun rememberPlaying(): Playing {
    val context = LocalContext.current
    var state by remember { mutableStateOf(Playing(null, null)) }

    DisposableEffect(Unit) {
        if (!NowPlaying.allowed(context)) return@DisposableEffect onDispose { }

        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            ?: return@DisposableEffect onDispose { }
        val listener = ComponentName(context, PleinNotifications::class.java)
        val callbacks = mutableMapOf<MediaController, MediaController.Callback>()

        fun readAll(controllers: List<MediaController>): Playing {
            val tracks = controllers.mapNotNull { controller ->
                val meta = controller.metadata ?: return@mapNotNull null
                val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)
                    ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                    ?: return@mapNotNull null
                val state = controller.playbackState
                controller to NowPlaying.Track(
                    title = title,
                    artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
                        ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
                    art = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                        ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART),
                    playing = state?.state == PlaybackState.STATE_PLAYING,
                    packageName = controller.packageName,
                    activeAt = state?.lastPositionUpdateTime ?: 0L,
                )
            }
            val chosen = NowPlaying.pick(tracks.map { it.second })
            val controller = tracks.firstOrNull { it.second == chosen }?.first
            return Playing(chosen, controller)
        }

        // Сессии живут своей жизнью: подписываемся на каждую и переспрашиваем
        // всё разом, иначе плитка застревает на предыдущем треке.
        fun bind(controllers: List<MediaController>) {
            callbacks.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
            callbacks.clear()
            controllers.forEach { controller ->
                val callback = object : MediaController.Callback() {
                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        state = readAll(controllers)
                    }

                    override fun onPlaybackStateChanged(playback: PlaybackState?) {
                        state = readAll(controllers)
                    }
                }
                controller.registerCallback(callback)
                callbacks[controller] = callback
            }
            state = readAll(controllers)
        }

        val onSessions = MediaSessionManager.OnActiveSessionsChangedListener { list ->
            bind(list.orEmpty())
        }

        runCatching {
            bind(manager.getActiveSessions(listener))
            manager.addOnActiveSessionsChangedListener(onSessions, listener)
        }

        onDispose {
            runCatching { manager.removeOnActiveSessionsChangedListener(onSessions) }
            callbacks.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
        }
    }

    return state
}
