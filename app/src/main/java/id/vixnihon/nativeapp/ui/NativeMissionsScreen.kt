package id.vixnihon.nativeapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import id.vixnihon.nativeapp.R
import id.vixnihon.nativeapp.data.AuthRepository
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.min

/** Native translation of client/src/screens/MissionsScreen.jsx.
 * The server remains authoritative for activity, eligibility, balances and reward amounts.
 * No WebView, local claim simulation or background polling.
 */
@Composable
internal fun NativeMissionsScreen(
    language: String,
    token: String,
    repository: AuthRepository,
    onBack: () -> Unit,
    onGoMission: (String) -> Unit,
    onNotice: (String) -> Unit,
) {
    val isId = language == "id"
    val scope = rememberCoroutineScope()
    var data by remember(token) { mutableStateOf<JSONObject?>(null) }
    var loading by remember(token) { mutableStateOf(true) }
    var claiming by remember(token) { mutableStateOf("") }
    var error by remember(token) { mutableStateOf("") }
    var refresh by remember(token) { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current

    BackHandler { onBack() }
    // Refresh on return from background, like the web's focus/visibility listener.
    // The initial fetch happens in LaunchedEffect below.
    DisposableEffect(owner) {
        var resumedOnce = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (resumedOnce) refresh++ else resumedOnce = true
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(token, refresh) {
        loading = true
        error = ""
        try {
            data = repository.fetchTodayMissions(token)
        } catch (e: Exception) {
            error = e.message ?: if (isId) "Gagal memuat misi" else "Failed to load missions"
        } finally {
            loading = false
        }
    }

    fun claim(id: String) {
        if (claiming.isNotEmpty() || loading) return
        claiming = id
        error = ""
        scope.launch {
            try {
                val next = repository.claimMission(token, id)
                data = next // server sends the updated mission/balance state atomically
                val reward = next.optJSONObject("reward")
                val xp = reward?.optInt("xp", 0) ?: 0
                val credits = reward?.optInt("credits", 0) ?: 0
                onNotice(if (isId) "Hadiah diklaim! +$xp XP · +$credits kredit" else "Reward claimed! +$xp XP · +$credits credits")
            } catch (e: Exception) {
                error = e.message ?: if (isId) "Gagal klaim hadiah" else "Claim failed"
                // A timeout may happen after the server committed the claim. Reconcile
                // without hiding the error banner or pretending it failed on the server.
                runCatching { repository.fetchTodayMissions(token) }.getOrNull()?.let { data = it }
            } finally {
                claiming = ""
            }
        }
    }

    val missions = remember(data) {
        val entries = data?.optJSONArray("missions") ?: JSONArray()
        (0 until entries.length()).mapNotNull { entries.optJSONObject(it) }
    }
    val daily = data?.optJSONObject("daily")
    val studySeconds = (daily?.optLong("missionStudySeconds", daily?.optLong("studySeconds", 0L) ?: 0L) ?: 0L).coerceAtLeast(0L)
    val studyPercent = (studySeconds / 6.0).toInt().coerceIn(0, 100)
    val studyMinutes = studySeconds / 60L
    val remainingMinutes = ceil((600L - studySeconds).coerceAtLeast(0L) / 60.0).toInt()
    val studyMission = missions.firstOrNull { it.optString("id") == "study_10_min" || it.optString("type") == "study" }
    val studyDone = (studyMission?.optBoolean("completed") == true || studyMission?.optBoolean("claimed") == true || studySeconds >= 600L)
    val baseDone = if (data?.has("baseClaimedCount") == true) data?.optInt("baseClaimedCount", 0) ?: 0
        else missions.count { it.optString("id") != "daily_bonus" && it.optBoolean("claimed") }
    val baseRequired = (data?.optInt("baseRequiredCount", 4) ?: 4).coerceAtLeast(1)
    val requiredPercent = (100f * baseDone / baseRequired).toInt().coerceIn(0, 100)
    val credits = data?.optJSONObject("balance")?.optLong("creditBalance", 0L) ?: 0L
    val claimedToday = data?.optLong("creditsClaimedToday", 0L) ?: 0L
    val dailyLimit = (data?.optLong("creditDailyLimit", 10L) ?: 10L).takeIf { it > 0L } ?: 10L
    val remaining = (data?.optLong("claimableCreditsRemainingToday", 0L) ?: 0L).coerceAtLeast(0L)
    val readyCredit = missions.filter { it.missionCanClaim() }.sumOf { it.optLong("credits", 0L).coerceAtLeast(0L) }
    val hasClaimableCredit = min(remaining, readyCredit) > 0

    val foreground = NihongoPalette.white
    val muted = NihongoPalette.dim
    val bg = NihongoPalette.background
    val surface = NihongoPalette.surface
    val surface2 = NihongoPalette.surface2
    val red = NihongoPalette.redLight
    val gold = if (NihongoPalette.lightMode) Color(0xFFC9952E) else Color(0xFFDEB55A)
    val blue = if (NihongoPalette.lightMode) Color(0xFF2563EB) else Color(0xFF3D7EEE)
    val green = NihongoPalette.green

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 98.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item(key = "mission_header") {
            Box(Modifier.fillMaxWidth().height(52.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 49.dp).align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(if (isId) "Misi Harian" else "Daily Missions", color = foreground,
                        fontSize = 21.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Text(if (isId) "Selesaikan misi dan kumpulkan kredit" else "Complete missions and collect credits",
                        color = muted, fontSize = 11.2.sp, fontWeight = FontWeight.Bold,
                        maxLines = 2, lineHeight = 13.sp)
                }
                MissionHeaderButton(Modifier.align(Alignment.CenterStart), enabled = true, onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = if (isId) "Kembali" else "Back", tint = foreground, modifier = Modifier.size(20.dp))
                }
                MissionHeaderButton(Modifier.align(Alignment.CenterEnd), enabled = !loading, onClick = { refresh++ }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = if (isId) "Muat ulang" else "Refresh", tint = red, modifier = Modifier.size(19.dp))
                }
            }
            Spacer(Modifier.height(13.dp))
        }

        item(key = "mission_hero") {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(red.copy(alpha = 0.21f), surface, surface2)))
                    .border(1.dp, red.copy(alpha = 0.19f), RoundedCornerShape(24.dp))
            ) {
                Box(Modifier.matchParentSize()) {
                    Image(
                        painterResource(R.drawable.mission_hero), contentDescription = null,
                        modifier = Modifier.align(Alignment.BottomEnd).offset(x = 52.dp, y = 34.dp)
                            .width(230.dp).alpha(0.42f), contentScale = ContentScale.FillWidth,
                    )
                }
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("$baseDone", color = red, fontSize = 34.sp, fontWeight = FontWeight.Black, lineHeight = 34.sp)
                                Text("/ $baseRequired", color = foreground, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(if (isId) "misi selesai" else "missions done", color = muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            MissionProgress(requiredPercent, red, modifier = Modifier.widthIn(max = 190.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        MissionPill(
                            if (hasClaimableCredit) (if (isId) "Siap klaim" else "Ready") else (if (isId) "Selesai" else "Done"),
                            green,
                        )
                    }
                    Spacer(Modifier.height(19.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(if (isId) "Kredit Saya" else "My Credits", color = muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(5.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MissionIcon(Icons.Outlined.Toll, gold, 27.dp, 14.dp)
                                Text(compactMissionNumber(credits), color = foreground, fontSize = 34.sp,
                                    fontWeight = FontWeight.Black, maxLines = 1)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (isId) "Batas kredit harian" else "Daily credit limit", color = muted,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(compactMissionNumber(dailyLimit), color = foreground, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.width(3.dp))
                                Icon(Icons.Outlined.Info, contentDescription = null, tint = muted, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        item(key = "mission_study") {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(surface, surface2)))
                    .border(1.dp, if (studyDone) green.copy(alpha = 0.30f) else NihongoPalette.borderStrong, RoundedCornerShape(22.dp))
            ) {
                Box(Modifier.matchParentSize()) {
                    Image(painterResource(R.drawable.mission_timer), contentDescription = null,
                        modifier = Modifier.align(Alignment.BottomEnd).offset(x = 18.dp, y = 26.dp).width(128.dp).alpha(0.55f),
                        contentScale = ContentScale.FillWidth)
                }
                Column(Modifier.fillMaxWidth().padding(15.dp).padding(end = 74.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MissionIcon(Icons.Outlined.AccessTime, if (studyDone) green else red, 42.dp, 21.dp)
                        Column {
                            Text(if (isId) "Belajar 10 Menit" else "Study 10 Minutes", color = foreground,
                                fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 2)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("%02d:%02d".format(Locale.US, studySeconds / 60, studySeconds % 60),
                                    color = if (studyDone) green else red, fontSize = 25.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.width(5.dp))
                                Text("/ 10:00", color = muted, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    MissionProgress(studyPercent, if (studyDone) green else red)
                    Spacer(Modifier.height(8.dp))
                    Text(if (studyDone) (if (isId) "Target belajar hari ini sudah tercapai." else "Today's study target is complete.")
                        else (if (isId) "Tinggal $remainingMinutes menit lagi" else "$remainingMinutes minutes left"),
                        color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 15.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        item(key = "mission_stats") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                MissionStat("${if (isId) "Menit Belajar" else "Study Min"}", studyMinutes.toString(),
                    Icons.Outlined.AccessTime, red, Modifier.weight(1f))
                MissionStat(if (isId) "Materi" else "Lessons", (daily?.optInt("lessonsDone", 0) ?: 0).toString(),
                    Icons.Outlined.School, blue, Modifier.weight(1f))
                MissionStat("Mini Quiz", (daily?.optInt("missionQuizCount", daily?.optInt("quizCount", 0) ?: 0) ?: 0).toString(),
                    Icons.Outlined.EmojiEvents, green, Modifier.weight(1f))
            }
            Spacer(Modifier.height(13.dp))
        }

        if (error.isNotBlank()) {
            item(key = "mission_error") {
                Surface(shape = RoundedCornerShape(17.dp), color = red.copy(alpha = 0.11f),
                    border = BorderStroke(1.dp, red.copy(alpha = 0.30f)), modifier = Modifier.fillMaxWidth()) {
                    Text(error, Modifier.padding(12.dp), color = red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        if (loading && data == null) {
            items(6, key = { "mission_loading_$it" }) {
                Box(Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(20.dp))
                    .background(surface2).border(1.dp, NihongoPalette.border, RoundedCornerShape(20.dp)))
                Spacer(Modifier.height(10.dp))
            }
        } else if (missions.isEmpty()) {
            item(key = "mission_empty") {
                Surface(modifier = Modifier.fillMaxWidth(), color = surface,
                    shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, NihongoPalette.borderStrong)) {
                    Text(if (isId) "Misi belum termuat. Coba muat ulang, atau cek login dan koneksi server."
                        else "Missions are not loaded. Reload, or check login and server connection.",
                        modifier = Modifier.padding(14.dp), color = muted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        } else {
            items(missions, key = { it.optString("id") }) { mission ->
                MissionListRow(mission, isId, claimBusy = claiming, onClaim = ::claim,
                    onGo = onGoMission)
                Spacer(Modifier.height(9.dp))
            }
        }

        item(key = "mission_reset") {
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(surface, surface2)))
                    .border(1.dp, NihongoPalette.borderStrong, RoundedCornerShape(22.dp))
                    .heightIn(min = 82.dp)
            ) {
                Box(Modifier.matchParentSize()) {
                    Image(painterResource(R.drawable.mission_reset), contentDescription = null,
                        modifier = Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 24.dp).width(116.dp).alpha(0.58f),
                        contentScale = ContentScale.FillWidth)
                }
                Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 108.dp, top = 14.dp, bottom = 14.dp)) {
                    Text(if (isId) "Misi direset setiap hari" else "Missions reset daily", color = foreground,
                        fontSize = 13.5.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(5.dp))
                    Text(if (isId) "Kredit misi dibatasi ${compactMissionNumber(dailyLimit)} kredit per hari. Hari ini sudah masuk ${compactMissionNumber(claimedToday)} kredit."
                        else "Mission credits are limited to ${compactMissionNumber(dailyLimit)} credits per day. Today you have claimed ${compactMissionNumber(claimedToday)} credits.",
                        color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
                }
            }
        }
    }
    if (loading && data == null) {
        NativeGlobalLoading(language = language, variant = "page", compact = true,
            message = if (isId) "Menyiapkan misi harian..." else "Preparing daily missions...")
    }
    }
}

private fun JSONObject.missionCanClaim(): Boolean =
    optBoolean("completed") && !optBoolean("claimed") && !optBoolean("optional") &&
        optBoolean("available", true) && optBoolean("claimable", true)

private fun compactMissionNumber(number: Long): String {
    if (number < 1000) return number.toString()
    return String.format(Locale.US, if (number >= 10000) "%.0frb" else "%.1frb", number / 1000.0)
}

@Composable
private fun MissionHeaderButton(modifier: Modifier, enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(modifier.size(42.dp).clip(RoundedCornerShape(15.dp))
        .background(NihongoPalette.surface)
        .border(1.dp, NihongoPalette.borderStrong, RoundedCornerShape(15.dp))
        .clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
private fun MissionProgress(percent: Int, tint: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(9.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.11f))) {
        Box(Modifier.fillMaxWidth((percent.coerceIn(0, 100)) / 100f)
            .fillMaxHeight().background(Brush.horizontalGradient(listOf(NihongoPalette.red, tint)), CircleShape))
    }
}

@Composable
private fun MissionIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, size: androidx.compose.ui.unit.Dp, iconSize: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(if (size > 30.dp) 15.dp else 100.dp))
        .background(tint.copy(alpha = 0.10f))
        .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(if (size > 30.dp) 15.dp else 100.dp)),
        contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun MissionPill(label: String, tint: Color) {
    Surface(color = tint.copy(alpha = 0.09f), shape = CircleShape,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.22f))) {
        Text(label, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = tint,
            fontSize = 10.5.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun MissionStat(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                        tint: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp),
        color = NihongoPalette.surface, border = BorderStroke(1.dp, tint.copy(alpha = 0.15f))) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
                Text(label, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Black,
                    maxLines = 2, lineHeight = 11.sp)
            }
            Spacer(Modifier.height(9.dp))
            Text(value, color = NihongoPalette.white, fontSize = 21.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun MissionListRow(
    mission: JSONObject, isId: Boolean, claimBusy: String, onClaim: (String) -> Unit, onGo: (String) -> Unit
) {
    val type = mission.optString("type")
    val id = mission.optString("id")
    val done = mission.optBoolean("completed")
    val claimed = mission.optBoolean("claimed")
    val optionalSafe = mission.optBoolean("optional") && !mission.optBoolean("available", true)
    val canClaim = mission.missionCanClaim()
    val locked = !done && !claimed && !optionalSafe
    val green = NihongoPalette.green
    val red = NihongoPalette.redLight
    val originalTint = when (type) {
        "quiz" -> Color(0xFF8B5CF6)
        "review" -> Color(0xFFF97316)
        "lesson" -> Color(0xFF3D7EEE)
        "study", "bonus" -> red
        else -> green
    }
    val tint = if (claimed || optionalSafe) green else originalTint
    val icon = when (type) {
        "checkin" -> Icons.Outlined.CheckCircle
        "study" -> Icons.Outlined.AccessTime
        "lesson" -> Icons.Outlined.MenuBook
        "quiz" -> Icons.Outlined.EmojiEvents
        "review" -> Icons.Outlined.Refresh
        else -> Icons.Outlined.CardGiftcard
    }
    val xp = if (claimed) mission.optLong("claimedXp", mission.optLong("xp", 0L)) else mission.optLong("xp", 0L)
    val credits = if (claimed) mission.optLong("claimedCredits", mission.optLong("credits", 0L)) else mission.optLong("credits", 0L)
    val reward = when {
        xp > 0 && credits > 0 -> "+$xp XP · +$credits ${if (isId) "kredit" else "credits"}"
        credits > 0 -> "+$credits ${if (isId) "kredit" else "credits"}"
        xp > 0 -> "+$xp XP"
        else -> if (isId) "Hadiah misi" else "Mission reward"
    }
    val status = when {
        optionalSafe -> if (isId) "Belum ada soal" else "No mistakes"
        claimed -> if (isId) "Selesai" else "Done"
        canClaim -> if (isId) "Siap klaim" else "Ready"
        type == "bonus" -> if (isId) "Terkunci" else "Locked"
        else -> if (isId) "Belum selesai" else "Not done"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(19.dp),
        color = NihongoPalette.surface,
        border = BorderStroke(1.dp,
            if (canClaim) red.copy(alpha = 0.24f)
            else if (claimed) green.copy(alpha = 0.20f)
            else NihongoPalette.borderStrong),
    ) {
        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            MissionIcon(icon, tint, 42.dp, 21.dp)
            Column(Modifier.weight(1f)) {
                Text(mission.optString(if (isId) "titleId" else "titleEn", id),
                    color = NihongoPalette.white, fontSize = 13.5.sp, fontWeight = FontWeight.Black,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Outlined.Toll, contentDescription = null, tint = if (NihongoPalette.lightMode) Color(0xFFC9952E) else Color(0xFFDEB55A),
                        modifier = Modifier.size(13.dp))
                    Text(reward, color = NihongoPalette.dim, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        maxLines = 2, lineHeight = 14.sp)
                }
                if (type == "checkin") {
                    Text("${mission.optInt("streak", 0)} ${if (isId) "hari" else "day(s)"}", color = green, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
            when {
                canClaim -> {
                    Surface(
                        shape = RoundedCornerShape(13.dp), color = red,
                        modifier = Modifier.widthIn(min = 72.dp).height(36.dp)
                            .clickable(enabled = claimBusy.isEmpty()) { onClaim(id) }
                    ) {
                        Box(Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                            Text(if (claimBusy == id) "..." else (if (isId) "Klaim" else "Claim"),
                                color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
                locked && type != "bonus" -> {
                    Surface(shape = RoundedCornerShape(13.dp), color = red.copy(alpha = 0.09f),
                        border = BorderStroke(1.dp, red.copy(alpha = 0.14f)),
                        modifier = Modifier.widthIn(min = 69.dp).height(34.dp).clickable { onGo(type) }) {
                        Box(Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                            Text(when (type) { "quiz", "study" -> if (isId) "Lanjut" else "Continue"
                                "review" -> "Review"; "lesson" -> if (isId) "Buka" else "Open"
                                else -> if (isId) "Mulai" else "Start" },
                                color = red, fontSize = 11.5.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                type == "bonus" && locked -> Icon(Icons.Outlined.Lock, contentDescription = status,
                    modifier = Modifier.size(16.dp), tint = NihongoPalette.dim)
                else -> MissionPill(status, if (claimed) green else NihongoPalette.dim)
            }
        }
    }
}
