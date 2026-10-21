package id.vixnihon.nativeapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.vixnihon.nativeapp.data.AuthRepository
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Native Home destinations. Not a WebView: the linked Home shortcuts consume real server data.
 * Scope: Home navigation and read/claim actions, not the full lesson player or payment checkout.
 */
@Composable
internal fun HomeLinkedDestination(
    route: String,
    language: String,
    token: String,
    repository: AuthRepository,
    chapter: NativeChapterMeta?,
    lessons: List<NativeLessonMeta>,
    progress: Map<String, NativeLessonProgress>,
    currentLessonId: String?,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onNotice: (String) -> Unit
) {
    val isId = language == "id"
    val scope = rememberCoroutineScope()
    var refresh by remember(route) { mutableIntStateOf(0) }
    var kind by remember(route) { mutableStateOf("all") }
    var page by remember(route) { mutableIntStateOf(1) }
    var loading by remember(route) { mutableStateOf(false) }
    var payload by remember(route) { mutableStateOf<JSONObject?>(null) }
    var error by remember(route) { mutableStateOf("") }
    var claimBusy by remember(route) { mutableStateOf("") }

    BackHandler(enabled = true) { onBack() }
    LaunchedEffect(route, language, token, refresh, kind, page) {
        if (route == "learn") return@LaunchedEffect
        loading = true
        error = ""
        try {
            payload = when (route) {
                "missions" -> repository.fetchTodayMissions(token)
                "topup" -> repository.fetchCreditPackages(token, language)
                "history" -> repository.fetchCreditHistory(token, language, kind, page)
                else -> null
            }
        } catch (e: Exception) {
            error = e.message ?: if (isId) "Gagal memuat data" else "Could not load data"
        } finally {
            loading = false
        }
    }

    val title = when (route) {
        "learn" -> if (isId) "Lanjutkan Belajar" else "Continue Learning"
        "missions" -> if (isId) "Misi Harian" else "Daily Missions"
        "topup" -> if (isId) "Beli Kredit" else "Buy Credits"
        "history" -> if (isId) "Riwayat Kredit & XP" else "Credit & XP History"
        else -> "NihonGo"
    }
    Column(Modifier.fillMaxSize().background(NihongoPalette.background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(42.dp).border(1.dp, NihongoPalette.borderStrong, RoundedCornerShape(15.dp))
                    .clickable(onClick = onBack), contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.ArrowBack, contentDescription = if (isId) "Kembali" else "Back", tint = NihongoPalette.white) }
            Text(title, Modifier.weight(1f), color = NihongoPalette.white, fontSize = 20.sp, fontWeight = FontWeight.Black)
            if (route != "learn") {
                Box(
                    Modifier.size(42.dp).border(1.dp, NihongoPalette.borderStrong, RoundedCornerShape(15.dp))
                        .clickable { refresh++ }, contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Refresh, contentDescription = if (isId) "Muat ulang" else "Reload", tint = NihongoPalette.redLight) }
            }
        }
        if (loading && payload == null) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NihongoPalette.redLight)
            }
        }
        if (error.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp), color = NihongoPalette.surface,
                border = BorderStroke(1.dp, NihongoPalette.redLight.copy(alpha = 0.3f))
            ) { Text(error, Modifier.padding(12.dp), color = NihongoPalette.redSoft, fontSize = 12.sp) }
        }
        when (route) {
            "learn" -> {
                val currentChapterLessons = chapter?.lessonIds?.mapNotNull { id -> lessons.firstOrNull { it.id == id } }.orEmpty()
                val byNo = remember(lessons) { lessons.associateBy { it.no } }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 36.dp)
                ) {
                    item {
                        HomeDetailCard {
                            Text(chapter?.let { if (isId) it.titleId else it.titleEn } ?: "NihonGo", color = NihongoPalette.white, fontSize = 17.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(5.dp))
                            Text(if (isId) "Materi berikutnya dipilih otomatis berdasarkan status lulus dan urutan unlock, sama seperti Home web." else "The next lesson uses the same pass and unlock rules as the website.", color = NihongoPalette.dim, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    items(currentChapterLessons, key = { it.id }) { lesson ->
                        val passed = progress[lesson.id]?.passed == true
                        val unlock = lesson.no == 1 || byNo[lesson.no - 1]?.let { progress[it.id]?.passed == true } == true
                        HomeDetailCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (passed) Icons.Outlined.CheckCircle else Icons.Outlined.MenuBook, contentDescription = null,
                                    tint = if (passed) NihongoPalette.green else if (unlock) NihongoPalette.redLight else NihongoPalette.dim, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("${lesson.no}. ${if (isId) lesson.titleId else lesson.titleEn}", color = NihongoPalette.white, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                    Text(when { passed -> if (isId) "Selesai" else "Completed"; lesson.id == currentLessonId -> if (isId) "Materi selanjutnya" else "Next lesson"; unlock -> if (isId) "Terbuka" else "Unlocked"; else -> if (isId) "Terkunci" else "Locked" },
                                        color = if (passed) NihongoPalette.green else NihongoPalette.dim, fontSize = 11.sp)
                                }
                            }
                            if (unlock && lesson.id == currentLessonId) {
                                Spacer(Modifier.height(7.dp))
                                Text(if (isId) "Pemutar materi native sedang dikembangkan; progres tetap disimpan server." else "Native lesson player is under development; progress remains on the server.",
                                    color = NihongoPalette.dim, fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            "missions" -> {
                val missions = payload?.optJSONArray("missions") ?: JSONArray()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 38.dp)
                ) {
                    if (payload != null) {
                        item {
                            HomeDetailCard {
                                Text("${payload?.optInt("baseClaimedCount", 0) ?: 0} / ${payload?.optInt("baseRequiredCount", 4) ?: 4} ${if (isId) "misi selesai" else "missions completed"}",
                                    color = NihongoPalette.white, fontSize = 23.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(4.dp))
                                Text("${if (isId) "Saldo kredit" else "Credit balance"}: ${formatHomeNumber(payload?.optJSONObject("balance")?.optLong("creditBalance", 0L) ?: 0L)}",
                                    color = NihongoPalette.dim2, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    items(missions.length(), key = { index -> missions.optJSONObject(index)?.optString("id") ?: index.toString() }) { index ->
                        val item = missions.optJSONObject(index) ?: JSONObject()
                        val id = item.optString("id")
                        val done = item.optBoolean("completed")
                        val claimed = item.optBoolean("claimed")
                        val claimable = done && !claimed && !item.optBoolean("optional") && item.optBoolean("claimable", true) && item.optBoolean("available", true)
                        HomeDetailCard {
                            Text(item.optString(if (isId) "titleId" else "titleEn", id), color = NihongoPalette.white, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(4.dp))
                            Text("+${item.optInt("xp", 0)} XP   ·   +${item.optInt("credits", 0)} ${if (isId) "kredit" else "credits"}", color = NihongoPalette.dim2, fontSize = 11.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(when { claimed -> if (isId) "Sudah diklaim" else "Claimed"; done -> if (isId) "Selesai · siap klaim" else "Complete · ready to claim"; else -> if (isId) "Belum selesai" else "Not yet complete" },
                                color = if (done) NihongoPalette.green else NihongoPalette.dim, fontSize = 11.sp)
                            if (claimable) {
                                Spacer(Modifier.height(9.dp))
                                Button(onClick = {
                                    if (claimBusy.isNotBlank()) return@Button
                                    claimBusy = id
                                    scope.launch {
                                        try {
                                            payload = repository.claimMission(token, id)
                                            error = ""
                                        } catch (e: Exception) {
                                            error = e.message ?: if (isId) "Klaim gagal" else "Claim failed"
                                        } finally { claimBusy = "" }
                                    }
                                }, enabled = claimBusy.isBlank(), colors = ButtonDefaults.buttonColors(containerColor = NihongoPalette.redLight)) {
                                    Text(if (claimBusy == id) (if (isId) "Mengklaim..." else "Claiming...") else (if (isId) "Klaim hadiah" else "Claim reward"))
                                }
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                    }
                }
            }
            "topup" -> {
                val packs = payload?.optJSONArray("packages") ?: JSONArray()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 38.dp)
                ) {
                    item {
                        HomeDetailCard {
                            Text(if (isId) "Paket Kredit" else "Credit Packages", color = NihongoPalette.white, fontSize = 17.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(4.dp))
                            Text(if (isId) "Daftar paket diambil langsung dari server. Pembayaran QRIS native belum diaktifkan agar tidak membuat order tanpa layar pembayaran." else "Live packages from the server. Native QRIS checkout is not enabled until the payment screen is ready.",
                                color = NihongoPalette.dim, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    items(packs.length(), key = { index -> packs.optJSONObject(index)?.optString("id") ?: index.toString() }) { index ->
                        val item = packs.optJSONObject(index) ?: JSONObject()
                        HomeDetailCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = Color(0xFFF5BC42))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("${formatHomeNumber(item.optLong("credits"))} ${if (isId) "Kredit" else "Credits"}", color = NihongoPalette.white, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                    Text(item.optString("id"), color = NihongoPalette.dim, fontSize = 11.sp)
                                }
                                Text("Rp ${formatHomeNumber(item.optLong("priceIdr"))}", color = Color(0xFFF5BC42), fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                    }
                }
            }
            "history" -> {
                val history = payload?.optJSONArray("items") ?: JSONArray()
                val paging = payload?.optJSONObject("pagination") ?: JSONObject()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp)
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("all", "credit", "xp").forEach { filter ->
                                val selected = filter == kind
                                Surface(
                                    modifier = Modifier.weight(1f).clickable { kind = filter; page = 1 },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) NihongoPalette.red.copy(alpha = 0.28f) else NihongoPalette.surface,
                                    border = BorderStroke(1.dp, if (selected) NihongoPalette.redLight else NihongoPalette.borderStrong)
                                ) {
                                    Text(when (filter) { "all" -> if (isId) "Semua" else "All"; "credit" -> if (isId) "Kredit" else "Credit"; else -> "XP" },
                                        Modifier.padding(10.dp), color = NihongoPalette.white, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    items(history.length(), key = { index -> "${page}-${history.optJSONObject(index)?.optString("id") ?: index}" }) { index ->
                        val item = history.optJSONObject(index) ?: JSONObject()
                        HomeDetailCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (item.optString("kind") == "xp") Icons.Outlined.Star else Icons.Outlined.History,
                                    contentDescription = null, tint = if (item.optString("kind") == "xp") Color(0xFFF5BC42) else NihongoPalette.green)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.optString("note").ifBlank { item.optString("source") }, color = NihongoPalette.white, fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    val timestamp = item.optLong("createdAt", 0L)
                                    Text(if (timestamp > 0) formatHomeDate(timestamp) else "", color = NihongoPalette.dim, fontSize = 10.sp)
                                }
                                Text("${if (item.optString("direction") == "debit") "−" else "+"}${formatHomeNumber(item.optLong("amount"))}",
                                    color = if (item.optString("direction") == "debit") NihongoPalette.redSoft else NihongoPalette.green,
                                    fontWeight = FontWeight.Black, fontSize = 15.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (payload != null && history.length() == 0) {
                        item { Text(if (isId) "Belum ada transaksi." else "No transactions yet.", Modifier.padding(18.dp), color = NihongoPalette.dim) }
                    }
                    if (paging.optInt("totalPages", 1) > 1) {
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isId) "Halaman $page / ${paging.optInt("totalPages", 1)}" else "Page $page / ${paging.optInt("totalPages", 1)}", color = NihongoPalette.dim, fontSize = 12.sp)
                                Row {
                                    Text("‹", Modifier.clickable(enabled = page > 1) { page-- }.padding(14.dp), color = NihongoPalette.white, fontSize = 24.sp)
                                    Text("›", Modifier.clickable(enabled = page < paging.optInt("totalPages", 1)) { page++ }.padding(14.dp), color = NihongoPalette.white, fontSize = 24.sp)
                                }
                            }
                        }
                    }
                }
            }
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (isId) "Belum tersedia" else "Not available", color = NihongoPalette.dim) }
        }
    }
}

@Composable
private fun HomeDetailCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = NihongoPalette.surface,
        border = BorderStroke(1.dp, NihongoPalette.borderStrong)
    ) { Column(Modifier.padding(14.dp)) { content() } }
}

private fun formatHomeNumber(number: Long): String = NumberFormat.getNumberInstance(Locale("id", "ID")).format(number)
private fun formatHomeDate(timestamp: Long): String = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(timestamp))
