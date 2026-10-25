package id.vixnihon.nativeapp.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.security.MessageDigest

private val Context.nihongoDataStore by preferencesDataStore(name = "nihongo_native_v1")

class SessionStore(context: Context) {
    private val dataStore = context.applicationContext.nihongoDataStore

    suspend fun getToken(): String? {
        val encrypted = dataStore.data.first()[TOKEN_KEY] ?: return null
        val token = LocalCipher.decrypt(encrypted)
        if (token.isNullOrBlank()) clearSession()
        return token?.takeIf { it.isNotBlank() }
    }

    suspend fun saveSession(token: String, user: AppUser) {
        val encryptedToken = LocalCipher.encrypt(token)
        dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = encryptedToken
            prefs[USER_KEY] = user.toJson()
        }
    }

    suspend fun updateUser(user: AppUser) {
        dataStore.edit { it[USER_KEY] = user.toJson() }
    }

    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(USER_KEY)
        }
    }

    suspend fun getLanguage(): String = dataStore.data.first()[LANGUAGE_KEY] ?: "id"

    suspend fun setLanguage(language: String) {
        dataStore.edit { it[LANGUAGE_KEY] = if (language == "en") "en" else "id" }
    }

    suspend fun isOnboardingComplete(userId: String): Boolean {
        return dataStore.data.first()[onboardingKey(userId)] ?: false
    }

    suspend fun completeOnboarding(userId: String) {
        dataStore.edit { it[onboardingKey(userId)] = true }
    }

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("session_token_encrypted")
        private val USER_KEY = stringPreferencesKey("session_user")
        private val LANGUAGE_KEY = stringPreferencesKey("language")

        private fun onboardingKey(userId: String) =
            booleanPreferencesKey("onboarding_v1_${sha256(userId).take(20)}")

        private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
