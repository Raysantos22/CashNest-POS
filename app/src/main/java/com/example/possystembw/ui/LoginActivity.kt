package com.example.possystembw.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.possystembw.DeviceUtils
import com.example.possystembw.MainActivity
import com.example.possystembw.R
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.database.User
import com.example.possystembw.ui.ViewModel.LoginDataState
import com.example.possystembw.ui.ViewModel.LoginViewModel
import com.example.possystembw.ui.ViewModel.TransactionLoadingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date


class LoginActivity : AppCompatActivity() {
    private lateinit var viewModel: LoginViewModel
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var loadingOverlay: RelativeLayout
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var loadingText: TextView
    private lateinit var hiddenWebView: WebView

    private var webLoginAttempted = false
    private var isWebLoginComplete = false
    private var isNativeLoginComplete = false
    private var webLoginTimeout = false
    private var isAttendanceDataLoaded = false

    private val webLoginTimeoutHandler = Handler(Looper.getMainLooper())
    private val WEB_LOGIN_TIMEOUT = 10000L // 10 seconds timeout

    private var usersFetched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtils.setOrientationBasedOnDevice(this)

        SessionManager.init(applicationContext)

        if (SessionManager.isLoggedIn()) {
            startMainActivity()
            return
        }

        setContentView(R.layout.activity_login)
        initializeViews()
        setupWebView()
        setupLoginButton()
        setupObservers()
        setupBackgroundVideo()

        // Start fetching users immediately
        fetchUsers()
    }

    private inner class WebAppInterface {
        @JavascriptInterface
        fun onWebLoginSuccess() {
            runOnUiThread {
                Log.d("LoginActivity", "Web login successful")
                webLoginTimeoutHandler.removeCallbacksAndMessages(null)
                val cookies = CookieManager.getInstance().getCookie("https://eljin.org")
                SessionManager.setWebSessionCookies(cookies)
                isWebLoginComplete = true
                checkLoginCompletion()
            }
        }

        @JavascriptInterface
        fun onWebLoginError(error: String) {
            runOnUiThread {
                Log.e("LoginActivity", "Web login error: $error")
                // Don't block the login process, just log the error
                proceedWithoutWebLogin("Web login failed: $error")
            }
        }
    }

    private fun fetchUsers() {
        if (!usersFetched) {
            showLoading("Syncing data...")
            viewModel.fetchUsers()
        }
    }

    private fun initializeViews() {
        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        loadingText = findViewById(R.id.loadingText)
        hiddenWebView = findViewById(R.id.hiddenWebView)

        // Prevent touch events from passing through the overlay
        loadingOverlay.setOnTouchListener { _, _ -> true }
    }

    private fun setupWebView() {
        hiddenWebView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }

            addJavascriptInterface(WebAppInterface(), "Android")

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d("LoginActivity", "Page finished loading: $url")

                    if (!webLoginAttempted && url?.contains("/login") == true) {
                        webLoginAttempted = true

                        // Set timeout for web login
                        webLoginTimeoutHandler.postDelayed({
                            if (!isWebLoginComplete) {
                                webLoginTimeout = true
                                proceedWithoutWebLogin("Web login timed out")
                            }
                        }, WEB_LOGIN_TIMEOUT)

                        view?.evaluateJavascript(
                            """
                            (async function() {
                                try {
                                    const token = document.querySelector('meta[name="csrf-token"]').getAttribute('content');
                                    const email = '${emailInput.text}';
                                    const password = '${passwordInput.text}';
                                    
                                    await fetch('/sanctum/csrf-cookie', {
                                        method: 'GET',
                                        credentials: 'include'
                                    });
                                    
                                    const response = await fetch('/login', {
                                        method: 'POST',
                                        headers: {
                                            'Content-Type': 'application/json',
                                            'Accept': 'application/json',
                                            'X-CSRF-TOKEN': token,
                                            'X-Requested-With': 'XMLHttpRequest'
                                        },
                                        credentials: 'include',
                                        body: JSON.stringify({
                                            email: email,
                                            password: password,
                                            _token: token
                                        })
                                    });
                                    
                                    if (response.redirected || response.ok) {
                                        window.location.href = '/dashboard';
                                    } else {
                                        Android.onWebLoginError('Login request failed');
                                    }
                                } catch(e) {
                                    Android.onWebLoginError(e.toString());
                                }
                            })();
                            """.trimIndent()
                        ) { result ->
                            Log.d("LoginActivity", "Login script result: $result")
                        }
                    }
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    Log.e("LoginActivity", "WebView error: ${error?.description}")
                    proceedWithoutWebLogin("WebView error: ${error?.description}")
                }
            }
        }
    }

    private fun proceedWithoutWebLogin(reason: String) {
        if (!isWebLoginComplete && !webLoginTimeout) {
            Log.w("LoginActivity", "Proceeding without web login: $reason")
            isWebLoginComplete = true // Mark as complete to allow login to proceed
            webLoginTimeoutHandler.removeCallbacksAndMessages(null)
            checkLoginCompletion()
        }
    }

    private fun setupLoginButton() {
        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                showLoading("Logging in...")
                webLoginAttempted = false
                isWebLoginComplete = false
                isNativeLoginComplete = false
                isAttendanceDataLoaded = false
                webLoginTimeout = false

                // Start native login
                viewModel.login(email, password)

                // Attempt web login but don't block on it
                try {
                    hiddenWebView.loadUrl("https://eljin.org/login")
                } catch (e: Exception) {
                    Log.e("LoginActivity", "Failed to start web login", e)
                    proceedWithoutWebLogin("Failed to start web login: ${e.message}")
                }
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        // Observe users fetch result
        viewModel.fetchUsersResult.observe(this) { result ->
            result.onSuccess { users ->
                usersFetched = true
                hideLoading()
                Log.d("LoginActivity", "Successfully fetched ${users.size} users")
            }.onFailure { error ->
                hideLoading()
                Toast.makeText(this, "Failed to sync data: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e("LoginActivity", "Failed to fetch users", error)
            }
        }

        viewModel.loginResult.observe(this) { result ->
            result.onSuccess { user ->
                SessionManager.setCurrentUser(user)
                isNativeLoginComplete = true

                // Fetch attendance data after successful login
                fetchAttendanceData(user.storeid ?: "")
            }.onFailure { error ->
                hideLoading()
                SessionManager.clearCurrentUser()
                Toast.makeText(this, "Login failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.staffData.observe(this) { result ->
            result.onSuccess { staffList ->
                Log.d("LoginActivity", "Successfully fetched and stored ${staffList.size} staff members")
            }.onFailure { error ->
                Log.e("LoginActivity", "Failed to fetch staff data", error)
            }
        }

        // Add attendance data observer
        viewModel.attendanceData.observe(this) { result ->
            result.onSuccess { attendanceList ->
                isAttendanceDataLoaded = true
                SessionManager.setAttendanceData(attendanceList)
                Log.d("LoginActivity", "Successfully fetched and stored ${attendanceList.size} attendance records")
                checkLoginCompletion()
            }.onFailure { error ->
                Log.e("LoginActivity", "Failed to fetch attendance data", error)
                // Don't block login if attendance fetch fails
                isAttendanceDataLoaded = true
                checkLoginCompletion()
            }
        }

        viewModel.loginDataState.observe(this) { state ->
            when (state) {
                is LoginDataState.Loading -> {
                    showLoading("Loading data...")
                }
                is LoginDataState.Success -> {
                    showLoading("Loaded: ${state.transactionSummaryCount} summaries, ${state.transactionRecordCount} records")
                }
                is LoginDataState.Error -> {
                    hideLoading()
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                is LoginDataState.Warning -> {
                    // Handle warning state if needed
                }
            }
        }
    }

    private fun fetchAttendanceData(storeId: String) {
        if (storeId.isNotEmpty()) {
            showLoading("Loading attendance data...")
            viewModel.fetchAttendanceData(storeId)
        } else {
            Log.w("LoginActivity", "No store ID available, skipping attendance fetch")
            isAttendanceDataLoaded = true
            checkLoginCompletion()
        }
    }

    private fun checkLoginCompletion() {
        val webStatus = if (isWebLoginComplete) "completed" else "pending"
        val attendanceStatus = if (isAttendanceDataLoaded) "loaded" else "loading"

        Log.d("LoginActivity", "Login status - Native: $isNativeLoginComplete, Web: $webStatus, Attendance: $attendanceStatus")

        if (isNativeLoginComplete && isAttendanceDataLoaded) {
            // If native login is successful and attendance is loaded, proceed regardless of web login state
            if (!isWebLoginComplete) {
                Log.w("LoginActivity", "Proceeding with login despite incomplete web login")
                isWebLoginComplete = true
            }

            Log.d("LoginActivity", "Login completed successfully with attendance data")
            SessionManager.refreshSession()
            startMainActivity()
        }
    }

    private fun setupBackgroundVideo() {
        val videoView = findViewById<VideoView>(R.id.backgroundVideo)

        try {
            // Set video path
            val path = "android.resource://$packageName/drawable/" + R.raw.ads
            videoView.setVideoPath(path)

            // Configure video playbook with muted audio
            videoView.setOnPreparedListener { mediaPlayer ->
                // Mute the video
                mediaPlayer.setVolume(0f, 0f)

                // Enable looping
                mediaPlayer.isLooping = true

                // Start playing
                videoView.start()

                // Optional: Scale video to fill screen while maintaining aspect ratio
                val videoRatio = mediaPlayer.videoWidth.toFloat() / mediaPlayer.videoHeight.toFloat()
                val screenRatio = videoView.width.toFloat() / videoView.height.toFloat()
                val scaleX = videoRatio / screenRatio
                if (scaleX >= 1f) {
                    videoView.scaleX = scaleX
                } else {
                    videoView.scaleY = 1f / scaleX
                }
            }

            // Error handling
            videoView.setOnErrorListener { _, what, extra ->
                Log.e("VideoView", "Error playing video: what=$what extra=$extra")
                true
            }

        } catch (e: Exception) {
            Log.e("VideoView", "Error setting up video", e)
        }
    }

    private fun showLoading(message: String) {
        runOnUiThread {
            loadingText.text = message
            loadingOverlay.visibility = View.VISIBLE
            // Animate the overlay appearing
            loadingOverlay.alpha = 0f
            loadingOverlay.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
            loginButton.isEnabled = false
        }
    }

    private fun hideLoading() {
        if (!isNativeLoginComplete && !isWebLoginComplete) {
            // Animate the overlay disappearing
            loadingOverlay.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    loadingOverlay.visibility = View.GONE
                }
                .start()
            loginButton.isEnabled = true
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
//class LoginActivity : AppCompatActivity() {
//    private lateinit var viewModel: LoginViewModel
//    private lateinit var emailInput: EditText
//    private lateinit var passwordInput: EditText
//    private lateinit var loginButton: Button
//    private lateinit var loadingProgressBar: ProgressBar
//    private lateinit var loadingText: TextView
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // Initialize SessionManager
//        SessionManager.init(applicationContext)
//
//        // Check if user is already logged in
//        if (SessionManager.isLoggedIn()) {
//            startMainActivity()
//            return
//        }
//
//        setContentView(R.layout.activity_login)
//
//        // Initialize ViewModel and views
//        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
//        emailInput = findViewById(R.id.emailInput)
//        passwordInput = findViewById(R.id.passwordInput)
//        loginButton = findViewById(R.id.loginButton)
//        loadingProgressBar = findViewById(R.id.loadingProgressBar)
//        loadingText = findViewById(R.id.loadingText)
////        setupLoginButton()
//        setupObservers()
////        addSampleUserIfFirstRun()
//
//        loginButton.setOnClickListener {
//            val email = emailInput.text.toString()
//            val password = passwordInput.text.toString()
//
//            if (email.isNotEmpty() && password.isNotEmpty()) {
//                Log.d("LoginActivity", "Attempting login with email: $email")
//                viewModel.login(email, password)
//            } else {
//                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        viewModel.loginResult.observe(this) { result ->
//            result.onSuccess { user ->
//                // Set the current user in SessionManager
//                SessionManager.setCurrentUser(user)
//
//                Toast.makeText(this, "Login successful for ${user.name}", Toast.LENGTH_SHORT).show()
//                Log.d(
//                    "LoginActivity",
//                    "Login successful - User: ${user.name}, Store: ${user.storeid}"
//                )
//
//                startMainActivity()
//            }.onFailure { error ->
//                SessionManager.clearCurrentUser()
//                Toast.makeText(this, "Login failed: ${error.message}", Toast.LENGTH_SHORT).show()
//                Log.e("LoginActivity", "Login failed", error)
//            }
//        }
//
//        Log.d("LoginActivity", "Fetching users from API...")
//        viewModel.fetchUsers()
//    }
//    private fun setupObservers() {
//        viewModel.loginResult.observe(this) { result ->
//            result.onSuccess { user ->
//                // Don't start MainActivity immediately - wait for transaction loading
//                Log.d("LoginActivity", "Login successful for ${user.name}, Store: ${user.storeid}")
//            }.onFailure { error ->
//                hideLoading()
//                SessionManager.clearCurrentUser()
//                Toast.makeText(this, "Login failed: ${error.message}", Toast.LENGTH_SHORT).show()
//                Log.e("LoginActivity", "Login failed", error)
//            }
//        }
//
//        viewModel.transactionLoadingState.observe(this) { state ->
//            when (state) {
//                is TransactionLoadingState.Loading -> {
//                    showLoading("Loading transactions...")
//                }
//                is TransactionLoadingState.Success -> {
//                    hideLoading()
//                    Log.d("LoginActivity", "Loaded ${state.summaryCount} summaries and ${state.detailsCount} details")
//                    startMainActivity()
//                }
//                is TransactionLoadingState.Error -> {
//                    hideLoading()
//                    Log.e("LoginActivity", "Transaction loading error: ${state.message}")
//                    // Show error but still proceed to main activity since login was successful
//                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
//                    startMainActivity()
//                }
//            }
//        }
//    }
//
//    private fun showLoading(message: String) {
//        loadingProgressBar.visibility = View.VISIBLE
//        loadingText.visibility = View.VISIBLE
//        loadingText.text = message
//        loginButton.isEnabled = false
//    }
//
//    private fun hideLoading() {
//        loadingProgressBar.visibility = View.GONE
//        loadingText.visibility = View.GONE
//        loginButton.isEnabled = true
//    }
//    private fun setupLoginObserver() {
//        viewModel.loginResult.observe(this) { result ->
//            result.onSuccess { user ->
//                Log.d("Login", "Login successful for user: ${user.name}, store: ${user.storeid}")
//            }.onFailure { error ->
//                Log.e("Login", "Login failed", error)
//            }
//        }
//    }
//
//    private fun startMainActivity() {
//        val intent = Intent(this@LoginActivity, MainActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        startActivity(intent)
//        finish()
//    }
//
//    // Add this method to check if user is already logged in
//    private fun checkExistingSession() {
//        if (SessionManager.getCurrentUser() != null) {
//            // User is already logged in, redirect to MainActivity
//            val intent = Intent(this@LoginActivity, MainActivity::class.java)
//            startActivity(intent)
//            finish()
//        }
//    }
//}
//class LoginActivity : AppCompatActivity() {
//    private lateinit var viewModel: LoginViewModel
//    private lateinit var emailInput: EditText
//    private lateinit var passwordInput: EditText
//    private lateinit var loginButton: Button
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // Initialize SessionManager
//        SessionManager.init(applicationContext)
//
//        // Check if user is already logged in
//        if (SessionManager.isLoggedIn()) {
//            startMainActivity()
//            return
//        }
//
//        setContentView(R.layout.activity_login)
//
//        // Initialize ViewModel and views
//        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
//        emailInput = findViewById(R.id.emailInput)
//        passwordInput = findViewById(R.id.passwordInput)
//        loginButton = findViewById(R.id.loginButton)
//
//        addSampleUserIfFirstRun()
//
//        loginButton.setOnClickListener {
//            val email = emailInput.text.toString()
//            val password = passwordInput.text.toString()
//
//            if (email.isNotEmpty() && password.isNotEmpty()) {
//                Log.d("LoginActivity", "Attempting login with email: $email")
//                viewModel.login(email, password)
//            } else {
//                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        viewModel.loginResult.observe(this) { result ->
//            result.onSuccess { user ->
//                // Set the current user in SessionManager
//                SessionManager.setCurrentUser(user)
//
//                Toast.makeText(this, "Login successful for ${user.name}", Toast.LENGTH_SHORT).show()
//                Log.d("LoginActivity", "Login successful - User: ${user.name}, Store: ${user.storeid}")
//
//                startMainActivity()
//            }.onFailure { error ->
//                SessionManager.clearCurrentUser()
//                Toast.makeText(this, "Login failed: ${error.message}", Toast.LENGTH_SHORT).show()
//                Log.e("LoginActivity", "Login failed", error)
//            }
//        }
//
//        Log.d("LoginActivity", "Fetching users from API...")
//        viewModel.fetchUsers()
//    }
//
//    private fun startMainActivity() {
//        val intent = Intent(this@LoginActivity, MainActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        startActivity(intent)
//        finish()
//    }
//
//    // Add this method to check if user is already logged in
//    private fun checkExistingSession() {
//        if (SessionManager.getCurrentUser() != null) {
//            // User is already logged in, redirect to MainActivity
//            val intent = Intent(this@LoginActivity, MainActivity::class.java)
//            startActivity(intent)
//            finish()
//        }
//    }
//
//
//    private fun addSampleUserIfFirstRun() {
//        val prefs = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
//        val isFirstRun = prefs.getBoolean("isFirstRun", true)
//
//        if (isFirstRun) {
//            lifecycleScope.launch {
//                withContext(Dispatchers.IO) {
//                    val userDao = AppDatabase.getDatabase(applicationContext).userDao()
//                    val currentDate = Date()
//                    val sampleUser = User(
//                        id = 0,
//                        name = "Ray Santos",
//                        email = "ray",
//                        storeid = "1",
//                        email_verified_at = null,
//                        password = "$2y$12$3DjPPc0yzbdTwI5HGzC9Fe29Nv6o4oN/lqcMznk/JXwHPL.V5SH5a", // Bcrypt hash for "welcome"
//                        two_factor_secret = null,
//                        two_factor_recovery_codes = null,
//                        two_factor_confirmed_at = null,
//                        remember_token = null,
//                        current_team_id = null,
//                        profile_photo_path = null,
//                        role = "user",
//                        created_at = currentDate,
//                        updated_at = currentDate
//                    )
//
//                    try {
//                        userDao.insertUser(sampleUser)
//                        Log.d("LoginActivity", "Sample user added: ${sampleUser.email}")
//                    } catch (e: Exception) {
//                        Log.e("LoginActivity", "Error inserting user: ${e.message}")
//                    }
//                }
//            }
//            prefs.edit().putBoolean("isFirstRun", false).apply()
//        }
//    }
//}
