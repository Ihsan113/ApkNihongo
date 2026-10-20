package id.vixnihon.nativeapp.ui

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import id.vixnihon.nativeapp.R
import id.vixnihon.nativeapp.data.ApiException
import id.vixnihon.nativeapp.data.AuthRepository
import id.vixnihon.nativeapp.data.AuthSession
import id.vixnihon.nativeapp.data.GoogleAuthConfig
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    activity: Activity,
    repository: AuthRepository,
    language: String,
    initialNotice: String,
    onToggleLanguage: () -> Unit,
    onSignedIn: (AuthSession) -> Unit
) {
    val isId = language == "id"
    val scope = rememberCoroutineScope()
    val credentialManager = remember(activity) { CredentialManager.create(activity) }
    var authConfig by remember { mutableStateOf<GoogleAuthConfig?>(null) }
    var configLoading by remember { mutableStateOf(true) }
    var signingIn by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }
    var noticeText by remember { mutableStateOf(initialNotice) }

    suspend fun loadConfig() {
        configLoading = true
        errorText = ""
        try {
            val config = repository.getAuthConfig()
            authConfig = config
            if (!config.configured || config.clientId.isBlank()) {
                errorText = if (isId) {
                    "Login Google belum disiapkan di server. Coba lagi nanti."
                } else {
                    "Google Login is not configured on the server yet."
                }
            }
        } catch (error: Exception) {
            authConfig = null
            errorText = if (isId) {
                "Tidak dapat memuat konfigurasi login. Periksa internet lalu coba lagi."
            } else {
                "Unable to load sign-in configuration. Check your internet and retry."
            }
        } finally {
            configLoading = false
        }
    }

    LaunchedEffect(Unit) { loadConfig() }
    LaunchedEffect(initialNotice) { noticeText = initialNotice }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(NihongoPalette.background)
    ) {
        val heroHeight = (maxHeight * 0.70f).coerceAtMost(520.dp).coerceAtLeast(390.dp)
        Image(
            painter = painterResource(R.drawable.login_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .align(Alignment.TopCenter)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0x0A060711),
                            0.24f to Color(0x1F060711),
                            0.49f to Color(0x69060711),
                            0.72f to Color(0xE0060711),
                            1.0f to NihongoPalette.background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xC0181824))
                        .border(1.dp, NihongoPalette.borderStrong, CircleShape)
                        .clickable(onClick = onToggleLanguage)
                        .padding(horizontal = 13.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = null,
                        tint = NihongoPalette.dim2,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = if (isId) "Indonesia · 日本語" else "English · 日本語",
                        color = NihongoPalette.white,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(if (maxHeight < 760.dp) 47.dp else 63.dp))

            Text(
                text = "日本語",
                color = NihongoPalette.redLight,
                fontSize = if (maxWidth < 360.dp) 57.sp else 66.sp,
                lineHeight = 68.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 5.sp,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (isId) "Masuk ke Nihongo" else "Sign in to Nihongo",
                color = NihongoPalette.white,
                fontSize = if (maxWidth < 360.dp) 29.sp else 32.sp,
                lineHeight = 37.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.1).sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (isId) {
                    "Login Google dipakai untuk menyimpan progres belajar, bintang, dan unlock bab."
                } else {
                    "Google Login saves your learning progress, stars, and chapter unlocks."
                },
                color = NihongoPalette.dim2,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(315.dp.coerceAtMost(maxWidth - 28.dp))
            )

            Spacer(Modifier.height(30.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = Color(0xE00D0D16),
                border = BorderStroke(1.dp, Color(0x47E03535)),
                shadowElevation = 18.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 19.dp, vertical = 21.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0x55FF5C5C), Color(0x1CE03535), Color(0x15E03535))
                                    )
                                )
                                .border(1.dp, Color(0x66FF5A5A), RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Outlined.VerifiedUser,
                                contentDescription = null,
                                tint = NihongoPalette.redSoft,
                                modifier = Modifier.size(33.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isId) "Progress aman per akun" else "Safe progress per account",
                                color = NihongoPalette.white,
                                fontSize = 18.sp,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (isId) {
                                    "Progres belajar tersimpan dan bisa dilanjutkan di perangkat lain."
                                } else {
                                    "Your learning progress is saved and can be resumed on another device."
                                },
                                color = NihongoPalette.dim2,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.height(21.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0x14FFFFFF))
                    )
                    Spacer(Modifier.height(18.dp))

                    if (noticeText.isNotBlank()) {
                        MessagePill(text = noticeText, warning = false)
                        Spacer(Modifier.height(12.dp))
                    }
                    if (errorText.isNotBlank()) {
                        MessagePill(text = errorText, warning = true)
                        Spacer(Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 58.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color(0xFFF9FAFC))
                            .clickable(enabled = !signingIn && !configLoading) {
                                scope.launch {
                                    signingIn = true
                                    errorText = ""
                                    noticeText = ""
                                    try {
                                        var config = authConfig
                                        if (config == null || !config.configured || config.clientId.isBlank()) {
                                            loadConfig()
                                            config = authConfig
                                        }
                                        if (config == null || !config.configured || config.clientId.isBlank()) {
                                            throw ApiException(
                                                if (isId) "Login Google belum tersedia. Periksa konfigurasi server." else "Google Login is not ready yet."
                                            )
                                        }
                                        val signInOption = GetSignInWithGoogleOption.Builder(config.clientId).build()
                                        val request = GetCredentialRequest.Builder()
                                            .addCredentialOption(signInOption)
                                            .build()
                                        val response = credentialManager.getCredential(activity, request)
                                        val credential = response.credential
                                        if (credential !is CustomCredential ||
                                            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                        ) {
                                            throw ApiException(
                                                if (isId) "Respons Login Google tidak dikenali. Coba lagi." else "Unrecognized Google sign-in response. Try again."
                                            )
                                        }
                                        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                                        val session = repository.loginWithGoogleIdToken(googleIdToken)
                                        onSignedIn(session)
                                    } catch (error: Exception) {
                                        errorText = when {
                                            error is ApiException -> error.message.orEmpty()
                                            error.message?.contains("cancel", ignoreCase = true) == true -> if (isId) "Login dibatalkan." else "Sign-in was cancelled."
                                            error is java.io.IOException -> error.message.orEmpty().ifBlank { if (isId) "Koneksi internet bermasalah." else "Network error." }
                                            else -> if (isId) "Login Google gagal. Pastikan Google Play Services aktif lalu coba lagi." else "Google sign-in failed. Check Google Play Services and try again."
                                        }
                                    } finally {
                                        signingIn = false
                                    }
                                }
                            }
                            .padding(horizontal = 17.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (signingIn || configLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(23.dp),
                                strokeWidth = 2.5.dp,
                                color = NihongoPalette.red
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.google_mark),
                                contentDescription = "Google",
                                modifier = Modifier.size(25.dp)
                            )
                        }
                        Spacer(Modifier.width(13.dp))
                        Text(
                            text = when {
                                signingIn -> if (isId) "Memproses login…" else "Signing in…"
                                configLoading -> if (isId) "Menyiapkan Google Login…" else "Preparing Google Login…"
                                else -> if (isId) "Login dengan Google" else "Sign in with Google"
                            },
                            color = Color(0xFF252735),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (errorText.isNotBlank() && !configLoading && !signingIn) {
                        Spacer(Modifier.height(11.dp))
                        Text(
                            text = if (isId) "Coba muat ulang konfigurasi" else "Retry configuration",
                            color = NihongoPalette.redSoft,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { scope.launch { loadConfig() } }
                                .padding(8.dp)
                        )
                    }

                    Spacer(Modifier.height(15.dp))
                    Text(
                        text = if (isId) {
                            "Dengan melanjutkan, kamu menyetujui penggunaan akun Google untuk autentikasi Nihongo."
                        } else {
                            "By continuing, you agree to use your Google account to authenticate with Nihongo."
                        },
                        color = NihongoPalette.dim,
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(19.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0x1CE03535)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        tint = NihongoPalette.redSoft,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isId) "Autentikasi aman · NihonGo Indonesia" else "Secure authentication · NihonGo Indonesia",
                    color = NihongoPalette.dim,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun MessagePill(text: String, warning: Boolean) {
    val tint = if (warning) Color(0xFFFF7777) else NihongoPalette.dim2
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(if (warning) Color(0x20E03535) else Color(0x151C8C58))
            .border(1.dp, if (warning) Color(0x46E03535) else Color(0x302DB36A), RoundedCornerShape(13.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (warning) "!" else "✓",
            color = tint,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(text = text, color = tint, fontSize = 11.5.sp, lineHeight = 16.sp)
    }
}
