package app.vybora.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.graphics.Bitmap
import android.content.Intent
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF050711)
private val Panel = Color(0xFF111528)
private val Violet = Color(0xFF8B35FF)
private val Pink = Color(0xFFFF2A9D)
private val Muted = Color(0xFFADB2CB)


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VyboraApp() }
    }
}

@Composable
private fun VyboraApp() {
    var tab by remember { mutableIntStateOf(0) }
    var liked by remember { mutableStateOf(setOf<String>()) }
    var notice by remember { mutableStateOf("") }
    var profileStat by remember { mutableStateOf("") }
    var profileStatUsers by remember { mutableStateOf<List<VyboraApi.UserSummary>>(emptyList()) }
    var showAuth by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var publicProfileUsername by remember { mutableStateOf("") }
    val appContext = androidx.compose.ui.platform.LocalContext.current
    val appPrefs = remember { appContext.getSharedPreferences("vybora_session", android.content.Context.MODE_PRIVATE) }
    var loggedInUser by remember { mutableStateOf(appPrefs.getString("username", "") ?: "") }
    var showSettings by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf(appPrefs.getString("display_name", "") ?: "") }
    var profileBio by remember { mutableStateOf(appPrefs.getString("bio", "") ?: "") }
    LaunchedEffect(Unit) {
        val token = appPrefs.getString("token", null)
        if (!token.isNullOrBlank()) {
            Thread {
                try {
                    val response = VyboraApi.getMyProfile(token)
                    val user = response.optJSONObject("user")
                    if (user != null) {
                        val username = user.optString("username")
                        val name = user.optString("displayName", user.optString("display_name"))
                        val bio = user.optString("bio")
                        appPrefs.edit().putString("username", username).putString("display_name", name).putString("bio", bio).apply()
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            if (username.isNotBlank()) loggedInUser = username
                            profileName = name
                            profileBio = bio
                        }
                    }
                } catch (_: Exception) {
                    // Keep the cached profile visible when the network is unavailable.
                }
            }.start()
        }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = Violet, secondary = Pink, background = Night, surface = Panel, onBackground = Color.White, onSurface = Color.White)) {
        Scaffold(containerColor = Night, bottomBar = {
            Column {
                if (tab == 3) {
                    Button(
                        onClick = { notice = "A transmissão ainda não pode começar: o servidor de mídia precisa ser integrado e validado." },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Radio, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Iniciar transmissão")
                    }
                }
                NavigationBar(containerColor = Color(0xFF080B16), contentColor = Color.White) {
                NavigationBarItem(selected = tab == 0, onClick = { showSettings = false; showEditProfile = false; publicProfileUsername = ""; showAuth = false; tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Início") })
                NavigationBarItem(selected = tab == 1, onClick = { showSettings = false; showEditProfile = false; publicProfileUsername = ""; showAuth = false; tab = 1 }, icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Mensagens") }, label = { Text("Mensagens") })
                NavigationBarItem(selected = tab == 2, onClick = { showSettings = false; showEditProfile = false; publicProfileUsername = ""; showAuth = false; tab = 2; notice = "" }, icon = { Icon(Icons.Default.Add, null) }, label = { Text("Publicar") })
                NavigationBarItem(selected = tab == 3, onClick = { showSettings = false; showEditProfile = false; publicProfileUsername = ""; showAuth = false; tab = 3 }, icon = { Icon(Icons.Default.Radio, null) }, label = { Text("Lives") })
                NavigationBarItem(selected = tab == 4, onClick = { showSettings = false; showEditProfile = false; publicProfileUsername = ""; tab = 4 }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Perfil") })
                }
            }
        }) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(Violet, Pink))), contentAlignment = Alignment.Center) { Text("V", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall) }
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text("VYBORA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                        Text("Lives e vídeos curtos. Pra todo mundo.", style = MaterialTheme.typography.labelSmall, color = Muted)
                    }
                    if (tab != 4 || showSettings || showEditProfile) {
                        Icon(Icons.Default.Search, contentDescription = when (tab) { 0 -> "Buscar pessoas e publicações"; 1 -> "Buscar mensagens"; 3 -> "Buscar usuários e lives"; else -> "Buscar pessoas" }, tint = Color.White, modifier = Modifier.size(25.dp).clickable { showSearchDialog = true })
                        Spacer(Modifier.width(14.dp))
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Abrir menu e configurações", tint = Color.White)
                    }
                }
                if (notice.isNotBlank()) {
                    Surface(color = Color(0xFF25143A), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(notice, Modifier.weight(1f), color = Color.White, style = MaterialTheme.typography.bodySmall); Text("×", Modifier.clickable { notice = "" }.padding(start = 12.dp), color = Pink) }
                    }
                }
                if (showSettings) {
                    SettingsPage(
                        prefs = appPrefs,
                        username = loggedInUser,
                        onBack = { showSettings = false },
                        onNotice = { notice = it },
                        onEditProfile = { showSettings = false; showEditProfile = true },
                        onLogout = {
                            appPrefs.edit().remove("token").remove("username").remove("display_name").remove("bio").apply()
                            loggedInUser = ""; profileName = ""; profileBio = ""; showSettings = false
                            notice = "Sessão encerrada neste aparelho. Seus dados online não foram apagados."
                        }
                    )
                } else if (showEditProfile) {
                    EditProfilePage(
                        initialUsername = loggedInUser,
                        initialName = profileName,
                        initialBio = profileBio,
                        onBack = { showEditProfile = false },
                        onSaved = { username, name, bio ->
                            loggedInUser = username; profileName = name; profileBio = bio
                            appPrefs.edit().putString("username", username).putString("display_name", name).putString("bio", bio).apply()
                            showEditProfile = false
                            notice = "Perfil atualizado e sincronizado."
                        },
                        onNotice = { notice = it }
                    )
                } else if (publicProfileUsername.isNotBlank()) {
                    PublicProfilePage(
                        username = publicProfileUsername,
                        onBack = { publicProfileUsername = "" },
                        onNotice = { notice = it }
                    )
                } else when (tab) {
                    0 -> FeedPage(currentUsername = loggedInUser, onNotice = { notice = it })
                    1 -> MessagesPage(onNotice = { notice = it })
                    2 -> PostPage(onNotice = { notice = it }, onCameraCancelled = { tab = 0 })
                    3 -> LivePage(onNotice = { notice = it })
                    else -> if (showAuth) {
                        AuthPage(
                            onSuccess = { username ->
                                loggedInUser = username
                                showAuth = false
                                notice = "Login realizado com sucesso!"
                                val token = appPrefs.getString("token", null)
                                if (!token.isNullOrBlank()) Thread {
                                    try {
                                        val response = VyboraApi.getMyProfile(token)
                                        val user = response.optJSONObject("user")
                                        if (user != null) {
                                            val serverName = user.optString("displayName", user.optString("display_name"))
                                            val serverBio = user.optString("bio")
                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                profileName = serverName
                                                profileBio = serverBio
                                                appPrefs.edit().putString("display_name", serverName).putString("bio", serverBio).apply()
                                            }
                                        }
                                    } catch (_: Exception) { }
                                }.start()
                            },
                            onCancel = { showAuth = false }
                        )
                    } else ProfilePage(username = loggedInUser, displayName = profileName, bio = profileBio, onNotice = { notice = it }, onStat = { stat ->
                        profileStat = stat
                        profileStatUsers = emptyList()
                        val tokenUser = loggedInUser
                        if (tokenUser.isNotBlank() && (stat == "Seguidores" || stat == "Seguindo")) {
                            Thread {
                                try {
                                    val users = VyboraApi.getConnections(tokenUser, if (stat == "Seguidores") "followers" else "following")
                                    android.os.Handler(android.os.Looper.getMainLooper()).post { profileStatUsers = users }
                                } catch (e: Exception) {
                                    android.os.Handler(android.os.Looper.getMainLooper()).post { notice = e.message ?: "Não foi possível carregar a lista." }
                                }
                            }.start()
                        }
                    }, onAuth = { showAuth = true }, onEdit = { showEditProfile = true })
                }
            }
        }
        if (showSearchDialog) {
            SearchUsersDialog(
                searchContext = when (tab) { 0 -> "home"; 1 -> "messages"; 3 -> "lives"; else -> "people" },
                onDismiss = { showSearchDialog = false },
                onOpenProfile = { username ->
                    showSearchDialog = false
                    publicProfileUsername = username
                },
                onError = { notice = it }
            )
        }
        if (profileStat.isNotBlank()) {
            AlertDialog(
                onDismissRequest = { profileStat = "" },
                title = { Text(profileStat) },
                text = {
                    if (profileStat == "Seguidores" || profileStat == "Seguindo") {
                        if (loggedInUser.isBlank()) Text("Entre na sua conta para consultar esta lista.")
                        else if (profileStatUsers.isEmpty()) Text("Nenhuma conta nesta lista ou carregamento ainda em andamento.")
                        else LazyColumn(Modifier.heightIn(max = 300.dp)) {
                            items(profileStatUsers, key = { it.id }) { user ->
                                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                    Text(user.displayName.ifBlank { user.username }, fontWeight = FontWeight.SemiBold)
                                    Text("@${user.username}", color = Muted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    } else Text("As publicações da sua conta serão exibidas no perfil quando o feed estiver carregado.")
                },
                confirmButton = { TextButton(onClick = { profileStat = "" }) { Text("Fechar") } }
            )
        }
    }
}


@Composable
private fun SearchUsersDialog(searchContext: String, onDismiss: () -> Unit, onOpenProfile: (String) -> Unit, onError: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("vybora_session", android.content.Context.MODE_PRIVATE) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<VyboraApi.UserSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(when (searchContext) { "home" -> "Buscar pessoas e publicações"; "messages" -> "Buscar mensagens e conversas"; "lives" -> "Buscar usuários e lives"; else -> "Buscar pessoas" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(when (searchContext) { "messages" -> "Pessoa ou conversa"; "lives" -> "Nome ou @usuário"; else -> "Nome, @usuário ou publicação" }) }
                )
                if (loading) CircularProgressIndicator(Modifier.size(24.dp))
                else if (searched && results.isEmpty()) Text("Nenhuma pessoa encontrada.", color = Muted)
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(results) { user ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpenProfile(user.username) }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(user.displayName.ifBlank { user.username }, fontWeight = FontWeight.SemiBold)
                                Text("@${user.username}", color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = {
                                val token = prefs.getString("token", null)
                                if (token.isNullOrBlank()) onError("Entre na sua conta para seguir pessoas.")
                                else Thread {
                                    try {
                                        val following = VyboraApi.toggleFollow(token, user.id)
                                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                                            onError(if (following) "Agora você segue @${user.username}." else "Você deixou de seguir @${user.username}.")
                                        }
                                    } catch (e: Exception) {
                                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                                            onError(e.message ?: "Não foi possível atualizar o seguimento.")
                                        }
                                    }
                                }.start()
                            }) { Text("Seguir") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !loading && query.trim().removePrefix("@").length >= 2, onClick = {
                loading = true
                Thread {
                    try {
                        val found = VyboraApi.searchUsers(query)
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            results = found; searched = true; loading = false
                        }
                    } catch (e: Exception) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            loading = false; onError(e.message ?: "Não foi possível pesquisar usuários.")
                        }
                    }
                }.start()
            }) { Text("Buscar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

@Composable
private fun FeedPage(currentUsername: String, onNotice: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("vybora_session", android.content.Context.MODE_PRIVATE) }
    var posts by remember { mutableStateOf<List<VyboraApi.PostSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var commentPost by remember { mutableStateOf<VyboraApi.PostSummary?>(null) }
    var comments by remember { mutableStateOf<List<VyboraApi.CommentSummary>>(emptyList()) }
    var commentDraft by remember { mutableStateOf("") }
    var commentsLoading by remember { mutableStateOf(false) }
    LaunchedEffect(refreshKey) {
        loading = true
        Thread {
            try {
                val loaded = VyboraApi.getPosts()
                android.os.Handler(android.os.Looper.getMainLooper()).post { posts = loaded; loading = false }
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    loading = false
                    onNotice(e.message ?: "Não foi possível carregar o feed.")
                }
            }
        }.start()
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Seu feed", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (!loading && posts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ainda não há publicações", fontWeight = FontWeight.SemiBold)
                    Text("Quando alguém publicar, as postagens públicas aparecerão aqui.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(posts, key = { it.id }) { post ->
                    var postMenuExpanded by remember(post.id) { mutableStateOf(false) }
                    Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(post.displayName.ifBlank { post.username }.ifBlank { "VYBORA" }, fontWeight = FontWeight.Bold)
                                    if (post.username.isNotBlank()) Text("@${post.username}", color = Muted, style = MaterialTheme.typography.labelSmall)
                                }
                                if (currentUsername.isNotBlank() && post.username.equals(currentUsername, ignoreCase = true)) {
                                    Box {
                                        IconButton(onClick = { postMenuExpanded = true }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Opções da publicação")
                                        }
                                        DropdownMenu(expanded = postMenuExpanded, onDismissRequest = { postMenuExpanded = false }) {
                                            DropdownMenuItem(
                                                text = { Text("Excluir publicação") },
                                                leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
                                                onClick = {
                                                    postMenuExpanded = false
                                                    val token = prefs.getString("token", null)
                                                    if (token.isNullOrBlank()) onNotice("Entre na sua conta para excluir esta publicação.")
                                                    else Thread {
                                                        try {
                                                            VyboraApi.deletePost(token, post.id)
                                                            val refreshed = VyboraApi.getPosts()
                                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                                posts = refreshed
                                                                onNotice("Publicação excluída.")
                                                            }
                                                        } catch (e: Exception) {
                                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                                onNotice(e.message ?: "Não foi possível excluir a publicação.")
                                                            }
                                                        }
                                                    }.start()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            if (post.caption.isNotBlank()) Text(post.caption, style = MaterialTheme.typography.bodyLarge)
                            if (post.mediaUrl.isNotBlank() && post.mediaType == "image") {
                                AsyncImage(model = post.mediaUrl, contentDescription = "Imagem da publicação", modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).clip(RoundedCornerShape(14.dp)))
                            } else if (post.mediaUrl.isNotBlank() && post.mediaType == "video") {
                                OutlinedButton(onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(post.mediaUrl), "video/*"))
                                    } catch (_: Exception) {
                                        onNotice("Não foi encontrado um reprodutor de vídeo neste aparelho.")
                                    }
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Reproduzir vídeo")
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    val token = prefs.getString("token", null)
                                    if (token.isNullOrBlank()) onNotice("Entre na sua conta para curtir publicações.")
                                    else Thread {
                                        try {
                                            VyboraApi.toggleLike(token, post.id)
                                            val refreshed = VyboraApi.getPosts()
                                            android.os.Handler(android.os.Looper.getMainLooper()).post { posts = refreshed }
                                        } catch (e: Exception) {
                                            android.os.Handler(android.os.Looper.getMainLooper()).post { onNotice(e.message ?: "Não foi possível curtir.") }
                                        }
                                    }.start()
                                }) { Icon(Icons.Default.FavoriteBorder, contentDescription = "Curtir") }
                                Text("${post.likes} curtidas", color = Muted, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.width(16.dp))
                                TextButton(onClick = {
                                    commentPost = post
                                    commentsLoading = true
                                    Thread {
                                        try {
                                            val loadedComments = VyboraApi.getComments(post.id)
                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                comments = loadedComments; commentsLoading = false
                                            }
                                        } catch (e: Exception) {
                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                commentsLoading = false; onNotice(e.message ?: "Não foi possível carregar comentários.")
                                            }
                                        }
                                    }.start()
                                }) {
                                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Abrir comentários", tint = Muted)
                                    Spacer(Modifier.width(5.dp))
                                    Text("${post.comments} comentários", color = Muted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    val activePost = commentPost
    if (activePost != null) {
        AlertDialog(
            onDismissRequest = { commentPost = null },
            title = { Text("Comentários") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (commentsLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    LazyColumn(Modifier.heightIn(max = 260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(comments, key = { it.id }) { comment ->
                            Column {
                                Text(comment.displayName.ifBlank { comment.username }.ifBlank { "Usuário" }, fontWeight = FontWeight.SemiBold)
                                Text(comment.body)
                                Divider()
                            }
                        }
                    }
                    OutlinedTextField(value = commentDraft, onValueChange = { commentDraft = it.take(1200) }, label = { Text("Escreva um comentário") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(enabled = commentDraft.isNotBlank() && !commentsLoading, onClick = {
                    val token = prefs.getString("token", null)
                    if (token.isNullOrBlank()) onNotice("Entre na sua conta para comentar.")
                    else {
                        commentsLoading = true
                        Thread {
                            try {
                                VyboraApi.addComment(token, activePost.id, commentDraft)
                                val loadedComments = VyboraApi.getComments(activePost.id)
                                val loadedPosts = VyboraApi.getPosts()
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    comments = loadedComments; posts = loadedPosts; commentDraft = ""; commentsLoading = false
                                }
                            } catch (e: Exception) {
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    commentsLoading = false; onNotice(e.message ?: "Não foi possível publicar o comentário.")
                                }
                            }
                        }.start()
                    }
                }) { Text("Comentar") }
            },
            dismissButton = { TextButton(onClick = { commentPost = null }) { Text("Fechar") } }
        )
    }
}

@Composable
private fun MessagesPage(onNotice: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("vybora_session", android.content.Context.MODE_PRIVATE) }
    var draft by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<VyboraApi.MessageSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var loadedRecipient by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Mensagens", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Abra uma conversa pelo ícone de pesquisa no topo.", color = Muted)
        if (loadedRecipient.isNotBlank()) {
            Text("Conversa com @$loadedRecipient", fontWeight = FontWeight.SemiBold)
        } else {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Use a lupa no topo para encontrar alguém e abrir uma conversa.", color = Muted)
            }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(
            Modifier.weight(if (loadedRecipient.isNotBlank()) 1f else 0.15f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(message.body)
                        Text(message.createdAt, color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

    }
}

@Composable
private fun ProfilePage(username: String, displayName: String, bio: String, onNotice: (String) -> Unit, onStat: (String) -> Unit, onAuth: () -> Unit, onEdit: () -> Unit) {
    var postsCount by remember { mutableIntStateOf(0) }
    var followersCount by remember { mutableIntStateOf(0) }
    var followingCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(username) {
        if (username.isNotBlank()) {
            Thread {
                try {
                    val response = VyboraApi.getPublicProfile(username)
                    val user = response.optJSONObject("user")
                    if (user != null) android.os.Handler(android.os.Looper.getMainLooper()).post {
                        postsCount = user.optInt("posts_count")
                        followersCount = user.optInt("followers")
                        followingCount = user.optInt("following")
                    }
                } catch (_: Exception) { }
            }.start()
        } else {
            postsCount = 0; followersCount = 0; followingCount = 0
        }
    }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Seu perfil", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Surface(color = Panel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(76.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Violet, Pink))), contentAlignment = Alignment.Center) { Text("V", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black) }
                Text(displayName.ifBlank { if (username.isBlank()) "Seu nome" else username }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(if (username.isBlank()) "Entre ou crie sua conta" else "@$username", color = Muted)
                if (bio.isNotBlank()) Text(bio, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ProfileStat("Publicações", postsCount.toString()) { onStat("Publicações") }
                    ProfileStat("Seguidores", followersCount.toString()) { onStat("Seguidores") }
                    ProfileStat("Seguindo", followingCount.toString()) { onStat("Seguindo") }
                }
                Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Edit, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Editar perfil") }
                OutlinedButton(onClick = onAuth, modifier = Modifier.fillMaxWidth()) { Text(if (username.isBlank()) "Entrar / criar conta" else "Trocar conta") }
            }
        }
        Text("Toque em Seguidores ou Seguindo para consultar as listas reais da conta.", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AuthPage(onSuccess: (String) -> Unit, onCancel: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("vybora_session", android.content.Context.MODE_PRIVATE) }
    var createAccount by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val activity = context as? android.app.Activity
        if (activity == null) {
            loading = false
            error = "Não foi possível concluir o login Google."
        } else {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                try {
                    // Parse the Play Services result even when Android reports RESULT_CANCELED,
                    // so the real Google status code is shown instead of a generic cancellation.
                    val idToken = GoogleSignIn.idTokenFromResult(result.data)
                    val auth = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        VyboraApi.authenticateWithGoogle(idToken)
                    }
                    prefs.edit().putString("token", auth.token).putString("username", auth.username).apply()
                    loading = false
                    onSuccess(auth.username)
                } catch (e: Exception) {
                    loading = false
                    error = e.message ?: "Não foi possível entrar com Google."
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(if (createAccount) "Criar conta" else "Entrar na VYBORA", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Sua conta será criada no servidor e poderá ser usada em outros aparelhos.", color = Muted, style = MaterialTheme.typography.bodySmall)
        Button(
            onClick = {
                val activity = context as? android.app.Activity
                if (activity == null) {
                    error = "Não foi possível abrir a seleção da conta Google."
                } else {
                    loading = true
                    error = ""
                    try {
                        googleSignInLauncher.launch(GoogleSignIn.signInIntent(activity))
                    } catch (e: Exception) {
                        loading = false
                        error = e.message ?: "Não foi possível abrir o login Google."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            enabled = !loading
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("Continuar com Google", fontWeight = FontWeight.SemiBold)
        }
        if (createAccount) OutlinedTextField(value = username, onValueChange = { username = it.trim().replace(" ", "") }, label = { Text("Nome de usuário") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !loading)
        OutlinedTextField(value = email, onValueChange = { email = it.trim() }, label = { Text("E-mail") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !loading)
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Senha (mínimo 8 caracteres)") }, singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), enabled = !loading)
        if (error.isNotBlank()) Text(error, color = Pink, style = MaterialTheme.typography.bodySmall)
        Button(onClick = {
            val normalizedEmail = email.trim().lowercase()
            when {
                !normalizedEmail.contains("@") || !normalizedEmail.substringAfter("@", "").contains(".") -> error = "Digite um e-mail válido."
                password.length < 8 -> error = "A senha precisa ter pelo menos 8 caracteres."
                createAccount && username.trim().length < 3 -> error = "O nome de usuário precisa ter 3 caracteres ou mais."
                createAccount && !username.matches(Regex("[A-Za-z0-9_.]{3,24}")) -> error = "Use 3–24 caracteres: letras, números, ponto ou sublinhado."
                else -> {
                    loading = true
                    error = ""
                    Thread {
                        try {
                            val result = VyboraApi.authenticate(createAccount, username, normalizedEmail, password)
                            prefs.edit().putString("token", result.token).putString("username", result.username).apply()
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                loading = false
                                onSuccess(result.username)
                            }
                        } catch (e: Exception) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                loading = false
                                error = e.message ?: "Não foi possível autenticar."
                            }
                        }
                    }.start()
                }
            }
        }, modifier = Modifier.fillMaxWidth(), enabled = !loading) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Text(if (createAccount) "Criar conta" else "Entrar")
        }
        TextButton(onClick = { createAccount = !createAccount; error = "" }, modifier = Modifier.fillMaxWidth(), enabled = !loading) { Text(if (createAccount) "Já tenho conta — entrar" else "Não tenho conta — criar agora") }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth(), enabled = !loading) { Text("Voltar ao perfil") }
        Text("Se aparecer ‘Backend ainda não configurado’, publique o servidor e configure API_BASE_URL conforme o arquivo BACKEND/README.md.", color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun EditProfilePage(initialUsername: String, initialName: String, initialBio: String, onBack: () -> Unit, onSaved: (String, String, String) -> Unit, onNotice: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("vybora_session", android.content.Context.MODE_PRIVATE) }
    var username by remember { mutableStateOf(initialUsername) }
    var name by remember { mutableStateOf(initialName) }
    var bio by remember { mutableStateOf(initialBio) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar") }
            Text("Editar perfil", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
        Text("Atualize como as pessoas veem você no VYBORA.", color = Muted)
        OutlinedTextField(value = username, onValueChange = { username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' || ch == '.' }.take(24) }, label = { Text("@Nome de usuário") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !loading, supportingText = { Text("3–24 caracteres: letras, números, ponto e sublinhado") })
        OutlinedTextField(value = name, onValueChange = { name = it.take(80) }, label = { Text("Nome de exibição") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !loading)
        OutlinedTextField(value = bio, onValueChange = { bio = it.take(300) }, label = { Text("Biografia") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5, enabled = !loading, supportingText = { Text("Até 300 caracteres") })
        if (error.isNotBlank()) Text(error, color = Pink)
        Button(onClick = {
            if (name.isBlank()) { error = "Informe um nome de exibição." }
            else if (!username.matches(Regex("[a-z0-9_.]{3,24}"))) { error = "O nome de usuário deve ter 3–24 caracteres válidos." }
            else {
                loading = true; error = ""
                val token = prefs.getString("token", null)
                Thread {
                    try {
                        if (!token.isNullOrBlank()) VyboraApi.updateProfile(token, username.trim(), name.trim(), bio.trim())
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            loading = false
                            if (token.isNullOrBlank()) {
                                error = "Entre na sua conta antes de editar o perfil. Assim, o nome e o @usuário ficam salvos no servidor e não dependem deste aparelho."
                                return@post
                            }
                            onSaved(username.trim(), name.trim(), bio.trim())
                        }
                    } catch (e: Exception) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post { loading = false; error = e.message ?: "Não foi possível salvar no servidor." }
                    }
                }.start()
            }
        }, modifier = Modifier.fillMaxWidth(), enabled = !loading) {
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Salvar alterações")
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), enabled = !loading) { Text("Cancelar") }
    }
}

@Composable
private fun SettingsPage(prefs: android.content.SharedPreferences, username: String, onBack: () -> Unit, onNotice: (String) -> Unit, onEditProfile: () -> Unit, onLogout: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var notifications by remember { mutableStateOf(prefs.getBoolean("setting_notifications", true)) }
    var privateAccount by remember { mutableStateOf(prefs.getBoolean("setting_private_account", false)) }
    var autoplay by remember { mutableStateOf(prefs.getBoolean("setting_autoplay", true)) }
    var activityStatus by remember { mutableStateOf(prefs.getBoolean("setting_activity_status", false)) }
    var allowMessages by remember { mutableStateOf(prefs.getBoolean("setting_messages", true)) }
    var allowComments by remember { mutableStateOf(prefs.getBoolean("setting_comments", true)) }
    var suggestProfile by remember { mutableStateOf(prefs.getBoolean("setting_suggest_profile", true)) }
    var expanded by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar ao perfil") }
            Column(Modifier.weight(1f)) {
                Text("Configurações e privacidade", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Controle sua conta e sua experiência", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
            item { Text("SUA CONTA", color = Pink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
            item { SettingsLink("Editar perfil", "Nome, @usuário e biografia", Icons.Default.Person, onEditProfile) }
            item { SettingsLink("Segurança da conta", "Senha, sessões e recuperação", Icons.Default.Lock) { expanded = if (expanded == "security") "" else "security" } }
            if (expanded == "security") item { InfoPanel("Proteja sua conta", "Use uma senha exclusiva. Recuperação de senha, autenticação em duas etapas e gestão de sessões precisam estar habilitadas no backend antes de serem consideradas ativas.") }
            item { Text("PRIVACIDADE", color = Pink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            item { SettingSwitch("Conta privada", "Preferência local; só será efetiva quando a API aplicar as regras de visibilidade.", privateAccount, Icons.Default.Lock) { privateAccount = it; prefs.edit().putBoolean("setting_private_account", it).apply(); onNotice("Preferência salva. A privacidade real precisa ser aplicada e validada no servidor.") } }
            item { SettingSwitch("Mostrar status de atividade", "Permite controlar sua preferência de presença.", activityStatus, Icons.Default.Visibility) { activityStatus = it; prefs.edit().putBoolean("setting_activity_status", it).apply() } }
            item { SettingSwitch("Permitir mensagens", "Preferência para quem pode iniciar conversas; depende de validação no backend.", allowMessages, Icons.Default.ChatBubbleOutline) { allowMessages = it; prefs.edit().putBoolean("setting_messages", it).apply(); onNotice("Preferência salva neste aparelho; a API ainda precisa aplicar essa regra.") } }
            item { SettingSwitch("Permitir comentários", "Preferência de interação nas publicações.", allowComments, Icons.Default.ChatBubbleOutline) { allowComments = it; prefs.edit().putBoolean("setting_comments", it).apply(); onNotice("Preferência salva neste aparelho; a API ainda precisa aplicar essa regra.") } }
            item { SettingSwitch("Sugerir meu perfil", "Controla a preferência de descoberta do seu perfil.", suggestProfile, Icons.Default.Person) { suggestProfile = it; prefs.edit().putBoolean("setting_suggest_profile", it).apply() } }
            item { SettingsLink("Contas bloqueadas", "Gerencie quem você não quer que interaja com você", Icons.Default.Block) { onNotice("A lista de bloqueio será exibida quando o recurso de bloqueio estiver conectado ao backend.") } }
            item { Text("TRANSMISSÃO AO VIVO", color = Pink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            item { SettingsLink("Integrações de live", "Configuração de provedores de transmissão", Icons.Default.Radio) { onNotice("As integrações de provedores ficam nesta área; para ativar transmissões reais ainda é necessário configurar e testar o servidor de mídia.") } }
            item { Text("PREFERÊNCIAS", color = Pink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            item { SettingSwitch("Notificações", "Preferência salva no aparelho; push exige configuração do servidor.", notifications, Icons.Default.Notifications) { notifications = it; prefs.edit().putBoolean("setting_notifications", it).apply(); onNotice(if (it) "Preferência de notificações ativada." else "Preferência de notificações desativada.") } }
            item { SettingSwitch("Reprodução automática", "Controla a preferência salva neste aparelho.", autoplay, Icons.Default.PlayCircle) { autoplay = it; prefs.edit().putBoolean("setting_autoplay", it).apply() } }
            item { SettingsLink("Idioma", "Português (Brasil)", Icons.Default.Language) { onNotice("O aplicativo está em português. Seletor de idiomas ainda não implementado.") } }
            item { SettingsLink("Aparência", "Tema escuro", Icons.Default.DarkMode) { onNotice("O VYBORA está usando o tema escuro nesta versão.") } }
            item { SettingsLink("Atualizações do aplicativo", "Verificar e baixar o APK mais recente", Icons.Default.FileDownload) { context.startActivity(Intent(context, RemoteUiActivity::class.java)) } }
            item { Text("SEUS DADOS", color = Pink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            item { SettingsLink("Baixar seus dados", "Solicite uma cópia das informações da conta", Icons.Default.FileDownload) { onNotice("A exportação existe na API, mas o download autenticado pelo aplicativo ainda precisa ser concluído.") } }
            item { SettingsLink("Desativar ou excluir conta", "Leia antes de solicitar a remoção", Icons.Default.DeleteForever) { onNotice("A exclusão de conta não é executada nesta tela para evitar perda acidental. O fluxo seguro precisa confirmar identidade e tratar os dados relacionados no servidor.") } }
            item { Text("AJUDA E INFORMAÇÕES", color = Pink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            item { SettingsLink("Ajuda e suporte", "Como usar o VYBORA e reportar um problema", Icons.Default.HelpOutline) { onNotice("Central de ajuda e suporte ainda será conectada.") } }
            item { SettingsLink("Sobre o VYBORA", "Versão 0.3.0 · Termos e privacidade", Icons.Default.Info) { onNotice("VYBORA 0.3.0. Os links de Termos de Uso e Política de Privacidade precisam ser publicados antes do lançamento.") } }
            item { Text("CONTA CONECTADA", color = Pink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            item { Text(if (username.isBlank()) "Nenhuma conta conectada" else "Conta: @$username", color = Muted) }
            item { OutlinedButton(onClick = { if (username.isBlank()) onNotice("Você não está conectado a uma conta.") else onLogout() }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Sair desta conta neste aparelho") } }
            item { Text("Importante: opções marcadas como preferências locais não alteram, sozinhas, as regras do servidor.", color = Muted, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
private fun SettingsLink(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(color = Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Violet)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Text("›", color = Muted, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun InfoPanel(title: String, message: String) {
    Surface(color = Color(0xFF1A2035), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(message, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, onChange: (Boolean) -> Unit) {
    Surface(color = Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Violet)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(description, color = Muted, style = MaterialTheme.typography.bodySmall) }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun PublicProfilePage(username: String, onBack: () -> Unit, onNotice: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("vybora_session", android.content.Context.MODE_PRIVATE) }
    var user by remember { mutableStateOf(org.json.JSONObject()) }
    var loading by remember { mutableStateOf(true) }
    var isFollowing by remember { mutableStateOf(false) }
    LaunchedEffect(username) {
        loading = true
        Thread {
            try {
                val response = VyboraApi.getPublicProfile(username)
                val found = response.optJSONObject("user") ?: org.json.JSONObject()
                android.os.Handler(android.os.Looper.getMainLooper()).post { user = found; loading = false }
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    loading = false; onNotice(e.message ?: "Não foi possível abrir este perfil.")
                }
            }
        }.start()
    }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar") }
            Text("Perfil", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        else Surface(color = Panel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(76.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Violet, Pink))), contentAlignment = Alignment.Center) {
                    Text(user.optString("display_name").take(1).ifBlank { username.take(1) }.uppercase(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                }
                Text(user.optString("display_name").ifBlank { username }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("@${user.optString("username", username)}", color = Muted)
                if (user.optString("bio").isNotBlank()) Text(user.optString("bio"), color = Color.White)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ProfileStat("Publicações", user.optInt("posts_count").toString()) { onNotice("Abra o feed para ver as publicações públicas deste perfil.") }
                    ProfileStat("Seguidores", user.optInt("followers").toString()) { onNotice("A lista de seguidores está disponível no perfil da própria conta.") }
                    ProfileStat("Seguindo", user.optInt("following").toString()) { onNotice("A lista de contas seguidas está disponível no perfil da própria conta.") }
                }
                Button(onClick = {
                    val token = prefs.getString("token", null)
                    val targetId = user.optString("id")
                    if (token.isNullOrBlank()) onNotice("Entre na sua conta para seguir pessoas.")
                    else if (targetId.isBlank()) onNotice("O identificador deste perfil não foi carregado.")
                    else Thread {
                        try {
                            val following = VyboraApi.toggleFollow(token, targetId)
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                isFollowing = following
                                onNotice(if (following) "Agora você segue @${username}." else "Você deixou de seguir @${username}.")
                            }
                        } catch (e: Exception) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post { onNotice(e.message ?: "Não foi possível atualizar o seguimento.") }
                        }
                    }.start()
                }, modifier = Modifier.fillMaxWidth()) { Text(if (isFollowing) "Seguindo" else "Seguir") }
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, count: String, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Pink)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

@Composable
private fun PostPage(onNotice: (String) -> Unit, onCameraCancelled: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val prefs = remember { context.getSharedPreferences("vybora_session", android.content.Context.MODE_PRIVATE) }
    var caption by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedType by remember { mutableStateOf("") }
    var publishing by remember { mutableStateOf(false) }
    var cameraGranted by remember { mutableStateOf(androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) }
    var cameraError by remember { mutableStateOf("") }
    var lensFacing by remember { mutableIntStateOf(androidx.camera.core.CameraSelector.LENS_FACING_BACK) }
    var selectedFilter by remember { mutableStateOf("Original") }
    var imageCapture by remember { mutableStateOf<androidx.camera.core.ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<androidx.camera.video.VideoCapture<androidx.camera.video.Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<androidx.camera.video.Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var captureMode by remember { mutableStateOf("Foto") }
    var cameraProvider by remember { mutableStateOf<androidx.camera.lifecycle.ProcessCameraProvider?>(null) }
    var previewView by remember { mutableStateOf<androidx.camera.view.PreviewView?>(null) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var flashEnabled by remember { mutableStateOf(false) }
    var showComposer by remember { mutableStateOf(false) }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        if (!granted) cameraError = "Permita o acesso à câmera para usar a câmera do VYBORA."
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { selectedUri = uri; selectedType = "foto"; showComposer = true }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { selectedUri = uri; selectedType = "vídeo"; showComposer = true }
    }

    fun startVideoRecording() {
        val video = videoCapture
        if (video == null) { cameraError = "A câmera de vídeo ainda está iniciando. Tente novamente."; return }
        try {
            val file = File(context.cacheDir, "vybora-video-" + System.currentTimeMillis() + ".mp4")
            val output = androidx.camera.video.FileOutputOptions.Builder(file).build()
            var pending = video.output.prepareRecording(context, output)
            val audioGranted = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (audioGranted) pending = pending.withAudioEnabled()
            activeRecording = pending.start(androidx.core.content.ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is androidx.camera.video.VideoRecordEvent.Start -> { isRecording = true; cameraError = "" }
                    is androidx.camera.video.VideoRecordEvent.Finalize -> {
                        isRecording = false
                        activeRecording = null
                        if (event.hasError()) cameraError = "Falha ao finalizar o vídeo (" + event.error + ")."
                        else { selectedUri = Uri.fromFile(file); selectedType = "vídeo"; showComposer = true }
                    }
                }
            }
        } catch (e: Exception) { cameraError = "Não foi possível gravar o vídeo: " + (e.localizedMessage ?: "erro desconhecido") }
    }

    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVideoRecording()
        else cameraError = "Permita o microfone para gravar vídeos com áudio."
    }
    LaunchedEffect(cameraGranted, lensFacing, showComposer, previewView) {
        if (cameraGranted && !showComposer && previewView != null) {
            try {
                val future = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)
                future.addListener({
                    try {
                        val provider = future.get()
                        cameraProvider = provider
                        val view = previewView ?: return@addListener
                        val preview = androidx.camera.core.Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                        val capture = androidx.camera.core.ImageCapture.Builder().setCaptureMode(androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                        val recorder = androidx.camera.video.Recorder.Builder()
                            .setQualitySelector(androidx.camera.video.QualitySelector.from(androidx.camera.video.Quality.HD))
                            .build()
                        val video = androidx.camera.video.VideoCapture.withOutput(recorder)
                        val selector = androidx.camera.core.CameraSelector.Builder().requireLensFacing(lensFacing).build()
                        provider.unbindAll()
                        val boundCamera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture, video)
                        cameraControl = boundCamera
                        zoomRatio = 1f
                        capture.flashMode = if (flashEnabled) androidx.camera.core.ImageCapture.FLASH_MODE_ON else androidx.camera.core.ImageCapture.FLASH_MODE_OFF
                        imageCapture = capture
                        videoCapture = video
                        cameraError = ""
                    } catch (e: Exception) {
                        cameraError = "Não foi possível iniciar a câmera: " + (e.localizedMessage ?: "dispositivo indisponível")
                    }
                }, androidx.core.content.ContextCompat.getMainExecutor(context))
            } catch (e: Exception) { cameraError = "Câmera indisponível neste aparelho." }
        } else {
            if (isRecording) activeRecording?.stop()
            cameraProvider?.unbindAll()
            cameraControl = null
            imageCapture = null
            videoCapture = null
        }
    }

    if (!cameraGranted && !showComposer) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(60.dp))
            Text("Câmera VYBORA", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Câmera integrada ao aplicativo, com prévia e filtros.", color = Muted)
            Button(onClick = { cameraPermission.launch(android.Manifest.permission.CAMERA) }) { Text("Permitir câmera") }
            OutlinedButton(onClick = { imagePicker.launch("image/*") }) { Text("Escolher foto da galeria") }
            OutlinedButton(onClick = { videoPicker.launch("video/*") }) { Text("Escolher vídeo da galeria") }
            TextButton(onClick = onCameraCancelled) { Text("Voltar ao início") }
        }
        return
    }

    if (!showComposer) {
        Box(Modifier.fillMaxSize().background(Color(0xFF100A18))) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx -> androidx.camera.view.PreviewView(ctx).also {
                    it.scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
                    previewView = it
                } },
                modifier = Modifier.fillMaxSize()
            )
            val tint = when (selectedFilter) {
                "Quente" -> Color(0x44FF9A42)
                "Frio" -> Color(0x443F8CFF)
                "Rosa" -> Color(0x44FF45B5)
                "Mono" -> Color(0x44808080)
                else -> Color.Transparent
            }
            Box(Modifier.fillMaxSize().background(tint))
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (isRecording) activeRecording?.stop(); onCameraCancelled() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White) }
                    Column(Modifier.weight(1f)) {
                        Text("VYBORA", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text("Crie • Grave • Compartilhe", color = Color.White.copy(alpha = 0.88f), style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = { imagePicker.launch("image/*") }) { Text("Galeria", color = Color.White) }
                    IconButton(enabled = !isRecording, onClick = { lensFacing = if (lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_BACK) androidx.camera.core.CameraSelector.LENS_FACING_FRONT else androidx.camera.core.CameraSelector.LENS_FACING_BACK }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Trocar câmera", tint = Color.White)
                    }
                }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(Brush.verticalGradient(listOf(Color(0x00150B20), Color(0xA6150B20), Color(0xE6150B20)))).padding(horizontal = 8.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    if (cameraError.isNotBlank()) Text(cameraError, color = Color.White, style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Foto", "Vídeo").forEach { mode ->
                            Surface(color = if (captureMode == mode) Pink else Color(0xAA171827), shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f).clickable { if (!isRecording) captureMode = mode }) {
                                Text(mode, color = Color.White, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                        Text("1×", color = Color.White, modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (zoomRatio == 1f) Color(0xAAFFFFFF) else Color.Transparent).clickable { zoomRatio = 1f; cameraControl?.cameraControl?.setZoomRatio(1f) }.padding(horizontal = 10.dp, vertical = 7.dp))
                        Text("2×", color = Color.White, modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (zoomRatio == 2f) Color(0xAAFFFFFF) else Color.Transparent).clickable { zoomRatio = 2f; cameraControl?.cameraControl?.setZoomRatio(2f) }.padding(horizontal = 10.dp, vertical = 7.dp))
                        Text("3×", color = Color.White, modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (zoomRatio == 3f) Color(0xAAFFFFFF) else Color.Transparent).clickable { zoomRatio = 3f; cameraControl?.cameraControl?.setZoomRatio(3f) }.padding(horizontal = 10.dp, vertical = 7.dp))
                        Text(if (flashEnabled) "⚡ Flash ligado" else "⚡ Flash desligado", color = Color.White, modifier = Modifier.clickable {
                            flashEnabled = !flashEnabled
                            imageCapture?.flashMode = if (flashEnabled) androidx.camera.core.ImageCapture.FLASH_MODE_ON else androidx.camera.core.ImageCapture.FLASH_MODE_OFF
                        }.padding(8.dp))
                    }
                    // FILTROS AO VIVO — rótulo visual atualizado para uma organização mais limpa.
                    Text("Efeitos", color = Color.White.copy(alpha = 0.92f), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Original", "Quente", "Frio", "Rosa", "Mono").forEach { filter ->
                            Surface(color = if (selectedFilter == filter) Violet else Color(0xAA171827), shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f).clickable { selectedFilter = filter }) {
                                Text(filter, color = Color.White, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                        IconButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.size(54.dp).background(Color(0xAA392843), CircleShape)) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Abrir galeria", tint = Color.White)
                        }
                        Box(Modifier.size(88.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFFF80C8), Color(0xFF9A5BFF), Color(0xFFFF2A9D)))).padding(5.dp).clip(CircleShape).background(if (isRecording) Pink else Color(0xFFFFFBFF)).clickable {
                            if (captureMode == "Vídeo") {
                                if (isRecording) activeRecording?.stop()
                                else {
                                    val audioGranted = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (audioGranted) startVideoRecording() else audioPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            } else {
                                val capture = imageCapture
                                if (capture == null) cameraError = "A câmera ainda está iniciando. Tente novamente."
                                else {
                                    val file = File(context.cacheDir, "vybora-camera-" + System.currentTimeMillis() + ".jpg")
                                    val output = androidx.camera.core.ImageCapture.OutputFileOptions.Builder(file).build()
                                    capture.takePicture(output, androidx.core.content.ContextCompat.getMainExecutor(context), object : androidx.camera.core.ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(result: androidx.camera.core.ImageCapture.OutputFileResults) {
                                            try {
                                                if (selectedFilter != "Original") {
                                                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                                    if (bitmap != null) {
                                                        val filtered = android.graphics.Bitmap.createBitmap(bitmap.width, bitmap.height, android.graphics.Bitmap.Config.ARGB_8888)
                                                        val canvas = android.graphics.Canvas(filtered)
                                                        val matrix = android.graphics.ColorMatrix()
                                                        when (selectedFilter) {
                                                            "Mono" -> matrix.setSaturation(0f)
                                                            "Quente" -> matrix.set(floatArrayOf(1.12f,0f,0f,0f,12f, 0f,1.02f,0f,0f,4f, 0f,0f,0.88f,0f,0f, 0f,0f,0f,1f,0f))
                                                            "Frio" -> matrix.set(floatArrayOf(0.9f,0f,0f,0f,0f, 0f,1.02f,0f,0f,0f, 0f,0f,1.15f,0f,12f, 0f,0f,0f,1f,0f))
                                                            "Rosa" -> matrix.set(floatArrayOf(1.08f,0f,0f,0f,8f, 0f,0.9f,0f,0f,0f,0f, 0f,0f,1.02f,0f,8f, 0f,0f,0f,1f,0f))
                                                        }
                                                        val paint = android.graphics.Paint().apply { colorFilter = android.graphics.ColorMatrixColorFilter(matrix) }
                                                        canvas.drawBitmap(bitmap, 0f, 0f, paint)
                                                        file.outputStream().use { filtered.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
                                                        bitmap.recycle(); filtered.recycle()
                                                    }
                                                }
                                                selectedUri = Uri.fromFile(file); selectedType = "foto"; showComposer = true
                                            } catch (e: Exception) { cameraError = "Foto capturada, mas o filtro não pôde ser aplicado." }
                                        }
                                        override fun onError(exception: androidx.camera.core.ImageCaptureException) { cameraError = "Falha ao tirar foto: " + (exception.message ?: "tente novamente") }
                                    })
                                }
                            }
                        }, contentAlignment = Alignment.Center) {
                            if (captureMode == "Vídeo") Box(Modifier.size(if (isRecording) 30.dp else 42.dp).clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape).background(if (isRecording) Color.White else Pink))
                            else Box(Modifier.size(68.dp).clip(CircleShape).background(Color(0xFFFFFBFF)))
                        }
                        IconButton(onClick = { videoPicker.launch("video/*") }, modifier = Modifier.size(54.dp).background(Color(0xAA171827), CircleShape)) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = "Escolher vídeo", tint = Color.White)
                        }
                    }
                    Text(if (captureMode == "Vídeo") { if (isRecording) "● GRAVANDO — toque para finalizar" else "Toque para iniciar a gravação com áudio" } else "Toque para fotografar • vídeos também podem ser escolhidos na galeria", color = if (isRecording) Pink else Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { showComposer = false; selectedUri = null; selectedType = "" }) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar à câmera") }
            Text("Finalizar publicação", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        selectedUri?.let { uri ->
            if (selectedType == "foto") AsyncImage(model = uri, contentDescription = "Prévia da foto", modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).clip(RoundedCornerShape(18.dp)))
            else Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Pink, modifier = Modifier.size(44.dp))
                    Column(Modifier.padding(start = 12.dp)) { Text("Vídeo selecionado", fontWeight = FontWeight.Bold); Text(uri.lastPathSegment ?: "Arquivo da galeria", color = Muted) }
                }
            }
        }
        OutlinedTextField(value = caption, onValueChange = { caption = it }, label = { Text("Escreva uma legenda") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        Button(onClick = {
            val token = prefs.getString("token", null)
            when {
                token.isNullOrBlank() -> onNotice("Entre na sua conta antes de publicar.")
                caption.isBlank() && selectedUri == null -> onNotice("Escreva um texto ou escolha uma foto/vídeo.")
                else -> {
                    publishing = true
                    Thread {
                        try {
                            if (selectedUri == null) VyboraApi.createTextPost(token, caption)
                            else {
                                val uri = selectedUri!!
                                val mime = context.contentResolver.getType(uri) ?: if (selectedType == "vídeo") "video/mp4" else "image/jpeg"
                                val size = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                                if (size <= 0L) throw IllegalStateException("Não foi possível determinar o tamanho do arquivo.")
                                val fileName = uri.lastPathSegment?.substringAfterLast('/')?.take(200)?.ifBlank { null } ?: if (mime.startsWith("video/")) "vybora-video.mp4" else "vybora-foto.jpg"
                                val fileId = VyboraApi.uploadPostMedia(context, token, uri, fileName, mime, size)
                                VyboraApi.createMediaPost(token, caption, fileId, if (mime.startsWith("video/")) "video" else "image")
                            }
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                publishing = false; caption = ""; selectedUri = null; selectedType = ""; showComposer = false
                                onNotice("Publicação enviada ao feed.")
                            }
                        } catch (e: Exception) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post { publishing = false; onNotice(e.message ?: "Não foi possível publicar.") }
                        }
                    }.start()
                }
            }
        }, modifier = Modifier.fillMaxWidth(), enabled = !publishing) {
            if (publishing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp)); Text(if (publishing) "Publicando..." else "Publicar no VYBORA")
        }
        OutlinedButton(onClick = { showComposer = false; selectedUri = null; selectedType = "" }, modifier = Modifier.fillMaxWidth()) { Text("Voltar à câmera") }
    }
}

@Composable
private fun LivePage(onNotice: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Lives", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Encontre transmissões ao vivo da comunidade.", color = Muted)
        Surface(color = Panel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Transmissões ao vivo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Inicie sua transmissão ao vivo pelo botão na parte inferior da tela.", color = Muted)
            }
        }
    }
}

@Composable
private fun SimplePage(title: String, description: String, action: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(color = Panel, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Violet, Pink))), contentAlignment = Alignment.Center) { Text("V", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black) }
                Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(description, color = Muted)
                Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) { Text(action) }
            }
        }
        Text("VYBORA • Protótipo Android", color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}
