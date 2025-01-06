package com.example.possystembw

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
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
import android.webkit.WebViewDatabase
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
import com.example.possystembw.Repository.StaffRepository
import com.example.possystembw.adapter.WindowTableAdapter
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.data.CartRepository
import com.example.possystembw.data.LoyaltyCardRepository
import com.example.possystembw.data.NumberSequenceRemoteRepository
import com.example.possystembw.data.TransactionRepository
import com.example.possystembw.data.WindowTableRepository
import com.example.possystembw.database.TransactionRecord
import com.example.possystembw.database.TransactionSummary
import com.example.possystembw.database.WindowTable
import com.example.possystembw.ui.AttendanceActivity
import com.example.possystembw.ui.LoginActivity
import com.example.possystembw.ui.PrinterSettingsActivity
import com.example.possystembw.ui.SessionManager
import com.example.possystembw.ui.StaffManager
import com.example.possystembw.ui.StockCountingActivity
import com.example.possystembw.ui.ViewModel.CartViewModel
import com.example.possystembw.ui.ViewModel.CartViewModelFactory
import com.example.possystembw.ui.ViewModel.LoyaltyCardViewModel
import com.example.possystembw.ui.ViewModel.LoyaltyCardViewModelFactory
import com.example.possystembw.ui.ViewModel.OrderWebViewActivity
import com.example.possystembw.ui.ViewModel.StaffViewModel
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

    private var isAnimating = false

    private lateinit var cartViewModel: CartViewModel


    private lateinit var staffViewModel: StaffViewModel

    // Add to your existing properties
    private var staffRefreshJob: Job? = null

    private lateinit var loyaltyCardViewModel: LoyaltyCardViewModel
    private var loyaltyCardRefreshJob: Job? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val loyaltyCardDao = AppDatabase.getDatabase(application).loyaltyCardDao()
        val loyaltyCardApi = RetrofitClient.loyaltyCardApi
        val loyaltyCardRepository = LoyaltyCardRepository(loyaltyCardDao, loyaltyCardApi)
        val loyaltyCardFactory = LoyaltyCardViewModelFactory(loyaltyCardRepository)
        loyaltyCardViewModel = ViewModelProvider(this, loyaltyCardFactory)[LoyaltyCardViewModel::class.java]

        val staffDao = AppDatabase.getDatabase(application).staffDao()
        val staffApi = RetrofitClient.staffApi
        val staffRepository = StaffRepository(staffApi, staffDao)
        staffViewModel = ViewModelProvider(
            this,
            StaffViewModel.StaffViewModelFactory(staffRepository)
        )[StaffViewModel::class.java]

        // Initialize ViewModels and Adapters
        val windowFactory = WindowViewModelFactory(application)
        windowViewModel = ViewModelProvider(this, windowFactory).get(WindowViewModel::class.java)

        val windowTableDao = AppDatabase.getDatabase(application).windowTableDao()
        val windowTableApi = RetrofitClient.windowTableApi
        val windowTableRepository = WindowTableRepository(windowTableDao, windowTableApi)
        val windowTableFactory = WindowTableViewModelFactory(windowTableRepository)
        windowTableViewModel =
            ViewModelProvider(this, windowTableFactory).get(WindowTableViewModel::class.java)

        val cartRepository = CartRepository(AppDatabase.getDatabase(application).cartDao())
        val cartViewModelFactory = CartViewModelFactory(cartRepository)  // Use the separate factory class
        cartViewModel = ViewModelProvider(this, cartViewModelFactory)[CartViewModel::class.java]

        windowAdapter = WindowAdapter(
            onClick = { window -> openWindow(window) },
            cartViewModel = cartViewModel
        )
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

        updateStoreInfo()
        observeCartChanges()

        startStaffRefresh()

        startLoyaltyCardRefresh()


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
    private fun startStaffRefresh() {
        val currentUser = SessionManager.getCurrentUser()
        if (currentUser?.storeid == null) return

        staffRefreshJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    staffViewModel.refreshStaffData(currentUser.storeid)
//                    delay(10000) // Refresh every 60 seconds
                    delay(24 * 60 * 60 * 1000) // Refresh every 24 hours

                } catch (e: Exception) {
                    Log.e("MainActivity", "Error refreshing staff data", e)
                }
            }
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
            loadWebContent("https://eljin.org/dashboard")
        }

        findViewById<ImageButton>(R.id.button3).setOnClickListener {
            showToast("ORDERING")
            loadWebContent("https://eljin.org/order")
        }

//        findViewById<ImageButton>(R.id.button4).setOnClickListener {
//            showToast("Batch Counting")
//            loadDirectContent("https://eljin.org/StockCounting")
//        }
        findViewById<ImageButton>(R.id.stockcounting).setOnClickListener {
            val intent = Intent (this, StockCountingActivity::class.java)
            startActivity(intent)
            showToast("Stock Counting")

        }
        findViewById<ImageButton>(R.id.button5).setOnClickListener {
            showToast("Stock Transfer")
            loadDirectContent("https://eljin.org/StockTransfer")
        }

        findViewById<ImageButton>(R.id.button6).setOnClickListener {
            showToast("REPORTS")
            loadWebContent("https://eljin.org/reports")
        }

        findViewById<ImageButton>(R.id.waste).setOnClickListener {
            showToast("WASTE")
            loadDirectContent("https://eljin.org/waste")
        }

        findViewById<ImageButton>(R.id.partycakes).setOnClickListener {
            showToast("Loyalty Card")
            loadDirectContent("https://eljin.org/loyalty-cards")
        }

        findViewById<ImageButton>(R.id.customer).setOnClickListener {
            showToast("CUSTOMER")
            loadDirectContent("https://eljin.org/customers")
        }

        findViewById<ImageButton>(R.id.printerSettingsButton).setOnClickListener {
            val intent = Intent(this, PrinterSettingsActivity::class.java)
            startActivity(intent)
            showToast("PRINTER SETTINGS")
        }

        findViewById<ImageButton>(R.id.attendanceButton).setOnClickListener {
            val intent = Intent(this, AttendanceActivity::class.java)
            startActivity(intent)
            showToast("ATTENDANCE")
        }

        findViewById<ImageButton>(R.id.button7).setOnClickListener {
            showToast("POS SYSTEM")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.button8).setOnClickListener {
            logout()
        }
    }
    private fun loadDirectContent(url: String) {
        webView.visibility = View.VISIBLE
        webViewContainer.visibility = View.VISIBLE

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

        // Load the URL directly without showing loading indicator
        webView.loadUrl(url)
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
    private fun updateStoreInfo() {
        val storeNameTextView = findViewById<TextView>(R.id.storeNameTextView)
        val currentStore = SessionManager.getCurrentUser()?.storeid ?: "Unknown Store"

        // Set initial alpha to 0 for fade-in animation
        storeNameTextView.alpha = 0f

        // Update text with store name
        storeNameTextView.text = "Store: $currentStore"

        // Animate the store name appearance
        storeNameTextView.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(300)
            .start()
    }


    private fun collapseSidebar() {
        // Prevent multiple animations from running simultaneously
        if (!isSidebarExpanded || isAnimating) return
        isAnimating = true

        // Create animations set
        val animatorSet = AnimatorSet()

        // Sidebar width animation
        val collapseWidth = ValueAnimator.ofInt(sidebarLayout.width, dpToPx(24)).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                sidebarLayout.layoutParams = sidebarLayout.layoutParams.apply {
                    width = value
                }
                updateContentMargins(value)
            }
        }

        // Toggle button position animation
        val toggleButtonMargin = ValueAnimator.ofInt(dpToPx(90), dpToPx(8)).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                toggleButton.layoutParams =
                    (toggleButton.layoutParams as ConstraintLayout.LayoutParams).apply {
                        marginStart = value
                    }
            }
        }

        // Setup animation sequence
        animatorSet.playTogether(
            collapseWidth,
            toggleButtonMargin,
            ObjectAnimator.ofFloat(toggleButton, View.ROTATION, 0f, 180f).apply {
                duration = 300
            },
            ObjectAnimator.ofFloat(buttonContainer, View.ALPHA, 1f, 0f).apply {
                duration = 150
            },
            ObjectAnimator.ofFloat(findViewById(R.id.ecposTitle), View.ALPHA, 1f, 0f).apply {
                duration = 150
            }
        )

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                // Initial state setup
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                buttonContainer.visibility = View.GONE
                isSidebarExpanded = false
                isAnimating = false
            }
        })

        animatorSet.start()
    }

    private fun expandSidebar() {
        // Prevent multiple animations from running simultaneously
        if (isSidebarExpanded || isAnimating) return
        isAnimating = true

        // Create animations set
        val animatorSet = AnimatorSet()

        // Sidebar width animation
        val expandWidth = ValueAnimator.ofInt(sidebarLayout.width, dpToPx(100)).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                sidebarLayout.layoutParams = sidebarLayout.layoutParams.apply {
                    width = value
                }
                updateContentMargins(value)
            }
        }

        // Toggle button position animation
        val toggleButtonMargin = ValueAnimator.ofInt(dpToPx(8), dpToPx(90)).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                toggleButton.layoutParams =
                    (toggleButton.layoutParams as ConstraintLayout.LayoutParams).apply {
                        marginStart = value
                    }
            }
        }

        // Setup animation sequence
        animatorSet.playTogether(
            expandWidth,
            toggleButtonMargin,
            ObjectAnimator.ofFloat(toggleButton, View.ROTATION, 180f, 0f).apply {
                duration = 300
            }
        )

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                buttonContainer.visibility = View.VISIBLE
                buttonContainer.alpha = 0f
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                // Fade in content after expansion
                buttonContainer.animate()
                    .alpha(1f)
                    .setDuration(150)
                    .start()
                findViewById<TextView>(R.id.ecposTitle).animate()
                    .alpha(1f)
                    .setDuration(150)
                    .start()
                isSidebarExpanded = true
                isAnimating = false
            }
        })

        animatorSet.start()
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
    private fun startLoyaltyCardRefresh() {
        loyaltyCardRefreshJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    Log.d("MainActivity", "Refreshing loyalty cards")
                    loyaltyCardViewModel.refreshLoyaltyCards()
                    delay(30000) // Refresh every 30 seconds
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error during loyalty card refresh", e)
                }
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
        lifecycleScope.launch {
            loyaltyCardViewModel.allLoyaltyCards.collect { cards ->
                Log.d("MainActivity", "Received loyalty cards: ${cards.size}")
                // Handle the loyalty cards here
            }
        }

        // Add these new observers
        lifecycleScope.launch {
            loyaltyCardViewModel.isLoading.collect { isLoading ->
                // Update UI loading state if needed
                // For example, show/hide a loading indicator
                loadingDialog.apply {
                    if (isLoading) show() else dismiss()
                }
            }
        }

        lifecycleScope.launch {
            loyaltyCardViewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                }
            }
        }
        // Add staff observers here
        lifecycleScope.launch {
            staffViewModel.staffData.collect { staffList ->
                Log.d("MainActivity", "Staff updated: ${staffList.size} members")
                // You can add additional handling here if needed
            }
        }

        // Add staff error observer
        staffViewModel.error.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
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
    private fun observeCartChanges() {
        lifecycleScope.launch {
            // Collect all windows that have items in cart
            cartViewModel.getAllWindows().collect { cartItems ->
                val windowsWithItems = cartItems
                    .map { it.windowId }
                    .toSet()
                windowAdapter.updateWindowsWithCartItems(windowsWithItems)
            }
        }
    }
    private fun setupWindowRecyclerView() {
        val windowRecyclerView: RecyclerView = findViewById(R.id.windowRecyclerView)
        windowRecyclerView.adapter = windowAdapter

        // Calculate span count based on screen width
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val spanCount = (screenWidthDp / 400).toInt().coerceAtLeast(2)

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
//                    loyaltyCardViewModel.refreshLoyaltyCards()
                    Log.d("MainActivity", "Windows and window tables refreshed successfully")
//                    delay(24 * 60 * 60 * 1000) // Refresh every 24 hours
                    delay(60000) // Refresh every 24 hours
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
        loyaltyCardRefreshJob?.cancel()
        transactionSyncService?.stopSyncService()
        webView.destroy()
        staffRefreshJob?.cancel()
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
            useWideViewPort = true
            loadWithOverviewMode = true
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            setEnableSmoothTransition(true)
            cacheMode = WebSettings.LOAD_DEFAULT
            databaseEnabled = true
            setGeolocationEnabled(true)
            javaScriptCanOpenWindowsAutomatically = true
        }

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // List of URLs that should skip loading indicator
        val skipLoadingUrls = listOf(
            "StockCounting",
//            "StockTransfer",
//            "waste",
//            "loyalty-cards",
//            "customers"
        )

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                // Only show loading for URLs not in skipLoadingUrls
                if (url != null && !skipLoadingUrls.any { url.contains(it, ignoreCase = true) }) {
                    showWebViewLoading("Loading page...")
                }
                injectScrollFix()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Hide loading only if it was shown (for non-skip URLs)
                if (url != null && !skipLoadingUrls.any { url.contains(it, ignoreCase = true) }) {
                    hideWebViewLoading()
                }
                if (url?.contains("/login") == true) {
                    handleLoginPage()
                }
                injectScrollFix()
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                val url = request?.url?.toString()
                if (url != null && !skipLoadingUrls.any { url.contains(it, ignoreCase = true) }) {
                    hideWebViewLoading()
                }
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Error loading page: ${error?.description}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                hideWebViewLoading()
                super.onReceivedSslError(view, handler, error)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                val url = view?.url
                if (url != null && !skipLoadingUrls.any { url.contains(it, ignoreCase = true) }) {
                    if (newProgress < 100) {
                        showWebViewLoading("Loading... $newProgress%")
                    } else {
                        hideWebViewLoading()
                    }
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
                val database = AppDatabase.getDatabase(application)

                // Delete staff data first
                database.staffDao().deleteAll()
                Log.d("Logout", "Successfully deleted all staff data")

                // Delete transaction records
                database.transactionDao().deleteAllTransactions()
                Log.d("Logout", "Successfully deleted all transaction records")

                // Delete transaction summaries
                database.transactionDao().deleteAllTransactionSummaries()
                Log.d("Logout", "Successfully deleted all transaction summaries")

            } catch (e: Exception) {
                Log.e("Logout", "Error clearing data", e)
                throw e
            }
        }
    }
    private fun logout() {
        // Show confirmation dialog first with custom style
        val dialogView = layoutInflater.inflate(R.layout.custom_dialog_layout, null)
        val titleTextView = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val messageTextView = dialogView.findViewById<TextView>(R.id.dialogMessage)

        titleTextView.text = "Confirm Logout"
        titleTextView.textSize = 20f // Bigger text size for title

        messageTextView.text = "Are you sure you want to logout?\n\n" +
                "Please don't logout if there's no problem.\n\n" +
                "Please contact the IT developer for assistance if needed."
        messageTextView.textSize = 18f // Bigger text size for message

        AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setView(dialogView)
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.Main) {
                            loadingDialog.show()
                        }

                        val currentUser = SessionManager.getCurrentUser()
                        if (currentUser != null) {
                            // Rest of the logout logic remains the same
                            withContext(Dispatchers.Main) {
                                endWebSession()
                                StaffManager.clearCurrentStaff()
                            }


                            // Wait briefly to ensure web session cleanup is complete
                            delay(500)

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

                                        // Clear web-related data
                                        withContext(Dispatchers.Main) {
                                            // Clear WebView data
                                            clearWebViewData()

                                            // Clear cookies from SessionManager
                                            SessionManager.setWebSessionCookies(null)

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
                                    // No number sequence to handle, proceed with clearing transactions
                                    withContext(Dispatchers.IO) {
                                        clearAllTransactions()
                                    }

                                    withContext(Dispatchers.Main) {
                                        // Clear web-related data
                                        clearWebViewData()
                                        SessionManager.setWebSessionCookies(null)

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
                            // No current user, just clear everything and proceed
                            withContext(Dispatchers.IO) {
                                clearAllTransactions()
                            }

                            withContext(Dispatchers.Main) {
                                // Clear web-related data
                                clearWebViewData()
                                SessionManager.setWebSessionCookies(null)

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
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }




    private fun endWebSession() {
        try {
            // First attempt to properly logout from the web session
            webView.evaluateJavascript(
                """
            (async function() {
                try {
                    // Get CSRF token first
                    const token = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content');
                    
                    // Attempt to logout from web session with proper headers
                    const response = await fetch('/logout', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'Accept': 'application/json',
                            'X-CSRF-TOKEN': token
                        },
                        credentials: 'include'
                    });
                    
                    // Clear any local storage or session storage
                    localStorage.clear();
                    sessionStorage.clear();
                    
                    // Clear any custom data attributes
                    document.documentElement.innerHTML = '';
                    
                    return response.ok;
                } catch(e) {
                    console.error('Logout failed:', e);
                    return false;
                }
            })();
            """
            ) { result ->
                // After web logout attempt, clear all local web data
                clearWebViewData()

                // Clear session cookies from SessionManager
                SessionManager.setWebSessionCookies(null)
            }
        } catch (e: Exception) {
            Log.e("Logout", "Error clearing web session", e)
            // Even if the JavaScript execution fails, still clear local data
            clearWebViewData()
            SessionManager.setWebSessionCookies(null)
        }
    }

    private fun clearWebViewData() {
        // Clear cookies
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies { success ->
            Log.d("Logout", "Cookies cleared: $success")
        }
        cookieManager.flush()

        // Clear all WebView data
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
        webView.clearSslPreferences()

        // Clear any saved passwords or form data
        WebViewDatabase.getInstance(this)?.clearFormData()
        WebViewDatabase.getInstance(this)?.clearHttpAuthUsernamePassword()

        // Load about:blank to ensure clean state
        webView.loadUrl("about:blank")
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

        // Stop any ongoing sync service
        transactionSyncService?.stopSyncService()

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
    private suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(application)

                // Clear staff data
                database.staffDao().deleteAll()

                // Clear transactions
                clearAllTransactions()

                Log.d("Logout", "Successfully cleared all data")
            } catch (e: Exception) {
                Log.e("Logout", "Error clearing data", e)
                throw e
            }
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
                val dialogView = layoutInflater.inflate(R.layout.custom_dialog_layout, null)
                val titleTextView = dialogView.findViewById<TextView>(R.id.dialogTitle)
                val messageTextView = dialogView.findViewById<TextView>(R.id.dialogMessage)

                titleTextView.text = "Logout Failed"
                titleTextView.textSize = 20f // Bigger text size for title

                val message = buildString {
                    append("Unable to complete logout process.\n\n")
                    if (unsyncedCount > 0) {
                        append("• $unsyncedCount unsynced transactions pending\n")
                    }
                    if (numberSequence != null) {
                        append("• Number sequence update pending\n")
                    }
                    append("\nPlease contact the IT developer for assistance.\n")
                    append("\nWould you like to retry the logout process?")
                }
                messageTextView.text = message
                messageTextView.textSize = 18f // Bigger text size for message

                AlertDialog.Builder(this@MainActivity, R.style.CustomDialogStyle1)
                    .setView(dialogView)
                    .setPositiveButton("Retry") { dialog, _ ->
                        dialog.dismiss()
                        logout()
                    }
                    .setNegativeButton("Back") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun initLoadingDialog() {
        val builder = AlertDialog.Builder(this, R.style.CustomDialogStyle3)
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