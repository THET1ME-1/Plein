package app.plein.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.security.MessageDigest

/**
 * Готовые значки на диске.
 *
 * Память переживает только сессию: после того как систему прижало и процесс
 * убили, весь экран рисовался заново — сотня адаптивных иконок в слоях с
 * клипом по форме. На диске лежит уже обрезанный битмап нужного размера,
 * поэтому возврат в лаунчер обходится чтением файла.
 *
 * Имя файла начинается с пакета: когда приложение обновилось, его значки
 * стираются по префиксу, а остальные остаются нетронутыми.
 */
class IconDiskCache(context: Context) {

    private val dir = File(context.cacheDir, "icons").apply { mkdirs() }

    fun read(packageName: String, key: String): Bitmap? {
        val file = fileOf(packageName, key)
        if (!file.exists()) return null
        val bitmap = runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull()
        if (bitmap == null) file.delete()
        return bitmap
    }

    /**
     * Пишем через временный файл и переименование.
     *
     * Прогрев идёт в два потока, а видимые значки рисуются параллельно с ним:
     * на одном ключе оба могли писать в один файл сразу, и на диске оставался
     * обрубок PNG. Чтение его отбрасывало и удаляло, значок перерисовывался —
     * то есть кэш лечился сам, но с миганием и лишней работой. Переименование
     * в пределах одного каталога атомарно, поэтому файл либо целый, либо его нет.
     */
    fun write(packageName: String, key: String, bitmap: Bitmap) {
        runCatching {
            val file = fileOf(packageName, key)
            val tmp = File(dir, "${file.name}.${Thread.currentThread().id}.tmp")
            tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            if (!tmp.renameTo(file)) tmp.delete()
        }
    }

    /** Пакет обновился или уехал — его картинки больше не годятся. */
    fun forget(packageName: String) {
        val prefix = safe(packageName) + "-"
        dir.listFiles()?.forEach { if (it.name.startsWith(prefix)) it.delete() }
    }

    /**
     * Чистка по счёту, а не по возрасту: смена формы или размера сетки плодит
     * новый набор файлов, а старый остаётся мёртвым грузом.
     */
    fun trim() {
        val files = dir.listFiles() ?: return
        // Недописанные хвосты от убитого процесса: их не должно быть видно
        // ни счётчику, ни чтению.
        files.filter { it.name.endsWith(".tmp") }.forEach { it.delete() }
        val kept = files.filterNot { it.name.endsWith(".tmp") }
        if (kept.size <= MAX_FILES) return
        kept.sortedByDescending { it.lastModified() }.drop(MAX_FILES).forEach { it.delete() }
    }

    private fun fileOf(packageName: String, key: String) =
        File(dir, "${safe(packageName)}-${digest(key)}.png")

    private fun safe(packageName: String) = packageName.replace('.', '_')

    private fun digest(key: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(key.toByteArray())
        return bytes.take(8).joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val MAX_FILES = 900
    }
}
