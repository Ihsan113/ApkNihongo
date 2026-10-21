package id.vixnihon.nativeapp.ui

import androidx.activity.compose.BackHandler
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.vixnihon.nativeapp.data.AppUser
import id.vixnihon.nativeapp.data.AuthRepository
import kotlinx.coroutines.launch
import java.util.Locale

/** Settings mirrors the WEB user settings hierarchy. Intentionally excludes the admin dashboard. */
@Composable
internal fun NativeSettingsScreen(
    user: AppUser,
    language: String,
    preferences: NativeUserPreferences,
    onPreferencesChange: (NativeUserPreferences) -> Unit,
    onToggleLanguage: () -> Unit,
    repository: AuthRepository,
    token: String,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNotice: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val id = language == "id"
    var page by remember { mutableStateOf("main") }
    var rating by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var deleteText by remember { mutableStateOf("") }
    var clearConfirm by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val support = "danzku290906@gmail.com"
    fun choose(next: String) { page = next }
    val title = mapOf(
        "main" to (if (id) "Pengaturan" else "Settings"),
        "account" to (if (id) "Akun" else "Account"),
        "tts" to (if (id) "Suara & Mic" else "Voice & Mic"),
        "avatar" to "Avatar", "theme" to (if (id) "Tema" else "Theme"),
        "language" to (if (id) "Bahasa Sistem" else "System Language"),
        "storage" to (if (id) "Kelola Penyimpanan" else "Manage Storage"),
        "feedback" to (if (id) "Kirim Saran" else "Send Feedback"),
        "about" to (if (id) "Tentang Kami" else "About Us"),
        "faq" to "FAQ", "support" to (if (id) "Kontak Bantuan" else "Support Contact"),
        "privacy" to (if (id) "Kebijakan Privasi" else "Privacy Policy"),
        "terms" to (if (id) "Syarat & Ketentuan" else "Terms & Conditions"),
        "delete" to (if (id) "Hapus Akun" else "Delete Account")
    )[page].orEmpty()
    val desc = mapOf(
        "main" to (if (id) "Akun, suara, mic, avatar, tema, dan bahasa" else "Account, voice, mic, avatar, theme, and language"),
        "account" to (if (id) "Profil Google dan keluar akun" else "Google profile and sign out"),
        "tts" to (if (id) "Atur suara Sensei dan mic premium" else "Adjust Sensei voice and premium mic"),
        "avatar" to (if (id) "Pilih model Sensei" else "Choose Sensei model"),
        "theme" to (if (id) "Gelap, terang, atau mengikuti sistem" else "Dark, light, or follow system"),
        "language" to (if (id) "Bahasa antarmuka aplikasi" else "App interface language")
    )[page].orEmpty()
    val back = { if (page == "main") onBack() else page = "main" }
    BackHandler { back() }

    Column(Modifier.fillMaxSize().background(NihongoPalette.background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Kembali",
                tint = NihongoPalette.white,
                modifier = Modifier.size(32.dp).clickable { back() }.padding(3.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.Black, color = NihongoPalette.white)
                if (desc.isNotEmpty()) Text(desc, fontSize = 11.sp, color = NihongoPalette.dim, maxLines = 2)
            }
        }
        HorizontalDivider(color = NihongoPalette.border)
        LazyColumn(
            Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (page) {
                "main" -> {
                    item { NativeSettingsMenu(Icons.Outlined.AccountCircle, user.name.ifBlank { "User" }, user.email, "", { choose("account") }) }
                    item { NativeSettingsMenu(Icons.Outlined.VolumeUp, if (id) "Suara & Mic" else "Voice & Mic", if (id) "Suara Sensei dan mic premium" else "Sensei voice and premium mic", "${String.format(Locale.US, "%.2f", preferences.speechRate)}x", { choose("tts") }) }
                    item { NativeSettingsMenu(Icons.Outlined.Face, "Avatar", if (id) "Pilih avatar Sensei" else "Choose Sensei avatar", "waifu", { choose("avatar") }) }
                    item { NativeSettingsMenu(Icons.Outlined.Palette, if (id) "Tema" else "Theme", if (id) "Default sistem, terang atau gelap" else "System, light or dark", preferences.theme, { choose("theme") }) }
                    item { NativeSettingsMenu(Icons.Outlined.Language, if (id) "Bahasa Sistem" else "System Language", if (id) "Indonesia Japan atau English Japan" else "Indonesia Japan or English Japan", language.uppercase(), { choose("language") }) }
                    item { NativeSettingsMenu(Icons.Outlined.Storage, if (id) "Kelola Penyimpanan" else "Manage Storage", if (id) "Lihat & hapus cache sementara" else "View & clear temporary cache", "", { choose("storage") }) }
                    item { NativeSettingsMenu(Icons.Outlined.RateReview, if (id) "Kirim Saran" else "Send Feedback", if (id) "Kirim pengalaman dan masukan" else "Send your feedback", "", { choose("feedback") }) }
                    item { Spacer(Modifier.height(6.dp)); SettingsSectionTitle(if (id) "BANTUAN & LEGAL" else "HELP & LEGAL") }
                    item { NativeSettingsMenu(Icons.Outlined.Info, if (id) "Tentang Nihongo App" else "About Nihongo App", "Nihongo · DanzKu", "", { choose("about") }) }
                    item { NativeSettingsMenu(Icons.Outlined.HelpOutline, "FAQ", if (id) "Pertanyaan umum" else "Common questions", "", { choose("faq") }) }
                    item { NativeSettingsMenu(Icons.Outlined.MailOutline, if (id) "Kontak Bantuan" else "Support Contact", support, "", { choose("support") }) }
                    item { NativeSettingsMenu(Icons.Outlined.PrivacyTip, if (id) "Kebijakan Privasi" else "Privacy Policy", if (id) "Data akun dan penggunaan layanan" else "Account and service data", "", { choose("privacy") }) }
                    item { NativeSettingsMenu(Icons.Outlined.Description, if (id) "Syarat & Ketentuan" else "Terms & Conditions", if (id) "Aturan kredit, belajar dan AI" else "Credits, learning and AI rules", "", { choose("terms") }) }
                    item { NativeSettingsMenu(Icons.Outlined.DeleteForever, if (id) "Hapus Akun & Data" else "Delete Account & Data", if (id) "Butuh konfirmasi manual" else "Manual confirmation required", "", { choose("delete") }) }
                }
                "account" -> {
                    item { SettingsSectionTitle(if (id) "PROFIL GOOGLE" else "GOOGLE PROFILE") }
                    item { SettingsContentCard { Text(user.name.ifBlank { "User" }, color = NihongoPalette.white, fontWeight = FontWeight.Black); Text(user.email, color = NihongoPalette.dim, fontSize = 12.sp); Spacer(Modifier.height(12.dp)); Text(if (id) "Akun ini tersinkron dengan server Nihongo." else "This account is synced with Nihongo.", color = NihongoPalette.dim, fontSize = 12.sp) } }
                    item { SettingsActionButton(if (id) "Keluar dari akun" else "Sign out", danger = true) { showLogoutDialog = true } }
                }
                "tts" -> {
                    item { SettingsSectionTitle(if (id) "SUARA SENSEI" else "SENSEI VOICE") }
                    item { SettingsContentCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(if (id) "Sapaan suara otomatis" else "Automatic greeting", fontWeight = FontWeight.Black, color = NihongoPalette.white, fontSize = 14.sp)
                                Text(if (id) "Sesuai web: sapaan Jepang, lalu ucapan ringan setiap 10–15 detik saat Home aktif." else "Like web: Japanese greeting, then short phrases every 10–15 seconds while Home is active.", color = NihongoPalette.dim, fontSize = 12.sp)
                            }
                            Switch(checked = preferences.speechEnabled, onCheckedChange = { onPreferencesChange(preferences.copy(speechEnabled = it)) })
                        }
                        Spacer(Modifier.height(12.dp))
                        SettingsFloatSlider(if (id) "Kecepatan baca" else "Reading speed", preferences.speechRate, 0.75f, 1.25f) { onPreferencesChange(preferences.copy(speechRate = it)) }
                        SettingsFloatSlider(if (id) "Nada suara" else "Voice pitch", preferences.speechPitch, 0.8f, 1.3f) { onPreferencesChange(preferences.copy(speechPitch = it)) }
                        SettingsFloatSlider(if (id) "Volume suara" else "Voice volume", preferences.speechVolume, 0f, 1f) { onPreferencesChange(preferences.copy(speechVolume = it)) }
                        Text(if (id) "Memakai Text-to-speech Android. Untuk suara Jepang, instal data suara Japanese di pengaturan HP jika belum ada." else "Uses Android Text-to-speech; install a Japanese voice in your device settings if needed.", color = NihongoPalette.dim, fontSize = 11.sp)
                    } }
                    item { SettingsSectionTitle(if (id) "MIKROFON" else "MICROPHONE") }
                    item { SettingsInfo("Mic Premium", if (id) "Web memakai Groq Whisper untuk input suara. Fitur percakapan mic native akan menyusul di layar Sensei; belum aktif dari halaman Pengaturan." else "The web uses Groq Whisper. Native microphone conversation is not yet available in Settings.") }
                }
                "avatar" -> {
                    item { SettingsSectionTitle(if (id) "PILIH AVATAR" else "CHOOSE AVATAR") }
                    item { SettingsContentCard {
                        Text("waifu.vrm", fontWeight = FontWeight.Black, color = NihongoPalette.white)
                        Spacer(Modifier.height(6.dp))
                        Text(if (id) "Avatar aktif, sudah tersedia offline sebagai model GLB asli di aplikasi. Hanya avatar ini yang terpaket di APK sehingga opsi lain belum ditampilkan." else "Active avatar, bundled offline as a GLB. Other avatars are not bundled in the native app yet.", fontSize = 12.sp, color = NihongoPalette.dim)
                    } }
                }
                "theme" -> {
                    item { SettingsSectionTitle(if (id) "PILIHAN TEMA" else "THEME OPTIONS") }
                    items(3) { idx ->
                        val modes = listOf("system", "light", "dark")
                        val values = if (id) listOf("Mengikuti sistem HP", "Terang", "Gelap") else listOf("Follow device", "Light", "Dark")
                        val current = modes[idx]
                        SettingsChoice(values[idx], preferences.theme == current) { onPreferencesChange(preferences.copy(theme = current)) }
                    }
                    item { SettingsInfo(if (id) "Tema aplikasi" else "Application theme", if (id) "Tema warna halaman native diperbarui. Background Home tetap berubah mengikuti jam, seperti di web." else "Native screen colors are updated. Home's background still follows time of day, like the web.") }
                }
                "language" -> {
                    item { SettingsSectionTitle(if (id) "PILIHAN BAHASA" else "LANGUAGE OPTIONS") }
                    item { SettingsChoice("Indonesia Japan", id) { if (!id) onToggleLanguage() } }
                    item { SettingsChoice("English Japan", !id) { if (id) onToggleLanguage() } }
                }
                "storage" -> {
                    item { SettingsSectionTitle(if (id) "PENYIMPANAN LOKAL" else "LOCAL STORAGE") }
                    item { SettingsInfo(if (id) "Aset offline" else "Offline assets", if (id) "Model 3D, latar dan metadata materi dibundel dalam APK. Menghapus cache tidak menghapus aset bawaan atau progres server." else "3D models, backgrounds and lesson metadata are bundled with the APK. Cache clearing does not delete bundled files or server progress.") }
                    item { SettingsActionButton(if (id) "Hapus Cache Sementara" else "Clear Temporary Cache", danger = clearConfirm) {
                        if (!clearConfirm) clearConfirm = true
                        else {
                            val result = runCatching { context.cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
                            clearConfirm = false
                            onNotice(if (result.isSuccess) (if (id) "Cache sementara sudah dibersihkan." else "Temporary cache cleared.") else (if (id) "Gagal menghapus cache." else "Failed to clear cache."))
                        }
                    } }
                    if (clearConfirm) item { Text(if (id) "Ketuk lagi untuk konfirmasi. Data akun tidak terhapus." else "Tap again to confirm. Your account data is safe.", color = NihongoPalette.redSoft, fontSize = 12.sp) }
                }
                "feedback" -> {
                    item { SettingsSectionTitle(if (id) "KIRIM SARAN" else "SEND FEEDBACK") }
                    item { SettingsContentCard {
                        Text(if (id) "Beri penilaian" else "Rate your experience", color = NihongoPalette.white, fontWeight = FontWeight.Bold)
                        Row { (1..5).forEach { star -> Text(if (star <= rating) "★" else "☆", color = NihongoPalette.gold, fontSize = 32.sp, modifier = Modifier.clickable { rating = star }.padding(end = 10.dp)) } }
                        OutlinedTextField(value = message, onValueChange = { message = it.take(2000) }, label = { Text(if (id) "Saran (opsional)" else "Feedback (optional)") }, minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = NihongoPalette.white, unfocusedTextColor = NihongoPalette.white))
                    } }
                    item { SettingsActionButton(if (busy) (if (id) "Mengirim..." else "Sending...") else if (id) "Kirim Saran" else "Send Feedback", enabled = !busy && (rating > 0 || message.isNotBlank())) {
                        busy = true
                        scope.launch {
                            runCatching { repository.sendFeedback(token, rating.takeIf { it > 0 }, message.trim()) }
                                .onSuccess { onNotice(if (id) "Terima kasih, saran terkirim." else "Thank you! Feedback sent."); rating = 0; message = "" }
                                .onFailure { onNotice(it.message ?: "Gagal mengirim saran") }
                            busy = false
                        }
                    } }
                }
                "about" -> {
                    item { SettingsInfo("Nihongo App", if (id) "Aplikasi belajar bahasa Jepang bertahap: materi, kuis, review, flashcard, dan bantuan Sensei." else "Step-by-step Japanese learning app with lessons, quizzes, flashcards and Sensei.") }
                    item { SettingsInfo("Developer", "Brand: DanzKu\nVersi native: 0.1.15\nNihongo bukan aplikasi resmi JLPT atau Japan Foundation.") }
                }
                "faq" -> {
                    item { SettingsInfo(if (id) "Apakah kredit permanen?" else "Are credits permanent?", if (id) "Ya, selama akun aktif dan tidak dihapus." else "Yes, while your account is active.") }
                    item { SettingsInfo(if (id) "Apakah unlock materi permanen?" else "Are unlocks permanent?", if (id) "Ya, terikat pada akun Google yang sama." else "Yes, tied to the same Google account.") }
                    item { SettingsInfo(if (id) "Pembayaran berhasil tapi saldo belum masuk?" else "Payment succeeded but credits missing?", if (id) "Cek riwayat transaksi dan hubungi dukungan dengan ID order." else "Check transaction history and contact support with your order ID.") }
                    item { SettingsInfo(if (id) "Apakah jawaban AI selalu benar?" else "Is AI always correct?", if (id) "Tidak selalu. Cocokkan dengan materi utama." else "Not always. Verify against lessons.") }
                }
                "support" -> {
                    item { SettingsInfo(if (id) "Email dukungan" else "Support email", support) }
                    item { SettingsInfo(if (id) "Sertakan informasi" else "Include", if (id) "Email akun, ID order jika terkait transaksi, screenshot error dan langkah untuk mengulang masalah." else "Account email, order ID, error screenshot and reproduction steps.") }
                    item { SettingsActionButton(if (id) "Kirim email bantuan" else "Email support") {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$support?subject=Nihongo%20App%20Support"))
                        runCatching { context.startActivity(intent) }.onFailure { onNotice(if (id) "Aplikasi email tidak tersedia." else "No email app available.") }
                    } }
                }
                "privacy" -> {
                    item { SettingsInfo(if (id) "Data yang disimpan" else "Data stored", if (id) "Profil Google, progress, bintang, streak, kredit, riwayat, unlock dan penggunaan layanan AI." else "Google profile, progress, stars, credits, history, unlocks, and AI usage.") }
                    item { SettingsInfo(if (id) "Penggunaan data" else "How data is used", if (id) "Data dipakai untuk autentikasi, sinkronisasi belajar dan kredit di perangkat lain." else "Data is used for authentication, synced learning progress and credits.") }
                    item { SettingsInfo(if (id) "Pembayaran" else "Payments", if (id) "Order QRIS diproses payment gateway. Status dan identitas order disimpan untuk proses kredit." else "QRIS orders are processed by the payment gateway; order details are used to credit accounts.") }
                }
                "terms" -> {
                    item { SettingsInfo(if (id) "Kredit dan pembelian" else "Credits and purchases", if (id) "Kredit terikat ke akun, tidak dapat ditransfer, dan pembelian sukses umumnya final." else "Credits are account-bound, not transferable, and successful purchases are generally final.") }
                    item { SettingsInfo(if (id) "Unlock materi" else "Lesson unlocks", if (id) "Unlock permanen tidak berarti progress pelajaran otomatis selesai." else "Permanent unlocks do not automatically complete lessons.") }
                    item { SettingsInfo(if (id) "Bantuan AI" else "AI assistance", if (id) "Jawaban Sensei AI dapat keliru; pastikan dengan materi belajar." else "AI Sensei responses may be inaccurate; verify against learning material.") }
                }
                "delete" -> {
                    item { SettingsInfo(if (id) "Hapus akun permanen" else "Permanent deletion", if (id) "Akun, progres, riwayat kredit, saldo, dan unlock terkait akan dihapus dari server. Tindakan ini tidak dapat dibatalkan." else "Your account, progress, history, credits and unlocks will be removed from the server. This cannot be undone.") }
                    item { SettingsContentCard {
                        Text(if (id) "Ketik CONFIRM untuk melanjutkan" else "Type CONFIRM to continue", color = NihongoPalette.white, fontSize = 13.sp)
                        OutlinedTextField(value = deleteText, onValueChange = { deleteText = it.take(32) }, label = { Text("CONFIRM") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    } }
                    item { SettingsActionButton(if (id) "Hapus Akun & Data" else "Delete Account & Data", danger = true, enabled = deleteText.trim().uppercase(Locale.ROOT) == "CONFIRM" && !busy) { showDeleteDialog = true } }
                }
            }
        }
    }

    if (showLogoutDialog) AlertDialog(
        onDismissRequest = { showLogoutDialog = false },
        title = { Text(if (id) "Keluar akun?" else "Sign out?") },
        text = { Text(if (id) "Kamu bisa masuk lagi dengan akun Google yang sama." else "You can sign back in with the same Google account.") },
        confirmButton = { TextButton(onClick = { showLogoutDialog = false; onLogout() }) { Text(if (id) "Keluar" else "Sign out") } },
        dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text(if (id) "Batal" else "Cancel") } }
    )
    if (showDeleteDialog) AlertDialog(
        onDismissRequest = { showDeleteDialog = false },
        title = { Text(if (id) "Hapus akun selamanya?" else "Delete account permanently?") },
        text = { Text(if (id) "Aksi ini tidak bisa dibatalkan. Data akun dan kredit di server akan dihapus." else "This cannot be undone. Account data and credits on the server will be deleted.") },
        confirmButton = { TextButton(onClick = {
            showDeleteDialog = false; busy = true
            scope.launch {
                runCatching { repository.deleteAccount(token) }
                    .onSuccess { onNotice(if (id) "Akun sudah dihapus." else "Account deleted."); onLogout() }
                    .onFailure { onNotice(it.message ?: "Gagal menghapus akun") }
                busy = false
            }
        }) { Text(if (id) "Hapus permanen" else "Delete permanently", color = NihongoPalette.redSoft) } },
        dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(if (id) "Batal" else "Cancel") } }
    )
}

@Composable
private fun NativeSettingsMenu(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, note: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().border(1.dp, NihongoPalette.borderStrong, RoundedCornerShape(22.dp))
            .background(NihongoPalette.surface, RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).background(NihongoPalette.surface3, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = NihongoPalette.redSoft, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = NihongoPalette.white, fontSize = 14.sp, fontWeight = FontWeight.Black)
            if (note.isNotBlank()) Text(note, color = NihongoPalette.dim, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (value.isNotBlank()) Text(value, color = NihongoPalette.gold, fontSize = 11.sp, maxLines = 1, modifier = Modifier.widthIn(max = 80.dp))
        Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = NihongoPalette.dim)
    }
}

@Composable
private fun SettingsSectionTitle(text: String) { Text(text, color = NihongoPalette.dim, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)) }

@Composable
private fun SettingsContentCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().border(1.dp, NihongoPalette.borderStrong, RoundedCornerShape(22.dp)).background(NihongoPalette.surface, RoundedCornerShape(22.dp)).padding(16.dp), content = content)
}

@Composable
private fun SettingsInfo(title: String, description: String) {
    SettingsContentCard {
        Text(title, color = NihongoPalette.white, fontWeight = FontWeight.Black, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Text(description, color = NihongoPalette.dim, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun SettingsChoice(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().border(1.dp, if (selected) NihongoPalette.red else NihongoPalette.borderStrong, RoundedCornerShape(18.dp))
        .background(if (selected) Color(0x332C121B) else NihongoPalette.surface2, RoundedCornerShape(18.dp))
        .clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(title, color = NihongoPalette.white, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SettingsFloatSlider(label: String, value: Float, min: Float, max: Float, onValueChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = NihongoPalette.dim, fontSize = 12.sp)
        Text(String.format(Locale.US, "%.2f", value), color = NihongoPalette.gold, fontSize = 12.sp)
    }
    Slider(value = value.coerceIn(min, max), onValueChange = { onValueChange(((it * 20).toInt() / 20f).coerceIn(min, max)) }, valueRange = min..max)
}

@Composable
private fun SettingsActionButton(text: String, danger: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().background(if (danger) Color(0x382F171C) else NihongoPalette.red, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(15.dp), contentAlignment = Alignment.Center
    ) { Text(text, color = if (enabled) (if (danger) NihongoPalette.redSoft else Color.White) else NihongoPalette.dim, fontWeight = FontWeight.Black, fontSize = 13.sp) }
}
