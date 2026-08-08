package app.plein.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Копия, которая делается сама.
 *
 * Раз в неделю лаунчер кладёт файл настроек в папку, выбранную человеком.
 * Планировщика не заводим: лаунчер и так открывают десятки раз в день, а
 * фоновая работа ради одного файла в неделю — это будильник ради будильника.
 *
 * Старые копии не копим бесконечно: держим последние восемь, дальше сносим.
 */
object AutoBackup {

    private const val WEEK = 7L * 24 * 60 * 60 * 1000
    private const val KEEP = 8

    fun due(prefs: Prefs): Boolean {
        if (!prefs.autoBackup || prefs.backupFolder.isEmpty()) return false
        return System.currentTimeMillis() - prefs.lastBackup > WEEK
    }

    suspend fun run(context: Context, prefs: Prefs): Boolean = withContext(Dispatchers.IO) {
        val treeUri = prefs.backupFolder.takeIf { it.isNotEmpty() } ?: return@withContext false
        runCatching {
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(treeUri)) ?: return@withContext false
            if (!tree.canWrite()) return@withContext false

            val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
            val name = "plein-$stamp.json"
            tree.findFile(name)?.delete()
            val file = tree.createFile("application/json", name) ?: return@withContext false

            val done = Backup.export(context, file.uri)
            if (done) {
                prefs.lastBackup = System.currentTimeMillis()
                trim(tree)
            }
            done
        }.getOrDefault(false)
    }

    /** Оставляем восемь последних копий: дальше это уже архив, а не страховка. */
    private fun trim(tree: DocumentFile) {
        val ours = tree.listFiles()
            .filter { it.isFile && it.name?.startsWith("plein-") == true }
            .sortedByDescending { it.lastModified() }
        ours.drop(KEEP).forEach { it.delete() }
    }
}
