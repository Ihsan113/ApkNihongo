package id.vixnihon.nativeapp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import id.vixnihon.nativeapp.data.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

@Composable
internal fun NativeCreditPaymentScreen(
    language: String, token: String, orderId: String, repository: AuthRepository,
    onBack: () -> Unit, onTopup: () -> Unit, onNotice: (String) -> Unit
) {
    val isId = language == "id"
    val context = LocalContext.current
    val narrowScreen = LocalConfiguration.current.screenWidthDp < 390
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    var active by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> active = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    var refresh by remember(orderId) { mutableIntStateOf(0) }
    var order by remember(orderId) { mutableStateOf<JSONObject?>(null) }
    var balance by remember(orderId) { mutableStateOf<JSONObject?>(null) }
    var loading by remember(orderId) { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember(orderId) { mutableStateOf("") }
    var feedback by remember(orderId) { mutableStateOf("") }
    var now by remember(orderId) { mutableStateOf(System.currentTimeMillis()) }
    var qrBitmap by remember(orderId) { mutableStateOf<Bitmap?>(null) }
    var qrBusy by remember(orderId) { mutableStateOf(false) }
    var qrError by remember(orderId) { mutableStateOf("") }
    var qrRetry by remember(orderId) { mutableIntStateOf(0) }
    var cancelConfirm by remember { mutableStateOf(false) }
    var help by remember { mutableStateOf(false) }

    val status = order?.optString("status").orEmpty()
    val pending = status == "pending"
    val isPremium = order?.optString("orderKind") == "premium_unlock"
    val canCancel = order?.optBoolean("canCancel", pending) == true
    val qrPayload = order?.optString("qrString").orEmpty()
    val hasQr = !order?.optString("qrImage").isNullOrBlank() || qrPayload.isNotBlank()
    val expiredAt = order?.optLong("expiredAt", 0L) ?: 0L
    val remaining = (if (expiredAt > 0L) expiredAt - now else order?.optLong("remainingMs", 0L) ?: 0L).coerceAtLeast(0L)

    BackHandler { if (!busy) onBack() }
    LaunchedEffect(token, orderId, language, refresh) {
        loading = order == null
        error = ""
        try {
            val response = repository.fetchCreditOrder(token, orderId, language)
            order = response.optJSONObject("order")
            balance = response.optJSONObject("balance")
            if (order == null) error = if (isId) "Detail order tidak tersedia." else "Order details unavailable."
            if (response.optString("warning").isNotBlank()) feedback = response.optString("warning")
        } catch (e: Exception) { error = e.message ?: if (isId) "Gagal memuat pembayaran." else "Failed to load payment." }
        finally { loading = false }
    }
    LaunchedEffect(orderId, pending, active) {
        if (!pending || !active) return@LaunchedEffect
        while (isActive) {
            delay(5_000L)
            try {
                val response = repository.fetchCreditOrder(token, orderId, language)
                order = response.optJSONObject("order") ?: order
                balance = response.optJSONObject("balance") ?: balance
            } catch (_: Exception) { /* Silent background poll: manual refresh remains available. */ }
        }
    }
    LaunchedEffect(orderId, pending, active) {
        if (!pending || !active) return@LaunchedEffect
        while (isActive) { now = System.currentTimeMillis(); delay(1_000L) }
    }
    LaunchedEffect(token, orderId, pending, hasQr, qrPayload, qrRetry) {
        qrBitmap = null; qrError = ""
        if (!pending || !hasQr) return@LaunchedEffect
        qrBusy = true
        try {
            // Prefer the server-proxied image; if the gateway uses SVG or is temporarily
            // unavailable, generate the same QRIS payload locally rather than a fake code.
            val fromServer = runCatching { repository.fetchCreditQrImage(token, orderId) }.getOrNull()
            val bitmap = withContext(Dispatchers.Default) {
                fromServer?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                    ?: qrPayload.takeIf { it.isNotBlank() }?.let(::bitmapFromQrPayload)
            } ?: throw IllegalStateException(if (isId) "Gambar QRIS tidak valid." else "Invalid QR image.")
            qrBitmap = bitmap
        } catch (e: Exception) { qrError = e.message ?: if (isId) "QRIS tidak bisa dimuat." else "QRIS could not load." }
        finally { qrBusy = false }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val bitmap = qrBitmap
        if (uri != null && bitmap != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                    } ?: throw IllegalStateException("Could not open selected file.")
                }
                onNotice(if (isId) "QRIS disimpan." else "QRIS saved.")
            } catch (e: Exception) { onNotice(e.message ?: "Gagal menyimpan QRIS") }
        }
    }
    fun copyOrderId() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("NihonGo Order ID", orderId))
        onNotice(if (isId) "Order ID disalin." else "Order ID copied.")
    }
    Box(Modifier.fillMaxSize().background(NihongoPalette.background)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { CreditHeader(if (isId) "Detail Pembayaran" else "Payment Details", if (isId) "Status QRIS dan pesanan" else "QRIS and order status", onBack = onBack, onInfo = { help = true }) }
            if (error.isNotBlank()) item {
                CreditNotice(error)
                CreditAction(if (isId) "Coba Lagi" else "Retry") { refresh++ }
            }
            if (order != null) {
                item {
                    CreditCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (status == "success") Icons.Outlined.CheckCircle else Icons.Outlined.QrCode, contentDescription = null, tint = creditStatusColor(status), modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (isId) "Status Pembayaran" else "Payment Status", color = NihongoPalette.dim, fontSize = 11.sp)
                                Text((if (isId) order?.optString("statusLabelId") else order?.optString("statusLabelEn")).orEmpty().ifBlank { creditStatus(status, language) }, color = creditStatusColor(status), fontSize = 16.sp, fontWeight = FontWeight.Black)
                                Text(orderId, color = NihongoPalette.dim, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp)
                            }
                        }
                        Text(creditMoney(order?.optLong("amountTotalIdr")?.takeIf { it > 0L } ?: order?.optLong("priceIdr") ?: 0L), color = NihongoPalette.white, fontWeight = FontWeight.Black, fontSize = 28.sp)
                        if (pending && expiredAt > 0L) {
                            val secs = remaining / 1000L
                            val timeText = if (secs >= 3_600) String.format(java.util.Locale.US, "%02d:%02d:%02d", secs / 3_600, (secs / 60) % 60, secs % 60)
                                else String.format(java.util.Locale.US, "%02d:%02d", secs / 60, secs % 60)
                            Text("${if (isId) "Sisa waktu" else "Time left"} • $timeText", color = CreditGold, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Shield, contentDescription = null, tint = CreditGreen)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isId) "Saldo dikreditkan otomatis setelah server memverifikasi pembayaran." else "Credits are added after server payment verification.", color = NihongoPalette.dim, fontSize = 11.sp)
                        }
                    }
                }
                if (pending) item {
                    CreditCard {
                        Text("QRIS Code", color = NihongoPalette.white, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text(if (isId) "Scan dengan e-wallet atau mobile banking." else "Scan with an e-wallet or mobile bank.", color = NihongoPalette.dim, fontSize = 11.sp)
                        if (qrBitmap != null) {
                            Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Box(Modifier.background(Color.White, RoundedCornerShape(18.dp)).padding(12.dp)) {
                                    Image(qrBitmap!!.asImageBitmap(), contentDescription = "QRIS untuk order $orderId", modifier = Modifier.size(240.dp))
                                }
                            }
                        } else {
                            Box(Modifier.fillMaxWidth().height(180.dp).background(NihongoPalette.surface2, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Outlined.QrCode, contentDescription = null, tint = CreditRed, modifier = Modifier.size(38.dp))
                                    Text(if (qrBusy) "Memuat QRIS..." else if (!hasQr) "QRIS belum tersedia" else qrError.ifBlank { "QRIS tidak tersedia" }, color = NihongoPalette.dim, fontSize = 11.sp, textAlign = TextAlign.Center)
                                    if (!qrBusy && hasQr) Text(if (isId) "Coba ulang" else "Retry", Modifier.clickable { qrRetry++ }, color = CreditRed, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                }
                            }
                        }
                        Text(if (isId) "QRIS berlaku untuk satu kali pembayaran." else "QRIS is valid for one payment only.", color = NihongoPalette.dim, fontSize = 11.sp)
                        CreditAction(if (isId) "Cek Status Pembayaran" else "Check Payment Status", enabled = !busy) { refresh++ }
                        if (narrowScreen) {
                            // Web switches to two columns on narrow handsets; don't squeeze three labels.
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MiniCreditAction(if (isId) "Salin ID" else "Copy ID", Modifier.weight(1f), enabled = !busy) { copyOrderId() }
                                MiniCreditAction(if (isId) "Unduh" else "Save QR", Modifier.weight(1f), enabled = !busy && qrBitmap != null) { saveLauncher.launch("NihonGo-QRIS-$orderId.png") }
                            }
                            if (canCancel) MiniCreditAction(if (isId) "Batalkan Order" else "Cancel Order", Modifier.fillMaxWidth(), enabled = !busy) { cancelConfirm = true }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MiniCreditAction(if (isId) "Salin ID" else "Copy ID", Modifier.weight(1f), enabled = !busy) { copyOrderId() }
                                MiniCreditAction(if (isId) "Unduh" else "Save QR", Modifier.weight(1f), enabled = !busy && qrBitmap != null) { saveLauncher.launch("NihonGo-QRIS-$orderId.png") }
                                if (canCancel) MiniCreditAction(if (isId) "Batalkan" else "Cancel", Modifier.weight(1f), enabled = !busy) { cancelConfirm = true }
                            }
                        }
                    }
                } else item {
                    CreditCard {
                        Icon(if (status == "success") Icons.Outlined.CheckCircle else Icons.Outlined.Warning, contentDescription = null, tint = creditStatusColor(status), modifier = Modifier.size(46.dp))
                        Text(if (status == "success") { if (isId) "Pembayaran berhasil" else "Payment successful" } else { if (isId) "Pembayaran tidak aktif" else "Payment inactive" }, color = NihongoPalette.white, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        if (status == "success") {
                            if (isPremium) Text(if (isId) "Premium Materi telah diaktifkan server." else "Premium Materi has been activated by the server.", color = CreditGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            else Text("+${creditCount(order?.optLong("credits") ?: 0L)} ${if (isId) "Kredit" else "Credits"}", color = CreditGreen, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                        Text(when {
                            status == "success" && isPremium -> if (isId) "Akses materi premium dikonfirmasi server." else "Premium access was confirmed by the server."
                            status == "success" -> if (isId) "Kredit sudah diproses server." else "Credits were processed by the server."
                            else -> if (isId) "Order telah selesai, dibatalkan, atau kedaluwarsa." else "This order is no longer payable."
                        }, color = NihongoPalette.dim, fontSize = 12.sp)
                    }
                }
                item {
                    CreditCard {
                        Text(if (isId) "Detail Transaksi" else "Transaction Details", color = NihongoPalette.white, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        PaymentDetailRow(if (isId) "Dibuat" else "Created", creditDate(order?.optLong("createdAt") ?: 0L, language))
                        PaymentDetailRow(if (isId) "Kedaluwarsa" else "Expires", creditDate(expiredAt, language))
                        PaymentDetailRow(if (isId) "Metode" else "Method", "QRIS")
                        PaymentDetailRow("Status", creditStatus(status, language))
                        PaymentDetailRow("Order ID", orderId)
                        order?.optString("reference")?.takeIf { it.isNotBlank() }?.let { PaymentDetailRow("Ref", it) }
                        balance?.let { PaymentDetailRow(if (isId) "Saldo sekarang" else "Current balance", creditCount(it.optLong("creditBalance"))) }
                        MiniCreditAction(if (isId) "Salin Order ID" else "Copy Order ID", Modifier.fillMaxWidth()) { copyOrderId() }
                    }
                }
                item { CreditAction(if (isId) "Buat Pembayaran Baru" else "Create New Payment") { onTopup() } }
            }
            if (feedback.isNotBlank()) item { CreditNotice(feedback) }
            item { Spacer(Modifier.height(26.dp)) }
        }
        if (loading && order == null) NativeGlobalLoading(language = language, variant = "page", compact = true)
    }
    if (cancelConfirm) AlertDialog(onDismissRequest = { if (!busy) cancelConfirm = false },
        title = { Text(if (isId) "Batalkan pembayaran?" else "Cancel payment?") },
        text = { Text(if (isId) "Order yang dibatalkan tidak bisa dibayar lagi. Pastikan kamu belum melakukan pembayaran." else "You cannot pay a canceled order. Make sure you haven't paid yet.") },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            busy = true
            scope.launch {
                try {
                    val response = repository.cancelCreditOrder(token, orderId, language)
                    order = response.optJSONObject("order") ?: order
                    balance = response.optJSONObject("balance") ?: balance
                    feedback = if (isId) "Pembayaran dibatalkan." else "Payment canceled."
                    cancelConfirm = false
                } catch (e: Exception) { error = e.message ?: "Gagal membatalkan pembayaran"; cancelConfirm = false }
                finally { busy = false }
            }
        }) { Text(if (busy) "Memproses" else if (isId) "Ya, batalkan" else "Cancel order") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { cancelConfirm = false }) { Text(if (isId) "Kembali" else "Go back") } })
    if (help) AlertDialog(onDismissRequest = { help = false }, title = { Text("Bantuan QRIS") },
        text = { Text(if (isId) "Simpan QRIS atau scan menggunakan aplikasi pembayaran. Status diperbarui otomatis setiap 5 detik saat layar terbuka. Jangan bayar order yang dibatalkan/kedaluwarsa." else "Save or scan the QRIS with a payment app. Status updates every 5 seconds while the screen is open. Do not pay expired/canceled orders.") },
        confirmButton = { TextButton(onClick = { help = false }) { Text("OK") } })
}

@Composable
private fun MiniCreditAction(label: String, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(modifier.background(NihongoPalette.surface2, RoundedCornerShape(14.dp)).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 8.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (enabled) NihongoPalette.white else NihongoPalette.dim, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun PaymentDetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(0.8f), color = NihongoPalette.dim, fontSize = 11.sp)
        Text(value, Modifier.weight(1.2f), color = NihongoPalette.white, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** Generate from the actual server-provided EMV QRIS string, never invent payment payloads. */
private fun bitmapFromQrPayload(payload: String): Bitmap {
    val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 680, 680, mapOf(EncodeHintType.MARGIN to 2))
    val width = matrix.width
    val height = matrix.height
    val pixels = IntArray(width * height) { index ->
        if (matrix.get(index % width, index / width)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}
