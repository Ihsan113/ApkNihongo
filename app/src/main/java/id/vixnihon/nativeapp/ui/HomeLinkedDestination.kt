package id.vixnihon.nativeapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.vixnihon.nativeapp.data.AuthRepository

/** Home destinations stay native. Payment and history are full screens; Home/learn retains its original behavior. */
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
    when {
        route == "missions" -> NativeMissionsScreen(
            language = language, token = token, repository = repository, onBack = onBack,
            onGoMission = { type ->
                when (type) {
                    "study", "lesson" -> onNavigate("learn")
                    "quiz", "review" -> onNotice(if (language == "id") "Aktivitas ini belum tersedia di native; progres misi tidak diubah." else "This activity isn't available yet; progress unchanged.")
                    else -> onBack()
                }
            }, onNotice = onNotice
        )
        route == "topup" -> NativeCreditTopupScreen(
            language = language, token = token, repository = repository, onBack = onBack,
            onHistory = { onNavigate("history") }, onOrder = { onNavigate("payment:$it") }
        )
        route == "history" -> NativeCreditHistoryScreen(
            language = language, token = token, repository = repository, onBack = onBack,
            onOpenOrder = { onNavigate("payment:$it") }
        )
        route.startsWith("payment:") -> NativeCreditPaymentScreen(
            language = language, token = token, repository = repository,
            orderId = route.substringAfter("payment:"), onBack = onBack,
            onTopup = { onNavigate("topup") }, onNotice = onNotice
        )
        route == "learn" -> NativeLearnScreen(language, token, repository, onBack = onBack, onTopup = { onNavigate("topup") }, onNotice = onNotice)
    }
}

