package id.vixnihon.nativeapp.ui

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import id.vixnihon.nativeapp.R

private data class WelcomeSlide(
    val imageId: Int,
    val titleId: String,
    val noteId: String,
    val titleEn: String,
    val noteEn: String
)

private val welcomeSlides = listOf(
    WelcomeSlide(
        R.drawable.onboarding_1,
        "Selamat Datang",
        "Belajar bahasa Jepang jadi lebih menyenangkan dan bermakna.",
        "Welcome",
        "Make learning Japanese more fun, friendly, and meaningful from the first day."
    ),
    WelcomeSlide(
        R.drawable.onboarding_2,
        "Belajar Terarah",
        "Pelajaran terstruktur, kuis interaktif, dan review untuk pemahaman lebih baik.",
        "Guided Learning",
        "Structured lessons, interactive quizzes, and review to help you understand better."
    ),
    WelcomeSlide(
        R.drawable.onboarding_3,
        "Latihan Setiap Hari",
        "Bangun kebiasaan belajar setiap hari dan raih kemajuan yang konsisten.",
        "Practice Every Day",
        "Build a daily study habit and keep your progress steady."
    )
)

@Composable
fun OnboardingScreen(
    language: String,
    onToggleLanguage: () -> Unit,
    onComplete: () -> Unit
) {
    val isId = language == "id"
    var index by remember { mutableIntStateOf(0) }
    val slide = welcomeSlides[index]

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(NihongoPalette.background)
    ) {
        val viewportWidth = maxWidth
        val viewportHeight = maxHeight
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size((viewportWidth * 0.84f).coerceAtMost(340.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x1DE03535), Color(0x07060711), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 9.dp),
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
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = null,
                        tint = NihongoPalette.dim2,
                        modifier = Modifier.size(15.dp)
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

            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((viewportHeight * 0.43f).coerceAtMost(365.dp).coerceAtLeast(275.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(slide.imageId),
                    contentDescription = if (isId) "Ilustrasi onboarding NihonGo" else "NihonGo onboarding illustration",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.96f)
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isId) slide.titleId else slide.titleEn,
                color = NihongoPalette.white,
                fontSize = if (viewportWidth < 360.dp) 29.sp else 33.sp,
                lineHeight = 37.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.9).sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(13.dp))
            Text(
                text = if (isId) slide.noteId else slide.noteEn,
                color = NihongoPalette.dim,
                fontSize = 13.5.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(320.dp.coerceAtMost(viewportWidth - 24.dp))
            )

            Spacer(Modifier.height(21.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                welcomeSlides.indices.forEach { dotIndex ->
                    Box(
                        modifier = Modifier
                            .size(if (dotIndex == index) 22.dp else 6.dp, 6.dp)
                            .clip(CircleShape)
                            .background(if (dotIndex == index) NihongoPalette.redLight else Color(0x3E8984AA))
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Brush.linearGradient(listOf(NihongoPalette.redLight, NihongoPalette.red)))
                    .clickable {
                        if (index < welcomeSlides.lastIndex) index++ else onComplete()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when {
                            isId && index == welcomeSlides.lastIndex -> "Mulai Sekarang"
                            isId -> "Lanjut"
                            index == welcomeSlides.lastIndex -> "Get Started"
                            else -> "Next"
                        },
                        color = Color.White,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(text = "›", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
