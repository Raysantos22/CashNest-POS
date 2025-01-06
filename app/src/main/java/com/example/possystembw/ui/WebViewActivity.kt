//package com.example.possystembw.ui
//
//import android.os.Bundle
//import android.webkit.CookieManager
//import android.webkit.WebView
//import androidx.appcompat.app.AppCompatActivity
//import com.example.possystembw.R
//
//class WebViewActivity : AppCompatActivity() {
//    private lateinit var webView: WebView
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_webview)
//
//        webView = findViewById(R.id.webView)
//        setupWebView()
//
//        // Load the dashboard or any other page
//        showWebContent(webView, "https://eljin.org/dashboard")
//    }
//    fun showWebContent(webView: WebView, url: String) {
//        // Load stored cookies
//        val cookies = SessionManager.getWebSessionCookies()
//        if (cookies != null) {
//            CookieManager.getInstance().setCookie("https://eljin.org", cookies)
//        }
//
//        webView.loadUrl(url)
//    }
//    private fun setupWebView() {
//        webView.settings.apply {
//            javaScriptEnabled = true
//            domStorageEnabled = true
//            databaseEnabled = true
//        }
//    }
//}