package id.vixnihon.nativeapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.vixnihon.nativeapp.R
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.isActive

/**
 * Native Jetpack Compose implementation of client/src/components/GlobalLoading.jsx.
 * Animated elements use Compose/GPU transforms and a tiny Canvas; no WebView,
 * Filament instance, repeated image decode, networking or artificial blocking delay.
 * This screen is shown only while an actual bootstrap or initial page request is pending.
 */
@Composable
internal fun NativeGlobalLoading(
    language: String,
    variant: String = "page",
    compact: Boolean = false,
    message: String? = null,
    subMessage: String? = null,
    progress: Int? = null,
    modifier: Modifier = Modifier,
) {
    val isId = language == "id"
    val light = NihongoPalette.lightMode
    val bg = NihongoPalette.background
    val s1 = NihongoPalette.surface
    val s3 = NihongoPalette.surface3
    val white = NihongoPalette.white
    val dim = if (light) Color(0xFF6F7288) else NihongoPalette.dim
    val red = NihongoPalette.red
    val redLight = if (light) Color(0xFFD93B3B) else NihongoPalette.redLight
    val goldLight = if (light) Color(0xFFC9952E) else Color(0xFFDEB55A)
    val border = NihongoPalette.border
    val borderHi = NihongoPalette.borderStrong
    val isAccount = variant == "account" || variant == "auth"
    val boundedProgress = progress?.coerceIn(0, 100)
    val title = message ?: if (isAccount) {
        if (isId) "Memuat akun..." else "Loading account..."
    } else {
        if (isId) "Menyiapkan halaman..." else "Preparing page..."
    }
    val subtitle = subMessage ?: if (boundedProgress != null && boundedProgress >= 85) {
        if (isId) "Hampir selesai..." else "Almost there..."
    } else {
        if (isId) "Memuat..." else "Loading..."
    }

    // One Choreographer frame clock, independent of Android's animator-duration scale.
    // A 0x developer-option scale must not freeze the visible loading indicator.
    // Cap updates at ~30 fps to keep this short-lived overlay inexpensive.
    var elapsedNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var started = 0L
        var lastUpdate = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (started == 0L) started = now
                if (lastUpdate == 0L || now - lastUpdate >= 30_000_000L) {
                    elapsedNanos = (now - started).coerceAtLeast(0L)
                    lastUpdate = now
                }
            }
        }
    }
    val ms = elapsedNanos / 1_000_000.0
    val orbit = ((ms / 1_850.0) * 360.0 % 360.0).toFloat()
    val floatOffset = (sin(ms * 2.0 * PI / 2_800.0) * 4.0).toFloat()
    val shimmer = (((ms / 1_650.0) % 1.0) * 1.9 - 0.65).toFloat()
    val petalPhase = ((ms / 4_500.0) % 1.0).toFloat()
    val pulse = (0.85 + 0.15 * sin(ms * 2.0 * PI / 1_600.0)).toFloat()
    val localDensity = LocalDensity.current
    val floatPx = with(localDensity) { floatOffset.dp.toPx() }
    val markSize = if (compact) 108.dp else 128.dp
    val logoText = remember(redLight) {
        buildAnnotatedString {
            append("Nihon")
            withStyle(SpanStyle(color = redLight)) { append("Go") }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(bg, s1, bg)))
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Same three ambient color glows and Japanese-inspired ornaments as web.
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(redLight.copy(alpha = 0.13f), Color.Transparent),
                    center = Offset(w * 0.50f, h * 0.27f), radius = w * 0.48f
                ),
                center = Offset(w * 0.50f, h * 0.27f), radius = w * 0.48f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(goldLight.copy(alpha = 0.09f), Color.Transparent),
                    center = Offset(w * 0.16f, h * 0.80f), radius = w * 0.44f
                ),
                center = Offset(w * 0.16f, h * 0.80f), radius = w * 0.44f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(red.copy(alpha = 0.11f), Color.Transparent),
                    center = Offset(w * 0.93f, h * 0.16f), radius = w * 0.47f
                ),
                center = Offset(w * 0.93f, h * 0.16f), radius = w * 0.47f
            )
            // Avoid off-screen outlined rectangles: they appeared as a large broken
            // box on the left of the loader on narrow Android screens.
            // Low-opacity ripples along the bottom, echoing the web loader background.
            for (i in 0 until 4) {
                drawArc(
                    color = redLight.copy(alpha = 0.055f),
                    startAngle = 188f, sweepAngle = 164f, useCenter = false,
                    topLeft = Offset(w * 0.13f - i * 21f, h * 0.91f - i * 9f),
                    size = Size(w * 0.74f + i * 42f, h * 0.15f + i * 24f),
                    style = Stroke(width = 2f)
                )
            }
            // Single animation clock for all seven blossoms (no seven timers or Views).
            for (i in 0 until 7) {
                val phase = (petalPhase + i * 0.18f) % 1f
                val x = w * (0.12f + ((i * 17) % 76) / 100f) - phase * with(localDensity) { 34.dp.toPx() }
                val y = h * (0.12f + i * 0.11f) - with(localDensity) { 10.dp.toPx() } + phase * with(localDensity) { 60.dp.toPx() }
                val fade = (sin(phase.toDouble() * PI).toFloat()).coerceAtLeast(0f) * 0.55f
                rotate(i * 31f + phase * 78f, pivot = Offset(x, y)) {
                    drawOval(
                        color = (if (i % 2 == 0) Color(0xFFF7A0B6) else redLight).copy(alpha = fade),
                        topLeft = Offset(x - 4f, y - 3f), size = Size(9f + i % 3, 6f + i % 2)
                    )
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            val shortHeight = maxHeight < 610.dp
            Column(
                modifier = Modifier.fillMaxWidth(0.91f).widthIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = if (shortHeight) 10.dp else 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (compact || shortHeight) 12.dp else 17.dp)
            ) {
                // Web: 5px multicolor orbit, circular dark disc, original 128px logo,
                // gentle bobbing animation. Logo is bundled locally as WebP.
                Box(
                    modifier = Modifier.size(markSize + 32.dp)
                        .graphicsLayer { translationY = floatPx },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.fillMaxSize().graphicsLayer { rotationZ = orbit }
                            .border(5.dp, Brush.sweepGradient(listOf(redLight, goldLight, red.copy(alpha = 0.55f), borderHi, redLight)), CircleShape)
                    )
                    Box(
                        Modifier.size(markSize + 2.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(s1.copy(alpha = 0.94f), bg.copy(alpha = 0.75f))))
                            .border(1.dp, border, CircleShape)
                    )
                    Image(
                        painter = painterResource(R.drawable.loading_nihongo_logo),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(markSize)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = logoText, color = white, fontSize = if (compact) 38.sp else 44.sp,
                        lineHeight = if (compact) 39.sp else 45.sp,
                        letterSpacing = (-1.4).sp, fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(9.dp))
                    Text(
                        text = if (isId) "BELAJAR BAHASA JEPANG" else "LEARN JAPANESE",
                        color = dim, fontSize = if (compact) 11.sp else 12.sp,
                        letterSpacing = 2.6.sp, fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(if (compact) 18.dp else 24.dp))
                    Text(
                        text = "読み込み中…", color = redLight, fontWeight = FontWeight.Black,
                        fontSize = if (compact) 18.sp else 21.sp, letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = title, color = white, fontWeight = FontWeight.Black,
                        fontSize = if (compact) 14.sp else 15.sp,
                        textAlign = TextAlign.Center,
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 260.dp).height(8.dp)
                            .clip(CircleShape).background(s3).border(1.dp, border, CircleShape)
                    ) {
                        val progressWidthPx = with(localDensity) { maxWidth.toPx() }
                        val fill = boundedProgress?.let { it / 100f } ?: 0.54f
                        Box(
                            modifier = Modifier.fillMaxWidth(fill)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    translationX = if (boundedProgress == null) shimmer * progressWidthPx else 0f
                                    alpha = if (boundedProgress != null) pulse else 1f
                                }
                                .background(Brush.horizontalGradient(listOf(red, redLight, goldLight)), CircleShape)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = subtitle + (boundedProgress?.let { " $it%" } ?: ""),
                        color = dim, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                if (!compact) {
                    Column(
                        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().padding(top = 5.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            LoadingFeatureBadge(Icons.Outlined.VerifiedUser, if (isId) "Aman" else "Safe", redLight, dim, border, Modifier.weight(1f))
                            LoadingFeatureBadge(Icons.Outlined.Language, if (isId) "Multi Bahasa" else "Multi Language", redLight, dim, border, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            LoadingFeatureBadge(Icons.Outlined.Palette, if (isId) "Tema Otomatis" else "Auto Theme", redLight, dim, border, Modifier.weight(1f))
                            LoadingFeatureBadge(Icons.Outlined.Bolt, if (isId) "Ringan" else "Lightweight", redLight, dim, border, Modifier.weight(1f))
                        }
                    }
                }

                Box(
                    modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth()
                        .padding(top = if (compact) 8.dp else 12.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(white.copy(alpha = 0.035f))
                        .border(1.dp, border, RoundedCornerShape(18.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isId) "Konsisten belajar sedikit setiap hari adalah kunci kemahiran."
                        else "Small daily practice is the key to fluency.",
                        color = dim, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold,
                        lineHeight = 17.sp, textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingFeatureBadge(
    icon: ImageVector, label: String, highlight: Color, dim: Color, border: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clip(CircleShape)
            .background(Color.White.copy(alpha = 0.035f))
            .border(1.dp, border, CircleShape)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = highlight, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = dim, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
            maxLines = 1, softWrap = false)
    }
}
