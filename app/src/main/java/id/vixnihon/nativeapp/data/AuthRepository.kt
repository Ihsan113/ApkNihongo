package id.vixnihon.nativeapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class ApiException(message: String, val statusCode: Int? = null) : Exception(message)

/** Native REST client for the existing NihonGo deployment. The server remains authoritative. */
class AuthRepository {
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .writeTimeout(18, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun getAuthConfig(): GoogleAuthConfig = withContext(Dispatchers.IO) {
        val data = request("/api/auth/config")
        GoogleAuthConfig(
            configured = data.optBoolean("googleConfigured", false),
            clientId = data.optString("googleClientId", "")
        )
    }

    suspend fun loginWithGoogleIdToken(idToken: String): AuthSession = withContext(Dispatchers.IO) {
        val body = JSONObject().put("credential", idToken).toString()
        val data = request("/api/auth/google", method = "POST", body = body)
        val token = data.optString("token", "")
        val userJson = data.optJSONObject("user")
        if (token.isBlank() || userJson == null) {
            throw ApiException("Server tidak mengirim sesi login yang valid.")
        }
        AuthSession(token = token, user = AppUser.fromJson(userJson))
    }

    suspend fun fetchMe(token: String): AppUser = withContext(Dispatchers.IO) {
        val data = request("/api/auth/me", token = token)
        val user = data.optJSONObject("user") ?: throw ApiException("Data akun tidak tersedia.")
        AppUser.fromJson(user)
    }

    /** The web client warms this endpoint on sign-in; failure must not block a valid session. */
    suspend fun warmProgress(token: String) = withContext(Dispatchers.IO) {
        request("/api/progress", token = token)
    }

    /** Read the authenticated progress map for the native Learn/Home experience. */
    suspend fun fetchProgress(token: String): JSONObject = withContext(Dispatchers.IO) {
        request("/api/progress", token = token)
    }

    /** Read server-authoritative XP, level, credits, accuracy, streak and seven-day activity. */
    suspend fun fetchWeeklyStats(token: String, language: String): JSONObject = withContext(Dispatchers.IO) {
        val encodedLanguage = URLEncoder.encode(language, StandardCharsets.UTF_8.name())
        request("/api/stats/weekly?lang=$encodedLanguage", token = token)
    }

    /** Home shortcuts stay native and use the same authenticated server contracts as the web. */
    suspend fun fetchTodayMissions(token: String): JSONObject = withContext(Dispatchers.IO) {
        request("/api/missions/today", token = token)
    }

    suspend fun claimMission(token: String, missionId: String): JSONObject = withContext(Dispatchers.IO) {
        request("/api/missions/claim", method = "POST", token = token,
            body = JSONObject().put("missionId", missionId).toString())
    }

    suspend fun fetchCreditPackages(token: String, language: String): JSONObject = withContext(Dispatchers.IO) {
        request("/api/credits/packages?lang=${if (language == "en") "en" else "id"}", token = token)
    }

    suspend fun fetchCreditHistory(token: String, language: String, kind: String, page: Int): JSONObject = withContext(Dispatchers.IO) {
        val requestedKind = if (kind == "xp" || kind == "credit") "&kind=$kind" else ""
        request("/api/credits/history?lang=${if (language == "en") "en" else "id"}&page=${page.coerceAtLeast(1)}&limit=20$requestedKind", token = token)
    }

    private fun request(
        path: String,
        method: String = "GET",
        token: String? = null,
        body: String? = null
    ): JSONObject {
        val builder = Request.Builder()
            .url("$BASE_URL$path")
            .header("Accept", "application/json")
        if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer $token")

        when (method) {
            "POST" -> builder.post((body ?: "{}").toRequestBody(jsonType))
            "GET" -> builder.get()
            else -> throw IllegalArgumentException("Unsupported HTTP method")
        }

        try {
            client.newCall(builder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
                if (!response.isSuccessful) {
                    val message = json.optString("error").ifBlank {
                        "Permintaan gagal (HTTP ${response.code})."
                    }
                    throw ApiException(message, response.code)
                }
                return json
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: IOException) {
            throw IOException(
                "Tidak dapat terhubung ke server NihonGo. Periksa koneksi internet lalu coba lagi.", e
            )
        }
    }

    companion object {
        const val BASE_URL = "https://www.vixnihon.web.id"
    }
}
