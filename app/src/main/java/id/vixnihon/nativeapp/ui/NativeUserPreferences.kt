package id.vixnihon.nativeapp.ui

import android.content.Context

/** Per-device native equivalent of the web's nihongo_settings. No server state is fabricated. */
internal data class NativeUserPreferences(
    val speechEnabled: Boolean = true,
    val speechRate: Float = 1f,
    val speechPitch: Float = 1f,
    val speechVolume: Float = 1f,
    val theme: String = "system",
    val selectedAvatar: String = "waifu.vrm"
)

internal object NativeSettingsStorage {
    private const val STORE = "nihongo_native_user_preferences_v2"
    fun load(context: Context): NativeUserPreferences {
        val p = context.applicationContext.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        return NativeUserPreferences(
            speechEnabled = p.getBoolean("speechEnabled", true),
            speechRate = p.getFloat("ttsRate", 1f).coerceIn(0.75f, 1.25f),
            speechPitch = p.getFloat("ttsPitch", 1f).coerceIn(0.8f, 1.3f),
            speechVolume = p.getFloat("ttsVolume", 1f).coerceIn(0f, 1f),
            theme = p.getString("appearanceMode", "system").takeIf { it in listOf("system", "light", "dark") } ?: "system",
            selectedAvatar = "waifu.vrm" // The APK currently bundles only this one converted GLB.
        )
    }
    fun save(context: Context, value: NativeUserPreferences) {
        context.applicationContext.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
            .putBoolean("speechEnabled", value.speechEnabled)
            .putFloat("ttsRate", value.speechRate.coerceIn(0.75f, 1.25f))
            .putFloat("ttsPitch", value.speechPitch.coerceIn(0.8f, 1.3f))
            .putFloat("ttsVolume", value.speechVolume.coerceIn(0f, 1f))
            .putString("appearanceMode", value.theme)
            .apply()
    }
}
