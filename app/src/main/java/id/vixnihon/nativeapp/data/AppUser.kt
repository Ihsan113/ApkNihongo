package id.vixnihon.nativeapp.data

import org.json.JSONObject

data class AppUser(
    val id: String,
    val name: String,
    val email: String,
    val picture: String = ""
) {
    fun toJson(): String = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("email", email)
        .put("picture", picture)
        .toString()

    companion object {
        fun fromJson(json: JSONObject): AppUser = AppUser(
            id = json.optString("id", ""),
            name = json.optString("name", ""),
            email = json.optString("email", ""),
            picture = json.optString("avatarUrl", json.optString("picture", ""))
        )
    }
}

data class AuthSession(val token: String, val user: AppUser)
data class GoogleAuthConfig(val configured: Boolean, val clientId: String)
