package app.vybora.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import org.json.JSONObject

/**
 * Client for the self-hosted VYBORA remote interface.
 * Only HTTPS navigation to the owned Railway origin is kept inside this WebView.
 */
class RemoteUiActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private val prefs by lazy { getSharedPreferences("vybora_session", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    override fun onDestroy() {
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
    }

    companion object {
        private const val OTA_HOST = "vybora-production.up.railway.app"
        private const val OTA_ORIGIN = "https://vybora-production.up.railway.app"
    }
}
