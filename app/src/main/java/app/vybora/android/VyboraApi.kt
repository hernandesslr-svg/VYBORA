package app.vybora.android

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Cliente HTTP mínimo para a API VYBORA. Nunca usa senha local como autenticação. */
object VyboraApi {
    data class AuthResult(val username: String, val token: String)

    fun authenticate(register: Boolean, username: String, email: String, password: String): AuthResult {
        val path = if (register) "/api/auth/register" else "/api/auth/login"
        val payload = JSONObject().put("email", email.trim().lowercase()).put("password", password)
        if (register) payload.put("username", username.trim().lowercase())
        val response = request("POST", path, payload)
        val user = response.optJSONObject("user") ?: throw IllegalStateException("Resposta inválida do servidor.")
        val token = response.optString("token")
        val name = user.optString("username")
        if (token.isBlank() || name.isBlank()) throw IllegalStateException("O servidor não retornou uma sessão válida.")
        return AuthResult(name, token)
    }

    fun updateProfile(token: String, displayName: String, bio: String): JSONObject =
        request("PATCH", "/api/users/me", JSONObject().put("displayName", displayName).put("bio", bio), token)

    fun getMyProfile(token: String): JSONObject = request("GET", "/api/auth/me", null, token)

    private fun request(method: String, path: String, body: JSONObject? = null, token: String? = null): JSONObject {
        val base = BuildConfig.API_BASE_URL.trim().trimEnd('/')
        require(!base.contains("configure-your-vybora-api.example") && (base.startsWith("https://") || base.startsWith("http://10.0.2.2"))) {
            "Backend ainda não configurado. Defina API_BASE_URL no app/build.gradle.kts com a URL HTTPS publicada."
        }
        val connection = (URL(base + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = try { JSONObject(text) } catch (_: Exception) { JSONObject() }
            if (status !in 200..299) throw IllegalStateException(json.optString("error").ifBlank { "Falha na comunicação com o servidor (HTTP $status)." })
            return json
        } catch (e: java.net.SocketTimeoutException) {
            throw IllegalStateException("O servidor demorou para responder. Tente novamente.")
        } catch (e: java.net.UnknownHostException) {
            throw IllegalStateException("Não foi possível encontrar o backend. Verifique a URL e a conexão.")
        } finally {
            connection.disconnect()
        }
    }
}
