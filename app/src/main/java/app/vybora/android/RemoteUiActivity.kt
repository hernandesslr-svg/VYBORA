package app.vybora.android

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.os.Environment
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import org.json.JSONObject

/**
 * Client for the self-hosted VYBORA remote interface.
 * APK downloads are handled by Android; installation is handed to the system installer.
 */
class RemoteUiActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private val prefs by lazy { getSharedPreferences("vybora_session", MODE_PRIVATE) }
    private var pendingDownloadId: Long = -1L

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id != pendingDownloadId) return
            val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(id)
            manager.query(query).use { cursor ->
                if (!cursor.moveToFirst()) {
                    notifyWeb("O download não foi encontrado. Tente novamente.", true)
                    return
                }
                val statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (statusColumn < 0 || cursor.getInt(statusColumn) != DownloadManager.STATUS_SUCCESSFUL) {
                    notifyWeb("O download do VYBORA falhou. Verifique sua conexão e tente novamente.", true)
                    return
                }
            }
            val apkUri = manager.getUriForDownloadedFile(id)
            if (apkUri == null) {
                notifyWeb("O Android não disponibilizou o arquivo baixado.", true)
                return
            }
            notifyWeb("Download concluído. Preparando a instalação…", false)
            openInstallerOrRequestPermission(apkUri)
        }
    }

    /**
     * Retoma a instalação depois que o usuário autoriza o VYBORA a instalar APKs.
     * O URI é mantido nas preferências porque abrir as configurações pausa esta Activity.
     */
    override fun onResume() {
        super.onResume()
        val pending = prefs.getString("pending_update_apk_uri", null) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) return
        prefs.edit().remove("pending_update_apk_uri").apply()
        try {
            startActivity(Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = Uri.parse(pending)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (_: Exception) {
            notifyWeb("O APK foi baixado, mas o Android não conseguiu abrir o instalador. Toque em Atualizar novamente.", true)
        }
    }

    private fun openInstallerOrRequestPermission(apkUri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            prefs.edit().putString("pending_update_apk_uri", apkUri.toString()).apply()
            notifyWeb("Autorize 'Instalar apps desconhecidos' para o VYBORA. Ao voltar, a instalação será aberta automaticamente.", false)
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            } catch (_: Exception) {
                notifyWeb("O APK foi baixado. Ative a permissão de instalar apps desconhecidos para o VYBORA e volte ao aplicativo.", true)
            }
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = apkUri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (_: Exception) {
            notifyWeb("O APK foi baixado, mas não foi possível abrir o instalador do Android. Toque em Atualizar novamente.", true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(
            this, downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        webView = WebView(this)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            safeBrowsingEnabled = true
        }
        webView.addJavascriptInterface(SessionBridge(), "VyboraNative")
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme == "https" && uri.host == OTA_HOST) return false
                if (uri.scheme == "https" || uri.scheme == "http") {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
                return true
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                if (url?.startsWith(OTA_ORIGIN + "/remote-ui/") == true) {
                    val token = prefs.getString("token", "").orEmpty()
                    if (token.isNotBlank()) {
                        val quoted = JSONObject.quote(token)
                        view.evaluateJavascript(
                            "sessionStorage.setItem('vybora_token', $quoted);" +
                                "window.dispatchEvent(new Event('vybora-native-session'));",
                            null
                        )
                    }
                }
            }
        }
        setContentView(webView)
        webView.loadUrl(OTA_ORIGIN + "/remote-ui/")
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }

    private fun notifyWeb(message: String, isError: Boolean) {
        if (!::webView.isInitialized) return
        val quoted = JSONObject.quote(message)
        runOnUiThread {
            if (::webView.isInitialized) {
                webView.evaluateJavascript(
                    "window.vyboraOtaNativeStatus && window.vyboraOtaNativeStatus($quoted, $isError);",
                    null
                )
            }
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(downloadReceiver) } catch (_: Exception) { }
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.removeJavascriptInterface("VyboraNative")
            webView.destroy()
        }
        super.onDestroy()
    }

    private inner class SessionBridge {
        @JavascriptInterface
        fun saveSession(token: String?, username: String?) {
            val safeToken = token?.trim().orEmpty()
            if (safeToken.length !in 20..10000) return
            val editor = prefs.edit().putString("token", safeToken)
            val safeUsername = username?.trim().orEmpty()
            if (safeUsername.isNotBlank() && safeUsername.length <= 24) editor.putString("username", safeUsername)
            editor.apply()
        }

        @JavascriptInterface
        fun clearSession() {
            prefs.edit().remove("token").remove("username").remove("display_name").remove("bio").apply()
        }

        @JavascriptInterface
        fun downloadAndInstall(apkUrl: String?) {
            runOnUiThread {
                try {
                    val uri = Uri.parse(apkUrl.orEmpty())
                    if (uri.scheme != "https" || uri.host != "github.com" || !uri.path.orEmpty().contains("/releases/download/") || !uri.path.orEmpty().endsWith("/vybora-release.apk")) {
                        notifyWeb("O endereço do APK não passou na validação de segurança.", true)
                        return@runOnUiThread
                    }
                    val request = DownloadManager.Request(uri)
                        .setTitle("Atualização do VYBORA")
                        .setDescription("Baixando a versão mais recente")
                        .setMimeType("application/vnd.android.package-archive")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setAllowedOverMetered(true)
                        .setAllowedOverRoaming(false)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "VYBORA-update.apk")
                    val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                    pendingDownloadId = manager.enqueue(request)
                    notifyWeb("Download iniciado. O Android avisará quando estiver pronto para instalar.", false)
                } catch (_: Exception) {
                    notifyWeb("Não foi possível iniciar o download. Verifique a conexão e tente novamente.", true)
                }
            }
        }
    }

    companion object {
        private const val OTA_HOST = "vybora-production.up.railway.app"
        private const val OTA_ORIGIN = "https://vybora-production.up.railway.app"
    }
}
