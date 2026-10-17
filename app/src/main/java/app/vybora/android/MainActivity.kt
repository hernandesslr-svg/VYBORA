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
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.material.icons.filled.Cameraswitch
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
import androidx.compose.ui.input.pointer.pointerInput

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
    var messageRecipient by remember { mutableStateOf("") }
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
                NavigationBar(
                    containerColor = Color(0xFF030710),
                    contentColor = Color.White,
                    tonalElevation = 0.dp
                ) {
                    val navColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Pink,
                        selectedTextColor = Pink,
                        indicatorColor = Color(0xFF251039),
                        unselectedIconColor = Muted,
                        unselectedTextColor = Muted
                    )
                    NavigationBarItem(selected = tab == 0, onClick = { showSettings = false; showEditProfile = false; publicProfileUsername = ""; messageRecipient = ""; showAuth = false; tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Início") }, colors = navColors)
                    NavigationBarItem(selected = tab == 1, onClick = { showSettings = false; showEditProfile = false; publicProfileUsername = ""; showAuth = false; tab = 1 }, icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Mensagens") }, label = { Text("Mensagens") }, colors = navColors)
                    NavigationBarItem(selected = tab == 2, onClick = { showSettings = false; showEditProfile = false; publicProfileUsername = ""; showAuth = false; tab = 2; notice = "" }, icon = { Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Pink, Violet))), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(29.dp)) } }, label = { Text("Publicar") }, colors = navColors)
                    NavigationBarItem(selected = tab == 3, onClick = { showSettings = false; showEditProfile = false; publicProfileUsername = ""; showAuth = false; tab = 3 }, icon = { Icon(Icons.Default.Radio, null) }, label = { Text("Lives") }, colors = navColors)
                    NavigationBarItem(selected = tab == 4, onClick = { showSettings = false; showEditProfile = false; publicProfileUsername = ""; tab = 4 }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Perfil") }, colors = navColors)
                }
            }
        }) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
                if (showSettings || showEditProfile || publicProfileUsername.isNotBlank() || tab == 2) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(Violet, Pink))), contentAlignment = Alignment.Center) { Text("V", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall) }
                        Column(Modifier.padding(start = 10.dp).weight(1f)) {
                            Text("VYBORA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                            Text("Lives e vídeos curtos. Pra todo mundo.", style = MaterialTheme.typography.labelSmall, color = Muted)
                        }
                        IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Menu, contentDescription = "Abrir menu e configurações", tint = Color.White) }
                    }
                } else when (tab) {
                    0 -> Unit
                    1 -> Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Mensagens", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showSearchDialog = true }) { Icon(Icons.Default.Search, contentDescription = "Pesquisar pessoas para conversar", tint = Color.White) }
                        IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Menu, contentDescription = "Abrir menu e configurações", tint = Color.White) }
                    }
                    3 -> Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Lives", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showSearchDialog = true }) { Icon(Icons.Default.Search, contentDescription = "Pesquisar lives e pessoas", tint = Color.White) }
                        IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Menu, contentDescription = "Abrir menu e configurações", tint = Color.White) }
                    }
                    4 -> Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Menu, contentDescription = "Abrir menu e configurações", tint = Color.White) }
                    }
                    else -> Unit
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
                    1 -> MessagesPage(recipientUsername = messageRecipient, onSearch = { showSearchDialog = true }, onNotice = { notice = it })
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
                    if (tab == 1) {
                        messageRecipient = username
                    } else {
                        publicProfileUsername = username
                    }
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
        var selectedFeedSection by remember { mutableStateOf("Para você") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            listOf("Para você", "Seguindo", "Descobrir").forEach { section ->
                Column(Modifier.weight(1f).clickable { selectedFeedSection = section }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(section, color = if (selectedFeedSection == section) Color.White else Muted, fontWeight = if (selectedFeedSection == section) FontWeight.Bold else FontWeight.Medium)
                    Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(3.dp)).background(if (selectedFeedSection == section) Brush.horizontalGradient(listOf(Pink, Violet)) else Brush.horizontalGradient(listOf(Night, Night)))
                    )
                }
            }
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
private fun MessagesPage(recipientUsername: String, onSearch: () -> Unit, onNotice: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("vybora_session", android.content.Context.MODE_PRIVATE) }
    var draft by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<VyboraApi.MessageSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }

    LaunchedEffect(recipientUsername) {
        messages = emptyList()
        if (recipientUsername.isNotBlank()) {
            val token = prefs.getString("token", null)
            if (token.isNullOrBlank()) {
                onNotice("Entre na sua conta para abrir mensagens.")
            } else {
                loading = true
                Thread {
                    try {
                        val loaded = VyboraApi.getMessages(token, recipientUsername)
                        android.os.Handler(android.os.Looper.getMainLooper()).post { messages = loaded; loading = false }
                    } catch (e: Exception) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            loading = false
                            onNotice(e.message ?: "Não foi possível carregar a conversa.")
                        }
                    }
                }.start()
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (recipientUsername.isBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("Todas", "Não lidas", "Grupos", "Solicitações").forEachIndexed { index, label ->
                    Surface(
                        color = if (index == 0) Brush.horizontalGradient(listOf(Pink, Violet)).let { Color(0xFF9D1BCA) } else Panel,
                        shape = RoundedCornerShape(22.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (index == 0) Pink else Color(0xFF30334D))
                    ) {
                        Text(label, Modifier.padding(horizontal = 11.dp, vertical = 10.dp), color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(74.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF28103F), Color(0xFF111A32)))), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Pink, modifier = Modifier.size(34.dp))
                    }
                    Text("Suas conversas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Pesquise uma pessoa pela lupa para iniciar uma conversa.", color = Muted, style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onSearch, colors = ButtonDefaults.buttonColors(containerColor = Pink), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Encontrar pessoas")
                    }
                    Text("A lista de conversas recentes depende de um endpoint de caixa de entrada que ainda não existe no backend.", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Conversa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("@$recipientUsername", color = Muted)
                }
                TextButton(onClick = onSearch) { Text("Nova conversa", color = Pink) }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Pink)
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(messages, key = { it.id }) { message ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Surface(color = Panel, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp), modifier = Modifier.widthIn(max = 300.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(message.body, color = Color.White)
                                Text(message.createdAt, color = Muted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = draft, onValueChange = { draft = it.take(2000) }, modifier = Modifier.weight(1f), placeholder = { Text("Escreva uma mensagem…") }, shape = RoundedCornerShape(24.dp), maxLines = 4)
                Button(enabled = draft.isNotBlank() && !sending, onClick = {
                    val token = prefs.getString("token", null)
                    if (token.isNullOrBlank()) onNotice("Entre na sua conta para enviar mensagens.")
                    else {
                        sending = true
                        val body = draft.trim()
                        Thread {
                            try {
                                VyboraApi.sendMessage(token, recipientUsername, body)
                                val loaded = VyboraApi.getMessages(token, recipientUsername)
                                android.os.Handler(android.os.Looper.getMainLooper()).post { messages = loaded; draft = ""; sending = false }
                            } catch (e: Exception) {
                                android.os.Handler(android.os.Looper.getMainLooper()).post { sending = false; onNotice(e.message ?: "Não foi possível enviar a mensagem.") }
                            }
                        }.start()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Pink), shape = CircleShape, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("Enviar")
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
    var profilePosts by remember { mutableStateOf<List<VyboraApi.PostSummary>>(emptyList()) }
    var profileTab by remember { mutableStateOf("Vídeos") }
    var loadingPosts by remember { mutableStateOf(true) }
    var avatarUrl by remember { mutableStateOf("") }
    var coverUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1519608487953-e999c86e7455?auto=format&fit=crop&w=1200&q=85") }

    LaunchedEffect(username) {
        loadingPosts = true
        if (username.isNotBlank()) {
            Thread {
                try {
                    val response = VyboraApi.getPublicProfile(username)
                    val user = response.optJSONObject("user")
                    val loadedPosts = VyboraApi.getPosts().filter { it.username.equals(username, ignoreCase = true) }
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        if (user != null) {
                            postsCount = user.optInt("posts_count")
                            followersCount = user.optInt("followers")
                            followingCount = user.optInt("following")
                            avatarUrl = user.optString("avatarUrl", user.optString("avatar_url"))
                            val serverCover = user.optString("coverUrl", user.optString("cover_url"))
                            if (serverCover.isNotBlank()) coverUrl = serverCover
                        }
                        profilePosts = loadedPosts
                        loadingPosts = false
                    }
                } catch (_: Exception) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post { loadingPosts = false }
                }
            }.start()
        } else {
            postsCount = 0
            followersCount = 0
            followingCount = 0
            avatarUrl = ""
            profilePosts = emptyList()
            loadingPosts = false
        }
    }

    Column(Modifier.fillMaxSize().background(Night), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Box(Modifier.fillMaxWidth().height(190.dp)) {
            AsyncImage(
                model = coverUrl,
                contentDescription = "Capa do perfil",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Night.copy(alpha = 0.08f), Night))))
            Row(Modifier.align(Alignment.TopEnd).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onNotice("Abra o menu para acessar as configurações do perfil.") }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu do perfil", tint = Color.White)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().offset(y = (-32).dp).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(104.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Pink, Violet, Color(0xFF00C9FF))))
                    .padding(3.dp).clip(CircleShape).background(Night),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Foto do perfil",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Text(
                        (displayName.ifBlank { username }.ifBlank { "V" }).take(1).uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).padding(top = 25.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(displayName.ifBlank { if (username.isBlank()) "Seu nome" else username }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(if (username.isBlank()) "Entre ou crie sua conta" else "@$username", color = Muted)
            }
        }
        Row(
            Modifier.fillMaxWidth().offset(y = (-18).dp).padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ProfileStat("Publicações", postsCount.toString()) { onStat("Publicações") }
            ProfileStat("Seguidores", followersCount.toString()) { onStat("Seguidores") }
            ProfileStat("Seguindo", followingCount.toString()) { onStat("Seguindo") }
        }
        if (bio.isNotBlank()) {
            Text(bio, Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 2.dp), color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Editar perfil")
            }
            OutlinedButton(
                onClick = onAuth,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Violet)
            ) {
                Text(if (username.isBlank()) "Entrar" else "Trocar conta")
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Vídeos", "Curtidas", "Salvos", "Marcados").forEach { item ->
                Column(Modifier.weight(1f).clickable { profileTab = item }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(item, color = if (profileTab == item) Color.White else Muted, fontWeight = if (profileTab == item) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.labelLarge)
                    Box(Modifier.fillMaxWidth().height(3.dp).background(if (profileTab == item) Brush.horizontalGradient(listOf(Pink, Violet)) else Brush.horizontalGradient(listOf(Night, Night))))
                }
            }
        }
        if (profileTab != "Vídeos") {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Esta seção será exibida quando houver dados disponíveis.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        } else if (loadingPosts) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Pink) }
        } else if (profilePosts.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f).padding(22.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Muted, modifier = Modifier.size(42.dp))
                    Text("Você ainda não publicou nada", fontWeight = FontWeight.Bold)
                    Text("Compartilhe seu primeiro vídeo com a comunidade.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(profilePosts.size) { index ->
                    val post = profilePosts[index]
                    Box(Modifier.fillMaxWidth().height(145.dp).clip(RoundedCornerShape(5.dp)).background(Brush.linearGradient(listOf(Color(0xFF241047), Color(0xFF9D236E), Color(0xFF071A38))))) {
                        if (post.mediaUrl.isNotBlank() && post.mediaType == "image") {
                            AsyncImage(model = post.mediaUrl, contentDescription = "Publicação", modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                        } else {
                            Column(Modifier.align(Alignment.Center).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                if (post.mediaType == "video") Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                                Text(post.caption.ifBlank { "VYBORA" }, color = Color.White, maxLines = 4, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (post.mediaType == "video") Icon(Icons.Default.PlayCircle, contentDescription = "Vídeo", tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(5.dp))
                    }
                }
            }
        }
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
    var selectedEffect by remember { mutableStateOf("Original") }
    var imageCapture by remember { mutableStateOf<androidx.camera.core.ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<androidx.camera.video.VideoCapture<androidx.camera.video.Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<androidx.camera.video.Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var captureMode by remember { mutableStateOf("Foto") }
    var selectedDuration by remember { mutableStateOf("15 s") }
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
                    it.implementationMode = androidx.camera.view.PreviewView.ImplementationMode.COMPATIBLE
                    previewView = it
                } },
                modifier = Modifier.fillMaxSize().pointerInput(cameraControl, zoomRatio) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        val camera = cameraControl ?: return@detectTransformGestures
                        val maxZoom = camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
                        val minZoom = camera.cameraInfo.zoomState.value?.minZoomRatio ?: 1f
                        val nextZoom = (zoomRatio * zoomChange).coerceIn(minZoom, maxZoom)
                        camera.cameraControl.setZoomRatio(nextZoom)
                        zoomRatio = nextZoom
                    }
                }
            )
            val tint = when (selectedEffect) {
                "Golden Hour", "Bronze" -> Color(0x44FF9A42)
                "Frio", "Gelo" -> Color(0x443F8CFF)
                "Rosa", "Corações", "Glow" -> Color(0x44FF45B5)
                "Neon", "Óculos Neon" -> Color(0x443F12FF)
                "Verde" -> Color(0x4432FF70)
                "Mono", "VHS" -> Color(0x44808080)
                "Fogo" -> Color(0x44FF3300)
                else -> Color.Transparent
            }
            Box(Modifier.fillMaxSize().background(tint))
            val effectOverlay = when (selectedEffect) {
                "Corações" -> "💖 ✨ 💖"
                "Orelhas de gato" -> "🐱"
                "Cachorrinho" -> "🐶"
                "Coroa" -> "👑"
                "Óculos Neon" -> "🕶️"
                "Unicórnio" -> "🦄"
                "Borboletas" -> "🦋 ✨ 🦋"
                "Estrelas" -> "⭐ ✨ ⭐"
                "Chuva de confete" -> "🎉 ✨ 🎊"
                "Fogo" -> "🔥"
                "Bolhas" -> "🫧 🫧"
                else -> ""
            }
            if (effectOverlay.isNotEmpty()) {
                Text(effectOverlay, modifier = Modifier.align(Alignment.Center).padding(bottom = 120.dp),
                    color = Color.White, style = MaterialTheme.typography.headlineLarge)
            }
            Box(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    Modifier.align(Alignment.TopCenter).fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0x66100A18))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (isRecording) activeRecording?.stop(); onCameraCancelled() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Fechar câmera", tint = Color.White)
                    }
                    TextButton(onClick = { onNotice("A seleção de sons será integrada à biblioteca de áudio do VYBORA.") }, modifier = Modifier.weight(1f)) {
                        Text("♫  Adicionar som", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }

                Column(
                    Modifier.align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0x66201A2C))
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(if (flashEnabled) "⚡" else "ϟ", color = if (flashEnabled) Color(0xFFFFD66B) else Color.White,
                        modifier = Modifier.clip(CircleShape).clickable {
                            flashEnabled = !flashEnabled
                            imageCapture?.flashMode = if (flashEnabled) androidx.camera.core.ImageCapture.FLASH_MODE_ON else androidx.camera.core.ImageCapture.FLASH_MODE_OFF
                        }.padding(10.dp), style = MaterialTheme.typography.titleLarge)
                    Text("⏱", color = Color.White, modifier = Modifier.clip(CircleShape).clickable {
                        onNotice("Temporizador: escolha 3 ou 10 segundos na próxima atualização da câmera.")
                    }.padding(10.dp), style = MaterialTheme.typography.titleLarge)
                    Text("✨", color = Color.White, modifier = Modifier.clip(CircleShape).clickable {
                        selectedFilter = when (selectedFilter) {
                            "Original" -> "Quente"
                            "Quente" -> "Frio"
                            "Frio" -> "Rosa"
                            "Rosa" -> "Mono"
                            else -> "Original"
                        }
                    }.padding(10.dp), style = MaterialTheme.typography.titleLarge)
                    IconButton(enabled = !isRecording, onClick = {
                        lensFacing = if (lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_BACK)
                            androidx.camera.core.CameraSelector.LENS_FACING_FRONT
                        else androidx.camera.core.CameraSelector.LENS_FACING_BACK
                    }) {
                        Icon(Icons.Default.Cameraswitch, contentDescription = "Alternar câmera", tint = Color.White)
                    }
                }

                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(Brush.verticalGradient(listOf(Color(0x00150B20), Color(0xB8140B22), Color(0xF20B0A16))))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (cameraError.isNotBlank()) {
                        Text(cameraError, color = Color.White, style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        listOf("10 s", "60 s", "15 s", "Foto").forEach { option ->
                            val chosen = (option == "Foto" && captureMode == "Foto") ||
                                (option != "Foto" && captureMode == "Vídeo" && selectedDuration == option)
                            Surface(
                                color = if (chosen) Violet else Color(0x66171727),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.padding(horizontal = 3.dp).clickable {
                                    if (!isRecording) {
                                        if (option == "Foto") captureMode = "Foto"
                                        else { captureMode = "Vídeo"; selectedDuration = option }
                                    }
                                }
                            ) {
                                Text(option, color = Color.White, fontWeight = if (chosen) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        items(listOf(
                            "Original" to "✨", "Golden Hour" to "🌅", "Cinemático" to "🎬",
                            "Vintage" to "📼", "Mono" to "⚫", "Neon" to "🌈", "Frio" to "❄️",
                            "Rosa" to "🌸", "Verde" to "💚", "Corações" to "💖",
                            "Orelhas de gato" to "🐱", "Cachorrinho" to "🐶", "Coroa" to "👑",
                            "Óculos Neon" to "🕶️", "Unicórnio" to "🦄", "Borboletas" to "🦋",
                            "Estrelas" to "⭐", "Chuva de confete" to "🎉", "Fogo" to "🔥",
                            "Bolhas" to "🫧", "Glow" to "✨", "Bronze" to "☀️", "Gelo" to "🧊",
                            "VHS" to "📺"
                        ), key = { it.first }) { effect ->
                            val chosen = selectedEffect == effect.first
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(78.dp).clickable {
                                    selectedEffect = effect.first
                                    selectedFilter = when (effect.first) {
                                        "Golden Hour", "Bronze" -> "Quente"
                                        "Frio", "Gelo" -> "Frio"
                                        "Rosa", "Corações", "Glow", "Neon", "Óculos Neon" -> "Rosa"
                                        "Mono", "VHS" -> "Mono"
                                        else -> "Original"
                                    }
                                }
                            ) {
                                Surface(
                                    color = if (chosen) Pink.copy(alpha = 0.32f) else Color(0x66201A2C),
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (chosen) 2.dp else 1.dp,
                                        if (chosen) Pink else Color(0x667C7190)
                                    ),
                                    modifier = Modifier.size(58.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(effect.second, style = MaterialTheme.typography.headlineSmall)
                                    }
                                }
                                Text(
                                    effect.first,
                                    color = if (chosen) Color(0xFFFF80C8) else Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 2,
                                    minLines = 2,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 28.dp)
                                )
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                        IconButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.size(52.dp).background(Color(0x77392843), CircleShape)) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Galeria", tint = Color.White)
                        }
                        Box(Modifier.size(78.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFFF80C8), Color(0xFF9A5BFF), Color(0xFFFF2A9D)))).padding(5.dp).clip(CircleShape).background(if (isRecording) Pink else Color(0xFFFFFBFF)).clickable {
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
                        IconButton(onClick = { videoPicker.launch("video/*") }, modifier = Modifier.size(52.dp).background(Color(0x77171827), CircleShape)) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = "Escolher vídeo", tint = Color.White)
                        }
                    }
                    Text(
                        if (captureMode == "Vídeo") { if (isRecording) "● GRAVANDO — toque para finalizar" else "Vídeo • até $selectedDuration" }
                        else "Foto • toque no botão para capturar",
                        color = if (isRecording) Pink else Color.White.copy(alpha = 0.86f),
                        style = MaterialTheme.typography.labelSmall
                    )
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
    var selectedCategory by remember { mutableStateOf("Para você") }
    val categories = listOf("Para você", "Em alta", "Jogos", "Música", "Seguindo")
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            categories.forEachIndexed { index, category ->
                Surface(
                    color = if (selectedCategory == category) Color(0xFF9D1BCA) else Panel,
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedCategory == category) Pink else Color(0xFF292D48)),
                    modifier = Modifier.clickable { selectedCategory = category }
                ) {
                    Text(category, Modifier.padding(horizontal = 10.dp, vertical = 10.dp), color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().height(230.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF503064))
        ) {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF140D31), Color(0xFF5E155D), Color(0xFF10182D))))) {
                Column(Modifier.align(Alignment.Center).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(58.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Pink, Violet))), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Radio, contentDescription = null, tint = Color.White, modifier = Modifier.size(31.dp))
                    }
                    Text("Entre no universo das Lives", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Descubra criadores, jogos, música e conversas ao vivo.", color = Muted, style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { onNotice("As transmissões e a reprodução ao vivo ainda precisam ser conectadas ao servidor de mídia.") }, colors = ButtonDefaults.buttonColors(containerColor = Pink), shape = RoundedCornerShape(14.dp)) {
                        Text("Explorar transmissões")
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Lives recomendadas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("VYBORA LIVE", color = Pink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = Muted, modifier = Modifier.size(40.dp))
                Text("Nenhuma transmissão disponível", fontWeight = FontWeight.SemiBold)
                Text("Quando houver lives conectadas ao serviço, elas aparecerão aqui com público e criador reais.", color = Muted, style = MaterialTheme.typography.bodySmall)
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
