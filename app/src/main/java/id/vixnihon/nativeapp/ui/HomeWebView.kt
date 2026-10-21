package id.vixnihon.nativeapp.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import id.vixnihon.nativeapp.data.AppUser
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Uses the deployed NihonGo web app as the Home experience so the Android app shares
 * its actual VRM Sensei, progress/reward widgets and working navigation instead of
 * maintaining a second, incomplete Compose-only copy of those features.
 * Google sign-in itself remains native; only the server-issued NihonGo session is
 * bootstrapped into the first-party WebView origin.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HomeWebView(
    user: AppUser,
    token: String,
    language: String,
    onLogout: () -> Unit,
    onLanguageChange: (String) -> Unit
) {
    val latestOnLogout by rememberUpdatedState(onLogout)
    val latestOnLanguageChange by rememberUpdatedState(onLanguageChange)
    val latestUser by rememberUpdatedState(user)
    val latestToken by rememberUpdatedState(token)
    val latestLanguage by rememberUpdatedState(language)
    var ready by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val logoutSent = remember { AtomicBoolean(false) }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { view ->
                runCatching {
                    view.stopLoading()
                    view.loadUrl("about:blank")
                    view.clearHistory()
                    view.removeAllViews()
                    view.destroy()
                }
            }
            webViewRef = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NihongoPalette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                WebView(viewContext).apply {
                    webViewRef = this
                    setBackgroundColor(android.graphics.Color.rgb(7, 7, 13))
                    overScrollMode = View.OVER_SCROLL_NEVER
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        safeBrowsingEnabled = true
                        databaseEnabled = true
                        loadsImagesAutomatically = true
                        allowFileAccess = false
                        allowContentAccess = false
                        javaScriptCanOpenWindowsAutomatically = false
                        setSupportMultipleWindows(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        setSupportZoom(false)
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    }
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            val uri = request.url
                            if (uri.scheme.equals("nihongo-native", true)) {
                                when (uri.host?.lowercase()) {
                                    "logout" -> {
                                        if (logoutSent.compareAndSet(false, true)) latestOnLogout()
                                    }
                                    "language" -> {
                                        val next = if (uri.getQueryParameter("value") == "en") "en" else "id"
                                        latestOnLanguageChange(next)
                                    }
                                }
                                return true
                            }
                            if (isNihongoOrigin(uri)) return false
                            if (uri.scheme.equals("https", true) || uri.scheme.equals("http", true) ||
                                uri.scheme.equals("mailto", true) || uri.scheme.equals("tel", true)) {
                                return openExternal(viewContext, uri)
                            }
                            return true
                        }

                        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                            ready = false
                            loadError = false
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
                            if (!isNihongoOrigin(uri)) return
                            view.evaluateJavascript(
                                buildBootstrapScript(latestToken, latestUser, latestLanguage),
                            ) { result ->
                                when {
                                    result?.contains("RELOAD") == true -> {
                                        // First visit/session swap: reload after writing the native session,
                                        // so the web app's auth bootstrap sees it before React starts.
                                        ready = false
                                    }
                                    result?.contains("READY") == true -> {
                                        ready = true
                                        loadError = false
                                    }
                                    result?.contains("BOOTSTRAP_ERROR") == true -> {
                                        ready = false
                                        loadError = true
                                    }
                                }
                            }
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError
                        ) {
                            if (request.isForMainFrame) {
                                ready = false
                                loadError = true
                            }
                        }
                    }
                    loadUrl(BASE_URL)
                }
            },
            update = { view ->
                // Hide page pixels until native session bootstrap finishes; keep rendering active
                // so the VRM canvas can initialize while the startup cover is shown.
                view.alpha = if (ready) 1f else 0f
                // Keep the existing WebView alive across Compose recompositions. Auth state is
                // intentionally injected only by the verified-origin page bootstrap above.
                if (view.url == null) view.loadUrl(BASE_URL)
            }
        )

        if (!ready) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NihongoPalette.background),
                contentAlignment = Alignment.Center
            ) {
                if (loadError) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("日本語", color = NihongoPalette.redLight, fontSize = 38.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (language == "id") "Home NihonGo belum bisa dimuat. Periksa koneksi internet lalu coba lagi." else "NihonGo Home could not load. Check your internet connection and try again.",
                            color = NihongoPalette.dim2,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            loadError = false
                            ready = false
                            webViewRef?.reload() ?: run { }
                        }) {
                            Text(if (language == "id") "Coba lagi" else "Retry")
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("日本語", color = NihongoPalette.redLight, fontSize = 38.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(14.dp))
                        CircularProgressIndicator(color = NihongoPalette.redLight, strokeWidth = 2.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = if (language == "id") "Memuat Home dan Sensei…" else "Loading Home and Sensei…",
                            color = NihongoPalette.dim,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun buildBootstrapScript(token: String, user: AppUser, language: String): String {
    val tokenJs = JSONObject.quote(token)
    val userJson = user.toJson()
    val userJsonJs = JSONObject.quote(userJson)
    val languageJs = JSONObject.quote(if (language == "en") "en" else "id")
    return """(function(){
      try {
        const nativeToken = $tokenJs;
        const nativeUserJson = $userJsonJs;
        const nativeUser = JSON.parse(nativeUserJson);
        const currentToken = localStorage.getItem('nihongo_auth_token') || '';
        const rawUserKey = String(nativeUser.id || nativeUser.email || 'local-user').trim().toLowerCase() || 'local-user';
        const onboardingKey = 'nihongo_onboarding_simple_completed_v2:' + encodeURIComponent(rawUserKey);
        const firstBootstrap = currentToken !== nativeToken ||
          localStorage.getItem(onboardingKey) !== '1' ||
          localStorage.getItem('nihongo_landing_seen') !== '1';
        localStorage.setItem('nihongo_auth_token', nativeToken);
        localStorage.setItem('nihongo_auth_user', nativeUserJson);
        localStorage.setItem('nihongo_landing_seen', '1');
        localStorage.setItem(onboardingKey, '1');
        if (!localStorage.getItem('nihongo_settings')) {
          localStorage.setItem('nihongo_settings', JSON.stringify({ systemLang: $languageJs, appearanceMode: 'system', selectedAvatar: 'waifu.vrm' }));
        }
        if (firstBootstrap) {
          window.location.reload();
          return 'RELOAD';
        }
        if (!window.__nativeNihongoBridgeReady) {
          const originalRemoveItem = Storage.prototype.removeItem;
          Storage.prototype.removeItem = function(key) {
            if (key === 'nihongo_auth_token' && location.hostname.endsWith('vixnihon.web.id')) {
              try { window.location.href = 'nihongo-native://logout'; } catch (e) {}
              return;
            }
            return originalRemoveItem.call(this, key);
          };
          const originalSetItem = Storage.prototype.setItem;
          Storage.prototype.setItem = function(key, value) {
            const previousSettings = key === 'nihongo_settings' ? this.getItem(key) : null;
            originalSetItem.call(this, key, value);
            if (key === 'nihongo_settings' && location.hostname.endsWith('vixnihon.web.id')) {
              try {
                const previous = JSON.parse(String(previousSettings || '{}'));
                const settings = JSON.parse(String(value || '{}'));
                if ((settings.systemLang === 'id' || settings.systemLang === 'en') && previous.systemLang !== settings.systemLang) {
                  window.location.href = 'nihongo-native://language?value=' + encodeURIComponent(settings.systemLang);
                }
              } catch (e) {}
            }
          };
          window.__nativeNihongoBridgeReady = true;
        }
        return 'READY';
      } catch (e) {
        return 'BOOTSTRAP_ERROR';
      }
    })();""".trimIndent()
}

private fun isNihongoOrigin(uri: Uri): Boolean {
    if (!uri.scheme.equals("https", ignoreCase = true)) return false
    val host = uri.host?.lowercase() ?: return false
    return host == "www.vixnihon.web.id" || host == "vixnihon.web.id"
}

private fun openExternal(context: Context, uri: Uri): Boolean {
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        true
    } catch (_: Exception) {
        true
    }
}

private const val BASE_URL = "https://www.vixnihon.web.id/"
