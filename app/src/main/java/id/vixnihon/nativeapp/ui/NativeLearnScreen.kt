package id.vixnihon.nativeapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.vixnihon.nativeapp.data.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.roundToInt
import kotlin.math.sin

/** Native copy of LearnScreen.jsx's latest access rules: choose any JLPT/chapter/lesson.
 * Progress is mastery-only; credit energy is the ONLY lesson gate, owned by the backend.
 */
@Composable
internal fun NativeLearnScreen(
    language: String, token: String, repository: AuthRepository,
    onBack: () -> Unit, onTopup: () -> Unit, onNotice: (String) -> Unit
) {
    val isId = language == "id"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var catalog by remember { mutableStateOf<JSONObject?>(null) }
    var progress by remember { mutableStateOf<Map<String, NativeLessonProgress>>(emptyMap()) }
    var balance by remember { mutableLongStateOf(0L) }
    var view by remember { mutableStateOf("levels") }
    var activeJlpt by remember { mutableStateOf("n5") }
    var activeChapterId by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var chapterSheet by remember { mutableStateOf<JSONObject?>(null) }
    var lessonSheet by remember { mutableStateOf<JSONObject?>(null) }
    var blocked by remember { mutableStateOf<JSONObject?>(null) }
    var confirm by remember { mutableStateOf<JSONObject?>(null) }
    var pendingLesson by remember { mutableStateOf<JSONObject?>(null) }
    var player by remember { mutableStateOf<JSONObject?>(null) }
    var busy by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var assetsReady by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(token, refresh) {
        loading = catalog == null
        try {
            coroutineScope {
                val metaTask = async { if (catalog == null) NativeLearnData.catalog(context) else catalog }
                val progressTask = async { runCatching { repository.fetchProgress(token) }.getOrNull() }
                val walletTask = async { runCatching { repository.fetchWallet(token) }.getOrNull() }
                catalog = metaTask.await()
                if (assetsReady == null) assetsReady = NativeLearnData.preflight(context)
                progressTask.await()?.let { resp ->
                    val entries = resp.optJSONObject("progress") ?: resp
                    progress = buildMap {
                        val keys = entries.keys()
                        while (keys.hasNext()) {
                            val id = keys.next(); val item = entries.optJSONObject(id) ?: continue
                            put(id, NativeLessonProgress(item.optBoolean("completed"), item.optInt("stars"), item.optInt("score"), item.optInt("total")))
                        }
                    }
                }
                walletTask.await()?.let { balance = it.optJSONObject("balance")?.optLong("creditBalance") ?: 0L }
            }
        } catch (e: Exception) { onNotice(e.message ?: "Gagal memuat katalog") }
        finally { loading = false }
    }

    val chapters = remember(catalog, activeJlpt) {
        catalog?.arr("chapters")?.filter { it.optString("jlpt") == activeJlpt }?.sortedBy { it.optInt("no") }.orEmpty()
    }
    val lessons = remember(catalog) { catalog?.arr("lessons").orEmpty() }
    val lessonLookup = remember(lessons) { lessons.associateBy { it.optString("id") } }
    val activeChapter = chapters.firstOrNull { it.optString("id") == activeChapterId } ?: chapters.firstOrNull()
    val chapterLessons = remember(activeChapter, lessonLookup) {
        activeChapter?.optJSONArray("lessonIds")?.strings()?.mapNotNull { lessonLookup[it] }.orEmpty()
    }
    fun done(id: String) = progress[id]?.passed == true
    fun back() {
        if (player != null) { player = null; refresh++; return }
        if (view == "lessons") { view = "chapters"; filter = "all" }
        else if (view == "chapters") view = "levels"
        else onBack()
    }
    BackHandler { back() }

    fun loadLesson(lesson: JSONObject) {
        if (busy) return
        val lessonId = lesson.optString("id")
        if (assetsReady == false) {
            onNotice(if (isId) "Paket materi tidak ditemukan di APK ini. Instal ZIP v0.1.27 yang sudah dibuild ulang." else "Lesson assets missing from installed APK. Rebuild and install the latest version.")
            return
        }
        busy = true
        pendingLesson = lesson
        scope.launch {
            try {
                // Load payload off-main before spending credits. No WebView/JS runtime needed.
                val payload = NativeLearnData.lesson(context, lessonId)
                if (!done(lessonId)) {
                    val access = repository.lessonAccess(token, lessonId)
                    val energy = access.optJSONObject("energy")
                    if (energy != null) {
                        if (!energy.optBoolean("sufficient", true)) {
                            blocked = JSONObject().put("balance", energy.optLong("balance"))
                                .put("cost", energy.optLong("creditsPerLesson"))
                            return@launch
                        }
                        val requiresConfirm = energy.optBoolean("enabled") && energy.optLong("creditsPerLesson") > 0L &&
                            !energy.optBoolean("alreadyPaid") && !energy.optBoolean("devBypass") && !energy.optBoolean("unlimitedActive")
                        if (requiresConfirm) {
                            confirm = JSONObject().put("balance", energy.optLong("balance"))
                                .put("cost", energy.optLong("creditsPerLesson"))
                            return@launch
                        }
                    }
                }
                lessonSheet = null
                player = payload
            } catch (e: Exception) { onNotice(e.message ?: "Gagal memeriksa akses materi") }
            finally { busy = false }
        }
    }

    // Keep expensive lesson-player composition isolated from the roadmap.
    if (player != null) {
        NativeLessonPlayer(player ?: JSONObject(), language, token, repository,
            onBack = { player = null; refresh++ },
            onSaved = { player = null; refresh++ }, onNotice = onNotice)
        return
    }

    if (loading && catalog == null) {
        NativeGlobalLoading(language = language, variant = "page", compact = true)
        return
    }

    Column(Modifier.fillMaxSize().background(NihongoPalette.background)) {
        val totalDone = lessons.count { done(it.optString("id")) }
        val currentChapterIds = when (view) {
            "levels" -> lessons.map { it.optString("id") }
            "chapters" -> lessons.filter { it.optString("jlpt") == activeJlpt }.map { it.optString("id") }
            else -> chapterLessons.map { it.optString("id") }
        }
        val pct = if (currentChapterIds.isEmpty()) 0 else
            (currentChapterIds.count(::done) * 100f / currentChapterIds.size).roundToInt()
        val title = when (view) {
            "levels" -> "JLPT Roadmap"
            "chapters" -> "${activeJlpt.uppercase()} · ${when (activeJlpt) { "n5" -> if (isId) "Dasar" else "Basic"; "n4" -> if (isId) "Dasar Lanjutan" else "Upper Basic"; "n3" -> if (isId) "Menengah" else "Intermediate"; "n2" -> if (isId) "Mahir" else "Advanced"; else -> "Expert" }}"
            else -> activeChapter?.localized("title", language) ?: activeJlpt.uppercase()
        }
        val subtitle = when (view) {
            "levels" -> if (isId) "$totalDone dari ${lessons.size} materi selesai" else "$totalDone of ${lessons.size} lessons completed"
            "chapters" -> if (isId) "Pilih bab untuk melanjutkan jalur belajarmu. Semua bab bisa dibuka tanpa syarat urutan." else "Choose your next chapter; no sequential locks."
            else -> activeChapter?.localized("goal", language).orEmpty()
        }
        val accent = if (view == "lessons") activeChapter?.jlptColor() ?: levelColor(activeJlpt) else levelColor(activeJlpt)
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (view != "levels") Surface(Modifier.size(38.dp).clickable { back() }, color = NihongoPalette.surface2, shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp,NihongoPalette.border)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.ArrowBack, null, tint = NihongoPalette.white) }
            }
            Column(Modifier.weight(1f)) {
                Text(if (view == "levels") "BELAJAR" else if (view == "chapters") "PILIH BAB" else "MATERI BAB", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text(title, color = NihongoPalette.white, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 2, lineHeight = 23.sp)
                Text(subtitle, color = NihongoPalette.dim, fontSize = 10.sp, maxLines = 3, lineHeight = 14.sp)
            }
            Surface(Modifier.clickable { onTopup() }, color = NihongoPalette.surface2, shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, CreditGold.copy(alpha = .4f))) {
                Text("◈ ${creditCount(balance)}", Modifier.padding(10.dp), color = CreditGold, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
        LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth().padding(horizontal = 17.dp).height(5.dp), color = accent, trackColor = NihongoPalette.surface3)
        Text("$pct%", Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 5.dp), color = NihongoPalette.dim, fontSize = 10.sp)

        val listState = rememberLazyListState()
        LaunchedEffect(view, activeJlpt, activeChapterId) { listState.scrollToItem(0) }
        LazyColumn(state = listState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 35.dp), verticalArrangement = Arrangement.spacedBy(if (view == "levels") 12.dp else 0.dp)) {
            when (view) {
                "levels" -> {
                    item { LearnInfo(if (isId) "Pilih level, bab, lalu materi apa saja. Bintang menunjukkan penguasaanmu; hanya energi/kredit yang membatasi akses." else "Choose any level, chapter, and lesson. Stars measure mastery; only credits/energy gate access.") }
                    item { LearnEnergyCard(balance, isId, onTopup) }
                    itemsIndexed(listOf("n5", "n4", "n3", "n2", "n1")) { _, level ->
                        val ids = lessons.filter { it.optString("jlpt") == level }.map { it.optString("id") }
                        val chs = catalog?.arr("chapters")?.count { it.optString("jlpt") == level } ?: 0
                        val complete = ids.count(::done)
                        LearnLevelCard(level, chs, complete, ids.size, isId) { activeJlpt = level; view = "chapters" }
                    }
                }
                "chapters" -> {
                    item {
                        val currentIds = lessons.filter { it.optString("jlpt") == activeJlpt }.map { it.optString("id") }
                        val earnedStars = currentIds.sumOf { progress[it]?.stars ?: 0 }
                        LearnInfo((if (isId) "Progress level" else "Level progress") + "  ·  ★ $earnedStars/${currentIds.size * 3}\n" +
                            (if (isId) "Pilih bab mana saja. N5 Dasar adalah fondasi, N5 Full Prep untuk persiapan ujian." else "Choose any chapter. N5 Basic builds fundamentals; Full Prep prepares you for exams."), levelColor(activeJlpt))
                    }
                    if (activeJlpt == "n5") item { LearnInfo(if (isId) "N5 Dasar (Bab 1–15) → N5 Full Prep (Bab 16–27)" else "N5 Basic (Chapters 1–15) → N5 Full Prep (Chapters 16–27)", levelColor(activeJlpt)) }
                    itemsIndexed(chapters, key = { _, c -> c.optString("id") }) { i, chapter ->
                        val ids = chapter.optJSONArray("lessonIds")?.strings().orEmpty()
                        val completed = ids.count(::done)
                        Column {
                            if (activeJlpt == "n5" && (i == 0 || chapter.optInt("no") == 16)) {
                                LearnSectionDivider(if (i == 0) "N5 Dasar" else "N5 Full Prep",
                                    if (i == 0) "Fondasi huruf Jepang, perkenalan, partikel, dan kalimat dasar." else "Kosakata, kanji, grammar, reading, listening, dan latihan ujian.", levelColor(activeJlpt))
                            }
                            LearnWindingNode(index = if (activeJlpt == "n5" && chapter.optInt("no") >= 16) i - 15 else i,
                                count = if (activeJlpt != "n5") chapters.size else if (chapter.optInt("no") >= 16) chapters.size - 15 else 15, badge = chapter.optString("badge", "${chapter.optInt("no")}"),
                                label = if (isId) "Bab ${chapter.optInt("no")}" else "Chapter ${chapter.optInt("no")}",
                                color = chapter.jlptColor(), finished = completed == ids.size && ids.isNotEmpty(),
                                stars = 0, isChapter = true, onClick = { chapterSheet = chapter })
                        }
                    }
                }
                else -> {
                    item { LearnInfo(activeChapter?.localized("goal", language).orEmpty()) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("all", "todo", "done").forEach { value ->
                                val label = when (value) { "all" -> if (isId) "Semua" else "All"; "todo" -> if (isId) "Belum selesai" else "Todo"; else -> if (isId) "Selesai" else "Done" }
                                Surface(Modifier.weight(1f).clickable { filter = value }, shape = RoundedCornerShape(30.dp),
                                    color = if (filter == value) accent.copy(alpha = .23f) else NihongoPalette.surface,
                                    border = BorderStroke(1.dp, if (filter == value) accent else NihongoPalette.border)) {
                                    Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) { Text(label, color = NihongoPalette.white, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                    val visible = chapterLessons.filter {
                        when (filter) { "done" -> done(it.optString("id")); "todo" -> !done(it.optString("id")); else -> true }
                    }
                    if (visible.isEmpty()) item { LearnInfo(if (isId) "Belum ada materi pada filter ini." else "No lessons match this filter.") }
                    itemsIndexed(visible, key = { _, l -> l.optString("id") }) { i, lesson ->
                        val p = progress[lesson.optString("id")]
                        LearnWindingNode(index = i, count = visible.size,
                            badge = lesson.optString("badge", "文"), label = lesson.localized("title", language),
                            color = lesson.jlptColor(), finished = p?.passed == true, stars = p?.stars ?: 0,
                            isChapter = false, onClick = { lessonSheet = lesson })
                    }
                    item { LearnCheckpoint(chapterLessons.count { done(it.optString("id")) }, chapterLessons.size, isId, accent) }
                }
            }
        }
        NativeBottomNav(isId = isId, activeKey = "learn", onSelect = { tab ->
            when (tab) {
                "home" -> onBack()
                "learn" -> { view = "levels"; filter = "all" }
                else -> onNotice(if (isId) "Menu ini sedang dikembangkan di native." else "This tab is not available yet.")
            }
        })
    }

    chapterSheet?.let { chapter ->
        val ids = chapter.optJSONArray("lessonIds")?.strings().orEmpty()
        LearnDetailSheet(title = chapter.localized("title", language), desc = chapter.localized("desc", language),
            kicker = if (isId) "Bab ${chapter.optInt("no")}" else "Chapter ${chapter.optInt("no")}", badge = chapter.optString("badge", "文"), color = chapter.jlptColor(),
            status = "${ids.count(::done)}/${ids.size} ${if (isId) "materi" else "lessons"}  ·  ★ ${ids.sumOf { progress[it]?.stars ?: 0 }}/${ids.size * 3}",
            action = if (isId) "Lihat Materi" else "View Lessons", onClose = { chapterSheet = null },
            onStart = { activeChapterId = chapter.optString("id"); chapterSheet = null; filter = "all"; view = "lessons" })
    }
    lessonSheet?.let { lesson ->
        val p = progress[lesson.optString("id")]
        LearnDetailSheet(title = lesson.localized("title", language), desc = lesson.localized("desc", language),
            kicker = if (isId) "Materi" else "Lesson", badge = lesson.optString("badge", "文"), color = lesson.jlptColor(),
            status = if (p?.passed == true) "★ ${p.stars} · ${p.score}/${p.total}" else if (isId) "Materi tersedia" else "Lesson available",
            action = if (busy) "Memeriksa..." else if (p?.passed == true) "Ulangi Materi" else "Mulai Belajar",
            onClose = { if (!busy) lessonSheet = null }, onStart = { loadLesson(lesson) })
    }
    confirm?.let { energy ->
        AlertDialog(onDismissRequest = { if (!busy) confirm = null },
            title = { Text(if (isId) "Konfirmasi Energi" else "Confirm Energy") },
            text = { Text(if (isId) "Mulai materi dengan ${energy.optLong("cost")} kredit? Saldo: ${energy.optLong("balance")}. Potongan dilakukan satu kali oleh server." else "Start this lesson for ${energy.optLong("cost")} credits? Balance: ${energy.optLong("balance")}. Charged once by the server.") },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                val target = pendingLesson ?: return@TextButton
                busy = true
                scope.launch {
                    try {
                        repository.startLesson(token, target.optString("id"))
                        balance = (balance - energy.optLong("cost")).coerceAtLeast(0L)
                        player = NativeLearnData.lesson(context, target.optString("id"))
                        confirm = null; lessonSheet = null
                    } catch (e: Exception) {
                        confirm = null; onNotice(e.message ?: "Gagal memulai materi")
                        refresh++ // Fetch authoritative balance again after an ambiguous network failure.
                    } finally { busy = false }
                }
            }) { Text(if (isId) "Mulai" else "Start") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { confirm = null }) { Text(if (isId) "Batal" else "Cancel") } })
    }
    blocked?.let { energy ->
        AlertDialog(onDismissRequest = { blocked = null },
            title = { Text(if (isId) "Kredit belum cukup" else "Not enough credits") },
            text = { Text(if (isId) "Materi ini membutuhkan ${energy.optLong("cost")} kredit. Saldo kamu ${energy.optLong("balance")}." else "This lesson costs ${energy.optLong("cost")} credits. Balance: ${energy.optLong("balance")}.") },
            confirmButton = { TextButton(onClick = { blocked = null; lessonSheet = null; onTopup() }) { Text(if (isId) "Isi Kredit" else "Top up") } },
            dismissButton = { TextButton(onClick = { blocked = null }) { Text(if (isId) "Nanti" else "Later") } })
    }
}

internal fun levelColor(level: String): Color = when (level) {
    "n5" -> Color(0xFF4080EE); "n4" -> Color(0xFF27AE60); "n3" -> Color(0xFFE8863A)
    "n2" -> Color(0xFF9B59B6); else -> Color(0xFFC42B2B)
}

@Composable
private fun LearnInfo(message: String, color: Color = NihongoPalette.dim) {
    Surface(Modifier.fillMaxWidth(), color = NihongoPalette.surface, shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, NihongoPalette.border)) {
        Text(message, Modifier.padding(14.dp), color = color, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun LearnLevelCard(level: String, chapters: Int, done: Int, total: Int, isId: Boolean, onClick: () -> Unit) {
    val color = levelColor(level)
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), color = NihongoPalette.surface, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, color.copy(alpha = .38f))) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(Modifier.size(54.dp), color = color.copy(alpha = .18f), shape = RoundedCornerShape(19.dp), border = BorderStroke(1.dp, color.copy(alpha = .4f))) {
                Box(contentAlignment = Alignment.Center) { Text(level.uppercase(), color = color, fontSize = 17.sp, fontWeight = FontWeight.Black) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("${level.uppercase()} · ${when (level) { "n5" -> if (isId) "Dasar" else "Basic"; "n4" -> if (isId) "Dasar Lanjutan" else "Upper Basic"; "n3" -> if (isId) "Menengah" else "Intermediate"; "n2" -> if (isId) "Mahir" else "Advanced"; else -> "Expert" }}", color = NihongoPalette.white, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text("$chapters ${if (isId) "bab" else "chapters"} · $done/$total ${if (isId) "materi" else "lessons"}", color = NihongoPalette.dim, fontSize = 10.sp)
                LinearProgressIndicator(progress = { if (total == 0) 0f else done.toFloat() / total }, modifier = Modifier.fillMaxWidth().height(5.dp), color = color, trackColor = NihongoPalette.surface3)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = color)
        }
    }
}


@Composable
private fun LearnEnergyCard(balance: Long, isId: Boolean, onTopup: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = NihongoPalette.surface, shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, CreditGold.copy(alpha = .36f))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(Modifier.size(48.dp), color = CreditGold.copy(alpha = .13f), shape = RoundedCornerShape(16.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("◈", color = CreditGold, fontSize = 27.sp) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(if (isId) "Energi belajar" else "Study energy", color = NihongoPalette.white, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(if (isId) "Kredit dipakai sekali saat mulai materi baru. Buka ulang tetap gratis." else "Credits are charged once per new lesson. Reopening is free.",
                    color = NihongoPalette.dim, fontSize = 11.sp, lineHeight = 16.sp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(creditCount(balance), color = CreditGold, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Surface(Modifier.clickable(onClick = onTopup), color = CreditGold, shape = RoundedCornerShape(11.dp)) {
                    Text(if (isId) "Isi ulang" else "Top up", Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        color = Color(0xFF2B1700), fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun LearnSectionDivider(title: String, desc: String, color: Color) {
    Surface(Modifier.fillMaxWidth().padding(vertical = 7.dp), color = NihongoPalette.surface,
        shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, color.copy(alpha = .34f))) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title.uppercase(), color = color, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(Modifier.height(5.dp))
                Text(desc, color = NihongoPalette.dim, fontSize = 11.sp, lineHeight = 16.sp)
            }
            Icon(Icons.Outlined.Book, null, tint = color)
        }
    }
}

/** Matches LearnScreen.jsx WindingPath + MapNode; a small circular stop on a sinuous route.
 * Each item is independently lazy to avoid measuring hundreds of long rows on navigation.
 */
@Composable
private fun LearnWindingNode(index: Int, count: Int, badge: String, label: String,
    color: Color, finished: Boolean, stars: Int, isChapter: Boolean, onClick: () -> Unit
) {
    val stepHeight = if (isChapter) 130.dp else 112.dp
    val nodeWidth = 112.dp
    val circleSize = if (isChapter) 60.dp else 50.dp
    fun pathX(i: Int): Float = 0.5f + .30f * sin(i * 1.15f + .4f)
    BoxWithConstraints(Modifier.fillMaxWidth().height(stepHeight)) {
        val currentX = pathX(index)
        val left = (maxWidth * currentX - nodeWidth / 2).coerceIn(0.dp, (maxWidth - nodeWidth).coerceAtLeast(0.dp))
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width * currentX
            val cy = size.height * .50f
            val path = Path()
            if (index > 0) {
                val prevX = size.width * pathX(index - 1)
                path.moveTo(prevX, 0f)
                path.cubicTo(prevX, size.height * .27f, cx, size.height * .22f, cx, cy)
            }
            if (index < count - 1) {
                val nextX = size.width * pathX(index + 1)
                path.moveTo(cx, cy)
                path.cubicTo(cx, size.height * .75f, nextX, size.height * .75f, nextX, size.height)
            }
            drawPath(path, color = Color.White.copy(alpha = .16f),
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 7.dp.toPx()))))
        }
        Column(Modifier.offset(x = left).width(nodeWidth).align(Alignment.CenterStart)
            .clickable(onClick = onClick).padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Surface(Modifier.size(circleSize), color = if (finished) color else color.copy(alpha = .16f),
                shape = CircleShape, border = BorderStroke(if (!finished && index == 0) 2.5.dp else 1.5.dp,
                    if (!finished && index == 0) CreditGold else color.copy(alpha = .68f)),
                shadowElevation = if (!finished && index == 0) 5.dp else 2.dp) {
                Box(contentAlignment = Alignment.Center) {
                    if (finished) Icon(Icons.Outlined.CheckCircle, null, tint = Color.White, modifier = Modifier.size(25.dp))
                    else Text(badge.take(3), color = color, fontSize = 17.sp, fontWeight = FontWeight.Black)
                }
            }
            if (stars > 0) Text("★".repeat(stars.coerceIn(1, 3)), color = CreditGold, fontSize = 10.sp, lineHeight = 12.sp)
            Text(label, color = NihongoPalette.dim, fontWeight = FontWeight.Black, fontSize = 10.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 13.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LearnCheckpoint(done: Int, total: Int, isId: Boolean, color: Color) {
    Surface(Modifier.fillMaxWidth().padding(vertical = 8.dp), color = NihongoPalette.surface,
        shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, if (done == total && total > 0) color.copy(alpha = .44f) else NihongoPalette.border)) {
        Row(Modifier.padding(15.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.School, null, tint = if (done == total && total > 0) color else NihongoPalette.dim)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (isId) "Checkpoint Bab" else "Chapter Checkpoint", color = NihongoPalette.white, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text(if (done == total && total > 0) (if (isId) "Semua materi selesai!" else "All lessons completed!")
                    else if (isId) "Selesaikan semua materi dalam urutan bebas. ($done/$total)" else "Complete each lesson in any order. ($done/$total)",
                    color = NihongoPalette.dim, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LearnDetailSheet(title: String, desc: String, status: String, action: String,
    kicker: String = "", badge: String = "文", color: Color = NihongoPalette.redLight,
    onClose: () -> Unit, onStart: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onClose, containerColor = NihongoPalette.surface2,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 26.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(52.dp), color = color.copy(alpha = .18f), shape = RoundedCornerShape(19.dp),
                    border = BorderStroke(1.dp, color.copy(alpha = .35f))) {
                    Box(contentAlignment = Alignment.Center) { Text(badge.take(2), color = color, fontSize = 21.sp, fontWeight = FontWeight.Black) }
                }
                Column(Modifier.weight(1f)) {
                    if (kicker.isNotBlank()) Text(kicker.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = NihongoPalette.dim)
                    Spacer(Modifier.height(4.dp))
                    Text(title, color = NihongoPalette.white, fontSize = 17.sp, fontWeight = FontWeight.Black, lineHeight = 22.sp)
                }
                Surface(Modifier.size(33.dp).clickable(onClick = onClose), color = NihongoPalette.background, shape = RoundedCornerShape(11.dp), border = BorderStroke(1.dp, NihongoPalette.border)) {
                    Box(contentAlignment = Alignment.Center) { Text("×", color = NihongoPalette.dim, fontSize = 20.sp) }
                }
            }
            Text(desc, color = NihongoPalette.dim, fontSize = 12.sp, lineHeight = 19.sp)
            Surface(color = NihongoPalette.background, shape = RoundedCornerShape(12.dp)) {
                Text(status, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = CreditGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onStart, colors = ButtonDefaults.buttonColors(containerColor = color),
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(action, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(7.dp))
                Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(17.dp))
            }
        }
    }
}
