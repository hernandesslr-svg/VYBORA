#!/usr/bin/env python3
"""Apply reviewed social features to the Build 72 source archive without replacing Google auth."""
from pathlib import Path
import re
import sys

root = Path(sys.argv[1]).resolve()
main_files = list(root.rglob("MainActivity.kt"))
api_files = list(root.rglob("VyboraApi.kt"))
if len(main_files) != 1 or len(api_files) != 1:
    raise SystemExit(f"Expected one MainActivity.kt and one VyboraApi.kt; found {len(main_files)} and {len(api_files)}")
main_path, api_path = main_files[0], api_files[0]
main = main_path.read_text(encoding="utf-8")
api = api_path.read_text(encoding="utf-8")

def once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Patch {label}: expected exactly one match, got {count}")
    return text.replace(old, new, 1)

# API functions are added to the existing client; Google auth and its request implementation remain untouched.
api_marker = "    private fun request(method: String, path: String, body: JSONObject? = null, token: String? = null): JSONObject {"
api_add = """    fun getPosts(): List<JSONObject> {
        val response = request("GET", "/api/posts?limit=30")
        val array = response.optJSONArray("posts") ?: org.json.JSONArray()
        return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
    }

    fun toggleLike(token: String, postId: String): Boolean =
        request("POST", "/api/posts/" + postId + "/like", JSONObject(), token).optBoolean("liked")

    fun getMessages(token: String, username: String): List<JSONObject> {
        val name = URLEncoder.encode(username.trim().removePrefix("@").lowercase(), "UTF-8")
        val response = request("GET", "/api/messages/" + name, null, token)
        val array = response.optJSONArray("messages") ?: org.json.JSONArray()
        return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
    }

    fun sendMessage(token: String, username: String, body: String): JSONObject {
        val name = URLEncoder.encode(username.trim().removePrefix("@").lowercase(), "UTF-8")
        return request("POST", "/api/messages/" + name, JSONObject().put("body", body.trim()), token)
    }

    fun getPublicProfile(username: String): JSONObject {
        val name = URLEncoder.encode(username.trim().removePrefix("@").lowercase(), "UTF-8")
        return request("GET", "/api/users/" + name)
    }

"""
if "fun getPosts():" not in api:
    api = once(api, api_marker, api_add + api_marker, "add backend feed and messaging methods")
api = api.replace(
    'fun updateProfile(token: String, displayName: String, bio: String): JSONObject =\n        request("PATCH", "/api/users/me", JSONObject().put("displayName", displayName).put("bio", bio), token)',
    'fun updateProfile(token: String, displayName: String, bio: String, username: String = ""): JSONObject =\n        request("PATCH", "/api/users/me", JSONObject().put("displayName", displayName).put("bio", bio).put("username", username.trim().lowercase()).also { if (username.isBlank()) it.remove("username") }, token)'
)
api_path.write_text(api, encoding="utf-8")

# Remove the false "coming later" notice from the plus button and use a real feed on Home.
main = once(
    main,
    'NavigationBarItem(selected = tab == 2, onClick = { tab = 2; notice = "Publicação: a seleção e o envio de vídeos serão conectados na próxima etapa." }, icon = { Icon(Icons.Default.Add, null) }, label = { Text("Publicar") })',
    'NavigationBarItem(selected = tab == 2, onClick = { tab = 2; notice = "" }, icon = { Icon(Icons.Default.Add, null) }, label = { Text("Publicar") })',
    "remove add-tab placeholder"
)
main = once(
    main,
    '''                    if (tab == 4 && !showSettings && !showEditProfile) {
                        IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Menu, contentDescription = "Abrir menu do perfil e configurações", tint = Color.White) }
                    } else {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações", tint = Color.White, modifier = Modifier.size(25.dp).clickable { showSettings = true })
                    }''',
    '''                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Abrir menu e configurações", tint = Color.White)
                    }''',
    "unify hamburger menu"
)
main = once(
    main,
    '0 -> SimplePage("Seu mundo, sem limites.", "Descubra criadores, acompanhe as lives e encontre novos vídeos.", "Explorar vídeos") { tab = 1 }',
    '0 -> FeedPage(onNotice = { notice = it })',
    "connect Home feed"
)
main = main.replace('Text("Configurar transmissão")', 'Text("Iniciar transmissão")')
main = main.replace('item { Text("Conteúdo ilustrativo: os vídeos acima são cartões de demonstração, não publicações reais. O feed público real será ligado ao armazenamento e às contas na próxima etapa.", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 12.dp)) }', '')

# Replace the disconnected message placeholder with authenticated persistent conversations.
start = main.find("@Composable\nprivate fun MessagesPage(")
end = main.find("@Composable\nprivate fun ProfilePage(", start)
if start < 0 or end < 0:
    raise SystemExit("Could not locate MessagesPage/ProfilePage boundaries")
messages = """@Composable
private fun MessagesPage(onOpenMessage: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("vybora_session", android.content.Context.MODE_PRIVATE) }
    var recipient by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var activeRecipient by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Mensagens", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = recipient, onValueChange = { recipient = it.take(24) }, label = { Text("@usuário") }, modifier = Modifier.weight(1f), singleLine = true)
            Spacer(Modifier.width(8.dp))
            Button(enabled = !loading && recipient.trim().removePrefix("@").length >= 3, onClick = {
                val token = prefs.getString("token", null)
                if (token.isNullOrBlank()) { error = "Entre na sua conta para acessar mensagens." }
                else {
                    loading = true; error = ""
                    Thread {
                        try {
                            val result = VyboraApi.getMessages(token, recipient)
                            android.os.Handler(android.os.Looper.getMainLooper()).post { messages = result; activeRecipient = recipient.trim().removePrefix("@"); loading = false }
                        } catch (e: Exception) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post { loading = false; error = e.message ?: "Não foi possível abrir a conversa." }
                        }
                    }.start()
                }
            }) { Text("Abrir") }
        }
        if (error.isNotBlank()) Text(error, color = Pink)
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { message ->
                Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(message.optString("body"))
                        Text(message.optString("createdAt"), color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = draft, onValueChange = { draft = it.take(5000) }, label = { Text("Mensagem") }, modifier = Modifier.weight(1f), enabled = !loading)
            Spacer(Modifier.width(8.dp))
            IconButton(enabled = !loading && draft.isNotBlank() && activeRecipient.isNotBlank(), onClick = {
                val token = prefs.getString("token", null)
                if (token.isNullOrBlank()) { error = "Entre na sua conta para enviar mensagens." }
                else {
                    loading = true
                    Thread {
                        try {
                            VyboraApi.sendMessage(token, activeRecipient, draft)
                            val result = VyboraApi.getMessages(token, activeRecipient)
                            android.os.Handler(android.os.Looper.getMainLooper()).post { messages = result; draft = ""; loading = false }
                        } catch (e: Exception) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post { loading = false; error = e.message ?: "Não foi possível enviar a mensagem." }
                        }
                    }.start()
                }
            }) { Icon(Icons.Default.Add, contentDescription = "Enviar mensagem") }
        }
    }
}

"""
main = main[:start] + messages + main[end:]

# Make the account's public username editable and persist it on the backend.
main = once(
    main,
    'private fun EditProfilePage(initialName: String, initialBio: String, onBack: () -> Unit, onSaved: (String, String) -> Unit, onNotice: (String) -> Unit) {',
    'private fun EditProfilePage(initialUsername: String, initialName: String, initialBio: String, onBack: () -> Unit, onSaved: (String, String, String) -> Unit, onNotice: (String) -> Unit) {',
    "edit-profile signature"
)
main = once(main, '    var name by remember { mutableStateOf(initialName) }', '    var username by remember { mutableStateOf(initialUsername) }\n    var name by remember { mutableStateOf(initialName) }', "edit-profile username state")
main = once(
    main,
    '        OutlinedTextField(value = name, onValueChange = { name = it.take(80) }, label = { Text("Nome de exibição") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !loading)',
    '        OutlinedTextField(value = username, onValueChange = { username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == \'_\' || ch == \'.\' }.take(24) }, label = { Text("@Nome de usuário") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !loading)\n        OutlinedTextField(value = name, onValueChange = { name = it.take(80) }, label = { Text("Nome de exibição") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !loading)',
    "edit-profile username field"
)
main = main.replace('if (!token.isNullOrBlank()) VyboraApi.updateProfile(token, name.trim(), bio.trim())', 'if (!token.isNullOrBlank()) VyboraApi.updateProfile(token, name.trim(), bio.trim(), username.trim())')
main = main.replace('onSaved(name.trim(), bio.trim())', 'onSaved(username.trim(), name.trim(), bio.trim())')
main = once(
    main,
    '                        initialName = profileName,\n                        initialBio = profileBio,',
    '                        initialUsername = loggedInUser,\n                        initialName = profileName,\n                        initialBio = profileBio,',
    "edit-profile call arguments"
)
main = once(
    main,
    '                        onSaved = { name, bio ->\n                            profileName = name; profileBio = bio\n                            appPrefs.edit().putString("display_name", name).putString("bio", bio).apply()',
    '                        onSaved = { username, name, bio ->\n                            loggedInUser = username; profileName = name; profileBio = bio\n                            appPrefs.edit().putString("username", username).putString("display_name", name).putString("bio", bio).apply()',
    "persist updated username"
)
profile_sig = 'private fun ProfilePage(username: String, displayName: String, bio: String, onNotice: (String) -> Unit, onStat: (String) -> Unit, onAuth: () -> Unit, onEdit: () -> Unit) {\n    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {'
profile_new = '''private fun ProfilePage(username: String, displayName: String, bio: String, onNotice: (String) -> Unit, onStat: (String) -> Unit, onAuth: () -> Unit, onEdit: () -> Unit) {
    var postCount by remember { mutableIntStateOf(0) }
    var followerCount by remember { mutableIntStateOf(0) }
    var followingCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(username) {
        if (username.isNotBlank()) Thread {
            try {
                val response = VyboraApi.getPublicProfile(username)
                val user = response.optJSONObject("user")
                if (user != null) android.os.Handler(android.os.Looper.getMainLooper()).post {
                    postCount = user.optInt("posts_count")
                    followerCount = user.optInt("followers")
                    followingCount = user.optInt("following")
                }
            } catch (_: Exception) { }
        }.start()
    }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {'''
main = once(main, profile_sig, profile_new, "load real profile statistics")
main = main.replace('ProfileStat("Publicações", "0") { onStat("Publicações") }', 'ProfileStat("Publicações", postCount.toString()) { onStat("Publicações") }')
main = main.replace('ProfileStat("Seguidores", "0") { onStat("Seguidores") }', 'ProfileStat("Seguidores", followerCount.toString()) { onStat("Seguidores") }')
main = main.replace('ProfileStat("Seguindo", "0") { onStat("Seguindo") }', 'ProfileStat("Seguindo", followingCount.toString()) { onStat("Seguindo") }')

# Real Home feed, using the existing backend API and session token.
feed = """@Composable
private fun FeedPage(onNotice: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("vybora_session", android.content.Context.MODE_PRIVATE) }
    var posts by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(refreshKey) {
        loading = true
        Thread {
            try {
                val result = VyboraApi.getPosts()
                android.os.Handler(android.os.Looper.getMainLooper()).post { posts = result; loading = false }
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post { loading = false; onNotice(e.message ?: "Não foi possível carregar o feed.") }
            }
        }.start()
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Seu feed", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = { refreshKey++ }) { Text("Atualizar") }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (!loading && posts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Ainda não há publicações. Volte mais tarde ou publique a primeira.", color = Muted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(posts) { post ->
                    val author = post.optJSONObject("author") ?: JSONObject()
                    Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(author.optString("displayName").ifBlank { author.optString("username") }.ifBlank { "VYBORA" }, fontWeight = FontWeight.Bold)
                            if (author.optString("username").isNotBlank()) Text("@${author.optString("username")}", color = Muted, style = MaterialTheme.typography.labelSmall)
                            if (post.optString("caption").isNotBlank()) Text(post.optString("caption"))
                            if (post.optString("mediaUrl").isNotBlank()) Text("Publicação com mídia", color = Muted, style = MaterialTheme.typography.bodySmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    val token = prefs.getString("token", null)
                                    if (token.isNullOrBlank()) onNotice("Entre na sua conta para curtir.")
                                    else Thread {
                                        try {
                                            VyboraApi.toggleLike(token, post.optString("id"))
                                            val result = VyboraApi.getPosts()
                                            android.os.Handler(android.os.Looper.getMainLooper()).post { posts = result }
                                        } catch (e: Exception) {
                                            android.os.Handler(android.os.Looper.getMainLooper()).post { onNotice(e.message ?: "Não foi possível curtir.") }
                                        }
                                    }.start()
                                }) { Icon(Icons.Default.FavoriteBorder, contentDescription = "Curtir") }
                                Text("${post.optInt("likes")} curtidas", color = Muted, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.width(14.dp))
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Muted)
                                Spacer(Modifier.width(5.dp))
                                Text("${post.optInt("comments")} comentários", color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

"""
msg_marker = "@Composable\nprivate fun MessagesPage("
if "private fun FeedPage(" not in main:
    main = once(main, msg_marker, feed + msg_marker, "add backend Home feed")

# Hydrate account fields from the backend whenever the app starts with a saved session.
marker = '    LaunchedEffect(resetToken) { if (!resetToken.isNullOrBlank()) showAuth = true }'
hydration = marker + """
    LaunchedEffect(Unit) {
        val token = appPrefs.getString("token", null)
        if (!token.isNullOrBlank()) Thread {
            try {
                val response = VyboraApi.getMyProfile(token)
                val user = response.optJSONObject("user")
                if (user != null) {
                    val serverUsername = user.optString("username")
                    val serverName = user.optString("displayName", user.optString("display_name"))
                    val serverBio = user.optString("bio")
                    appPrefs.edit().putString("username", serverUsername).putString("display_name", serverName).putString("bio", serverBio).apply()
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        if (serverUsername.isNotBlank()) loggedInUser = serverUsername
                        profileName = serverName; profileBio = serverBio
                    }
                }
            } catch (_: Exception) { }
        }.start()
    }"""
main = once(main, marker, hydration, "restore profile from API")
main_path.write_text(main, encoding="utf-8")
print("Applied Build 72 social feature patches:", main_path, api_path)
