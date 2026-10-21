package id.vixnihon.nativeapp.ui

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.random.Random

/**
 * Optional on-device Japanese greeting. TTS never runs during scrolling or in background.
 * Web greets once/session then says random JP phrases at 10–15 s intervals.
 * Native differs intentionally by making the auto-voice opt-in for battery and UX.
 */
@Composable
internal fun NativeHomeSpeech(enabled: Boolean) {
    val context = LocalContext.current.applicationContext
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    var isReady by remember { mutableStateOf(false) }
    var greeted by remember { mutableStateOf(false) }
    DisposableEffect(context, enabled) {
        if (enabled) {
            val tts = TextToSpeech(context) { state -> isReady = state == TextToSpeech.SUCCESS }
            engine = tts
            onDispose { tts.stop(); tts.shutdown(); engine = null; isReady = false }
        } else {
            onDispose { }
        }
    }
    LaunchedEffect(enabled, isReady, engine) {
        val tts = engine ?: return@LaunchedEffect
        if (!enabled || !isReady || tts.isLanguageAvailable(Locale.JAPANESE) < TextToSpeech.LANG_AVAILABLE) {
            tts.stop()
            return@LaunchedEffect
        }
        tts.language = Locale.JAPANESE
        tts.setSpeechRate(0.94f)
        if (!greeted) {
            greeted = true
            delay(520)
            tts.speak("おかえりなさい！今日も一緒に頑張りましょう！", TextToSpeech.QUEUE_FLUSH, null, "home-welcome")
            delay(4500)
        }
        val phrases = listOf("頑張って！", "すごい！", "やった！", "いくよ！", "一緒に練習しよう！", "日本語は面白い！", "今日も頑張ろう！", "毎日少しずつね。", "いい調子！", "最高だね！")
        var last = -1
        while (isActive) {
            delay(Random.nextLong(10_000, 15_001))
            var next = Random.nextInt(phrases.size)
            if (next == last) next = (next + 1) % phrases.size
            last = next
            tts.speak(phrases[next], TextToSpeech.QUEUE_FLUSH, null, "home-idle-$next")
        }
    }
}
