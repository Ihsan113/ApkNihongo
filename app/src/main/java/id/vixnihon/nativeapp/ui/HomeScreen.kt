package id.vixnihon.nativeapp.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
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
import androidx.compose.ui.platform.LocalDensity
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.pow
import kotlin.math.floor
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.random.Random
import kotlin.math.roundToInt

internal data class NativeLessonMeta(
    val id: String,
    val no: Int,
    val jlpt: String,
    val titleId: String,
    val titleEn: String
)

internal data class NativeChapterMeta(
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

internal data class NativeLessonProgress(val completed: Boolean, val stars: Int, val score: Int, val total: Int) {
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
    
    var catalog by remember { mutableStateOf(NativeLessonCatalog()) }
    var lessonProgress by remember { mutableStateOf<Map<String, NativeLessonProgress>>(emptyMap()) }
    var weeklyStats by remember { mutableStateOf<NativeWeeklyStats?>(null) }
    var refreshError by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    var catalogLoaded by remember(authToken) { mutableStateOf(false) }
    var firstHomeFetchCompleted by remember(authToken) { mutableStateOf(false) }
    val initialHomeReady = catalogLoaded && firstHomeFetchCompleted
    var destination by remember { mutableStateOf<String?>(null) }
    val destinationStack = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    fun openNested(destinationId: String) {
        destination?.let { previous -> if (previous != destinationId) destinationStack.add(previous) }
        destination = destinationId
    }
    fun backNested() {
        destination = if (destinationStack.isNotEmpty()) destinationStack.removeAt(destinationStack.lastIndex) else null
        if (destination == null) refreshKey++
    }
    // The Home is only a handful of sections. Keep the 3D renderer in the composition
    // while scrolling through Rewards / Stats so returning to the hero does not
    // re-create a 6.5 MB GLB and a Filament context during a fling.
    val homeScrollState = rememberScrollState()
    val visibleHeroLimit = with(LocalDensity.current) { 540.dp.roundToPx() }
    val renderSensei by remember(homeScrollState, visibleHeroLimit) {
        derivedStateOf {
            homeScrollState.value < visibleHeroLimit && !homeScrollState.isScrollInProgress
        }
    }
    var settings by remember { mutableStateOf(NativeSettingsStorage.load(context)) }
    val systemDark = isSystemInDarkTheme()
    val lightTheme = when (settings.theme) {
        "light" -> true
        "dark" -> false
        else -> !systemDark
    }
    SideEffect { NihongoPalette.lightMode = lightTheme }
    BackHandler(enabled = showSettings) { showSettings = false }

    var senseiSpeaking by remember { mutableStateOf(false) }
    var senseiViseme by remember { mutableIntStateOf(0) }
    var voiceUnavailable by remember { mutableStateOf("") }
    var appIsForeground by remember { mutableStateOf(true) }
    val lifecycle = remember(context) { findNativeLifecycleOwner(context) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> { if (!appIsForeground) refreshKey++; appIsForeground = true }
                Lifecycle.Event.ON_PAUSE -> appIsForeground = false
                else -> Unit
            }
        }
        lifecycle?.lifecycle?.addObserver(observer)
        onDispose { lifecycle?.lifecycle?.removeObserver(observer) }
    }
    val firstName = remember(user.name, user.email) {
        (user.name.ifBlank { user.email.substringBefore('@') })
            .trim().split(Regex("\\s+")).firstOrNull().orEmpty().ifBlank { "Sensei" }
    }
    NativeHomeSpeech(
        enabled = settings.speechEnabled && initialHomeReady && destination == null && !showSettings && appIsForeground,
        rate = settings.speechRate, pitch = settings.speechPitch, volume = settings.speechVolume,
        onSpeaking = { senseiSpeaking = it },
        onViseme = { senseiViseme = it },
        onUnavailable = { error -> if (voiceUnavailable != error) { voiceUnavailable = error; onNotice(error) } }
    )
    var clockTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (isActive) { delay(60_000); clockTick++ }
    }
    val clock = remember(clockTick) { Calendar.getInstance() }
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
        try {
            catalog = withContext(Dispatchers.IO) { readNativeLessonCatalog(context) }
        } finally {
            catalogLoaded = true
        }
    }
    LaunchedEffect(authToken, language, refreshKey) {
        if (authToken.isBlank()) return@LaunchedEffect
        refreshError = false
        try {
            coroutineScope {
                val progressTask = async { runCatching { repository.fetchProgress(authToken) }.getOrNull() }
                val statsTask = async { runCatching { repository.fetchWeeklyStats(authToken, language) }.getOrNull() }
                val progressJson = progressTask.await()
                val statsJson = statsTask.await()
                if (progressJson == null || statsJson == null) refreshError = true
                if (progressJson != null) lessonProgress = parseNativeProgress(progressJson)
                if (statsJson != null) weeklyStats = parseNativeWeeklyStats(statsJson)
            }
        } finally {
            // Show the real Home even on network error so the retry action remains reachable.
            firstHomeFetchCompleted = true
        }
    }

    val learning = remember(catalog, lessonProgress) { calculateNativeHomeLearning(catalog, lessonProgress) }
    val completedCount = weeklyStats?.completedLessons ?: learning.allDone
    val allLessonCount = catalog.lessons.size.takeIf { it > 0 } ?: 1296
    val stats = weeklyStats ?: NativeWeeklyStats(completedLessons = learning.allDone)
    val days = remember(stats.days, isId) {
        // Same as web normalizeBackendStats: server entries align to the seven local labels by index.
        // Keep server values even if its date timezone differs from the device by a day.
        fallbackDays(isId).mapIndexed { index, base ->
            stats.days.getOrNull(index)?.copy(label = base.label) ?: base
        }
    }

    Box(Modifier.fillMaxSize().background(NihongoPalette.background)) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(NihongoPalette.background)
    ) {
        if (showSettings) {
            NativeSettingsScreen(
                user = user,
                language = language,
                preferences = settings,
                onPreferencesChange = { next ->
                    settings = next
                    NativeSettingsStorage.save(context, next)
                },
                onToggleLanguage = onToggleLanguage,
                repository = repository, token = authToken,
                onBack = { showSettings = false },
                onLogout = onLogout, onNotice = onNotice
            )
        } else if (destination != null) {
            HomeLinkedDestination(
                route = destination.orEmpty(), language = language, token = authToken,
                repository = repository, chapter = learning.chapter, lessons = catalog.lessons,
                progress = lessonProgress, currentLessonId = learning.lesson?.id,
                onBack = { backNested() },
                onNavigate = { openNested(it) }, onNotice = onNotice
            )
        } else {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .verticalScroll(homeScrollState)
                .padding(bottom = 18.dp)
        ) {
                // The web design uses a ~506px hero and a card overlapping it by 42px.
                // Measure the real card height, instead of a 680dp fixed viewport + offset.
                // This keeps the next section directly below the visible card on every device.
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    // CSS Home web: min-height 506px. Android dp matches CSS px at normal density.
                    val heroHeight = 506.dp
                    val overlap = 42.dp
                    Layout(
                        modifier = Modifier.fillMaxWidth(),
                        content = {
                            HomeHeroNative(
                                user = user,
                                firstName = firstName,
                                isId = isId,
                                heroDrawable = heroDrawable,
                                onSettings = { showSettings = true },
                                speaking = senseiSpeaking,
                                viseme = senseiViseme,
                                renderActive = renderSensei
                            )
                            ContinueLearningNativeCard(
                                isId = isId,
                                learning = learning,
                                modifier = Modifier.padding(horizontal = 18.dp),
                                onClick = {
                                    if (learning.lesson != null) destination = "learn"
                                    else onNotice(if (isId) "Materi sedang dimuat." else "Lessons are loading.")
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
            RewardCenterNative(isId = isId, onAction = { action ->
                    destination = action
                })
                Spacer(Modifier.height(25.dp))
                WeeklyStatisticsNative(
                    isId = isId,
                    stats = stats,
                    days = days,
                    completedCount = completedCount,
                    totalLessons = allLessonCount,
                    onDailyXp = {},
                    onStudyStart = {},
                    onRetry = { refreshKey++ },
                    refreshError = refreshError
                )
        }

        NativeBottomNav(
            isId = isId,
            onSelect = { key ->
                when (key) {
                    "home" -> Unit
                    "learn" -> destination = "learn"
                    "lab" -> onNotice(if (isId) "Menu Lab native sedang disiapkan." else "The native Lab screen is being prepared.")
                    "game" -> onNotice(if (isId) "Menu Game native sedang disiapkan." else "The native Game screen is being prepared.")
                    else -> onNotice(if (isId) "Menu Sensei native sedang disiapkan." else "The native Sensei screen is being prepared.")
                }
            }
        )
        }
    }
    if (!initialHomeReady && !showSettings && destination == null) {
        NativeGlobalLoading(language = language, variant = "page", compact = true)
    }
    } // Full-screen loading overlay; underlying Home remains composed for warm-up.
}

@Composable
private fun HomeHeroNative(
    user: AppUser,
    firstName: String,
    isId: Boolean,
    heroDrawable: Int,
    onSettings: () -> Unit,
    speaking: Boolean,
    viseme: Int,
    renderActive: Boolean
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxWidth < 360.dp
        val avatarWidth = maxWidth * 0.76f
        val avatarHeight = 470.dp
        val introWidth = (maxWidth - 36.dp) * 0.58f

        Image(
            painter = painterResource(heroDrawable),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(-0.52f, -0.60f), // web background-position: 24% 20%
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colorStops = if (NihongoPalette.lightMode) arrayOf(
                        0f to Color(0x8A05050B),
                        0.33f to Color(0x3805050B),
                        0.66f to Color(0x1005050B),
                        1f to Color(0x18F6F7FB)
                    ) else arrayOf(
                        0f to Color(0xA305050B),
                        0.30f to Color(0x4D05050B),
                        0.52f to Color(0x2905050B),
                        0.80f to Color(0xB807070D),
                        1f to NihongoPalette.background
                    )
                )
            )
        )
        // Web HomeAvatarCanvas: subtle red halo behind the upper-right avatar.
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x2EE03535), Color.Transparent),
                    center = Offset(size.width * 0.80f, size.height * 0.22f),
                    radius = size.width * 0.50f
                )
            )
        }
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
                .offset(x = 12.dp, y = 30.dp)
                .width(avatarWidth)
                .height(avatarHeight)
                .zIndex(1f)
        ) {
            NativeSenseiModel(
                modifier = Modifier.fillMaxSize(),
                speaking = speaking,
                viseme = viseme,
                renderActive = renderActive
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(horizontal = 18.dp)
                .padding(top = 24.dp)
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
            Row(modifier = Modifier.width(introWidth), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Text("✿", color = NihongoPalette.redLight, fontSize = 17.sp, lineHeight = 23.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isId) "Selamat datang kembali, $firstName. Ayo lanjut belajar hari ini." else "Welcome back, $firstName. Let's continue learning today.",
                        color = Color.White,
                        fontSize = 17.sp,
                        lineHeight = 21.25.sp,
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
        color = if (NihongoPalette.lightMode) Color(0xFFFCFCFE) else Color(0xF51A1118),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (NihongoPalette.lightMode) Color(0x22111827) else Color(0x5AE03535)),
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
                        Text(if (jlpt == "N4") (if (isId) "Lanjutan" else "Plus") else (if (isId) "Dasar" else "Basic"), color = NihongoPalette.dim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                        Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(if (NihongoPalette.lightMode) Color(0x26111827) else Color(0x24FFFFFF))) {
                            Box(Modifier.fillMaxWidth(percent / 100f).height(6.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(NihongoPalette.red, NihongoPalette.redLight))))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("$percent%", color = NihongoPalette.dim, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(NihongoPalette.border))
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
    Column(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        Column(Modifier.padding(horizontal = 18.dp)) {
            Text("Reward Center", color = NihongoPalette.white, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(if (isId) "Misi, top up, dan log XP/kredit" else "Missions, top up, and XP/credit log", color = NihongoPalette.dim, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            // Web card width 126px, but fit all three at standard 360-400dp phones.
            // Narrow screens retain swipe access instead of clipping a destination.
            val available = maxWidth
            val cardWidth = minOf(126.dp, ((available - 20.dp) / 3f).coerceAtLeast(112.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RewardTile(Modifier.width(cardWidth), Icons.Outlined.CardGiftcard,
                    if (isId) "Misi" else "Missions", if (isId) "Klaim XP & kredit" else "Claim XP & credits",
                    NihongoPalette.green, onClick = { onAction("missions") })
                RewardTile(Modifier.width(cardWidth), Icons.Outlined.CreditCard,
                    if (isId) "Beli Kredit" else "Buy Credits", if (isId) "Paket 1.000+" else "Packages 1,000+",
                    Color(0xFFE2B957), onClick = { onAction("topup") })
                RewardTile(Modifier.width(cardWidth), Icons.Outlined.History,
                    if (isId) "Riwayat" else "History", if (isId) "Log XP & kredit" else "XP & credit log",
                    Color(0xFFA66DFF), onClick = { onAction("history") })
            }
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
        modifier = modifier.heightIn(min = 108.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = NihongoPalette.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NihongoPalette.borderStrong)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .border(1.dp, accent.copy(alpha = 0.27f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp)) }
                Text("›", color = NihongoPalette.dim, fontSize = 20.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(title, color = NihongoPalette.white, fontSize = 13.sp, lineHeight = 15.sp, fontWeight = FontWeight.Black, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Text(detail, color = NihongoPalette.dim, fontSize = 10.5.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold, minLines = 2)
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
                Text(if (isId) "Statistik Mingguan" else "Weekly Stats", color = NihongoPalette.white, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text(if (isId) "Ringkasan progres dari aktivitas belajar" else "Progress summary from your study activity", color = NihongoPalette.dim, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                modifier = Modifier,
                shape = CircleShape,
                color = NihongoPalette.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, NihongoPalette.borderStrong)
            ) {
                Row(Modifier.padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.TrendingUp, contentDescription = null, tint = NihongoPalette.redLight, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(if (isId) "XP Harian" else "Daily XP", color = NihongoPalette.white, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

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
        shape = RoundedCornerShape(22.dp),
        color = NihongoPalette.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NihongoPalette.borderStrong)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(Color(0x24DEB55A)).border(1.dp, Color(0x4CDEB55A), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = Color(0xFFE2BD58), modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Level ${stats.level}", color = NihongoPalette.white, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isId) "${stats.xpIntoLevel} / ${stats.xpForNext} XP ke level berikutnya" else "${stats.xpIntoLevel} / ${stats.xpForNext} XP to next level",
                        color = NihongoPalette.dim, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(7.dp))
                Column(
                    Modifier.width(82.dp).clip(RoundedCornerShape(18.dp)).background(Color(0x1FDEB55A)).border(1.dp, Color(0x3BDEB55A), RoundedCornerShape(18.dp)).padding(horizontal = 9.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = Color(0xFFE2BD58), modifier = Modifier.size(21.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(formatCompactCredits(stats.credits), color = Color(0xFFFFD653), fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Text(if (isId) "Kredit" else "Credits", color = NihongoPalette.dim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(14.dp))
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
    val largest = maxOf(100, days.maxOfOrNull { it.xp } ?: 0)
    val magnitude = 10.0.pow(floor(kotlin.math.log10(largest.toDouble())))
    val norm = largest / magnitude
    val factor = when { norm <= 1 -> 1.0; norm <= 2 -> 2.0; norm <= 5 -> 5.0; else -> 10.0 }
    val ceiling = maxOf(100, (factor * magnitude).roundToInt())
    val ticks = listOf(ceiling, (ceiling * .75).roundToInt(), (ceiling * .50).roundToInt(), (ceiling * .25).roundToInt(), 0)
    Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        color = NihongoPalette.surface, border = androidx.compose.foundation.BorderStroke(1.dp, NihongoPalette.borderStrong)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 18.dp)) {
            Box(Modifier.fillMaxWidth().height(156.dp)) {
                Row(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.width(34.dp).fillMaxSize().padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        ticks.forEach { Text(formatCompactCredits(it.toLong()), color = NihongoPalette.dim, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    }
                    Box(Modifier.weight(1f).fillMaxSize()) {
                        Canvas(Modifier.fillMaxSize().padding(bottom = 24.dp)) {
                            val w = size.width
                            val h = size.height
                            val slot = w / days.size.coerceAtLeast(1)
                            val barWidth = minOf(24.dp.toPx(), slot * .72f)
                            for (i in 0..4) {
                                val y = h * i / 4f
                                drawLine(NihongoPalette.border, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
                            }
                            days.forEachIndexed { index, day ->
                                val ratio = (day.xp.toFloat() / ceiling).coerceIn(0f, 1f)
                                val barHeight = if (day.xp == 0) 4.dp.toPx() else maxOf(9.dp.toPx(), (h * ratio * .72f))
                                val x = slot * index + (slot - barWidth) / 2f
                                drawRoundRect(
                                    brush = Brush.verticalGradient(if (day.xp > 0) listOf(NihongoPalette.redLight, NihongoPalette.red) else listOf(NihongoPalette.dim.copy(alpha = .25f), NihongoPalette.dim.copy(alpha = .25f))),
                                    topLeft = Offset(x, h - barHeight), size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(5.dp.toPx())
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth().align(Alignment.BottomCenter), horizontalArrangement = Arrangement.SpaceEvenly) {
                            days.forEach { day ->
                                Text(day.label, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = NihongoPalette.dim, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
                if (isEmpty) {
                    Text(if (isId) "Belum ada aktivitas minggu ini" else "No activity this week yet",
                        color = NihongoPalette.dim, fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center).padding(start = 30.dp))
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
        shape = RoundedCornerShape(20.dp),
        color = NihongoPalette.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.20f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.11f)).border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(9.dp))
                Text(label, color = NihongoPalette.dim, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(9.dp))
            Text(value, color = accent, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    var selectedDay by remember(days) { mutableIntStateOf(-1) }
    val maxMinutes = maxOf(1f, days.maxOfOrNull { it.minutes.toFloat() } ?: 0f)
    Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        color = NihongoPalette.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NihongoPalette.borderStrong)
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(50.dp).clip(RoundedCornerShape(18.dp))
                    .background(Color(0x24E0353B)).border(1.dp, Color(0x4CE0353B), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null,
                        tint = NihongoPalette.redLight, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${stats.streak}", color = NihongoPalette.white, fontSize = 24.sp,
                            lineHeight = 26.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isId) "hari streak" else "day streak", color = NihongoPalette.dim,
                            fontSize = 12.5.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(if (isId) "Pertahankan ritmenya!" else "Keep the rhythm!",
                        color = NihongoPalette.white, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text(if (isId) "Aktif sedikit tiap hari lebih aman daripada ngebut semalam." else "A little daily practice beats a single long sprint.",
                        color = NihongoPalette.dim, fontSize = 11.sp, lineHeight = 14.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                days.forEach { day ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(22.dp).clip(CircleShape)
                            .background(if (day.active) NihongoPalette.redLight else NihongoPalette.surface3)
                            .border(1.dp, NihongoPalette.borderStrong, CircleShape),
                            contentAlignment = Alignment.Center) {
                            if (day.active) Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(day.label, color = NihongoPalette.dim, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(NihongoPalette.border))
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(15.dp))
                    .background(Color(0x24A66DFF)).border(1.dp, Color(0x52A66DFF), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AccessTime, contentDescription = null,
                        tint = Color(0xFFA66DFF), modifier = Modifier.size(21.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (isId) "Estimasi belajar" else "Study estimate",
                        color = NihongoPalette.dim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(formatNativeDuration(stats.studyMinutes, isId), color = Color(0xFFA66DFF),
                        fontSize = 28.sp, fontWeight = FontWeight.Black, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
                Surface(
                    modifier = Modifier, shape = CircleShape,
                    color = Color(0x20A66DFF), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x50A66DFF))
                ) {
                    Text(if (isMinutesEmpty) (if (isId) "Mulai" else "Start") else (if (isId) "Stabil" else "Stable"),
                        Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        color = Color(0xFFA66DFF), fontWeight = FontWeight.Black, fontSize = 10.5.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(if (isId) "Rata-rata ${formatNativeDuration(stats.averageMinutes, isId, true)}/hari"
                 else "Average ${formatNativeDuration(stats.averageMinutes, isId, true)}/day",
                color = NihongoPalette.green, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(93.dp)) {
                Canvas(Modifier.fillMaxSize().pointerInput(days) {
                    detectTapGestures(onTap = { pos ->
                        val dayIndex = (pos.x / size.width * days.size).toInt().coerceIn(0, days.lastIndex)
                        selectedDay = if (selectedDay == dayIndex) -1 else dayIndex
                    })
                }) {
                    val w = size.width
                    val h = size.height
                    val top = 10.dp.toPx()
                    val bottom = h - 9.dp.toPx()
                    for (i in 0..3) {
                        val y = top + (bottom - top) * i / 3f
                        drawLine(NihongoPalette.border, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
                    }
                    val slot = w / days.size.coerceAtLeast(1)
                    val points = days.mapIndexed { i, day ->
                        Offset(slot * (i + .5f), bottom - (bottom - top) * (day.minutes / maxMinutes).coerceIn(0f, 1f))
                    }
                    val path = Path()
                    if (points.size > 1) {
                        path.moveTo(points.first().x, points.first().y)
                        points.drop(1).forEachIndexed { index, point ->
                            val previous = points[index]
                            path.quadraticTo(previous.x, previous.y, (previous.x + point.x) / 2f, (previous.y + point.y) / 2f)
                        }
                        val previous = points[points.lastIndex - 1]
                        val last = points.last()
                        path.quadraticTo(previous.x, previous.y, last.x, last.y)
                        val area = Path().apply {
                            addPath(path)
                            lineTo(last.x, bottom)
                            lineTo(points.first().x, bottom)
                            close()
                        }
                        drawPath(area, Brush.verticalGradient(listOf(Color(0x55A66DFF), Color(0x05A66DFF))))
                        drawPath(path, Color(0xFFA66DFF), style = Stroke(width = 2.8.dp.toPx()))
                    }
                    points.forEachIndexed { idx, point ->
                        drawCircle(if (days[idx].minutes > 0) Color(0xFFA66DFF) else NihongoPalette.surface3,
                            radius = if (idx == selectedDay) 5.dp.toPx() else 3.4.dp.toPx(), center = point)
                        drawCircle(Color(0xFFA66DFF), radius = 1.dp.toPx(), center = point)
                    }
                }
                if (selectedDay in days.indices) {
                    val day = days[selectedDay]
                    Surface(modifier = Modifier.align(Alignment.TopCenter).clickable { selectedDay = -1 },
                        shape = RoundedCornerShape(12.dp), color = Color(0xFF241D37),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66A66DFF))) {
                        Text("${day.label} • ${formatNativeDuration(day.minutes, isId, true)}",
                            Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = NihongoPalette.white,
                            fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                } else if (isMinutesEmpty) {
                    Text(if (isId) "Belum ada aktivitas belajar" else "No study activity yet",
                        color = NihongoPalette.dim, fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.Center))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                days.forEach { day ->
                    Text(day.label, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = NihongoPalette.dim, fontSize = 9.sp)
                }
            }
        }
    }
}

private fun formatNativeDuration(minutes: Int, isId: Boolean, compact: Boolean = false): String {
    val safe = minutes.coerceAtLeast(0)
    val unit = if (isId) (if (compact) "mnt" else "menit") else "min"
    if (safe < 60) return "$safe $unit"
    val hours = safe / 60
    if (hours < 24) return "$hours ${if (isId) "jam" else "hr"}" + if (safe % 60 > 0) " ${safe % 60} $unit" else ""
    val days = hours / 24
    if (days < 7) return "$days ${if (isId) "hari" else "days"}"
    val weeks = days / 7
    if (weeks < 4) return "$weeks ${if (isId) "minggu" else "weeks"}"
    val months = days / 30
    if (months < 12) return "${months.coerceAtLeast(1)} ${if (isId) "bulan" else "mo"}"
    return "${(days / 365).coerceAtLeast(1)} ${if (isId) "tahun" else "yr"}"
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

private fun parseNativeWeeklyStats(response: JSONObject): NativeWeeklyStats {
    val root = response.optJSONObject("stats") ?: response
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
        allDone = catalog.lessons.count { progress[it.id]?.passed == true }
    )
}

private fun fallbackDays(isId: Boolean): List<NativeDayStat> {
    val labels = if (isId) listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
                 else listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    return (6 downTo 0).map { before ->
        val day = Calendar.getInstance().apply { add(Calendar.DATE, -before) }
        val key = "%04d-%02d-%02d".format(Locale.US, day.get(Calendar.YEAR), day.get(Calendar.MONTH) + 1, day.get(Calendar.DATE))
        NativeDayStat(key, labels[day.get(Calendar.DAY_OF_WEEK) - 1], 0, 0, false)
    }
}

private fun formatCompactCredits(value: Long): String = when {
    value >= 1_000_000L -> "${((value / 100_000.0).roundToInt() / 10.0).toString().removeSuffix(".0")}jt"
    value >= 1_000L -> "${((value / 100.0).roundToInt() / 10.0).toString().removeSuffix(".0")}rb"
    else -> value.toString()
}

/** Find Activity lifecycle even when Compose LocalContext is a themed wrapper. */
private fun findNativeLifecycleOwner(context: Context): LifecycleOwner? {
    var current: Context = context
    while (current is ContextWrapper) {
        if (current is LifecycleOwner) return current
        val next = current.baseContext
        if (next === current) break
        current = next
    }
    return current as? LifecycleOwner
}
