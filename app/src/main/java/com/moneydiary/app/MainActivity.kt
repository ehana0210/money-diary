package com.moneydiary.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var auth: FirebaseAuth

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        webView = findViewById(R.id.webView)
        configureWebView()
        webView.addJavascriptInterface(AuthBridge(), "AndroidBridge")
        webView.loadUrl("file:///android_asset/index.html")

        // 앱 시작과 동시에 익명 로그인을 미리 시작해 둔다.
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    private fun configureWebView() {
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            // file:// 페이지에서 원격 API(fetch) 호출을 허용한다.
            allowUniversalAccessFromFileURLs = true
            cacheMode = WebSettings.LOAD_DEFAULT
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            displayZoomControls = false
        }
    }

    /** WebView 의 JS 가 호출하는 토큰/설정 브리지. */
    private inner class AuthBridge {

        @JavascriptInterface
        fun getApiBase(): String = API_BASE

        /**
         * Firebase 익명 인증으로 ID 토큰을 발급(필요 시 갱신)해 JS 로 전달한다.
         * JS 의 window.__onAuthToken(token) 으로 콜백한다.
         */
        @JavascriptInterface
        fun requestToken(forceRefresh: Boolean) {
            val user = auth.currentUser
            if (user != null) {
                user.getIdToken(forceRefresh)
                    .addOnSuccessListener { result -> pushToken(result.token) }
                    .addOnFailureListener { pushToken(null) }
            } else {
                auth.signInAnonymously()
                    .addOnSuccessListener { authResult ->
                        authResult.user?.getIdToken(true)
                            ?.addOnSuccessListener { result -> pushToken(result.token) }
                            ?.addOnFailureListener { pushToken(null) }
                            ?: pushToken(null)
                    }
                    .addOnFailureListener { pushToken(null) }
            }
        }
    }

    private fun pushToken(token: String?) {
        val arg = if (token == null) "null" else JSONObject.quote(token)
        runOnUiThread {
            webView.evaluateJavascript(
                "window.__onAuthToken && window.__onAuthToken($arg);",
                null
            )
        }
    }

    companion object {
        private const val API_BASE = "https://money-diary-api-223320053383.asia-northeast3.run.app"
    }
}
