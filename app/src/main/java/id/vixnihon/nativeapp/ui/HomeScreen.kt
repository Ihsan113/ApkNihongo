package id.vixnihon.nativeapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.vixnihon.nativeapp.R
import id.vixnihon.nativeapp.data.AppUser
import java.util.Calendar

@Composable
fun HomeScreen(
    user: AppUser,
    language: String,
    onToggleLanguage: () -> Unit,
    onLogout: () -> Unit,
    onNotice: (String) -> Unit
) {
    val isId = language == "id"
    var showSettings by remember { mutableStateOf(false) }
    val firstName = remember(user.name, user.email) {
        (user.name.ifBlank { user.email.substringBefore('@') })
            .trim().split(Regex("\\s+")).firstOrNull().orEmpty().ifBlank { "Sensei" }
    }
    val clock = Calendar.getInstance()
    val minuteOfDay = clock.get(Calendar.HOUR_OF_DAY) * 60 + clock.get(Calendar.MINUTE)
    val heroDrawable = when (minuteOfDay) {
        in 270..359 -> R.drawable.home_hero_fajar          // 04:30–05:59
        in 360..599 -> R.drawable.home_hero_pagi           // 06:00–09:59
        in 600..899 -> R.drawable.home_hero_siang          // 10:00–14:59
        in 900..1049 -> R.drawable.home_hero_sore          // 15:00–17:29
        in 1050..1139 -> R.drawable.home_hero_senja        // 17:30–18:59
        in 1140..1379 -> R.drawable.home_hero_malam        // 19:00–22:59
        else -> R.drawable.home_hero_tengah_malam          // 23:00–04:29
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NihongoPalette.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 22.dp)
        ) {
            item {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(506.dp)) {
                        HomeHero(
                            user = user,
                            firstName = firstName,
                            isId = isId,
                            heroDrawable = heroDrawable,
                            onSettings = { showSettings = true }
                        )
                        ContinueLearningCard(
                            isId = isId,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = 35.dp)
                                .padding(horizontal = 18.dp),
                            onClick = {
                                onNotice(if (isId) "Halaman materi akan disambungkan pada tahap berikutnya." else "Lessons will be connected in the next phase.")
                            }
                        )
                    }
                    Spacer(Modifier.height(57.dp))
                }
            }
            item {
                SectionHeading(
                    title = if (isId) "Jelajahi Nihongo" else "Explore Nihongo",
                    subtitle = if (isId) "Pilih cara belajar yang kamu suka" else "Choose how you want to study",
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                Spacer(Modifier.height(14.dp))
            }
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FeatureCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.School,
                            title = if (isId) "Belajar" else "Learn",
                            detail = if (isId) "Materi bertahap" else "Guided lessons",
                            accent = Color(0xFFE03535),
                            onClick = { onNotice(if (isId) "Menu Belajar disiapkan pada tahap berikutnya." else "Learn screen is coming in the next phase.") }
                        )
                        FeatureCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Science,
                            title = if (isId) "Flashcard" else "Flashcards",
                            detail = if (isId) "Kosakata Jepang" else "Japanese words",
                            accent = Color(0xFF9E78FF),
                            onClick = { onNotice(if (isId) "Menu Flashcard disiapkan pada tahap berikutnya." else "Flashcards are coming in the next phase.") }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FeatureCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.SportsEsports,
                            title = if (isId) "Kuis" else "Quiz",
                            detail = if (isId) "Uji pemahaman" else "Test your skills",
                            accent = Color(0xFFDEB55A),
                            onClick = { onNotice(if (isId) "Menu Kuis disiapkan pada tahap berikutnya." else "Quiz is coming in the next phase.") }
                        )
                        FeatureCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.ChatBubbleOutline,
                            title = "Sensei AI",
                            detail = if (isId) "Teman latihan" else "Your study partner",
                            accent = Color(0xFF3D7EEE),
                            onClick = { onNotice(if (isId) "Sensei AI disambungkan pada tahap berikutnya." else "Sensei AI is coming in the next phase.") }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            item {
                LearningSummaryCard(isId = isId, modifier = Modifier.padding(horizontal = 18.dp))
            }
        }

        NativeBottomNav(
            isId = isId,
            onSelect = { key ->
                if (key != "home") {
                    val label = when (key) {
                        "learn" -> if (isId) "Belajar" else "Learn"
                        "flashcard" -> "Flashcard"
                        "quiz" -> if (isId) "Kuis" else "Quiz"
                        else -> "Sensei"
                    }
                    onNotice(if (isId) "Menu $label akan diaktifkan pada tahap berikutnya." else "$label will be enabled in the next phase.")
                }
            }
        )
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            containerColor = NihongoPalette.surface2,
            title = {
                Text(
                    text = if (isId) "Pengaturan akun" else "Account settings",
                    color = NihongoPalette.white,
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Column {
                    Text(user.name.ifBlank { "NihonGo user" }, color = NihongoPalette.white, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(user.email, color = NihongoPalette.dim2, fontSize = 12.sp)
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = if (isId) "Bahasa aplikasi" else "App language",
                        color = NihongoPalette.dim,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LanguageChoice("Indonesia", selected = isId, onClick = { if (!isId) onToggleLanguage() }, modifier = Modifier.weight(1f))
                        LanguageChoice("English", selected = !isId, onClick = { if (isId) onToggleLanguage() }, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text(if (isId) "Selesai" else "Done", color = NihongoPalette.redSoft, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false; onLogout() }) {
                    Text(if (isId) "Keluar" else "Sign out", color = Color(0xFFFF8585), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun HomeHero(
    user: AppUser,
    firstName: String,
    isId: Boolean,
    heroDrawable: Int,
    onSettings: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(heroDrawable),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color(0x9E05050B),
                            0.25f to Color(0x6505050B),
                            0.53f to Color(0x3405050B),
                            0.77f to Color(0xB807070D),
                            1f to NihongoPalette.background
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x23E03535), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(780f, 180f),
                        radius = 620f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(top = 23.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "日本語",
                        color = Color.White,
                        fontSize = 42.sp,
                        lineHeight = 45.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    )
                    Text(
                        text = if (isId) "Nihongo · Belajar Bahasa Jepang" else "Nihongo · Learn Japanese",
                        color = Color(0xD7FFFFFF),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = if (isId) "Pengaturan" else "Settings",
                        tint = Color(0xD7FFFFFF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("✿", color = NihongoPalette.redLight, fontSize = 19.sp, lineHeight = 25.sp)
                Column(modifier = Modifier.fillMaxWidth(0.76f)) {
                    Text(
                        text = if (isId) "Selamat datang kembali, $firstName. Ayo lanjut belajar hari ini." else "Welcome back, $firstName. Let’s continue learning today.",
                        color = Color.White,
                        fontSize = 16.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = if (isId) "Pilih materi, latihan, atau tanya Sensei kapan saja." else "Choose lessons, practice, or ask Sensei anytime.",
                        color = Color(0xD7FFFFFF),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            Surface(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = 82.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0x8B090A12),
                border = BorderStroke(1.dp, Color(0x24FFFFFF))
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text("今日も一緒に頑張ろう！", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${user.name.ifBlank { "NihonGo" }} · 日本語",
                        color = Color(0xBFFFFFFF),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueLearningCard(isId: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xF51A1118),
        border = BorderStroke(1.dp, Color(0x5AE03535)),
        shadowElevation = 18.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x19E03535))
                        .border(1.dp, Color(0x45E03535), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("N5", color = NihongoPalette.redSoft, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(if (isId) "Dasar" else "Basic", color = NihongoPalette.dim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isId) "Lanjutkan Belajar" else "Continue Learning",
                        color = NihongoPalette.redSoft,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isId) "Salam dan Perkenalan" else "Greetings and Introductions",
                        color = NihongoPalette.white,
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isId) "Materi awal · JLPT N5" else "Starter lesson · JLPT N5",
                        color = NihongoPalette.dim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color(0x24FFFFFF))
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("0%", color = NihongoPalette.dim, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
            Spacer(Modifier.height(13.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isId) "0 dari 12 materi selesai" else "0 of 12 lessons complete",
                    color = NihongoPalette.dim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
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
private fun SectionHeading(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(title, color = NihongoPalette.white, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = NihongoPalette.dim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FeatureCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    detail: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = NihongoPalette.surface,
        border = BorderStroke(1.dp, NihongoPalette.border),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.13f))
                    .border(1.dp, accent.copy(alpha = 0.26f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.height(13.dp))
            Text(title, color = NihongoPalette.white, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(detail, color = NihongoPalette.dim, fontSize = 10.5.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LearningSummaryCard(isId: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = NihongoPalette.surface,
        border = BorderStroke(1.dp, NihongoPalette.border)
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Star, contentDescription = null, tint = NihongoPalette.gold, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(9.dp))
                Text(if (isId) "Ringkasan belajar" else "Learning summary", color = NihongoPalette.white, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(15.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    value = "0 XP",
                    label = if (isId) "Total XP" else "Total XP",
                    valueColor = Color(0xFFDEB55A)
                )
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    value = "0",
                    label = if (isId) "Hari beruntun" else "Day streak",
                    valueColor = NihongoPalette.redSoft
                )
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    value = "N5",
                    label = if (isId) "Level aktif" else "Active level",
                    valueColor = Color(0xFF86A8FF)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (isId) "Statistik lengkap akan tersambung saat modul belajar diaktifkan." else "Full statistics will connect when the study modules are enabled.",
                color = NihongoPalette.dim,
                fontSize = 10.5.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun SummaryMetric(modifier: Modifier, value: String, label: String, valueColor: Color) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x0FFFFFFF))
            .padding(horizontal = 9.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(value, color = valueColor, fontSize = 19.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(4.dp))
        Text(label, color = NihongoPalette.dim, fontSize = 9.5.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NativeBottomNav(isId: Boolean, onSelect: (String) -> Unit) {
    val tabs = listOf(
        Triple("home", if (isId) "Beranda" else "Home", Icons.Filled.Home),
        Triple("learn", if (isId) "Belajar" else "Learn", Icons.Outlined.School),
        Triple("flashcard", if (isId) "Kartu" else "Cards", Icons.Outlined.Science),
        Triple("quiz", if (isId) "Kuis" else "Quiz", Icons.Outlined.SportsEsports),
        Triple("sensei", "Sensei", Icons.Outlined.ChatBubbleOutline)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NihongoPalette.surface)
            .border(BorderStroke(1.dp, Color(0x12FFFFFF)))
            .navigationBarsPadding()
            .padding(horizontal = 4.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { (key, label, icon) ->
            val active = key == "home"
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(key) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (active) NihongoPalette.redLight else NihongoPalette.dim,
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = label,
                    color = if (active) NihongoPalette.redLight else NihongoPalette.dim,
                    fontSize = 9.sp,
                    fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LanguageChoice(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0x28E03535) else NihongoPalette.surface3)
            .border(1.dp, if (selected) Color(0x68E03535) else NihongoPalette.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) NihongoPalette.redSoft else NihongoPalette.dim2, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

