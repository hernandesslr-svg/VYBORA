package app.vybora.android

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Cliente HTTP mínimo para a API VYBORA. Nunca usa senha local como autenticação. */
object VyboraApi {
    @Volatile private var applicationContext: Context? = null
    private val refreshLock = Any()

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    data class AuthResult(val username: String, val token: String, val refreshToken: String)

    fun authenticateWithGoogle(idToken: String): AuthResult {
        require(idToken.isNotBlank() && idToken.length <= 10000) { "Credencial Google inválida." }
        val response = request("POST", "/api/auth/google", JSONObject().put("idToken", idToken))
        val user = response.optJSONObject("user") ?: throw IllegalStateException("Resposta inválida do servidor.")
        val token = response.optString("token")
        val name = user.optString("username")
        if (token.isBlank() || name.isBlank()) throw IllegalStateException("O servidor não retornou uma sessão válida.")
        return AuthResult(name, token, response.optString("refreshToken"))
    }

    fun authenticate(register: Boolean, username: String, email: String, password: String): AuthResult {
        val path = if (register) "/api/auth/register" else "/api/auth/login"
        val payload = JSONObject().put("email", email.trim().lowercase()).put("password", password)
        if (register) payload.put("username", username.trim().lowercase())
        val response = request("POST", path, payload)
        val user = response.optJSONObject("user") ?: throw IllegalStateException("Resposta inválida do servidor.")
        val token = response.optString("token")
        val name = user.optString("username")
        if (token.isBlank() || name.isBlank()) throw IllegalStateException("O servidor não retornou uma sessão válida.")
        return AuthResult(name, token, response.optString("refreshToken"))
    }

    fun refreshSession(refreshToken: String): JSONObject {
        require(refreshToken.isNotBlank()) { "Sua sessão precisa ser renovada. Entre novamente na conta uma vez." }
        return request("POST", "/api/auth/refresh", JSONObject().put("refreshToken", refreshToken))
    }

    fun updateProfile(token: String, username: String, displayName: String, bio: String): JSONObject =
        request("PATCH", "/api/users/me",
            JSONObject().put("username", username.trim().lowercase())
                .put("displayName", displayName.trim()).put("bio", bio.trim()), token)

    fun getMyProfile(token: String): JSONObject = request("GET", "/api/auth/me", null, token)

    fun exportMyData(token: String): JSONObject = request("GET", "/api/users/me/export", null, token)

    fun uploadProfileImage(context: Context, token: String, uri: Uri, category: String): String {
        require(category == "avatar" || category == "cover" || category == "cover_original") { "Tipo de imagem de perfil inválido." }
        val contentType = (context.contentResolver.getType(uri) ?: "image/jpeg").lowercase()
        require(contentType in setOf("image/jpeg", "image/png", "image/webp")) { "Escolha uma imagem JPG, PNG ou WebP." }
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                require(total <= 25 * 1024 * 1024) { "A imagem deve ter até 25 MB." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: throw IllegalStateException("Não foi possível ler a imagem selecionada.")
        require(bytes.isNotEmpty()) { "A imagem selecionada está vazia." }
        val prefs = context.getSharedPreferences("vybora_session", Context.MODE_PRIVATE)
        val storedRefreshToken = prefs.getString("refresh_token", null).orEmpty()
        var activeToken = token
        if (storedRefreshToken.isBlank()) {
            // Nunca tenta upload com access token antigo sem credencial de renovação.
            prefs.edit().remove("token").apply()
            try {
                val currentUsername = prefs.getString("username", "").orEmpty()
                val accounts = org.json.JSONArray(prefs.getString("vybora_accounts", "[]") ?: "[]")
                for (i in 0 until accounts.length()) {
                    val account = accounts.optJSONObject(i) ?: continue
                    if (account.optString("username").equals(currentUsername, true)) {
                        account.remove("token")
                        account.remove("refreshToken")
                    }
                }
                prefs.edit().putString("vybora_accounts", accounts.toString()).apply()
            } catch (_: Exception) { }
            throw IllegalStateException("Esta conta não tem um token de renovação válido. Entre novamente com o Google para gerar uma sessão nova e depois salve a foto.")
        }
        if (storedRefreshToken.isNotBlank()) {
            // Aceita tanto o formato da API VYBORA (token/refreshToken) quanto
            // o formato padrão do Supabase Auth (access_token/refresh_token).
            val refreshed = try {
                refreshSession(storedRefreshToken)
            } catch (e: Exception) {
                // O refresh token foi recusado: descarta as credenciais antigas desta conta
                // para que o próximo login gere uma sessão nova, em vez de reutilizar JWT vencido.
                prefs.edit().remove("token").remove("refresh_token").apply()
                try {
                    val currentUsername = prefs.getString("username", "").orEmpty()
                    val accounts = org.json.JSONArray(prefs.getString("vybora_accounts", "[]") ?: "[]")
                    for (i in 0 until accounts.length()) {
                        val account = accounts.optJSONObject(i) ?: continue
                        if (account.optString("username").equals(currentUsername, true)) {
                            account.remove("token")
                            account.remove("refreshToken")
                        }
                    }
                    prefs.edit().putString("vybora_accounts", accounts.toString()).apply()
                } catch (_: Exception) { }
                throw IllegalStateException(
                    "A sessão antiga foi descartada porque o servidor recusou a renovação. Entre novamente com o Google para criar uma sessão nova antes de salvar a foto."
                )
            }
            activeToken = refreshed.optString("token")
                .ifBlank { refreshed.optString("access_token") }
                .ifBlank { refreshed.optString("accessToken") }
            if (activeToken.isBlank()) {
                throw IllegalStateException("O servidor não retornou um token válido ao renovar a sessão.")
            }
            val nextRefreshToken = refreshed.optString("refreshToken")
                .ifBlank { refreshed.optString("refresh_token") }
                .ifBlank { storedRefreshToken }
            prefs.edit().putString("token", activeToken).putString("refresh_token", nextRefreshToken).apply()
            try {
                val currentUsername = prefs.getString("username", "").orEmpty()
                val accounts = org.json.JSONArray(prefs.getString("vybora_accounts", "[]") ?: "[]")
                for (i in 0 until accounts.length()) {
                    val account = accounts.optJSONObject(i) ?: continue
                    if (account.optString("username").equals(currentUsername, true)) {
                        account.put("token", activeToken).put("refreshToken", nextRefreshToken)
                    }
                }
                prefs.edit().putString("vybora_accounts", accounts.toString()).apply()
            } catch (_: Exception) { }
        }
        val fileName = "profile-${category}-${System.currentTimeMillis()}." + when (contentType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val upload = request("POST", "/api/files/upload-url",
            JSONObject().put("fileName", fileName).put("contentType", contentType)
                .put("sizeBytes", bytes.size).put("category", category), activeToken)
        val file = upload.optJSONObject("file") ?: throw IllegalStateException("O servidor não reservou a imagem.")
        val fileId = file.optString("id")
        val uploadUrl = upload.optString("uploadUrl")
        if (fileId.isBlank() || uploadUrl.isBlank()) throw IllegalStateException("Resposta de upload inválida.")
        val connection = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            connectTimeout = 20000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Content-Type", contentType)
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $activeToken")
            setFixedLengthStreamingMode(bytes.size)
        }
        try {
            connection.outputStream.use { it.write(bytes) }
            val status = connection.responseCode
            if (status !in 200..299) {
                val detail = try {
                    (if (status >= 400) connection.errorStream else connection.inputStream)
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                } catch (_: Exception) { "" }
                throw IllegalStateException("Upload da imagem recusado (HTTP $status): " + detail.ifBlank { connection.responseMessage ?: "sem detalhes do servidor" })
            }
        } finally {
            connection.disconnect()
        }
        request("POST", "/api/files/$fileId/complete", JSONObject(), activeToken)
        request("PATCH", "/api/users/me/$category", JSONObject().put("fileId", fileId), activeToken)
        val user = request("GET", "/api/auth/me", null, activeToken).optJSONObject("user")
        return user?.optString(when (category) { "cover" -> "coverUrl"; "cover_original" -> "originalCoverUrl"; else -> "avatarUrl" }).orEmpty()
    }

    fun uploadAvatarMedia(context: Context, token: String, uri: Uri, fileName: String, contentType: String, sizeBytes: Long): String =
        uploadProfileImage(context, token, uri, "avatar")

    data class PostSummary(
        val id: String, val caption: String, val mediaUrl: String, val mediaType: String,
        val username: String, val displayName: String, val likes: Int, val comments: Int,
        val avatarUrl: String = "", val likedByMe: Boolean = false, val isFollowing: Boolean = false,
        val createdAt: String = ""
    )

    fun getPosts(token: String? = null): List<PostSummary> {
        val response = request("GET", "/api/posts?limit=30", null, token)
        val posts = response.optJSONArray("posts") ?: org.json.JSONArray()
        val result = (0 until posts.length()).mapNotNull { index ->
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
                comments = post.optInt("comments"),
                avatarUrl = author.optString("avatarUrl", author.optString("avatar_url")),
                likedByMe = post.optBoolean("likedByMe"),
                isFollowing = post.optBoolean("isFollowing"),
                createdAt = post.optString("createdAt", post.optString("created_at"))
            )
        }
        applicationContext?.let { cachePosts(it, result) }
        return result
    }

    /** Cache local para abrir o feed rapidamente/offline; o servidor continua sendo a fonte oficial. */
    fun getCachedPosts(context: Context): List<PostSummary> {
        return try {
            val raw = context.getSharedPreferences("qbly_local_cache", Context.MODE_PRIVATE)
                .getString("feed_posts_v1", null) ?: return emptyList()
            val array = org.json.JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val post = array.optJSONObject(index) ?: return@mapNotNull null
                PostSummary(
                    id = post.optString("id"), caption = post.optString("caption"),
                    mediaUrl = post.optString("mediaUrl"), mediaType = post.optString("mediaType"),
                    username = post.optString("username"), displayName = post.optString("displayName"),
                    likes = post.optInt("likes"), comments = post.optInt("comments"),
                    avatarUrl = post.optString("avatarUrl"), likedByMe = post.optBoolean("likedByMe"),
                    isFollowing = post.optBoolean("isFollowing")
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun cachePosts(context: Context, posts: List<PostSummary>) {
        try {
            val array = org.json.JSONArray()
            posts.forEach { post ->
                array.put(JSONObject()
                    .put("id", post.id).put("caption", post.caption)
                    .put("mediaUrl", post.mediaUrl).put("mediaType", post.mediaType)
                    .put("username", post.username).put("displayName", post.displayName)
                    .put("likes", post.likes).put("comments", post.comments)
                    .put("avatarUrl", post.avatarUrl).put("likedByMe", post.likedByMe)
                    .put("isFollowing", post.isFollowing))
            }
            context.getSharedPreferences("qbly_local_cache", Context.MODE_PRIVATE)
                .edit().putString("feed_posts_v1", array.toString()).apply()
        } catch (_: Exception) { /* Cache failure must never block the feed. */ }
    }

    fun createTextPost(token: String, caption: String): JSONObject =
        request("POST", "/api/posts", JSONObject().put("caption", caption.trim()).put("mediaType", "text"), token)

    fun createMediaPost(token: String, caption: String, fileId: String, mediaType: String): JSONObject =
        request("POST", "/api/posts", JSONObject().put("caption", caption.trim())
            .put("mediaFileId", fileId).put("mediaType", mediaType), token)

    fun uploadPostMedia(context: Context, token: String, uri: Uri, fileName: String, contentType: String, sizeBytes: Long): String {
        require(sizeBytes in 1..(100L * 1024L * 1024L)) { "O arquivo está vazio ou ultrapassa 100 MB." }
        val requestBody = JSONObject().put("fileName", fileName).put("contentType", contentType)
            .put("sizeBytes", sizeBytes).put("category", "post_media")
        val upload = request("POST", "/api/files/upload-url", requestBody, token)
        val file = upload.optJSONObject("file") ?: throw IllegalStateException("O servidor não reservou o arquivo.")
        val fileId = file.optString("id")
        val uploadUrl = upload.optString("uploadUrl")
        if (fileId.isBlank() || uploadUrl.isBlank()) throw IllegalStateException("Resposta de upload inválida.")
        val connection = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            connectTimeout = 20000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Content-Type", contentType)
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setFixedLengthStreamingMode(sizeBytes)
        }
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Não foi possível ler o arquivo selecionado.")
            input.use { source -> connection.outputStream.use { output -> source.copyTo(output, 64 * 1024) } }
            val status = connection.responseCode
            if (status !in 200..299) throw IllegalStateException("Falha no envio da mídia (HTTP $status).")
        } finally {
            connection.disconnect()
        }
        request("POST", "/api/files/" + fileId + "/complete", JSONObject(), token)
        return fileId
    }

    fun deletePost(token: String, postId: String) {
        request("DELETE", "/api/posts/" + postId, JSONObject(), token)
    }

    fun toggleLike(token: String, postId: String): Boolean =
        request("POST", "/api/posts/" + postId + "/like", JSONObject(), token).optBoolean("liked")

    data class CommentSummary(val id: String, val body: String, val username: String, val displayName: String)

    fun getComments(postId: String): List<CommentSummary> {
        val response = request("GET", "/api/posts/" + postId + "/comments")
        val comments = response.optJSONArray("comments") ?: org.json.JSONArray()
        return (0 until comments.length()).mapNotNull { index ->
            val comment = comments.optJSONObject(index) ?: return@mapNotNull null
            val author = comment.optJSONObject("author") ?: org.json.JSONObject()
            CommentSummary(comment.optString("id"), comment.optString("body"), author.optString("username"), author.optString("displayName"))
        }
    }

    fun addComment(token: String, postId: String, body: String): JSONObject =
        request("POST", "/api/posts/" + postId + "/comments", JSONObject().put("body", body.trim()), token)

    data class InboxSummary(
        val username: String,
        val displayName: String,
        val avatarUrl: String,
        val body: String,
        val createdAt: String,
        val unreadCount: Int
    )

    fun getInbox(token: String): List<InboxSummary> {
        val response = request("GET", "/api/inbox", null, token)
        val items = response.optJSONArray("conversations") ?: org.json.JSONArray()
        return (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            InboxSummary(
                username = item.optString("username"),
                displayName = item.optString("displayName"),
                avatarUrl = item.optString("avatarUrl"),
                body = item.optString("body"),
                createdAt = item.optString("createdAt"),
                unreadCount = item.optInt("unreadCount")
            )
        }
    }

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

    data class NotificationSummary(
        val id: String,
        val kind: String,
        val entityId: String,
        val createdAt: String,
        val readAt: String,
        val actorUsername: String,
        val actorDisplayName: String,
        val actorAvatarUrl: String
    )

    fun getNotifications(token: String): List<NotificationSummary> {
        val response = request("GET", "/api/notifications", null, token)
        val items = response.optJSONArray("notifications") ?: org.json.JSONArray()
        return (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val actor = item.optJSONObject("actor") ?: org.json.JSONObject()
            NotificationSummary(
                id = item.optString("id"),
                kind = item.optString("kind"),
                entityId = item.optString("entityId"),
                createdAt = item.optString("createdAt"),
                readAt = item.optString("readAt"),
                actorUsername = actor.optString("username"),
                actorDisplayName = actor.optString("displayName"),
                actorAvatarUrl = actor.optString("avatarUrl")
            )
        }
    }

    fun markNotificationsRead(token: String) {
        request("POST", "/api/notifications/read", JSONObject(), token)
    }

    data class UserSummary(
        val id: String, val username: String, val displayName: String,
        val bio: String, val avatarUrl: String, val verified: Boolean
    )

    fun toggleFollow(token: String, userId: String): Boolean =
        request("POST", "/api/users/" + userId + "/follow", JSONObject(), token).optBoolean("following")

    fun getPublicProfile(username: String, token: String? = null): JSONObject {
        val name = java.net.URLEncoder.encode(username.trim().removePrefix("@").lowercase(), "UTF-8")
        return request("GET", "/api/users/" + name, null, token)
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

    fun searchUsers(query: String, token: String? = null): List<UserSummary> {
        val normalized = query.trim().removePrefix("@").trim()
        if (normalized.length < 2) return emptyList()
        val q = java.net.URLEncoder.encode(normalized, "UTF-8")
        // Envia a sessão atual para permitir renovação automática caso o backend rejeite o token expirado.
        val response = request("GET", "/api/search?q=" + q + "&limit=30", token = token)
        val users = response.optJSONArray("users") ?: org.json.JSONArray()
        return (0 until users.length()).mapNotNull { index ->
            val user = users.optJSONObject(index) ?: return@mapNotNull null
            val username = user.optString("username").trim()
            if (username.isBlank()) return@mapNotNull null
            UserSummary(
                id = user.optString("id").ifBlank { username },
                username = username,
                displayName = user.optString("displayName", user.optString("display_name")).ifBlank { username },
                bio = user.optString("bio"),
                avatarUrl = user.optString("avatarUrl", user.optString("avatar_url")),
                verified = user.optBoolean("verified", user.optBoolean("is_verified"))
            )
        }.distinctBy { it.username.lowercase() }
    }

    private fun request(method: String, path: String, body: JSONObject? = null, token: String? = null, allowRefresh: Boolean = true): JSONObject {
        val base = BuildConfig.API_BASE_URL.trim().trimEnd('/')
        require(!base.contains("configure-your-vybora-api.example") && (base.startsWith("https://") || base.startsWith("http://10.0.2.2"))) {
            "Backend ainda não configurado. Defina API_BASE_URL no app/build.gradle.kts com a URL HTTPS publicada."
        }
        val connection = (URL(base + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
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
            if (status == 401 && allowRefresh && !token.isNullOrBlank() &&
                path != "/api/auth/refresh" && path != "/api/auth/google" &&
                path != "/api/auth/login" && path != "/api/auth/register") {
                val refreshedToken = renewExpiredSession(token)
                return request(method, path, body, refreshedToken, false)
            }
            if (status !in 200..299) {
                if (path == "/api/auth/google") {
                    // Show the server's exact HTTP status and raw response for Google login debugging.
                    throw IllegalStateException("HTTP $status — resposta do servidor: ${text.ifBlank { "(corpo vazio)" }}")
                }
                val serverMessage = json.optString("error").ifBlank { json.optString("message") }
                val diagnostic = if (path == "/api/posts" || path == "/api/files/upload-url" || path.endsWith("/complete")) {
                    "Falha ao publicar [${method} ${path}, HTTP ${status}]: " +
                        serverMessage.ifBlank { text.take(350).ifBlank { "Resposta vazia do servidor" } }
                } else serverMessage.ifBlank { "Falha na comunicação com o servidor (HTTP $status)." }
                throw IllegalStateException(diagnostic)
            }
            return json
        } catch (e: java.net.SocketTimeoutException) {
            throw IllegalStateException("O servidor demorou para responder. Tente novamente.")
        } catch (e: java.net.UnknownHostException) {
            throw IllegalStateException("Não foi possível encontrar o backend. Verifique a URL e a conexão.")
        } finally {
            connection.disconnect()
        }
    }
    private fun renewExpiredSession(expiredToken: String): String = synchronized(refreshLock) {
        val context = applicationContext
            ?: throw IllegalStateException("A sessão expirou. Abra novamente o aplicativo e entre na sua conta.")
        val prefs = context.getSharedPreferences("vybora_session", Context.MODE_PRIVATE)
        val currentToken = prefs.getString("token", null).orEmpty()
        if (currentToken.isNotBlank() && currentToken != expiredToken) return@synchronized currentToken
        val refreshToken = prefs.getString("refresh_token", null).orEmpty()
        if (refreshToken.isBlank()) {
            throw IllegalStateException("Sua sessão expirou. Entre novamente com o Google para continuar.")
        }
        val result = try {
            request("POST", "/api/auth/refresh", JSONObject().put("refreshToken", refreshToken), null, false)
        } catch (_: Exception) {
            throw IllegalStateException("Sua sessão expirou. Entre novamente com o Google para continuar.")
        }
        val newToken = result.optString("token").ifBlank { result.optString("access_token") }
        val newRefreshToken = result.optString("refreshToken").ifBlank { result.optString("refresh_token") }
        if (newToken.isBlank() || newRefreshToken.isBlank()) {
            throw IllegalStateException("O servidor não conseguiu renovar sua sessão. Entre novamente com o Google.")
        }
        prefs.edit().putString("token", newToken).putString("refresh_token", newRefreshToken).apply()
        try {
            val accounts = org.json.JSONArray(prefs.getString("vybora_accounts", "[]") ?: "[]")
            val username = prefs.getString("username", "").orEmpty()
            for (i in 0 until accounts.length()) {
                val account = accounts.optJSONObject(i) ?: continue
                if (account.optString("username").equals(username, true)) {
                    account.put("token", newToken)
                    account.put("refreshToken", newRefreshToken)
                }
            }
            prefs.edit().putString("vybora_accounts", accounts.toString()).apply()
        } catch (_: Exception) { }
        newToken
    }

}
