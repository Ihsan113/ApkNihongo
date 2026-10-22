package id.vixnihon.nativeapp.ui

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.vixnihon.nativeapp.data.AuthRepository
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun nativeDate(daysAgo: Int): String {
    val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
}

@Composable
internal fun NativeCreditHistoryScreen(language: String, token: String, repository: AuthRepository,
    onBack: () -> Unit, onOpenOrder: (String) -> Unit) {
    val isId = language == "id"
    val context = LocalContext.current
    var kind by remember { mutableStateOf("all") }
    var quick by remember { mutableStateOf("30d") }
    var from by remember { mutableStateOf(nativeDate(29)) }
    var to by remember { mutableStateOf(nativeDate(0)) }
    var page by remember { mutableIntStateOf(1) }
    var refresh by remember { mutableIntStateOf(0) }
    var filtersExpanded by remember { mutableStateOf(false) }
    var payload by remember { mutableStateOf<JSONObject?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    fun reset() { kind = "all"; quick = "30d"; from = nativeDate(29); to = nativeDate(0); page = 1; refresh++ }
    fun chooseQuick(id: String) {
        quick = id; page = 1
        when (id) {
            "today" -> { from = nativeDate(0); to = nativeDate(0) }
            "7d" -> { from = nativeDate(6); to = nativeDate(0) }
            "30d" -> { from = nativeDate(29); to = nativeDate(0) }
            "all" -> { from = ""; to = "" }
        }
    }
    fun chooseDate(start: Boolean) {
        val current = if (start) from else to
        val calendar = Calendar.getInstance()
        runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(current) }.getOrNull()?.let { calendar.time = it }
        DatePickerDialog(context, { _, year, month, day ->
            val chosen = "%04d-%02d-%02d".format(Locale.US, year, month + 1, day)
            if (start) {
                from = chosen
                if (to.isNotBlank() && chosen > to) to = chosen
            } else {
                to = chosen
                if (from.isNotBlank() && chosen < from) from = chosen
            }
            quick = "custom"; page = 1
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    BackHandler { onBack() }
    LaunchedEffect(token, language, kind, from, to, page, refresh) {
        loading = true; error = ""
        try { payload = repository.fetchCreditHistoryFiltered(token, language, kind, page, from, to) }
        catch (e: Exception) { error = e.message ?: "Gagal memuat riwayat" }
        finally { loading = false }
    }
    val paging = payload?.optJSONObject("pagination")
    val entries = remember(payload) { payload?.optJSONArray("items")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } } ?: emptyList() }
    val orders = remember(payload) { payload?.optJSONArray("orders")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } } ?: emptyList() }
    val total = paging?.optLong("total", 0L) ?: 0L
    val pages = (paging?.optInt("totalPages", 1) ?: 1).coerceAtLeast(1)
    val shownPage = paging?.optInt("page", page)?.coerceIn(1, pages) ?: page
    Box(Modifier.fillMaxSize().background(NihongoPalette.background)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                CreditHeader(if (isId) "Riwayat Kredit & XP" else "Credit & XP History", if (isId) "Pantau saldo, XP, dan order top up" else "Track balance, XP, and top-up orders", onBack = onBack, onInfo = { reset() }, trailingRefresh = true)
            }
            item {
                CreditCard {
                    val balance = payload?.optJSONObject("balance")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HistoryMetric("Kredit", creditCount(balance?.optLong("creditBalance") ?: 0L), CreditGold, Modifier.weight(1f))
                        HistoryMetric("XP", creditCount(balance?.optLong("xpTotal") ?: 0L), CreditBlue, Modifier.weight(1f))
                        HistoryMetric(if (isId) "Riwayat" else "Records", creditCount(total), CreditRed, Modifier.weight(1f))
                    }
                }
            }
            item {
                CreditCard {
                    val kindName = when (kind) {
                        "credit" -> if (isId) "Kredit" else "Credit"
                        "xp" -> "XP"
                        else -> if (isId) "Semua transaksi" else "All transactions"
                    }
                    val periodName = when (quick) {
                        "today" -> if (isId) "Hari ini" else "Today"
                        "7d" -> if (isId) "7 hari" else "7 days"
                        "30d" -> if (isId) "30 hari" else "30 days"
                        "all" -> if (isId) "Semua waktu" else "All time"
                        else -> if (from.isNotBlank() && to.isNotBlank()) "$from – $to" else if (isId) "Pilih tanggal" else "Select dates"
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable { filtersExpanded = !filtersExpanded }.padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(40.dp).background(NihongoPalette.surface2, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Tune, contentDescription = null, tint = CreditRed, modifier = Modifier.size(20.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(if (isId) "Filter riwayat" else "History filters", color = NihongoPalette.white, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            Text("$kindName · $periodName", color = NihongoPalette.dim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(if (filtersExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = if (filtersExpanded) "Tutup filter" else "Buka filter", tint = NihongoPalette.dim)
                    }
                    if (filtersExpanded) {
                        Spacer(Modifier.height(3.dp))
                        HorizontalDivider(color = NihongoPalette.border)
                        Spacer(Modifier.height(3.dp))
                        Text(if (isId) "Jenis transaksi" else "Transaction type", color = NihongoPalette.dim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("all", "credit", "xp").forEach { choice ->
                                val label = when(choice) { "all" -> if (isId) "Semua" else "All"; "credit" -> if (isId) "Kredit" else "Credit"; else -> "XP" }
                                CreditPill(label, kind == choice, Modifier.weight(1f)) { kind = choice; page = 1 }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(if (isId) "Periode" else "Period", color = NihongoPalette.dim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CreditPill(if (isId) "Hari ini" else "Today", quick == "today", Modifier.weight(1f)) { chooseQuick("today") }
                            CreditPill(if (isId) "7 hari" else "7 days", quick == "7d", Modifier.weight(1f)) { chooseQuick("7d") }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CreditPill(if (isId) "30 hari" else "30 days", quick == "30d", Modifier.weight(1f)) { chooseQuick("30d") }
                            CreditPill(if (isId) "Semua waktu" else "All time", quick == "all", Modifier.weight(1f)) { chooseQuick("all") }
                        }
                        Row(
                            Modifier.fillMaxWidth().background(
                                if (quick == "custom") NihongoPalette.red.copy(alpha = .13f) else NihongoPalette.surface2,
                                RoundedCornerShape(14.dp)
                            ).clickable { chooseQuick("custom") }.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Outlined.DateRange, contentDescription = null, tint = CreditRed, modifier = Modifier.size(19.dp))
                            Text(if (isId) "Rentang tanggal khusus" else "Custom date range", color = NihongoPalette.white, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = NihongoPalette.dim, modifier = Modifier.size(19.dp))
                        }
                        if (quick == "custom") {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                HistoryDateButton(if (isId) "Dari" else "From", from, Modifier.weight(1f)) { chooseDate(true) }
                                HistoryDateButton(if (isId) "Sampai" else "To", to, Modifier.weight(1f)) { chooseDate(false) }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Text(if (isId) "Reset filter" else "Reset filters", color = CreditRed, fontWeight = FontWeight.Bold,
                                fontSize = 12.sp, modifier = Modifier.clickable { reset() }.padding(vertical = 7.dp, horizontal = 4.dp))
                        }
                    }
                    payload?.optJSONObject("retention")?.takeIf { it.optBoolean("detailAutoArchive", false) }?.let { retention ->
                        Text(if (isId) "Detail riwayat diarsipkan setelah ${retention.optInt("days")} hari. Saldo tetap aman."
                        else "History detail is archived after ${retention.optInt("days")} days.", color = NihongoPalette.dim, fontSize = 11.sp)
                    }
                }
            }
            if (error.isNotBlank()) item { CreditNotice(error) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isId) "Riwayat Transaksi" else "Transaction History", color = NihongoPalette.white, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text("${creditCount(total)} ${if (isId) "catatan" else "records"}", color = NihongoPalette.dim, fontSize = 11.sp)
                }
            }
            if (loading && payload == null) items(4) { CreditCard { Spacer(Modifier.height(65.dp)) } }
            else if (!loading && entries.isEmpty()) item {
                CreditCard {
                    Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(36.dp), tint = CreditRed)
                    Text(if (isId) "Belum ada riwayat di filter ini" else "No history in this filter", color = NihongoPalette.white, fontWeight = FontWeight.Black)
                    Text(if (isId) "Coba ubah tanggal atau jenis riwayat." else "Try another date or history type.", color = NihongoPalette.dim, fontSize = 12.sp)
                }
            } else items(entries, key = { "${page}:${it.optString("kind")}:${it.optString("id")}" }) { item ->
                HistoryLedgerRow(item, language)
            }
            if (!loading && total > 0L) item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.clickable(enabled = shownPage > 1) { page = shownPage - 1 }, color = NihongoPalette.surface2, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous", Modifier.padding(10.dp), tint = if (shownPage > 1) NihongoPalette.white else NihongoPalette.dim)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${if (isId) "Halaman" else "Page"} $shownPage / $pages", color = NihongoPalette.white, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text("${creditCount(total)} ${if (isId) "catatan tersimpan" else "saved records"}", color = NihongoPalette.dim, fontSize = 10.sp)
                    }
                    Surface(Modifier.clickable(enabled = shownPage < pages) { page = shownPage + 1 }, color = NihongoPalette.surface2, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = "Next", Modifier.padding(10.dp), tint = if (shownPage < pages) NihongoPalette.white else NihongoPalette.dim)
                    }
                }
            }
            if (orders.isNotEmpty()) {
                item { Text(if (isId) "Order Top Up" else "Top-up Orders", color = NihongoPalette.white, fontWeight = FontWeight.Black, fontSize = 16.sp) }
                items(orders, key = { "order-${it.optString("id").ifBlank { it.optString("orderId") }}" }) { order ->
                    val id = order.optString("orderId")
                    CreditCard(Modifier.clickable(enabled = id.isNotBlank()) { onOpenOrder(id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Receipt, contentDescription = null, tint = creditStatusColor(order.optString("status")))
                            Column(Modifier.weight(1f)) {
                                Text("${if (isId) "Paket Kredit" else "Credit Package"} ${creditCount(order.optLong("credits"))}", color = NihongoPalette.white, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                Text(id, color = NihongoPalette.dim, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${creditDate(order.optLong("createdAt"), language)} · ${creditMoney(order.optLong("priceIdr"))}", color = NihongoPalette.dim, fontSize = 11.sp)
                            }
                            Text(creditStatus(order.optString("status"), language), color = creditStatusColor(order.optString("status")), fontSize = 10.sp)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
        if (loading && payload == null) NativeGlobalLoading(language = language, variant = "page", compact = true)
    }
}

@Composable
private fun HistoryMetric(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = NihongoPalette.dim, fontSize = 10.sp, fontWeight = FontWeight.Black)
        Text(value, color = tint, fontSize = 21.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun HistoryLedgerRow(item: JSONObject, language: String) {
    val isId = language == "id"
    val isCredit = item.optString("kind") == "credit"
    val direction = item.optString("direction")
    val incoming = direction == "in" || direction == "credit" // Server uses 'in' in the ledger.
    val tint = if (!isCredit) CreditBlue else if (incoming) CreditGreen else CreditRed
    val unit = (if (isId) item.optString("amountUnitId") else item.optString("amountUnitEn")).ifBlank { item.optString("amountUnit").ifBlank { if (isCredit) "kredit" else "XP" } }
    val typeLabel = (if (isId) item.optString("kindLabelId") else item.optString("kindLabelEn")).ifBlank { item.optString("kindLabel").ifBlank { if (isCredit) "Kredit" else "XP" } }
    val note = (if (isId) item.optString("noteId") else item.optString("noteEn")).ifBlank { item.optString("note").ifBlank { item.optString("source") } }
    CreditCard {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(40.dp).background(tint.copy(alpha = 0.12f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(if (!isCredit) Icons.Outlined.Star else if (incoming) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward, contentDescription = null, tint = tint)
            }
            Column(Modifier.weight(1f)) {
                Text(typeLabel, fontWeight = FontWeight.Black, color = NihongoPalette.white, fontSize = 13.sp)
                Text(note, color = NihongoPalette.dim, fontSize = 11.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(creditDate(item.optLong("createdAt"), language), color = NihongoPalette.dim, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${if (!isCredit || incoming) "+" else "−"}${creditCount(item.optLong("amount"))}", color = tint, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text(unit, color = NihongoPalette.dim, fontSize = 10.sp)
                Text("${if (isId) "Saldo" else "Balance"} ${creditCount(item.optLong("balanceAfter"))}", color = NihongoPalette.dim, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun HistoryDateButton(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.background(NihongoPalette.surface2, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, color = NihongoPalette.dim, fontSize = 10.sp)
        Text(value.ifBlank { "Pilih tanggal" }, color = NihongoPalette.white, fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
