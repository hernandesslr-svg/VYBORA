package app.vybora.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

private data class ClipItem(val creator: String, val title: String, val views: String, val emoji: String, val colors: List<Color>)
private val clips = listOf(
    ClipItem("@mundoselvagem", "Lugares que parecem outro planeta", "12,4 mil", "🏞️", listOf(Color(0xFFEC8A9D), Color(0xFF334F8A))),
    ClipItem("@darkvibes", "Ranked com a galera!", "8,7 mil", "🎮", listOf(Color(0xFF6518A8), Color(0xFF102A72))),
    ClipItem("@cozinhadalu", "Receita rápida de hoje", "5,3 mil", "🍜", listOf(Color(0xFFFF8B55), Color(0xFF8B255F))),
    ClipItem("@gatodigital", "Olha quem apareceu 😂", "3,9 mil", "🐱", listOf(Color(0xFFCA9D72), Color(0xFF4B355E)))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VyboraApp() }
    }
}

@Composable
private fun VyboraApp() {
    var tab by remember { mutableIntStateOf(1) }
    var liked by remember { mutableStateOf(setOf<String>()) }
    var notice by remember { mutableStateOf("") }
    var profileStat by remember { mutableStateOf("") }
    var showAuth by remember { mutableStateOf(false) }
    val appContext = androidx.compose.ui.platform.LocalContext.current
    val appPrefs = remember { appContext.getSharedPreferences("vybora_session", android.content.Context.MODE_PRIVATE) }
    var loggedInUser by remember { mutableStateOf(appPrefs.getString("username", "") ?: "") }
    var showSettings by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf(appPrefs.getString("display_name", "") ?: "") }
    var profileBio by remember { mutableStateOf(appPrefs.getString("bio", "") ?: "") }
    MaterialTheme(colorScheme = darkColorScheme(primary = Violet, secondary = Pink, background = Night, surface = Panel, onBackground = Color.White, onSurface = Color.White)) {
        Scaffold(containerColor = Night, bottomBar = {
            NavigationBar(containerColor = Color(0xFF080B16), contentColor = Color.White) {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Início") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Mensagens") }, label = { Text("Mensagens") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2; notice = "Publicação: a seleção e o envio de vídeos serão conectados na próxima etapa." }, icon = { Icon(Icons.Default.Add, null) }, label = { Text("Publicar") })
                NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Icon(Icons.Default.Radio, null) }, label = { Text("Lives") })
                NavigationBarItem(selected = tab == 4, onClick = { tab = 4 }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Perfil") })
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
                        Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.White, modifier = Modifier.size(25.dp).clickable { notice = "Busca global ainda depende da integração com o backend." })
                        Spacer(Modifier.width(14.dp))
                    }
                    if (tab == 4 && !showSettings && !showEditProfile) {
                        IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Menu, contentDescription = "Abrir menu do perfil e configurações", tint = Color.White) }
                    } else {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações", tint = Color.White, modifier = Modifier.size(25.dp).clickable { showSettings = true })
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
                        onLogout = {
                            appPrefs.edit().remove("token").remove("username").remove("display_name").remove("bio").apply()
                            loggedInUser = ""; profileName = ""; profileBio = ""; showSettings = false
                            notice = "Sessão encerrada neste aparelho. Seus dados online não foram apagados."
                        }
                    )
                } else if (showEditProfile) {
                    EditProfilePage(
                        initialName = profileName,
                        initialBio = profileBio,
                        onBack = { showEditProfile = false },
                        onSaved = { name, bio ->
                            profileName = name; profileBio = bio
                            appPrefs.edit().putString("display_name", name).putString("bio", bio).apply()
                            showEditProfile = false
                            notice = "Perfil atualizado."
                        },
                        onNotice = { notice = it }
                    )
                } else when (tab) {
                    0 -> SimplePage("Seu mundo, sem limites.", "Descubra criadores, acompanhe as lives e encontre novos vídeos.", "Explorar vídeos") { tab = 1 }
                    1 -> MessagesPage(onOpenMessage = { name -> notice = "Conversa com $name: mensagens reais serão ativadas quando as contas e o serviço de mensagens forem conectados." })
                    2 -> PostPage(onNotice = { notice = it })
                    3 -> LivePage(onNotice = { notice = it })
                    else -> if (showAuth) {
                        AuthPage(
                            onSuccess = { username -> loggedInUser = username; showAuth = false; notice = "Login realizado com sucesso!" },
                            onCancel = { showAuth = false }
                        )
                    } else ProfilePage(username = loggedInUser, displayName = profileName, bio = profileBio, onNotice = { notice = it }, onStat = { profileStat = it }, onAuth = { showAuth = true }, onEdit = { showEditProfile = true })
                }
            }
        }
        if (profileStat.isNotBlank()) {
            AlertDialog(
                onDismissRequest = { profileStat = "" },
                title = { Text(profileStat) },
                text = { Text(if (profileStat == "Seguidores") "Ainda não há seguidores reais. A lista aparecerá aqui quando as contas e o backend forem conectados." else if (profileStat == "Seguindo") "Você ainda não segue nenhuma conta nesta versão de demonstração." else "Ainda não há publicações reais nesta conta.") },
                confirmButton = { TextButton(onClick = { profileStat = "" }) { Text("Fechar") } }
            )
        }
    }
}


@Composable
private fun MessagesPage(onOpenMessage: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text("Mensagens", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Suas conversas com a comunidade", color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Violet, modifier = Modifier.size(42.dp))
                Text("Nenhuma conversa ainda", fontWeight = FontWeight.SemiBold)
                Text("Suas conversas reais aparecerão aqui quando a tela for conectada à API.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ProfilePage(username: String, displayName: String, bio: String, onNotice: (String) -> Unit, onStat: (String) -> Unit, onAuth: () -> Unit, onEdit: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Seu perfil", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Surface(color = Panel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(76.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Violet, Pink))), contentAlignment = Alignment.Center) { Text("V", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black) }
                Text(displayName.ifBlank { if (username.isBlank()) "Seu nome" else username }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(if (username.isBlank()) "Entre ou crie sua conta" else "@$username", color = Muted)
                if (bio.isNotBlank()) Text(bio, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ProfileStat("Publicações", "0") { onStat("Publicações") }
                    ProfileStat("Seguidores", "0") { onStat("Seguidores") }
                    ProfileStat("Seguindo", "0") { onStat("Seguindo") }
                }
                Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Edit, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Editar perfil") }
                OutlinedButton(onClick = onAuth, modifier = Modifier.fillMaxWidth()) { Text(if (username.isBlank()) "Entrar / criar conta" else "Trocar conta") }
            }
        }
        Text("Toque em Seguidores ou Seguindo para abrir a respectiva lista. Nesta base, as listas ainda não contêm usuários reais.", color = Muted, style = MaterialTheme.typography.bodySmall)
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

    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(if (createAccount) "Criar conta" else "Entrar na VYBORA", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Sua conta será criada no servidor e poderá ser usada em outros aparelhos.", color = Muted, style = MaterialTheme.typography.bodySmall)
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
private fun EditProfilePage(initialName: String, initialBio: String, onBack: () -> Unit, onSaved: (String, String) -> Unit, onNotice: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("vybora_session", android.content.Context.MODE_PRIVATE) }
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
        OutlinedTextField(value = name, onValueChange = { name = it.take(80) }, label = { Text("Nome de exibição") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !loading)
        OutlinedTextField(value = bio, onValueChange = { bio = it.take(300) }, label = { Text("Biografia") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5, enabled = !loading, supportingText = { Text("Até 300 caracteres") })
        if (error.isNotBlank()) Text(error, color = Pink)
        Button(onClick = {
            if (name.isBlank()) { error = "Informe um nome de exibição." }
            else {
                loading = true; error = ""
                val token = prefs.getString("token", null)
                Thread {
                    try {
                        if (!token.isNullOrBlank()) VyboraApi.updateProfile(token, name.trim(), bio.trim())
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            loading = false
                            if (token.isNullOrBlank()) onNotice("Perfil salvo neste aparelho. Entre na conta para sincronizar as alterações com o servidor.")
                            onSaved(name.trim(), bio.trim())
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
private fun SettingsPage(prefs: android.content.SharedPreferences, username: String, onBack: () -> Unit, onNotice: (String) -> Unit, onLogout: () -> Unit) {
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
            item { SettingsLink("Editar perfil", "Nome e biografia", Icons.Default.Person) { onNotice("Volte ao perfil e toque em Editar perfil para alterar seu nome e biografia.") } }
            item { SettingsLink("Segurança da conta", "Senha, sessões e recuperação", Icons.Default.Lock) { expanded = if (expanded == "security") "" else "security" } }
            if (expanded == "security") item { InfoPanel("Proteja sua conta", "Use uma senha exclusiva. Recuperação de senha, autenticação em duas etapas e gestão de sessões precisam estar habilitadas no backend antes de serem consideradas ativas.") }
            item { Text("PRIVACIDADE", color = Pink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            item { SettingSwitch("Conta privada", "Preferência local; só será efetiva quando a API aplicar as regras de visibilidade.", privateAccount, Icons.Default.Lock) { privateAccount = it; prefs.edit().putBoolean("setting_private_account", it).apply(); onNotice("Preferência salva. A privacidade real precisa ser aplicada e validada no servidor.") } }
            item { SettingSwitch("Mostrar status de atividade", "Permite controlar sua preferência de presença.", activityStatus, Icons.Default.Visibility) { activityStatus = it; prefs.edit().putBoolean("setting_activity_status", it).apply() } }
            item { SettingSwitch("Permitir mensagens", "Preferência para quem pode iniciar conversas; depende de validação no backend.", allowMessages, Icons.Default.ChatBubbleOutline) { allowMessages = it; prefs.edit().putBoolean("setting_messages", it).apply(); onNotice("Preferência salva neste aparelho; a API ainda precisa aplicar essa regra.") } }
            item { SettingSwitch("Permitir comentários", "Preferência de interação nas publicações.", allowComments, Icons.Default.ChatBubbleOutline) { allowComments = it; prefs.edit().putBoolean("setting_comments", it).apply(); onNotice("Preferência salva neste aparelho; a API ainda precisa aplicar essa regra.") } }
            item { SettingSwitch("Sugerir meu perfil", "Controla a preferência de descoberta do seu perfil.", suggestProfile, Icons.Default.Person) { suggestProfile = it; prefs.edit().putBoolean("setting_suggest_profile", it).apply() } }
            item { SettingsLink("Contas bloqueadas", "Gerencie quem você não quer que interaja com você", Icons.Default.Block) { onNotice("A lista de bloqueio será exibida quando o recurso de bloqueio estiver conectado ao backend.") } }
            item { Text("PREFERÊNCIAS", color = Pink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            item { SettingSwitch("Notificações", "Preferência salva no aparelho; push exige configuração do servidor.", notifications, Icons.Default.Notifications) { notifications = it; prefs.edit().putBoolean("setting_notifications", it).apply(); onNotice(if (it) "Preferência de notificações ativada." else "Preferência de notificações desativada.") } }
            item { SettingSwitch("Reprodução automática", "Controla a preferência salva neste aparelho.", autoplay, Icons.Default.PlayCircle) { autoplay = it; prefs.edit().putBoolean("setting_autoplay", it).apply() } }
            item { SettingsLink("Idioma", "Português (Brasil)", Icons.Default.Language) { onNotice("O aplicativo está em português. Seletor de idiomas ainda não implementado.") } }
            item { SettingsLink("Aparência", "Tema escuro", Icons.Default.DarkMode) { onNotice("O VYBORA está usando o tema escuro nesta versão.") } }
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
private fun ProfileStat(label: String, count: String, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Pink)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

@Composable
private fun VideoFeed(liked: Set<String>, onLike: (String) -> Unit, onProfile: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Para você", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Vídeos públicos de toda a comunidade", color = Muted, style = MaterialTheme.typography.bodySmall) }
                Surface(color = Color(0xFF231438), shape = RoundedCornerShape(30.dp)) { Text("PÚBLICO", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = Pink, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
            }
        }
        items(clips) { clip ->
            Surface(color = Panel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    Box(Modifier.fillMaxWidth().height(205.dp).background(Brush.linearGradient(clip.colors)), contentAlignment = Alignment.Center) {
                        Text(clip.emoji, style = MaterialTheme.typography.displayLarge)
                        Surface(color = Color(0xAA050711), shape = RoundedCornerShape(30.dp), modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) { Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.PlayCircle, null, tint = Pink, modifier = Modifier.size(16.dp)); Text("  VÍDEO DE EXEMPLO", style = MaterialTheme.typography.labelSmall) } }
                        Text("▶", Modifier.align(Alignment.Center).clip(CircleShape).background(Color(0x88303050)).padding(horizontal = 18.dp, vertical = 12.dp), style = MaterialTheme.typography.headlineMedium)
                    }
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).clickable { onProfile() }) { Text(clip.creator, fontWeight = FontWeight.Bold); Text(clip.title, color = Muted, style = MaterialTheme.typography.bodySmall); Text("${clip.views} visualizações • público", color = Muted, style = MaterialTheme.typography.labelSmall) }
                        IconButton(onClick = { onLike(clip.creator) }) { Icon(Icons.Default.FavoriteBorder, contentDescription = "Curtir", tint = if (clip.creator in liked) Pink else Color.White) }
                        Text(if (clip.creator in liked) "Curtido" else "Curtir", color = if (clip.creator in liked) Pink else Muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        item { Text("Conteúdo ilustrativo: os vídeos acima são cartões de demonstração, não publicações reais. O feed público real será ligado ao armazenamento e às contas na próxima etapa.", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 12.dp)) }
    }
}

@Composable
private fun PostPage(onNotice: (String) -> Unit) {
    var caption by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedType by remember { mutableStateOf("") }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { selectedUri = uri; selectedType = "vídeo" }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { selectedUri = uri; selectedType = "foto" }
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Criar publicação", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Escolha uma foto ou vídeo e escreva uma legenda.", color = Muted)
        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it },
            label = { Text("Escreva uma legenda") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    selectedUri?.let { "Arquivo selecionado: $selectedType\n${it.lastPathSegment ?: "arquivo escolhido"}" }
                        ?: "Nenhum arquivo selecionado",
                    color = if (selectedUri != null) Color.White else Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.weight(1f)) {
                        Text("Escolher foto")
                    }
                    OutlinedButton(onClick = { videoPicker.launch("video/*") }, modifier = Modifier.weight(1f)) {
                        Text("Escolher vídeo")
                    }
                }
            }
        }
        Button(
            onClick = {
                when {
                    selectedUri == null -> onNotice("Escolha uma foto ou vídeo antes de continuar.")
                    caption.isBlank() -> onNotice("Escreva uma legenda para a publicação.")
                    else -> onNotice("Publicação preparada com legenda e arquivo selecionado. O envio para outros usuários ainda precisa de um servidor de upload.")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Preparar publicação")
        }
        Text(
            "Nesta versão, o seletor de mídia e a validação funcionam no aparelho. Para publicar e aparecer no feed de outras pessoas, falta conectar o armazenamento online.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun LivePage(onNotice: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Lives", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Encontre transmissões e configure destinos externos.", color = Muted)
        Surface(color = Panel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Começar uma live", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Câmera, microfone e transmissão ao vivo ainda precisam de integração com um servidor de mídia.", color = Muted)
            Button(onClick = { onNotice("Transmissão ainda não conectada: nenhum vídeo está sendo enviado.") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Radio, null); Spacer(Modifier.width(8.dp)); Text("Configurar transmissão") }
        } }
        Surface(color = Panel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Multiplataforma", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Destinos planejados", color = Muted)
            listOf("VYBORA — plataforma principal", "YouTube Live — integração pendente", "Twitch — integração pendente", "TikTok LIVE — acesso precisa ser validado", "KICK — integração pendente").forEach { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).clip(CircleShape).background(if (it.startsWith("VYBORA")) Pink else Violet)); Text(it, Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodyMedium) } }
            OutlinedButton(onClick = { onNotice("Nenhuma conta externa foi conectada. As integrações só serão marcadas como ativas após testes reais.") }, modifier = Modifier.fillMaxWidth()) { Text("Ver status das integrações") }
        } }
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
