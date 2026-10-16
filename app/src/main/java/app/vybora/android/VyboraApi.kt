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

    fun updateProfile(token: String, username: String, displayName: String, bio: String): JSONObject =
        request("PATCH", "/api/users/me",
            JSONObject().put("username", username.trim().lowercase())
                .put("displayName", displayName.trim()).put("bio", bio.trim()), token)

    fun getMyProfile(token: String): JSONObject = request("GET", "/api/auth/me", null, token)

    data class PostSummary(
        val id: String, val caption: String, val mediaUrl: String, val mediaType: String,
        val username: String, val displayName: String, val likes: Int, val comments: Int
    )

    fun getPosts(): List<PostSummary> {
        val response = request("GET", "/api/posts?limit=30")
        val posts = response.optJSONArray("posts") ?: org.json.JSONArray()
        return (0 until posts.length()).mapNotNull { index ->
            val post = posts.optJSONObject(index) ?: return@mapNotNull null
            val author = post.optJSONObject("author") ?: org.json.JSONObject()
            PostSummary(
                id = post.optString("id"),
                caption = post.optString("caption"),
                mediaUrl = post.optString("mediaUrl"),
                mediaType = post.optString("mediaType"),
                username = author.optString("username"),
                displayName = author.optString("displayName", author.optString("display_name")),
                likes = post.optInt("likes"),
                comments = post.optInt("comments")
            )
        }
    }

    fun createTextPost(token: String, caption: String): JSONObject =
        request("POST", "/api/posts", JSONObject().put("caption", caption.trim()).put("mediaType", "text"), token)

    fun toggleLike(token: String, postId: String): Boolean =
        request("POST", "/api/posts/" + postId + "/like", JSONObject(), token).optBoolean("liked")

    fun addComment(token: String, postId: String, body: String): JSONObject =
        request("POST", "/api/posts/" + postId + "/comments", JSONObject().put("body", body.trim()), token)

    data class MessageSummary(val id: String, val body: String, val senderId: String, val createdAt: String)

    fun getMessages(token: String, username: String): List<MessageSummary> {
        val name = java.net.URLEncoder.encode(username.trim().removePrefix("@").lowercase(), "UTF-8")
        val response = request("GET", "/api/messages/" + name, null, token)
        val items = response.optJSONArray("messages") ?: org.json.JSONArray()
        return (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            MessageSummary(item.optString("id"), item.optString("body"), item.optString("senderId"), item.optString("createdAt"))
        }
    }

    fun sendMessage(token: String, username: String, body: String): JSONObject {
        val name = java.net.URLEncoder.encode(username.trim().removePrefix("@").lowercase(), "UTF-8")
        return request("POST", "/api/messages/" + name, JSONObject().put("body", body.trim()), token)
    }

    data class UserSummary(
        val id: String, val username: String, val displayName: String,
        val bio: String, val avatarUrl: String, val verified: Boolean
    )

    fun toggleFollow(token: String, userId: String): Boolean =
        request("POST", "/api/users/" + userId + "/follow", JSONObject(), token).optBoolean("following")

    fun getPublicProfile(username: String): JSONObject {
        val name = java.net.URLEncoder.encode(username.trim().removePrefix("@").lowercase(), "UTF-8")
        return request("GET", "/api/users/" + name)
    }

    fun getConnections(username: String, type: String): List<UserSummary> {
        require(type == "followers" || type == "following") { "Tipo de lista inválido." }
        val name = java.net.URLEncoder.encode(username.trim().removePrefix("@").lowercase(), "UTF-8")
        val response = request("GET", "/api/users/" + name + "/" + type)
        val users = response.optJSONArray("users") ?: org.json.JSONArray()
        return (0 until users.length()).mapNotNull { index ->
            val user = users.optJSONObject(index) ?: return@mapNotNull null
            UserSummary(
                id = user.optString("id"), username = user.optString("username"),
                displayName = user.optString("displayName", user.optString("display_name")),
                bio = user.optString("bio"), avatarUrl = user.optString("avatarUrl", user.optString("avatar_url")),
                verified = user.optBoolean("verified", user.optBoolean("is_verified"))
            )
        }
    }

    fun searchUsers(query: String): List<UserSummary> {
        val q = java.net.URLEncoder.encode(query.trim().removePrefix("@"), "UTF-8")
        val response = request("GET", "/api/users/search?q=" + q)
        val users = response.optJSONArray("users") ?: org.json.JSONArray()
        return (0 until users.length()).mapNotNull { index ->
            val user = users.optJSONObject(index) ?: return@mapNotNull null
            UserSummary(
                id = user.optString("id"),
                username = user.optString("username"),
                displayName = user.optString("displayName", user.optString("display_name")),
                bio = user.optString("bio"),
                avatarUrl = user.optString("avatarUrl", user.optString("avatar_url")),
                verified = user.optBoolean("verified", user.optBoolean("is_verified"))
            )
        }
    }

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
