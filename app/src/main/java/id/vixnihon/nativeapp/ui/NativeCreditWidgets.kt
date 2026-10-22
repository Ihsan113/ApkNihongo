package id.vixnihon.nativeapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val CreditGold = Color(0xFFF1C35D)
internal val CreditBlue = Color(0xFF63B3FF)
internal val CreditRed = Color(0xFFFF575C)
internal val CreditGreen = Color(0xFF3BC787)

internal fun creditMoney(n: Long): String = "Rp" + NumberFormat.getNumberInstance(Locale("id", "ID")).format(n)
internal fun creditCount(n: Long): String = when {
    n >= 10_000_000 -> "${n / 1_000_000}jt"
    n >= 1_000_000 -> String.format(Locale.US, "%.1fjt", n / 1_000_000.0)
    n >= 10_000 -> "${n / 1_000}rb"
    n >= 1_000 -> String.format(Locale.US, "%.1frb", n / 1_000.0)
    else -> n.toString()
}
internal fun creditDate(epochMillis: Long, language: String): String {
    if (epochMillis <= 0L) return "-"
    return SimpleDateFormat("dd MMM, HH:mm", if (language == "en") Locale.US else Locale("id", "ID")).format(Date(epochMillis))
}
internal fun creditStatus(status: String, language: String): String = when (status.lowercase(Locale.ROOT)) {
    "pending" -> if (language == "id") "Menunggu pembayaran" else "Waiting for payment"
    "success" -> if (language == "id") "Berhasil" else "Success"
    "failed" -> if (language == "id") "Gagal" else "Failed"
    "expired" -> if (language == "id") "Kedaluwarsa" else "Expired"
    "cancelled", "canceled" -> if (language == "id") "Dibatalkan" else "Canceled"
    else -> status.ifBlank { "-" }
}
internal fun creditStatusColor(status: String): Color = when (status) {
    "success" -> CreditGreen
    "pending" -> CreditGold
    else -> CreditRed
}

@Composable
internal fun CreditCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        color = NihongoPalette.surface,
        border = BorderStroke(1.dp, NihongoPalette.borderStrong)
    ) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() } }
}

@Composable
internal fun CreditHeader(title: String, subtitle: String, onBack: () -> Unit, onInfo: (() -> Unit)? = null, trailingRefresh: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.size(42.dp).background(NihongoPalette.surface, RoundedCornerShape(15.dp))
                .border(1.dp, NihongoPalette.borderStrong, RoundedCornerShape(15.dp)).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Kembali", tint = NihongoPalette.white) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = NihongoPalette.white, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = NihongoPalette.dim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.size(42.dp).background(NihongoPalette.surface, RoundedCornerShape(15.dp))
                .border(1.dp, NihongoPalette.borderStrong, RoundedCornerShape(15.dp))
                .clickable(enabled = onInfo != null) { onInfo?.invoke() }, contentAlignment = Alignment.Center
        ) { Icon(if (trailingRefresh) Icons.Outlined.Refresh else Icons.Outlined.Info, contentDescription = if (trailingRefresh) "Reset" else "Info", tint = NihongoPalette.dim) }
    }
}

@Composable
internal fun CreditPill(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(50),
        color = if (selected) NihongoPalette.red else NihongoPalette.surface2,
        border = BorderStroke(1.dp, if (selected) CreditRed else NihongoPalette.borderStrong)
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) Color.White else NihongoPalette.dim, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
internal fun CreditAction(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick), shape = RoundedCornerShape(16.dp), color = if (enabled) NihongoPalette.red else NihongoPalette.surface3) {
        Box(Modifier.padding(13.dp), contentAlignment = Alignment.Center) { Text(label, color = if (enabled) Color.White else NihongoPalette.dim, fontSize = 13.sp, fontWeight = FontWeight.Black) }
    }
}

@Composable
internal fun CreditNotice(message: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), color = CreditRed.copy(alpha = 0.10f), border = BorderStroke(1.dp, CreditRed.copy(alpha = 0.35f))) {
        Text(message, Modifier.padding(12.dp), fontSize = 12.sp, color = CreditRed, lineHeight = 18.sp)
    }
}
