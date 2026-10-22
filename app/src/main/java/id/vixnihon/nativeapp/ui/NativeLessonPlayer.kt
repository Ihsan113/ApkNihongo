package id.vixnihon.nativeapp.ui

import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import id.vixnihon.nativeapp.data.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.ceil
import java.util.Locale

@Composable
internal fun NativeLessonPlayer(
    lesson: JSONObject, language: String, token: String, repository: AuthRepository,
    onBack: () -> Unit, onSaved: () -> Unit, onNotice: (String) -> Unit
) {
    val isId = language == "id"
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pageIndex by remember(lesson.optString("id")) { mutableIntStateOf(0) }
    var quizScore by remember(lesson.optString("id")) { mutableIntStateOf(0) }
    var quizTotal by remember(lesson.optString("id")) { mutableIntStateOf(0) }
    var saved by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var foreground by remember { mutableStateOf(true) }
    val pages = remember(lesson) { lesson.arr("pages") }
    val page = pages.getOrNull(pageIndex)
    val accent = lesson.jlptColor()

    DisposableEffect(context) {
        val engine = TextToSpeech(context.applicationContext) { status -> ttsReady = status == TextToSpeech.SUCCESS }
        tts = engine
        onDispose { engine.stop(); engine.shutdown(); tts = null }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) { foreground = false; tts?.stop() }
            if (event == Lifecycle.Event.ON_RESUME) foreground = true
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(lesson.optString("id"), token, foreground) {
        if (!foreground || token.isBlank()) return@LaunchedEffect
        while (isActive) {
            runCatching { repository.studyHeartbeat(token) }
            delay(20_000)
        }
    }
    fun speak(text: String) {
        if (!ttsReady) { onNotice(if (isId) "Mesin suara belum siap." else "Speech engine unavailable."); return }
        val engine = tts ?: return
        val result = engine.setLanguage(Locale.JAPANESE)
        if (result < 0) { onNotice(if (isId) "Data suara Jepang belum tersedia di HP." else "Japanese voice isn't installed."); return }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lesson-${System.nanoTime()}")
    }
    fun exit() { if (pageIndex > 0 && !saved) confirmExit = true else onBack() }
    BackHandler { exit() }

    if (confirmExit) AlertDialog(onDismissRequest = { confirmExit = false },
        title = { Text(if (isId) "Keluar dari materi?" else "Leave lesson?") },
        text = { Text(if (isId) "Progres halaman yang belum diselesaikan tidak akan disimpan sebagai kelulusan." else "Unfinished pages won't count as a passed lesson.") },
        confirmButton = { TextButton(onClick = { confirmExit = false; onBack() }) { Text(if (isId) "Keluar" else "Leave") } },
        dismissButton = { TextButton(onClick = { confirmExit = false }) { Text(if (isId) "Lanjut Belajar" else "Keep studying") } })

    Column(Modifier.fillMaxSize().background(NihongoPalette.background)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(Modifier.size(40.dp).clickable { exit() }, color = NihongoPalette.surface2, shape = RoundedCornerShape(12.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Close, contentDescription = "Tutup", tint = NihongoPalette.white) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(lesson.localized("title", language), color = NihongoPalette.white, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                LinearProgressIndicator(progress = { ((pageIndex + 1).toFloat() / pages.size.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(5.dp), color = accent, trackColor = NihongoPalette.surface3)
                Text(lessonStage(page?.optString("type").orEmpty(), isId), color = NihongoPalette.dim, fontSize = 10.sp)
            }
            Text("${pageIndex + 1}/${pages.size}", color = NihongoPalette.dim, fontSize = 11.sp)
        }
        if (page != null) {
            when (page.optString("type")) {
                "quiz" -> key(lesson.optString("id"), pageIndex) {
                    NativeLessonQuiz(page, lesson, language, ::speak) { score, total, errors ->
                        quizScore = score; quizTotal = total; pageIndex++
                        if (errors.length() > 0) scope.launch {
                            runCatching { repository.saveWrongLessonAnswers(token, errors) }
                        }
                    }
                }
                "done" -> {
                    val required = maxOf(1, ceil(quizTotal * .8).toInt())
                    val passed = quizTotal > 0 && quizScore >= required
                    val stars = if (quizTotal > 0 && quizScore == quizTotal) 3 else if (passed) 2 else 1
                    LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                        item { LessonSurface {
                            Text(if (passed) "🎉 ${if (isId) "Materi berhasil" else "Lesson completed"}" else "🔄 ${if (isId) "Perlu latihan lagi" else "Practice again"}", color = NihongoPalette.white, fontSize = 23.sp, fontWeight = FontWeight.Black)
                            Text("★".repeat(stars), color = CreditGold, fontSize = 38.sp)
                            Text("$quizScore/$quizTotal · ${if (isId) "Minimal" else "Minimum"} $required", color = NihongoPalette.dim, fontSize = 15.sp)
                            Text(if (isId) "Nilai lulus minimal 80%. Bintang dan skor akan disimpan ke akun saat tombol selesai ditekan." else "Passing requires 80%. Stars and score are saved when you finish.", color = NihongoPalette.dim, fontSize = 12.sp)
                        } }
                        item {
                            Button(enabled = !busy && !saved, onClick = {
                                busy = true
                                scope.launch {
                                    try {
                                        // The server is authoritative; do not mark complete before success.
                                        repository.saveLessonProgress(token, lesson.optString("id"), stars, passed, quizScore, quizTotal)
                                        if (quizTotal >= 5) runCatching { repository.reportLessonQuiz(token, quizScore, quizTotal, lesson.optString("id")) }
                                        saved = true
                                        onSaved()
                                    } catch (e: Exception) { onNotice(e.message ?: "Gagal menyimpan progress") }
                                    finally { busy = false }
                                }
                            }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = accent), shape = RoundedCornerShape(16.dp)) {
                                Text(if (busy) "Menyimpan..." else if (isId) "Simpan Hasil dan Kembali" else "Save Result & Return", fontWeight = FontWeight.Black)
                            }
                        }
                        item { OutlinedButton(onClick = { quizScore = 0; quizTotal = 0; pageIndex = 0 }, modifier = Modifier.fillMaxWidth()) { Text(if (isId) "Ulangi Materi" else "Retry Lesson") } }
                    }
                }
                else -> {
                    key(pageIndex) {
                        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                            item { LessonPageContent(page, lesson, language, ::speak) }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (pageIndex > 0) OutlinedButton(onClick = { pageIndex-- }, modifier = Modifier.weight(.44f).height(50.dp), shape = RoundedCornerShape(15.dp)) { Text(if (isId) "Kembali" else "Previous") }
                        Button(onClick = { pageIndex = (pageIndex + 1).coerceAtMost(pages.lastIndex) }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                            Text(if (isId) "Lanjutkan →" else "Continue →", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

private fun lessonStage(type: String, isId: Boolean): String = when (type) {
    "intro" -> if (isId) "Pembuka" else "Introduction"
    "studyPattern" -> if (isId) "Pola" else "Pattern"
    "studyExplanation" -> if (isId) "Penjelasan" else "Explanation"
    "studyFormation" -> if (isId) "Pembentukan" else "Formation"
    "char", "phrase" -> if (isId) "Contoh" else "Examples"
    "studySenseiNote" -> if (isId) "Catatan Sensei" else "Sensei's note"
    "studyMistakes" -> if (isId) "Kesalahan Umum" else "Common mistakes"
    "miniPractice" -> if (isId) "Latihan Singkat" else "Mini practice"
    "studySummary" -> if (isId) "Ringkasan" else "Summary"
    "quiz" -> if (isId) "Tes" else "Quiz"
    "done" -> if (isId) "Hasil" else "Result"
    else -> type
}

@Composable
internal fun LessonSurface(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = NihongoPalette.surface, shape = RoundedCornerShape(19.dp), border = BorderStroke(1.dp, NihongoPalette.border)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun LessonText(title: String, text: String) {
    if (text.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = NihongoPalette.redSoft, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Text(text, color = NihongoPalette.white, fontSize = 14.sp, lineHeight = 22.sp)
    }
}

@Composable
private fun LessonPageContent(page: JSONObject, lesson: JSONObject, lang: String, speak: (String) -> Unit) {
    val isId = lang == "id"
    val type = page.optString("type")
    val study = page.optJSONObject("study") ?: lesson.optJSONObject("study") ?: JSONObject()
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LessonSurface {
            Text(lessonStage(type, isId).uppercase(), color = lesson.jlptColor(), fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, fontSize = 11.sp)
            Text(page.localized("title", lang).ifBlank { lesson.localized("title", lang) }, color = NihongoPalette.white, fontWeight = FontWeight.Black, fontSize = 23.sp, lineHeight = 29.sp)
            if (type == "char" || type == "phrase") {
                val jp = page.optString("jp")
                if (jp.isNotBlank()) {
                    Text(jp, color = NihongoPalette.white, fontSize = if (jp.length < 5) 57.sp else 34.sp, lineHeight = 60.sp, fontWeight = FontWeight.Bold)
                    Text(page.optString("romaji"), color = CreditGold, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { speak(jp) }) { Icon(Icons.Outlined.VolumeUp, null); Spacer(Modifier.width(7.dp)); Text(if (isId) "Dengarkan" else "Listen") }
                }
                LessonText(if (isId) "Arti" else "Meaning", page.localized("meaning", lang))
                LessonText(if (isId) "Ingat" else "Remember", page.localized("mnemonic", lang))
                LessonText(if (isId) "Catatan" else "Note", page.localized("note", lang))
                val word = page.optJSONObject("word")
                if (word != null) LessonText(if (isId) "Contoh kata" else "Word example", "${word.optString("jp")} (${word.optString("romaji")}) · ${word.localized(lang)}")
                val guide = page.optJSONObject("guide")
                if (guide != null) {
                    listOf("focus", "breakdown", "pattern", "tip", "warning").forEach { field ->
                        LessonText(field.replaceFirstChar { it.uppercase() }, guide.localized(field, lang))
                    }
                }
            } else {
                LessonText(if (isId) "Tujuan" else "Goal", study.localized("goal", lang))
                when (type) {
                    "intro" -> {
                        LessonText(if (isId) "Pembuka" else "Opening", page.localized("body", lang).ifBlank { study.localized("opening", lang) })
                        page.optJSONArray("preview")?.strings()?.take(8)?.let { preview -> Text(preview.joinToString("　"), color = CreditGold, fontSize = 27.sp) }
                    }
                    "studyPattern" -> LessonText(if (isId) "Pola utama" else "Main pattern", study.localized("mainPattern", lang))
                    "studyExplanation" -> {
                        LessonText(if (isId) "Penjelasan Sensei" else "Sensei's explanation", study.localized("teacherExplanation", lang))
                        LessonText(if (isId) "Detail" else "Details", study.localized("detailedExplanation", lang))
                    }
                    "studyFormation" -> {
                        study.optJSONArray("formationSteps")?.let { steps ->
                            for (i in 0 until minOf(steps.length(), 15)) {
                                val v = steps.opt(i)
                                val line = if (v is JSONObject) v.localized(lang).ifBlank { v.localized("text", lang) } else v?.toString().orEmpty()
                                LessonText("${i + 1}.", line)
                            }
                        }
                    }
                    "studySenseiNote" -> LessonText(if (isId) "Catatan Sensei" else "Sensei's note", study.localized("senseiNote", lang))
                    "studyMistakes" -> {
                        study.optJSONArray("commonMistakes")?.let { rows ->
                            for (i in 0 until minOf(rows.length(), 12)) {
                                val r = rows.opt(i)
                                val t = if (r is JSONObject) r.localized(lang) else r?.toString().orEmpty()
                                LessonText("⚠ ${i + 1}", t)
                            }
                        }
                    }
                    "miniPractice" -> {
                        study.optJSONArray("miniPractice")?.let { rows ->
                            for (i in 0 until minOf(rows.length(), 12)) {
                                val r = rows.optJSONObject(i) ?: continue
                                LessonText(r.localized("title", lang).ifBlank { "${i + 1}" }, r.localized("prompt", lang))
                            }
                        }
                    }
                    "studySummary" -> {
                        LessonText(if (isId) "Ringkasan" else "Summary", study.localized("summary", lang))
                        study.optJSONArray("summaryPoints")?.let { rows ->
                            for (i in 0 until minOf(rows.length(), 10)) {
                                val r = rows.opt(i)
                                LessonText("✓", if (r is JSONObject) r.localized(lang) else r?.toString().orEmpty())
                            }
                        }
                    }
                    else -> LessonText(if (isId) "Penjelasan" else "Explanation", page.localized("body", lang))
                }
            }
        }
    }
}

private fun quizText(value: Any?, lang: String): String = when (value) {
    is JSONObject -> value.localized(lang)
    is String -> value
    is Number -> value.toString()
    else -> ""
}

private fun quizOptions(question: JSONObject, lang: String): List<String> {
    val raw = question.opt("opts")
    val arr = if (raw is JSONObject) raw.optJSONArray(lang) ?: raw.optJSONArray("id") else raw as? JSONArray
    return (0 until (arr?.length() ?: 0)).mapNotNull { quizText(arr?.opt(it), lang).takeIf { s -> s.isNotBlank() } }
}

/** Quiz is native, consumes the original seven quiz kinds, supports review and 80% pass. */
@Composable
private fun NativeLessonQuiz(page: JSONObject, lesson: JSONObject, lang: String, speak: (String) -> Unit, onFinish: (Int, Int, JSONArray) -> Unit) {
    val isId = lang == "id"
    val questions = remember(page) {
        val raw = page.arr("questions")
        val limit = page.optInt("questionLimit", raw.size).coerceIn(1, raw.size.coerceAtLeast(1))
        val kinds = listOf("reading_choice", "character_choice", "multiple_choice", "matching", "sentence_matching", "listening_mini", "arrange")
        val quota = if (limit == 12) listOf(2, 2, 2, 2, 1, 2, 1) else if (limit == 15) listOf(3, 2, 2, 2, 2, 2, 2)
            else List(kinds.size) { limit / kinds.size + if (it < limit % kinds.size) 1 else 0 }
        val shuffled = raw.shuffled()
        val chosen = mutableListOf<JSONObject>()
        kinds.forEachIndexed { i, kind ->
            chosen.addAll(shuffled.filter { it.optString("kind", "multiple_choice") == kind && it !in chosen }.take(quota[i]))
        }
        chosen.addAll(shuffled.filter { it !in chosen }.take((limit - chosen.size).coerceAtLeast(0)))
        chosen.take(limit).shuffled()
    }
    var index by remember(page) { mutableIntStateOf(0) }
    var mistakes by remember(page) { mutableStateOf<List<Int>>(emptyList()) }
    var round by remember(page) { mutableStateOf("main") }
    var mastered by remember(page) { mutableStateOf<Set<Int>>(emptySet()) }
    var wrongEntries by remember(page) { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selected by remember(page, index, round) { mutableStateOf<String?>(null) }
    var arranged by remember(page, index, round) { mutableStateOf<List<Int>>(emptyList()) }
    val activeIndices = if (round == "review") mistakes else questions.indices.toList()
    val questionId = activeIndices.getOrNull(index)
    val q = questionId?.let { questions[it] }
    val kind = q?.optString("kind", "multiple_choice") ?: "multiple_choice"
    val answer = quizText(q?.opt("answer"), lang)
    val opts = remember(q, lang) {
        if (q == null) emptyList() else (quizOptions(q, lang) + answer).filter { it.isNotBlank() }.distinct().shuffled()
    }
    val tokens = remember(q) { q?.optJSONArray("answerTokens")?.strings()?.mapIndexed { i, s -> i to s }?.shuffled().orEmpty() }
    fun continueQuiz() {
        val id = questionId ?: return
        val response = selected ?: return
        val correct = response == answer || (kind == "arrange" && response.replace(Regex("[\\s。、，,.]"), "") == answer.replace(Regex("[\\s。、，,.]"), ""))
        if (correct) mastered = mastered + id
        else if (round == "main") {
            mistakes = mistakes + id
            wrongEntries = wrongEntries + JSONObject()
                .put("lessonId", lesson.optString("id"))
                .put("questionId", "${lesson.optString("id")}-${id}")
                .put("questionText", q.optString("q", q.optString("audioText")))
                .put("correctAnswer", answer).put("selectedAnswer", response)
                .put("options", JSONArray(if (kind == "arrange") tokens.map { it.second } else opts))
                .put("quizType", kind)
                .put("promptText", q.localized("prompt", lang))
        }
        if (index + 1 < activeIndices.size) index++
        else if (round == "main" && mistakes.isNotEmpty()) { round = "review"; index = 0 }
        else onFinish(mastered.size, questions.size, JSONArray(wrongEntries))
        selected = null; arranged = emptyList()
    }
    if (q == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Button(onClick = { onFinish(mastered.size, questions.size, JSONArray(wrongEntries)) }) { Text(if (isId) "Selesai" else "Finish") } }
        return
    }
    Column(Modifier.fillMaxSize()) {
        LinearProgressIndicator(progress = { (index + 1f) / activeIndices.size.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth().height(4.dp), color = lesson.jlptColor(), trackColor = NihongoPalette.surface3)
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { LessonSurface {
                Text(if (round == "review") (if (isId) "ULANGI JAWABAN SALAH" else "REVIEW MISTAKES") else "TES · ${index + 1}/${activeIndices.size}", color = lesson.jlptColor(), fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text(if (isId) when (kind) { "reading_choice" -> "Pilih Bacaan"; "character_choice" -> "Pilih Huruf"; "matching", "sentence_matching" -> "Cocokkan Pasangan"; "listening_mini" -> "Dengarkan"; "arrange" -> "Susun Kalimat"; else -> "Pilih Jawaban" }
                    else kind.replace('_',' ').replaceFirstChar { it.uppercase() }, color = NihongoPalette.dim, fontSize = 13.sp)
                Text(q.localized("prompt", lang).ifBlank { if (isId) "Pilih jawaban yang tepat." else "Choose the correct answer." }, color = NihongoPalette.dim, fontSize = 12.sp)
                if (kind != "listening_mini") Text(q.optString("q"), color = NihongoPalette.white, fontSize = if (q.optString("q").length < 20) 27.sp else 18.sp, fontWeight = FontWeight.Bold, lineHeight = 31.sp)
                if (kind == "listening_mini" || (q.optString("q").any { it.code > 0x3000 } && kind != "character_choice")) {
                    OutlinedButton(onClick = { speak(q.optString("audioText", q.optString("q"))) }) { Icon(Icons.Outlined.VolumeUp, null); Spacer(Modifier.width(6.dp)); Text(if (isId) "Putar suara Jepang" else "Play Japanese") }
                }
            } }
            if (kind == "arrange") {
                item {
                    LessonSurface {
                        Text(if (isId) "Urutan pilihanmu:" else "Your arrangement:", color = NihongoPalette.dim, fontSize = 12.sp)
                        Text(arranged.map { tokens[it].second }.joinToString(""), color = NihongoPalette.white, fontSize = 23.sp, lineHeight = 30.sp)
                        OutlinedButton(onClick = { arranged = emptyList(); selected = null }) { Text(if (isId) "Ulangi susunan" else "Reset arrangement") }
                    }
                }
                itemsIndexed(tokens) { i, pair ->
                    if (i !in arranged) QuizAnswerChip(pair.second, false) {
                        arranged = arranged + i
                        selected = arranged.map { tokens[it].second }.joinToString("")
                    }
                }
            } else {
                itemsIndexed(opts) { _, option -> QuizAnswerChip(option, selected == option) { selected = option } }
            }
        }
        Button(enabled = selected != null, onClick = { continueQuiz() }, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp).height(52.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = lesson.jlptColor())) {
            Text(if (isId) "Periksa dan Lanjutkan" else "Check and Continue", fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun QuizAnswerChip(text: String, active: Boolean, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp),
        color = if (active) NihongoPalette.red.copy(alpha = .25f) else NihongoPalette.surface,
        border = BorderStroke(1.dp, if (active) NihongoPalette.redSoft else NihongoPalette.borderStrong)) {
        Text(text, Modifier.padding(15.dp), color = NihongoPalette.white, fontSize = 14.sp, lineHeight = 20.sp)
    }
}
