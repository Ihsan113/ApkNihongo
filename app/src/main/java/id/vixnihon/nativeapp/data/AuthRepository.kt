package id.vixnihon.nativeapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
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

    /** Web LearnScreen energy gate: GET is read-only, POST charges at most once server-side. */
    suspend fun lessonAccess(token: String, lessonId: String): JSONObject = withContext(Dispatchers.IO) {
        request("/api/content/access/lesson/${encodeSegment(lessonId)}", token = token)
    }

    suspend fun startLesson(token: String, lessonId: String): JSONObject = withContext(Dispatchers.IO) {
        request("/api/content/access/lesson/${encodeSegment(lessonId)}/start", method = "POST", token = token, body = "{}")
    }

    suspend fun saveLessonProgress(token: String, lessonId: String, stars: Int, passed: Boolean,
        score: Int, total: Int): JSONObject = withContext(Dispatchers.IO) {
        val passing = kotlin.math.max(1, kotlin.math.ceil(total * 0.8).toInt())
        request("/api/progress/lesson", method = "POST", token = token, body = JSONObject()
            .put("lessonId", lessonId).put("stars", stars).put("passed", passed)
            .put("score", score).put("total", total).put("passScore", passing).toString())
    }

    suspend fun studyHeartbeat(token: String): JSONObject = withContext(Dispatchers.IO) {
        request("/api/activity/study-heartbeat", method = "POST", token = token,
            body = JSONObject().put("screen", "lesson").toString())
    }

    suspend fun reportLessonQuiz(token: String, score: Int, total: Int, lessonId: String): JSONObject = withContext(Dispatchers.IO) {
        request("/api/activity/quiz", method = "POST", token = token,
            body = JSONObject().put("source", "quiz_lesson").put("score", score)
                .put("total", total).put("category", lessonId.take(80)).toString())
    }

    suspend fun saveWrongLessonAnswers(token: String, entries: org.json.JSONArray): JSONObject = withContext(Dispatchers.IO) {
        request("/api/wrong-answers", method = "POST", token = token,
            body = JSONObject().put("items", entries).toString())
    }

    suspend fun fetchWallet(token: String): JSONObject = withContext(Dispatchers.IO) {
        request("/api/wallet", token = token)
    }

    /** Purchase and ledger endpoints mirror the web checkout. The server alone owns amounts and status. */
    suspend fun fetchPaymentMethods(token: String): JSONObject = withContext(Dispatchers.IO) {
        request("/api/credits/payment-methods", token = token)
    }

    suspend fun createCreditOrder(token: String, packageId: String, language: String): JSONObject = withContext(Dispatchers.IO) {
        request("/api/credits/purchase/create", method = "POST", token = token,
            body = JSONObject().put("packageId", packageId).put("lang", if (language == "en") "en" else "id").toString())
    }

    suspend fun createPremiumOrder(token: String, language: String): JSONObject = withContext(Dispatchers.IO) {
        request("/api/credits/premium/purchase/create", method = "POST", token = token,
            body = JSONObject().put("lang", if (language == "en") "en" else "id").toString())
    }

    suspend fun fetchCreditOrder(token: String, orderId: String, language: String): JSONObject = withContext(Dispatchers.IO) {
        request("/api/credits/purchase/status/${encodeSegment(orderId)}?lang=${if (language == "en") "en" else "id"}", token = token)
    }

    suspend fun cancelCreditOrder(token: String, orderId: String, language: String): JSONObject = withContext(Dispatchers.IO) {
        request("/api/credits/purchase/cancel", method = "POST", token = token,
            body = JSONObject().put("orderId", orderId).put("lang", if (language == "en") "en" else "id").toString())
    }

    suspend fun fetchCreditQrImage(token: String, orderId: String): ByteArray = withContext(Dispatchers.IO) {
        val builder = Request.Builder()
            .url("$BASE_URL/api/credits/purchase/qr-image/${encodeSegment(orderId)}")
            .header("Accept", "image/*")
        if (token.isNotBlank()) builder.header("Authorization", "Bearer $token")
        try {
            client.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful) throw ApiException("QRIS tidak tersedia (HTTP ${response.code}).", response.code)
                val type = response.header("Content-Type").orEmpty().lowercase(Locale.ROOT)
                if (!type.startsWith("image/")) throw ApiException("Respons QRIS bukan gambar.")
                val content = response.body?.bytes() ?: throw ApiException("Gambar QRIS kosong.")
                if (content.size > 4_000_000 || content.isEmpty()) throw ApiException("Ukuran gambar QRIS tidak valid.")
                content
            }
        } catch (e: IOException) {
            throw IOException("Gagal mengambil QRIS. Periksa koneksi dan coba lagi.", e)
        }
    }

    suspend fun fetchCreditHistoryFiltered(
        token: String, language: String, kind: String, page: Int, from: String, to: String
    ): JSONObject = withContext(Dispatchers.IO) {
        val filters = buildList {
            add("lang=${if (language == "en") "en" else "id"}")
            add("page=${page.coerceAtLeast(1)}")
            add("limit=20")
            if (kind == "credit" || kind == "xp") add("kind=$kind")
            if (from.isNotBlank()) add("from=${encodeSegment(from)}")
            if (to.isNotBlank()) add("to=${encodeSegment(to)}")
        }.joinToString("&")
        request("/api/credits/history?$filters", token = token)
    }

    private fun encodeSegment(value: String) = URLEncoder.encode(value.trim(), StandardCharsets.UTF_8.name())

    /** Match the authenticated SettingsScreen.jsx API contracts. */
    suspend fun sendFeedback(token: String, rating: Int?, message: String): JSONObject = withContext(Dispatchers.IO) {
        request("/api/feedback", method = "POST", token = token,
            body = JSONObject().put("rating", rating).put("message", message.take(2000)).put("pageContext", "settings").toString())
    }

    /** Irreversible; only call after the native CONFIRM + final dialog. */
    suspend fun deleteAccount(token: String): JSONObject = withContext(Dispatchers.IO) {
        request("/api/auth/delete-account", method = "POST", token = token,
            body = JSONObject().put("confirmation", "CONFIRM").toString())
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
