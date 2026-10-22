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
    private var recentChapter: String? = null
    private var recentLessons: JSONArray? = null

    suspend fun catalog(context: Context): JSONObject = withContext(Dispatchers.IO) {
        JSONObject(context.assets.open("lesson-meta.json").bufferedReader().use { it.readText() })
    }

    suspend fun lesson(context: Context, lessonId: String): JSONObject? = withContext(Dispatchers.IO) {
        val index = JSONObject(context.assets.open("study/index.json").bufferedReader().use { it.readText() })
        val file = index.optString(lessonId).takeIf { it.isNotBlank() } ?: return@withContext null
        val chapters = synchronized(this@NativeLearnData) {
            if (file == recentChapter) recentLessons else null
        } ?: JSONArray(GZIPInputStream(context.assets.open("study/$file")).bufferedReader().use { it.readText() }).also {
            synchronized(this@NativeLearnData) { recentChapter = file; recentLessons = it }
        }
        (0 until chapters.length()).asSequence().mapNotNull { chapters.optJSONObject(it) }
            .firstOrNull { it.optString("id") == lessonId }
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
