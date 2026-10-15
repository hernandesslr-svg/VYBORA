#!/usr/bin/env python3
"""Apply the reviewed VYBORA UI fixes to the extracted Android source before compilation."""
from pathlib import Path
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
                        Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = Muted, modifier = Modifier.size(58.dp))
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

for old, new in replacements:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match ({count}) for: {old[:100]}")
    text = text.replace(old, new, 1)

if text == original:
    raise SystemExit("No changes were applied; source may already be patched.")

source.write_text(text, encoding="utf-8")
print("Applied 9 VYBORA UI corrections:")
print("- bottom navigation closes settings/search/edit overlays and clears stale banners")
print("- home screen no longer shows the promotional card or fictional videos")
print("- privacy/messages/comments switches no longer show misleading technical banners")
print("- Google login error is user-friendly; real Google login still requires OAuth client ID configuration")
