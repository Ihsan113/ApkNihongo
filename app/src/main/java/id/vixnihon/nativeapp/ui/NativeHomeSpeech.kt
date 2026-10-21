package id.vixnihon.nativeapp.ui

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.random.Random

/** Status is deliberately exposed for the Settings voice self-test. */
internal enum class NativeSpeechPhase { INITIALIZING, READY, NO_ENGINE, MISSING_JAPANESE, AUDIO_ERROR }
internal data class NativeSpeechInfo(val phase: NativeSpeechPhase, val message: String)

/**
 * A single Android TTS engine for a single visible native screen.
 *
 * Android 11+ additionally requires the TTS_SERVICE <queries> manifest entry.
 * All engine and utterance callbacks are marshalled to the main thread. A late
 * callback from a disposed/fallback engine cannot change the active screen.
 */
internal class NativeJapaneseTts(
    context: Context,
    private val onStatus: (NativeSpeechInfo) -> Unit,
    private val onSpeaking: (Boolean) -> Unit = {}
) {
    private val applicationContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var closed = false
    private var generation = 0
    private var fallbackAttempted = false
    private var utteranceNumber = 0L

    var ready: Boolean = false
        private set
    var speaking: Boolean = false
        private set

    fun start() {
        if (!closed && tts == null) initialize(null)
    }

    private fun initialize(enginePackage: String?) {
        if (closed) return
        val attemptId = ++generation
        ready = false
        speaking = false
        tts?.runCatching { stop(); shutdown() }
        tts = null
        onStatus(NativeSpeechInfo(NativeSpeechPhase.INITIALIZING, "Menghubungkan mesin suara Jepang…"))
        try {
            val listener = TextToSpeech.OnInitListener { result ->
                main.post {
                    if (closed || attemptId != generation) return@post
                    completeInitialization(result, enginePackage, attemptId)
                }
            }
            tts = if (enginePackage == null) {
                TextToSpeech(applicationContext, listener)
            } else {
                TextToSpeech(applicationContext, listener, enginePackage)
            }
        } catch (_: Exception) {
            if (!closed && attemptId == generation) {
                onStatus(NativeSpeechInfo(NativeSpeechPhase.NO_ENGINE,
                    "Mesin Text-to-speech tidak dapat dibuka. Periksa mesin pilihan di Setelan HP, lalu tes ulang."))
            }
        }
    }

    private fun completeInitialization(result: Int, enginePackage: String?, attemptId: Int) {
        val local = tts ?: return
        if (result != TextToSpeech.SUCCESS) {
            // If the system-selected engine fails to bind, try one different
            // installed engine only once; never endlessly re-create TTS.
            if (!fallbackAttempted) {
                val defaultPackage = runCatching { local.defaultEngine }.getOrNull()
                val available = runCatching { local.engines.map { it.name } }.getOrDefault(emptyList())
                val alternative = available.firstOrNull { it != enginePackage && it != defaultPackage }
                    ?: available.firstOrNull { enginePackage == null && it != defaultPackage }
                if (alternative != null) {
                    fallbackAttempted = true
                    initialize(alternative)
                    return
                }
            }
            onStatus(NativeSpeechInfo(NativeSpeechPhase.NO_ENGINE,
                "Mesin Text-to-speech gagal dimulai. Pilih mesin suara di Setelan HP, lalu gunakan Tes Suara."))
            runCatching { local.shutdown() }
            tts = null
            return
        }

        val japaneseAvailability = runCatching { local.setLanguage(Locale.JAPAN) }
            .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        val languageResult = if (japaneseAvailability >= TextToSpeech.LANG_AVAILABLE) {
            japaneseAvailability
        } else {
            runCatching { local.setLanguage(Locale.JAPANESE) }
                .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        }
        if (languageResult < TextToSpeech.LANG_AVAILABLE) {
            val message = if (languageResult == TextToSpeech.LANG_MISSING_DATA) {
                "Mesin suara ditemukan, tetapi data suara Jepang belum terpasang. Unduh Japanese di setelan mesin TTS HP."
            } else {
                "Mesin suara aktif tidak mendukung bahasa Jepang. Pilih/instal mesin TTS dan suara Japanese di Setelan HP."
            }
            onStatus(NativeSpeechInfo(NativeSpeechPhase.MISSING_JAPANESE, message))
            runCatching { local.shutdown() }
            tts = null
            return
        }
        runCatching {
            local.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            // Prefer an already-installed offline Japanese voice when offered.
            // Missing offline voices are not an error: let the TTS engine choose.
            val voice = local.voices?.filter {
                it.locale.language == "ja" && !it.isNetworkConnectionRequired
            }?.maxByOrNull { it.quality }
            if (voice != null) local.setVoice(voice)
        }
        local.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                main.post {
                    if (!closed && attemptId == generation) {
                        speaking = true
                        onSpeaking(true)
                    }
                }
            }
            override fun onDone(utteranceId: String?) {
                main.post {
                    if (!closed && attemptId == generation) {
                        speaking = false
                        onSpeaking(false)
                    }
                }
            }
            @Deprecated("Deprecated in Android TTS API")
            override fun onError(utteranceId: String?) { reportAudioFailure(attemptId) }
            override fun onError(utteranceId: String?, errorCode: Int) { reportAudioFailure(attemptId) }
        })
        ready = true
        val activePackage = enginePackage ?: runCatching { local.defaultEngine }.getOrNull().orEmpty()
        onStatus(NativeSpeechInfo(NativeSpeechPhase.READY,
            if (activePackage.isNotBlank()) "Mesin suara Jepang siap ($activePackage)." else "Mesin suara Jepang siap."))
    }

    private fun reportAudioFailure(attemptId: Int) {
        main.post {
            if (!closed && attemptId == generation) {
                ready = false
                speaking = false
                onSpeaking(false)
                onStatus(NativeSpeechInfo(NativeSpeechPhase.AUDIO_ERROR,
                    "Mesin TTS terbuka, tetapi gagal menghasilkan audio. Periksa volume media dan paket suara Jepang, lalu tes ulang."))
                ++generation
                tts?.runCatching { stop(); shutdown() }
                tts = null
            }
        }
    }

    fun speak(text: String, rate: Float, pitch: Float, volume: Float): Boolean {
        val local = tts ?: return false
        if (closed || !ready || text.isBlank()) return false
        val speedResult = runCatching {
            local.setSpeechRate((rate * 0.94f).coerceIn(0.6f, 1.4f))
            local.setPitch(pitch.coerceIn(0.8f, 1.3f))
            val options = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f))
            }
            local.speak(text, TextToSpeech.QUEUE_FLUSH, options, "nihongo-ja-${++utteranceNumber}")
        }.getOrDefault(TextToSpeech.ERROR)
        if (speedResult != TextToSpeech.SUCCESS) {
            reportAudioFailure(generation)
            return false
        }
        return true
    }

    fun close() {
        if (closed) return
        closed = true
        ++generation // invalidate any in-flight binder callbacks
        ready = false
        speaking = false
        onSpeaking(false)
        tts?.runCatching { stop(); shutdown() }
        tts = null
    }
}

/**
 * Same Home voice behavior as web: an initial Japanese greeting and occasional
 * idle sentences. The engine exists only while Home is foreground/visible.
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
    val currentSpeaking = rememberUpdatedState(onSpeaking)
    val currentUnavailable = rememberUpdatedState(onUnavailable)
    var currentSession by remember { mutableStateOf<NativeJapaneseTts?>(null) }
    var sessionReady by remember { mutableStateOf(false) }
    var greetedSession by remember { mutableStateOf<NativeJapaneseTts?>(null) }

    DisposableEffect(context, enabled) {
        if (!enabled) {
            currentSpeaking.value(false)
            onDispose { }
        } else {
            val session = NativeJapaneseTts(
                context = context,
                onStatus = { state ->
                    sessionReady = state.phase == NativeSpeechPhase.READY
                    if (state.phase != NativeSpeechPhase.READY && state.phase != NativeSpeechPhase.INITIALIZING) {
                        currentUnavailable.value(state.message)
                    }
                },
                onSpeaking = { currentSpeaking.value(it) }
            )
            currentSession = session
            session.start()
            onDispose {
                session.close()
                if (currentSession === session) currentSession = null
                sessionReady = false
            }
        }
    }

    LaunchedEffect(enabled, currentSession, sessionReady, rate, pitch, volume) {
        val session = currentSession ?: return@LaunchedEffect
        if (!enabled || !sessionReady) return@LaunchedEffect

        if (greetedSession !== session) {
            greetedSession = session
            delay(600)
            if (isActive) session.speak(
                "おかえりなさい！今日も一緒に頑張りましょう！", rate, pitch, volume
            )
        }
        val phrases = listOf(
            "頑張って！", "すごい！", "やった！", "いくよ！", "一緒に練習しよう！",
            "日本語は面白い！", "今日も頑張ろう！", "毎日少しずつね。", "いい調子！", "最高だね！"
        )
        var lastPhrase = -1
        while (isActive && session.ready) {
            delay(Random.nextLong(10_000, 15_001))
            if (session.speaking || !session.ready) continue
            var nextPhrase = Random.nextInt(phrases.size)
            if (nextPhrase == lastPhrase) nextPhrase = (nextPhrase + 1) % phrases.size
            lastPhrase = nextPhrase
            session.speak(phrases[nextPhrase], rate, pitch, volume)
        }
    }
}
