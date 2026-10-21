package id.vixnihon.nativeapp.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.zIndex
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import id.vixnihon.nativeapp.R
import id.vixnihon.nativeapp.data.AppUser
import id.vixnihon.nativeapp.data.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.math.roundToInt

private data class NativeLessonMeta(
    val id: String,
    val no: Int,
    val jlpt: String,
    val titleId: String,
    val titleEn: String
)

private data class NativeChapterMeta(
    val id: String,
    val jlpt: String,
    val no: Int,
    val titleId: String,
    val titleEn: String,
    val lessonIds: List<String>
)

private data class NativeLessonCatalog(
    val lessons: List<NativeLessonMeta> = emptyList(),
    val chapters: List<NativeChapterMeta> = emptyList()
)

private data class NativeLessonProgress(val completed: Boolean, val stars: Int, val score: Int, val total: Int) {
    val passed: Boolean get() = completed && stars >= 2
}

private data class NativeDayStat(
    val key: String,
    val label: String,
    val xp: Int,
    val minutes: Int,
    val active: Boolean
)

private data class NativeWeeklyStats(
    val xpWeek: Int = 0,
    val accuracy: Int = 0,
    val completedLessons: Int = 0,
    val wrongActive: Int = 0,
    val studyMinutes: Int = 0,
    val averageMinutes: Int = 0,
    val streak: Int = 0,
    val xpTrendPct: Int = 0,
    val totalXp: Int = 0,
    val credits: Long = 0,
    val level: Int = 1,
    val xpIntoLevel: Int = 0,
    val xpForNext: Int = 1,
    val levelProgressPct: Int = 0,
    val days: List<NativeDayStat> = emptyList()
)

private data class NativeHomeLearning(
    val lesson: NativeLessonMeta? = null,
    val chapter: NativeChapterMeta? = null,
    val chapterDone: Int = 0,
    val chapterTotal: Int = 0,
    val chapterPct: Int = 0,
    val allDone: Int = 0
)

@Composable
fun HomeScreen(
    user: AppUser,
    authToken: String,
    repository: AuthRepository,
    language: String,
    onToggleLanguage: () -> Unit,
    onLogout: () -> Unit,
    onNotice: (String) -> Unit
) {
    val isId = language == "id"
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var catalog by remember { mutableStateOf(NativeLessonCatalog()) }
    var lessonProgress by remember { mutableStateOf<Map<String, NativeLessonProgress>>(emptyMap()) }
    var weeklyStats by remember { mutableStateOf<NativeWeeklyStats?>(null) }
    var refreshError by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    val firstName = remember(user.name, user.email) {
        (user.name.ifBlank { user.email.substringBefore('@') })
            .trim().split(Regex("\\s+")).firstOrNull().orEmpty().ifBlank { "Sensei" }
    }
    val clock = Calendar.getInstance()
    val minuteOfDay = clock.get(Calendar.HOUR_OF_DAY) * 60 + clock.get(Calendar.MINUTE)
    val heroDrawable = when (minuteOfDay) {
        in 270..359 -> R.drawable.home_hero_fajar
        in 360..599 -> R.drawable.home_hero_pagi
        in 600..899 -> R.drawable.home_hero_siang
        in 900..1049 -> R.drawable.home_hero_sore
        in 1050..1139 -> R.drawable.home_hero_senja
        in 1140..1379 -> R.drawable.home_hero_malam
        else -> R.drawable.home_hero_tengah_malam
    }

    LaunchedEffect(Unit) {
        catalog = withContext(Dispatchers.IO) { readNativeLessonCatalog(context) }
    }
    LaunchedEffect(authToken, language, refreshKey) {
        if (authToken.isBlank()) return@LaunchedEffect
        refreshError = false
        coroutineScope {
            val progressTask = async { runCatching { repository.fetchProgress(authToken) }.getOrNull() }
            val statsTask = async { runCatching { repository.fetchWeeklyStats(authToken, language) }.getOrNull() }
            val progressJson = progressTask.await()
            val statsJson = statsTask.await()
            if (progressJson == null && statsJson == null) refreshError = true
            if (progressJson != null) lessonProgress = parseNativeProgress(progressJson)
            weeklyStats = statsJson?.let(::parseNativeWeeklyStats)
        }
    }

    val learning = remember(catalog, lessonProgress) { calculateNativeHomeLearning(catalog, lessonProgress) }
    val completedCount = weeklyStats?.completedLessons?.takeIf { it > 0 }
        ?: learning.allDone
    val allLessonCount = catalog.lessons.size.takeIf { it > 0 } ?: 1296
    val stats = weeklyStats ?: NativeWeeklyStats(completedLessons = learning.allDone)
    val days = stats.days.ifEmpty { fallbackDays() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NihongoPalette.background)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 18.dp)
        ) {
            item {
                // The web design uses a ~506px hero and a card overlapping it by 42px.
                // Measure the real card height, instead of a 680dp fixed viewport + offset.
                // This keeps the next section directly below the visible card on every device.
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val heroHeight = (maxWidth * 1.28f).coerceIn(480.dp, 540.dp)
                    val overlap = 42.dp
                    Layout(
                        modifier = Modifier.fillMaxWidth(),
                        content = {
                            HomeHeroNative(
                                user = user,
                                firstName = firstName,
                                isId = isId,
                                heroDrawable = heroDrawable,
                                onSettings = { showSettings = true }
                            )
                            ContinueLearningNativeCard(
                                isId = isId,
                                learning = learning,
                                modifier = Modifier.padding(horizontal = 18.dp),
                                onClick = {
                                    onNotice(
                                        if (learning.lesson != null) {
                                            if (isId) "Materi ${learning.lesson.titleId} siap dibuka. Halaman belajar native akan disambungkan berikutnya." else "${learning.lesson.titleEn} is ready. The native lesson screen will be connected next."
                                        } else if (isId) "Materi sedang dimuat. Coba lagi sebentar." else "Lessons are loading. Please try again shortly."
                                    )
                                }
                            )
                        }
                    ) { measurables, constraints ->
                        val width = constraints.maxWidth
                        val heroPx = heroHeight.roundToPx()
                        val overlapPx = overlap.roundToPx()
                        val hero = measurables[0].measure(Constraints.fixed(width, heroPx))
                        val card = measurables[1].measure(
                            constraints.copy(
                                minWidth = width,
                                maxWidth = width,
                                minHeight = 0,
                                maxHeight = Constraints.Infinity
                            )
                        )
                        layout(width, hero.height + card.height - overlapPx) {
                            hero.placeRelative(0, 0)
                            card.placeRelative(0, hero.height - overlapPx)
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
            }
            item {
                RewardCenterNative(isId = isId, onAction = { action ->
                    when (action) {
                        "missions" -> onNotice(if (isId) "Misi: klaim XP dan kredit. Halaman Misi native akan disambungkan berikutnya." else "Missions: claim XP and credits. The native Missions page is next.")
                        "topup" -> onNotice(if (isId) "Pembelian kredit aman dilakukan melalui halaman top up resmi NihonGo." else "Credit purchases are handled through NihonGo's official top-up page.")
                        else -> onNotice(if (isId) "Riwayat XP dan kredit akan ditampilkan di halaman riwayat native." else "XP and credit logs will be shown on the native History page.")
                    }
                })
                Spacer(Modifier.height(25.dp))
            }
            item {
                WeeklyStatisticsNative(
                    isId = isId,
                    stats = stats,
                    days = days,
                    completedCount = completedCount,
                    totalLessons = allLessonCount,
                    onDailyXp = { onNotice(if (isId) "Grafik XP harian menggunakan data tujuh hari dari server." else "The daily XP chart uses the server's seven-day activity data.") },
                    onStudyStart = { onNotice(if (isId) "Pilih Lanjutkan Belajar untuk membuka materi berikutnya." else "Choose Continue Learning to open your next lesson.") },
                    onRetry = { refreshKey++ },
                    refreshError = refreshError
                )
            }
        }

        NativeBottomNav(
            isId = isId,
            onSelect = { key ->
                when (key) {
                    "home" -> Unit
                    "learn" -> onNotice(if (isId) "Menu Belajar native sedang disiapkan." else "The native Learn screen is being prepared.")
                    "lab" -> onNotice(if (isId) "Menu Lab native sedang disiapkan." else "The native Lab screen is being prepared.")
                    "game" -> onNotice(if (isId) "Menu Game native sedang disiapkan." else "The native Game screen is being prepared.")
                    else -> onNotice(if (isId) "Menu Sensei native sedang disiapkan." else "The native Sensei screen is being prepared.")
                }
            }
        )
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            containerColor = NihongoPalette.surface2,
            title = { Text(if (isId) "Pengaturan akun" else "Account settings", color = NihongoPalette.white, fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text(user.name.ifBlank { "NihonGo user" }, color = NihongoPalette.white, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(user.email, color = NihongoPalette.dim2, fontSize = 12.sp)
                    Spacer(Modifier.height(18.dp))
                    Text(if (isId) "Bahasa tampilan" else "Display language", color = NihongoPalette.dim2, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LanguageChoice("Indonesia", isId, onClick = { if (!isId) onToggleLanguage() }, modifier = Modifier.weight(1f))
                        LanguageChoice("English", !isId, onClick = { if (isId) onToggleLanguage() }, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) { Text(if (isId) "Selesai" else "Done", color = NihongoPalette.redSoft) }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false; showLogoutConfirmation = true }) {
                    Text(if (isId) "Keluar" else "Sign out", color = NihongoPalette.dim2)
                }
            }
        )
    }

    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            containerColor = NihongoPalette.surface2,
            title = { Text(if (isId) "Keluar dari akun?" else "Sign out?", color = NihongoPalette.white, fontWeight = FontWeight.Black) },
            text = { Text(if (isId) "Kamu bisa masuk kembali kapan saja dengan akun Google yang sama." else "You can sign in again at any time with the same Google account.", color = NihongoPalette.dim2) },
            confirmButton = {
                TextButton(onClick = { showLogoutConfirmation = false; onLogout() }, colors = ButtonDefaults.textButtonColors(contentColor = NihongoPalette.redSoft)) {
                    Text(if (isId) "Keluar" else "Sign out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmation = false }) { Text(if (isId) "Batal" else "Cancel", color = NihongoPalette.dim2) }
            }
        )
    }
}

@Composable
private fun HomeHeroNative(
    user: AppUser,
    firstName: String,
    isId: Boolean,
    heroDrawable: Int,
    onSettings: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxWidth < 360.dp
        val avatarWidth = (maxWidth * 0.61f).coerceIn(188.dp, 264.dp)
        val avatarHeight = (maxWidth * 0.98f).coerceIn(320.dp, 383.dp)
        val introWidth = (maxWidth - 36.dp) * (if (compact) 0.51f else 0.55f)

        Image(
            painter = painterResource(heroDrawable),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color(0x2205050B),
                        0.30f to Color(0x2905050B),
                        0.56f to Color(0x3605050B),
                        0.80f to Color(0xA507070D),
                        1f to NihongoPalette.background
                    )
                )
            )
        )
        // A subtle left scrim preserves white-text contrast against the sky.
        Box(
            Modifier.fillMaxWidth(0.68f).height(320.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0x6807070D), Color(0x3507070D), Color.Transparent)
                    )
                )
        )

        // Keep the model BELOW the title and on the right, so head and
        // shoulders cannot obscure the Japanese heading or greeting.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 13.dp, y = if (compact) 137.dp else 124.dp)
                .width(avatarWidth)
                .height(avatarHeight)
                .zIndex(1f)
        ) {
            NativeSenseiModel(Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(top = 22.dp)
                .zIndex(2f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "日本語", color = Color.White, fontSize = 42.sp,
                        lineHeight = 45.sp, fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        style = TextStyle(shadow = Shadow(Color(0xAA000000), Offset(0f, 3f), 10f))
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isId) "Nihongo · Belajar Bahasa Jepang" else "Nihongo · Learn Japanese",
                        color = Color(0xE7FFFFFF), fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(shadow = Shadow(Color(0xCC000000), Offset(0f, 2f), 7f))
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color(0xC212121C))
                        .border(1.dp, Color(0x2FFFFFFF), RoundedCornerShape(17.dp))
                        .clickable(onClick = onSettings),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = if (isId) "Pengaturan" else "Settings",
                        tint = Color(0xD7FFFFFF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(26.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.Top) {
                Text("✿", color = NihongoPalette.redLight, fontSize = 18.sp, lineHeight = 25.sp)
                Column(modifier = Modifier.width(introWidth)) {
                    Text(
                        if (isId) "Selamat datang kembali, $firstName. Ayo lanjut belajar hari ini." else "Welcome back, $firstName. Let's continue learning today.",
                        color = Color.White,
                        fontSize = if (compact) 15.sp else 16.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Black,
                        style = TextStyle(shadow = Shadow(Color(0xD6000000), Offset(0f, 3f), 9f))
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (isId) "Pilih materi, latihan, atau tanya Sensei kapan saja." else "Choose lessons, practice, or ask Sensei anytime.",
                        color = Color(0xEDFFFFFF), fontSize = 12.sp,
                        lineHeight = 17.sp, fontWeight = FontWeight.SemiBold,
                        style = TextStyle(shadow = Shadow(Color(0xCC000000), Offset(0f, 2f), 7f))
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueLearningNativeCard(
    isId: Boolean,
    learning: NativeHomeLearning,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val chapter = learning.chapter
    val lesson = learning.lesson
    val jlpt = (chapter?.jlpt ?: lesson?.jlpt ?: "n5").uppercase()
    val chapterTitle = chapter?.let { if (isId) it.titleId else it.titleEn }
        ?: if (isId) "Memuat materi belajar" else "Loading lessons"
    val percent = learning.chapterPct.coerceIn(0, 100)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(25.dp),
        color = Color(0xF51A1118),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x5AE03535)),
        shadowElevation = 16.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(Color(0x3DE03535), Color(0x160B0B13))))
                        .border(1.dp, Color(0x40E03535), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(jlpt, color = NihongoPalette.redSoft, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(if (isId) "Dasar" else "Basic", color = NihongoPalette.dim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (isId) "Lanjutkan Belajar" else "Continue Learning", color = NihongoPalette.redSoft, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text(chapterTitle, color = NihongoPalette.white, fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        lesson?.let { if (isId) it.titleId else it.titleEn } ?: if (isId) "Pilih materi untuk mulai belajar" else "Choose a lesson to start",
                        color = NihongoPalette.dim, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(Color(0x24FFFFFF))) {
                            Box(Modifier.fillMaxWidth(percent / 100f).height(6.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(NihongoPalette.red, NihongoPalette.redLight))))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("$percent%", color = NihongoPalette.dim, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
            Spacer(Modifier.height(13.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isId) "${learning.chapterDone} dari ${learning.chapterTotal} materi selesai" else "${learning.chapterDone} of ${learning.chapterTotal} lessons complete",
                    color = NihongoPalette.dim, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(NihongoPalette.red, NihongoPalette.redLight)))
                        .clickable(onClick = onClick)
                        .padding(horizontal = 17.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if (isId) "Lanjutkan" else "Continue", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun RewardCenterNative(isId: Boolean, onAction: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 18.dp)) {
            Text("Reward Center", color = NihongoPalette.white, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(
                if (isId) "Misi, top up, dan log XP/kredit" else "Missions, top up, and XP/credit logs",
                color = NihongoPalette.dim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(14.dp))
        // 3 equal-weight native shortcuts: no offscreen third card, no 126dp clipping.
        // Titles/subtitles can wrap, and each card grows with the device font scale.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            RewardTile(
                Modifier.weight(1f), Icons.Outlined.CardGiftcard,
                if (isId) "Misi" else "Missions",
                if (isId) "Klaim XP & kredit" else "Claim XP & credits",
                Color(0xFF2DB36A), onClick = { onAction("missions") }
            )
            RewardTile(
                Modifier.weight(1f), Icons.Outlined.CreditCard,
                if (isId) "Beli Kredit" else "Buy Credits",
                if (isId) "Paket 1.000+" else "1,000+ packs",
                Color(0xFFE2B957), onClick = { onAction("topup") }
            )
            RewardTile(
                Modifier.weight(1f), Icons.Outlined.History,
                if (isId) "Riwayat" else "History",
                if (isId) "Log XP & kredit" else "XP & credit logs",
                Color(0xFF9C6AFF), onClick = { onAction("history") }
            )
        }
    }
}

@Composable
private fun RewardTile(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    detail: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.heightIn(min = 156.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = NihongoPalette.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NihongoPalette.borderStrong)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(36.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
                }
                Text("›", color = NihongoPalette.dim, fontSize = 23.sp, lineHeight = 25.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                title, color = NihongoPalette.white, fontSize = 12.5.sp,
                lineHeight = 16.sp, fontWeight = FontWeight.Black,
                minLines = 2, maxLines = 2
            )
            Spacer(Modifier.height(5.dp))
            Text(
                detail, color = NihongoPalette.dim, fontSize = 10.sp,
                lineHeight = 13.sp, fontWeight = FontWeight.SemiBold,
                minLines = 2, maxLines = 3
            )
        }
    }
}

@Composable
private fun WeeklyStatisticsNative(
    isId: Boolean,
    stats: NativeWeeklyStats,
    days: List<NativeDayStat>,
    completedCount: Int,
    totalLessons: Int,
    onDailyXp: () -> Unit,
    onStudyStart: () -> Unit,
    onRetry: () -> Unit,
    refreshError: Boolean
) {
    val allXpZero = days.all { it.xp <= 0 }
    val allMinutesZero = days.all { it.minutes <= 0 }
    Column(Modifier.padding(horizontal = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(if (isId) "Statistik Mingguan" else "Weekly Statistics", color = NihongoPalette.white, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text(if (isId) "Ringkasan progres dari aktivitas belajar" else "Progress summary from learning activity", color = NihongoPalette.dim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                modifier = Modifier.clickable(onClick = onDailyXp),
                shape = CircleShape,
                color = NihongoPalette.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, NihongoPalette.borderStrong)
            ) {
                Row(Modifier.padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.TrendingUp, contentDescription = null, tint = NihongoPalette.redLight, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(if (isId) "XP Harian" else "Daily XP", color = NihongoPalette.white, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        EconomyLevelCard(isId = isId, stats = stats)
        Spacer(Modifier.height(12.dp))
        WeeklyXpChart(isId = isId, days = days, isEmpty = allXpZero)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WeeklyMetricCard(
                Modifier.weight(1f), Icons.Outlined.Star,
                if (isId) "XP Minggu Ini" else "XP This Week", "${stats.xpWeek} XP",
                when {
                    stats.xpTrendPct > 0 -> "+${stats.xpTrendPct}% ${if (isId) "dari pekan lalu" else "vs last week"}"
                    stats.xpTrendPct < 0 -> "${stats.xpTrendPct}% ${if (isId) "dari pekan lalu" else "vs last week"}"
                    else -> if (isId) "Belum ada pembanding" else "No comparison yet"
                }, Color(0xFFE1BD59)
            )
            WeeklyMetricCard(Modifier.weight(1f), Icons.Outlined.GpsFixed, if (isId) "Akurasi" else "Accuracy", "${stats.accuracy}%", if (isId) "Dari latihan tersimpan" else "From saved practice", Color(0xFF2DB36A))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val donePct = if (totalLessons > 0) (completedCount * 100f / totalLessons).roundToInt() else 0
            WeeklyMetricCard(Modifier.weight(1f), Icons.Outlined.MenuBook, if (isId) "Materi Selesai" else "Lessons Completed", "$completedCount / $totalLessons", "$donePct% ${if (isId) "selesai" else "complete"}", Color(0xFF4689FF))
            WeeklyMetricCard(Modifier.weight(1f), Icons.Outlined.Cancel, if (isId) "Review Salah" else "Wrong Reviews", "${stats.wrongActive} ${if (isId) "soal" else "items"}", if (stats.wrongActive == 0) { if (isId) "Aman" else "All clear" } else { if (isId) "Perlu diulas" else "Needs review" }, Color(0xFFE33A47))
        }
        Spacer(Modifier.height(12.dp))
        StreakStudyCard(isId = isId, stats = stats, days = days, isMinutesEmpty = allMinutesZero, onStudyStart = onStudyStart)
        if (refreshError) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(NihongoPalette.surface).padding(horizontal = 13.dp, vertical = 10.dp)) {
                Text(if (isId) "Sebagian statistik belum bisa dimuat." else "Some statistics could not be loaded.", color = NihongoPalette.dim, fontSize = 11.sp)
                Text(if (isId) "Coba lagi" else "Retry", color = NihongoPalette.redSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onRetry).padding(4.dp))
            }
        }
    }
}

@Composable
private fun EconomyLevelCard(isId: Boolean, stats: NativeWeeklyStats) {
    val progress = (stats.levelProgressPct.coerceIn(0, 100) / 100f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = NihongoPalette.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NihongoPalette.borderStrong)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Color(0x24DEB55A)).border(1.dp, Color(0x4CDEB55A), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = Color(0xFFE2BD58), modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Level ${stats.level}", color = NihongoPalette.white, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isId) "${stats.xpIntoLevel} / ${stats.xpForNext} XP ke level berikutnya" else "${stats.xpIntoLevel} / ${stats.xpForNext} XP to next level",
                        color = NihongoPalette.dim, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(7.dp))
                Column(
                    Modifier.width(92.dp).clip(RoundedCornerShape(20.dp)).background(Color(0x1FDEB55A)).border(1.dp, Color(0x3BDEB55A), RoundedCornerShape(20.dp)).padding(horizontal = 9.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = Color(0xFFE2BD58), modifier = Modifier.size(21.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(formatCompactCredits(stats.credits), color = Color(0xFFFFD653), fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Text(if (isId) "Kredit" else "Credits", color = NihongoPalette.dim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isId) "Progress Level ${stats.level}" else "Level ${stats.level} progress", color = NihongoPalette.dim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("${stats.levelProgressPct}%", color = Color(0xFFE2BD58), fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color(0x18FFFFFF))) {
                Box(Modifier.fillMaxWidth(progress).height(8.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(Color(0xFFE0BB57), Color(0xFFE33B42)))))
            }
        }
    }
}

@Composable
private fun WeeklyXpChart(isId: Boolean, days: List<NativeDayStat>, isEmpty: Boolean) {
    val ticks = listOf("100", "75", "50", "25", "0")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = NihongoPalette.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NihongoPalette.borderStrong)
    ) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 15.dp)) {
            Box(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().height(178.dp)) {
                    Column(Modifier.width(35.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        ticks.forEach { Text(it, color = NihongoPalette.dim, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                    }
                    Canvas(Modifier.weight(1f).fillMaxSize().padding(start = 5.dp, end = 1.dp)) {
                        val w = size.width
                        val h = size.height
                        val grid = Color(0x16FFFFFF)
                        for (i in 0..4) {
                            val y = h * i / 4f
                            drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
                        }
                        val maxValue = maxOf(100f, days.maxOfOrNull { it.xp.toFloat() } ?: 0f)
                        val slot = w / days.size.coerceAtLeast(1)
                        val barWidth = minOf(40.dp.toPx(), slot * 0.52f)
                        days.forEachIndexed { index, day ->
                            val raw = (day.xp / maxValue).coerceIn(0f, 1f)
                            val barHeight = (h * raw).coerceAtLeast(if (day.xp > 0) 7.dp.toPx() else 3.dp.toPx())
                            val x = slot * index + (slot - barWidth) / 2f
                            drawRoundRect(
                                color = Color(0xFFE0353B).copy(alpha = if (day.xp > 0) 0.92f else 0.12f),
                                topLeft = Offset(x, h - barHeight),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                            )
                        }
                    }
                }
                if (isEmpty) {
                    Text(
                        if (isId) "Belum ada aktivitas minggu ini" else "No activity this week yet",
                        color = NihongoPalette.dim,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center).padding(start = 28.dp, end = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().padding(start = 35.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                days.forEach { day ->
                    Text(day.label.ifBlank { "—" }, color = NihongoPalette.dim, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun WeeklyMetricCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    detail: String,
    accent: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(25.dp),
        color = NihongoPalette.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(55.dp).clip(RoundedCornerShape(20.dp)).background(accent.copy(alpha = 0.11f)).border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(9.dp))
                Text(label, color = NihongoPalette.dim, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(11.dp))
            Text(value, color = accent, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(5.dp))
            Text(detail, color = NihongoPalette.dim, fontSize = 10.5.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StreakStudyCard(
    isId: Boolean,
    stats: NativeWeeklyStats,
    days: List<NativeDayStat>,
    isMinutesEmpty: Boolean,
    onStudyStart: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = NihongoPalette.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NihongoPalette.borderStrong)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(78.dp).clip(RoundedCornerShape(23.dp)).background(Color(0x1EE0353B)).border(1.dp, Color(0x3EE0353B), RoundedCornerShape(23.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = Color(0xFFE33A42), modifier = Modifier.size(37.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${stats.streak}", color = NihongoPalette.white, fontSize = 35.sp, lineHeight = 37.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(9.dp))
                        Text(if (isId) "hari streak" else "day streak", color = NihongoPalette.dim, fontSize = 15.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 5.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(if (isId) "Pertahankan ritmenya!" else "Keep your rhythm!", color = NihongoPalette.white, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(5.dp))
                    Text(if (isId) "Aktif sedikit tiap hari lebih aman daripada ngebut semalam." else "A little every day beats cramming overnight.", color = NihongoPalette.dim, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(25.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                days.forEach { day ->
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(33.dp).clip(CircleShape)
                                .background(if (day.active) Color(0x2DE0353B) else Color(0x18A7A0C8))
                                .border(1.dp, if (day.active) Color(0x85E0353B) else Color(0x22A7A0C8), CircleShape)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(day.label, color = NihongoPalette.dim, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(25.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(66.dp).clip(RoundedCornerShape(22.dp)).background(Color(0x249B61FF)).border(1.dp, Color(0x3C9B61FF), RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = Color(0xFFA96FFF), modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (isId) "Estimasi belajar" else "Study estimate", color = NihongoPalette.dim, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("${stats.studyMinutes} menit", color = Color(0xFFA96FFF), fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black)
                }
                Box(
                    Modifier.clip(CircleShape).background(Color(0x279B61FF)).border(1.dp, Color(0x409B61FF), CircleShape)
                        .clickable(onClick = onStudyStart).padding(horizontal = 13.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFA96FFF)))
                        Spacer(Modifier.width(7.dp))
                        Text(if (isId) "Mulai" else "Start", color = Color(0xFFA96FFF), fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Spacer(Modifier.height(15.dp))
            Text(
                if (isId) "Rata-rata ${stats.averageMinutes} mnt/hari" else "Average ${stats.averageMinutes} min/day",
                color = Color(0xFF2DB36A), fontSize = 14.sp, fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(35.dp))
            Box(Modifier.fillMaxWidth().height(94.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val top = 12.dp.toPx()
                    val bottom = h - 9.dp.toPx()
                    for (i in 0..3) {
                        val y = top + (bottom - top) * i / 3f
                        drawLine(Color(0x13FFFFFF), Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
                    }
                    val maxValue = maxOf(1f, days.maxOfOrNull { it.minutes.toFloat() } ?: 0f)
                    val slot = w / days.size.coerceAtLeast(1)
                    val path = Path()
                    val points = days.mapIndexed { index, day ->
                        val valueRatio = if (isMinutesEmpty) 0.05f else (day.minutes / maxValue).coerceIn(0f, 1f)
                        Offset(slot * index + slot / 2f, bottom - (bottom - top) * valueRatio)
                    }
                    points.forEachIndexed { index, point -> if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y) }
                    drawPath(path, Color(0xFFA96FFF), style = Stroke(width = 2.5.dp.toPx()))
                    points.forEach { point ->
                        drawCircle(Color(0xFFA96FFF), radius = 4.5.dp.toPx(), center = point)
                        drawCircle(NihongoPalette.surface, radius = 2.dp.toPx(), center = point)
                    }
                }
                if (isMinutesEmpty) {
                    Text(if (isId) "Belajar sebentar untuk mulai grafik" else "Study a little to start your graph", color = NihongoPalette.dim, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                days.forEach { day -> Text(day.label, color = NihongoPalette.dim, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
            }
        }
    }
}

@Composable
private fun NativeBottomNav(isId: Boolean, onSelect: (String) -> Unit) {
    data class Tab(val key: String, val label: String, val icon: ImageVector)
    val tabs = listOf(
        Tab("home", if (isId) "Rumah" else "Home", Icons.Filled.Home),
        Tab("learn", if (isId) "Belajar" else "Learn", Icons.Outlined.School),
        Tab("lab", "Lab", Icons.Outlined.Science),
        Tab("game", if (isId) "Game" else "Games", Icons.Outlined.SportsEsports),
        Tab("sensei", "Sensei", Icons.Outlined.ChatBubbleOutline)
    )
    Row(
        modifier = Modifier.fillMaxWidth().background(NihongoPalette.surface)
            .border(1.dp, Color(0x12FFFFFF)).navigationBarsPadding()
            .padding(horizontal = 4.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val active = tab.key == "home"
            Column(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { onSelect(tab.key) }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(tab.icon, contentDescription = tab.label, tint = if (active) NihongoPalette.redLight else NihongoPalette.dim, modifier = Modifier.size(22.dp))
                Text(tab.label, color = if (active) NihongoPalette.redLight else NihongoPalette.dim, fontSize = 9.sp, fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun LanguageChoice(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0x28E03535) else NihongoPalette.surface3)
            .border(1.dp, if (selected) Color(0x68E03535) else NihongoPalette.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) NihongoPalette.redSoft else NihongoPalette.dim2, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}


private fun readNativeLessonCatalog(context: Context): NativeLessonCatalog {
    return runCatching {
        val text = context.assets.open("lesson-meta.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(text)
        val chaptersArray = root.optJSONArray("chapters") ?: JSONArray()
        val lessonsArray = root.optJSONArray("lessons") ?: JSONArray()
        val chapters = buildList {
            for (index in 0 until chaptersArray.length()) {
                val item = chaptersArray.optJSONObject(index) ?: continue
                val title = item.optJSONObject("title") ?: JSONObject()
                val ids = item.optJSONArray("lessonIds") ?: JSONArray()
                add(NativeChapterMeta(
                    id = item.optString("id"), jlpt = item.optString("jlpt", "n5"), no = item.optInt("no"),
                    titleId = title.optString("id"), titleEn = title.optString("en"),
                    lessonIds = buildList { for (i in 0 until ids.length()) add(ids.optString(i)) }
                ))
            }
        }
        val lessons = buildList {
            for (index in 0 until lessonsArray.length()) {
                val item = lessonsArray.optJSONObject(index) ?: continue
                val title = item.optJSONObject("title") ?: JSONObject()
                add(NativeLessonMeta(item.optString("id"), item.optInt("no"), item.optString("jlpt", "n5"), title.optString("id"), title.optString("en")))
            }
        }.sortedBy { it.no }
        NativeLessonCatalog(lessons, chapters)
    }.getOrDefault(NativeLessonCatalog())
}

private fun parseNativeProgress(root: JSONObject): Map<String, NativeLessonProgress> {
    val items = root.optJSONObject("progress") ?: root
    return buildMap {
        val keys = items.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val row = items.optJSONObject(key) ?: continue
            put(key, NativeLessonProgress(
                completed = row.optBoolean("completed", false),
                stars = row.optInt("stars", 0),
                score = row.optInt("score", 0),
                total = row.optInt("total", 0)
            ))
        }
    }
}

private fun parseNativeWeeklyStats(root: JSONObject): NativeWeeklyStats {
    val summary = root.optJSONObject("summary") ?: root
    val economy = root.optJSONObject("economy") ?: JSONObject()
    val balance = root.optJSONObject("balance") ?: JSONObject()
    val balanceLevel = balance.optJSONObject("level") ?: JSONObject()
    val daysArray = root.optJSONArray("days") ?: JSONArray()
    val days = buildList {
        for (index in 0 until daysArray.length()) {
            val item = daysArray.optJSONObject(index) ?: continue
            add(NativeDayStat(
                key = item.optString("key", item.optString("date")),
                label = item.optString("label"),
                xp = item.optInt("xp", item.optInt("value", 0)),
                minutes = item.optInt("minutes", item.optInt("studyMinutes", 0)),
                active = item.optBoolean("active", false)
            ))
        }
    }
    return NativeWeeklyStats(
        xpWeek = summary.optInt("xpWeek", root.optInt("xpWeek", root.optInt("weeklyXp", 0))),
        accuracy = summary.optInt("accuracy", root.optInt("accuracy", 0)),
        completedLessons = summary.optInt("completedLessons", root.optInt("completedLessons", root.optInt("completed", 0))),
        wrongActive = summary.optInt("wrongActive", root.optInt("wrongActive", root.optInt("wrongCount", 0))),
        studyMinutes = summary.optInt("studyMinutes", root.optInt("studyMinutes", root.optInt("totalMinutes", 0))),
        averageMinutes = summary.optInt("averageMinutes", root.optInt("averageMinutes", root.optInt("avgMinutes", 0))),
        streak = summary.optInt("streak", root.optInt("streak", 0)),
        xpTrendPct = summary.optInt("xpTrendPct", root.optInt("xpTrendPct", 0)),
        totalXp = economy.optInt("totalXp", balance.optInt("xpTotal", 0)),
        credits = economy.optLong("creditBalance", balance.optLong("creditBalance", 0L)),
        level = economy.optInt("level", balanceLevel.optInt("level", 1)).coerceAtLeast(1),
        xpIntoLevel = economy.optInt("xpIntoLevel", balanceLevel.optInt("current", 0)),
        xpForNext = economy.optInt("xpForNext", balanceLevel.optInt("needed", 1)).coerceAtLeast(1),
        levelProgressPct = economy.optInt("levelProgressPct", balanceLevel.optInt("progress", 0)).coerceIn(0, 100),
        days = days
    )
}

private fun calculateNativeHomeLearning(
    catalog: NativeLessonCatalog,
    progress: Map<String, NativeLessonProgress>
): NativeHomeLearning {
    if (catalog.lessons.isEmpty()) return NativeHomeLearning()
    val byNo = catalog.lessons.associateBy { it.no }
    fun passed(lesson: NativeLessonMeta?): Boolean = lesson != null && progress[lesson.id]?.passed == true
    fun unlocked(lesson: NativeLessonMeta): Boolean = lesson.no == 1 || passed(byNo[lesson.no - 1])
    val current = catalog.lessons.firstOrNull { !passed(it) && unlocked(it) } ?: catalog.lessons.last()
    val chapter = catalog.chapters.firstOrNull { current.id in it.lessonIds } ?: catalog.chapters.firstOrNull()
    val chapterLessons = chapter?.lessonIds?.mapNotNull { id -> catalog.lessons.firstOrNull { it.id == id } }.orEmpty()
    val done = chapterLessons.count(::passed)
    val total = chapterLessons.size
    val percent = if (total > 0) (done * 100f / total).roundToInt() else 0
    return NativeHomeLearning(
        lesson = current,
        chapter = chapter,
        chapterDone = done,
        chapterTotal = total,
        chapterPct = percent,
        allDone = progress.values.count { it.completed }
    )
}

private fun fallbackDays(): List<NativeDayStat> = listOf("Kam", "Jum", "Sab", "Min", "Sen", "Sel", "Rab").mapIndexed { index, label ->
    NativeDayStat("$index", label, 0, 0, false)
}

private fun formatCompactCredits(value: Long): String = when {
    value >= 1_000_000L -> "${((value / 100_000.0).roundToInt() / 10.0).toString().removeSuffix(".0")}jt"
    value >= 1_000L -> "${((value / 100.0).roundToInt() / 10.0).toString().removeSuffix(".0")}rb"
    else -> value.toString()
}
