package id.vixnihon.nativeapp.ui

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Exported directly from the same JS lesson sources as the web, split by chapter.
 * Only the selected chapter is decoded, never all 1,296 full lesson payloads.
 */

internal object NativeLearnData {
    private var cachedIndex: JSONObject? = null
    private var recentChapter: String? = null
    private var recentLessons: JSONArray? = null

    suspend fun catalog(context: Context): JSONObject = withContext(Dispatchers.IO) {
        JSONObject(context.assets.open("lesson-meta.json").bufferedReader().use { it.readText() })
    }

    /** Test that the files really made it into the installed APK, not just the source ZIP. */
    suspend fun preflight(context: Context): Boolean = withContext(Dispatchers.IO) {
        val index = readIndex(context)
        val sample = index.optString("self-intro").ifBlank { return@withContext false }
        runCatching {
            GZIPInputStream(context.assets.open("study/$sample")).use { it.read() }
        }.isSuccess
    }

    private fun readIndex(context: Context): JSONObject {
        synchronized(this) { cachedIndex?.let { return it } }
        val fresh = JSONObject(context.assets.open("study/index.json").bufferedReader().use { it.readText() })
        synchronized(this) { cachedIndex = fresh }
        return fresh
    }

    suspend fun lesson(context: Context, lessonId: String): JSONObject = withContext(Dispatchers.IO) {
        val file = readIndex(context).optString(lessonId).takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Materi '$lessonId' belum ada di katalog offline.")
        val chapters = synchronized(this@NativeLearnData) {
            if (recentChapter == file) recentLessons else null
        } ?: try {
            val payload = context.assets.open("study/$file")
            GZIPInputStream(payload).bufferedReader(Charsets.UTF_8).use { JSONArray(it.readText()) }
        } catch (e: java.io.FileNotFoundException) {
            throw IllegalStateException(
                "Data materi belum ikut terpasang di APK. Pasang APK lengkap versi terbaru dan coba lagi.", e
            )
        } catch (e: Exception) {
            throw IllegalStateException("Data materi gagal dibaca. Silakan pasang ulang APK terbaru.", e)
        }.also {
            synchronized(this@NativeLearnData) { recentChapter = file; recentLessons = it }
        }
        (0 until chapters.length()).asSequence().mapNotNull { chapters.optJSONObject(it) }
            .firstOrNull { it.optString("id") == lessonId }
            ?: throw IllegalStateException("Materi '$lessonId' belum tersedia dalam paket bab.")
    }
}

internal fun JSONObject.localized(lang: String): String = optString(lang).ifBlank { optString("id").ifBlank { optString("en") } }
internal fun JSONObject.localized(key: String, lang: String): String {
    val value = opt(key)
    return when (value) {
        is JSONObject -> value.localized(lang)
        is String -> value
        is Number -> value.toString()
        else -> ""
    }
}
internal fun JSONObject.arr(key: String): List<JSONObject> {
    val a = optJSONArray(key) ?: return emptyList()
    return (0 until a.length()).mapNotNull { a.optJSONObject(it) }
}
internal fun JSONArray.strings(): List<String> = (0 until length()).mapNotNull { opt(it)?.toString() }
internal fun JSONObject.jlptColor(): androidx.compose.ui.graphics.Color = runCatching {
    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(optString("color", "#E03535")))
}.getOrDefault(NihongoPalette.redLight)
