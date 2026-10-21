package id.vixnihon.nativeapp.ui

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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

/** Web Home parity: Japanese welcome once per visit then short idle Japanese utterances.
 * One persistent Android TTS instance while Home is active, no network polling.
 * Android has no guaranteed Japanese voice: availability must be communicated.
 */
@Composable
internal fun NativeHomeSpeech(
    enabled: Boolean,
    rate: Float = 1f,
    pitch: Float = 1f,
    volume: Float = 1f,
    onSpeaking: (Boolean) -> Unit = {},
    onUnavailable: (String) -> Unit = {}
) {
    val context = LocalContext.current.applicationContext
    val currentSpeaking = androidx.compose.runtime.rememberUpdatedState(onSpeaking)
    val currentUnavailable = androidx.compose.runtime.rememberUpdatedState(onUnavailable)
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }
    var unsupported by remember { mutableStateOf(false) }
    var greeted by remember { mutableStateOf(false) }
    DisposableEffect(context, enabled) {
        if (!enabled) {
            currentSpeaking.value(false)
            onDispose { }
        } else {
            unsupported = false
            val tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ready = true
                } else {
                    unsupported = true
                    currentUnavailable.value("Mesin suara Android gagal memulai. Periksa Text-to-speech di Pengaturan HP.")
                }
            }
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { currentSpeaking.value(true) }
                override fun onDone(utteranceId: String?) { currentSpeaking.value(false) }
                @Deprecated("Deprecated by Android")
                override fun onError(utteranceId: String?) { currentSpeaking.value(false) }
            })
            engine = tts
            onDispose {
                ready = false
                currentSpeaking.value(false)
                tts.stop()
                tts.shutdown()
                engine = null
            }
        }
    }
    LaunchedEffect(enabled, engine, ready, rate, pitch, volume) {
        val tts = engine ?: return@LaunchedEffect
        if (!enabled || !ready || unsupported) return@LaunchedEffect
        val availability = tts.isLanguageAvailable(Locale.JAPANESE)
        if (availability < TextToSpeech.LANG_AVAILABLE) {
            unsupported = true
            currentUnavailable.value("Suara Jepang belum tersedia. Instal paket suara Japanese di pengaturan Text-to-speech Android.")
            return@LaunchedEffect
        }
        tts.language = Locale.JAPANESE
        tts.setSpeechRate((rate * 0.94f).coerceIn(0.6f, 1.4f))
        tts.setPitch(pitch.coerceIn(0.8f, 1.3f))
        val bundle = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f)) }
        if (!greeted) {
            greeted = true
            delay(520)
            tts.speak("おかえりなさい！今日も一緒に頑張りましょう！", TextToSpeech.QUEUE_FLUSH, bundle, "nihongo-home-welcome")
            delay(4000)
        }
        val phrases = listOf("頑張って！", "すごい！", "やった！", "いくよ！", "一緒に練習しよう！", "日本語は面白い！", "今日も頑張ろう！", "毎日少しずつね。", "いい調子！", "最高だね！")
        var last = -1
        while (isActive) {
            delay(Random.nextLong(10_000, 15_001))
            var idx = Random.nextInt(phrases.size)
            if (idx == last) idx = (idx + 1) % phrases.size
            last = idx
            tts.speak(phrases[idx], TextToSpeech.QUEUE_FLUSH, bundle, "nihongo-home-idle-$idx")
        }
    }
}
