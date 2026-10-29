package app.vybora.android

import android.os.Bundle
import org.json.JSONArray
import org.json.JSONObject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
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
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
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
private val Violet = Color(0xFFFFFFFF)
private val Pink = Color(0xFFFFFFFF)
private val Muted = Color(0xFFADB2CB)


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VyboraApp() }
    }
}

private fun readImageAspectRatio(context: android.content.Context, source: Uri?): Float {
    if (source == null) return 16f / 10f
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
        if (options.outWidth > 0 && options.outHeight > 0) options.outWidth.toFloat() / options.outHeight.toFloat() else 16f / 10f
    } catch (_: Exception) { 16f / 10f }
}

private fun createAdjustedImage(context: android.content.Context, source: Uri, ratio: String, filter: String, zoom: Float = 1f, panX: Float = 0f, panY: Float = 0f, rotationDegrees: Float = 0f): Uri {
    val decoded = context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it) }
        ?: throw IllegalArgumentException("Não foi possível abrir a imagem selecionada.")
    val rotated = if (rotationDegrees % 360f != 0f) {
        val rotationMatrix = android.graphics.Matrix().apply { postRotate(rotationDegrees) }
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, rotationMatrix, true)
            .also { if (it !== decoded) decoded.recycle() }
    } else decoded

    val targetRatio = when (ratio) {
        "1:1" -> 1f
        "4:5" -> 4f / 5f
        "16:9" -> 16f / 9f
        "9:16" -> 9f / 16f
        "16:10" -> 16f / 10f
        else -> rotated.width.toFloat() / rotated.height.toFloat()
    }
    val outputWidth = 1600
    val outputHeight = (outputWidth / targetRatio).toInt().coerceAtLeast(1)
    val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawColor(android.graphics.Color.rgb(5, 8, 20))

    // Foto do perfil em tela cheia: preencher a proporção ampla com recorte, igual à prévia.
    // Não amplia além do necessário para preencher; só preserva o reposicionamento por arraste.
    val safeZoom = 1f
    val baseScale = maxOf(outputWidth.toFloat() / rotated.width, outputHeight.toFloat() / rotated.height)
    val scale = baseScale * safeZoom
    val drawWidth = rotated.width * scale
    val drawHeight = rotated.height * scale
    val maxPanX = maxOf(0f, (drawWidth - outputWidth) / (2f * outputWidth))
    val maxPanY = maxOf(0f, (drawHeight - outputHeight) / (2f * outputHeight))
    val safePanX = panX.coerceIn(-maxPanX, maxPanX)
    val safePanY = panY.coerceIn(-maxPanY, maxPanY)
    val left = (outputWidth - drawWidth) / 2f + safePanX * outputWidth
    val top = (outputHeight - drawHeight) / 2f + safePanY * outputHeight

    val matrix = ColorMatrix()
    when (filter) {
        "P&B", "Preto e branco" -> matrix.setSaturation(0f)
        "Vivo" -> matrix.setSaturation(1.45f)
        "Neon" -> matrix.setSaturation(1.7f)
        "Quente" -> matrix.set(floatArrayOf(1.08f,0f,0f,0f,12f, 0f,1f,0f,0f,2f, 0f,0f,0.9f,0f,0f, 0f,0f,0f,1f,0f))
        "Frio" -> matrix.set(floatArrayOf(0.92f,0f,0f,0f,0f, 0f,1f,0f,0f,0f, 0f,0f,1.12f,0f,10f, 0f,0f,0f,1f,0f))
        "Cinema" -> matrix.set(floatArrayOf(1.08f,0f,0f,0f,-8f, 0f,0.98f,0f,0f,0f,0f,0f,0.86f,0f,4f, 0f,0f,0f,1f,0f))
        "Retrô" -> { matrix.setSaturation(0.78f); matrix.postConcat(ColorMatrix(floatArrayOf(1f,0f,0f,0f,10f, 0f,0.92f,0f,0f,4f, 0f,0f,0.78f,0f,0f, 0f,0f,0f,1f,0f))) }
        "Brilho" -> matrix.set(floatArrayOf(1.12f,0f,0f,0f,18f, 0f,1.12f,0f,0f,18f, 0f,0f,1.12f,0f,18f, 0f,0f,0f,1f,0f))
        "Contraste" -> matrix.set(floatArrayOf(1.25f,0f,0f,0f,-22f, 0f,1.25f,0f,0f,-22f, 0f,0f,1.25f,0f,-22f, 0f,0f,0f,1f,0f))
        "Saturação" -> matrix.setSaturation(1.65f)
        "Fade" -> matrix.set(floatArrayOf(0.92f,0f,0f,0f,18f, 0f,0.92f,0f,0f,18f, 0f,0f,0.92f,0f,18f, 0f,0f,0f,1f,0f))
        "Glow" -> matrix.set(floatArrayOf(1.18f,0f,0f,0f,14f, 0f,1.08f,0f,0f,8f, 0f,0f,1.2f,0f,16f, 0f,0f,0f,1f,0f))
        "VHS" -> matrix.set(floatArrayOf(1.05f,0f,0f,0f,0f, 0f,0.92f,0f,0f,0f,0f, 0f,0f,1.08f,0f,0f, 0f,0f,0f,1f,0f))
        "Granulado" -> matrix.setSaturation(0.9f)
        "Vinheta" -> matrix.set(floatArrayOf(0.82f,0f,0f,0f,0f, 0f,0.82f,0f,0f,0f, 0f,0f,0.82f,0f,0f, 0f,0f,0f,1f,0f))
        "Desfoque" -> matrix.setSaturation(0.75f)
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(matrix)
    }
    canvas.drawBitmap(rotated, null, android.graphics.RectF(left, top, left + drawWidth, top + drawHeight), paint)
    if (!rotated.isRecycled) rotated.recycle()

    val file = File(context.cacheDir, "vybora-adjusted-${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { output.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    output.recycle()
    return Uri.fromFile(file)
}

private fun createEditorOverlayImage(context: android.content.Context, source: Uri, overlayText: String, sticker: String, strokes: List<List<Offset>> = emptyList()): Uri {
    if (overlayText.isBlank() && sticker.isBlank() && strokes.isEmpty()) return source
    val bitmap = context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it) }
        ?: throw IllegalArgumentException("Não foi possível abrir a imagem para aplicar as edições.")
    val canvas = Canvas(bitmap)
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        textSize = (bitmap.width * 0.065f).coerceIn(28f, 96f)
        setShadowLayer(8f, 2f, 3f, android.graphics.Color.BLACK)
    }
    if (sticker.isNotBlank()) {
        textPaint.textSize = (bitmap.width * 0.12f).coerceIn(44f, 150f)
        canvas.drawText(sticker, bitmap.width / 2f, bitmap.height * 0.40f, textPaint)
    }
    if (strokes.isNotEmpty()) {
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(255, 42, 157)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = (bitmap.width * 0.009f).coerceIn(8f, 24f)
            setShadowLayer(3f, 1f, 1f, android.graphics.Color.BLACK)
        }
        strokes.forEach { points ->
            if (points.size > 1) {
                val path = android.graphics.Path()
                path.moveTo(points.first().x * bitmap.width, points.first().y * bitmap.height)
                points.drop(1).forEach { path.lineTo(it.x * bitmap.width, it.y * bitmap.height) }
                canvas.drawPath(path, strokePaint)
            }
        }
    }
    if (overlayText.isNotBlank()) {
        textPaint.textSize = (bitmap.width * 0.055f).coerceIn(24f, 72f)
        val maxWidth = bitmap.width * 0.86f
        val words = overlayText.trim().split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var line = ""
        for (word in words) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (textPaint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
                lines.add(line)
                line = word
            } else line = candidate
        }
        if (line.isNotEmpty()) lines.add(line)
        val lineHeight = textPaint.textSize * 1.25f
        val startY = bitmap.height * 0.55f - (lines.size - 1) * lineHeight / 2f
        lines.forEachIndexed { index, value -> canvas.drawText(value, bitmap.width / 2f, startY + index * lineHeight, textPaint) }
    }
    val file = File(context.cacheDir, "vybora-editor-${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 94, it) }
    bitmap.recycle()
    return Uri.fromFile(file)
}

private data class SavedVyboraAccount(val username: String, val token: String, val displayName: String = "", val bio: String = "", val refreshToken: String = "")
private fun savedVyboraAccounts(prefs: android.content.SharedPreferences): List<SavedVyboraAccount> = try {
 val a=JSONArray(prefs.getString("vybora_accounts","[]") ?: "[]")
 (0 until a.length()).mapNotNull { i -> val o=a.optJSONObject(i) ?: return@mapNotNull null; val u=o.optString("username"); val t=o.optString("token"); if(u.isBlank()||t.isBlank()) null else SavedVyboraAccount(u,t,o.optString("displayName"),o.optString("bio"),o.optString("refreshToken")) }.distinctBy { it.username.lowercase() }
} catch (_:Exception) { emptyList() }
private fun rememberVyboraAccount(prefs: android.content.SharedPreferences, username:String, token:String, displayName:String="", bio:String="", refreshToken:String=(prefs.getString("refresh_token","") ?: "")) {
 if(username.isBlank()||token.isBlank()) return
 val list=savedVyboraAccounts(prefs).filterNot { it.username.equals(username,true) }.toMutableList()
 list.add(0,SavedVyboraAccount(username,token,displayName,bio,refreshToken))
 val a=JSONArray()
 list.take(8).forEach { a.put(JSONObject().put("username",it.username).put("token",it.token).put("displayName",it.displayName).put("bio",it.bio).put("refreshToken",it.refreshToken)) }
 prefs.edit().putString("vybora_accounts",a.toString()).apply()
}
@Composable
private fun AccountSwitcherDialog(
 accounts: List<SavedVyboraAccount>,
 currentUsername: String,
 onDismiss: () -> Unit,
 onSelect: (SavedVyboraAccount) -> Unit,
 onAddAccount: () -> Unit,
 onRemove: (String) -> Unit
) {
 var selectedUsername by remember { mutableStateOf("") }
 var confirmRemove by remember { mutableStateOf("") }
 androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
  Surface(Modifier.fillMaxWidth(), color = Panel, shape = RoundedCornerShape(26.dp), border = BorderStroke(1.dp, Violet.copy(alpha = .55f))) {
   Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Trocar de conta", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text("Escolha uma conta para continuar", color = Muted)
    if (accounts.isEmpty()) Text("Nenhuma conta salva neste aparelho ainda.", color = Muted, style = MaterialTheme.typography.bodySmall)
    accounts.forEach { account ->
     val active = account.username.equals(currentUsername, true)
     val selected = account.username.equals(selectedUsername, true)
     Surface(
      Modifier.fillMaxWidth().clickable {
       selectedUsername = account.username
       if (!active) onSelect(account)
      },
      color = if (active) Violet.copy(alpha = .16f) else Color(0xFF0A0D1A),
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.dp, if (selected) Pink.copy(alpha = .9f) else if (active) Pink.copy(alpha = .65f) else Color.White.copy(alpha = .08f))
     ) {
      Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
       Box(Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Violet, Pink))), contentAlignment = Alignment.Center) {
        Text(account.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
       }
       Column(Modifier.weight(1f).padding(start = 12.dp)) {
        Text(account.displayName.ifBlank { account.username }, color = Color.White, fontWeight = FontWeight.SemiBold)
        Text("@" + account.username.removePrefix("@"), color = Muted, style = MaterialTheme.typography.bodySmall)
       }
       if (active) Icon(Icons.Default.CheckCircle, contentDescription = "Conta atual", tint = Pink)
       else Icon(Icons.Default.ChevronRight, contentDescription = "Selecionar", tint = Muted)
       if (selected) {
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = { confirmRemove = account.username }, modifier = Modifier.size(32.dp)) {
         Icon(Icons.Default.Close, contentDescription = "Remover conta", tint = Color.White)
        }
       }
      }
     }
    }
    OutlinedButton(onClick = onAddAccount, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
     Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar outra conta")
    }
    OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("Cancelar") }
   }
  }
 }
 if (confirmRemove.isNotBlank()) {
  androidx.compose.material3.AlertDialog(
   onDismissRequest = { confirmRemove = "" },
   title = { Text("Remover conta?") },
   text = { Text("Tem certeza de que deseja remover esta conta da lista neste aparelho?") },
   confirmButton = {
    TextButton(onClick = {
     val target = confirmRemove
     confirmRemove = ""
     onRemove(target)
     selectedUsername = ""
    }) { Text("Remover", color = Pink) }
   },
   dismissButton = { TextButton(onClick = { confirmRemove = "" }) { Text("Cancelar") } },
   containerColor = Panel,
   titleContentColor = Color.White,
   textContentColor = Muted
  )
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
    var showAccountSwitcher by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var publicProfileUsername by remember { mutableStateOf("") }
    var messageRecipient by remember { mutableStateOf("") }
    val appContext = androidx.compose.ui.platform.LocalContext.current
    VyboraApi.initialize(appContext)
    val appPrefs = remember { appContext.getSharedPreferences("vybora_session", android.content.Context.MODE_PRIVATE) }
    var loggedInUser by remember { mutableStateOf(appPrefs.getString("username", "") ?: "") }
    var unreadMessages by remember { mutableIntStateOf(0) }
    LaunchedEffect(appPrefs.getString("token", null)) {
        val token = appPrefs.getString("token", null)
        if (token.isNullOrBlank()) {
            unreadMessages = 0
        } else {
            while (true) {
                try {
                    val inbox = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        VyboraApi.getInbox(token)
                    }
                    unreadMessages = inbox.sumOf { it.unreadCount }.coerceAtLeast(0)
                } catch (_: Exception) {
                    // Mantém o último contador válido se a rede estiver indisponível.
                }
                kotlinx.coroutines.delay(15_000)
            }
        }
    }
    var showSettings by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showEditProfilePhoto by remember { mutableStateOf(false) }
    var profilePhotoRefreshKey by remember { mutableIntStateOf(0) }
    var showNotifications by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf(appPrefs.getString("display_name", "") ?: "") }
    var profileBio by remember { mutableStateOf(appPrefs.getString("bio", "") ?: "") }
    LaunchedEffect(Unit) {
        val token = appPrefs.getString("token", null)
        if (!token.isNullOrBlank() && loggedInUser.isNotBlank()) rememberVyboraAccount(appPrefs, loggedInUser, token, profileName, profileBio)
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
                        rememberVyboraAccount(appPrefs, username, token, name, bio)
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
    BackHandler {
        when {
            showAccountSwitcher -> showAccountSwitcher = false
            showSearchDialog -> showSearchDialog = false
            profileStat.isNotBlank() -> profileStat = ""
            showEditProfilePhoto -> showEditProfilePhoto = false
            showEditProfile -> showEditProfile = false
            showSettings -> showSettings = false
            showNotifications -> showNotifications = false
            publicProfileUsername.isNotBlank() -> publicProfileUsername = ""
            showAuth -> showAuth = false
            tab != 0 -> {
                tab = 0
                messageRecipient = ""
            }
            else -> (appContext as? android.app.Activity)?.finish()
        }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = Violet, secondary = Pink, background = Night, surface = Panel, onBackground = Color.White, onSurface = Color.White)) {
        Scaffold(containerColor = Night, bottomBar = {
            if (tab != 2) {
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
                    containerColor = Color(0x99030710),
                    contentColor = Color.White,
                    tonalElevation = 0.dp
                ) {
                    val navColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = Color(0x33FFFFFF),
                        unselectedIconColor = Color.White,
                        unselectedTextColor = Color.White
                    )
                    NavigationBarItem(selected = tab == 0 && !showNotifications, onClick = { showSettings = false; showEditProfile = false; showNotifications = false; publicProfileUsername = ""; messageRecipient = ""; showAuth = false; tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Início") }, colors = navColors)
                    NavigationBarItem(selected = tab == 3 && !showNotifications, onClick = { showSettings = false; showEditProfile = false; showNotifications = false; publicProfileUsername = ""; showAuth = false; tab = 3 }, icon = { Icon(Icons.Default.Radio, null) }, label = { Text("Lives") }, colors = navColors)
                    NavigationBarItem(selected = false, onClick = { showSettings = false; showEditProfile = false; showNotifications = false; publicProfileUsername = ""; showAuth = false; tab = 2 }, icon = {
                        Box(
                            Modifier.size(width = 30.dp, height = 30.dp)
                                .border(1.5.dp, Color.White, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Criar publicação", modifier = Modifier.size(22.dp))
                        }
                    }, label = { Text("Criar") }, colors = navColors)
                    NavigationBarItem(
                        selected = tab == 1 && !showNotifications,
                        onClick = { showSettings = false; showEditProfile = false; showNotifications = false; publicProfileUsername = ""; showAuth = false; tab = 1 },
                        icon = {
                            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Mensagens")
                                if (unreadMessages > 0) {
                                    Box(
                                        Modifier.align(Alignment.TopEnd)
                                            .offset(x = 7.dp, y = (-5).dp)
                                            .sizeIn(minWidth = 18.dp, minHeight = 18.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF3045))
                                            .padding(horizontal = 4.dp, vertical = 1.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (unreadMessages > 99) "99+" else unreadMessages.toString(),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        },
                        label = { Text("Mensagens") },
                        colors = navColors
                    )
                    NavigationBarItem(selected = tab == 4 && !showNotifications, onClick = { showSettings = false; showEditProfile = false; showNotifications = false; publicProfileUsername = ""; showAuth = false; tab = 4 }, icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") }, label = { Text("Perfil") }, colors = navColors)
                }
            }
            }
        }) { pad ->
            Column(
                Modifier.fillMaxSize()
                    .padding(pad)
                    .pointerInput(tab, showSettings, showEditProfile, showEditProfilePhoto, showNotifications, showAuth, publicProfileUsername) {
                        var horizontalDistance = 0f
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                if (!showSettings && !showEditProfile && !showEditProfilePhoto &&
                                    !showAuth && publicProfileUsername.isBlank() && tab != 2
                                ) {
                                    horizontalDistance += dragAmount
                                }
                            },
                            onDragEnd = {
                                val threshold = 72.dp.toPx()
                                if (!showSettings && !showEditProfile && !showEditProfilePhoto &&
                                    !showAuth && publicProfileUsername.isBlank()
                                ) {
                                    if (horizontalDistance <= -threshold) {
                                        // Ordem do gesto: Início -> Mensagens -> Lives -> Avisos -> Perfil.
                                        // O botão "+" abre a câmera/publicação e não entra na navegação por gesto.
                                        if (showNotifications) {
                                            showNotifications = false
                                            tab = 4
                                        } else when (tab) {
                                            0 -> tab = 1
                                            1 -> tab = 3
                                            3 -> showNotifications = true
                                            4 -> { /* já está no último destino */ }
                                            else -> tab = 0
                                        }
                                    } else if (horizontalDistance >= threshold) {
                                        if (showNotifications) {
                                            showNotifications = false
                                            tab = 3
                                        } else when (tab) {
                                            0 -> { /* já está no primeiro destino */ }
                                            1 -> tab = 0
                                            3 -> tab = 1
                                            4 -> showNotifications = true
                                            else -> tab = 0
                                        }
                                    }
                                }
                                horizontalDistance = 0f
                            },
                            onDragCancel = { horizontalDistance = 0f }
                        )
                    }
            ) {
                if (publicProfileUsername.isNotBlank() || tab == 2) {
                    // Cabeçalho de marca removido para liberar espaço acima do perfil.
                } else if (!showSettings && !showEditProfile && !showEditProfilePhoto && !showNotifications && !showAuth) when (tab) {
                    0 -> Unit
                    1 -> Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Mensagens", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
                        onAuth = { showSettings = false; showAuth = true; tab = 4 },
                        onSwitchAccounts = { showAccountSwitcher = true },
                        onLogout = {
                            // Encerrar apenas a sessão atual; manter as outras contas salvas para uso futuro.
                            appPrefs.edit()
                                .remove("token")
                                .remove("refresh_token")
                                .remove("username")
                                .remove("display_name")
                                .remove("bio")
                                .apply()
                            loggedInUser = ""
                            profileName = ""
                            profileBio = ""
                            showAccountSwitcher = false
                            showAuth = false
                            showSettings = false
                            showEditProfile = false
                            showEditProfilePhoto = false
                            publicProfileUsername = ""
                            messageRecipient = ""
                            tab = 4
                            notice = "Você saiu da sua conta."
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
                } else if (showEditProfilePhoto) {
                    EditProfilePhotoPage(
                        onBack = { showEditProfilePhoto = false },
                        onSaved = {
                            showEditProfilePhoto = false
                            profilePhotoRefreshKey++
                            notice = "Foto do perfil atualizada."
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
                            val previousUsername = loggedInUser
                            val currentToken = appPrefs.getString("token", "") ?: ""
                            val currentRefreshToken = appPrefs.getString("refresh_token", "") ?: ""
                            // Atualizar a conta existente pelo identificador atual; não manter o @ antigo como outra conta.
                            val remainingAccounts = savedVyboraAccounts(appPrefs).filterNot {
                                it.username.equals(previousUsername, true) || it.username.equals(username, true)
                            }
                            val accountsJson = JSONArray()
                            remainingAccounts.forEach { a ->
                                accountsJson.put(JSONObject().put("username", a.username).put("token", a.token)
                                    .put("displayName", a.displayName).put("bio", a.bio).put("refreshToken", a.refreshToken))
                            }
                            appPrefs.edit().putString("vybora_accounts", accountsJson.toString())
                                .putString("username", username).putString("display_name", name).putString("bio", bio).apply()
                            if (currentToken.isNotBlank()) rememberVyboraAccount(appPrefs, username, currentToken, name, bio, currentRefreshToken)
                            loggedInUser = username
                            profileName = name
                            profileBio = bio
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
                    0 -> FeedPage(currentUsername = loggedInUser, onNotice = { notice = it }, onOpenProfile = { publicProfileUsername = it }, onAddStory = { tab = 2 }, onSearch = { showSearchDialog = true }, onNotifications = { showNotifications = true })
                    1 -> MessagesPage(recipientUsername = messageRecipient, onSearch = { showSearchDialog = true }, onOpenConversation = { messageRecipient = it }, onOpenProfile = { publicProfileUsername = it }, onNotice = { notice = it })
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
                    } else ProfilePage(username = loggedInUser, displayName = profileName, bio = profileBio, photoRefreshKey = profilePhotoRefreshKey, onNotice = { notice = it }, onStat = { stat ->
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
                    }, onAuth = { showAuth = true }, onEdit = { showEditProfile = true; showSettings = false; showEditProfilePhoto = false; publicProfileUsername = "" }, onEditProfilePhoto = { showEditProfilePhoto = true; showEditProfile = false; showSettings = false; publicProfileUsername = "" }, onSettings = { showSettings = true })
                }
            }
        }
        if (showAccountSwitcher) {
            AccountSwitcherDialog(savedVyboraAccounts(appPrefs), loggedInUser, { showAccountSwitcher = false }, { account ->
                appPrefs.edit().putString("token",account.token).putString("username",account.username).putString("display_name",account.displayName).putString("bio",account.bio).putString("refresh_token",account.refreshToken).apply()
                loggedInUser=account.username; profileName=account.displayName; profileBio=account.bio
                showAuth=false; showEditProfile=false; publicProfileUsername=""; tab=4
                notice="Conta alterada para @"+account.username+"."
                Thread {
                    try {
                        val response=VyboraApi.getMyProfile(account.token); val user=response.optJSONObject("user")
                        if(user!=null) {
                            val n=user.optString("displayName",user.optString("display_name")); val b=user.optString("bio"); val u=user.optString("username",account.username)
                            appPrefs.edit().putString("username",u).putString("display_name",n).putString("bio",b).apply()
                            rememberVyboraAccount(appPrefs,u,account.token,n,b)
                            android.os.Handler(android.os.Looper.getMainLooper()).post { loggedInUser=u; profileName=n; profileBio=b }
                        }
                    } catch (_:Exception) { android.os.Handler(android.os.Looper.getMainLooper()).post { notice="Conta selecionada; não foi possível atualizar o perfil pela rede agora." } }
                }.start()
            }, {
                showAccountSwitcher=false; showAuth=true; showSettings=false; tab=4
            }, { usernameToRemove ->
                val remaining = savedVyboraAccounts(appPrefs).filterNot { it.username.equals(usernameToRemove, true) }
                val saved = JSONArray()
                remaining.forEach { account ->
                    saved.put(JSONObject().put("username", account.username).put("token", account.token)
                        .put("displayName", account.displayName).put("bio", account.bio).put("refreshToken", account.refreshToken))
                }
                val removingCurrent = loggedInUser.equals(usernameToRemove, true)
                val edit = appPrefs.edit().putString("vybora_accounts", saved.toString())
                if (removingCurrent) {
                    edit.remove("token").remove("refresh_token").remove("username").remove("display_name").remove("bio")
                    loggedInUser = ""; profileName = ""; profileBio = ""
                    showAccountSwitcher = false; showAuth = true
                    notice = "Conta removida deste aparelho. Entre novamente para continuar."
                } else {
                    notice = "Conta removida da lista deste aparelho."
                }
                edit.apply()
            })
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
                val demoNotifications = listOf(
                    VyboraApi.NotificationSummary("demo-notif-01", "like", "demo-post-06", "Agora", "", "carol.mendes.demo", "Carol Mendes", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=160"),
                    VyboraApi.NotificationSummary("demo-notif-02", "comment", "demo-post-07", "Agora", "", "julia.ribeiro.demo", "Júlia Ribeiro", "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=160"),
                    VyboraApi.NotificationSummary("demo-notif-03", "follow", "", "Agora", "", "marina.costa.demo", "Marina Costa", "https://images.unsplash.com/photo-1531123897727-8f129e1688ce?w=160"),
                    VyboraApi.NotificationSummary("demo-notif-04", "system", "", "Hoje", "", "qinbly", "QINBLY", "")
                )
                notifications = (loaded + demoNotifications).distinctBy { it.id }
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
            "Do QINBLY" -> item.kind == "system"
            else -> true
        }
    }
    Column(Modifier.fillMaxSize().background(Night).padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White) }
            Text("Notificações", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = Pink)
                val unreadVisibleNotifications = notifications.count { it.readAt.isBlank() && it.kind != "message" }
                if (unreadVisibleNotifications > 0) {
                    Box(
                        Modifier.align(Alignment.TopEnd).sizeIn(minWidth = 17.dp, minHeight = 17.dp)
                            .clip(CircleShape).background(Color(0xFFFF3045)).padding(horizontal = 3.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (unreadVisibleNotifications > 99) "99+" else unreadVisibleNotifications.toString(),
                            color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todas", "Atividades", "Do QINBLY").forEach { label ->                Surface(                    modifier = Modifier.clickable { selectedFilter = label },                    color = if (selectedFilter == label) Color(0xFF35113B) else Panel,
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
                    val actor = item.actorDisplayName.ifBlank { item.actorUsername.ifBlank { "QINBLY" } }
                    val description = when (item.kind) {
                        "follow" -> "começou a seguir você"
                        "like" -> "curtiu sua publicação"
                        "comment" -> "comentou na sua publicação"
                        "message" -> "enviou uma mensagem"
                        "system" -> "aviso do QINBLY"
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
                        if (item.readAt.isBlank()) Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFFF3045)))
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
    var searchError by remember { mutableStateOf("") }

    // Só consulta quando há pelo menos dois caracteres e descarta resultados de buscas antigas.
    var searchRequest by remember { mutableIntStateOf(0) }
    LaunchedEffect(query) {
        val requestId = ++searchRequest
        kotlinx.coroutines.delay(250)
        val normalized = query.trim().removePrefix("@").trim()
        searchError = ""
        if (normalized.length < 2) {
            results = emptyList()
            loading = false
            searched = false
        } else {
            loading = true
            try {
                val found = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    VyboraApi.searchUsers(normalized, prefs.getString("token", null))
                }
                if (requestId == searchRequest) {
                    results = found
                    searched = true
                }
            } catch (e: Exception) {
                if (requestId == searchRequest) {
                    searchError = e.message ?: "Não foi possível pesquisar usuários."
                    results = emptyList()
                    searched = true
                }
            } finally {
                if (requestId == searchRequest) loading = false
            }
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Night, shape = RoundedCornerShape(0.dp)) {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                    Text(
                        when (searchContext) {
                            "messages" -> "Encontrar pessoas"
                            "lives" -> "Buscar usuários e lives"
                            "home" -> "Buscar pessoas e publicações"
                            else -> "Buscar pessoas"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(40) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                    singleLine = true,
                    placeholder = { Text("Nome ou @usuário") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Muted) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar busca", tint = Muted)
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                Text(
                    if (query.isBlank()) "Sugestões de pessoas" else "Resultados da busca",
                    color = Muted,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                when {
                    loading && results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Pink)
                    }
                    searchError.isNotBlank() && results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(searchError, color = Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Button(onClick = { query = query + " " }) { Text("Tentar novamente") }
                        }
                    }
                    query.trim().removePrefix("@").trim().length < 2 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Digite pelo menos 2 letras para encontrar usuários.", color = Muted)
                    }
                    searched && results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nenhum usuário encontrado para essa pesquisa.", color = Muted)
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(results, key = { it.id }) { user ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                    .clickable { onOpenProfile(user.username) }
                                    .padding(horizontal = 4.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    Modifier.size(52.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Pink, Violet))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (user.avatarUrl.isNotBlank()) {
                                        AsyncImage(model = user.avatarUrl, contentDescription = "Foto de perfil", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                    } else {
                                        Text(user.displayName.ifBlank { user.username }.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(user.displayName.ifBlank { user.username }, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text("@${user.username}", color = Muted, style = MaterialTheme.typography.bodySmall)
                                    if (user.bio.isNotBlank()) Text(user.bio, color = Muted, maxLines = 1, style = MaterialTheme.typography.labelSmall)
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
                                }) { Text("Seguir", color = Pink) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineVideoPlayer(url: String, modifier: Modifier = Modifier, autoplay: Boolean = false) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var playbackError by remember(url) { mutableStateOf(false) }
    var isPlaying by remember(url) { mutableStateOf(autoplay) }
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            playWhenReady = autoplay
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    playbackError = true
                    isPlaying = false
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == androidx.media3.common.Player.STATE_READY) playbackError = false
                }
            })
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        if (playbackError) {
            AsyncImage(
                model = "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&h=700&q=85",
                contentDescription = "Prévia do vídeo",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Box(Modifier.size(62.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.62f))
                .clickable {
                    playbackError = false
                    player.prepare()
                    player.play()
                    isPlaying = true
                }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Tentar reproduzir vídeo", tint = Color.White, modifier = Modifier.size(38.dp))
            }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext -> PlayerView(viewContext).apply {
                    this.player = player
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                } },
                update = { view ->
                    view.player = player
                    view.useController = false
                }
            )
            if (!isPlaying) {
                Box(
                    modifier = Modifier.size(62.dp).clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { player.play(); isPlaying = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Reproduzir vídeo", tint = Color.White, modifier = Modifier.size(38.dp))
                }
            } else {
                Box(Modifier.fillMaxSize().clickable { player.pause(); isPlaying = false })
            }
        }
    }
}

@Composable
private fun FeedPage(currentUsername: String, onNotice: (String) -> Unit, onOpenProfile: (String) -> Unit, onAddStory: () -> Unit, onSearch: () -> Unit, onNotifications: () -> Unit) {
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
    var savedPostIds by remember { mutableStateOf(prefs.getStringSet("saved_post_ids", emptySet())?.toSet() ?: emptySet()) }
    var unreadNotifications by remember { mutableIntStateOf(0) }
    var selectedFeedTab by remember { mutableStateOf("Para você") }
    LaunchedEffect(prefs.getString("token", null)) {
        val token = prefs.getString("token", null)
        if (token.isNullOrBlank()) {
            unreadNotifications = 0
        } else {
            while (true) {
                try {
                    val loaded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { VyboraApi.getNotifications(token) }
                    unreadNotifications = loaded.count { it.readAt.isBlank() && it.kind != "message" }
                } catch (_: Exception) {
                    // Mantém o último contador confirmado se a rede estiver indisponível.
                }
                kotlinx.coroutines.delay(15_000)
            }
        }
    }
    // Perfis/publicações demonstrativas locais para testar a interface sem tocar no banco.
    var demoLikes by remember { mutableStateOf(prefs.getStringSet("demo_liked_post_ids", emptySet())?.toSet() ?: emptySet()) }
    var demoComments by remember { mutableStateOf<Map<String, List<VyboraApi.CommentSummary>>>(emptyMap()) }
    val demoPosts = remember {
        listOf(
            VyboraApi.PostSummary("demo-post-01", "Confiança é o melhor look. Qual foto merece ir para os favoritos? 🖤✨ #QINBLY", "https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?w=900", "image", "ana.lima.demo", "Ana Lima", 128, 14, "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=160"),
            VyboraApi.PostSummary("demo-post-02", "Noite elegante, luz baixa e playlist perfeita 🥂", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900", "image", "marcos.rocha.demo", "Marcos Rocha", 76, 8, "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=160"),
            VyboraApi.PostSummary("demo-post-03", "Um pouco de sol, mar e atitude. 🌊🔥", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=900", "image", "bia.santos.demo", "Beatriz Santos", 203, 27, "https://images.unsplash.com/photo-1531123897727-8f129e1688ce?w=160"),
            VyboraApi.PostSummary("demo-post-04", "Treino concluído. Um passo de cada vez 💪", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=900", "image", "lucas.fit.demo", "Lucas Ferreira", 54, 6, "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=160"),
            VyboraApi.PostSummary("demo-post-05", "Look preto, batom marcante e zero pressa. 🖤", "https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=900", "image", "nina.alves.demo", "Nina Alves", 91, 11, "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=160"),
            VyboraApi.PostSummary("demo-post-06", "Biquíni, sol e mar: dia perfeito de verão ☀️👙🌊", "https://images.unsplash.com/photo-1679620455972-72e53db09d18?auto=format&fit=crop&fm=jpg&q=85&w=1000", "image", "carol.mendes.demo", "Carol Mendes", 347, 42, "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=160&auto=format&fit=crop&q=80"),
            VyboraApi.PostSummary("demo-post-07", "Curtindo a praia com meu biquíni favorito 🏖️👙", "https://images.unsplash.com/photo-1726842569517-56877c34aaac?auto=format&fit=crop&fm=jpg&q=85&w=1000", "image", "julia.ribeiro.demo", "Júlia Ribeiro", 286, 31, "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=160&auto=format&fit=crop&q=80"),
            VyboraApi.PostSummary("demo-post-08", "Dia de piscina, biquíni preto e boas energias 🖤☀️", "https://images.unsplash.com/photo-1698447695797-df112ffb87fc?auto=format&fit=crop&fm=jpg&q=85&w=1000", "image", "marina.costa.demo", "Marina Costa", 419, 56, "https://images.unsplash.com/photo-1531123897727-8f129e1688ce?w=160&auto=format&fit=crop&q=80"),
            VyboraApi.PostSummary("demo-post-09", "Um sorriso, um olhar e uma noite para lembrar ✨", "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=1000&auto=format&fit=crop&q=85", "image", "leticia.alves.demo", "Letícia Alves", 192, 23, "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=160&auto=format&fit=crop&q=80")
        )
    }
    // Perfis e posts extras de demonstração, mantidos somente em memória no aparelho.
    // 20 publicações locais com URLs de imagens distintas, selecionadas de fotos reais de stock.
    val extraDemoPosts = remember {
        listOf(
            VyboraApi.PostSummary("demo-post-10", "Lingerie preta e atitude 🖤", "https://images.pexels.com/photos/13636983/pexels-photo-13636983.jpeg?auto=compress&cs=tinysrgb&w=1000", "image", "ana.clara.demo", "Ana Clara", 320, 24, "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=160"),
            VyboraApi.PostSummary("demo-post-11", "Elegância em renda preta ✨", "https://images.pexels.com/photos/11254431/pexels-photo-11254431.jpeg?auto=compress&cs=tinysrgb&w=1000", "image", "bruna.martins.demo", "Bruna Martins", 337, 26, "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=160"),
            VyboraApi.PostSummary("demo-post-12", "Tons neutros e pose editorial 🤎", "https://images.pexels.com/photos/16585363/pexels-photo-16585363.jpeg?auto=compress&cs=tinysrgb&w=1000", "image", "giovana.reis.demo", "Giovana Reis", 354, 28, "https://images.unsplash.com/photo-1531123897727-8f129e1688ce?w=160"),
            VyboraApi.PostSummary("demo-post-13", "Estilo ousado em preto e branco 🖤", "https://images.pexels.com/photos/34225851/pexels-photo-34225851.jpeg?auto=compress&cs=tinysrgb&w=1000", "image", "leticia.souza.demo", "Letícia Souza", 371, 30, "https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?w=160"),
            VyboraApi.PostSummary("demo-post-14", "Um toque vermelho ❤️", "https://images.pexels.com/photos/28179353/pexels-photo-28179353.jpeg?auto=compress&cs=tinysrgb&w=1000", "image", "amanda.reis.demo", "Amanda Reis", 388, 32, "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?w=160"),
            VyboraApi.PostSummary("demo-post-15", "Lingerie preta sob a luz do sol ☀️", "https://images.pexels.com/photos/24701207/pexels-photo-24701207.jpeg?auto=compress&cs=tinysrgb&w=1000", "image", "vitoria.lima.demo", "Vitória Lima", 405, 34, "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=160"),
            VyboraApi.PostSummary("demo-post-16", "Preto, luz e sombra 🖤", "https://images.pexels.com/photos/4456681/pexels-photo-4456681.jpeg?auto=compress&cs=tinysrgb&w=1000", "image", "marina.alves.demo", "Marina Alves", 422, 36, "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=160"),
            VyboraApi.PostSummary("demo-post-17", "Renda preta e olhar marcante ✨", "https://images.pexels.com/photos/11345542/pexels-photo-11345542.jpeg?auto=compress&cs=tinysrgb&w=1000", "image", "carol.santos.demo", "Carol Santos", 439, 38, "https://images.unsplash.com/photo-1531123897727-8f129e1688ce?w=160"),
            VyboraApi.PostSummary("demo-post-18", "Editorial em tons terrosos 🤎", "https://images.pexels.com/photos/16585365/pexels-photo-16585365.jpeg?auto=compress&cs=tinysrgb&w=1000", "image", "beatriz.costa.demo", "Beatriz Costa", 456, 40, "https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?w=160"),
            VyboraApi.PostSummary("demo-post-19", "Conjunto clássico e elegante 🖤", "https://images.pexels.com/photos/11810949/pexels-photo-11810949.jpeg?auto=compress&cs=tinysrgb&w=1000", "image", "isadora.lima.demo", "Isadora Lima", 473, 42, "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?w=160"),
            VyboraApi.PostSummary("demo-post-20", "Visual sofisticado em preto 💙", "https://images.pexels.com/photos/9836062/pexels-photo-9836062.jpeg?auto=compress&cs=tinysrgb&w=1000", "image", "julia.mendes.demo", "Julia Mendes", 490, 44, "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=160"),
            VyboraApi.PostSummary("demo-post-21", "Minimalismo preto e branco 🤍", "https://images.pexels.com/photos/14037286/pexels-photo-14037286.jpeg?auto=compress&cs=tinysrgb&w=1000", "image", "leticia.rocha.demo", "Letícia Rocha", 507, 46, "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=160"),
            VyboraApi.PostSummary("demo-post-22", "Detalhes escuros e atitude 🖤", "https://images.pexels.com/photos/16585371/pexels-photo-16585371.jpeg?auto=compress&cs=tinysrgb&w=1000", "image", "fernanda.ribeiro.demo", "Fernanda Ribeiro", 524, 48, "https://images.unsplash.com/photo-1531123897727-8f129e1688ce?w=160"),
            VyboraApi.PostSummary("demo-post-23", "Renda preta em estilo editorial ✨", "https://images.pexels.com/photos/21367288/pexels-photo-21367288.jpeg?auto=compress&cs=tinysrgb&w=1000", "image", "alice.barros.demo", "Alice Barros", 541, 50, "https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?w=160"),
            VyboraApi.PostSummary("demo-post-24", "Retrato monocromático 🖤", "https://images.pexels.com/photos/8953405/pexels-photo-8953405.jpeg?auto=compress&cs=tinysrgb&w=1000", "image", "heloisa.freire.demo", "Heloísa Freire", 558, 52, "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?w=160"),
            VyboraApi.PostSummary("demo-post-25", "Luz e sombra em preto e branco", "https://images.pexels.com/photos/14982381/pexels-photo-14982381.jpeg?auto=compress&cs=tinysrgb&w=1000", "image", "clarissa.nunes.demo", "Clarissa Nunes", 575, 54, "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=160"),
            VyboraApi.PostSummary("demo-post-26", "Corset de renda e detalhes dourados ✨", "https://images.pexels.com/photos/16070984/pexels-photo-16070984.jpeg?auto=compress&cs=tinysrgb&w=1000", "image", "raquel.pires.demo", "Raquel Pires", 592, 56, "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=160"),
            VyboraApi.PostSummary("demo-post-27", "Renda delicada em tons suaves 🌫️", "https://images.pexels.com/photos/7737960/pexels-photo-7737960.jpeg?auto=compress&cs=tinysrgb&w=1000", "image", "sabrina.lopes.demo", "Sabrina Lopes", 609, 58, "https://images.unsplash.com/photo-1531123897727-8f129e1688ce?w=160"),
            VyboraApi.PostSummary("demo-post-28", "Pose editorial em luz suave 🤍", "https://images.pexels.com/photos/7738073/pexels-photo-7738073.jpeg?auto=compress&cs=tinysrgb&w=1000", "image", "laura.martins.demo", "Laura Martins", 626, 60, "https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?w=160"),
            VyboraApi.PostSummary("demo-post-29", "Lingerie preta com luz natural ☀️", "https://images.pexels.com/photos/26761372/pexels-photo-26761372.jpeg?auto=compress&cs=tinysrgb&w=1000", "image", "gabriela.moraes.demo", "Gabriela Moraes", 643, 62, "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?w=160")
        )
    }
    val displayedPosts = (demoPosts + extraDemoPosts + posts).distinctBy { it.id }
    LaunchedEffect(refreshKey) {
        loading = true
        Thread {
            try {
                val loaded = VyboraApi.getPosts(prefs.getString("token", null))
                android.os.Handler(android.os.Looper.getMainLooper()).post { posts = loaded; loading = false }
            } catch (e: Exception) {
                val cached = VyboraApi.getCachedPosts(context)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (cached.isNotEmpty()) posts = cached
                    loading = false
                    onNotice(if (cached.isNotEmpty()) "Sem conexão: mostrando o último feed sincronizado." else (e.message ?: "Não foi possível carregar o feed."))
                }
            }
        }.start()
    }
    Column(Modifier.fillMaxSize().padding(0.dp)) {
        // Barra superior agora sobrepõe a foto da primeira publicação.
        // Feed original preservado; somente os fundos foram tornados translúcidos.
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (!loading && displayedPosts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ainda não há publicações", fontWeight = FontWeight.SemiBold)
                    Text("Quando alguém publicar, as postagens públicas aparecerão aqui.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            val feedListState = androidx.compose.foundation.lazy.rememberLazyListState()
            LaunchedEffect(refreshKey, displayedPosts.size) { feedListState.scrollToItem(0) }
            LazyColumn(state = feedListState, verticalArrangement = Arrangement.spacedBy(0.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                if (!loading && displayedPosts.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 56.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Ainda não há publicações", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Quando as pessoas publicarem, elas aparecerão aqui. As ações do feed funcionarão nas publicações reais.", color = Muted, fontSize = 14.sp)
                            OutlinedButton(onClick = { refreshKey++ }) {
                                Text("Atualizar feed")
                            }
                        }
                    }
                }
                items(displayedPosts, key = { it.id }) { post ->
                    var postMenuExpanded by remember(post.id) { mutableStateOf(false) }
                    var likedByCurrentUser by remember(post.id, demoLikes, post.likedByMe) {
                        mutableStateOf(if (post.id.startsWith("demo-post-")) post.id in demoLikes else post.likedByMe)
                    }
                    Surface(color = Color.Transparent, shape = RoundedCornerShape(0.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            val followingPrefKey = "following_${post.username.lowercase()}"
                            var isFollowingPost by remember(post.id, post.isFollowing) {
                                mutableStateOf(if (post.id.startsWith("demo-post-")) prefs.getBoolean(followingPrefKey, false) else post.isFollowing)
                            }

                            if (post.mediaUrl.isNotBlank() && post.mediaType == "image") {
                                val demoGallery = remember(post.id) {
                                    when (post.id) {
                                        "demo-post-06" -> listOf(
                                            "https://images.unsplash.com/photo-1502680390469-be75c86b636f?w=1000&auto=format&fit=crop&q=85",
                                            "https://images.unsplash.com/photo-1519046904884-53103b34b206?w=1000&auto=format&fit=crop&q=85",
                                            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1000&auto=format&fit=crop&q=85",
                                            "https://images.unsplash.com/photo-1473116763249-2faaef81ccda?w=1000&auto=format&fit=crop&q=85"
                                        )
                                        "demo-post-07" -> listOf(
                                            "https://images.unsplash.com/photo-1529139574466-a303027c1d8b?w=1000&auto=format&fit=crop&q=85",
                                            "https://images.unsplash.com/photo-1515886657613-9f3515f0d78f?w=1000&auto=format&fit=crop&q=85",
                                            "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1000&auto=format&fit=crop&q=85",
                                            "https://images.unsplash.com/photo-1485230895905-ec40ba36b9bc?w=1000&auto=format&fit=crop&q=85"
                                        )
                                        "demo-post-08" -> listOf(
                                            "https://images.unsplash.com/photo-1519046904884-53103b34b206?w=1000&auto=format&fit=crop&q=85",
                                            "https://images.unsplash.com/photo-1500375592092-40eb2168fd21?w=1000&auto=format&fit=crop&q=85",
                                            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1000&auto=format&fit=crop&q=85",
                                            "https://images.unsplash.com/photo-1473116763249-2faaef81ccda?w=1000&auto=format&fit=crop&q=85"
                                        )
                                        else -> listOf(post.mediaUrl)
                                    }
                                }
                                var photoIndex by remember(post.id) { mutableIntStateOf(0) }
                                val shownPhoto = demoGallery[photoIndex.coerceIn(0, demoGallery.lastIndex)]
                                var dragDistance by remember(post.id) { mutableFloatStateOf(0f) }
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clipToBounds()
                                        .background(Color.Black)
                                ) {
                                    AsyncImage(
                                        model = shownPhoto, contentDescription = "Foto ${photoIndex + 1} de ${demoGallery.size}",
                                        modifier = Modifier.fillMaxSize()
                                            .pointerInput(post.id, photoIndex, demoGallery.size) {
                                                detectHorizontalDragGestures(
                                                    onHorizontalDrag = { change, amount ->
                                                        dragDistance += amount
                                                        change.consume()
                                                    },
                                                    onDragEnd = {
                                                        if (dragDistance < -40f) photoIndex = (photoIndex + 1).coerceAtMost(demoGallery.lastIndex)
                                                        else if (dragDistance > 40f) photoIndex = (photoIndex - 1).coerceAtLeast(0)
                                                        dragDistance = 0f
                                                    },
                                                    onDragCancel = { dragDistance = 0f }
                                                )
                                            }
                                            .clickable { expandedMedia = shownPhoto },
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                    Column(
                                        modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                                            .background(Brush.verticalGradient(listOf(Color(0x77030711), Color(0x33030711), Color.Transparent)))
                                            .padding(top = 4.dp, bottom = 8.dp)
                                    ) {
                                        if (post.id == displayedPosts.firstOrNull()?.id) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape)
                        .background(Color(0x553A3546)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSearch, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = onNotifications, modifier = Modifier.size(42.dp)) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notificações", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    if (unreadNotifications > 0) {
                        Box(
                            Modifier.align(Alignment.TopEnd).offset(x = (-1).dp, y = 1.dp)
                                .sizeIn(minWidth = 18.dp, minHeight = 18.dp)
                                .clip(CircleShape).background(Color(0xFFFF3045))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (unreadNotifications > 99) "99+" else unreadNotifications.toString(),
                                color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
                                        }
                                        if (post.id == displayedPosts.firstOrNull()?.id) {
                                            Row(
                                                Modifier.fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 2.dp)
                                                    .background(Color.Transparent)
                                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                listOf("Para você", "Amigos", "Em alta").forEach { tab ->
                                                    val selected = selectedFeedTab == tab
                                                    Column(
                                                        Modifier.weight(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.Transparent)
                                                            .clickable { selectedFeedTab = tab }
                                                            .padding(vertical = 8.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(tab, color = Color.White, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                                                        Spacer(Modifier.height(3.dp))
                                                        Box(
                                                            Modifier.width(if (selected) 42.dp else 0.dp).height(2.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(0xFFB56CFF))
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(Color.Transparent).padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    val avatarUrl = post.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?w=240&auto=format&fit=crop&q=80" }
                                    AsyncImage(model = avatarUrl, contentDescription = "Abrir perfil de @${post.username}", modifier = Modifier.size(42.dp).clip(CircleShape).clickable { if (post.username.isNotBlank()) onOpenProfile(post.username) }, contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f).clickable { if (post.username.isNotBlank()) onOpenProfile(post.username) }) {
                                        Text(post.displayName.ifBlank { post.username }.ifBlank { "QINBLY" }, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("publicação recente", color = Muted, fontSize = 12.sp)
                                    }
                                    val currentUsername = prefs.getString("username", "").orEmpty()
                                    if (post.username.isNotBlank() && !post.username.equals(currentUsername, ignoreCase = true)) {
                                        OutlinedButton(
                                            onClick = {
                                                val token = prefs.getString("token", null)
                                                if (token.isNullOrBlank()) onNotice("Entre na sua conta para seguir pessoas.")
                                                else Thread {
                                                    try {
                                                        val profile = VyboraApi.getPublicProfile(post.username)
                                                        val user = profile.optJSONObject("user") ?: profile
                                                        val userId = user.optString("id")
                                                        if (userId.isBlank()) throw IllegalStateException("Não foi possível localizar este perfil.")
                                                        val following = VyboraApi.toggleFollow(token, userId)
                                                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                            isFollowingPost = following
                                                            prefs.edit().putBoolean(followingPrefKey, following).apply()
                                                            onNotice(if (following) "Você está seguindo ${post.displayName.ifBlank { post.username }}." else "Você deixou de seguir ${post.displayName.ifBlank { post.username }}.")
                                                        }
                                                    } catch (e: Exception) {
                                                        android.os.Handler(android.os.Looper.getMainLooper()).post { onNotice(e.message ?: "Não foi possível seguir este perfil.") }
                                                    }
                                                }.start()
                                            },
                                            modifier = Modifier.height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(18.dp),
                                            border = BorderStroke(1.dp, Color.White),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (isFollowingPost) Color.White else Color.Transparent,
                                                contentColor = if (isFollowingPost) Color.Black else Color.White
                                            )
                                        ) {
                                            Text(if (isFollowingPost) "Seguindo" else "Seguir", fontSize = 12.sp, color = if (isFollowingPost) Color.Black else Color.White)
                                        }
                                        Spacer(Modifier.width(3.dp))
                                    }
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
                                        if (post.caption.isNotBlank()) Text(post.caption, color = Color.White, fontSize = 16.sp, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().background(Color.Transparent).padding(horizontal = 14.dp, vertical = 10.dp), lineHeight = 23.sp)

                                    }
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC030711))))
                                    .heightIn(min = 58.dp)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f).clickable {
                                        if (post.id.startsWith("demo-post-")) {
                                            val alreadyLiked = post.id in demoLikes
                                            demoLikes = if (alreadyLiked) demoLikes - post.id else demoLikes + post.id
                                            prefs.edit().putStringSet("demo_liked_post_ids", demoLikes.toSet()).apply()
                                            likedByCurrentUser = !alreadyLiked
                                        } else {
                                            val token = prefs.getString("token", null)
                                            if (token.isNullOrBlank()) onNotice("Entre na sua conta para curtir publicações.")
                                            else Thread {
                                                try {
                                                    val nowLiked = VyboraApi.toggleLike(token, post.id)
                                                    val refreshed = VyboraApi.getPosts()
                                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                        likedByCurrentUser = nowLiked
                                                        posts = refreshed
                                                    }
                                                } catch (e: Exception) {
                                                    android.os.Handler(android.os.Looper.getMainLooper()).post { onNotice(e.message ?: "Não foi possível curtir.") }
                                                }
                                            }.start()
                                        }
                                    },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Icon(
                                        if (likedByCurrentUser) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = if (likedByCurrentUser) "Descurtir" else "Curtir",
                                        tint = if (likedByCurrentUser) Color(0xFFFF3045) else Color.White,
                                        modifier = Modifier.size(25.dp)
                                    )
                                    Text(
                                        if (post.likes >= 1000) String.format(java.util.Locale.US, "%.1fK", post.likes / 1000.0) else post.likes.toString(),
                                        color = Muted,
                                        fontSize = 12.sp
                                    )
                                }
                                Row(
                                    modifier = Modifier.weight(1f).clickable {
                                        commentPost = post
                                        commentDraft = ""
                                        commentsLoading = true
                                        Thread {
                                            try {
                                                val loadedComments = VyboraApi.getComments(post.id)
                                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                    comments = loadedComments
                                                    commentsLoading = false
                                                }
                                            } catch (e: Exception) {
                                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                    commentsLoading = false
                                                    onNotice(e.message ?: "Não foi possível carregar comentários.")
                                                }
                                            }
                                        }.start()
                                    },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comentários", tint = Color.White, modifier = Modifier.size(24.dp))
                                    Text(
                                        if (post.comments >= 1000) String.format(java.util.Locale.US, "%.1fK", post.comments / 1000.0) else post.comments.toString(),
                                        color = Muted,
                                        fontSize = 12.sp
                                    )
                                }
                                Row(
                                    modifier = Modifier.weight(1f).clickable {
                                        val postLink = "https://vybora.com/post/${Uri.encode(post.id)}"
                                        val shareText = buildString {
                                            append("Confira esta publicação no QINBLY")
                                            if (post.caption.isNotBlank()) append(": ${post.caption}")
                                            append("\\n")
                                            append(postLink)
                                        }
                                        val mediaUrl = post.mediaUrl
                                        if (mediaUrl.isBlank()) {
                                            try {
                                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_SUBJECT, "Publicação no QINBLY")
                                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                                }
                                                context.startActivity(Intent.createChooser(sendIntent, "Compartilhar publicação"))
                                            } catch (_: Exception) {
                                                onNotice("Não foi possível abrir as opções de compartilhamento neste aparelho.")
                                            }
                                        } else {
                                            onNotice("Preparando a mídia para compartilhar…")
                                            Thread {
                                                try {
                                                    val connection = java.net.URL(mediaUrl).openConnection().apply {
                                                        connectTimeout = 15000
                                                        readTimeout = 30000
                                                    }
                                                    connection.getInputStream().use { input ->
                                                        val shareDir = File(context.cacheDir, "shared_media").apply { mkdirs() }
                                                        val extension = if (post.mediaType.equals("video", true)) ".mp4" else ".jpg"
                                                        val file = File(shareDir, "vybora_${post.id.hashCode().toUInt().toString(16)}$extension")
                                                        file.outputStream().use { output -> input.copyTo(output) }
                                                        val contentUri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
                                                        val mime = if (post.mediaType.equals("video", true)) "video/*" else "image/*"
                                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                            type = mime
                                                            putExtra(Intent.EXTRA_SUBJECT, "Publicação no QINBLY")
                                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                                            putExtra(Intent.EXTRA_STREAM, contentUri)
                                                            clipData = android.content.ClipData.newUri(context.contentResolver, "Mídia da publicação QINBLY", contentUri)
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                            try {
                                                                context.startActivity(Intent.createChooser(sendIntent, "Compartilhar publicação"))
                                                            } catch (_: Exception) {
                                                                onNotice("Não foi possível abrir o compartilhamento neste aparelho.")
                                                            }
                                                        }
                                                    }
                                                } catch (_: Exception) {
                                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                        onNotice("Não foi possível preparar a mídia para compartilhar.")
                                                    }
                                                }
                                            }.start()
                                        }
                                    },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = Color.White, modifier = Modifier.size(24.dp))
                                    Text("1.1K", color = Muted, fontSize = 12.sp)
                                }
                                IconButton(
                                    onClick = {
                                        val wasSaved = post.id in savedPostIds
                                        savedPostIds = if (wasSaved) savedPostIds - post.id else savedPostIds + post.id
                                        prefs.edit().putStringSet("saved_post_ids", savedPostIds).apply()
                                        onNotice(if (wasSaved) "Publicação removida dos salvos." else "Publicação salva.")
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        if (post.id in savedPostIds) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = if (post.id in savedPostIds) "Remover dos salvos" else "Salvar publicação",
                                        tint = if (post.id in savedPostIds) Pink else Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                                    if (demoGallery.size > 1) {
                                        Box(Modifier.align(Alignment.TopEnd).padding(10.dp).clip(CircleShape)
                                            .background(Color(0x99030711)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                                            Text("${photoIndex + 1}/${demoGallery.size}", color = Color.White, fontSize = 12.sp)
                                        }
                                        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                            demoGallery.indices.forEach { idx ->
                                                Box(Modifier.size(if (idx == photoIndex) 8.dp else 7.dp).clip(CircleShape)
                                                    .background(if (idx == photoIndex) Pink else Color(0xFF777982)))
                                            }
                                        }
                                    }
                                }
                            } else if (post.mediaUrl.isNotBlank() && post.mediaType == "video") {
                                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clipToBounds().background(Color.Black)) {
                                    InlineVideoPlayer(url = post.mediaUrl, modifier = Modifier.fillMaxSize(), autoplay = true)
                                }
                            }
                            if (post.mediaType != "image" && post.caption.isNotBlank()) {
                                Text(post.caption, color = Color.White, fontSize = 16.sp, style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.fillMaxWidth().background(Color(0x66030711)).padding(horizontal = 14.dp, vertical = 10.dp), lineHeight = 23.sp)
                            }
                            if (post.mediaType == "video") {
                                Text("♫ som original - ${post.displayName.ifBlank { post.username }}", color = Muted, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(horizontal = 10.dp))
                            }
                            if (post.mediaType != "image") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x66030711))
                                        .heightIn(min = 58.dp)
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f).clickable {
                                            if (post.id.startsWith("demo-post-")) {
                                                val alreadyLiked = post.id in demoLikes
                                                demoLikes = if (alreadyLiked) demoLikes - post.id else demoLikes + post.id
                                                prefs.edit().putStringSet("demo_liked_post_ids", demoLikes.toSet()).apply()
                                                likedByCurrentUser = !alreadyLiked
                                            } else {
                                                val token = prefs.getString("token", null)
                                                if (token.isNullOrBlank()) onNotice("Entre na sua conta para curtir publicações.")
                                                else Thread {
                                                    try {
                                                        val nowLiked = VyboraApi.toggleLike(token, post.id)
                                                        val refreshed = VyboraApi.getPosts()
                                                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                            likedByCurrentUser = nowLiked
                                                            posts = refreshed
                                                        }
                                                    } catch (e: Exception) {
                                                        android.os.Handler(android.os.Looper.getMainLooper()).post { onNotice(e.message ?: "Não foi possível curtir.") }
                                                    }
                                                }.start()
                                            }
                                        },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        Icon(
                                            if (likedByCurrentUser) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = if (likedByCurrentUser) "Descurtir" else "Curtir",
                                            tint = if (likedByCurrentUser) Color(0xFFFF3045) else Color.White,
                                            modifier = Modifier.size(25.dp)
                                        )
                                        Text(
                                            if (post.likes >= 1000) String.format(java.util.Locale.US, "%.1fK", post.likes / 1000.0) else post.likes.toString(),
                                            color = Muted,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.weight(1f).clickable {
                                            commentPost = post
                                            commentDraft = ""
                                            commentsLoading = true
                                            Thread {
                                                try {
                                                    val loadedComments = VyboraApi.getComments(post.id)
                                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                        comments = loadedComments
                                                        commentsLoading = false
                                                    }
                                                } catch (e: Exception) {
                                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                        commentsLoading = false
                                                        onNotice(e.message ?: "Não foi possível carregar comentários.")
                                                    }
                                                }
                                            }.start()
                                        },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comentários", tint = Color.White, modifier = Modifier.size(24.dp))
                                        Text(
                                            if (post.comments >= 1000) String.format(java.util.Locale.US, "%.1fK", post.comments / 1000.0) else post.comments.toString(),
                                            color = Muted,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.weight(1f).clickable {
                                            val postLink = "https://vybora.com/post/${Uri.encode(post.id)}"
                                            val shareText = buildString {
                                                append("Confira esta publicação no QINBLY")
                                                if (post.caption.isNotBlank()) append(": ${post.caption}")
                                                append("\\n")
                                                append(postLink)
                                            }
                                            val mediaUrl = post.mediaUrl
                                            if (mediaUrl.isBlank()) {
                                                try {
                                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_SUBJECT, "Publicação no QINBLY")
                                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                                    }
                                                    context.startActivity(Intent.createChooser(sendIntent, "Compartilhar publicação"))
                                                } catch (_: Exception) {
                                                    onNotice("Não foi possível abrir as opções de compartilhamento neste aparelho.")
                                                }
                                            } else {
                                                onNotice("Preparando a mídia para compartilhar…")
                                                Thread {
                                                    try {
                                                        val connection = java.net.URL(mediaUrl).openConnection().apply {
                                                            connectTimeout = 15000
                                                            readTimeout = 30000
                                                        }
                                                        connection.getInputStream().use { input ->
                                                            val shareDir = File(context.cacheDir, "shared_media").apply { mkdirs() }
                                                            val extension = if (post.mediaType.equals("video", true)) ".mp4" else ".jpg"
                                                            val file = File(shareDir, "vybora_${post.id.hashCode().toUInt().toString(16)}$extension")
                                                            file.outputStream().use { output -> input.copyTo(output) }
                                                            val contentUri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
                                                            val mime = if (post.mediaType.equals("video", true)) "video/*" else "image/*"
                                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                                type = mime
                                                                putExtra(Intent.EXTRA_SUBJECT, "Publicação no QINBLY")
                                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                                                putExtra(Intent.EXTRA_STREAM, contentUri)
                                                                clipData = android.content.ClipData.newUri(context.contentResolver, "Mídia da publicação QINBLY", contentUri)
                                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            }
                                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                                try {
                                                                    context.startActivity(Intent.createChooser(sendIntent, "Compartilhar publicação"))
                                                                } catch (_: Exception) {
                                                                    onNotice("Não foi possível abrir o compartilhamento neste aparelho.")
                                                                }
                                                            }
                                                        }
                                                    } catch (_: Exception) {
                                                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                            onNotice("Não foi possível preparar a mídia para compartilhar.")
                                                        }
                                                    }
                                                }.start()
                                            }
                                        },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = Color.White, modifier = Modifier.size(24.dp))
                                        Text("1.1K", color = Muted, fontSize = 12.sp)
                                    }
                                    IconButton(
                                        onClick = {
                                            val wasSaved = post.id in savedPostIds
                                            savedPostIds = if (wasSaved) savedPostIds - post.id else savedPostIds + post.id
                                            prefs.edit().putStringSet("saved_post_ids", savedPostIds).apply()
                                            onNotice(if (wasSaved) "Publicação removida dos salvos." else "Publicação salva.")
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            if (post.id in savedPostIds) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = if (post.id in savedPostIds) "Remover dos salvos" else "Salvar publicação",
                                            tint = if (post.id in savedPostIds) Pink else Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
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
            }
        }
    }
    val commentEmojiContext = androidx.compose.ui.platform.LocalContext.current
    val commentEmojiPrefs = remember(commentEmojiContext) {
        commentEmojiContext.getSharedPreferences("vybora_comment_emoji_history", android.content.Context.MODE_PRIVATE)
    }
    var recentCommentEmojis by remember {
        mutableStateOf(commentEmojiPrefs.getString("recent", "")!!.split("|").filter { it.isNotBlank() })
    }
    var showCommentEmojis by remember { mutableStateOf(false) }
    var selectedCommentEmojiCategory by remember { mutableStateOf("Emoções") }
    BackHandler(enabled = showCommentEmojis) {
        showCommentEmojis = false
    }
    val commentDensity = androidx.compose.ui.platform.LocalDensity.current
    val keyboardInset = with(commentDensity) { WindowInsets.ime.getBottom(this).toDp() }
    // Android can report a small IME inset even with the keyboard dismissed.
    // Use a real keyboard-height threshold so the sheet doesn't stay collapsed.
    val keyboardVisible = keyboardInset > 120.dp
    val activePost = commentPost
    if (activePost != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                if (showCommentEmojis) showCommentEmojis = false else commentPost = null
            },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    // Apply the IME inset to this Dialog window before measuring the sheet.
                    .imePadding()
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.58f))
                    .clickable { if (showCommentEmojis) showCommentEmojis = false else commentPost = null },
                contentAlignment = Alignment.BottomCenter
            ) {
                // Read IME insets inside the Dialog composition, not from the Activity window.
                // Keep Android's gesture/navigation controls in place, but remove the extra dark navigation-bar scrim behind them while the comments dialog is open.
                val dialogWindow = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
                SideEffect {
                    dialogWindow?.let { window ->
                        window.navigationBarColor = android.graphics.Color.TRANSPARENT
                        window.statusBarColor = android.graphics.Color.TRANSPARENT
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            window.isNavigationBarContrastEnforced = false
                        }
                        // Work around the Compose 1.7.x Dialog system-bar inset bug.
                        // Keep the sheet's existing dimensions and composer position unchanged.
                        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
                        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR)
                        window.setLayout(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            window.attributes = window.attributes.apply {
                                fitInsetsTypes = 0
                                fitInsetsSides = 0
                            }
                        }
                    }
                }

                val dialogDensity = androidx.compose.ui.platform.LocalDensity.current
                val dialogKeyboardInset = with(dialogDensity) { WindowInsets.ime.getBottom(this).toDp() }
                val dialogNavigationInset = with(dialogDensity) { WindowInsets.navigationBars.getBottom(this).toDp() }
                val dialogKeyboardVisible = dialogKeyboardInset > 120.dp

                // The sheet height is based on the usable height of this Dialog after IME padding.
                // Keep the sheet inside the screen and lift its lower edge above the system navigation area.
                // The previous 2x height made the sheet extend beyond the top of the display.
                val panelHeight = maxHeight * if (dialogKeyboardVisible) 0.90f else 0.90f
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(panelHeight)
                        .offset(y = (-5).dp)
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
                        Text(
                            "Comentários",
                            color = Color.White,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable(enabled = showCommentEmojis) { showCommentEmojis = false }
                        )
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
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clickable(
                                enabled = showCommentEmojis,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { showCommentEmojis = false },
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            // The Dialog may report a zero navigation inset on some Android versions.
                            // Use a small, explicit lift for the composer only; sheet/header stay fixed.
                            .padding(bottom = if (dialogKeyboardVisible) 0.dp else 48.dp)
                    ) {
                        if (showCommentEmojis) {
                            val commentEmojiGroups = listOf(
                                "Recentes" to recentCommentEmojis,
                                "Emoções" to listOf("😀","😃","😄","😁","😆","😅","🤣","😂","🙂","🙃","😉","😊","😇","🥰","😍","🤩","😘","😗","☺️","😚","😙","🥲","😋","😛","😜","🤪","😝","🤑","🤗","🤭","🫢","🫣","🤫","🤔","🫡","🤐","🤨","😐","😑","😶","🫥","😏","😒","🙄","😬","😮‍💨","🤥","😌","😔","😪","🤤","😴","😷","🤒","🤕","🤢","🤮","🤧","🥵","🥶","🥴","😵","🤯","🤠","🥳","🥸","😎","🤓","🧐","😕","🫤","😟","🙁","☹️","😮","😯","😲","😳","🥺","🥹","😦","😧","😨","😰","😥","😢","😭","😱","😖","😣","😞","😓","😩","😫","🥱","😤","😡","😠","🤬","😈","👿","💀","☠️","💩","🤡","👹","👺","👻","👽","👾","🤖"),
                                "Amor" to listOf("❤️","🩷","🧡","💛","💚","💙","🩵","💜","🤎","🖤","🩶","🤍","💔","❤️‍🔥","❤️‍🩹","💕","💞","💓","💗","💖","💘","💝","💟","❣️","💌","💋","💍","💐","🌹","🌷","🌸","💮","🪷","🌺","🌻","🌼","🌈","🦋","💎","💫","💑","👩‍❤️‍👨","👨‍❤️‍👨","👩‍❤️‍👩","💏","💐","🥀","🌹","🌺","🌻","🌼","🌷","🪻","🪷","🌱","🌿","🍀","☘️","🪴","🌵","🌴","🌳","🌲","🪺","🪷","✨","💍","🫀","💟","💝","💗","💖","💘","💞","💌","💒"),
                                "Pessoas e gestos" to listOf("👋","🤚","🖐️","✋","🖖","👌","🤌","🤏","✌️","🤞","🫰","🤟","🤘","🤙","👈","👉","👆","👇","☝️","👍","👎","✊","👊","🤛","🤜","👏","🙌","👐","🤲","🤝","🙏","✍️","💅","🤳","💪","🦾","🫶","🫂","👶","🧒","👦","👧","🧑","👨","👩","🧔","👮","🕺","💃","🧑‍🎤","🧑‍🚀","🧑‍🎨","👨‍🍳","👩‍🍳","🧑‍🚒","🧑‍⚕️","🧑‍🏫","🧑‍💻","🧑‍🔬","🧑‍🔧","🧑‍🎓","🧑‍🌾","🧑‍⚖️","🧑‍✈️","🧑‍🚀","👰","🤵","🤱","🧑‍🍼","🧏","🦻","🦵","🦶","👂","👃","🧠","🫁","🦷","🦴","👀","👁️","👅","👄","🫦","💋","🗣️","👤","👥","🫂"),
                                "Animais" to listOf("🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐻‍❄️","🐨","🐯","🦁","🐮","🐷","🐸","🐵","🙈","🙉","🙊","🐔","🐧","🐦","🐤","🦆","🦉","🦄","🐝","🦋","🐌","🐞","🐢","🐍","🦎","🐙","🦀","🐬","🐳","🦈","🐊","🦖","🐘","🦒","🦓","🦩","🦚","🦜","🦥","🦦","🦔","🐾","🐺","🐗","🐴","🫎","🫏","🦌","🦬","🦛","🦏","🦍","🦧","🐪","🐫","🦘","🦙","🦒","🦣","🦫","🦨","🦡","🦃","🦤","🦢","🦜","🦅","🪿","🦩","🦚","🦉","🪶","🪱","🪲","🪳","🦟","🪰","🪸","🪼","🦭","🦐","🦑","🦞","🦪","🐠","🐡","🪼","🐚","🪹","🐌","🐛","🪰","🦗","🕷️","🕸️","🦂","🐜","🐕","🐈","🐩","🐈‍⬛","🐇","🐿️","🦔","🦝","🦨"),
                                "Comida" to listOf("🍏","🍎","🍐","🍊","🍋","🍌","🍉","🍇","🍓","🫐","🍈","🍒","🍑","🥭","🍍","🥥","🥝","🍅","🍆","🥑","🥦","🥕","🌽","🌶️","🥒","🥬","🥔","🍠","🥐","🥨","🍞","🧀","🥚","🍳","🥞","🧇","🥓","🍔","🍟","🍕","🌭","🥪","🌮","🌯","🍜","🍣","🍤","🍰","🧁","🍩","🍪","🍫","🍿","☕","🧋","🥤","🍍","🥑","🫒","🧄","🧅","🥜","🌰","🫘","🫚","🫛","🥗","🥙","🫓","🥯","🥖","🥨","🥩","🍗","🍖","🦴","🌭","🥫","🍝","🍛","🍲","🫕","🥘","🫔","🫙","🥟","🦪","🍱","🍘","🍙","🍚","🍥","🥠","🥮","🍢","🍡","🍧","🍨","🍦","🥧","🍮","🍭","🍬","🍯","🍼","🥛","🫖","🍵","🧃","🧉","🍶","🍺","🍻","🥂","🍷","🥃","🍸","🍹","🧊"),
                                "Atividades" to listOf("⚽","🏀","🏈","⚾","🎾","🏐","🏉","🎱","🏓","🏸","🥊","🥋","⛸️","🎿","🛹","🛼","🎮","🕹️","🎲","🧩","🎯","🎳","🎤","🎧","🎼","🎹","🥁","🎷","🎸","🎻","🎬","🎨","🧵","🪡","📸","📹","🎥","📱","💻","⌚","🎁","🏆","🥇","🏅","🎖️","🎉","🎊","🎈","🎀","🪄","🏃","🚶","🧘","🏋️","🤸","🤼","🤽","🤾","🏌️","🏇","🧗","🏄","🚣","🧑‍🦽","🧑‍🦼","🧑‍🦯","🚴","🚵","🤹","🎪","🎭","🩰","🎟️","🎫","🎹","🪇","🪈","🪗","🪕","🪘","🪉","🎙️","🎚️","🎛️","📻","📺","📼","📀","💿","🧸","🪅","🪆","🪁","🪀","🛝","🎠","🎡","🎢","🎰","🪄","🧿","🪬","🧶","🪢","🧷","🪭","🪮","🛍️","🛒"),
                                "Viagens" to listOf("🚗","🚕","🚌","🚎","🏎️","🚓","🚑","🚒","🚲","🛵","✈️","🛫","🚀","🛸","🚁","⛵","🚢","⚓","🏝️","🏖️","🌋","🗻","🏕️","🏜️","🌅","🌄","🌃","🌌","🌠","🌙","☀️","⭐","🌟","⚡","❄️","☔","🌊","🌪️","🏠","🏡","🏢","🏬","🏥","🏦","🏨","🏩","🏪","🏫","🏭","🏰","🏯","🗼","🗽","⛪","🕌","🛕","🕍","⛩️","🕋","⛲","⛺","🌁","🌉","🌆","🌇","🌄","🌅","🌠","🌌","🌍","🌎","🌏","🌐","🗺️","🧭","🧳","🛣️","🛤️","🚦","🚥","🛑","🚧","⛽","🚏","🛴","🛹","🚂","🚆","🚄","🚅","🚈","🚇","🚊","🚉","🚝","🚞","🚋","🚃","🚟","🚠","🚡","🛶","🛥️","🛳️","🛟","🪝","🛩️","🪂","💺","🛰️","🛎️","🧱","🪵","🪨","🪴"),
                                "Símbolos" to listOf("❤️‍🔥","💯","✨","💫","⭐","🌟","⚡","🔥","💧","❄️","☀️","🌙","🌈","☁️","⛅","🌤️","🌦️","🌧️","⛈️","🌩️","🌨️","🌫️","🌪️","🌀","🌊","💥","💢","💨","🕳️","💤","🎵","🎶","🔔","🔕","📣","📢","🔊","🔉","🔈","🔇","♻️","✅","☑️","✔️","❌","❎","➕","➖","✖️","➗","🟰","♾️","‼️","⁉️","❓","❔","❕","❗","〰️","➰","➿","🔱","⚜️","🔰","♻️","🔆","🔅","〽️","⚠️","🚫","⛔","🔞","📵","🚭","🚯","🚱","🚷","🔃","🔄","🔙","🔚","🔛","🔜","🔝","🆗","🆒","🆕","🆓","🆙","🆖","🅰️","🅱️","🆎","🅾️","🆘","🆔","㊙️","㊗️","🔴","🟠","🟡","🟢","🔵","🟣","⚫","⚪","🟤","🔺","🔻","🔶","🔷","🔸","🔹","🔳","🔲","▪️","▫️","◾","◽","◼️","◻️","⬛","⬜","🟥","🟧","🟨","🟩","🟦","🟪","🟫","💜","👑","💸","💰","💵","💎","🔮","🪩","🪐","🖤","💟","⚡","♛","©️","®️","™️","🔒","🔓","🔑","🗝️","🛡️","⚔️","🪄","🧿","🪬","🎯","🎲","🃏","🀄","♠️","♥️","♦️","♣️","🕉️","☮️","✝️","☪️","☯️","🕎","🔯","🛐","⛎","♈","♉","♊","♋","♌","♍","♎","♏","♐","♑","♒","♓","🆚","🏳️","🏴","🏁","🚩","🎌","🏳️‍🌈","🏳️‍⚧️")
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .heightIn(max = if (dialogKeyboardVisible) 300.dp else 360.dp)
                                    .background(Color(0xFF080A12))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 8.dp)
                                ) {
                                    items(commentEmojiGroups) { group ->
                                        Surface(
                                            color = if (selectedCommentEmojiCategory == group.first) Violet.copy(alpha = 0.35f) else Color(0xFF1B1B2B),
                                            shape = RoundedCornerShape(18.dp),
                                            modifier = Modifier.clickable { selectedCommentEmojiCategory = group.first }
                                        ) {
                                            Text(
                                                group.first,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "EMOJIS E REAÇÕES",
                                    color = Muted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                )
                                val visibleCommentEmojis = commentEmojiGroups.firstOrNull { it.first == selectedCommentEmojiCategory }?.second.orEmpty()
                                if (selectedCommentEmojiCategory == "Recentes" && visibleCommentEmojis.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Seus emojis recentes aparecerão aqui.", color = Muted, fontSize = 13.sp)
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 42.dp),
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        contentPadding = PaddingValues(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        items(visibleCommentEmojis) { emoji ->
                                            Box(
                                                modifier = Modifier.fillMaxWidth().height(42.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        commentDraft = (commentDraft + emoji).take(1200)
                                                        recentCommentEmojis = (listOf(emoji) + recentCommentEmojis.filterNot { it == emoji }).take(40)
                                                        commentEmojiPrefs.edit().putString("recent", recentCommentEmojis.joinToString("|")).apply()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(emoji, fontSize = 27.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFF303041))
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(Color(0xFF191925))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                // Move only the composer row clear of Android's overlaid navigation controls.
                                .offset(y = if (dialogKeyboardVisible) 0.dp else 0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        Text(
                            "😊",
                            fontSize = 28.sp,
                            modifier = Modifier.size(48.dp).clickable { showCommentEmojis = !showCommentEmojis }
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
                                if (token.isNullOrBlank()) {
                                    onNotice("Entre na sua conta para comentar.")
                                } else {
                                    commentsLoading = true
                                    Thread {
                                        try {
                                            val loadedComments: List<VyboraApi.CommentSummary>
                                            val loadedPosts: List<VyboraApi.PostSummary>
                                            
                                                VyboraApi.addComment(token ?: "", activePost.id, draftToSend)
                                                loadedComments = VyboraApi.getComments(activePost.id)
                                                loadedPosts = VyboraApi.getPosts()
                                            
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

}

}

@Composable
private fun MessagesPage(recipientUsername: String, onSearch: () -> Unit, onOpenConversation: (String) -> Unit, onOpenProfile: (String) -> Unit, onNotice: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("vybora_session", android.content.Context.MODE_PRIVATE) }
    var draft by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<VyboraApi.MessageSummary>>(emptyList()) }
    var inbox by remember { mutableStateOf<List<VyboraApi.InboxSummary>>(emptyList()) }
    var loadingInbox by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Todas") }
    var inboxSearch by remember { mutableStateOf("") }
    var showConversationMenu by remember { mutableStateOf(false) }
    var conversationSearch by remember { mutableStateOf(false) }
    var messageSearch by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }
    var showReportConfirm by remember { mutableStateOf(false) }
    var isMuted by remember(recipientUsername) { mutableStateOf(prefs.getBoolean("muted_$recipientUsername", false)) }
    var isPinned by remember(recipientUsername) { mutableStateOf(prefs.getBoolean("pinned_$recipientUsername", false)) }
    var ownUserId by remember { mutableStateOf(prefs.getString("user_id", "") ?: "") }
    var recipientAvatarUrl by remember(recipientUsername) { mutableStateOf("") }
    val visibleMessages = if (messageSearch.isBlank()) messages else messages.filter { it.body.contains(messageSearch, ignoreCase = true) }
    val filteredInbox = inbox.filter { inboxSearch.isBlank() || it.displayName.contains(inboxSearch, ignoreCase = true) || it.username.contains(inboxSearch, ignoreCase = true) || it.body.contains(inboxSearch, ignoreCase = true) }
    val visibleInbox = when (selectedFilter) {
        "Não lidas" -> filteredInbox.filter { it.unreadCount > 0 }
        // A API atual só retorna conversas individuais; ainda não classifica grupos nem solicitações.
        "Grupos", "Solicitações" -> emptyList()
        else -> filteredInbox
    }

    LaunchedEffect(prefs.getString("token", null)) {
        val profileToken = prefs.getString("token", null)
        if (!profileToken.isNullOrBlank()) {
            try {
                val profileResponse = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { VyboraApi.getMyProfile(profileToken) }
                val currentUser = profileResponse.optJSONObject("user")
                if (currentUser != null) {
                    val id = currentUser.optString("id", currentUser.optString("userId", currentUser.optString("user_id")))
                    if (id.isNotBlank()) {
                        ownUserId = id
                        prefs.edit().putString("user_id", id).apply()
                    }
                }
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(recipientUsername) {
        recipientAvatarUrl = ""
        if (recipientUsername.isNotBlank()) {
            try {
                val publicProfile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { VyboraApi.getPublicProfile(recipientUsername) }
                val profileUser = publicProfile.optJSONObject("user") ?: publicProfile.optJSONObject("profile") ?: publicProfile
                recipientAvatarUrl = profileUser.optString("avatarUrl").ifBlank { profileUser.optString("avatar_url") }
            } catch (_: Exception) { }
        }
        messages = emptyList()
        if (recipientUsername.isBlank()) {
            val token = prefs.getString("token", null)
            if (!token.isNullOrBlank()) {
                loadingInbox = true
                var firstInboxLoad = true
                while (true) {
                    try {
                        val loadedInbox = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { VyboraApi.getInbox(token) }
                        val demoInbox = listOf(
                            VyboraApi.InboxSummary("carol.mendes.demo", "Carol Mendes", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=160", "Amei sua publicação! ❤️", "Agora", 0),
                            VyboraApi.InboxSummary("julia.ribeiro.demo", "Júlia Ribeiro", "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=160", "Vamos combinar aquele café? ☕", "Há 5 min", 0),
                            VyboraApi.InboxSummary("marina.costa.demo", "Marina Costa", "https://images.unsplash.com/photo-1531123897727-8f129e1688ce?w=160", "Que foto linda! ✨", "Há 12 min", 0),
                            VyboraApi.InboxSummary("ana.lima.demo", "Ana Lima", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=160", "Oi, tudo bem? 😊", "Ontem", 0)
                        )
                        inbox = (loadedInbox + demoInbox).distinctBy { it.username }
                    } catch (e: Exception) {
                        if (firstInboxLoad) onNotice(e.message ?: "Não foi possível carregar suas conversas.")
                    } finally {
                        loadingInbox = false
                    }
                    firstInboxLoad = false
                    kotlinx.coroutines.delay(15_000)
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
                        val demoMessages = if (recipientUsername.endsWith(".demo")) listOf(
                            VyboraApi.MessageSummary("demo-msg-01", "Oi! Vi suas fotos no QINBLY 😊", "demo-user", "Hoje"),
                            VyboraApi.MessageSummary("demo-msg-02", "Obrigada! Estou testando as mensagens por aqui ✨", ownUserId, "Hoje"),
                            VyboraApi.MessageSummary("demo-msg-03", "Ficou ótimo! Depois me manda outra publicação ❤️", "demo-user", "Agora")
                        ) else emptyList()
                        messages = if (loaded.isEmpty()) demoMessages else loaded
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
                listOf("Todas", "Não lidas", "Grupos", "Solicitações").forEach { label ->
                    val selected = selectedFilter == label
                    Surface(
                        modifier = Modifier.clickable { selectedFilter = label },
                        color = if (selected) Color(0xFF9D1BCA) else Panel,
                        shape = RoundedCornerShape(22.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Pink else Color(0xFF30334D))
                    ) {
                        Text(label, Modifier.padding(horizontal = 11.dp, vertical = 10.dp), color = if (selected) Color.White else Muted, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }
            when {
                loadingInbox -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Pink) }
                selectedFilter == "Grupos" || selectedFilter == "Solicitações" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Pink, modifier = Modifier.size(36.dp))
                        Text(if (selectedFilter == "Grupos") "Nenhum grupo disponível" else "Nenhuma solicitação", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (selectedFilter == "Grupos") "As conversas em grupo precisam ser habilitadas no serviço de mensagens." else "As solicitações aparecerão aqui quando o backend disponibilizar essa função.",
                            color = Muted, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                visibleInbox.isNotEmpty() -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(0.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                    items(visibleInbox, key = { it.username }) { conversation ->
                        Column(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { onOpenConversation(conversation.username) }.padding(horizontal = 4.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(Modifier.size(70.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Pink, Violet))).padding(2.dp), contentAlignment = Alignment.Center) {
                                if (conversation.avatarUrl.isNotBlank()) AsyncImage(model = conversation.avatarUrl, contentDescription = "Foto de perfil", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                else Box(Modifier.fillMaxSize().clip(CircleShape).background(Panel), contentAlignment = Alignment.Center) {
                                    Text(conversation.displayName.ifBlank { conversation.username }.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(conversation.displayName.ifBlank { conversation.username }, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                Text(conversation.body.ifBlank { "@${conversation.username}" }, color = if (conversation.unreadCount > 0) Color.White else Muted, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Agora", color = Muted, style = MaterialTheme.typography.labelSmall)
                                if (conversation.unreadCount > 0) Box(
                                    Modifier.sizeIn(minWidth = 20.dp, minHeight = 20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF3045))
                                        .padding(horizontal = 5.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.7.dp)
                        }
                    }
                }
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(74.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF28103F), Color(0xFF111A32)))), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Pink, modifier = Modifier.size(34.dp))
                        }
                        Text(if (selectedFilter == "Não lidas") "Tudo em dia!" else "Suas conversas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(if (selectedFilter == "Não lidas") "Você não tem mensagens não lidas." else "Pesquise pessoas e descubra contas para iniciar uma conversa.", color = Muted, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        if (selectedFilter != "Não lidas") Button(onClick = onSearch, colors = ButtonDefaults.buttonColors(containerColor = Pink), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Encontrar pessoas")
                        }
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = { onOpenProfile(recipientUsername) }, modifier = Modifier.size(48.dp)) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Pink, Violet))).padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (recipientAvatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = recipientAvatarUrl,
                                contentDescription = "Foto de perfil de $recipientUsername",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        } else {
                            Box(Modifier.fillMaxSize().clip(CircleShape).background(Panel), contentAlignment = Alignment.Center) {
                                Text(recipientUsername.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f).clickable { onOpenProfile(recipientUsername) }) {
                    Text(recipientUsername.removePrefix("@"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("Ver perfil", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = { conversationSearch = !conversationSearch; if (!conversationSearch) messageSearch = "" }) {
                    Icon(Icons.Default.Search, contentDescription = "Pesquisar nesta conversa", tint = Pink)
                }
                IconButton(onClick = { onNotice("A chamada de voz ficará disponível quando o serviço de chamadas for integrado.") }) {
                    Icon(Icons.Default.Call, contentDescription = "Ligar", tint = Color.White)
                }
                IconButton(onClick = { onNotice("A chamada de vídeo ficará disponível quando o serviço de chamadas for integrado.") }) {
                    Icon(Icons.Default.Videocam, contentDescription = "Chamada de vídeo", tint = Color.White)
                }
                Box {
                    IconButton(onClick = { showConversationMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opções da conversa", tint = Color.White)
                    }
                    DropdownMenu(expanded = showConversationMenu, onDismissRequest = { showConversationMenu = false }, modifier = Modifier.background(Panel)) {
                        DropdownMenuItem(
                            text = { Text(if (isMuted) "Ativar notificações" else "Silenciar notificações") },
                            leadingIcon = { Icon(Icons.Default.NotificationsOff, null, tint = Pink) },
                            onClick = {
                                isMuted = !isMuted
                                prefs.edit().putBoolean("muted_$recipientUsername", isMuted).apply()
                                showConversationMenu = false
                                onNotice(if (isMuted) "Notificações silenciadas neste dispositivo." else "Notificações reativadas.")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isPinned) "Desafixar conversa" else "Fixar conversa") },
                            leadingIcon = { Icon(Icons.Default.PushPin, null, tint = Pink) },
                            onClick = {
                                isPinned = !isPinned
                                prefs.edit().putBoolean("pinned_$recipientUsername", isPinned).apply()
                                showConversationMenu = false
                                onNotice(if (isPinned) "Conversa marcada como fixada neste dispositivo." else "Conversa desafixada.")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Limpar conversa") },
                            leadingIcon = { Icon(Icons.Default.DeleteForever, null, tint = Pink) },                            onClick = { showConversationMenu = false; showClearConfirm = true }
                        )
                        DropdownMenuItem(                            text = { Text("Bloquear usuário") },
                            leadingIcon = { Icon(Icons.Default.Block, null, tint = Pink) },
                            onClick = { showConversationMenu = false; showBlockConfirm = true }                        )
                        DropdownMenuItem(
                            text = { Text("Denunciar usuário") },
                            leadingIcon = { Icon(Icons.Default.Warning, null, tint = Pink) },
                            onClick = { showConversationMenu = false; showReportConfirm = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Configurações da conversa") },
                            leadingIcon = { Icon(Icons.Default.Settings, null, tint = Pink) },
                            onClick = { showConversationMenu = false; onNotice("As preferências locais desta conversa estão disponíveis neste menu.") }
                        )
                    }
                }
            }
            if (conversationSearch) {
                OutlinedTextField(
                    value = messageSearch,
                    onValueChange = { messageSearch = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Pesquisar mensagens...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Pink) },
                    trailingIcon = { IconButton(onClick = { messageSearch = ""; conversationSearch = false }) { Icon(Icons.Default.Close, contentDescription = "Fechar pesquisa") } },
                    shape = RoundedCornerShape(18.dp)
                )
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Pink)
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 14.dp)) {
                items(visibleMessages, key = { it.id }) { message ->
                    val isOwn = (ownUserId.isNotBlank() && message.senderId == ownUserId) ||
                        message.senderId.equals(prefs.getString("username", "") ?: "", ignoreCase = true)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start) {
                        val bubbleShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (isOwn) 18.dp else 5.dp, bottomEnd = if (isOwn) 5.dp else 18.dp)
                        Surface(
                            color = Color.Transparent,
                            shape = bubbleShape,
                            modifier = Modifier.widthIn(max = 340.dp).background(
                                brush = if (isOwn) Brush.linearGradient(listOf(Pink, Violet)) else Brush.linearGradient(listOf(Color(0xFF171B30), Color(0xFF111426))),
                                shape = bubbleShape
                            )
                        ) {
                            Column(Modifier.padding(horizontal = 17.dp, vertical = 14.dp)) {
                                Text(message.body, color = Color.White, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 24.sp))
                                val timeLabel = try {
                                    java.time.Instant.parse(message.createdAt).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                                } catch (_: Exception) { message.createdAt.take(16).replace("T", " ") }
                                Row(Modifier.align(Alignment.End).padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(timeLabel, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                                    if (isOwn) Icon(Icons.Default.CheckCircle, contentDescription = "Enviada", tint = Color.White.copy(alpha = 0.72f), modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                            if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
                            else onNotice("Nenhuma câmera disponível.")
                        } catch (_: Exception) { onNotice("Não foi possível abrir a câmera.") }
                    },
                    modifier = Modifier.size(48.dp).background(Brush.linearGradient(listOf(Pink, Violet)), CircleShape)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Abrir câmera", tint = Color.White, modifier = Modifier.size(25.dp))
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(2000) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escreva uma mensagem...", maxLines = 1) },
                    shape = RoundedCornerShape(28.dp),
                    maxLines = 4,
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onNotice("O seletor de emojis será integrado à caixa de mensagem.") }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.EmojiEmotions, contentDescription = "Emojis", tint = Muted, modifier = Modifier.size(22.dp))
                            }
                            IconButton(onClick = { onNotice("O envio de fotos ainda precisa ser conectado ao backend.") }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Image, contentDescription = "Escolher imagem", tint = Muted, modifier = Modifier.size(22.dp))
                            }
                            IconButton(onClick = { onNotice("O envio de áudio ainda precisa ser conectado ao backend.") }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Mic, contentDescription = "Gravar áudio", tint = Muted, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                )
                IconButton(
                    onClick = {
                        if (draft.isBlank()) onNotice("Escreva uma mensagem para enviar.")
                        else if (!sending) {
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
                        }
                    },
                    modifier = Modifier.size(48.dp).background(Brush.linearGradient(listOf(Pink, Violet)), CircleShape),
                    enabled = !sending
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar mensagem", tint = Color.White, modifier = Modifier.size(25.dp))
                }
            }
            if (showClearConfirm) {
                AlertDialog(
                    onDismissRequest = { showClearConfirm = false },
                    title = { Text("Limpar conversa?") },
                    text = { Text("O servidor atual não oferece exclusão de mensagens. Esta ação limpa apenas a visualização; as mensagens voltarão quando a conversa for recarregada.") },
                    confirmButton = {
                        TextButton(onClick = {
                            messages = emptyList()
                            showClearConfirm = false
                            onNotice("Visualização limpa; as mensagens continuam no servidor.")
                        }) { Text("Limpar visualização", color = Pink) }
                    },
                    dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Cancelar") } },
                    containerColor = Panel, titleContentColor = Color.White, textContentColor = Muted
                )
            }
            if (showBlockConfirm) {
                AlertDialog(
                    onDismissRequest = { showBlockConfirm = false },
                    title = { Text("Bloquear usuário?") },
                    text = { Text("O backend atual ainda não oferece a operação de bloqueio. Nenhuma alteração de conta será feita.") },
                    confirmButton = { TextButton(onClick = { showBlockConfirm = false; onNotice("Bloqueio precisa ser habilitado no backend.") }) { Text("Entendi", color = Pink) } },
                    dismissButton = { TextButton(onClick = { showBlockConfirm = false }) { Text("Cancelar") } },
                    containerColor = Panel, titleContentColor = Color.White, textContentColor = Muted
                )
            }
            if (showReportConfirm) {
                AlertDialog(
                    onDismissRequest = { showReportConfirm = false },
                    title = { Text("Denunciar usuário?") },
                    text = { Text("O backend atual ainda não oferece o envio de denúncias. Nenhuma denúncia será enviada agora.") },
                    confirmButton = { TextButton(onClick = { showReportConfirm = false; onNotice("Denúncias precisam ser habilitadas no backend.") }) { Text("Entendi", color = Pink) } },
                    dismissButton = { TextButton(onClick = { showReportConfirm = false }) { Text("Cancelar") } },
                    containerColor = Panel, titleContentColor = Color.White, textContentColor = Muted
                )
            }
        }
    }
}

@Composable
private fun ProfilePage(username: String, displayName: String, bio: String, photoRefreshKey: Int, onNotice: (String) -> Unit, onStat: (String) -> Unit, onAuth: () -> Unit, onEdit: () -> Unit, onEditProfilePhoto: () -> Unit, onSettings: () -> Unit) {
    var postsCount by remember { mutableIntStateOf(0) }
    var followersCount by remember { mutableIntStateOf(0) }
    var followingCount by remember { mutableIntStateOf(0) }
    var profilePosts by remember { mutableStateOf<List<VyboraApi.PostSummary>>(emptyList()) }
    var profileTab by remember { mutableStateOf("Publicações") }
    var loadingPosts by remember { mutableStateOf(true) }
    var avatarUrl by remember { mutableStateOf("") }
    var profilePhotoUrl by remember { mutableStateOf("") }
    var originalCoverUrl by remember { mutableStateOf("") }
    var expandedImage by remember { mutableStateOf("") }

    val shownName = displayName.ifBlank { username }
    val shownBio = bio
    val profileContext = androidx.compose.ui.platform.LocalContext.current
    val profilePrefs = remember { profileContext.getSharedPreferences("vybora_session", android.content.Context.MODE_PRIVATE) }

    LaunchedEffect(username, photoRefreshKey) {
        loadingPosts = true
        if (username.isNotBlank()) {
            Thread {
                try {
                    val token = profilePrefs.getString("token", "") ?: ""
                    val response = if (token.isNotBlank()) {
                        try { VyboraApi.getMyProfile(token) } catch (_: Exception) { VyboraApi.getPublicProfile(username) }
                    } else VyboraApi.getPublicProfile(username)
                    val user = response.optJSONObject("user") ?: response.optJSONObject("profile") ?: response
                    // Falha no feed de publicações não pode impedir o carregamento da capa do perfil.
                    val loadedPosts = try {
                        VyboraApi.getPosts().filter { it.username.equals(username, ignoreCase = true) }
                    } catch (_: Exception) { emptyList() }
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        if (user != null) {
                            postsCount = user.optInt("posts_count")
                            followersCount = user.optInt("followers")
                            followingCount = user.optInt("following")
                            fun cleanImageValue(value: String): String =
                                value.trim().takeUnless { it.isBlank() || it.equals("null", true) || it.equals("undefined", true) }.orEmpty()
                            val coverPath = cleanImageValue(user.optString("cover_path"))
                            val savedCover = cleanImageValue(user.optString("coverUrl"))
                                .ifBlank { cleanImageValue(user.optString("cover_url")) }
                                .ifBlank {
                                    if (coverPath.startsWith("http")) coverPath else if (coverPath.isNotBlank()) "https://jqomgrdlocdqaarfqkay.supabase.co/storage/v1/object/public/post-media/$coverPath" else ""
                                }
                            avatarUrl = cleanImageValue(user.optString("avatarUrl"))
                                .ifBlank { cleanImageValue(user.optString("avatar_url")) }
                                .ifBlank {
                                    val avatarPath = cleanImageValue(user.optString("avatar_path"))
                                    if (avatarPath.startsWith("http")) avatarPath else if (avatarPath.isNotBlank()) "https://jqomgrdlocdqaarfqkay.supabase.co/storage/v1/object/public/post-media/$avatarPath" else ""
                                }
                            profilePhotoUrl = savedCover.ifBlank {
                                cleanImageValue(user.optString("profilePhotoUrl")).ifBlank { avatarUrl }
                            }
                            val originalPath = cleanImageValue(user.optString("cover_original_path"))
                            originalCoverUrl = cleanImageValue(user.optString("originalCoverUrl")).ifBlank {
                                if (originalPath.startsWith("http")) originalPath else if (originalPath.isNotBlank()) "https://jqomgrdlocdqaarfqkay.supabase.co/storage/v1/object/public/post-media/$originalPath" else ""
                            }
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
        // Capa do perfil: proporção 16:9, igual à área de enquadramento do editor.
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            // Capa provisória padrão para não deixar o topo do perfil vazio enquanto corrigimos o carregamento da capa salva.
            val coverImage = profilePhotoUrl.ifBlank { avatarUrl }.ifBlank { "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1600&h=900&q=85" }
            AsyncImage(model = coverImage, contentDescription = "Foto de capa do perfil", modifier = Modifier.fillMaxSize().clickable { expandedImage = originalCoverUrl.ifBlank { coverImage } }, contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops = arrayOf(0.0f to Color.Black.copy(alpha = 0.10f), 0.48f to Color.Transparent, 0.76f to Night.copy(alpha = 0.68f), 1.0f to Night))))
            IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 10.dp).size(38.dp)) {
                Icon(Icons.Default.Menu, contentDescription = "Configurações do perfil", tint = Color.White, modifier = Modifier.size(25.dp))
            }
            IconButton(onClick = onEditProfilePhoto, modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).size(36.dp).background(Color.Black.copy(alpha = 0.32f), CircleShape)) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Editar foto do perfil", tint = Color.White.copy(alpha = 0.88f), modifier = Modifier.size(20.dp))
            }
            if (loadingPosts) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.BottomCenter), color = Pink)
        }
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(shownName.ifBlank { if (username.isBlank()) "Seu nome" else username }, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                    Column(Modifier.clickable { onEdit() }.padding(horizontal = 2.dp, vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar informações do perfil", tint = Color.White, modifier = Modifier.size(18.dp))
                        Box(Modifier.width(18.dp).height(2.dp).background(Brush.horizontalGradient(listOf(Pink, Violet)), RoundedCornerShape(2.dp)))
                    }
                }
                Text(if (username.isBlank()) "Entre ou crie sua conta" else "@${username.removePrefix("@")}", color = Color.White.copy(alpha = 0.82f), maxLines = 1)
            }
            Row(Modifier.weight(1.65f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                ProfileStat("Publicações", postsCount.toString(), Icons.Outlined.GridView) { onStat("Publicações") }
                ProfileStat("Seguidores", followersCount.toString(), Icons.Default.Groups) { onStat("Seguidores") }
                ProfileStat("Seguindo", followingCount.toString(), Icons.Default.PersonAddAlt1) { onStat("Seguindo") }
            }
        }
        if (shownBio.isNotBlank()) Text(shownBio, Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 5.dp), color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
        else Text("Ainda não adicionou uma descrição.", Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 5.dp), color = Muted, style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            listOf("Publicações", "Vídeos", "Republicações", "Curtidas", "Salvos").forEach { item ->
                Column(Modifier.weight(1f).clickable { profileTab = item }.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = when (item) {
                        "Vídeos" -> Icons.Default.PlayCircle
                        "Republicações" -> Icons.Default.Repeat
                        "Curtidas" -> Icons.Default.FavoriteBorder
                        "Salvos" -> Icons.Default.BookmarkBorder
                        else -> Icons.Outlined.GridView
                    }, contentDescription = item, tint = if (profileTab == item) Pink else Muted, modifier = Modifier.size(25.dp))
                    Text(item, color = if (profileTab == item) Color.White else Muted, fontWeight = if (profileTab == item) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    Box(Modifier.fillMaxWidth().height(3.dp).background(if (profileTab == item) Brush.horizontalGradient(listOf(Pink, Violet)) else Brush.horizontalGradient(listOf(Night, Night))))
                }
            }
        }
        if (profileTab != "Publicações" && profileTab != "Vídeos") {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(when (profileTab) {
                    "Republicações" -> "Suas republicações aparecerão aqui"
                    "Curtidas" -> "Publicações que você curtiu"
                    else -> "Publicações que você salvou"
                }, color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        } else if (loadingPosts) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Pink) }
        } else if (profilePosts.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f).padding(22.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.GridView, contentDescription = null, tint = Violet, modifier = Modifier.size(38.dp))
                    Text("Você ainda não publicou nada", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Compartilhe sua primeira publicação com a comunidade.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(profilePosts.size) { index ->
                    val post = profilePosts[index]
                    Box(Modifier.fillMaxWidth().height(145.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(Color(0xFF241047), Color(0xFF9D236E), Color(0xFF071A38)))).clickable { if (post.mediaUrl.isNotBlank()) expandedImage = post.mediaUrl }) {
                        if (post.mediaUrl.isNotBlank()) AsyncImage(model = post.mediaUrl, contentDescription = "Publicação", modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                        else Column(Modifier.align(Alignment.Center).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (post.mediaType == "video") Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                            Text(post.caption.ifBlank { "QINBLY" }, color = Color.White, maxLines = 4, style = MaterialTheme.typography.labelSmall)
                        }
                        if (post.mediaType == "video") Icon(Icons.Default.PlayCircle, contentDescription = "Vídeo", tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(5.dp))
                    }
                }
            }
        }
    }
    if (expandedImage.isNotBlank()) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { expandedImage = "" },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            // Visualização em tela cheia usando FIT: mostra a imagem inteira, sem recortar as bordas.
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                AsyncImage(
                    model = expandedImage,
                    contentDescription = "Foto de capa completa",
                    modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 8.dp, vertical = 24.dp).clickable { expandedImage = "" },
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
                IconButton(
                    onClick = { expandedImage = "" },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.65f), CircleShape)
                ) { Icon(Icons.Default.ArrowBack, contentDescription = "Fechar imagem", tint = Color.White) }
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
    var googleError by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    // Clear stale profile-photo errors when opening authentication.
    androidx.compose.runtime.LaunchedEffect(Unit) { error = ""; googleError = "" }
    val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val activity = context as? android.app.Activity
        if (activity == null) {
            loading = false
            googleError = "Não foi possível concluir o login Google."
        } else {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                try {
                    val idToken = GoogleSignIn.idTokenFromResult(result.data)
                    val auth = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        VyboraApi.authenticateWithGoogle(idToken)
                    }
                    prefs.edit().putString("token", auth.token).putString("refresh_token", auth.refreshToken).putString("username", auth.username).apply()
                    rememberVyboraAccount(prefs, auth.username, auth.token, prefs.getString("display_name", "") ?: "", prefs.getString("bio", "") ?: "")
                    loading = false
                    onSuccess(auth.username)
                } catch (e: Exception) {
                    loading = false
                    val authMessage = e.message?.takeIf { it.isNotBlank() } ?: "Erro de autenticação sem detalhes retornados."
                    googleError = authMessage
                    error = ""
                    android.widget.Toast.makeText(context, authMessage, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(Night).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel, enabled = !loading) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            Spacer(Modifier.width(4.dp))
            Text("Acesso à conta", color = Muted, style = MaterialTheme.typography.titleSmall)
        }

        Column(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(76.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Pink, Violet))),
                contentAlignment = Alignment.Center
            ) {
                Text("V", color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            }
            Text("QINBLY", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text(
                if (createAccount) "Crie sua conta e faça parte da comunidade." else "Entre para se conectar com sua comunidade.",
                color = Muted, style = MaterialTheme.typography.bodyMedium
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(26.dp)),
            color = Panel,
            shape = RoundedCornerShape(26.dp)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(if (createAccount) "Criar conta" else "Bem-vindo de volta", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(if (createAccount) "Preencha os dados para começar." else "Escolha como deseja entrar.", color = Muted, style = MaterialTheme.typography.bodySmall)

                Button(
                    onClick = {
                        val activity = context as? android.app.Activity
                        if (activity == null) {
                            googleError = "Não foi possível abrir a seleção da conta Google."
                        } else {
                            loading = true
                            // Reset errors for this Google attempt; profile errors stay separate.
                            error = ""
                            googleError = ""
                            GoogleSignIn.signOut(activity) {
                                try {
                                    googleSignInLauncher.launch(GoogleSignIn.signInIntent(activity))
                                } catch (e: Exception) {
                                    loading = false
                                    googleError = e.message ?: "Não foi possível abrir o login Google."
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    enabled = !loading,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Color.White)
                ) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Continuar com Google", fontWeight = FontWeight.Bold)
                }

                // Show only errors from the current Google authentication attempt.
                if (googleError.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Pink.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Pink.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = googleError,
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            color = Pink,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.14f))
                    Text("  ou entre com e-mail  ", color = Muted, style = MaterialTheme.typography.labelSmall)
                    androidx.compose.material3.HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.14f))
                }

                if (createAccount) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.trim().replace(" ", "") },
                        label = { Text("Nome de usuário") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Pink, unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                            focusedLabelColor = Pink, cursorColor = Pink, focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White, focusedContainerColor = Night, unfocusedContainerColor = Night
                        )
                    )
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    label = { Text("E-mail") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Pink, unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                        focusedLabelColor = Pink, cursorColor = Pink, focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White, focusedContainerColor = Night, unfocusedContainerColor = Night
                    )
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha (mínimo 8 caracteres)") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Pink, unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                        focusedLabelColor = Pink, cursorColor = Pink, focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White, focusedContainerColor = Night, unfocusedContainerColor = Night
                    )
                )

                Button(
                    onClick = {
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
                                        prefs.edit().putString("token", result.token).putString("refresh_token", result.refreshToken).putString("username", result.username).apply()
                                        rememberVyboraAccount(prefs, result.username, result.token, prefs.getString("display_name", "") ?: "", prefs.getString("bio", "") ?: "")
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
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    enabled = !loading,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.White)
                ) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text(if (createAccount) "Criar minha conta" else "Entrar", fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = { createAccount = !createAccount; error = "" },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading
                ) {
                    Text(if (createAccount) "Já tem conta? Fazer login" else "Não tem conta? Criar agora", color = Violet, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !loading,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Violet.copy(alpha = 0.7f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Violet)
        ) {
            Text("Voltar ao perfil", fontWeight = FontWeight.SemiBold)
        }
        Text(
            "Se o acesso falhar por configuração do servidor, verifique a API_BASE_URL conforme BACKEND/README.md.",
            color = Muted, style = MaterialTheme.typography.labelSmall
        )
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
    var adjustingImage by remember { mutableStateOf("") }
    var cropRatio by remember { mutableStateOf("16:10") }
    var imageFilter by remember { mutableStateOf("Original") }

    val effectOptions = listOf("Original", "Vivo", "Neon", "Quente", "Frio", "Preto e branco", "Cinema", "Retrô", "Brilho", "Contraste", "Saturação", "Fade", "Glow", "Desfoque", "VHS", "Granulado", "Vinheta")
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
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    loading = false
                                    onSaved(username.trim(), name.trim(), bio.trim())
                                    onNotice("Informações do perfil atualizadas.")
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
            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name.ifBlank { "Seu nome" }, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("@${username.ifBlank { "seuusuario" }}", color = Muted, style = MaterialTheme.typography.bodyLarge)
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
                        if (!singleLine) Text("${value.length}/300", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End))
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
private fun EditProfilePhotoPage(onBack: () -> Unit, onSaved: () -> Unit, onNotice: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("vybora_session", android.content.Context.MODE_PRIVATE) }
    var selectedProfilePhoto by remember { mutableStateOf<Uri?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var profilePhotoScale by remember { mutableFloatStateOf(1f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenWidthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    val previewWidthPx = with(density) { (screenWidthDp - 16).coerceAtLeast(1).dp.toPx() }
    val previewHeightPx = previewWidthPx * 9f / 16f
    val profilePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedProfilePhoto = uri
            dragX = 0f
            dragY = 0f
            profilePhotoScale = 1f
            error = ""
        }
    }

    LaunchedEffect(Unit) { profilePhotoPicker.launch("image/*") }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = !loading) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Cancelar e voltar", tint = Color.White)
            }
            Text(
                "Arraste para ajustar",
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(
                onClick = { if (selectedProfilePhoto != null && !loading) {
                    // O botão superior confirma a mesma ação do botão Salvar inferior.
                    error = ""
                } },
                enabled = selectedProfilePhoto != null && !loading
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Confirmar ajuste", tint = Color.White)
            }
        }

        Box(
            Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (selectedProfilePhoto != null) {
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp).aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF101010))
                        .pointerInput(selectedProfilePhoto) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                // Permite arrastar e ampliar/reduzir a foto para ajustar o enquadramento 16:9.
                                profilePhotoScale = (profilePhotoScale * zoom).coerceIn(0.5f, 4f)
                                dragX += pan.x
                                dragY += pan.y
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Fundo de preenchimento: ocupa a foto inteira, sem faixas pretas.
                    AsyncImage(
                        model = selectedProfilePhoto,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))
                    // Foto completa por cima: preserva toda a imagem e permite movê-la livremente.
                    AsyncImage(
                        model = selectedProfilePhoto,
                        contentDescription = "Foto completa sobre a foto do perfil em formato amplo; arraste livremente",
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            translationX = dragX
                            translationY = dragY
                            scaleX = profilePhotoScale
                            scaleY = profilePhotoScale
                        },
                        contentScale = ContentScale.Fit
                    )
                    // Grade de referência na mesma proporção 16:9 da foto do perfil.
                    Row(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxHeight().border(width = 0.5.dp, color = Color.White.copy(alpha = 0.45f)))
                        Box(Modifier.weight(1f).fillMaxHeight().border(width = 0.5.dp, color = Color.White.copy(alpha = 0.45f)))
                        Box(Modifier.weight(1f).fillMaxHeight().border(width = 0.5.dp, color = Color.White.copy(alpha = 0.45f)))
                    }
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxWidth().border(width = 0.5.dp, color = Color.White.copy(alpha = 0.45f)))
                        Box(Modifier.weight(1f).fillMaxWidth().border(width = 0.5.dp, color = Color.White.copy(alpha = 0.45f)))
                        Box(Modifier.weight(1f).fillMaxWidth().border(width = 0.5.dp, color = Color.White.copy(alpha = 0.45f)))
                    }
                    // Cantos brancos da moldura de ajuste.
                    Box(Modifier.align(Alignment.TopStart).size(width = 22.dp, height = 4.dp).background(Color.White))
                    Box(Modifier.align(Alignment.TopStart).size(width = 4.dp, height = 22.dp).background(Color.White))
                    Box(Modifier.align(Alignment.TopEnd).size(width = 22.dp, height = 4.dp).background(Color.White))
                    Box(Modifier.align(Alignment.TopEnd).size(width = 4.dp, height = 22.dp).background(Color.White))
                    Box(Modifier.align(Alignment.BottomStart).size(width = 22.dp, height = 4.dp).background(Color.White))
                    Box(Modifier.align(Alignment.BottomStart).size(width = 4.dp, height = 22.dp).background(Color.White))
                    Box(Modifier.align(Alignment.BottomEnd).size(width = 22.dp, height = 4.dp).background(Color.White))
                    Box(Modifier.align(Alignment.BottomEnd).size(width = 4.dp, height = 22.dp).background(Color.White))
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Text("Escolha uma foto para ajustar", color = Color.White, fontWeight = FontWeight.Medium)
                    OutlinedButton(onClick = { profilePhotoPicker.launch("image/*") }) {
                        Text("Escolher imagem", color = Color.White)
                    }
                }
            }
        }

        if (error.isNotBlank()) {
            Text(error, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), color = Pink, style = MaterialTheme.typography.bodySmall)
        }

        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = !loading,
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Cancelar", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = {
                    val uri = selectedProfilePhoto
                    val token = prefs.getString("token", null)
                    if (uri == null) {
                        error = "Escolha uma imagem para ajustar."
                    } else if (token.isNullOrBlank()) {
                        error = "Entre na sua conta para alterar a foto."
                    } else {
                        loading = true
                        error = ""
                        val x = dragX
                        val y = dragY
                        val scale = profilePhotoScale
                        val previewW = previewWidthPx
                        val previewH = previewHeightPx
                        Thread {
                            var source: Bitmap? = null
                            var output: Bitmap? = null
                            try {
                                // Salva exatamente o enquadramento mostrado no editor, na mesma proporção 16:9 da capa.
                                source = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                                    ?: throw IllegalStateException("Não foi possível abrir a imagem.")
                                val outW = 1600
                                val outH = 900
                                output = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(output!!)
                                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                                // Camada de fundo: mesmo comportamento ContentScale.Crop da prévia.
                                val bgScale = maxOf(outW.toFloat() / source!!.width, outH.toFloat() / source!!.height)
                                val bgW = source!!.width * bgScale
                                val bgH = source!!.height * bgScale
                                canvas.drawBitmap(source!!, null, android.graphics.RectF((outW-bgW)/2f, (outH-bgH)/2f, (outW+bgW)/2f, (outH+bgH)/2f), paint)
                                canvas.drawColor(android.graphics.Color.argb(71, 0, 0, 0))
                                // Camada principal: mesmo ContentScale.Fit, zoom e arrasto escolhidos pela pessoa.
                                val fitScale = minOf(outW.toFloat() / source!!.width, outH.toFloat() / source!!.height) * scale
                                val fgW = source!!.width * fitScale
                                val fgH = source!!.height * fitScale
                                val left = (outW - fgW) / 2f + x * outW / previewW
                                val top = (outH - fgH) / 2f + y * outH / previewH
                                canvas.drawBitmap(source!!, null, android.graphics.RectF(left, top, left + fgW, top + fgH), paint)
                                val outFile = File(context.cacheDir, "vybora-cover-framing.jpg")
                                FileOutputStream(outFile).use { stream -> output!!.compress(Bitmap.CompressFormat.JPEG, 94, stream) }
                                // Guarda a foto original separadamente para que tocar na capa abra a imagem completa.
                                VyboraApi.uploadProfileImage(context, token, uri, "cover_original")
                                // A capa visível no perfil continua usando exatamente o enquadramento escolhido.
                                VyboraApi.uploadProfileImage(context, token, Uri.fromFile(outFile), "cover")
                                outFile.delete()
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    loading = false
                                    onSaved()
                                }
                            } catch (e: Exception) {
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    loading = false
                                    error = e.message?.takeIf { it.isNotBlank() } ?: "Falha ao salvar a imagem da capa."
                                }
                            } finally {
                                source?.recycle()
                                output?.recycle()
                            }
                        }.start()
                    }
                },
                enabled = !loading && selectedProfilePhoto != null,
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.White)
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Salvar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SettingsPage(prefs: android.content.SharedPreferences, username: String, onBack: () -> Unit, onNotice: (String) -> Unit, onEditProfile: () -> Unit, onAuth: () -> Unit, onSwitchAccounts: () -> Unit, onLogout: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var section by remember { mutableStateOf("home") }
    BackHandler {
        if (section != "home") section = "home" else onBack()
    }
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
    var contentComments by remember { mutableStateOf(prefs.getBoolean("setting_content_comments", true)) }
    var contentMentions by remember { mutableStateOf(prefs.getBoolean("setting_content_mentions", true)) }
    var contentDirectMessages by remember { mutableStateOf(prefs.getBoolean("setting_content_dm", true)) }
    var contentReuse by remember { mutableStateOf(prefs.getBoolean("setting_content_reuse", true)) }
    var contentShareProfile by remember { mutableStateOf(prefs.getBoolean("setting_content_share_profile", true)) }
    var contentFollowing by remember { mutableStateOf(prefs.getBoolean("setting_content_following", true)) }
    var contentLikedVideos by remember { mutableStateOf(prefs.getBoolean("setting_content_liked", true)) }
    var contentViewers by remember { mutableStateOf(prefs.getBoolean("setting_content_viewers", true)) }

    fun exportAccountData() {
        val token = prefs.getString("token", null)
        if (token.isNullOrBlank()) {
            onNotice("Entre na sua conta antes de exportar os dados.")
            return
        }
        onNotice("Preparando a exportação dos seus dados…")
        Thread {
            try {
                val json = VyboraApi.exportMyData(token).toString(2)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    try {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, "Meus dados do QINBLY")
                            putExtra(Intent.EXTRA_TEXT, json)
                        }
                        context.startActivity(Intent.createChooser(share, "Salvar ou compartilhar seus dados"))
                        onNotice("Exportação preparada. Escolha um aplicativo para salvar o arquivo.")
                    } catch (_: Exception) {
                        onNotice("Os dados foram obtidos, mas não foi possível abrir o compartilhamento.")
                    }
                }
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onNotice(e.message ?: "Não foi possível exportar seus dados.")
                }
            }
        }.start()
    }

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
                Text(if (section == "home") "Gerencie sua conta e sua experiência" else "Personalize sua experiência no QINBLY", color = Muted, style = MaterialTheme.typography.bodySmall)
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
                    Triple("account", "Conta e perfil", "Editar perfil, login e segurança"),
                    Triple("privacy", "Privacidade e segurança", "Conta privada, bloqueios e controle de interações"),
                    Triple("notifications", "Notificações", "Curtidas, comentários, mensagens e lives"),
                    Triple("content", "Conteúdo e interações", "Mensagens, comentários, sugestões e filtros"),
                    Triple("lives", "Lives e transmissão", "Configurações de live e provedores"),
                    Triple("storage", "Armazenamento e dados", "Downloads, cache e informações da conta"),
                    Triple("appearance", "Aparência e reprodução", "Tema, idioma, reprodução automática e dados"),
                    Triple("help", "Ajuda e suporte", "Suporte, termos, privacidade e versão")
                ).filter { it.second.contains(search, true) || it.third.contains(search, true) }
                val loggedIn = username.isNotBlank() && !prefs.getString("token", null).isNullOrBlank()
                val accountActionMatches = search.isBlank() ||
                    "Alternar entre suas contas".contains(search, true) ||
                    "Sair".contains(search, true) ||
                    "Login".contains(search, true)
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
                if (!loggedIn && accountActionMatches) {
                    item {
                        Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Violet, modifier = Modifier.size(42.dp))
                                Text("Faça login para acessar mais opções", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Entre na sua conta para personalizar sua experiência e salvar suas preferências.", color = Muted, style = MaterialTheme.typography.bodySmall)
                                Button(onClick = onAuth, colors = ButtonDefaults.buttonColors(containerColor = Pink), modifier = Modifier.fillMaxWidth()) {
                                    Text("Login", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (loggedIn && accountActionMatches) {
                    item { SettingsLink("Alternar entre suas contas", "Trocar de perfil sem sair da conta", Icons.Default.Person) { onSwitchAccounts() } }
                    item { SettingsLink("Sair", "Encerrar a sessão neste dispositivo", Icons.Default.Logout) { onLogout() } }
                }
                if (categories.isEmpty() && !accountActionMatches) item { Text("Nenhuma configuração encontrada.", color = Muted, modifier = Modifier.padding(16.dp)) }
            } else when (section) {
                "account" -> {
                    item { Text(if (username.isBlank()) "Nenhuma conta conectada" else "Conta atual: @$username", color = Muted, modifier = Modifier.padding(vertical = 6.dp)) }
                        item { SettingsLink("Segurança e permissões", "Senha, verificação e acesso", Icons.Default.Lock) { try { context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + context.packageName))) } catch (_: Exception) { onNotice("Não foi possível abrir as configurações de segurança do aplicativo.") } } }
                    item { SettingsLink("Seus pedidos", "Compras e assinaturas", Icons.Default.FileDownload) { onNotice("O módulo de pedidos ainda não está conectado.") } }
                    item { SettingsLink("Compartilhar perfil", "Convide amigos para conhecer seu perfil", Icons.Default.Share) {
                        try {
                            val share = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Conheça meu perfil no QINBLY: @${username}") }
                            context.startActivity(Intent.createChooser(share, "Compartilhar perfil"))
                        } catch (_: Exception) { onNotice("Não foi possível abrir o compartilhamento neste aparelho.") }
                    } }
                    item { SettingsLink("Dispositivos conectados", "Veja este dispositivo e encerre a sessão", Icons.Default.Lock) { onNotice("Dispositivo atual: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + ". Para encerrar esta sessão, use Trocar conta.") } }
                    item { SettingsLink("Baixar seus dados", "Exportar seus dados da conta", Icons.Default.FileDownload) { exportAccountData() } }
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
                    item { SettingsLink("Tema escuro", "O QINBLY usa tema escuro nesta versão", Icons.Default.DarkMode) { onNotice("O tema escuro está ativo. Alternância de tema ainda não foi implementada.") } }
                    item { SettingsLink("Idioma", "Português (Brasil)", Icons.Default.Language) { onNotice("O seletor de idiomas ainda não está implementado.") } }
                    item { SettingsLink("Qualidade de vídeo", "Automática", Icons.Default.VideoLibrary) { onNotice("A seleção de qualidade será conectada ao reprodutor.") } }
                    item { SettingSwitch("Reprodução automática", "Reproduzir vídeos automaticamente", autoplay, Icons.Default.PlayCircle) { autoplay = it; prefs.edit().putBoolean("setting_autoplay", it).apply() } }
                    item { SettingSwitch("Economia de dados", "Reduzir o uso de dados móveis", dataSaver, Icons.Default.FileDownload) { dataSaver = it; prefs.edit().putBoolean("setting_data_saver", it).apply() } }
                }
                "content" -> {
                    item { SettingSwitch("Comentários", "Permitir comentários nas suas publicações (salvo neste aparelho)", contentComments, Icons.Default.ChatBubbleOutline) { contentComments = it; prefs.edit().putBoolean("setting_content_comments", it).apply() } }
                    item { SettingSwitch("Menções", "Permitir menções ao seu perfil (salvo neste aparelho)", contentMentions, Icons.Default.AlternateEmail) { contentMentions = it; prefs.edit().putBoolean("setting_content_mentions", it).apply() } }
                    item { SettingSwitch("Mensagens diretas", "Preferência local para mensagens diretas", contentDirectMessages, Icons.Default.ChatBubbleOutline) { contentDirectMessages = it; prefs.edit().putBoolean("setting_content_dm", it).apply() } }
                    item { SettingSwitch("Reutilização de conteúdo", "Permitir reutilização (preferência local)", contentReuse, Icons.Default.VideoLibrary) { contentReuse = it; prefs.edit().putBoolean("setting_content_reuse", it).apply() } }
                    item { SettingSwitch("Exibir perfil ao compartilhar links", "Preferência local de visibilidade do perfil", contentShareProfile, Icons.Default.Share) { contentShareProfile = it; prefs.edit().putBoolean("setting_content_share_profile", it).apply() } }
                    item { SettingSwitch("Lista de usuários seguidos", "Permitir exibir a lista (preferência local)", contentFollowing, Icons.Default.Person) { contentFollowing = it; prefs.edit().putBoolean("setting_content_following", it).apply() } }
                    item { SettingSwitch("Vídeos curtidos", "Permitir exibir vídeos curtidos (preferência local)", contentLikedVideos, Icons.Default.FavoriteBorder) { contentLikedVideos = it; prefs.edit().putBoolean("setting_content_liked", it).apply() } }
                    item { SettingSwitch("Espectadores", "Preferência local de visualização de espectadores", contentViewers, Icons.Default.Visibility) { contentViewers = it; prefs.edit().putBoolean("setting_content_viewers", it).apply() } }
                }
                "lives" -> {
                    item { SettingsLink("Integrações de live", "Conecte serviços e provedores de transmissão", Icons.Default.Radio) { onNotice("As transmissões reais ainda exigem configurar e testar o servidor de mídia.") } }
                    item { SettingsLink("Qualidade da transmissão", "Ajustes de qualidade e conexão", Icons.Default.VideoLibrary) { onNotice("A qualidade será configurável quando o provedor estiver integrado.") } }
                    item { SettingsLink("Interações na live", "Comentários, presentes e moderação", Icons.Default.ChatBubbleOutline) { onNotice("As interações serão habilitadas junto com o backend de streaming.") } }
                }
                "storage" -> {
                    item { SettingsLink("Downloads", "Gerencie downloads e arquivos salvos", Icons.Default.FileDownload) { onNotice("O gerenciador de downloads ainda será conectado.") } }
                    item { SettingsLink("Limpar cache", "Apagar arquivos temporários do aplicativo", Icons.Default.DeleteForever) { Thread { val ok = runCatching { context.cacheDir.deleteRecursively(); context.codeCacheDir.deleteRecursively(); true }.getOrDefault(false); android.os.Handler(android.os.Looper.getMainLooper()).post { onNotice(if (ok) "Cache temporário limpo." else "Não foi possível limpar todo o cache.") } }.start() } }
                    item { SettingsLink("Baixar seus dados", "Exportar seus dados da conta", Icons.Default.FileDownload) { exportAccountData() } }
                }
                else -> {
                    item { SettingsLink("Central de ajuda", "Endereço do site em configuração", Icons.Default.HelpOutline) { onNotice("O endereço oficial da central de ajuda do QINBLY ainda não foi configurado.") } }
                    item { SettingsLink("Reportar um problema", "Enviar um relato por e-mail ou aplicativo", Icons.Default.Info) { try { val report = Intent(Intent.ACTION_SENDTO).apply { data = android.net.Uri.parse("mailto:"); putExtra(Intent.EXTRA_SUBJECT, "Relato de problema — QINBLY"); putExtra(Intent.EXTRA_TEXT, "Descreva o problema encontrado:\n\nVersão: 0.3.0\nDispositivo: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL) }; context.startActivity(Intent.createChooser(report, "Reportar problema")) } catch (_: Exception) { onNotice("Não há aplicativo de e-mail disponível neste aparelho.") } } }
                    item { SettingsLink("Sobre o QINBLY", "Versão 0.3.0 · Termos e privacidade", Icons.Default.Info) { onNotice("QINBLY 0.3.0. Os links de termos e privacidade precisam ser publicados antes do lançamento.") } }
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
    var user by remember(username) { mutableStateOf(org.json.JSONObject()) }
    var loading by remember(username) { mutableStateOf(true) }
    var isFollowing by remember(username) { mutableStateOf(false) }
    var selectedTab by remember(username) { mutableStateOf("Publicações") }
    var expandedImage by remember { mutableStateOf<String?>(null) }
    var showMoreOptions by remember { mutableStateOf(false) }
    var profilePosts by remember(username) { mutableStateOf<List<VyboraApi.PostSummary>>(emptyList()) }
    val displayName = user.optString("display_name").ifBlank { user.optString("displayName").ifBlank { username.removePrefix("@") } }
    val publicUsername = user.optString("username").ifBlank { username.removePrefix("@") }
    val avatarUrl = user.optString("avatar_url").ifBlank { user.optString("avatarUrl") }
    val profilePhotoUrl = avatarUrl.ifBlank {
        user.optString("profilePhotoUrl").ifBlank {
            user.optString("cover_url").ifBlank { "https://images.unsplash.com/photo-1519608487953-e999c86e7455?auto=format&fit=crop&w=1200&q=85" }
        }
    }
    val bio = user.optString("bio")
    LaunchedEffect(username) {
        loading = true
        Thread {
            try {
                val response = VyboraApi.getPublicProfile(username, prefs.getString("token", null))
                val found = response.optJSONObject("user") ?: org.json.JSONObject()
                val posts = VyboraApi.getPosts(prefs.getString("token", null)).filter { it.username.equals(username.removePrefix("@"), ignoreCase = true) }
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    user = found
                    isFollowing = found.optBoolean("isFollowing")
                    profilePosts = posts
                    loading = false
                }
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    loading = false
                    onNotice(e.message ?: "Não foi possível abrir este perfil.")
                }
            }
        }.start()
    }
    Column(Modifier.fillMaxSize().background(Night).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(300.dp).clickable {
            val image = profilePhotoUrl.ifBlank { avatarUrl }
            if (image.isNotBlank()) expandedImage = image
        }) {
            if (profilePhotoUrl.isNotBlank()) {
                AsyncImage(model = profilePhotoUrl, contentDescription = "Foto do perfil de $displayName", modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF251039), Color(0xFF070914), Violet.copy(alpha = 0.75f)))))
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent, Night))))
            Row(Modifier.fillMaxWidth().align(Alignment.TopStart).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showMoreOptions = true }) { Icon(Icons.Default.MoreHoriz, contentDescription = "Opções deste perfil", tint = Color.White, modifier = Modifier.size(25.dp)) }
            }
            Row(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 18.dp, end = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(Modifier.weight(1.05f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(displayName, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                        Icon(Icons.Default.CheckCircle, contentDescription = "Perfil", tint = Pink, modifier = Modifier.size(19.dp))
                    }
                    Text("@$publicUsername", color = Color.White.copy(alpha = 0.9f), maxLines = 1)
                }
                Row(Modifier.weight(1.7f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    ProfileStat("Publicações", user.optInt("posts_count").toString(), Icons.Outlined.GridView) { selectedTab = "Publicações" }
                    ProfileStat("Seguidores", user.optInt("followers").toString(), Icons.Default.Groups) { onNotice("A lista de seguidores será aberta quando a tela de conexões estiver disponível.") }
                    ProfileStat("Seguindo", user.optInt("following").toString(), Icons.Default.PersonAddAlt1) { onNotice("A lista de contas seguidas será aberta quando a tela de conexões estiver disponível.") }
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.BottomCenter), color = Pink)
        }
        if (bio.isNotBlank()) Text(bio, Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), color = Color.White, style = MaterialTheme.typography.bodyMedium)
        else Text("Ainda não adicionou uma descrição.", Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), color = Muted, style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            onNotice(if (following) "Agora você segue @$publicUsername." else "Você deixou de seguir @$publicUsername.")
                        }
                    } catch (e: Exception) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post { onNotice(e.message ?: "Não foi possível atualizar o seguimento.") }
                    }
                }.start()
            }, modifier = Modifier.weight(1f).height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isFollowing) Color.White else Color.Transparent, contentColor = if (isFollowing) Color.Black else Color.White)) {
                Icon(if (isFollowing) Icons.Default.CheckCircle else Icons.Default.PersonAddAlt1, contentDescription = null, tint = if (isFollowing) Color.Black else Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isFollowing) "Seguindo" else "Seguir", color = if (isFollowing) Color.Black else Color.White, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = {
                onNotice("A conversa com @$publicUsername será aberta quando a tela de mensagens estiver conectada.")
            }, modifier = Modifier.weight(0.88f).height(46.dp), border = BorderStroke(1.dp, Violet.copy(alpha = 0.9f))) {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(17.dp), tint = Pink)
                Spacer(Modifier.width(6.dp))
                Text("Mensagem", color = Pink)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            listOf("Publicações", "Vídeos", "Republicações", "Curtidas", "Salvos").forEach { section ->
                Column(Modifier.weight(1f).clickable { selectedTab = section }.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = when (section) {
                        "Vídeos" -> Icons.Default.PlayCircle
                        "Republicações" -> Icons.Default.Repeat
                        "Curtidas" -> Icons.Default.FavoriteBorder
                        "Salvos" -> Icons.Default.BookmarkBorder
                        else -> Icons.Default.GridView
                    }, contentDescription = section, tint = if (selectedTab == section) Pink else Muted, modifier = Modifier.size(25.dp))
                    Text(section, color = if (selectedTab == section) Color.White else Muted, fontWeight = if (selectedTab == section) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    Box(Modifier.fillMaxWidth().height(3.dp).background(if (selectedTab == section) Brush.horizontalGradient(listOf(Pink, Violet)) else Brush.horizontalGradient(listOf(Night, Night))))
                }
            }
        }
        if (loading) {
            Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Pink) }
        } else if (selectedTab != "Publicações") {
            Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                Text(when (selectedTab) {
                    "Vídeos" -> "Vídeos de $displayName"
                    "Republicações" -> "Republicações de $displayName"
                    "Curtidas" -> "Publicações curtidas"
                    "Salvos" -> "Publicações salvas por $displayName"
                    else -> "Publicações de $displayName"
                }, color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        } else if (profilePosts.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.GridView, contentDescription = null, tint = Violet, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Nenhuma publicação ainda", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                profilePosts.chunked(3).forEach { rowPosts ->                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        rowPosts.forEach { post ->
                            Box(Modifier.weight(1f).height(145.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFF111426)).clickable {
                                if (post.mediaUrl.isNotBlank()) expandedImage = post.mediaUrl
                            }) {
                                if (post.mediaUrl.isNotBlank()) {
                                    AsyncImage(model = post.mediaUrl, contentDescription = "Publicação de $displayName", modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                } else {
                                    Text(post.caption.ifBlank { "QINBLY" }, color = Color.White, modifier = Modifier.align(Alignment.Center).padding(8.dp), maxLines = 4)
                                }
                                if (post.mediaType == "video") Icon(Icons.Default.PlayCircle, contentDescription = "Vídeo", tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(5.dp))
                            }
                        }
                        repeat(3 - rowPosts.size) { Spacer(Modifier.weight(1f).height(145.dp)) }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    if (expandedImage != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { expandedImage = null }) {
            Box(Modifier.fillMaxWidth().heightIn(max = 650.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black)) {
                AsyncImage(model = expandedImage, contentDescription = "Imagem ampliada de $displayName", modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp, max = 650.dp).clickable { expandedImage = null }, contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                IconButton(onClick = { expandedImage = null }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.65f), CircleShape)) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar imagem", tint = Color.White)
                }
            }
        }
    }
    if (showMoreOptions) {
        AlertDialog(
            onDismissRequest = { showMoreOptions = false },
            title = { Text("Perfil de $displayName") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Ações disponíveis para este perfil:", color = Muted)
                    TextButton(onClick = {
                        showMoreOptions = false
                        onNotice("Denúncias ainda precisam ser conectadas ao backend; nenhuma denúncia foi enviada.")
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Pink)
                        Spacer(Modifier.width(8.dp))
                        Text("Denunciar perfil", color = Color.White)
                    }
                    TextButton(onClick = {
                        showMoreOptions = false
                        onNotice("Bloqueio ainda precisa ser conectado ao backend; esta conta não foi bloqueada.")
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = Pink)
                        Spacer(Modifier.width(8.dp))
                        Text("Bloquear perfil", color = Color.White)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Perfil QINBLY", "@$publicUsername"))
                    android.widget.Toast.makeText(context, "@$publicUsername copiado", android.widget.Toast.LENGTH_SHORT).show()
                    showMoreOptions = false
                }) { Text("Copiar @") }
            },
            dismissButton = {
                TextButton(onClick = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Perfil de $displayName no QINBLY: @$publicUsername") }
                    context.startActivity(Intent.createChooser(sendIntent, "Compartilhar perfil"))
                    showMoreOptions = false
                }) { Text("Compartilhar") }
            },
            containerColor = Color(0xFF111426),
            titleContentColor = Color.White,
            textContentColor = Muted
        )
    }
}

@Composable
private fun ProfileStat(label: String, count: String, icon: ImageVector, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = if (label == "Publicações") Pink else Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(count, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

private data class VyboraGalleryAsset(val uri: Uri, val isVideo: Boolean, val dateAdded: Long, val name: String)

private fun extractVyboraVideoFrames(context: android.content.Context, uri: Uri): List<Bitmap> {
    val retriever = android.media.MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        if (durationMs <= 0L) emptyList() else {
            (0..9).mapNotNull { index ->
                val timeUs = ((durationMs * 1000L) * index / 9L).coerceIn(0L, durationMs * 1000L)
                retriever.getFrameAtTime(timeUs, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
        }
    } catch (_: Exception) {
        emptyList()
    } finally {
        try { retriever.release() } catch (_: Exception) { }
    }
}

@Composable
private fun PostPage(onNotice: (String) -> Unit, onCameraCancelled: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val prefs = remember { context.getSharedPreferences("vybora_session", android.content.Context.MODE_PRIVATE) }
    var caption by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var selectedType by remember { mutableStateOf("") }
    var publishing by remember { mutableStateOf(false) }
    var cameraGranted by remember { mutableStateOf(androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) }
    var cameraError by remember { mutableStateOf("") }
    var lensFacing by remember { mutableIntStateOf(androidx.camera.core.CameraSelector.LENS_FACING_BACK) }
    var selectedFilter by remember { mutableStateOf("Original") }
    var selectedEffect by remember { mutableStateOf("Original") }
    var activeCameraPanel by remember { mutableStateOf("") }
    var selectedCameraFormat by remember { mutableStateOf("9:16") }
    var selectedVideoSpeed by remember { mutableStateOf("1×") }
    var selectedTimerSeconds by remember { mutableStateOf("Desligado") }
    var beautyLevel by remember { mutableFloatStateOf(0.35f) }
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
    var publishSettings by remember { mutableStateOf(false) }
    var editorTool by remember { mutableStateOf("Filtros") }
    var allowComments by remember { mutableStateOf(true) }
    var allowSharing by remember { mutableStateOf(true) }
    var saveToDevice by remember { mutableStateOf(false) }
    var editorText by remember { mutableStateOf("") }
    var editorSticker by remember { mutableStateOf("") }
    var editorRatio by remember { mutableStateOf("Original") }
    val editorStrokes = remember { mutableStateListOf<List<Offset>>() }
    var publishSuccess by remember { mutableStateOf(false) }
    var galleryOpen by remember { mutableStateOf(false) }
    var galleryFilter by remember { mutableStateOf("Todos") }
    var galleryAssets by remember { mutableStateOf<List<VyboraGalleryAsset>>(emptyList()) }
    var galleryLoading by remember { mutableStateOf(false) }
    var videoFrames by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    val gallerySelection = remember { mutableStateListOf<Uri>() }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        if (!granted) cameraError = "Permita o acesso à câmera para usar a câmera do QINBLY."
    }
    val galleryPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val granted = results.values.any { it }
        if (granted) {
            galleryAssets = emptyList()
            galleryOpen = true
        } else {
            onNotice("Permita o acesso às fotos e vídeos para abrir a galeria do QINBLY.")
        }
    }
    fun openVyboraGallery() {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= 34) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
            )
        } else if (android.os.Build.VERSION.SDK_INT >= 33) {
            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES, android.Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val missing = permissions.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            galleryOpen = true
            if (galleryAssets.isEmpty()) galleryLoading = true
        } else galleryPermission.launch(missing.toTypedArray())
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { sourceUri = uri; selectedUri = uri; selectedType = "foto"; showComposer = true }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { sourceUri = uri; selectedUri = uri; showComposer = true; selectedType = "vídeo" }
    }
    LaunchedEffect(showComposer, sourceUri, selectedType) {
        val uri = sourceUri
        if (showComposer && selectedType == "vídeo" && uri != null) {
            videoFrames = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                extractVyboraVideoFrames(context, uri)
            }
        } else {
            videoFrames = emptyList()
        }
    }
    LaunchedEffect(galleryOpen) {
        if (galleryOpen && galleryAssets.isEmpty()) {
            galleryLoading = true
            val assets = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val result = mutableListOf<VyboraGalleryAsset>()
                val resolver = context.contentResolver
                val collections = listOf(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI to false,
                    android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI to true
                )
                collections.forEach { (collection, isVideo) ->
                    val projection = arrayOf(
                        android.provider.MediaStore.MediaColumns._ID,
                        android.provider.MediaStore.MediaColumns.DISPLAY_NAME,
                        android.provider.MediaStore.MediaColumns.DATE_ADDED
                    )
                    resolver.query(collection, projection, null, null,
                        "${android.provider.MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns._ID)
                        val nameCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                        val dateCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATE_ADDED)
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idCol)
                            result.add(VyboraGalleryAsset(
                                android.content.ContentUris.withAppendedId(collection, id),
                                isVideo,
                                cursor.getLong(dateCol),
                                cursor.getString(nameCol) ?: ""
                            ))
                        }
                    }
                }
                result.sortedByDescending { it.dateAdded }.take(250)
            }
            galleryAssets = assets
            galleryLoading = false
        }
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
                        else { sourceUri = Uri.fromFile(file); selectedUri = sourceUri; selectedType = "vídeo"; showComposer = true }
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
            Text("Câmera QINBLY", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Câmera integrada ao aplicativo, com prévia e filtros.", color = Muted)
            Button(onClick = { cameraPermission.launch(android.Manifest.permission.CAMERA) }) { Text("Permitir câmera") }
            OutlinedButton(onClick = { imagePicker.launch("image/*") }) { Text("Escolher foto da galeria") }
            OutlinedButton(onClick = { videoPicker.launch("video/*") }) { Text("Escolher vídeo da galeria") }
            TextButton(onClick = onCameraCancelled) { Text("Voltar ao início") }
        }
        return
    }

    if (publishSuccess) {
        Box(Modifier.fillMaxSize().background(Color(0xFF050505)), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    Modifier.size(124.dp).border(3.dp, Pink, CircleShape).clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(76.dp))
                }
                Text("Publicado!", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Seu conteúdo já está no feed.", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Button(
                    onClick = {
                        publishSuccess = false
                        showComposer = false
                        selectedUri = null
                        sourceUri = null
                        selectedType = ""
                        caption = ""
                        onCameraCancelled()
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) { Text("Ver publicação", fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = {
                        publishSuccess = false
                        showComposer = false
                        selectedUri = null
                        sourceUri = null
                        selectedType = ""
                        caption = ""
                        publishSettings = false
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) { Text("Criar outra publicação") }
            }
        }
        return
    }

    if (!showComposer) {
        Box(Modifier.fillMaxSize().background(Color(0xFF080808))) {
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
                        .background(Color(0x55000000)).padding(horizontal = 4.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { if (isRecording) activeRecording?.stop(); onCameraCancelled() }) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar câmera", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = {
                        flashEnabled = !flashEnabled
                        imageCapture?.flashMode = if (flashEnabled) androidx.camera.core.ImageCapture.FLASH_MODE_ON else androidx.camera.core.ImageCapture.FLASH_MODE_OFF
                    }) {
                        Text(if (flashEnabled) "⚡" else "ϟ", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = { onNotice("Configurações da câmera") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações", tint = Color.White)
                    }
                }

                Column(
                    Modifier.align(Alignment.CenterEnd).clip(RoundedCornerShape(24.dp))
                        .background(Color(0x77201F25)).padding(horizontal = 7.dp, vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    listOf(
                        "Efeitos" to Icons.Default.AutoAwesome, "Beleza" to Icons.Default.Face,
                        "Timer" to Icons.Default.Timer, "Formato" to Icons.Default.Crop,
                        "Velocidade" to Icons.Default.Speed, "Filtros" to Icons.Default.FilterVintage,
                        "Mais" to Icons.Default.ExpandMore
                    ).forEach { (label, icon) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(42.dp).clip(CircleShape)
                                .background(if (activeCameraPanel == label) Color(0xFF55545B) else Color(0x33202025))
                                .border(1.dp, Color(0x557D7D83), CircleShape)
                                .clickable { activeCameraPanel = if (activeCameraPanel == label) "" else label },
                                contentAlignment = Alignment.Center) {
                                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (activeCameraPanel.isNotEmpty()) {
                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .padding(bottom = 150.dp, start = 12.dp, end = 12.dp)
                        .clip(RoundedCornerShape(20.dp)).background(Color(0xE916171D))
                        .border(1.dp, Color(0x557D7D83), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(activeCameraPanel, color = Color.White, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { activeCameraPanel = "" }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar painel", tint = Color.White)
                            }
                        }
                        when(activeCameraPanel) {
                            "Filtros", "Efeitos" -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Original", "Quente", "Frio", "Vivo", "Cinema", "P&B").forEach { f ->
                                    Text(f, color = Color.White, style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                                            .background(if (selectedFilter == f) Color(0xFF55545B) else Color(0xFF292A31))
                                            .clickable { selectedFilter = f; selectedEffect = f }
                                            .padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                            "Velocidade" -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                listOf("0,3×", "0,5×", "1×", "2×", "3×").forEach { v ->
                                    Text(v, color = Color.White, modifier = Modifier.clip(CircleShape)
                                        .background(if (selectedVideoSpeed == v) Color(0xFF55545B) else Color.Transparent)
                                        .clickable { selectedVideoSpeed = v }.padding(10.dp))
                                }
                            }
                            "Formato" -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                listOf("9:16", "1:1", "4:3").forEach { f ->
                                    Text(f, color = Color.White, modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedCameraFormat == f) Color(0xFF55545B) else Color(0xFF292A31))
                                        .clickable { selectedCameraFormat = f }.padding(horizontal = 18.dp, vertical = 10.dp))
                                }
                            }
                            "Timer" -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                listOf("Desligado", "3 s", "5 s", "10 s").forEach { v ->
                                    Text(v, color = Color.White, modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedTimerSeconds == v) Color(0xFF55545B) else Color(0xFF292A31))
                                        .clickable { selectedTimerSeconds = v }.padding(horizontal = 10.dp, vertical = 10.dp))
                                }
                            }
                            "Beleza" -> {
                                Text("Suavização", color = Color(0xFFE4E4E8), style = MaterialTheme.typography.bodySmall)
                                Slider(value = beautyLevel, onValueChange = { beautyLevel = it }, valueRange = 0f..1f)
                            }
                            "Mais" -> {
                                listOf("Grade", "Flash", "Qualidade", "Áudio").forEach { option ->
                                    Row(Modifier.fillMaxWidth().clickable {
                                        if (option == "Flash") {
                                            flashEnabled = !flashEnabled
                                            imageCapture?.flashMode = if (flashEnabled) androidx.camera.core.ImageCapture.FLASH_MODE_ON else androidx.camera.core.ImageCapture.FLASH_MODE_OFF
                                        } else onNotice("$option: configuração da câmera")
                                    }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(option, color = Color.White, modifier = Modifier.weight(1f))
                                        Text(if (option == "Flash" && flashEnabled) "Ligado" else "›", color = Color(0xFFB8B8BE))
                                    }
                                }
                            }
                        }
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
                        listOf("Foto", "Vídeo", "Live").forEach { option ->
                            val chosen = (option == "Foto" && captureMode == "Foto") ||
                                (option == "Vídeo" && captureMode == "Vídeo")
                            Text(
                                option,
                                color = if (chosen) Color.White else Color(0xFFB7B7B7),
                                fontWeight = if (chosen) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp)
                                    .clickable {
                                        when (option) {
                                            "Foto" -> if (!isRecording) captureMode = "Foto"
                                            "Vídeo" -> if (!isRecording) { captureMode = "Vídeo"; selectedDuration = "15 s" }
                                            "Live" -> onNotice("A câmera ao vivo será integrada ao recurso Lives do QINBLY.")
                                        }
                                    },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                        IconButton(onClick = { galleryFilter = "Todos"; openVyboraGallery() }, modifier = Modifier.size(52.dp).background(Color(0x77392843), CircleShape)) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Galeria", tint = Color.White)
                        }
                        Box(Modifier.size(78.dp).clip(CircleShape).background(Color.White).padding(5.dp).clip(CircleShape).background(Color(0xFFFF3045)).clickable {
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
                                            sourceUri = Uri.fromFile(file)
                                            selectedUri = sourceUri
                                            selectedType = "foto"
                                            showComposer = true
                                        }
                                        override fun onError(exception: androidx.camera.core.ImageCaptureException) { cameraError = "Falha ao tirar foto: " + (exception.message ?: "tente novamente") }
                                    })
                                }
                            }
                        }, contentAlignment = Alignment.Center) {
                            if (captureMode == "Vídeo") Box(Modifier.size(if (isRecording) 30.dp else 42.dp).clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape).background(if (isRecording) Color.White else Pink))
                            else Box(Modifier.size(58.dp).clip(CircleShape).background(Color(0xFFFF3045)))
                        }
                        IconButton(onClick = { videoPicker.launch("video/*") }, modifier = Modifier.size(52.dp).background(Color(0x77171827), CircleShape)) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = "Escolher vídeo", tint = Color.White)
                        }
                    }
                    if (captureMode == "Vídeo" && !isRecording) {
                        Text("Vídeo • até $selectedDuration", color = Color.White.copy(alpha = 0.86f), style = MaterialTheme.typography.labelSmall)
                    } else if (isRecording) {
                        Text("● GRAVANDO — toque para finalizar", color = Color(0xFFFF3045), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        return
    }

    if (galleryOpen) {
        Column(Modifier.fillMaxSize().background(Color(0xFF080808)).padding(horizontal = 14.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { galleryOpen = false; gallerySelection.clear() }) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar galeria", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text("Recentes", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Escolha uma foto ou vídeo", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { galleryFilter = "Todos" }) { Text("▾", color = Color.White) }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Todos", "Fotos", "Vídeos").forEach { filter ->
                    val active = galleryFilter == filter
                    Surface(
                        color = if (active) Color.White else Color(0xFF202020),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.clickable { galleryFilter = filter }
                    ) {
                        Text(filter, color = if (active) Color.Black else Color.White, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    }
                }
            }
            val visibleAssets = galleryAssets.filter { when (galleryFilter) {
                "Fotos" -> !it.isVideo
                "Vídeos" -> it.isVideo
                else -> true
            } }
            if (galleryLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) }
            } else if (visibleAssets.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma mídia encontrada.", color = Color.LightGray)
                }
            } else {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(visibleAssets.size, key = { visibleAssets[it].uri.toString() }) { index ->
                        val asset = visibleAssets[index]
                        val selectedIndex = gallerySelection.indexOf(asset.uri)
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(0.76f).clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    gallerySelection.clear()
                                    gallerySelection.add(asset.uri)
                                    sourceUri = asset.uri
                                    selectedUri = asset.uri
                                    selectedType = if (asset.isVideo) "vídeo" else "foto"
                                    galleryOpen = false
                                    showComposer = true
                                }
                        ) {
                            AsyncImage(model = asset.uri, contentDescription = asset.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            if (asset.isVideo) {
                                Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color(0x66000000)).padding(5.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    Text("VÍDEO", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(23.dp).border(1.5.dp, Color.White, CircleShape)
                                .background(if (selectedIndex >= 0) Color(0xFFB12AFF) else Color(0x55000000), CircleShape),
                                contentAlignment = Alignment.Center) {
                                if (selectedIndex >= 0) Text("1", color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            Button(
                onClick = {
                    val asset = gallerySelection.lastOrNull()?.let { selected -> galleryAssets.firstOrNull { it.uri == selected } }
                    if (asset != null) {
                        sourceUri = asset.uri
                        selectedUri = asset.uri
                        selectedType = if (asset.isVideo) "vídeo" else "foto"
                        galleryOpen = false
                        showComposer = true
                    } else onNotice("Selecione uma foto ou vídeo.")
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(52.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) { Text(if (gallerySelection.isEmpty()) "Avançar" else "Avançar (1)", fontWeight = FontWeight.Bold) }
        }
        return
    }

    // Etapa 1: editor visual. Etapa 2: opções e publicação.
    if (!publishSettings) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val uri = selectedUri
            if (uri != null && selectedType == "foto") {
                AsyncImage(
                    model = uri,
                    contentDescription = "Prévia da publicação",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (uri != null) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.widget.VideoView(ctx).apply {
                            setVideoURI(uri)
                            setOnPreparedListener { player -> player.isLooping = true; start() }
                            setOnErrorListener { _, _, _ -> true }
                        }
                    },
                    update = { view ->
                        if (view.tag != uri.toString()) {
                            view.tag = uri.toString()
                            view.setVideoURI(uri)
                            view.setOnPreparedListener { player -> player.isLooping = true; view.start() }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            val editorFilterTint = when (selectedFilter) {
                "Quente" -> Color(0x22FF9A42)
                "Frio" -> Color(0x223F8CFF)
                "Vivo" -> Color(0x18FF45B5)
                "Cinema" -> Color(0x22333355)
                "P&B" -> Color(0x55999999)
                "Retrô" -> Color(0x22C78D5A)
                else -> Color.Transparent
            }
            if (editorFilterTint != Color.Transparent) {
                Box(Modifier.fillMaxSize().background(editorFilterTint))
            }
            if (editorTool == "Desenho") {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.fillMaxSize().pointerInput(editorTool) {
                        detectDragGestures(
                            onDragStart = { start ->
                                editorStrokes.add(listOf(Offset(start.x / size.width, start.y / size.height)))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                if (editorStrokes.isNotEmpty()) {
                                    val last = editorStrokes.last()
                                    editorStrokes[editorStrokes.lastIndex] = last + Offset(
                                        (change.position.x / size.width).coerceIn(0f, 1f),
                                        (change.position.y / size.height).coerceIn(0f, 1f)
                                    )
                                }
                            }
                        )
                    }
                ) {
                    editorStrokes.forEach { points ->
                        if (points.size > 1) {
                            val path = androidx.compose.ui.graphics.Path()
                            path.moveTo(points.first().x * size.width, points.first().y * size.height)
                            points.drop(1).forEach { path.lineTo(it.x * size.width, it.y * size.height) }
                            drawPath(path, Color(0xFFFF2A9D), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 7.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                        }
                    }
                }
            }
            if (editorSticker.isNotBlank()) {
                Text(editorSticker, modifier = Modifier.align(Alignment.Center), color = Color.White, style = MaterialTheme.typography.displayLarge)
            }
            if (editorText.isNotBlank()) {
                Text(editorText, modifier = Modifier.align(Alignment.Center).padding(horizontal = 28.dp, vertical = 72.dp),
                    color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent, Color.Transparent, Color(0xEE000000)))))
            // Cabeçalho: voltar à esquerda e música centralizada, sem ferramentas horizontais.
            IconButton(
                onClick = {
                    selectedUri = null
                    sourceUri = null
                    selectedType = ""
                    caption = ""
                    editorText = ""
                    editorSticker = ""
                    editorStrokes.clear()
                    editorRatio = "Original"
                    publishSettings = false
                    showComposer = false
                },
                modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 12.dp)
                    .size(48.dp).border(1.dp, Color(0x557E828B), CircleShape)
                    .background(Color(0x88303030), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            Row(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(1.dp, Color(0x557E828B), RoundedCornerShape(28.dp))
                    .background(Color(0x88303030))
                    .clickable { onNotice("A biblioteca de músicas será conectada aqui.") }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text("Adicionar som", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            // Nova coluna de ferramentas do editor, com identidade visual própria.
            Column(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                val editorActions = listOf(
                    "Compartilhar" to Icons.Default.Share,
                    "Efeitos" to Icons.Default.AutoAwesome,
                    "Texto" to Icons.Default.Title,
                    "Stickers" to Icons.Default.ChatBubbleOutline,
                    "Editar vídeo" to Icons.Default.Movie,
                    "Camadas" to Icons.Default.Layers,
                    "Recortar" to Icons.Default.Crop
                )
                editorActions.forEachIndexed { index, action ->
                    val (tool, symbol) = action
                    val active = tool == "Editar vídeo"
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                             .shadow(if (active) 7.dp else 2.dp, CircleShape, clip = false)
                            .clip(CircleShape)
                            .background(Color(0xB52A2A2E))
                            .border(
                                width = if (active) 1.5.dp else 1.dp,
                                color = if (active) Color(0xFFE8E8EB) else Color(0x557D7D83),
                                shape = CircleShape
                            )
                            .clickable {
                                when (tool) {
                                    "Compartilhar" -> try {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            val uri = selectedUri
                                            type = if (selectedType == "vídeo") "video/*" else "image/*"
                                            if (uri != null) {
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            } else {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, caption)
                                            }
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Compartilhar publicação"))
                                    } catch (_: Exception) {
                                        onNotice("Não foi possível abrir o compartilhamento neste aparelho.")
                                    }
                                    "Efeitos" -> {
                                        editorTool = "Filtros"
                                        selectedFilter = when (selectedFilter) {
                                            "Original" -> "Quente"; "Quente" -> "Frio"; "Frio" -> "Vivo"
                                            "Vivo" -> "Cinema"; "Cinema" -> "P&B"; else -> "Original"
                                        }
                                    }
                                    "Texto" -> editorTool = "Texto"
                                    "Stickers" -> editorTool = "Stickers"
                                    "Editar vídeo" -> if (selectedType == "vídeo") editorTool = "Vídeo" else videoPicker.launch("video/*")
                                    "Camadas" -> editorTool = "Camadas"
                                    "Recortar" -> editorTool = "Ajustar"
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape)
                                .background(if (active) Color(0x332FFFFFF) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = symbol,
                                contentDescription = tool,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000))))
                    .padding(bottom = 20.dp, top = 52.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // A tira de quadros pertence à edição de vídeo. Não repetir
                // a mesma fotografia como se fossem frames de uma timeline.
                if (selectedUri != null && selectedType == "vídeo") {
                    Row(
                        Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 12.dp)
                            .clip(RoundedCornerShape(8.dp)).background(Color(0xAA171717)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier.weight(1f).height(50.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(videoFrames.size) { index ->
                                AsyncImage(
                                    model = videoFrames[index],
                                    contentDescription = "Quadro de vídeo ${index + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.width(43.dp).height(46.dp).clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                        Box(Modifier.width(3.dp).height(52.dp).background(Color.White))
                    }
                }
                if (editorTool == "Ajustar") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        listOf("Original", "1:1", "4:5", "9:16", "16:9").forEach { ratio ->
                            TextButton(onClick = { editorRatio = ratio }) {
                                Text(ratio, color = if (editorRatio == ratio) Color.White else Color.LightGray)
                            }
                        }
                    }
                }
                if (editorTool == "Texto") {
                    OutlinedTextField(
                        value = editorText,
                        onValueChange = { editorText = it },
                        placeholder = { Text("Digite o texto da foto", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White, unfocusedBorderColor = Color.LightGray,
                            cursorColor = Color.White
                        )
                    )
                }
                if (editorTool == "Stickers") {
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp)
                    ) {
                        items(listOf("✨", "❤️", "😂", "🔥", "😍", "⭐", "🎉", "🦋", "🌈", "💯")) { sticker ->
                            Text(sticker, modifier = Modifier.clip(CircleShape).clickable { editorSticker = sticker }
                                .background(if (editorSticker == sticker) Color(0x66444444) else Color.Transparent)
                                .padding(8.dp), style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onNotice("A publicação no Story será conectada ao recurso de Stories do QINBLY.") },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(28.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF77717E)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Seu Story", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { publishSettings = true },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("Avançar", fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Black)
                    }
                }
            }
        }
    } else {
        Column(
            Modifier.fillMaxSize().background(Color(0xFF080808)).verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { publishSettings = false }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar à edição", tint = Color.White)
                }
                Text("Nova publicação", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { publishSettings = false }) { Text("Editar", color = Color.White) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                if (selectedUri != null && selectedType == "foto") {
                    AsyncImage(
                        model = selectedUri,
                        contentDescription = "Mídia selecionada",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(86.dp).height(112.dp).clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(Modifier.width(86.dp).height(112.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF222222)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                }
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    placeholder = { Text("Escreva uma legenda…", color = Color.LightGray) },
                    modifier = Modifier.weight(1f),
                    minLines = 4,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0xFF555555),
                        cursorColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }
            Divider(color = Color(0xFF303030))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonAddAlt1, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(14.dp))
                Text("Marcar pessoas", color = Color.White, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Language, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(14.dp))
                Text("Adicionar localização", color = Color.White, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(14.dp))
                Text("Adicionar música", color = Color.White, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
            }
            Divider(color = Color(0xFF303030))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Público", color = Color.White)
                    Text("Todos podem ver sua publicação", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(14.dp))
                Text("Permitir comentários", color = Color.White, modifier = Modifier.weight(1f))
                androidx.compose.material3.Switch(checked = allowComments, onCheckedChange = { allowComments = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(14.dp))
                Text("Permitir compartilhamento", color = Color.White, modifier = Modifier.weight(1f))
                androidx.compose.material3.Switch(checked = allowSharing, onCheckedChange = { allowSharing = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(14.dp))
                Text("Salvar no dispositivo", color = Color.White, modifier = Modifier.weight(1f))
                androidx.compose.material3.Switch(checked = saveToDevice, onCheckedChange = { saveToDevice = it })
            }
            Button(
                onClick = {
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
                                        val selectedMediaUri = selectedUri!!
                                        val mime = context.contentResolver.getType(selectedMediaUri) ?: if (selectedType == "vídeo") "video/mp4" else "image/jpeg"
                                        var uploadUri = selectedMediaUri
                                        var tempUploadFile: java.io.File? = null
                                        var size = try { context.contentResolver.openAssetFileDescriptor(selectedMediaUri, "r")?.use { it.length } ?: -1L } catch (_: Exception) { -1L }
                                        if (size <= 0L) {
                                            // Alguns provedores de galeria/nuvem não informam o tamanho do URI.
                                            // Materializa o arquivo no cache para que o upload HTTP tenha tamanho exato.
                                            tempUploadFile = java.io.File.createTempFile("qinbly-upload-", if (mime.startsWith("video/")) ".mp4" else ".jpg", context.cacheDir)
                                            context.contentResolver.openInputStream(selectedMediaUri)?.use { input ->
                                                tempUploadFile!!.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
                                            } ?: throw IllegalStateException("Não foi possível ler a mídia selecionada.")
                                            size = tempUploadFile!!.length()
                                            uploadUri = Uri.fromFile(tempUploadFile)
                                        }
                                        try {
                                            if (size <= 0L) throw IllegalStateException("O arquivo selecionado está vazio ou não pôde ser lido.")
                                            val fileName = selectedMediaUri.lastPathSegment?.substringAfterLast('/')?.take(200)?.ifBlank { null } ?: if (mime.startsWith("video/")) "vybora-video.mp4" else "vybora-foto.jpg"
                                            val fileId = VyboraApi.uploadPostMedia(context, token, uploadUri, fileName, mime, size)
                                            VyboraApi.createMediaPost(token, caption, fileId, if (mime.startsWith("video/")) "video" else "image")
                                        } finally {
                                            tempUploadFile?.delete()
                                        }
                                    }
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        publishing = false
                                        caption = ""
                                        selectedUri = null
                                        selectedType = ""
                                        publishSettings = false
                                        publishSuccess = true
                                    }
                                } catch (e: Exception) {
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        publishing = false
                                        onNotice(e.message ?: "Não foi possível publicar.")
                                    }
                                }
                            }.start()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = !publishing,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                if (publishing) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = Color.Black)
                else Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text(if (publishing) "Publicando…" else "Publicar", fontWeight = FontWeight.Bold, color = Color.Black)
            }
            TextButton(onClick = { publishSettings = false }) {
                Text("Voltar à edição", color = Color.White)
            }
        }
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
            Text("QINBLY LIVE", color = Pink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
        Text("QINBLY • Protótipo Android", color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}