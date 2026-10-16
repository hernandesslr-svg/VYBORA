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

# Build 72's archived VyboraApp may not yet declare the cached profile fields.
# The edit-profile patch below references them in VyboraApp, so define them
# in that composable using the same preferences store before applying UI edits.
if "var profileName by remember" not in main:
    match = re.search(r"(?m)^(\s*var loggedInUser by remember[^\n]*\n)", main)
    if not match:
        raise SystemExit("Could not find loggedInUser state in Build 72 VyboraApp; refusing to add profile state in the wrong scope.")
    profile_state = (
        '    var profileName by remember { mutableStateOf(appPrefs.getString("display_name", "") ?: "") }\n'
        '    var profileBio by remember { mutableStateOf(appPrefs.getString("bio", "") ?: "") }\n'
    )
    main = main[:match.end()] + profile_state + main[match.end():]
    print("Applied: declare cached profileName/profileBio state in VyboraApp")

def once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Patch {label}: expected exactly one match, got {count}")
    return text.replace(old, new, 1)

def maybe(text, old, new, label):
    count = text.count(old)
    if count == 1:
        print("Applied:", label)
        return text.replace(old, new, 1)
    print("Skipped optional patch:", label, "(matches:", count, ")")
    return text

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
# Use the signed-upload protocol implemented by the current Railway backend.
upload_start = api.find("    fun uploadMedia(")
upload_end = api.find("    fun createPost(", upload_start)
if upload_start < 0 or upload_end < 0:
    raise SystemExit("Could not locate uploadMedia/createPost in Build 72 API client.")
upload_impl = """    fun uploadMedia(contentResolver: ContentResolver, uri: Uri, token: String, category: String = "post_media"): JSONObject {
        val mime = contentResolver.getType(uri) ?: if (uri.toString().endsWith(".mp4", true)) "video/mp4" else "image/jpeg"
        if (!mime.startsWith("image/") && !mime.startsWith("video/")) throw IllegalStateException("Escolha uma foto ou vídeo.")
        val rawName = uri.lastPathSegment?.substringAfterLast('/')?.takeLast(120)?.ifBlank { "midia" } ?: "midia.jpg"
        val descriptorLength = try { contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L } catch (_: Exception) { -1L }
        val sizeBytes = if (descriptorLength > 0) descriptorLength else if (uri.scheme == "file") java.io.File(uri.path ?: "").length() else -1L
        if (sizeBytes <= 0L) throw IllegalStateException("Não foi possível determinar o tamanho do arquivo.")
        val reservation = request("POST", "/api/files/upload-url", JSONObject()
            .put("fileName", rawName).put("contentType", mime).put("sizeBytes", sizeBytes).put("category", category), token)
        val file = reservation.optJSONObject("file") ?: throw IllegalStateException("O servidor não reservou o arquivo.")
        val fileId = file.optString("id")
        val uploadUrl = reservation.optString("uploadUrl")
        if (fileId.isBlank() || uploadUrl.isBlank()) throw IllegalStateException("Resposta de upload inválida.")
        val connection = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"; connectTimeout = 20000; readTimeout = 60000; doOutput = true
            setRequestProperty("Content-Type", mime)
            setFixedLengthStreamingMode(sizeBytes)
        }
        try {
            val input = contentResolver.openInputStream(uri) ?: throw IllegalStateException("Não foi possível abrir o arquivo escolhido.")
            input.use { source -> connection.outputStream.use { sink -> source.copyTo(sink, 64 * 1024) } }
            val status = connection.responseCode
            if (status !in 200..299) throw IllegalStateException("Falha ao enviar mídia (HTTP $status).")
        } finally { connection.disconnect() }
        request("POST", "/api/files/" + fileId + "/complete", JSONObject(), token)
        return file
    }

"""
api = api[:upload_start] + upload_impl + api[upload_end:]
api_path.write_text(api, encoding="utf-8")

# Remove the false "coming later" notice from the plus button and use a real feed on Home.
old_add = 'NavigationBarItem(selected = tab == 2, onClick = { tab = 2; notice = "Publicação: a seleção e o envio de vídeos serão conectados na próxima etapa." }, icon = { Icon(Icons.Default.Add, null) }, label = { Text("Publicar") })'
new_add = 'NavigationBarItem(selected = tab == 2, onClick = { tab = 2; notice = "" }, icon = { Icon(Icons.Default.Add, null) }, label = { Text("Publicar") })'
if old_add in main:
    main = once(main, old_add, new_add, "remove add-tab placeholder")
elif 'NavigationBarItem(selected = tab == 2, onClick = { showSettings = false; showSearch = false; showEditProfile = false; tab = 2; notice = "" }, icon = { Icon(Icons.Default.Add, null) }, label = { Text("Publicar") })' not in main:
    raise SystemExit("Could not locate add-tab handler after the Build 72 UI fixes.")
main = maybe(
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
home_demo = '0 -> SimplePage("Seu mundo, sem limites.", "Descubra criadores, acompanhe as lives e encontre novos vídeos.", "Explorar vídeos") { tab = 1 }'
home_ui_fix = '''0 -> Column(
                        Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(14.dp))
                        Text("Ainda não há vídeos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text("Quando a comunidade publicar vídeos, eles aparecerão aqui.", color = Muted, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = { showSettings = false; tab = 2; notice = "" }) { Text("Publicar vídeo") }
                    }'''
if home_demo in main:
    main = once(main, home_demo, '0 -> FeedPage(onNotice = { notice = it })', "connect Home feed")
elif home_ui_fix in main:
    main = once(main, home_ui_fix, '0 -> FeedPage(onNotice = { notice = it })', "connect Home feed")
elif '0 -> FeedPage(onNotice = { notice = it })' not in main:
    raise SystemExit("Could not locate Home demo placeholder after the Build 72 UI fixes.")
main = main.replace('Text("Configurar transmissão")', 'Text("Iniciar transmissão")')
main = main.replace('item { Text("Conteúdo ilustrativo: os vídeos acima são cartões de demonstração, não publicações reais. O feed público real será ligado ao armazenamento e às contas na próxima etapa.", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 12.dp)) }', '')

# Make the plus button open the system camera immediately, while keeping gallery alternatives.
if "import android.graphics.Bitmap" not in main:
    main = main.replace("import android.net.Uri", "import android.net.Uri\nimport android.graphics.Bitmap\nimport android.content.Intent\nimport coil.compose.AsyncImage\nimport java.io.File")
post_start = main.find("@Composable\nprivate fun PostPage(")
post_end = main.find("@Composable\nprivate fun LivePage(", post_start)
if post_start < 0 or post_end < 0:
    raise SystemExit("Could not locate PostPage/LivePage boundaries for camera patch.")
post = main[post_start:post_end]
camera_anchor = '    val context = androidx.compose.ui.platform.LocalContext.current'
camera_impl = camera_anchor + """
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val file = File(context.cacheDir, "vybora-camera-${System.currentTimeMillis()}.jpg")
            file.outputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output) }
            selectedUri = Uri.fromFile(file)
            selectedType = "foto"
        }
    }
    LaunchedEffect(Unit) { cameraLauncher.launch(null) }"""
if "val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview())" not in post:
    post = once(post, camera_anchor, camera_impl, "launch camera from plus button")
post = once(
    post,
    '                    OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.weight(1f)) {',
    '                    OutlinedButton(onClick = { cameraLauncher.launch(null) }, modifier = Modifier.weight(1f)) { Text("Câmera") }\n                    OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.weight(1f)) {',
    "add camera alternative"
)
main = main[:post_start] + post + main[post_end:]

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
                            if (post.optString("mediaUrl").isNotBlank() && post.optString("mediaType") == "image") {
                                AsyncImage(model = post.optString("mediaUrl"), contentDescription = "Imagem da publicação", modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).clip(RoundedCornerShape(14.dp)))
                            } else if (post.optString("mediaUrl").isNotBlank() && post.optString("mediaType") == "video") {
                                OutlinedButton(onClick = {
                                    try { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(post.optString("mediaUrl")), "video/*")) }
                                    catch (_: Exception) { onNotice("Não foi encontrado um reprodutor de vídeo neste aparelho.") }
                                }, modifier = Modifier.fillMaxWidth()) { Text("Reproduzir vídeo") }
                            }
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
# Add the image loader only to the extracted Build 72 app module.
gradle_files = list(root.rglob("app/build.gradle.kts"))
if len(gradle_files) != 1:
    raise SystemExit(f"Expected one app/build.gradle.kts; found {len(gradle_files)}")
gradle_path = gradle_files[0]
gradle_text = gradle_path.read_text(encoding="utf-8")
if "io.coil-kt:coil-compose" not in gradle_text:
    dependency_anchor = "dependencies {"
    if gradle_text.count(dependency_anchor) != 1:
        raise SystemExit("Could not locate app dependencies block for image loading.")
    gradle_text = gradle_text.replace(dependency_anchor, dependency_anchor + '\n    implementation("io.coil-kt:coil-compose:2.7.0")', 1)
    gradle_path.write_text(gradle_text, encoding="utf-8")

main_path.write_text(main, encoding="utf-8")
print("Applied Build 72 social feature patches:", main_path, api_path)
