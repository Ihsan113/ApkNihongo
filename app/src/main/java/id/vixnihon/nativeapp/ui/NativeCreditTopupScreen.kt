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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.vixnihon.nativeapp.data.AuthRepository
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.roundToLong

data class CreditPurchaseChoice(val packageId: String?, val title: String, val base: Long, val fee: Long, val premium: Boolean = false)

@Composable
internal fun NativeCreditTopupScreen(
    language: String, token: String, repository: AuthRepository,
    onBack: () -> Unit, onHistory: () -> Unit, onOrder: (String) -> Unit
) {
    val isId = language == "id"
    val scope = rememberCoroutineScope()
    val narrowScreen = LocalConfiguration.current.screenWidthDp < 370
    var refresh by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var payload by remember { mutableStateOf<JSONObject?>(null) }
    var methods by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf<CreditPurchaseChoice?>(null) }
    var info by remember { mutableStateOf(false) }
    var lookup by remember { mutableStateOf("") }

    BackHandler { if (!creating) onBack() }
    LaunchedEffect(token, language, refresh) {
        loading = true
        error = ""
        try { payload = repository.fetchCreditPackages(token, language) }
        catch (e: Exception) { error = e.message ?: "Gagal memuat paket kredit" }
        finally { loading = false }
    }
    // Payment-method provider can time out; never block visible package data for this.
    LaunchedEffect(token) {
        methods = runCatching { repository.fetchPaymentMethods(token) }.getOrNull()
    }
    val packs = remember(payload) { (payload?.optJSONArray("packages")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } } ?: emptyList()) }
    val recent = remember(payload) {
        val a = payload?.optJSONArray("recentOrders") ?: payload?.optJSONArray("orders")
        a?.let { (0 until minOf(5, it.length())).mapNotNull { i -> it.optJSONObject(i) } } ?: emptyList()
    }
    val payment = payload?.optJSONObject("payment")
    val ready = payload != null && (payment?.optBoolean("ready", true) != false) && (payment?.optBoolean("maintenance", false) != true)
    val premium = payment?.optJSONObject("premium")
    val methodArray = methods?.optJSONArray("methods")
    val qris = if (methodArray == null) null else (0 until methodArray.length()).mapNotNull { methodArray.optJSONObject(it) }
        .firstOrNull { it.optString("code").equals("QRIS", true) } ?: methodArray.optJSONObject(0)
    fun pick(packageId: String?, title: String, price: Long, premiumChoice: Boolean = false) {
        if (!ready || creating) return
        val feeFlat = qris?.optDouble("feeFlat", 0.0) ?: 0.0
        val feePercent = qris?.optDouble("feePercent", 0.0) ?: 0.0
        val fee = (feeFlat.coerceAtLeast(0.0) + price.coerceAtLeast(0).toDouble() * feePercent.coerceAtLeast(0.0) / 100.0).roundToLong()
        selection = CreditPurchaseChoice(packageId, title, price, fee, premiumChoice)
    }
    Box(Modifier.fillMaxSize().background(NihongoPalette.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { CreditHeader(if (isId) "Beli Kredit" else "Buy Credits", if (isId) "Pilih paket kredit yang kamu butuhkan" else "Choose the credit package you need", onBack = onBack, onInfo = { info = true }) }
            item {
                Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(NihongoPalette.red, CreditRed, CreditGold.copy(alpha = 0.55f))), RoundedCornerShape(23.dp)).padding(17.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (isId) "Saldo Kredit" else "Credit Balance", color = Color.White.copy(alpha = 0.84f), fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Text("✦ ${creditCount(payload?.optJSONObject("balance")?.optLong("creditBalance", 0L) ?: 0L)}", color = Color.White, fontSize = 33.sp, fontWeight = FontWeight.Black)
                            Text(if (isId) "Kredit dipakai untuk fitur premium aplikasi." else "Credits are used for premium features.", color = Color.White.copy(alpha = .82f), fontSize = 11.sp)
                        }
                        Box(Modifier.size(44.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(50)).clickable { onHistory() }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.ChevronRight, contentDescription = "Riwayat", tint = Color.White)
                        }
                    }
                }
            }
            if (error.isNotBlank()) item { CreditNotice(error) }
            if (!loading && !ready && payload != null) item {
                CreditNotice(if (payment?.optBoolean("maintenance") == true) {
                    if (isId) payment.optString("maintenanceMessageId").ifBlank { "Pembelian kredit sedang pemeliharaan." }
                    else payment.optString("maintenanceMessageEn").ifBlank { "Credit purchase is under maintenance." }
                } else if (isId) "Payment gateway belum aktif. Pembelian sementara dinonaktifkan." else "Payment gateway is not active. Purchasing is disabled.")
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (isId) "Pilih Paket Kredit" else "Choose Package", fontWeight = FontWeight.Black, color = NihongoPalette.white, fontSize = 17.sp)
                        Text(if (isId) "Pilih paket, lalu bayar dengan QRIS." else "Choose a package, then pay via QRIS.", color = NihongoPalette.dim, fontSize = 11.sp)
                    }
                    Text(if (isId) "Hemat & Cepat" else "Fast & Safe", color = CreditGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (loading) {
                items(2) { CreditCard { Spacer(Modifier.height(136.dp)) } }
            } else {
                // Match the web's mobile breakpoint: single-column below 370dp.
                if (narrowScreen) {
                    items(packs, key = { "pack-${it.optString("id")}" }) { pack ->
                        CreditPackageCard(pack, language, popular = pack === packs.firstOrNull() || pack.optString("badge").isNotBlank(), enabled = ready && !creating) {
                            pick(pack.optString("id"), if (isId) "Beli ${creditCount(pack.optLong("credits"))} Kredit" else "Buy ${creditCount(pack.optLong("credits"))} Credits", pack.optLong("priceIdr"))
                        }
                    }
                } else {
                    items(packs.chunked(2), key = { "pack-row-${it.firstOrNull()?.optString("id") ?: "none"}" }) { group ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            group.forEach { pack ->
                                val popular = pack === packs.firstOrNull() || pack.optString("badge").isNotBlank()
                                Box(Modifier.weight(1f)) {
                                    CreditPackageCard(pack, language, popular, enabled = ready && !creating) {
                                        pick(pack.optString("id"), if (isId) "Beli ${creditCount(pack.optLong("credits"))} Kredit" else "Buy ${creditCount(pack.optLong("credits"))} Credits", pack.optLong("priceIdr"))
                                    }
                                }
                            }
                            if (group.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                if (packs.isEmpty() && payload != null) item { CreditCard { Text(if (isId) "Paket kredit belum tersedia." else "No packages available.", color = NihongoPalette.dim) } }
            }
            if (premium != null) item {
                CreditCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Star, contentDescription = null, tint = CreditGold, modifier = Modifier.size(34.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Premium Materi", color = NihongoPalette.white, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(if (premium.optInt("durationDays") > 0) "${premium.optInt("durationDays")} ${if (isId) "hari" else "days"}" else if (isId) "Seumur hidup" else "Lifetime", color = CreditGold, fontSize = 11.sp)
                        }
                    }
                    Text(if (isId) "Buka semua bab N5–N1 tanpa potongan kredit pada menu Belajar. Pembelian satu kali, bukan langganan." else "Unlock N5–N1 learning chapters without credit deduction. One-time purchase.", color = NihongoPalette.dim, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(creditMoney(premium.optLong("priceIdr")), color = CreditGold, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        if (premium.optBoolean("active")) {
                            Text(
                                (if (isId) "Aktif" else "Active") + premium.optLong("activeUntil", 0L).takeIf { it > 0L }?.let { " · ${creditDate(it, language)}" }.orEmpty(),
                                color = CreditGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(if (isId) "Beli Sekarang ›" else "Buy Now ›", Modifier.clickable(enabled = ready && !creating) { pick(null, "Premium Materi", premium.optLong("priceIdr"), true) }.padding(8.dp), color = if (ready) CreditGold else NihongoPalette.dim, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }
            item {
                CreditCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = CreditRed)
                        Column {
                            Text(if (isId) "Punya Order ID?" else "Have an Order ID?", color = NihongoPalette.white, fontWeight = FontWeight.Black)
                            Text(if (isId) "Cek status pembayaran manual" else "Check payment status manually", fontSize = 11.sp, color = NihongoPalette.dim)
                        }
                    }
                    OutlinedTextField(value = lookup, onValueChange = { lookup = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text(if (isId) "Tempel Order ID" else "Paste Order ID") })
                    CreditAction(if (isId) "Buka Order" else "Open Order") {
                        if (lookup.isNotBlank()) onOrder(lookup.trim()) else error = if (isId) "Masukkan Order ID terlebih dahulu." else "Enter an Order ID first."
                    }
                }
            }
            item {
                CreditCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.History, contentDescription = null, tint = CreditGold)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isId) "Riwayat Pembelian Terakhir" else "Recent Purchases", Modifier.weight(1f), color = NihongoPalette.white, fontWeight = FontWeight.Black)
                        Text(if (isId) "Lihat semua" else "View all", Modifier.clickable { onHistory() }, color = CreditRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (recent.isEmpty()) Text(if (isId) "Belum ada pembelian kredit." else "No credit purchases yet.", color = NihongoPalette.dim, fontSize = 11.sp)
                    recent.forEach { order ->
                        val id = order.optString("orderId").ifBlank { order.optString("id") }
                        Row(Modifier.fillMaxWidth().background(NihongoPalette.background, RoundedCornerShape(16.dp)).clickable(enabled = id.isNotBlank()) { onOrder(id) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = creditStatusColor(order.optString("status")))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${creditCount(order.optLong("credits"))} ${if (isId) "Kredit" else "Credits"} · ${creditMoney(order.optLong("priceIdr"))}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = NihongoPalette.white, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${creditDate(order.optLong("createdAt"), language)} · ${creditStatus(order.optString("status"), language)}", fontSize = 10.sp, color = NihongoPalette.dim, maxLines = 1)
                            }
                            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = NihongoPalette.dim)
                        }
                    }
                }
            }
            item {
                CreditCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Shield, contentDescription = null, tint = CreditRed)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(if (isId) "Pembayaran aman & terpercaya" else "Secure payment", color = NihongoPalette.white, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Text(if (isId) "Kredit masuk otomatis setelah pembayaran sukses diverifikasi server." else "Credits are added after the server verifies successful payment.", color = NihongoPalette.dim, fontSize = 11.sp)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
        if (loading && payload == null) NativeGlobalLoading(language = language, variant = "page", compact = true)
    }

    if (info) AlertDialog(onDismissRequest = { info = false }, title = { Text(if (isId) "Tentang pembelian kredit" else "About credit purchase") },
        text = { Text(if (isId) "Pilih paket, konfirmasi biaya QRIS, lalu selesaikan pembayaran. Simpan Order ID agar status dapat dicek kembali." else "Choose a package, confirm the QRIS fee, and complete the payment. Keep your Order ID to check its status.") },
        confirmButton = { TextButton(onClick = { info = false }) { Text("OK") } })
    selection?.let { choice ->
        AlertDialog(
            onDismissRequest = { if (!creating) selection = null },
            title = { Text(choice.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("${if (isId) "Harga dasar" else "Base price"}: ${creditMoney(choice.base)}")
                    Text("${if (isId) "Biaya QRIS" else "QRIS fee"}: ${creditMoney(choice.fee)}")
                    Text("${if (isId) "Estimasi total bayar" else "Estimated total payment"}: ${creditMoney(choice.base + choice.fee)}", fontWeight = FontWeight.Black)
                    Text(if (choice.premium) "Premium Materi tidak mengubah biaya Sensei AI." else if (isId) "Kredit yang diterima tetap penuh; biaya QRIS terpisah." else "Full credits are delivered; QRIS fee is separate.", color = NihongoPalette.dim, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(enabled = !creating, onClick = {
                creating = true
                scope.launch {
                    try {
                        val response = if (choice.premium) repository.createPremiumOrder(token, language) else repository.createCreditOrder(token, choice.packageId.orEmpty(), language)
                        val orderId = response.optJSONObject("order")?.optString("orderId").orEmpty()
                        if (orderId.isNotBlank()) { selection = null; onOrder(orderId) }
                        else { error = if (isId) "Order dibuat, tetapi Order ID tidak tersedia. Periksa riwayat." else "Order ID missing; check history."; selection = null; refresh++ }
                    } catch (e: Exception) { error = e.message ?: "Gagal membuat order"; selection = null }
                    finally { creating = false }
                }
            }) { Text(if (creating) "Memproses..." else if (isId) "Bayar Sekarang" else "Pay Now") } },
            dismissButton = { TextButton(enabled = !creating, onClick = { selection = null }) { Text(if (isId) "Batal" else "Cancel") } }
        )
    }
}

@Composable
private fun CreditPackageCard(pack: JSONObject, language: String, popular: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val isId = language == "id"
    Column(
        Modifier.fillMaxWidth().height(174.dp)
            .background(if (popular) NihongoPalette.surface2 else NihongoPalette.surface, RoundedCornerShape(23.dp))
            .border(1.dp, if (popular) CreditRed.copy(alpha = .6f) else NihongoPalette.borderStrong, RoundedCornerShape(23.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            if (popular) Text(if (pack.optString("badge").isNotBlank()) pack.optString("badge") else "POPULER", color = CreditGold, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = CreditGold, modifier = Modifier.padding(top = 5.dp))
            Text(creditCount(pack.optLong("credits")), color = NihongoPalette.white, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Text(if (isId) "Kredit" else "Credits", color = NihongoPalette.dim, fontSize = 11.sp)
        }
        Column {
            Text(creditMoney(pack.optLong("priceIdr")), color = if (popular) CreditRed else CreditGold, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(if (isId) "Bayar aman ›" else "Secure payment ›", color = if (enabled) NihongoPalette.dim else NihongoPalette.dim.copy(alpha = 0.4f), fontSize = 10.sp)
        }
    }
}
