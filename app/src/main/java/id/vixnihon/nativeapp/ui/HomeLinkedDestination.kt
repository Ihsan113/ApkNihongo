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
        route == "learn" -> NativeLinkedLearnScreen(language, chapter, lessons, progress, currentLessonId, onBack)
    }
}

@Composable
private fun NativeLinkedLearnScreen(
    language: String, chapter: NativeChapterMeta?, lessons: List<NativeLessonMeta>,
    progress: Map<String, NativeLessonProgress>, currentLessonId: String?, onBack: () -> Unit
) {
    val isId = language == "id"
    val currentChapterLessons = chapter?.lessonIds?.mapNotNull { id -> lessons.firstOrNull { it.id == id } }.orEmpty()
    val byNo = lessons.associateBy { it.no }
    BackHandler { onBack() }
    Column(Modifier.fillMaxSize().background(NihongoPalette.background)) {
        CreditHeader(if (isId) "Lanjutkan Belajar" else "Continue Learning", "NihonGo", onBack = onBack)
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 36.dp)) {
            item {
                LinkedLearnCard {
                    Text(chapter?.let { if (isId) it.titleId else it.titleEn } ?: "NihonGo", color = NihongoPalette.white, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text(if (isId) "Materi dipilih menurut status lulus dan urutan unlock." else "Lessons follow progress and unlock order.", color = NihongoPalette.dim, fontSize = 11.sp)
                }
                Spacer(Modifier.height(12.dp))
            }
            items(currentChapterLessons, key = { it.id }) { lesson ->
                val passed = progress[lesson.id]?.passed == true
                val unlocked = lesson.no == 1 || byNo[lesson.no - 1]?.let { progress[it.id]?.passed == true } == true
                LinkedLearnCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (passed) Icons.Outlined.CheckCircle else Icons.Outlined.MenuBook, contentDescription = null,
                            tint = if (passed) NihongoPalette.green else if (unlocked) NihongoPalette.redLight else NihongoPalette.dim, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("${lesson.no}. ${if (isId) lesson.titleId else lesson.titleEn}", color = NihongoPalette.white, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            Text(when { passed -> if (isId) "Selesai" else "Completed"; lesson.id == currentLessonId -> if (isId) "Materi selanjutnya" else "Next lesson"; unlocked -> if (isId) "Terbuka" else "Unlocked"; else -> if (isId) "Terkunci" else "Locked" }, color = NihongoPalette.dim, fontSize = 11.sp)
                        }
                    }
                    if (unlocked && lesson.id == currentLessonId) {
                        Text(if (isId) "Pemutar materi native masih dikembangkan." else "Native lesson player is still in development.", color = NihongoPalette.dim, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun LinkedLearnCard(content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = NihongoPalette.surface,
        border = BorderStroke(1.dp, NihongoPalette.borderStrong)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}
