#!/usr/bin/env python3
"""Apply the reviewed VYBORA UI fixes to the extracted Android source before compilation."""
from pathlib import Path
import re
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: apply-vybora-ui-fixes.py <android-project-dir>")

root = Path(sys.argv[1]).resolve()
source = root / "app/src/main/java/app/vybora/android/MainActivity.kt"
if not source.is_file():
    raise SystemExit(f"MainActivity.kt not found: {source}")

text = source.read_text(encoding="utf-8")
original = text

replacements = [
(
'NavigationBarItem(selected = tab == 0, onClick = { tab = 0 },',
'NavigationBarItem(selected = tab == 0, onClick = { showSettings = false; showSearch = false; showEditProfile = false; tab = 0; notice = "" },'
),
(
'NavigationBarItem(selected = tab == 1, onClick = { tab = 1 },',
'NavigationBarItem(selected = tab == 1, onClick = { showSettings = false; showSearch = false; showEditProfile = false; tab = 1; notice = "" },'
),
(
'NavigationBarItem(selected = tab == 2, onClick = { tab = 2; notice = "Publicação: a seleção e o envio de vídeos serão conectados na próxima etapa." },',
'NavigationBarItem(selected = tab == 2, onClick = { showSettings = false; showSearch = false; showEditProfile = false; tab = 2; notice = "" },'
),
(
'NavigationBarItem(selected = tab == 3, onClick = { tab = 3 },',
'NavigationBarItem(selected = tab == 3, onClick = { showSettings = false; showSearch = false; showEditProfile = false; tab = 3; notice = "" },'
),
(
'NavigationBarItem(selected = tab == 4, onClick = { tab = 4 },',
'NavigationBarItem(selected = tab == 4, onClick = { showSettings = false; showSearch = false; showEditProfile = false; tab = 4; notice = "" },'
),
(
'0 -> SimplePage("Seu mundo, sem limites.", "Descubra criadores, acompanhe as lives e encontre novos vídeos.", "Explorar vídeos") { tab = 1 }',
'''0 -> Column(
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
),
(
'item { SettingSwitch("Conta privada", "Preferência local; só será efetiva quando a API aplicar as regras de visibilidade.", privateAccount, Icons.Default.Lock) { privateAccount = it; prefs.edit().putBoolean("setting_private_account", it).apply(); onNotice("Preferência salva. A privacidade real precisa ser aplicada e validada no servidor.") } }',
'item { SettingSwitch("Conta privada", "Controla a preferência de privacidade da sua conta.", privateAccount, Icons.Default.Lock) { privateAccount = it; prefs.edit().putBoolean("setting_private_account", it).apply() } }'
),
(
'item { SettingSwitch("Permitir mensagens", "Preferência para quem pode iniciar conversas; depende de validação no backend.", allowMessages, Icons.Default.ChatBubbleOutline) { allowMessages = it; prefs.edit().putBoolean("setting_messages", it).apply(); onNotice("Preferência salva neste aparelho; a API ainda precisa aplicar essa regra.") } }',
'item { SettingSwitch("Permitir mensagens", "Escolha se deseja permitir mensagens de outras pessoas.", allowMessages, Icons.Default.ChatBubbleOutline) { allowMessages = it; prefs.edit().putBoolean("setting_messages", it).apply() } }'
),
(
'item { SettingSwitch("Permitir comentários", "Preferência de interação nas publicações.", allowComments, Icons.Default.ChatBubbleOutline) { allowComments = it; prefs.edit().putBoolean("setting_comments", it).apply(); onNotice("Preferência salva neste aparelho; a API ainda precisa aplicar essa regra.") } }',
'item { SettingSwitch("Permitir comentários", "Escolha se deseja permitir comentários nas suas publicações.", allowComments, Icons.Default.ChatBubbleOutline) { allowComments = it; prefs.edit().putBoolean("setting_comments", it).apply() } }'
),
(
'if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) { error = "Falta configurar GOOGLE_WEB_CLIENT_ID e recompilar o APK." }',
'if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) { error = "O acesso com Google está temporariamente indisponível. Use e-mail e senha enquanto a configuração é concluída." }'
),
]

# Remove the explanatory helper line from the Google sign-in screen.
# It is redundant with the button label and should not appear to end users.
text, google_helper_removed = re.subn(
    r'(?m)^\s*Text\("O Google abre o seletor oficial de contas do Android\."[^\n]*\n',
    '',
    text
)

for old, new in replacements:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match ({count}) for: {old[:100]}")
    text = text.replace(old, new, 1)

# Google Credential Manager: don't restrict the picker to accounts previously authorized
# for this app. That restriction causes NoCredentialException / "No credentials available"
# for users who have never signed in before. Keep the server client ID as the audience.
import re

# Handle Kotlin/Java builder syntax and named-argument variants across Credential Manager versions.
# Only change the authorized-account filter when it is explicitly set to true.
credential_patterns = [
    r'(\.setFilterByAuthorizedAccounts\s*\(\s*)true(\s*\))',
    r'(\bfilterByAuthorizedAccounts\s*=\s*)true(\b)',
    r'(\bsetFilterByAuthorizedAccounts\s*\(\s*)true(\s*\))',
]
text, credential_fix_count = re.subn(
    '|'.join(f'(?:{pattern})' for pattern in credential_patterns),
    lambda m: re.sub(r'(?<![A-Za-z])true(?![A-Za-z])', 'false', m.group(0), count=1),
    text
)
# A filter already set to false is the desired final state; do not fail just
# because there was nothing left to replace on a subsequent build.
has_unrestricted_filter = bool(re.search(
    r'(?:setFilterByAuthorizedAccounts\s*\(\s*false\s*\)|filterByAuthorizedAccounts\s*=\s*false\b)',
    text
))
if credential_fix_count == 0 and not has_unrestricted_filter and ('GetGoogleIdOption' in text or 'GetGoogleIdToken' in text or 'getCredential(' in text):
    # Print a small, non-secret source excerpt to make future mismatches diagnosable.
    lines = [line.strip() for line in text.splitlines() if 'AuthorizedAccounts' in line or 'authorizedAccounts' in line or 'filterByAuthorizedAccounts' in line]
    raise SystemExit('Google Credential Manager flow found but no authorized-account filter was found. Relevant lines: ' + repr(lines[:8]))

if text == original:
    raise SystemExit("No changes were applied; source may already be patched.")

source.write_text(text, encoding="utf-8")
print("Applied 9 VYBORA UI corrections:")
print("- bottom navigation closes settings/search/edit overlays and clears stale banners")
print("- home screen no longer shows the promotional card or fictional videos")
print("- privacy/messages/comments switches no longer show misleading technical banners")
print("- Google login error is user-friendly; real Google login still requires OAuth client ID configuration")
if credential_fix_count:
    print(f"- Google Credential Manager now allows account selection for first-time users ({credential_fix_count} filter(s) updated)")

# Fetch public remote app configuration without blocking the UI thread.
api_source = root / "app/src/main/java/app/vybora/android/VyboraApi.kt"
if not api_source.is_file():
    raise SystemExit(f"VyboraApi.kt not found: {api_source}")
api_text = api_source.read_text(encoding="utf-8")
config_method = """    fun getAppConfig(): JSONObject = request("GET", "/api/app-config")

"""
if "fun getAppConfig():" not in api_text:
    anchor = "    private fun request(method: String, path: String, body: JSONObject? = null, token: String? = null): JSONObject {"
    if api_text.count(anchor) != 1:
        raise SystemExit("Could not locate VyboraApi request method to add remote config support.")
    api_text = api_text.replace(anchor, config_method + anchor, 1)
    api_source.write_text(api_text, encoding="utf-8")

# MainActivity loads remote config on launch and displays server-managed announcements/maintenance.
main_text = source.read_text(encoding="utf-8")
state_anchor = '    var loggedInUser by remember { mutableStateOf(appPrefs.getString("username", "") ?: "") }'
state_insert = state_anchor + '''
    var remoteAnnouncement by remember { mutableStateOf("") }
    var remoteMaintenance by remember { mutableStateOf(false) }
    var remoteMaintenanceMessage by remember { mutableStateOf("Estamos fazendo ajustes. Tente novamente em instantes.") }
    LaunchedEffect(Unit) {
        Thread {
            try {
                val config = VyboraApi.getAppConfig()
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    remoteAnnouncement = config.optString("announcement").trim()
                    remoteMaintenance = config.optBoolean("maintenanceMode", false)
                    remoteMaintenanceMessage = config.optString("maintenanceMessage").ifBlank { remoteMaintenanceMessage }
                }
            } catch (_: Exception) {
                // Remote configuration is optional: keep the last working UI when the API is unavailable.
            }
        }.start()
    }'''
if "var remoteAnnouncement by remember" not in main_text:
    if main_text.count(state_anchor) != 1:
        raise SystemExit("Could not locate VyboraApp state anchor for remote configuration.")
    main_text = main_text.replace(state_anchor, state_insert, 1)

# Render a server-controlled announcement in the existing notice area; maintenance gets a clear banner.
notice_anchor = '                if (notice.isNotBlank()) {'
notice_insert = '''                if (remoteMaintenance) {
                    Surface(color = Color(0xFF3A1724), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(remoteMaintenanceMessage, Modifier.padding(12.dp), color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                } else if (remoteAnnouncement.isNotBlank() && notice.isBlank()) {
                    Surface(color = Color(0xFF25143A), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(remoteAnnouncement, Modifier.padding(12.dp), color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
''' + notice_anchor
if "if (remoteMaintenance) {" not in main_text:
    if main_text.count(notice_anchor) != 1:
        raise SystemExit("Could not locate notice area for remote configuration UI.")
    main_text = main_text.replace(notice_anchor, notice_insert, 1)
source.write_text(main_text, encoding="utf-8")
print("Applied remote app-config integration:")
print("- app requests GET /api/app-config asynchronously at startup")
print("- server-controlled announcement and maintenance message can change without rebuilding the APK")
print("- API outage does not block startup")
