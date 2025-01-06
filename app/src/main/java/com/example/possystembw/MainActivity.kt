package com.example.possystembw

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.helper.widget.MotionEffect.TAG
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.adapter.WindowAdapter
import com.example.possystembw.database.Window
import com.example.possystembw.ui.ViewModel.WindowViewModel
import com.example.possystembw.ui.Window1
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.possystembw.DAO.NumberSequenceRemoteDao
import com.example.possystembw.DAO.TransactionRecordRequest
import com.example.possystembw.DAO.TransactionSummaryRequest
import com.example.possystembw.DAO.TransactionSyncRequest
import com.example.possystembw.adapter.WindowTableAdapter
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.data.NumberSequenceRemoteRepository
import com.example.possystembw.data.TransactionRepository
import com.example.possystembw.data.WindowTableRepository
import com.example.possystembw.database.TransactionRecord
import com.example.possystembw.database.TransactionSummary
import com.example.possystembw.database.WindowTable
import com.example.possystembw.ui.LoginActivity
import com.example.possystembw.ui.SessionManager
import com.example.possystembw.ui.ViewModel.OrderWebViewActivity
import com.example.possystembw.ui.ViewModel.TransactionSyncService
import com.example.possystembw.ui.ViewModel.TransactionViewModel
import com.example.possystembw.ui.ViewModel.WindowTableViewModel
import com.example.possystembw.ui.ViewModel.WindowTableViewModelFactory
import com.example.possystembw.ui.ViewModel.WindowViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {
    private lateinit var windowViewModel: WindowViewModel
    private lateinit var windowTableViewModel: WindowTableViewModel
    private lateinit var windowAdapter: WindowAdapter
    private lateinit var windowTableAdapter: WindowTableAdapter
    private lateinit var refreshJob: Job
    private lateinit var refreshButton: Button
    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var toggleViewFab: FloatingActionButton

    private lateinit var sidebarLayout: ConstraintLayout
    private lateinit var toggleButton: ImageButton
    private lateinit var buttonContainer: LinearLayout
    private var isSidebarExpanded = true
    private lateinit var webView: WebView
    private lateinit var mainRecyclerViewContent: ConstraintLayout

    private lateinit var windowRecyclerView: RecyclerView
    private lateinit var windowTableRecyclerView: RecyclerView
    private lateinit var numberSequenceRemoteDao: NumberSequenceRemoteDao
    private lateinit var numberSequenceRemoteRepository: NumberSequenceRemoteRepository
    private lateinit var loadingDialog: AlertDialog
    private lateinit var transactionViewModel: TransactionViewModel // Add this

    private lateinit var webViewScrollContainer: NestedScrollView
    private lateinit var imageView1: ImageView


    private lateinit var webViewContainer: RelativeLayout

    private var transactionSyncService: TransactionSyncService? = null
    private lateinit var webViewLoadingOverlay: FrameLayout
    private lateinit var webViewLoadingText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize ViewModels and Adapters
        val windowFactory = WindowViewModelFactory(application)
        windowViewModel = ViewModelProvider(this, windowFactory).get(WindowViewModel::class.java)

        val windowTableDao = AppDatabase.getDatabase(application).windowTableDao()
        val windowTableApi = RetrofitClient.windowTableApi
        val windowTableRepository = WindowTableRepository(windowTableDao, windowTableApi)
        val windowTableFactory = WindowTableViewModelFactory(windowTableRepository)
        windowTableViewModel =
            ViewModelProvider(this, windowTableFactory).get(WindowTableViewModel::class.java)

        windowAdapter = WindowAdapter { window -> openWindow(window) }
        windowTableAdapter = WindowTableAdapter { windowTable -> showAlignedWindows(windowTable) }

        val database = AppDatabase.getDatabase(application)
        numberSequenceRemoteDao = AppDatabase.getDatabase(application).numberSequenceRemoteDao()
        val numberSequenceApi = RetrofitClient.numberSequenceApi
        numberSequenceRemoteRepository =
            NumberSequenceRemoteRepository(numberSequenceApi, numberSequenceRemoteDao)


        val transactionDao = AppDatabase.getDatabase(application).transactionDao()
//        val numberSequenceApi = RetrofitClient.numberSequenceApi
        val numberSequenceRemoteDao = AppDatabase.getDatabase(application).numberSequenceRemoteDao()
        val numberSequenceRemoteRepository =
            NumberSequenceRemoteRepository(numberSequenceApi, numberSequenceRemoteDao)

        val factory = TransactionViewModel.TransactionViewModelFactory(
            TransactionRepository(transactionDao, numberSequenceRemoteRepository),
            numberSequenceRemoteRepository
        )
        transactionViewModel =
            ViewModelProvider(this, factory).get(TransactionViewModel::class.java)
        // Initialize loading dialog
        initLoadingDialog()
        // Initialize all views
        initializeViews()

        // Setup UI components
        setupRefreshButton()
        setupSwipeRefreshLayout()
        setupWindowRecyclerView()
        setupWindowTableRecyclerView()
        initializeSidebarComponents()
        setupSidebar()
        setupWebView()
        setupToggleViewFab()
        webView = findViewById(R.id.webView)
        // Setup observers
        setupObservers()

        // Start periodic refresh
        startPeriodicRefresh()

        val repository = TransactionRepository(
            database.transactionDao(),
            numberSequenceRemoteRepository
        )
        transactionSyncService = TransactionSyncService(repository)

        // Start sync service
        transactionSyncService?.startSyncService(lifecycleScope)

        initializeWebViewComponents()
        setupWebView()

        handleIncomingIntent(intent)

    }
    private fun initializeWebViewComponents() {
        webViewLoadingOverlay = findViewById(R.id.webViewLoadingOverlay)
        webViewLoadingText = findViewById(R.id.webViewLoadingText)

        // Prevent touch events from passing through the overlay
        webViewLoadingOverlay.setOnTouchListener { _, _ -> true }
    }

    fun initializeViews() {
        webView = findViewById(R.id.webView)
        mainRecyclerViewContent = findViewById(R.id.mainRecyclerViewContent)
        toggleViewFab = findViewById(R.id.toggleViewFab)
        refreshButton = findViewById(R.id.refreshButton)
        refreshLayout = findViewById(R.id.swipeRefreshLayout)
        windowRecyclerView = findViewById(R.id.windowRecyclerView)
        windowTableRecyclerView = findViewById(R.id.windowTableRecyclerView)
        sidebarLayout = findViewById(R.id.sidebarLayout)
        toggleButton = findViewById(R.id.toggleButton)
        buttonContainer = findViewById(R.id.buttonContainer)
        imageView1 = findViewById(R.id.imageView1)

        webViewContainer = findViewById(R.id.webViewContainer)


    }
    private fun handleIncomingIntent(intent: Intent?) {
        intent?.getStringExtra("web_url")?.let { url ->
            // Show WebView content
            webViewContainer.visibility = View.VISIBLE
            refreshLayout.visibility = View.GONE
            windowRecyclerView.visibility = View.GONE
            windowTableRecyclerView.visibility = View.GONE
            imageView1.visibility = View.GONE
            toggleViewFab.setImageResource(R.drawable.ic_grid)

            // Load the URL
            loadWebContent(url)
        }
    }

    // Also add this to handle new intents when activity is already running
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }
    private fun setupToggleViewFab() {
        toggleViewFab.setOnClickListener {
            toggleView()
        }
    }

    private fun toggleView() {
        if (webView.visibility == View.VISIBLE) {
            // Switch to main content
            webViewContainer.visibility = View.GONE
            refreshLayout.visibility = View.VISIBLE
            windowRecyclerView.visibility = View.VISIBLE
            windowTableRecyclerView.visibility = View.VISIBLE
            imageView1.visibility = View.VISIBLE
            toggleViewFab.setImageResource(R.drawable.ic_web)
        } else {
            // Switch to WebView
            webViewContainer.visibility = View.VISIBLE
            webView.visibility = View.VISIBLE
            refreshLayout.visibility = View.GONE
            windowRecyclerView.visibility = View.GONE
            windowTableRecyclerView.visibility = View.GONE
            imageView1.visibility = View.GONE
            toggleViewFab.setImageResource(R.drawable.ic_grid)
        }
    }
    private fun initializeSidebarComponents() {
        sidebarLayout = findViewById(R.id.sidebarLayout)
        toggleButton = findViewById(R.id.toggleButton)
        buttonContainer = findViewById(R.id.buttonContainer)
    }

    private fun setupSidebar() {
        toggleButton.setOnClickListener {
            if (isSidebarExpanded) {
                collapseSidebar()
            } else {
                expandSidebar()
            }
        }
        setupSidebarButtons()
    }

    private fun setupSidebarButtons() {
        findViewById<ImageButton>(R.id.button2).setOnClickListener {
            showToast("DASHBOARD")
            // Load Dashboard
            loadWebContent("https://eljin.org/dashboard")
        }

        findViewById<ImageButton>(R.id.button3).setOnClickListener {
            showToast("ORDERING")
            // Load Shopping Cart
            loadWebContent("https://eljin.org/order")
        }

        findViewById<ImageButton>(R.id.button4).setOnClickListener {
            showToast("STOCK COUNTING")

            loadWebContent("https://eljin.org/StockCounting")
        }
        findViewById<ImageButton>(R.id.button5).setOnClickListener {
            showToast("RECEIVING")

            loadWebContent("https://eljin.org/Received")
        }
        findViewById<ImageButton>(R.id.button6).setOnClickListener {
            showToast("REPORTS")

            loadWebContent("https://eljin.org/reports")
        }

        findViewById<ImageButton>(R.id.waste).setOnClickListener {
            showToast("WASTE")
            loadWebContent("https://eljin.org/waste")
        }
        findViewById<ImageButton>(R.id.partycakes).setOnClickListener {
            showToast("PARTYCAKES")
            loadWebContent("https://eljin.org/partycakes")
        }

        findViewById<ImageButton>(R.id.customer).setOnClickListener {
            showToast("CUSTOMER")
            loadWebContent("https://eljin.org/customers")
        }

        findViewById<ImageButton>(R.id.button7).setOnClickListener {
            showToast("POS SYSTEM")
            // Start MainActivity when Cashier button is clicked
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


        findViewById<ImageButton>(R.id.button8).setOnClickListener {
            logout()
        }
    }

    private fun showWebContent(url: String) {
        // Ensure cookies are loaded
        val cookies = SessionManager.getWebSessionCookies()
        if (cookies != null) {
            CookieManager.getInstance().setCookie("https://eljin.org", cookies)
        }

        // Make WebView visible if it's not already
        if (webView.visibility != View.VISIBLE) {
            mainRecyclerViewContent.visibility = View.GONE
            sidebarLayout.visibility = View.VISIBLE  // Keep sidebar visible
            webView.visibility = View.VISIBLE
            toggleViewFab.setImageResource(R.drawable.ic_grid)
        }

        // Load the URL
        webView.loadUrl(url)
    }

    private fun collapseSidebar() {
        val collapse = ValueAnimator.ofInt(sidebarLayout.width, dpToPx(24))
        collapse.duration = 200
        collapse.interpolator = AccelerateDecelerateInterpolator()
        collapse.addUpdateListener { animator ->
            val value = animator.animatedValue as Int
            val params = sidebarLayout.layoutParams
            params.width = value
            sidebarLayout.layoutParams = params
            updateContentMargins(value)
        }
        collapse.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                buttonContainer.animate().alpha(0f).duration = 100
                toggleButton.animate().rotation(180f).duration = 200
                findViewById<TextView>(R.id.ecposTitle).animate().alpha(0f).duration = 100
                toggleButton.layoutParams =
                    (toggleButton.layoutParams as ConstraintLayout.LayoutParams).apply {
                        marginStart = dpToPx(8)
                    }
            }

            override fun onAnimationEnd(animation: Animator) {
                isSidebarExpanded = false
                buttonContainer.visibility = View.GONE
            }
        })
        collapse.start()
    }

    private fun expandSidebar() {
        val expand = ValueAnimator.ofInt(sidebarLayout.width, dpToPx(56))
        expand.duration = 200
        expand.interpolator = AccelerateDecelerateInterpolator()
        expand.addUpdateListener { animator ->
            val value = animator.animatedValue as Int
            val params = sidebarLayout.layoutParams
            params.width = value
            sidebarLayout.layoutParams = params
            updateContentMargins(value)
        }
        expand.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                buttonContainer.visibility = View.VISIBLE
                buttonContainer.alpha = 0f
                toggleButton.animate().rotation(0f).duration = 200
                findViewById<TextView>(R.id.ecposTitle).animate().alpha(1f).duration = 200
                toggleButton.layoutParams =
                    (toggleButton.layoutParams as ConstraintLayout.LayoutParams).apply {
                        marginStart = dpToPx(40)
                    }
            }

            override fun onAnimationEnd(animation: Animator) {
                buttonContainer.animate().alpha(1f).duration = 100
                isSidebarExpanded = true
            }
        })
        expand.start()
    }

    private fun updateContentMargins(sidebarWidth: Int) {
        findViewById<TextView>(R.id.textView3).updateLayoutParams<ConstraintLayout.LayoutParams> {
            marginStart = sidebarWidth + dpToPx(10)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }


    private fun setupRefreshButton() {
        refreshButton = findViewById(R.id.refreshButton)
        refreshButton.setOnClickListener {
            performManualRefresh()
        }
    }

    private fun setupSwipeRefreshLayout() {
        refreshLayout = findViewById(R.id.swipeRefreshLayout)
        refreshLayout.setOnRefreshListener {
            performManualRefresh()
        }
    }

    private fun performManualRefresh() {
        lifecycleScope.launch {
            try {
                refreshButton.isEnabled = false
                refreshLayout.isRefreshing = true

                // Attempt to refresh from network
                try {
                    windowViewModel.refreshWindows()
                    windowTableViewModel.refreshWindowTables()
                    showToast("Data refreshed successfully")
                } catch (e: Exception) {
                    Log.w("MainActivity", "Network refresh failed, loading from local database", e)
                    // Load from local database
                    windowViewModel.loadFromLocalDatabase()
                    windowTableViewModel.loadFromLocalDatabase()
                    showToast("Offline mode: Loaded from local database")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error during manual refresh", e)
                showToast("Error refreshing data: ${e.message}")
            } finally {
                refreshButton.isEnabled = true
                refreshLayout.isRefreshing = false
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            windowViewModel.allWindows.collect { windows ->
                Log.d(
                    "MainActivity",
                    "Received windows: ${windows.joinToString { it.description }}"
                )
                windowAdapter.submitList(windows)
            }
        }

        lifecycleScope.launch {
            windowTableViewModel.allWindowTables.collect { windowTables ->
                Log.d(
                    "MainActivity",
                    "Received window tables: ${windowTables.joinToString { it.description }}"
                )
                windowTableAdapter.submitList(windowTables)
            }
        }

        lifecycleScope.launch {
            windowViewModel.alignedWindows.collect { alignedWindows ->
                if (alignedWindows.isNotEmpty()) {
                    Log.d(
                        "MainActivity",
                        "Received aligned windows: ${alignedWindows.joinToString { it.description }}"
                    )
                    windowAdapter.submitList(alignedWindows)
                }
            }
        }

        windowViewModel.errorState.observe(this) { error ->
            error?.let {
                showToast(it)
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun setupWindowRecyclerView() {
        val windowRecyclerView: RecyclerView = findViewById(R.id.windowRecyclerView)
        windowRecyclerView.adapter = windowAdapter

        // Calculate span count based on screen width
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val spanCount = (screenWidthDp / 400).toInt().coerceAtLeast(2) // minimum 2 columns

        windowRecyclerView.layoutManager = GridLayoutManager(this, spanCount)
    }

    private fun setupWindowTableRecyclerView() {
        Log.d("MainActivity", "Setting up window table RecyclerView")
        val windowTableRecyclerView: RecyclerView = findViewById(R.id.windowTableRecyclerView)
        windowTableRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false) // Horizontal layout
        windowTableRecyclerView.adapter = windowTableAdapter
        Log.d("MainActivity", "Window table RecyclerView set up successfully")
    }

    private fun startPeriodicRefresh() {
        Log.d("MainActivity", "Starting periodic refresh")
        refreshJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    Log.d("MainActivity", "Refreshing windows and window tables")
                    windowViewModel.refreshWindows()
                    windowTableViewModel.refreshWindowTables()
                    Log.d("MainActivity", "Windows and window tables refreshed successfully")
                    delay(60000) // Refresh every 60 seconds
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error during periodic refresh", e)
                    Toast.makeText(
                        this@MainActivity,
                        "Error refreshing data: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

//    private fun loadWebContent(url: String) {
//        webViewContainer.visibility = View.VISIBLE
//        refreshLayout.visibility = View.GONE
//        windowRecyclerView.visibility = View.GONE
//        windowTableRecyclerView.visibility = View.GONE
//        imageView1.visibility = View.GONE
//
//        // Restore cookies
//        val cookies = SessionManager.getWebSessionCookies()
//        if (cookies != null) {
//            CookieManager.getInstance().apply {
//                setCookie("https://eljin.org", cookies)
//                flush()
//            }
//        }
//
//        webView.loadUrl(url)
//    }
//private fun loadWebContent(url: String) {
//    // Show WebView and its container
//    webView.visibility = View.VISIBLE
//    webViewContainer.visibility = View.VISIBLE
//
//    // Hide main activity content
//    refreshLayout.visibility = View.GONE
//    windowRecyclerView.visibility = View.GONE
//    windowTableRecyclerView.visibility = View.GONE
//    imageView1.visibility = View.GONE
//
//    // Restore cookies
//    val cookies = SessionManager.getWebSessionCookies()
//    if (cookies != null) {
//        CookieManager.getInstance().apply {
//            setCookie("https://eljin.org", cookies)
//            flush()
//        }
//    }
//
//    // Load the URL
//    webView.loadUrl(url)
//}
private fun loadWebContent(url: String) {
    webView.visibility = View.VISIBLE
    webViewContainer.visibility = View.VISIBLE
    showWebViewLoading("Loading page...")

    // Hide main activity content
    refreshLayout.visibility = View.GONE
    windowRecyclerView.visibility = View.GONE
    windowTableRecyclerView.visibility = View.GONE
    imageView1.visibility = View.GONE

    // Restore cookies
    val cookies = SessionManager.getWebSessionCookies()
    if (cookies != null) {
        CookieManager.getInstance().apply {
            setCookie("https://eljin.org", cookies)
            flush()
        }
    }

    // Load the URL
    webView.loadUrl(url)
}

    // Update your onBackPressed to handle loading state



    override fun onResume() {
        super.onResume()
        webView.onResume()

        // Check session validity and refresh
        if (!SessionManager.isSessionValid()) {
            // Redirect to login if session is invalid
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        // Refresh session timestamp on activity resume
        SessionManager.refreshSession()
    }
    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        transactionSyncService?.stopSyncService()
        webView.destroy()
        super.onDestroy()
    }


    private fun retryWithSession() {
        // Get current user credentials
        val currentUser = SessionManager.getCurrentUser()
        if (currentUser != null) {
            // Re-login using stored credentials
            val js = """
            (async function() {
                try {
                    const response = await fetch('/sanctum/csrf-cookie');
                    const token = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content');
                    
                    const loginResponse = await fetch('/login', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'X-CSRF-TOKEN': token,
                            'Accept': 'application/json'
                        },
                        body: JSON.stringify({
                            email: '${currentUser.email}',
                            password: '${currentUser.password}'
                        }),
                        credentials: 'include'
                    });

                    if (loginResponse.ok) {
                        window.location.reload();
                    }
                } catch(e) {
                    console.error('Session restore failed:', e);
                }
            })();
        """.trimIndent()

            webView.evaluateJavascript(js, null)
        } else {
            // Handle case where session can't be restored
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
            logout()
        }
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            defaultTextEncodingName = "utf-8"
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL

            // Enable viewport meta tag
            useWideViewPort = true
            loadWithOverviewMode = true

            // Enable better content handling
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            setEnableSmoothTransition(true)

            // Cache settings for better performance
            cacheMode = WebSettings.LOAD_DEFAULT
            databaseEnabled = true

            // Additional features
            setGeolocationEnabled(true)
            javaScriptCanOpenWindowsAutomatically = true
        }

        // Enable hardware acceleration
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                showWebViewLoading("Loading page...")
                injectScrollFix()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                hideWebViewLoading()
                if (url?.contains("/login") == true) {
                    handleLoginPage()
                }
                injectScrollFix()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                hideWebViewLoading()
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Error loading page: ${error?.description}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // Add this to handle SSL errors if needed
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                hideWebViewLoading()
                super.onReceivedSslError(view, handler, error)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress < 100) {
                    showWebViewLoading("Loading... $newProgress%")
                } else {
                    hideWebViewLoading()
                }
            }

            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                Log.d("WebView", "${message.message()} -- From line ${message.lineNumber()} of ${message.sourceId()}")
                return true
            }
        }
    }
    private fun showWebViewLoading(message: String = "Loading...") {
        runOnUiThread {
            webViewLoadingText.text = message
            if (webViewLoadingOverlay.visibility != View.VISIBLE) {
                webViewLoadingOverlay.alpha = 0f
                webViewLoadingOverlay.visibility = View.VISIBLE
                webViewLoadingOverlay.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
        }
    }

    private fun hideWebViewLoading() {
        runOnUiThread {
            webViewLoadingOverlay.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    webViewLoadingOverlay.visibility = View.GONE
                }
                .start()
        }
    }

    private fun injectScrollFix() {
        val js = """
            javascript:(function() {
                document.documentElement.style.webkitUserSelect = 'none';
                document.documentElement.style.webkitTouchCallout = 'none';
                document.documentElement.style.overflowX = 'auto';
                document.documentElement.style.overflowY = 'auto';
                document.body.style.overflowX = 'auto';
                document.body.style.overflowY = 'auto';
                document.documentElement.style.height = 'auto';
                document.body.style.height = 'auto';
                
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta) {
                    meta = document.createElement('meta');
                    meta.name = 'viewport';
                    document.head.appendChild(meta);
                }
                meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=yes';
            })()
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun showLoading() {
        // Add loading indicator if needed
        // For example, a ProgressBar in the WebView container
    }

    private fun hideLoading() {
        // Hide loading indicator
    }

    private fun handleLoginPage() {
        val currentUser = SessionManager.getCurrentUser()
        if (currentUser != null) {
            val js = """
            (async function() {
                try {
                    // Get CSRF token
                    const response = await fetch('/sanctum/csrf-cookie');
                    const token = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content');
                    
                    // Perform login
                    const loginResponse = await fetch('/login', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'Accept': 'application/json',
                            'X-CSRF-TOKEN': token
                        },
                        credentials: 'include',
                        body: JSON.stringify({
                            email: '${currentUser.email}',
                            password: '${currentUser.password}',
                            remember: true
                        })
                    });

                    if (loginResponse.ok || loginResponse.redirected) {
                        // Store new cookies
                        const cookies = document.cookie;
                        Android.onSessionRestored(cookies);
                        // Redirect back to the original page
                        window.location.reload();
                    }
                } catch(e) {
                    console.error('Login failed:', e);
                }
            })();
        """.trimIndent()

            webView.evaluateJavascript(js, null)
        }
    }


    private fun openWindow(window: Window) {
        Log.d("MainActivity", "Opening window with ID: ${window.id}")
        val intent = Intent(this, Window1::class.java).apply {
            putExtra("WINDOW_ID", window.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

//    override fun onBackPressed() {
//        when {
//            webViewScrollContainer.visibility == View.VISIBLE && webView.canGoBack() -> webView.goBack()
//            webViewScrollContainer.visibility == View.VISIBLE -> toggleView()
//            else -> super.onBackPressed()
//        }
//    }
//override fun onBackPressed() {
//    when {
//        // First check if WebView is visible and can go back
//        webView.visibility == View.VISIBLE && webView.canGoBack() -> {
//            webView.goBack()
//        }
//        // If WebView is visible but can't go back, return to main activity view
//        webView.visibility == View.VISIBLE -> {
//            // Hide WebView container
//            webViewContainer.visibility = View.GONE
//
//            // Show main activity content
//            refreshLayout.visibility = View.VISIBLE
//            windowRecyclerView.visibility = View.VISIBLE
//            windowTableRecyclerView.visibility = View.VISIBLE
//            imageView1.visibility = View.VISIBLE
//
//            // Update FAB icon if needed
//            toggleViewFab.setImageResource(R.drawable.ic_web)
//        }
//        // If WebView is not visible, handle normal back button behavior
//        else -> {
//            super.onBackPressed()
//        }
//    }
//}
override fun onBackPressed() {
    when {
        webViewLoadingOverlay.visibility == View.VISIBLE -> {
            // Do nothing while loading
            return
        }
        webView.visibility == View.VISIBLE && webView.canGoBack() -> {
            webView.goBack()
        }
        webView.visibility == View.VISIBLE -> {
            webViewContainer.visibility = View.GONE
            refreshLayout.visibility = View.VISIBLE
            windowRecyclerView.visibility = View.VISIBLE
            windowTableRecyclerView.visibility = View.VISIBLE
            imageView1.visibility = View.VISIBLE
            toggleViewFab.setImageResource(R.drawable.ic_web)
        }
        else -> {
            super.onBackPressed()
        }
    }
}


    // Add this method to handle screen rotation or configuration changes
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (webView.visibility == View.VISIBLE) {
            webView.requestLayout()
        }
    }


    private fun showAlignedWindows(windowTable: WindowTable) {
        Log.d("MainActivity", "Aligning windows with table ID: ${windowTable.id}")
        windowViewModel.alignWindowsWithTable(windowTable.id)
    }

    private suspend fun updateAndDeleteNumberSequence(): Boolean {
        val currentUser = SessionManager.getCurrentUser() ?: return false
        val numberSequence = numberSequenceRemoteDao.getNumberSequenceByStoreId(currentUser.storeid)

        return if (numberSequence != null) {
            try {
                // First try to update remote sequence
                val result = numberSequenceRemoteRepository.updateRemoteNextRec(
                    currentUser.storeid,
                    numberSequence.nextRec
                )

                if (result.isSuccess) {
                    try {
                        // If update successful, delete the local sequence
                        numberSequenceRemoteDao.deleteNumberSequenceByStoreId(currentUser.storeid)
                        Log.d(
                            "MainActivity",
                            "Successfully deleted number sequence for store: ${currentUser.storeid}"
                        )
                        true
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Failed to delete number sequence", e)
                        false
                    }
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to update number sequence", e)
                false
            }
        } else {
            false
        }
    }



    private suspend fun clearAllTransactions() {
        withContext(Dispatchers.IO) {
            try {
                val transactionDao = AppDatabase.getDatabase(application).transactionDao()

                // Delete transaction records first
                transactionDao.deleteAllTransactions()
                Log.d("Logout", "Successfully deleted all transaction records")

                // Then delete transaction summaries
                transactionDao.deleteAllTransactionSummaries()
                Log.d("Logout", "Successfully deleted all transaction summaries")

            } catch (e: Exception) {
                Log.e("Logout", "Error clearing transactions", e)
                throw e
            }
        }
    }




    private fun logout() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    loadingDialog.show()
                }

                val currentUser = SessionManager.getCurrentUser()
                if (currentUser != null) {
                    val transactionDao = AppDatabase.getDatabase(application).transactionDao()
                    // First sync any unsynced transactions
                    val syncSuccess = syncAllTransactions()

                    if (syncSuccess) {
                        // Handle number sequence
                        val numberSequence = numberSequenceRemoteDao.getNumberSequenceByStoreId(currentUser.storeid)
                        if (numberSequence != null) {
                            val updateResult = numberSequenceRemoteRepository.updateRemoteNextRec(
                                currentUser.storeid,
                                numberSequence.nextRec
                            )

                            if (updateResult.isSuccess) {
                                // Delete number sequence first
                                numberSequenceRemoteDao.deleteNumberSequenceByStoreId(currentUser.storeid)

                                // Then clear all transactions
                                withContext(Dispatchers.IO) {
                                    clearAllTransactions()
                                }

                                withContext(Dispatchers.Main) {
                                    loadingDialog.dismiss()
                                    proceedWithLogout()
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    loadingDialog.dismiss()
                                    showRetryDialog()
                                }
                            }
                        } else {
                            withContext(Dispatchers.IO) {
                                clearAllTransactions()
                            }

                            withContext(Dispatchers.Main) {
                                loadingDialog.dismiss()
                                proceedWithLogout()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            loadingDialog.dismiss()
                            showRetryDialog()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()
                        proceedWithLogout()
                    }
                }
            } catch (e: Exception) {
                Log.e("Logout", "Error during logout", e)
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    showRetryDialog()
                }
            }
        }
    }




    private fun endWebSession() {
        try {
            // Clear web session
            webView.evaluateJavascript(
                """
            (async function() {
                try {
                    // Attempt to logout from web session
                    await fetch('/logout', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'Accept': 'application/json'
                        },
                        credentials: 'include'
                    });
                } catch(e) {
                    console.error('Logout failed:', e);
                }
            })();
            """.trimIndent(),
                null
            )

            // Clear cookies
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()

            // Clear WebView
            webView.clearCache(true)
            webView.clearHistory()
            webView.clearFormData()
        } catch (e: Exception) {
            Log.e("Logout", "Error clearing web session", e)
        }
    }


    private suspend fun syncAllTransactions(): Boolean {
        return try {
            val transactionDao = AppDatabase.getDatabase(application).transactionDao()
            // Get only unsynced transactions
            val unsyncedTransactions = transactionDao.getUnsyncedTransactionSummaries()
            Log.d("Sync", "Found ${unsyncedTransactions.size} unsynced transactions")

            if (unsyncedTransactions.isEmpty()) {
                Log.d("Sync", "No unsynced transactions found")
                return true
            }

            var allSuccessful = true
            for (summary in unsyncedTransactions) {
                try {
                    // Get unsynced records for this transaction
                    val unsyncedRecords =
                        transactionDao.getUnsyncedTransactionRecords(summary.transactionId)
                    Log.d(
                        "Sync",
                        "Found ${unsyncedRecords.size} unsynced records for transaction ${summary.transactionId}"
                    )

                    if (unsyncedRecords.isNotEmpty()) {
                        val api = RetrofitClient.transactionApi
                        val syncRequest = TransactionSyncRequest(
                            transactionSummary = createTransactionSummaryRequest(summary),
                            transactionRecords = unsyncedRecords.map { record ->
                                createTransactionRecordRequest(record, summary)
                            }
                        )

                        val response = api.syncTransaction(syncRequest)

                        if (response.isSuccessful && response.body() != null) {
                            Log.d(
                                "Sync",
                                "Successfully synced transaction ${summary.transactionId}"
                            )
                            // Mark as synced only after successful sync
                            transactionDao.markTransactionSummaryAsSynced(summary.transactionId)
                            transactionDao.markTransactionRecordsAsSynced(summary.transactionId)
                        } else {
                            Log.e(
                                "Sync",
                                "Failed to sync transaction ${summary.transactionId}: ${response.message()}"
                            )
                            allSuccessful = false
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Sync", "Error syncing transaction ${summary.transactionId}", e)
                    allSuccessful = false
                }
            }

            allSuccessful
        } catch (e: Exception) {
            Log.e("Sync", "Error in sync process", e)
            false
        }
    }


    private fun forceLogout() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    loadingDialog.show()
                }

                // End web session even in force logout
                endWebSession()

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    SessionManager.clearCurrentUser()
                    refreshJob.cancel()

                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                Log.e("Logout", "Error during force logout", e)
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this@MainActivity,
                        "Error during force logout",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun proceedWithLogout() {
        SessionManager.clearCurrentUser()
        refreshJob.cancel()

        val intent = Intent(this@MainActivity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private suspend fun syncAllUnsyncedTransactions(): Boolean {
        return try {
            val transactionDao = AppDatabase.getDatabase(application).transactionDao()
            // Get only unsynced transactions
            val unsyncedTransactions = transactionDao.getUnsyncedTransactionSummaries()

            if (unsyncedTransactions.isEmpty()) {
                Log.d("Sync", "No unsynced transactions found")
                return true
            }

            var allSuccessful = true
            unsyncedTransactions.forEach { summary ->
                try {
                    // Get only unsynced records for this transaction
                    val unsyncedRecords =
                        transactionDao.getUnsyncedTransactionRecords(summary.transactionId)

                    if (unsyncedRecords.isNotEmpty()) {
                        val api = RetrofitClient.transactionApi
                        val syncRequest = TransactionSyncRequest(
                            transactionSummary = createTransactionSummaryRequest(summary),
                            transactionRecords = unsyncedRecords.map { record ->
                                createTransactionRecordRequest(record, summary)
                            }
                        )

                        val response = api.syncTransaction(syncRequest)

                        if (response.isSuccessful && response.body() != null) {
                            // Only mark as synced if API call was successful
                            transactionDao.markTransactionSummaryAsSynced(summary.transactionId)
                            transactionDao.markTransactionRecordsAsSynced(summary.transactionId)
                            Log.d(
                                "Sync",
                                "Successfully synced transaction ${summary.transactionId}"
                            )
                        } else {
                            Log.e(
                                "Sync",
                                "Failed to sync transaction ${summary.transactionId}: ${response.message()}"
                            )
                            allSuccessful = false
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Sync", "Error syncing transaction ${summary.transactionId}", e)
                    allSuccessful = false
                }
            }

            allSuccessful
        } catch (e: Exception) {
            Log.e("Sync", "Error in sync process", e)
            false
        }
    }

    private fun showLogoutErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Logout Error")
            .setMessage(message)
            .setPositiveButton("Retry") { dialog, _ ->
                dialog.dismiss()
                logout()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }


    private fun showRetryDialog() {
        lifecycleScope.launch {
            val transactionDao = AppDatabase.getDatabase(application).transactionDao()
            val unsyncedCount = transactionDao.getUnsyncedTransactionSummaries().size
            val numberSequence = SessionManager.getCurrentUser()?.let { user ->
                numberSequenceRemoteDao.getNumberSequenceByStoreId(user.storeid)
            }

            withContext(Dispatchers.Main) {
                val message = buildString {
                    if (unsyncedCount > 0) {
                        append("You have $unsyncedCount unsynced transactions.\n")
                    }
                    if (numberSequence != null) {
                        append("Number sequence needs to be updated.\n")
                    }
                    append("\nWould you like to retry?")
                }

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Sync Required")
                    .setMessage(message)
                    .setPositiveButton("Retry") { dialog, _ ->
                        dialog.dismiss()
                        logout()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }
    private fun initLoadingDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_loading, null)
        builder.setView(dialogView)
        builder.setCancelable(false)
        loadingDialog = builder.create()
    }

    private fun createTransactionRecordRequest(record: TransactionRecord, summary: TransactionSummary): TransactionRecordRequest {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val currentDate = dateFormat.format(Date())  // Default to current date if null

        return TransactionRecordRequest(
            transactionid = record.transactionId,
            linenum = record.lineNum?.toString() ?: "1",
            receiptid = record.receiptId ?: "",
            storeKey = record.storeKey,
            storeSequence = record.storeSequence,
            itemid = record.itemId ?: "",
            itemname = record.name,
            itemgroup = record.itemGroup ?: "",
            price = formatDecimal(record.price),
            netprice = formatDecimal(record.netPrice),
            qty = record.quantity.toString(),
            discamount = formatDecimal(record.discountAmount),
            costamount = formatDecimal(record.costAmount),
            netamount = formatDecimal(record.netAmount),
            grossamount = formatDecimal(record.grossAmount),
            custaccount = summary.customerAccount,
            store = summary.store,
            priceoverride = record.priceOverride?.toInt() ?: 0,
            paymentmethod = summary.paymentMethod,
            staff = record.staff ?: "Unknown Staff",
            linedscamount = formatDecimal(record.lineDiscountAmount ?: 0.0),
            linediscpct = formatDecimal(record.lineDiscountPercentage ?: 0.0),
            custdiscamount = formatDecimal(record.customerDiscountAmount ?: 0.0),
            unit = record.unit ?: "PCS",
            unitqty = formatDecimal(record.unitQuantity ?: record.quantity.toDouble()),
            unitprice = formatDecimal(record.unitPrice ?: record.price),
            taxamount = formatDecimal(record.taxAmount),
            createddate = record.createdDate?.let { dateFormat.format(it) } ?: currentDate,
            remarks = record.remarks ?: "",
            taxinclinprice = formatDecimal(record.taxIncludedInPrice),
            description = record.description ?: "",
            discofferid = record.discountOfferId?.takeIf { it.isNotBlank() } ?: "",
            inventbatchid = "",
            inventbatchexpdate = "",
            giftcard = "",
            returntransactionid = "",
            returnqty = "0",
            creditmemonumber = "",
            returnlineid = "0",
            priceunit = "0",
            netamountnotincltax = formatDecimal(record.netAmountNotIncludingTax),
            storetaxgroup = "",
            currency = "PHP",
            taxexempt = "0"
        )
    }

    private fun createTransactionSummaryRequest(summary: TransactionSummary): TransactionSummaryRequest {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val currentDate = dateFormat.format(Date())  // Default to current date if null

        return TransactionSummaryRequest(
            transactionid = summary.transactionId,
            type = summary.type,
            receiptid = summary.receiptId,
            storeKey = summary.storeKey,
            storeSequence = summary.storeSequence,
            store = summary.store,
            staff = summary.staff,
            custaccount = summary.customerAccount,
            netamount = formatDecimal(summary.netAmount),
            costamount = formatDecimal(summary.costAmount),
            grossamount = formatDecimal(summary.grossAmount),
            partialpayment = formatDecimal(summary.partialPayment),
            transactionstatus = summary.transactionStatus,
            discamount = formatDecimal(summary.discountAmount),
            cashamount = formatDecimal(summary.totalAmountPaid),
            custdiscamount = formatDecimal(summary.customerDiscountAmount),
            totaldiscamount = formatDecimal(summary.totalDiscountAmount),
            numberofitems = summary.numberOfItems.toString(),
            refundreceiptid = "",
            refunddate = "",
            returnedby = "",
            currency = summary.currency,
            zreportid = "",
            createddate = summary.createdDate?.let { dateFormat.format(it) } ?: currentDate,
            priceoverride = summary.priceOverride.toInt(),
            comment = summary.comment ?: "",
            receiptemail = "",
            markupamount = "",
            markupdescription = "",
            taxinclinprice = formatDecimal(summary.taxIncludedInPrice),
            netamountnotincltax = formatDecimal(summary.vatableSales),
            window_number = summary.windowNumber,
            charge = formatDecimal(summary.charge),
            gcash = formatDecimal(summary.gCash),
            paymaya = formatDecimal(summary.payMaya),
            cash = formatDecimal(summary.cash),
            card = formatDecimal(summary.card),
            loyaltycard = formatDecimal(summary.loyaltyCard),
            foodpanda = formatDecimal(summary.foodpanda),
            grabfood = formatDecimal(summary.grabfood),
            representation = formatDecimal(summary.representation)
        )
    }

    // Helper function to format decimal values
    private fun formatDecimal(value: Double?): String {
        return try {
            String.format(Locale.US, "%.2f", value ?: 0.0)
        } catch (e: Exception) {
            "0.00"
        }
    }
}

//    private fun showSyncDialog() {
//        AlertDialog.Builder(this)
//            .setTitle("Sync Required")
//            .setMessage("Transactions need to be synchronized. Would you like to sync now?")
//            .setPositiveButton("Sync") { dialog, _ ->
//                dialog.dismiss()
//                syncAllTransactions()
//            }
//            .setNegativeButton("Force Logout") { dialog, _ ->
//                dialog.dismiss()
//                proceedWithLogout()
//            }
//            .show()
//    }
//
//    private fun showSyncFailedDialog() {
//        AlertDialog.Builder(this)
//            .setTitle("Sync Failed")
//            .setMessage("Some transactions failed to sync. Would you like to retry?")
//            .setPositiveButton("Retry") { dialog, _ ->
//                dialog.dismiss()
//                syncAllTransactions()
//            }
//            .setNegativeButton("Force Logout") { dialog, _ ->
//                dialog.dismiss()
//                proceedWithLogout()
//            }
//            .show()
//    }
//    override fun onDestroy() {
//        super.onDestroy()
//        Log.d("MainActivity", "onDestroy: Cancelling refresh job")
//        refreshJob.cancel()
//    }


//    private fun loadWebContent(url: String) {
//        // Show WebView container
//        webViewScrollContainer.visibility = View.VISIBLE
//        // Hide RecyclerViews
//        imageView1.visibility = View.GONE
//        windowRecyclerView.visibility = View.GONE
//        windowTableRecyclerView.visibility = View.GONE
//        refreshLayout.visibility = View.GONE
//
//        // Keep sidebar visible
//        sidebarLayout.visibility = View.VISIBLE
//
//        // Restore cookies
//        val cookies = SessionManager.getWebSessionCookies()
//        if (cookies != null) {
//            CookieManager.getInstance().apply {
//                setCookie("https://eljin.org", cookies)
//                flush()
//            }
//        }
//
//        // Configure WebView
//        webView.apply {
//            settings.apply {
//                javaScriptEnabled = true
//                domStorageEnabled = true
//                loadWithOverviewMode = true
//                useWideViewPort = true
//                setSupportZoom(true)
//                builtInZoomControls = true
//                displayZoomControls = false
//                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
//            }
//
//            webViewClient = object : WebViewClient() {
//                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
//                    super.onPageStarted(view, url, favicon)
//                    // Show loading if needed
//                }
//
//                override fun onPageFinished(view: WebView?, url: String?) {
//                    super.onPageFinished(view, url)
//                    if (url?.contains("/login") == true) {
//                        handleLoginPage()
//                    }
//                }
//
//                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
//                    request?.url?.let { uri ->
//                        if (uri.toString().contains("/login")) {
//                            handleLoginPage()
//                            return true
//                        }
//                    }
//                    return false
//                }
//            }
//        }
//
//        // Load the URL
//        webView.loadUrl(url)
//    }

//        webView.webViewClient = object : WebViewClient() {
//            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
//                url?.let { view?.loadUrl(it) }
//                return true
//            }

//            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
//                super.onPageStarted(view, url, favicon)
//                // Show loading indicator if needed
//            }
//
//            override fun onPageFinished(view: WebView?, url: String?) {
//                super.onPageFinished(view, url)
//                // Hide loading indicator if needed
//            }
//        }

//        webView.setOnKeyListener { _, keyCode, event ->
//            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == MotionEvent.ACTION_UP && webView.canGoBack()) {
//                webView.goBack()
//                return@setOnKeyListener true
//            }
//            false
//        }
//    }


//    private fun logout() {
//        lifecycleScope.launch {
//            try {
//                withContext(Dispatchers.Main) {
//                    loadingDialog.show()
//                }
//
//                val currentUser = SessionManager.getCurrentUser()
//                if (currentUser != null) {
//                    val transactionDao = AppDatabase.getDatabase(application).transactionDao()
//
//                    // Only get unsynced transactions
//                    val unsyncedTransactions = transactionDao.getUnsyncedTransactionSummaries()
//
//                    if (unsyncedTransactions.isNotEmpty()) {
//                        // Attempt to sync only unsynced transactions
//                        val syncSuccess = syncAllUnsyncedTransactions()
//
//                        if (syncSuccess) {
//                            // Update number sequence before clearing transactions
//                            val numberSequenceSuccess = updateAndDeleteNumberSequence()
//
//                            if (numberSequenceSuccess) {
//                                // Clear transactions after successful sync
//                                clearAllTransactions()
//
//                                // End web session
//                                endWebSession()
//
//                                withContext(Dispatchers.Main) {
//                                    loadingDialog.dismiss()
//                                    proceedWithLogout()
//                                }
//                            } else {
//                                withContext(Dispatchers.Main) {
//                                    loadingDialog.dismiss()
//                                    showRetryDialog()
//                                }
//                            }
//                        } else {
//                            withContext(Dispatchers.Main) {
//                                loadingDialog.dismiss()
//                                showRetryDialog()
//                            }
//                        }
//                    } else {
//                        // No unsynced transactions to sync
//                        endWebSession()
//                        clearAllTransactions()
//                        withContext(Dispatchers.Main) {
//                            loadingDialog.dismiss()
//                            proceedWithLogout()
//                        }
//                    }
//                } else {
//                    // No user logged in, just proceed with logout
//                    endWebSession()
//                    withContext(Dispatchers.Main) {
//                        loadingDialog.dismiss()
//                        proceedWithLogout()
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e("Logout", "Error during logout", e)
//                withContext(Dispatchers.Main) {
//                    loadingDialog.dismiss()
//                    showRetryDialog()
//                }
//            }
//        }
//    }
//    private fun loadWebContent(url: String) {
//        webViewContainer.visibility = View.VISIBLE
//        webViewScrollContainer.visibility = View.VISIBLE
//        mainRecyclerViewContent.visibility = View.VISIBLE
//        windowRecyclerView.visibility = View.GONE
//        windowTableRecyclerView.visibility = View.GONE
//        sidebarLayout.visibility = View.VISIBLE
//        refreshLayout.visibility = View.GONE
//        imageView1.visibility = View.GONE
//        webView.visibility = View.VISIBLE
//        refreshLayout.visibility = View.GONE
//        if (webViewScrollContainer.visibility == View.VISIBLE) {
//            // Switch to main content
//            webViewScrollContainer.visibility = View.GONE
//            refreshLayout.visibility = View.VISIBLE
//            windowRecyclerView.visibility = View.VISIBLE
//            windowTableRecyclerView.visibility = View.VISIBLE
//            toggleViewFab.setImageResource(R.drawable.ic_web)
//        } else {
//            // Switch to WebView
//            refreshLayout.visibility = View.GONE
//            windowRecyclerView.visibility = View.GONE
//            windowTableRecyclerView.visibility = View.GONE
//            webViewScrollContainer.visibility = View.VISIBLE
//            webView.loadUrl("https://eljin.org")
//            toggleViewFab.setImageResource(R.drawable.ic_grid)
//        }
//    private fun logout() {
//        lifecycleScope.launch {
//            try {
//                withContext(Dispatchers.Main) {
//                    loadingDialog.show()
//                }
//
//                val currentUser = SessionManager.getCurrentUser()
//                if (currentUser != null) {
//                    val transactionDao = AppDatabase.getDatabase(application).transactionDao()
//                    val unsyncedTransactions = transactionDao.getUnsyncedTransactionSummaries()
//
//                    if (unsyncedTransactions.isNotEmpty()) {
//                        // Attempt to sync all transactions
//                        val syncSuccess = syncAllTransactions()
//
//                        if (syncSuccess) {
//                            // Update number sequence before clearing transactions
//                            val numberSequenceSuccess = updateAndDeleteNumberSequence()
//
//                            if (numberSequenceSuccess) {
//                                // Clear transactions after successful sync
//                                clearAllTransactions()
//
//                                // End web session
//                                endWebSession()
//
//                                withContext(Dispatchers.Main) {
//                                    loadingDialog.dismiss()
//                                    proceedWithLogout()
//                                }
//                            } else {
//                                withContext(Dispatchers.Main) {
//                                    loadingDialog.dismiss()
//                                    showRetryDialog()
//                                }
//                            }
//                        } else {
//                            withContext(Dispatchers.Main) {
//                                loadingDialog.dismiss()
//                                showRetryDialog()
//                            }
//                        }
//                    } else {
//                        // No transactions to sync
//                        endWebSession()
//                        clearAllTransactions()
//                        withContext(Dispatchers.Main) {
//                            loadingDialog.dismiss()
//                            proceedWithLogout()
//                        }
//                    }
//                } else {
//                    // No user logged in, just proceed with logout
//                    endWebSession()
//                    withContext(Dispatchers.Main) {
//                        loadingDialog.dismiss()
//                        proceedWithLogout()
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e("Logout", "Error during logout", e)
//                withContext(Dispatchers.Main) {
//                    loadingDialog.dismiss()
//                    showRetryDialog()
//                }
//            }
//        }
//

// And in your logout function, call clearAllTransactions() right after successful sync:
//    private fun logout() {
//        lifecycleScope.launch {
//            try {
//                withContext(Dispatchers.Main) {
//                    loadingDialog.show()
//                }
//
//                val currentUser = SessionManager.getCurrentUser()
//                if (currentUser != null) {
//                    val transactionDao = AppDatabase.getDatabase(application).transactionDao()
//                    // First sync any unsynced transactions
//                    val syncSuccess = syncAllTransactions()
//
//                    if (syncSuccess) {
//                        // Handle number sequence
//                        val numberSequence = numberSequenceRemoteDao.getNumberSequenceByStoreId(currentUser.storeid)
//                        if (numberSequence != null) {
//                            val updateResult = numberSequenceRemoteRepository.updateRemoteNextRec(
//                                currentUser.storeid,
//                                numberSequence.nextRec
//                            )
//
//                            if (updateResult.isSuccess) {
//                                // Delete number sequence first
//                                numberSequenceRemoteDao.deleteNumberSequenceByStoreId(currentUser.storeid)
//
//                                // Then clear all transactions
//                                withContext(Dispatchers.IO) {
//                                    clearAllTransactions()
//                                }
//
//                                withContext(Dispatchers.Main) {
//                                    loadingDialog.dismiss()
//                                    proceedWithLogout()
//                                }
//                            } else {
//                                withContext(Dispatchers.Main) {
//                                    loadingDialog.dismiss()
//                                    showRetryDialog()
//                                }
//                            }
//                        } else {
//                            withContext(Dispatchers.IO) {
//                                clearAllTransactions()
//                            }
//
//                            withContext(Dispatchers.Main) {
//                                loadingDialog.dismiss()
//                                proceedWithLogout()
//                            }
//                        }
//                    } else {
//                        withContext(Dispatchers.Main) {
//                            loadingDialog.dismiss()
//                            showRetryDialog()
//                        }
//                    }
//                } else {
//                    withContext(Dispatchers.Main) {
//                        loadingDialog.dismiss()
//                        proceedWithLogout()
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e("Logout", "Error during logout", e)
//                withContext(Dispatchers.Main) {
//                    loadingDialog.dismiss()
//                    showRetryDialog()
//                }
//            }
//        }
//    }