package app.plein.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Что лежит в последнем выпуске на GitHub. */
data class Update(
    val version: String,
    val notes: String,
    val url: String,
    val size: Long,
)

/**
 * Обновление прямо из лаунчера.
 *
 * Сборка разложена по ABI, поэтому из выпуска берём файл под свой процессор:
 * универсальный весит вдвое больше и качать его незачем.
 *
 * Если приложение поставил магазин или Obtainium, свой обновлятор молчит:
 * два обновляющих механизма на одном приложении дерутся за подпись и версию,
 * а человек получает вечное «доступно обновление».
 */
object Updates {

    private const val RELEASES = "https://api.github.com/repos/THET1ME-1/Plein/releases/latest"
    private const val AGENT = "PleinLauncher"

    private val foreignInstallers = setOf(
        "dev.imranr.obtainium",
        "dev.imranr.obtainium.fdroid",
        "com.android.vending",
        "org.fdroid.fdroid",
        "com.aurora.store",
    )

    /** Кто поставил приложение. Пусто означает установку файлом. */
    fun installerOf(context: Context): String? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }
    }.getOrNull()

    /** Показывать ли раздел обновлений. */
    fun selfUpdating(context: Context): Boolean = installerOf(context) !in foreignInstallers

    /**
     * Есть ли выпуск новее текущего.
     *
     * Сравниваем числами по частям: «0.10.0» новее «0.9.0», хотя по буквам
     * оно меньше.
     */
    suspend fun check(context: Context, currentVersion: String): Update? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(RELEASES).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 12000
                setRequestProperty("User-Agent", AGENT)
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val release = JSONObject(body)
            if (release.optBoolean("draft") || release.optBoolean("prerelease")) return@withContext null
            val tag = release.optString("tag_name").removePrefix("v")
            if (!isNewer(tag, currentVersion)) return@withContext null

            val assets = release.optJSONArray("assets") ?: return@withContext null
            val names = (0 until assets.length()).map { assets.getJSONObject(it) }
            val wanted = Build.SUPPORTED_ABIS.firstOrNull { abi ->
                names.any { it.optString("name").contains(abi) }
            }
            val asset = names.firstOrNull { wanted != null && it.optString("name").contains(wanted) }
                ?: names.firstOrNull { it.optString("name").contains("universal") }
                ?: return@withContext null

            Update(
                version = tag,
                notes = release.optString("body").take(600),
                url = asset.optString("browser_download_url"),
                size = asset.optLong("size"),
            )
        }.getOrNull()
    }

    fun isNewer(candidate: String, current: String): Boolean {
        fun parts(value: String) = value.split('.', '-').mapNotNull { it.toIntOrNull() }
        val left = parts(candidate)
        val right = parts(current)
        for (index in 0 until maxOf(left.size, right.size)) {
            val a = left.getOrElse(index) { 0 }
            val b = right.getOrElse(index) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    /** Скачиваем в кэш: файл переживёт поворот экрана и уход в фон. */
    suspend fun download(
        context: Context,
        update: Update,
        onProgress: (Float) -> Unit = {},
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            val target = File(context.cacheDir, "plein-${update.version}.apk")
            if (target.exists() && target.length() == update.size) return@withContext target

            val connection = (URL(update.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 30000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", AGENT)
            }
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var done = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (update.size > 0) onProgress((done.toFloat() / update.size).coerceIn(0f, 1f))
                    }
                }
            }
            connection.disconnect()
            target
        }.getOrNull()
    }

    /**
     * Установка через системный установщик.
     *
     * Просим `REQUEST_INSTALL_PACKAGES` и ведём человека в системное
     * разрешение, если его ещё нет: без него сессия открывается, но система
     * молча ничего не показывает.
     */
    fun canInstall(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun permissionIntent(context: Context): Intent =
        Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .setData(android.net.Uri.parse("package:${context.packageName}"))

    suspend fun install(context: Context, apk: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
            val sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                session.openWrite("plein", 0, apk.length()).use { output ->
                    apk.inputStream().use { input -> input.copyTo(output) }
                    session.fsync(output)
                }
                val intent = Intent(context, app.plein.UpdateReceiver::class.java)
                val flags = android.app.PendingIntent.FLAG_MUTABLE or
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                val pending = android.app.PendingIntent.getBroadcast(context, sessionId, intent, flags)
                session.commit(pending.intentSender)
            }
            true
        }.getOrDefault(false)
    }
}
