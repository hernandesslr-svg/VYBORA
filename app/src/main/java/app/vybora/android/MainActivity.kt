package app.vybora.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import java.io.FileOutputStream
import android.content.Intent
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

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

private fun createAdjustedImage(context: android.content.Context, source: Uri, ratio: String, filter: String): Uri {
    val original = context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it) }
        ?: throw IllegalArgumentException("Não foi possível abrir a imagem selecionada.")
    val targetRatio = when (ratio) {
        "1:1" -> 1f
        "4:5" -> 4f / 5f
        "16:9" -> 16f / 9f
        else -> original.width.toFloat() / original.height.toFloat()
    }
    val sourceRatio = original.width.toFloat() / original.height.toFloat()
    val cropWidth: Int
    val cropHeight: Int
    if (sourceRatio > targetRatio) {
        cropHeight = original.height
        cropWidth = (cropHeight * targetRatio).toInt().coerceIn(1, original.width)
    } else {
        cropWidth = original.width
        cropHeight = (cropWidth / targetRatio).toInt().coerceIn(1, original.height)
    }
    val left = (original.width - cropWidth) / 2
    val top = (original.height - cropHeight) / 2
    val cropped = Bitmap.createBitmap(original, left, top, cropWidth, cropHeight)
    if (cropped !== original) original.recycle()
    val output = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
    val matrix = ColorMatrix()
    when (filter) {
        "P&B" -> matrix.setSaturation(0f)
        "Vivo" -> matrix.setSaturation(1.45f)
        "Neon" -> matrix.setSaturation(1.7f)
        "Quente" -> matrix.set(floatArrayOf(1.08f,0f,0f,0f,12f, 0f,1f,0f,0f,2f, 0f,0f,0.9f,0f,0f, 0f,0f,0f,1f,0f))
        "Frio" -> matrix.set(floatArrayOf(0.92f,0f,0f,0f,0f, 0f,1f,0f,0f,0f, 0f,0f,1.12f,0f,10f, 0f,0f,0f,1f,0f))
    }
    Canvas(output).drawBitmap(cropped, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(matrix)
    })
    cropped.recycle()
    val file = File(context.cacheDir, "vybora-adjusted-${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { output.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    output.recycle()
    return Uri.fromFile(file)
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
    var showNotifications by remember { mutableStateOf(false) }
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
                    NavigationBarItem(selected = tab == 0 && !showNotifications, onClick = { showSettings = false; showEditProfile = false; showNotifications = false; publicProfileUsername = ""; messageRecipient = ""; showAuth = false; tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Início") }, colors = navColors)
                    NavigationBarItem(selected = tab == 1 && !showNotifications, onClick = { showSettings = false; showEditProfile = false; showNotifications = false; publicProfileUsername = ""; showAuth = false; tab = 1 }, icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Mensagens") }, label = { Text("Mensagens") }, colors = navColors)
                    NavigationBarItem(selected = tab == 2 && !showNotifications, onClick = { showSettings = false; showEditProfile = false; showNotifications = false; publicProfileUsername = ""; showAuth = false; tab = 2; notice = "" }, icon = { Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Pink, Violet))), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(29.dp)) } }, label = { Text("Publicar") }, colors = navColors)
                    NavigationBarItem(selected = tab == 3 && !showNotifications, onClick = { showSettings = false; showEditProfile = false; showNotifications = false; publicProfileUsername = ""; showAuth = false; tab = 3 }, icon = { Icon(Icons.Default.Radio, null) }, label = { Text("Lives") }, colors = navColors)
                    NavigationBarItem(selected = showNotifications, onClick = { showSettings = false; showEditProfile = false; showNotifications = true; publicProfileUsername = ""; showAuth = false }, icon = { Icon(Icons.Default.Notifications, contentDescription = "Avisos", tint = if (showNotifications) Pink else Muted) }, label = { Text("Avisos") }, colors = navColors)
                    NavigationBarItem(selected = tab == 4 && !showNotifications, onClick = { showSettings = false; showEditProfile = false; showNotifications = false; publicProfileUsername = ""; showAuth = false; tab = 4 }, icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") }, label = { Text("Perfil") }, colors = navColors)
                }
            }
        }) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
                if (publicProfileUsername.isNotBlank() || tab == 2) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(Violet, Pink))), contentAlignment = Alignment.Center) { Text("V", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall) }
                        Column(Modifier.padding(start = 10.dp).weight(1f)) {
                            Text("VYBORA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                            Text("Lives e vídeos curtos. Pra todo mundo.", style = MaterialTheme.typography.labelSmall, color = Muted)
                        }
                        IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Menu, contentDescription = "Abrir menu e configurações", tint = Color.White) }
                    }
                } else if (!showSettings && !showEditProfile) when (tab) {
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
                     else -> Unit
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
                            showAuth = true; tab = 4
                            notice = "Entre com a conta que deseja usar. Seus dados online não foram apagados."
                        }
                    )
                } else if (showNotifications) {
                    NotificationsPage(
                        token = appPrefs.getString("token", null) ?: "",
                        onBack = { showNotifications = false },
                        onOpen = { notification ->
                            showNotifications = false
                            if (notification.kind == "message" && notification.actorUsername.isNotBlank()) {
                                messageRecipient = notification.actorUsername
                                tab = 1
                            } else if (notification.actorUsername.isNotBlank()) {
                                publicProfileUsername = notification.actorUsername
                            }
                        },
                        onNotice = { notice = it }
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
                    0 -> FeedPage(currentUsername = loggedInUser, onNotice = { notice = it }, onOpenProfile = { publicProfileUsername = it }, onAddStory = { tab = 2 })
                    1 -> MessagesPage(recipientUsername = messageRecipient, onSearch = { showSearchDialog = true }, onOpenConversation = { messageRecipient = it }, onNotice = { notice = it })
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
                    }, onAuth = { showAuth = true }, onEdit = { showEditProfile = true }, onSettings = { showSettings = true })
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
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { profileStat = "" },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Night,
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { profileStat = "" }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(profileStat, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    if (profileStat == "Seguidores") "Pessoas que seguem você" else if (profileStat == "Seguindo") "Pessoas que você segue" else "Atividade do perfil",
                                    color = Muted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Divider(color = Color.White.copy(alpha = 0.08f))
                        if (profileStat == "Seguidores" || profileStat == "Seguindo") {
                            when {
                                loggedInUser.isBlank() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Entre na sua conta para consultar esta lista.", color = Muted)
                                }
                                profileStatUsers.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Nenhuma conta nesta lista.", color = Muted)
                                }
                                else -> LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    items(profileStatUsers, key = { it.id }) { user ->
                                        Row(
                                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
                                                publicProfileUsername = user.username
                                                profileStat = ""
                                            }.padding(horizontal = 8.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                Modifier.size(50.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Pink, Violet))),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (user.avatarUrl.isNotBlank()) {
                                                    AsyncImage(model = user.avatarUrl, contentDescription = "Foto de @${user.username}", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                                } else {
                                                    Text(user.displayName.ifBlank { user.username }.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Text(user.displayName.ifBlank { user.username }, fontWeight = FontWeight.SemiBold, color = Color.White)
                                                Text("@${user.username}", color = Muted, style = MaterialTheme.typography.bodySmall)
                                            }
                                            Icon(Icons.Default.ChevronRight, contentDescription = "Abrir perfil", tint = Muted)
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("As publicações da sua conta serão exibidas quando o feed estiver carregado.", color = Muted)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun NotificationsPage(
    token: String,
    onBack: () -> Unit,
    onOpen: (VyboraApi.NotificationSummary) -> Unit,
    onNotice: (String) -> Unit
) {
    var notifications by remember { mutableStateOf<List<VyboraApi.NotificationSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("Todas") }
    LaunchedEffect(token) {
        if (token.isBlank()) {
            loading = false
        } else {
            try {
                val loaded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { VyboraApi.getNotifications(token) }
                notifications = loaded
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { VyboraApi.markNotificationsRead(token) }
            } catch (e: Exception) {
                onNotice(e.message ?: "Não foi possível carregar as notificações.")
            } finally {
                loading = false
            }
        }
    }
    // Mensagens pertencem à aba Mensagens; Avisos mostra somente atividade e avisos do sistema.
    val visible = notifications.filter { item ->
        item.kind != "message" && when (selectedFilter) {
            "Atividades" -> item.kind != "system"
            "Do VYBORA" -> item.kind == "system"
            else -> true
        }
    }
    Column(Modifier.fillMaxSize().background(Night).padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White) }
            Text("Notificações", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Notifications, contentDescription = null, tint = Pink)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todas", "Atividades", "Do VYBORA").forEach { label ->
                Surface(
                    modifier = Modifier.clickable { selectedFilter = label },
                    color = if (selectedFilter == label) Color(0xFF35113B) else Panel,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedFilter == label) Pink else Color.Transparent)
                ) {
                    Text(label, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = if (selectedFilter == label) Pink else Muted, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Pink) }
            token.isBlank() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Entre na sua conta para ver as notificações.", color = Muted) }
            visible.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Violet, modifier = Modifier.size(48.dp))
                    Text("Você está em dia!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Novos seguidores, curtidas e comentários aparecerão aqui.", color = Muted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(visible, key = { it.id }) { item ->
                    val actor = item.actorDisplayName.ifBlank { item.actorUsername.ifBlank { "VYBORA" } }
                    val description = when (item.kind) {
                        "follow" -> "começou a seguir você"
                        "like" -> "curtiu sua publicação"
                        "comment" -> "comentou na sua publicação"
                        "message" -> "enviou uma mensagem"
                        "system" -> "aviso do VYBORA"
                        else -> "tem uma novidade para você"
                    }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (item.readAt.isBlank()) Color(0xFF171024) else Night)
                            .clickable { onOpen(item) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (item.actorAvatarUrl.isNotBlank()) {
                            AsyncImage(model = item.actorAvatarUrl, contentDescription = "Foto de $actor", modifier = Modifier.size(46.dp).clip(CircleShape), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                        } else {
                            Box(Modifier.size(46.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Pink, Violet))), contentAlignment = Alignment.Center) {
                                Icon(if (item.kind == "system") Icons.Default.Info else Icons.Default.Notifications, contentDescription = null, tint = Color.White)
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("$actor $description", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text(item.createdAt, color = Muted, style = MaterialTheme.typography.labelSmall)
                        }
                        if (item.readAt.isBlank()) Box(Modifier.size(8.dp).clip(CircleShape).background(Pink))
                    }
                }
            }
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
private fun InlineVideoPlayer(url: String, modifier: Modifier = Modifier, autoplay: Boolean = false) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            playWhenReady = autoplay
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        modifier = modifier.background(Color.Black),
        factory = { viewContext -> PlayerView(viewContext).apply {
            this.player = player
            useController = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)

            // VYBORA: manter apenas play/pause central e a barra de progresso.
            // A barra padrão continua permitindo tocar/arrastar para avançar ou voltar.
            post {
                listOf(
                    androidx.media3.ui.R.id.exo_prev,
                    androidx.media3.ui.R.id.exo_next,
                    androidx.media3.ui.R.id.exo_rew,
                    androidx.media3.ui.R.id.exo_ffwd
                ).forEach { controlId ->
                    findViewById<android.view.View>(controlId)?.visibility = android.view.View.GONE
                }
            }
        } },
        update = { view ->
            view.player = player
            view.post {
                listOf(
                    androidx.media3.ui.R.id.exo_prev,
                    androidx.media3.ui.R.id.exo_next,
                    androidx.media3.ui.R.id.exo_rew,
                    androidx.media3.ui.R.id.exo_ffwd
                ).forEach { controlId ->
                    view.findViewById<android.view.View>(controlId)?.visibility = android.view.View.GONE
                }
            }
        }
    )
}

@Composable
private fun FeedPage(currentUsername: String, onNotice: (String) -> Unit, onOpenProfile: (String) -> Unit, onAddStory: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("vybora_session", android.content.Context.MODE_PRIVATE) }
    var posts by remember { mutableStateOf<List<VyboraApi.PostSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var commentPost by remember { mutableStateOf<VyboraApi.PostSummary?>(null) }
    var comments by remember { mutableStateOf<List<VyboraApi.CommentSummary>>(emptyList()) }
    var commentDraft by remember { mutableStateOf("") }
    var commentsLoading by remember { mutableStateOf(false) }
    var expandedMedia by remember { mutableStateOf("") }
    var selectedStory by remember { mutableStateOf<Pair<String, String>?>(null) }
    var demoLiked by remember { mutableStateOf(setOf<String>()) }
    val demoPosts = remember {
        listOf(
            VyboraApi.PostSummary("demo-photo-1", "🧪 PERFIL DE TESTE · Um fim de tarde pela cidade ✨", "https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=1000&auto=format&fit=crop&q=85", "image", "marina_teste", "Marina • TESTE", 124, 18),
            VyboraApi.PostSummary("demo-video-1", "🧪 PERFIL DE TESTE · Rolê noturno 🌃", "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4", "video", "lucas_teste", "Lucas • TESTE", 87, 9),
            VyboraApi.PostSummary("demo-photo-2", "🧪 PERFIL DE TESTE · Lugares que dão paz 🌊", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1000&auto=format&fit=crop&q=85", "image", "ana_teste", "Ana • TESTE", 203, 31),
            VyboraApi.PostSummary("demo-video-2", "🧪 PERFIL DE TESTE · Um pouco de aventura 🚗", "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4", "video", "pedro_teste", "Pedro • TESTE", 64, 7),
            VyboraApi.PostSummary("demo-photo-3", "🧪 PERFIL DE TESTE · Inspiração para hoje 💜", "https://images.unsplash.com/photo-1518837695005-2083093ee35b?w=1000&auto=format&fit=crop&q=85", "image", "julia_teste", "Julia • TESTE", 156, 22),
            VyboraApi.PostSummary("demo-video-3", "🧪 PERFIL DE TESTE · Pequenos momentos 🎬", "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4", "video", "caio_teste", "Caio • TESTE", 92, 12),
            VyboraApi.PostSummary("demo-photo-4", "🧪 PERFIL DE TESTE · Céu de outra cor 🌆", "https://images.unsplash.com/photo-1472120435266-53107fd0c44a?w=1000&auto=format&fit=crop&q=85", "image", "bia_teste", "Bia • TESTE", 311, 45),
            VyboraApi.PostSummary("demo-photo-5", "🧪 PERFIL DE TESTE · Explorando novos lugares 📍", "https://images.unsplash.com/photo-1519501025264-65ba15a82390?w=1000&auto=format&fit=crop&q=85", "image", "enzo_teste", "Enzo • TESTE", 78, 6),
            VyboraApi.PostSummary("ana-rose-01", "🌆 Uma noite cheia de luzes ✨ · PERFIL DE TESTE", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=1000&auto=format&fit=crop&q=85", "image", "ana_rose", "Ana Rose • TESTE", 1250, 84),
            VyboraApi.PostSummary("ana-rose-02", "💜 Um pouco de cor no meu dia", "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=1000&auto=format&fit=crop&q=85", "image", "ana_rose", "Ana Rose • TESTE", 986, 52),
            VyboraApi.PostSummary("ana-rose-03", "☀️ Energia boa e sorriso no rosto", "https://images.unsplash.com/photo-1529139574466-a303027c1d8b?w=1000&auto=format&fit=crop&q=85", "image", "ana_rose", "Ana Rose • TESTE", 1432, 97),
            VyboraApi.PostSummary("ana-rose-04", "🌴 Colecionando momentos especiais", "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=1000&auto=format&fit=crop&q=85", "image", "ana_rose", "Ana Rose • TESTE", 1104, 63),
            VyboraApi.PostSummary("ana-rose-05", "📸 Um registro de hoje", "https://images.unsplash.com/photo-1526510747491-58f928ec870f?w=1000&auto=format&fit=crop&q=85", "image", "ana_rose", "Ana Rose • TESTE", 875, 41),
            VyboraApi.PostSummary("ana-rose-06", "🌺 Cores que combinam comigo", "https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?w=1000&auto=format&fit=crop&q=85", "image", "ana_rose", "Ana Rose • TESTE", 1678, 102),
            VyboraApi.PostSummary("ana-rose-07", "✨ Fazendo memórias bonitas", "https://images.unsplash.com/photo-1508214682278-9c4d8b4a1b6b?w=1000&auto=format&fit=crop&q=85", "image", "ana_rose", "Ana Rose • TESTE", 932, 48),
            VyboraApi.PostSummary("ana-rose-08", "🖤 Um look para guardar", "https://images.unsplash.com/photo-1539109136881-3be0616acf4b?w=1000&auto=format&fit=crop&q=85", "image", "ana_rose", "Ana Rose • TESTE", 2011, 136)
        )
    }
    val displayedPosts = if (posts.isEmpty() && !loading) demoPosts else posts
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
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.width(62.dp).clickable { onAddStory() }) {
                    Box(Modifier.size(58.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Pink, Violet))).padding(2.dp).clip(CircleShape).background(Night), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar story", tint = Pink, modifier = Modifier.size(28.dp))
                    }
                    Text("Seu story", color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
            items(listOf(
                "ana_rose" to "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=240&auto=format&fit=crop&q=80",
                "caiosilva" to "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=240&auto=format&fit=crop&q=80",
                "marireal" to "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=240&auto=format&fit=crop&q=80",
                "joaovitor" to "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=240&auto=format&fit=crop&q=80",
                "leticia" to "https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?w=240&auto=format&fit=crop&q=80"
            )) { story ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.width(62.dp).clickable { selectedStory = story }) {
                    Box(Modifier.size(58.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Pink, Violet))).padding(2.dp).clip(CircleShape).background(Night).padding(2.dp)) {
                        AsyncImage(model = story.second, contentDescription = "Abrir story de @" + story.first, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    }
                    Text(story.first, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (!loading && posts.isEmpty()) {
            Surface(color = Color(0xFF251039), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("Modo demonstração: publicações e perfis fictícios para testar a rolagem. Nada foi publicado por usuários reais.", modifier = Modifier.padding(12.dp), color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (!loading && displayedPosts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ainda não há publicações", fontWeight = FontWeight.SemiBold)
                    Text("Quando alguém publicar, as postagens públicas aparecerão aqui.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(displayedPosts, key = { it.id }) { post ->
                    var postMenuExpanded by remember(post.id) { mutableStateOf(false) }
                    Surface(color = Night, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                val avatarUrl = when (post.username.lowercase()) {
                                    "ana_rose", "ana_teste" -> "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=240&auto=format&fit=crop&q=80"
                                    "caiosilva", "caio_teste" -> "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=240&auto=format&fit=crop&q=80"
                                    "marireal", "marina_teste" -> "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=240&auto=format&fit=crop&q=80"
                                    "joaovitor", "lucas_teste" -> "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=240&auto=format&fit=crop&q=80"
                                    else -> "https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?w=240&auto=format&fit=crop&q=80"
                                }
                                AsyncImage(model = avatarUrl, contentDescription = "Abrir perfil de @${post.username}", modifier = Modifier.size(42.dp).clip(CircleShape).clickable { if (post.username.isNotBlank()) onOpenProfile(post.username) }, contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f).clickable { if (post.username.isNotBlank()) onOpenProfile(post.username) }) {
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
                                AsyncImage(model = post.mediaUrl, contentDescription = "Ampliar imagem da publicação", modifier = Modifier.fillMaxWidth().height(520.dp).clip(RoundedCornerShape(14.dp)).clickable { expandedMedia = post.mediaUrl }, contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                            } else if (post.mediaUrl.isNotBlank() && post.mediaType == "video") {
                                if (post.id.startsWith("demo-video-")) {
                                    val thumbnail = when (post.id) {
                                        "demo-video-1" -> "https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=1000&auto=format&fit=crop&q=85"
                                        "demo-video-2" -> "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1000&auto=format&fit=crop&q=85"
                                        else -> "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1000&auto=format&fit=crop&q=85"
                                    }
                                    Box(Modifier.fillMaxWidth().height(520.dp).clip(RoundedCornerShape(14.dp))) {
                                        InlineVideoPlayer(url = post.mediaUrl, modifier = Modifier.fillMaxSize(), autoplay = true)
                                        Surface(Modifier.align(Alignment.TopStart).padding(10.dp), color = Color.Black.copy(alpha = 0.65f), shape = RoundedCornerShape(20.dp)) {
                                            Text("VÍDEO DE TESTE", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    InlineVideoPlayer(url = post.mediaUrl, modifier = Modifier.fillMaxWidth().height(520.dp), autoplay = true)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    if (post.id.startsWith("demo-") || post.id.startsWith("ana-rose-")) {
                                        demoLiked = if (post.id in demoLiked) demoLiked - post.id else demoLiked + post.id
                                    } else {
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
                                    }
                                }) { Icon(Icons.Default.FavoriteBorder, contentDescription = if (post.id in demoLiked) "Descurtir" else "Curtir", tint = if (post.id in demoLiked) Pink else Color.White) }
                                Text("${post.likes + if (post.id in demoLiked) 1 else 0} curtidas", color = Muted, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.width(16.dp))
                                TextButton(onClick = {
                                    commentPost = post
                                    commentDraft = ""
                                    commentsLoading = true
                                    if (post.id.startsWith("demo-") || post.id.startsWith("ana-rose-")) {
                                        val demoPeople = listOf(
                                            "marireal" to "Mari Real",
                                            "joaovitor" to "João Vitor",
                                            "caiosilva" to "Caio Silva",
                                            "leticia" to "Letícia Alves",
                                            "pedro_santos" to "Pedro Santos",
                                            "bia_mendes" to "Bia Mendes",
                                            "enzo_lima" to "Enzo Lima",
                                            "julia_moraes" to "Júlia Moraes",
                                            "rafael_costa" to "Rafael Costa",
                                            "nina_rocha" to "Nina Rocha",
                                            "gui_oliveira" to "Gui Oliveira",
                                            "luana_ferreira" to "Luana Ferreira",
                                            "bruno_alves" to "Bruno Alves",
                                            "carol_souza" to "Carol Souza",
                                            "duda_martins" to "Duda Martins",
                                            "leo_barros" to "Léo Barros"
                                        )
                                        val demoMessages = listOf(
                                            "Que foto maravilhosa!", "A vibe desse post tá perfeita ✨", "Quero conhecer esse lugar!",
                                            "Ficou lindo demais 💜", "Amei os detalhes!", "Essa luz ficou incrível 😍",
                                            "Posta mais fotos assim!", "Que cenário bonito!", "Salvei de inspiração 📌",
                                            "Combinação perfeita de cores!", "Muito bom esse registro 👏", "Ficou com cara de cinema 🎬",
                                            "Adorei a energia dessa publicação!", "Esse merece muitas curtidas 🔥", "Simplesmente perfeito!",
                                            "A composição ficou ótima!", "Já quero ir também 😍", "Que momento especial!",
                                            "Curti muito o resultado!", "O feed fica lindo com posts assim 💖",
                                            "Amei a escolha!", "Que lugar aconchegante!", "Arrasou nessa!", "Não canso de olhar ✨",
                                            "Ficou muito natural!", "Esse detalhe fez toda a diferença.", "Muito estilo! 🔥",
                                            "Compartilhando porque ficou demais!", "Uma das minhas favoritas 💜", "Que registro incrível!",
                                            "A qualidade ficou ótima!", "Esse post transmite paz 🌊", "Perfeito para guardar de lembrança.",
                                            "O ângulo ficou sensacional!", "Quero ver a parte dois 👀", "Isso ficou bom demais!",
                                            "As cores estão lindas!", "Deu vontade de sair e explorar 📍", "Muito autêntico!",
                                            "Esse conteúdo merece destaque ⭐"
                                        )
                                        val demoClosers = listOf("", " 💜", " 😍", " ✨", " 👏", " 🔥", " ❤️", " 📸")
                                        val demoAdditions = listOf("", " Eu precisava ver isso hoje.", " Concordo demais!", " Voltei aqui para comentar de novo.", " Essa publicação merece destaque.")
                                        val seed = post.id.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
                                        comments = List(post.comments.coerceIn(0, 200)) { index ->
                                            val personIndex = (index * 7 + seed) % demoPeople.size
                                            val messageIndex = (index * 13 + seed / 3) % demoMessages.size
                                            val closerIndex = (index + seed) % demoClosers.size
                                            val person = demoPeople[personIndex]
                                            val body = demoMessages[messageIndex] + demoClosers[closerIndex] + demoAdditions[(index / 40).coerceAtMost(demoAdditions.lastIndex)]
                                            VyboraApi.CommentSummary(
                                                "demo-${post.id}-comment-${index + 1}",
                                                body,
                                                person.first,
                                                person.second
                                            )
                                        }
                                        commentsLoading = false
                                    } else Thread {
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
    if (expandedMedia.isNotBlank()) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { expandedMedia = "" }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black).clickable { expandedMedia = "" }, contentAlignment = Alignment.Center) {
                AsyncImage(model = expandedMedia, contentDescription = "Imagem ampliada", modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                IconButton(onClick = { expandedMedia = "" }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) { Icon(Icons.Default.Close, contentDescription = "Fechar imagem", tint = Color.White) }
            }
        }
    }
    if (selectedStory != null) {
        val story = selectedStory!!
        androidx.compose.ui.window.Dialog(onDismissRequest = { selectedStory = null }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black).clickable { selectedStory = null }) {
                AsyncImage(model = story.second.replace("w=240", "w=1200"), contentDescription = "Story de @" + story.first, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                Row(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = story.second, contentDescription = null, modifier = Modifier.size(42.dp).clip(CircleShape), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    Text("@" + story.first, modifier = Modifier.padding(start = 10.dp).weight(1f), color = Color.White, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { selectedStory = null }) { Icon(Icons.Default.Close, contentDescription = "Fechar story", tint = Color.White) }
                }
                Text("STORY DE TESTE", modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
    var showCommentEmojis by remember { mutableStateOf(false) }
    val activePost = commentPost
    if (activePost != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { commentPost = null },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.58f))
                    .clickable { commentPost = null },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.74f)
                        .heightIn(min = 320.dp, max = 640.dp)
                        .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF171722), Color(0xFF090A12))))
                        .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { }
                ) {
                    Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.width(48.dp).height(5.dp).clip(CircleShape).background(Color(0xFF777887)))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 18.dp, top = 8.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Comentários", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text("  ${comments.size}", color = Muted, fontSize = 20.sp, modifier = Modifier.weight(1f))
                        var sortComments by remember { mutableStateOf("Principais") }
                        TextButton(onClick = { sortComments = if (sortComments == "Principais") "Recentes" else "Principais" }) {
                            Text(sortComments, color = Color(0xFFD8D5E7), fontSize = 14.sp)
                            Icon(Icons.Default.ChevronRight, contentDescription = "Ordenar comentários", tint = Color(0xFFD8D5E7), modifier = Modifier.padding(start = 3.dp).size(18.dp))
                        }
                    }
                    HorizontalDivider(color = Color(0xFF292936))
                    if (commentsLoading) LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Pink,
                        trackColor = Color(0xFF292936)
                    )
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier.size(48.dp).clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(Violet, Pink))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        comment.displayName.ifBlank { comment.username }.ifBlank { "U" }.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                                Column(modifier = Modifier.weight(1f).padding(start = 14.dp, end = 8.dp)) {
                                    Text(
                                        comment.displayName.ifBlank { comment.username }.ifBlank { "Usuário" },
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                    Text("agora", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                                    Text(comment.body, color = Color(0xFFF2F0F7), fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                                    Text(
                                        "Responder",
                                        color = Color(0xFFB9B7CA),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 10.dp).clickable { commentDraft = "@${comment.username} " }
                                    )
                                }
                                Icon(Icons.Default.FavoriteBorder, contentDescription = "Curtir comentário", tint = Color(0xFFB9B7CA), modifier = Modifier.padding(top = 6.dp).size(22.dp))
                            }
                            HorizontalDivider(color = Color(0xFF252531))
                        }
                        if (comments.isEmpty() && !commentsLoading) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                    Text("Seja a primeira pessoa a comentar.", color = Muted)
                                }
                            }
                        }
                    }
                    if (showCommentEmojis) {
                        Row(
                            modifier = Modifier.offset(y = (-72).dp)
                                .fillMaxWidth()
                                .background(Color(0xFF0C0D16))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("❤️", "😂", "😍", "🥰", "🔥", "👏", "😭", "😮", "😎", "💜").forEach { emoji ->
                                Text(
                                    emoji,
                                    fontSize = 23.sp,
                                    modifier = Modifier.clickable {
                                        commentDraft = (commentDraft + emoji).take(1200)
                                    }.padding(horizontal = 2.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.offset(y = (-72).dp),
                        color = Color(0xFF303041)
                    )
                    Row(
                        modifier = Modifier.offset(y = (-72).dp)
                            .fillMaxWidth()
                            .background(Color(0xFF191925))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Violet, Pink))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Seu perfil", tint = Color.White)
                        }
                        Text(
                            "😊",
                            fontSize = 23.sp,
                            modifier = Modifier.clickable { showCommentEmojis = !showCommentEmojis }
                                .padding(end = 2.dp)
                        )
                        OutlinedTextField(
                            value = commentDraft,
                            onValueChange = { commentDraft = it.take(1200) },
                            placeholder = { Text("Adicione um comentário...", color = Muted) },
                            modifier = Modifier.weight(1f).padding(start = 10.dp),
                            shape = RoundedCornerShape(28.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF686879),
                                unfocusedBorderColor = Color(0xFF555564),
                                cursorColor = Pink,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        Spacer(Modifier.width(10.dp))
                        IconButton(
                            enabled = commentDraft.isNotBlank() && !commentsLoading,
                            onClick = {
                                val draftToSend = commentDraft.trim()
                                val token = prefs.getString("token", null)
                                if (draftToSend.isBlank()) return@IconButton
                                if (activePost.id.startsWith("demo-") || activePost.id.startsWith("ana-rose-")) {
                                    val newComment = VyboraApi.CommentSummary(
                                        "local-${System.currentTimeMillis()}",
                                        draftToSend,
                                        prefs.getString("username", "voce") ?: "voce",
                                        prefs.getString("display_name", "Você") ?: "Você"
                                    )
                                    comments = comments + newComment
                                    commentDraft = ""
                                } else if (token.isNullOrBlank()) {
                                    onNotice("Entre na sua conta para comentar.")
                                } else {
                                    commentsLoading = true
                                    Thread {
                                        try {
                                            VyboraApi.addComment(token, activePost.id, draftToSend)
                                            val loadedComments = VyboraApi.getComments(activePost.id)
                                            val loadedPosts = VyboraApi.getPosts()
                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                comments = loadedComments
                                                posts = loadedPosts
                                                commentDraft = ""
                                                commentsLoading = false
                                            }
                                        } catch (e: Exception) {
                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                commentsLoading = false
                                                onNotice(e.message ?: "Não foi possível publicar o comentário.")
                                            }
                                        }
                                    }.start()
                                }
                            },
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                                .background(if (commentDraft.isNotBlank()) Brush.linearGradient(listOf(Pink, Violet)) else Brush.linearGradient(listOf(Color(0xFF454553), Color(0xFF454553))))
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Enviar comentário", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessagesPage(recipientUsername: String, onSearch: () -> Unit, onOpenConversation: (String) -> Unit, onNotice: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("vybora_session", android.content.Context.MODE_PRIVATE) }
    var draft by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<VyboraApi.MessageSummary>>(emptyList()) }
    var inbox by remember { mutableStateOf<List<VyboraApi.InboxSummary>>(emptyList()) }
    var loadingInbox by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }

    LaunchedEffect(recipientUsername) {
        messages = emptyList()
        if (recipientUsername.isBlank()) {
            val token = prefs.getString("token", null)
            if (!token.isNullOrBlank()) {
                loadingInbox = true
                try {
                    inbox = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { VyboraApi.getInbox(token) }
                } catch (e: Exception) {
                    onNotice(e.message ?: "Não foi possível carregar suas conversas.")
                } finally {
                    loadingInbox = false
                }
            } else {
                inbox = emptyList()
                loadingInbox = false
            }
        }
        if (recipientUsername.isNotBlank()) {
            val token = prefs.getString("token", null)
            if (token.isNullOrBlank()) {
                onNotice("Entre na sua conta para abrir mensagens.")
            } else {
                loading = true
                var firstLoad = true
                while (true) {
                    try {
                        val loaded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            VyboraApi.getMessages(token, recipientUsername)
                        }
                        messages = loaded
                        loading = false
                    } catch (e: Exception) {
                        loading = false
                        if (firstLoad) onNotice(e.message ?: "Não foi possível carregar a conversa.")
                    }
                    firstLoad = false
                    kotlinx.coroutines.delay(5000)
                }
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
            when {
                loadingInbox -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Pink) }
                inbox.isNotEmpty() -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(5.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(inbox, key = { it.username }) { conversation ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onOpenConversation(conversation.username) }.padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (conversation.avatarUrl.isNotBlank()) AsyncImage(model = conversation.avatarUrl, contentDescription = "Foto de perfil", modifier = Modifier.size(54.dp).clip(CircleShape), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                            else Box(Modifier.size(54.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Pink, Violet))), contentAlignment = Alignment.Center) {
                                Text(conversation.displayName.ifBlank { conversation.username }.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(conversation.displayName.ifBlank { conversation.username }, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("@${conversation.username}", color = Muted, style = MaterialTheme.typography.labelSmall)
                                Text(conversation.body, color = Muted, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (conversation.unreadCount > 0) Box(Modifier.size(24.dp).clip(CircleShape).background(Pink), contentAlignment = Alignment.Center) {
                                Text(conversation.unreadCount.coerceAtMost(99).toString(), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    }
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
                    val ownId = prefs.getString("user_id", "") ?: ""
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (ownId.isNotBlank() && message.senderId == ownId) Arrangement.End else Arrangement.Start) {
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
private fun ProfilePage(username: String, displayName: String, bio: String, onNotice: (String) -> Unit, onStat: (String) -> Unit, onAuth: () -> Unit, onEdit: () -> Unit, onSettings: () -> Unit) {
    var postsCount by remember { mutableIntStateOf(0) }
    var followersCount by remember { mutableIntStateOf(0) }
    var followingCount by remember { mutableIntStateOf(0) }
    var profilePosts by remember { mutableStateOf<List<VyboraApi.PostSummary>>(emptyList()) }
    var profileTab by remember { mutableStateOf("Vídeos") }
    var loadingPosts by remember { mutableStateOf(true) }
    var avatarUrl by remember { mutableStateOf("") }
    var coverUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1519608487953-e999c86e7455?auto=format&fit=crop&w=1200&q=85") }
    var expandedImage by remember { mutableStateOf("") }

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
                        profilePosts = if (username.equals("ana_rose", ignoreCase = true) && loadedPosts.isEmpty()) listOf(
                            VyboraApi.PostSummary("ana-rose-01", "🌆 Uma noite cheia de luzes ✨ · PERFIL DE TESTE", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=1000&auto=format&fit=crop&q=85", "image", "ana_rose", "Ana Rose • TESTE", 1250, 84),
                            VyboraApi.PostSummary("ana-rose-02", "💜 Um pouco de cor no meu dia", "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=1000&auto=format&fit=crop&q=85", "image", "ana_rose", "Ana Rose • TESTE", 986, 52),
                            VyboraApi.PostSummary("ana-rose-03", "☀️ Energia boa e sorriso no rosto", "https://images.unsplash.com/photo-1529139574466-a303027c1d8b?w=1000&auto=format&fit=crop&q=85", "image", "ana_rose", "Ana Rose • TESTE", 1432, 97),
                            VyboraApi.PostSummary("ana-rose-04", "🌴 Colecionando momentos especiais", "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=1000&auto=format&fit=crop&q=85", "image", "ana_rose", "Ana Rose • TESTE", 1104, 63),
                            VyboraApi.PostSummary("ana-rose-05", "📸 Um registro de hoje", "https://images.unsplash.com/photo-1526510747491-58f928ec870f?w=1000&auto=format&fit=crop&q=85", "image", "ana_rose", "Ana Rose • TESTE", 875, 41),
                            VyboraApi.PostSummary("ana-rose-06", "🌺 Cores que combinam comigo", "https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?w=1000&auto=format&fit=crop&q=85", "image", "ana_rose", "Ana Rose • TESTE", 1678, 102),
                            VyboraApi.PostSummary("ana-rose-07", "✨ Fazendo memórias bonitas", "https://images.unsplash.com/photo-1508214682278-9c4d8b4a1b6b?w=1000&auto=format&fit=crop&q=85", "image", "ana_rose", "Ana Rose • TESTE", 932, 48),
                            VyboraApi.PostSummary("ana-rose-08", "🖤 Um look para guardar", "https://images.unsplash.com/photo-1539109136881-3be0616acf4b?w=1000&auto=format&fit=crop&q=85", "image", "ana_rose", "Ana Rose • TESTE", 2011, 136)
                        ) else loadedPosts
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
        Box(Modifier.fillMaxWidth().height(168.dp)) {
            AsyncImage(
                model = coverUrl,
                contentDescription = "Capa do perfil",
                modifier = Modifier.fillMaxSize().clickable { expandedImage = coverUrl },
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Box(Modifier.fillMaxSize().clickable { expandedImage = coverUrl }.background(Brush.verticalGradient(listOf(Color.Transparent, Night.copy(alpha = 0.08f), Night))))
            Row(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Editar perfil", tint = Color.White) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSettings) { Icon(Icons.Default.Menu, contentDescription = "Configurações", tint = Color.White) }
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
                        modifier = Modifier.fillMaxSize().clip(CircleShape).clickable { expandedImage = avatarUrl },
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
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Vídeos", "Curtidas", "Salvos", "Marcados").forEach { item ->
                Column(Modifier.weight(1f).clickable { profileTab = item }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    val tabIcon = when (item) {
                        "Vídeos" -> Icons.Default.VideoLibrary
                        "Curtidas" -> Icons.Default.FavoriteBorder
                        "Salvos" -> Icons.Default.FileDownload
                        else -> Icons.Default.Person
                    }
                    Icon(tabIcon, contentDescription = item, tint = if (profileTab == item) Pink else Muted, modifier = Modifier.size(21.dp))
                    Text(item, color = if (profileTab == item) Color.White else Muted, fontWeight = if (profileTab == item) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.labelMedium)
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
    if (expandedImage.isNotBlank()) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { expandedImage = "" }) {
            Box(Modifier.fillMaxWidth().heightIn(max = 620.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black)) {
                AsyncImage(model = expandedImage, contentDescription = "Imagem ampliada", modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp, max = 620.dp).clickable { expandedImage = "" }, contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                IconButton(onClick = { expandedImage = "" }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)) { Icon(Icons.Default.ArrowBack, contentDescription = "Fechar imagem", tint = Color.White) }
            }
        }
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
                    GoogleSignIn.signOut(activity) {
                        try {
                            googleSignInLauncher.launch(GoogleSignIn.signInIntent(activity))
                        } catch (e: Exception) {
                            loading = false
                            error = e.message ?: "Não foi possível abrir o login Google."
                        }
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
    var selectedAvatar by remember { mutableStateOf<Uri?>(null) }
    var selectedCover by remember { mutableStateOf<Uri?>(null) }
    var adjustingImage by remember { mutableStateOf("") }
    var cropRatio by remember { mutableStateOf("Livre") }
    var imageFilter by remember { mutableStateOf("Original") }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) { selectedAvatar = uri; adjustingImage = "avatar"; cropRatio = "1:1"; imageFilter = "Original" } }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) { selectedCover = uri; adjustingImage = "cover"; cropRatio = "16:9"; imageFilter = "Original" } }

    if (adjustingImage.isNotBlank()) {
        val previewUri = if (adjustingImage == "avatar") selectedAvatar else selectedCover
        AlertDialog(
            onDismissRequest = { adjustingImage = "" },
            containerColor = Panel,
            title = { Text(if (adjustingImage == "avatar") "Ajustar foto de perfil" else "Ajustar capa do perfil", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.fillMaxWidth().height(if (adjustingImage == "avatar") 240.dp else 140.dp).clip(RoundedCornerShape(12.dp)).background(Night), contentAlignment = Alignment.Center) {
                        if (previewUri != null) AsyncImage(model = previewUri, contentDescription = "Prévia para ajuste", modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                        val overlay = when (imageFilter) {
                            "Vivo" -> Color(0x22FF2A9D)
                            "Neon" -> Color(0x338B35FF)
                            "Quente" -> Color(0x33FF8A3D)
                            "Frio" -> Color(0x333D9BFF)
                            "P&B" -> Color(0x55808080)
                            else -> Color.Transparent
                        }
                        Box(Modifier.fillMaxSize().background(overlay))
                        Box(Modifier.fillMaxSize().padding(12.dp).border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(if (cropRatio == "1:1") 1000.dp else 8.dp)))
                    }
                    Text("Proporção", color = Muted, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Livre", "1:1", "4:5", "16:9").forEach { ratio -> FilterChip(selected = cropRatio == ratio, onClick = { cropRatio = ratio }, label = { Text(ratio) }) }
                    }
                    Text("Filtros", color = Muted, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf("Original", "Vivo", "Neon", "Quente", "Frio", "P&B").forEach { filter -> FilterChip(selected = imageFilter == filter, onClick = { imageFilter = filter }, label = { Text(filter) }) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = {
                try {
                    previewUri?.let { adjusted ->
                        val result = createAdjustedImage(context, adjusted, cropRatio, imageFilter)
                        if (adjustingImage == "avatar") selectedAvatar = result else selectedCover = result
                    }
                    adjustingImage = ""
                } catch (e: Exception) {
                    onNotice(e.message ?: "Não foi possível ajustar a imagem.")
                }
            }) { Text("Concluir", color = Pink) } },
            dismissButton = { TextButton(onClick = { if (adjustingImage == "avatar") selectedAvatar = null else selectedCover = null; adjustingImage = "" }) { Text("Cancelar", color = Muted) } }
        )
    }

    Column(Modifier.fillMaxSize().background(Night).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = !loading) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White) }
            Text("Editar perfil", modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Button(
                onClick = {
                    if (name.isBlank()) error = "Informe seu nome."
                    else if (!username.matches(Regex("[a-z0-9_.]{3,24}"))) error = "O @usuário precisa ter de 3 a 24 caracteres."
                    else {
                        loading = true; error = ""
                        val token = prefs.getString("token", null)
                        Thread {
                            try {
                                if (token.isNullOrBlank()) {
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        loading = false
                                        error = "Entre na sua conta para salvar as alterações."
                                    }
                                    return@Thread
                                }
                                VyboraApi.updateProfile(token, username.trim(), name.trim(), bio.trim())
                                selectedAvatar?.let { VyboraApi.uploadProfileImage(context, token, it, "avatar") }
                                selectedCover?.let { VyboraApi.uploadProfileImage(context, token, it, "cover") }
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    loading = false
                                    onSaved(username.trim(), name.trim(), bio.trim())
                                    onNotice("Perfil atualizado e imagens salvas.")
                                }
                            } catch (e: Exception) {
                                android.os.Handler(android.os.Looper.getMainLooper()).post { loading = false; error = e.message ?: "Não foi possível salvar no servidor." }
                            }
                        }.start()
                    }
                },
                enabled = !loading,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 9.dp)
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else Text("Salvar", fontWeight = FontWeight.Bold)
            }
        }

        Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().height(164.dp).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Color(0xFF40105E), Color(0xFF151A3C), Color(0xFF07121E))))) {
                if (selectedCover != null) {
                    AsyncImage(model = selectedCover, contentDescription = "Prévia da capa", modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                } else {
                    AsyncImage(model = "https://images.unsplash.com/photo-1519608487953-e999c86e7455?auto=format&fit=crop&w=1200&q=85", contentDescription = "Capa do perfil", modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Night.copy(alpha = 0.5f)))))
                OutlinedButton(
                    onClick = { coverPicker.launch("image/*") },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, Violet),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Night.copy(alpha = 0.78f), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Pink); Spacer(Modifier.width(7.dp)); Text("Trocar capa")
                }
            }

            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp).offset(y = (-25).dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(94.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Pink, Violet, Color(0xFF00C9FF)))).padding(3.dp).clip(CircleShape).background(Night), contentAlignment = Alignment.Center) {
                    if (selectedAvatar != null) {
                        AsyncImage(model = selectedAvatar, contentDescription = "Prévia da foto de perfil", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    } else {
                        Text((name.ifBlank { username }.ifBlank { "V" }).take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                    }

                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(name.ifBlank { "Seu nome" }, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("@${username.ifBlank { "seuusuario" }}", color = Muted)
                    Text("Trocar foto", color = Pink, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { avatarPicker.launch("image/*") }.padding(vertical = 2.dp))
                }
            }

            Text("INFORMAÇÕES DO PERFIL", color = Muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

            @Composable
            fun editCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, hint: String, onValue: (String) -> Unit, singleLine: Boolean = true, max: Int = 80) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(Color(0xAA17162B), Color(0xAA0A1325), Color(0xAA171126))))
                        .border(BorderStroke(1.dp, Brush.linearGradient(listOf(Pink.copy(alpha = 0.72f), Violet.copy(alpha = 0.62f)))), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = if (singleLine) 10.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = Pink, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(label, color = Muted, style = MaterialTheme.typography.labelMedium)
                        androidx.compose.foundation.text.BasicTextField(
                            value = value,
                            onValueChange = { onValue(it.take(max)) },
                            enabled = !loading,
                            singleLine = singleLine,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                            cursorBrush = Brush.linearGradient(listOf(Pink, Violet)),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                Box {
                                    if (value.isEmpty()) Text(hint, color = Muted.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                                    inner()
                                }
                            }
                        )
                        if (!singleLine) Text("\${value.length}/300", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End))
                    }
                    if (singleLine) Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
                }
            }

            editCard(Icons.Default.Person, "Nome", name, "Seu nome", { name = it }, max = 80)
            editCard(Icons.Default.AlternateEmail, "Nome de usuário", username, "seuusuario", { username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' || ch == '.' }.take(24) }, max = 24)
            editCard(Icons.Default.Description, "Biografia", bio, "Escreva uma breve descrição sobre você", { bio = it }, singleLine = false, max = 300)
            if (error.isNotBlank()) Text(error, color = Pink, style = MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable
private fun SettingsPage(prefs: android.content.SharedPreferences, username: String, onBack: () -> Unit, onNotice: (String) -> Unit, onEditProfile: () -> Unit, onLogout: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var section by remember { mutableStateOf("home") }
    var search by remember { mutableStateOf("") }
    var notifications by remember { mutableStateOf(prefs.getBoolean("setting_notifications", true)) }
    var privateAccount by remember { mutableStateOf(prefs.getBoolean("setting_private_account", false)) }
    var autoplay by remember { mutableStateOf(prefs.getBoolean("setting_autoplay", true)) }
    var activityStatus by remember { mutableStateOf(prefs.getBoolean("setting_activity_status", false)) }
    var allowMessages by remember { mutableStateOf(prefs.getBoolean("setting_messages", true)) }
    var allowComments by remember { mutableStateOf(prefs.getBoolean("setting_comments", true)) }
    var suggestProfile by remember { mutableStateOf(prefs.getBoolean("setting_suggest_profile", true)) }
    var dataSaver by remember { mutableStateOf(prefs.getBoolean("setting_data_saver", false)) }
    var likesNotice by remember { mutableStateOf(prefs.getBoolean("setting_notice_likes", true)) }
    var commentsNotice by remember { mutableStateOf(prefs.getBoolean("setting_notice_comments", true)) }
    var followersNotice by remember { mutableStateOf(prefs.getBoolean("setting_notice_followers", true)) }
    var messagesNotice by remember { mutableStateOf(prefs.getBoolean("setting_notice_messages", true)) }
    var livesNotice by remember { mutableStateOf(prefs.getBoolean("setting_notice_lives", true)) }

    val title = when (section) {
        "account" -> "Conta e perfil"
        "privacy" -> "Privacidade e segurança"
        "notifications" -> "Notificações"
        "appearance" -> "Aparência e reprodução"
        "content" -> "Conteúdo e interações"
        "lives" -> "Lives e transmissão"
        "storage" -> "Armazenamento e dados"
        "help" -> "Ajuda e suporte"
        else -> "Configurações"
    }
    Column(Modifier.fillMaxSize().background(Night).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (section == "home") onBack() else section = "home" }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text(if (section == "home") "Gerencie sua conta e sua experiência" else "Personalize sua experiência no VYBORA", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (section == "home") {
            Surface(color = Color(0xFF101426).copy(alpha = 0.8f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))) {
                Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Pink)
                    Spacer(Modifier.width(10.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = search, onValueChange = { search = it }, singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                        cursorBrush = Brush.horizontalGradient(listOf(Pink, Violet)), modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner -> Box { if (search.isBlank()) Text("Buscar configurações...", color = Muted); inner() } }
                    )
                }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (section == "home") {
                item { Text("CONFIGURAÇÕES", color = Pink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                val categories = listOf(
                    Triple("account", "Conta e perfil", "Editar perfil, trocar conta, login e segurança"),
                    Triple("privacy", "Privacidade e segurança", "Conta privada, bloqueios e controle de interações"),
                    Triple("notifications", "Notificações", "Curtidas, comentários, mensagens e lives"),
                    Triple("content", "Conteúdo e interações", "Mensagens, comentários, sugestões e filtros"),
                    Triple("lives", "Lives e transmissão", "Configurações de live e provedores"),
                    Triple("storage", "Armazenamento e dados", "Downloads, cache e informações da conta"),
                    Triple("appearance", "Aparência e reprodução", "Tema, idioma, reprodução automática e dados"),
                    Triple("help", "Ajuda e suporte", "Suporte, termos, privacidade e versão")
                ).filter { it.second.contains(search, true) || it.third.contains(search, true) }
                categories.forEach { category ->
                    item {
                        val icon = when (category.first) {
                            "account" -> Icons.Default.Person
                            "privacy" -> Icons.Default.Lock
                            "notifications" -> Icons.Default.Notifications
                            "appearance" -> Icons.Default.DarkMode
                            "content" -> Icons.Default.ChatBubbleOutline
                            "lives" -> Icons.Default.Radio
                            "storage" -> Icons.Default.FileDownload
                            else -> Icons.Default.HelpOutline
                        }
                        SettingsLink(category.second, category.third, icon) { section = category.first }
                    }
                }
                if (categories.isEmpty()) item { Text("Nenhuma configuração encontrada.", color = Muted, modifier = Modifier.padding(16.dp)) }
            } else when (section) {
                "account" -> {
                    item { Text(if (username.isBlank()) "Nenhuma conta conectada" else "Conta atual: @$username", color = Muted, modifier = Modifier.padding(vertical = 6.dp)) }
                    item { SettingsLink("Editar perfil", "Nome, nome de usuário, foto e biografia", Icons.Default.Person, onEditProfile) }
                    item { SettingsLink("Trocar conta", "Entrar com outra conta ou adicionar uma", Icons.Default.Logout) { if (username.isBlank()) onNotice("Você ainda não está conectado.") else onLogout() } }
                    item { SettingsLink("Segurança e permissões", "Senha, verificação e acesso", Icons.Default.Lock) { onNotice("As opções de segurança serão habilitadas quando os fluxos correspondentes da API estiverem disponíveis.") } }
                    item { SettingsLink("Seus pedidos", "Compras e assinaturas", Icons.Default.FileDownload) { onNotice("O módulo de pedidos ainda não está conectado.") } }
                    item { SettingsLink("Compartilhar perfil", "Convide amigos para conhecer seu perfil", Icons.Default.Share) {
                        try {
                            val share = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Conheça meu perfil no VYBORA: https://vybora.com/@${username}") }
                            context.startActivity(Intent.createChooser(share, "Compartilhar perfil"))
                        } catch (_: Exception) { onNotice("Não foi possível abrir o compartilhamento neste aparelho.") }
                    } }
                    item { SettingsLink("Dispositivos conectados", "Gerencie os dispositivos com acesso", Icons.Default.Lock) { onNotice("A gestão de sessões ainda precisa ser conectada à API.") } }
                    item { SettingsLink("Baixar seus dados", "Solicite uma cópia das informações da conta", Icons.Default.FileDownload) { onNotice("A exportação autenticada ainda precisa ser concluída no aplicativo.") } }
                    item { SettingsLink("Desativar ou excluir conta", "Gerencie sua conta permanentemente", Icons.Default.DeleteForever) { onNotice("A exclusão exige um fluxo seguro de confirmação no servidor e não é executada nesta tela.") } }
                }
                "privacy" -> {
                    item { Text("VISIBILIDADE DA CONTA", color = Pink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                    item { SettingSwitch("Conta privada", "Preferência local; a API precisa aplicar as regras de visibilidade.", privateAccount, Icons.Default.Lock) { privateAccount = it; prefs.edit().putBoolean("setting_private_account", it).apply(); onNotice("Preferência salva neste aparelho. A visibilidade real depende do backend.") } }
                    item { SettingSwitch("Mostrar status de atividade", "Controle sua preferência de presença.", activityStatus, Icons.Default.Visibility) { activityStatus = it; prefs.edit().putBoolean("setting_activity_status", it).apply() } }
                    item { Text("INTERAÇÕES", color = Pink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                    item { SettingSwitch("Permitir mensagens", "Escolha quem pode iniciar conversas.", allowMessages, Icons.Default.ChatBubbleOutline) { allowMessages = it; prefs.edit().putBoolean("setting_messages", it).apply(); onNotice("Preferência salva localmente; a API ainda precisa aplicar essa regra.") } }
                    item { SettingSwitch("Permitir comentários", "Controle quem pode comentar nas publicações.", allowComments, Icons.Default.ChatBubbleOutline) { allowComments = it; prefs.edit().putBoolean("setting_comments", it).apply(); onNotice("Preferência salva localmente; a API ainda precisa aplicar essa regra.") } }
                    item { SettingSwitch("Sugerir meu perfil", "Controle se seu perfil pode ser sugerido.", suggestProfile, Icons.Default.Person) { suggestProfile = it; prefs.edit().putBoolean("setting_suggest_profile", it).apply() } }
                    item { SettingsLink("Contas bloqueadas", "Gerencie quem não pode interagir com você", Icons.Default.Block) { onNotice("A lista de bloqueio será exibida quando o recurso estiver conectado ao backend.") } }
                }
                "notifications" -> {
                    item { SettingSwitch("Notificações", "Ativar ou desativar notificações neste aparelho", notifications, Icons.Default.Notifications) { notifications = it; prefs.edit().putBoolean("setting_notifications", it).apply() } }
                    item { SettingSwitch("Curtidas", "Quando alguém curtir seu conteúdo", likesNotice, Icons.Default.FavoriteBorder) { likesNotice = it; prefs.edit().putBoolean("setting_notice_likes", it).apply() } }
                    item { SettingSwitch("Comentários", "Quando alguém comentar", commentsNotice, Icons.Default.ChatBubbleOutline) { commentsNotice = it; prefs.edit().putBoolean("setting_notice_comments", it).apply() } }
                    item { SettingSwitch("Novos seguidores", "Quando alguém começar a seguir você", followersNotice, Icons.Default.Person) { followersNotice = it; prefs.edit().putBoolean("setting_notice_followers", it).apply() } }
                    item { SettingSwitch("Mensagens", "Quando você receber mensagens", messagesNotice, Icons.Default.ChatBubbleOutline) { messagesNotice = it; prefs.edit().putBoolean("setting_notice_messages", it).apply() } }
                    item { SettingSwitch("Lives", "Quando alguém iniciar uma live", livesNotice, Icons.Default.Radio) { livesNotice = it; prefs.edit().putBoolean("setting_notice_lives", it).apply() } }
                    item { Text("As preferências são locais; notificações push dependem da configuração do servidor.", color = Muted, style = MaterialTheme.typography.labelSmall) }
                }
                "appearance" -> {
                    item { SettingsLink("Tema escuro", "O VYBORA usa tema escuro nesta versão", Icons.Default.DarkMode) { onNotice("O tema escuro está ativo. Alternância de tema ainda não foi implementada.") } }
                    item { SettingsLink("Idioma", "Português (Brasil)", Icons.Default.Language) { onNotice("O seletor de idiomas ainda não está implementado.") } }
                    item { SettingsLink("Qualidade de vídeo", "Automática", Icons.Default.VideoLibrary) { onNotice("A seleção de qualidade será conectada ao reprodutor.") } }
                    item { SettingSwitch("Reprodução automática", "Reproduzir vídeos automaticamente", autoplay, Icons.Default.PlayCircle) { autoplay = it; prefs.edit().putBoolean("setting_autoplay", it).apply() } }
                    item { SettingSwitch("Economia de dados", "Reduzir o uso de dados móveis", dataSaver, Icons.Default.FileDownload) { dataSaver = it; prefs.edit().putBoolean("setting_data_saver", it).apply() } }
                }
                "content" -> {
                    item { SettingsLink("Comentários", "Quem pode comentar nos seus vídeos", Icons.Default.ChatBubbleOutline) { onNotice("As regras de comentários ainda dependem do backend.") } }
                    item { SettingsLink("Menções", "Quem pode mencionar você", Icons.Default.AlternateEmail) { onNotice("O controle de menções será conectado às regras de interação.") } }
                    item { SettingsLink("Mensagens diretas", "Quem pode enviar mensagens", Icons.Default.ChatBubbleOutline) { onNotice("O controle de mensagens depende da validação da API.") } }
                    item { SettingsLink("Reutilização de conteúdo", "Permitir que outros reutilizem seus vídeos", Icons.Default.VideoLibrary) { onNotice("As permissões de reutilização ainda precisam ser aplicadas pelo backend.") } }
                    item { SettingsLink("Exibir perfil ao compartilhar links", "Preferência de visibilidade de links", Icons.Default.Share) { onNotice("Essa preferência ainda não está conectada ao backend.") } }
                    item { SettingsLink("Lista de usuários seguidos", "Controlar quem pode ver a lista", Icons.Default.Person) { onNotice("A visibilidade da lista precisa ser aplicada pela API.") } }
                    item { SettingsLink("Vídeos curtidos", "Controlar quem pode ver seus vídeos curtidos", Icons.Default.FavoriteBorder) { onNotice("A visibilidade de curtidas precisa ser aplicada pela API.") } }
                    item { SettingsLink("Espectadores", "Preferência de visualização de espectadores", Icons.Default.Visibility) { onNotice("O controle de espectadores será conectado quando o recurso estiver disponível.") } }
                }
                "lives" -> {
                    item { SettingsLink("Integrações de live", "Conecte serviços e provedores de transmissão", Icons.Default.Radio) { onNotice("As transmissões reais ainda exigem configurar e testar o servidor de mídia.") } }
                    item { SettingsLink("Qualidade da transmissão", "Ajustes de qualidade e conexão", Icons.Default.VideoLibrary) { onNotice("A qualidade será configurável quando o provedor estiver integrado.") } }
                    item { SettingsLink("Interações na live", "Comentários, presentes e moderação", Icons.Default.ChatBubbleOutline) { onNotice("As interações serão habilitadas junto com o backend de streaming.") } }
                }
                "storage" -> {
                    item { SettingsLink("Downloads", "Gerencie downloads e arquivos salvos", Icons.Default.FileDownload) { onNotice("O gerenciador de downloads ainda será conectado.") } }
                    item { SettingsLink("Limpar cache", "Liberar espaço usado por conteúdo temporário", Icons.Default.DeleteForever) { onNotice("O cache não foi apagado; a limpeza segura ainda precisa ser implementada.") } }
                    item { SettingsLink("Baixar seus dados", "Solicite uma cópia das informações da conta", Icons.Default.FileDownload) { onNotice("A exportação autenticada ainda precisa ser concluída no aplicativo.") } }
                }
                else -> {
                    item { SettingsLink("Central de ajuda", "Dúvidas e tutoriais", Icons.Default.HelpOutline) { onNotice("A central de ajuda ainda será conectada.") } }
                    item { SettingsLink("Reportar um problema", "Envie um relato sobre o aplicativo", Icons.Default.Info) { onNotice("O envio de relatórios ainda será conectado ao suporte.") } }
                    item { SettingsLink("Sobre o VYBORA", "Versão 0.3.0 · Termos e privacidade", Icons.Default.Info) { onNotice("VYBORA 0.3.0. Os links de termos e privacidade precisam ser publicados antes do lançamento.") } }
                    item { SettingsLink("Atualizações do aplicativo", "Verificar e baixar o APK mais recente", Icons.Default.FileDownload) { context.startActivity(Intent(context, RemoteUiActivity::class.java)) } }
                }
            }
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
