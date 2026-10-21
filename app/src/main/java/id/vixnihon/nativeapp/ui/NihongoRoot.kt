package id.vixnihon.nativeapp.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import id.vixnihon.nativeapp.data.AppUser
import id.vixnihon.nativeapp.data.AuthRepository
import id.vixnihon.nativeapp.data.AuthSession
import id.vixnihon.nativeapp.data.SessionStore
import kotlinx.coroutines.launch

enum class NativeStage { Booting, Login, Onboarding, Home }

@Composable
fun NihongoRoot(
    activity: Activity,
    repository: AuthRepository,
    sessionStore: SessionStore
) {
    var stage by remember { mutableStateOf(NativeStage.Booting) }
    var user by remember { mutableStateOf<AppUser?>(null) }
    var token by remember { mutableStateOf<String?>(null) }
    var language by remember { mutableStateOf("id") }
    var startupNotice by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        language = sessionStore.getLanguage()
        val savedToken = sessionStore.getToken()
        if (savedToken.isNullOrBlank()) {
            stage = NativeStage.Login
            return@LaunchedEffect
        }
        try {
            val currentUser = repository.fetchMe(savedToken)
            sessionStore.updateUser(currentUser)
            token = savedToken
            user = currentUser
            stage = if (sessionStore.isOnboardingComplete(currentUser.id)) {
                NativeStage.Home
            } else {
                NativeStage.Onboarding
            }
        } catch (error: Exception) {
            sessionStore.clearSession()
            startupNotice = if (error is java.io.IOException) {
                "Koneksi ke server terputus. Silakan login kembali."
            } else {
                "Sesi masuk perlu diperbarui. Silakan login kembali."
            }
            token = null
            user = null
            stage = NativeStage.Login
        }
    }

    fun setLanguage(nextLanguage: String) {
        val next = if (nextLanguage == "en") "en" else "id"
        if (language != next) {
            language = next
            scope.launch { sessionStore.setLanguage(next) }
        }
    }

    fun switchLanguage() {
        setLanguage(if (language == "id") "en" else "id")
    }

    fun completeLogin(session: AuthSession) {
        scope.launch {
            // Persist the session immediately so progress-sync failure cannot lose a valid login.
            sessionStore.saveSession(session.token, session.user)
            token = session.token
            user = session.user
            stage = if (sessionStore.isOnboardingComplete(session.user.id)) {
                NativeStage.Home
            } else {
                NativeStage.Onboarding
            }
            startupNotice = ""
            // Keep the first screen responsive; progress warm-up is best effort in the background.
            scope.launch { runCatching { repository.warmProgress(session.token) } }
        }
    }

    fun logOut() {
        scope.launch {
            sessionStore.clearSession()
            runCatching {
                CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest())
            }
            token = null
            user = null
            startupNotice = "Kamu sudah keluar dari akun."
            stage = NativeStage.Login
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NihongoPalette.background)
    ) {
        when (stage) {
            NativeStage.Booting -> BootScreen()
            NativeStage.Login -> LoginScreen(
                activity = activity,
                repository = repository,
                language = language,
                initialNotice = startupNotice,
                onToggleLanguage = ::switchLanguage,
                onSignedIn = ::completeLogin
            )
            NativeStage.Onboarding -> {
                val currentUser = user
                if (currentUser == null || token.isNullOrBlank()) {
                    LaunchedEffect(stage) { stage = NativeStage.Login }
                } else {
                    OnboardingScreen(
                        language = language,
                        onToggleLanguage = ::switchLanguage,
                        onComplete = {
                            scope.launch {
                                sessionStore.completeOnboarding(currentUser.id)
                                stage = NativeStage.Home
                            }
                        }
                    )
                }
            }
            NativeStage.Home -> {
                val currentUser = user
                if (currentUser == null || token.isNullOrBlank()) {
                    LaunchedEffect(stage) { stage = NativeStage.Login }
                } else {
                    HomeScreen(
                        user = currentUser,
                        authToken = token.orEmpty(),
                        repository = repository,
                        language = language,
                        onLogout = ::logOut,
                        onToggleLanguage = ::switchLanguage,
                        onNotice = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun BootScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.Text(
                text = "日本語",
                color = NihongoPalette.redLight,
                style = androidx.compose.material3.MaterialTheme.typography.headlineLarge
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
            CircularProgressIndicator(color = NihongoPalette.redLight, strokeWidth = 2.dp)
        }
    }
}
