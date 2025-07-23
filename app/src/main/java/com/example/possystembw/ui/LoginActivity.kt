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
import com.example.possystembw.DAO.TransactionDao
import com.example.possystembw.DeviceUtils
import com.example.possystembw.MainActivity
import com.example.possystembw.R
import com.example.possystembw.RetrofitClient
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.data.NumberSequenceRemoteRepository
import com.example.possystembw.database.User
import com.example.possystembw.ui.ViewModel.LoginDataState
import com.example.possystembw.ui.ViewModel.LoginViewModel
import com.example.possystembw.ui.ViewModel.TransactionLoadingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


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
    private var isTransactionDataLoaded = false
    private var isDateConversionComplete = false
    private lateinit var numberSequenceRemoteRepository: NumberSequenceRemoteRepository

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
                // Don't call checkLoginCompletion here - let fetchAndConvertTransactionData handle it
            }.onFailure { error ->
                Log.e("LoginActivity", "Failed to fetch attendance data", error)
                isAttendanceDataLoaded = true
                // Don't call checkLoginCompletion here either
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


    private fun fetchAndConvertTransactionData(storeId: String) {
        if (storeId.isNotEmpty()) {
            showLoading("Syncing transactions...")

            lifecycleScope.launch {
                try {
                    // Step 1: Fetch transactions from API
                    withContext(Dispatchers.IO) {
                        val database = AppDatabase.getDatabase(this@LoginActivity)
                        val transactionDao = database.transactionDao()

                        try {
                            // STEP 1: Check if data already exists to prevent duplication
                            val existingSummaries = transactionDao.getTransactionsByStore(storeId)
                            val existingRecords = transactionDao.getAllTransactionRecords()

                            Log.d("LoginSync", "Existing data check:")
                            Log.d("LoginSync", "  Existing summaries: ${existingSummaries.size}")
                            Log.d("LoginSync", "  Existing records: ${existingRecords.size}")

                            // STEP 2: Only sync if no data exists OR if user explicitly wants fresh data
                            if (existingSummaries.isNotEmpty()) {
                                Log.d("LoginSync", "Transaction data already exists, skipping API fetch")
                                withContext(Dispatchers.Main) {
                                    showLoading("Using existing transaction data...")
                                }

                                // Just ensure dates are in correct format for existing data
                                convertExistingDataDates(transactionDao)

                                delay(1000)
                                isTransactionDataLoaded = true
                                isDateConversionComplete = true
                                checkLoginCompletion()
                                return@withContext
                            }

                            // STEP 3: Fetch fresh data only if no existing data
                            Log.d("LoginSync", "No existing transaction data, fetching from API...")

                            withContext(Dispatchers.Main) {
                                showLoading("Fetching transaction data from API...")
                            }

                            // Get transactions from API
                            val summariesResponse = RetrofitClient.transactionSyncApi.getTransactionSummaries(storeId)
                            val detailsResponse = RetrofitClient.transactionSyncApi.getTransactionDetails(storeId)

                            var transactionCount = 0
                            var recordCount = 0

                            if (summariesResponse.isSuccessful) {
                                val apiSummaries = summariesResponse.body() ?: emptyList()

                                // FIXED: Convert dates and handle null values during insert
                                val convertedSummaries = apiSummaries.map { summary ->
                                    summary.copy(
                                        createdDate = summary.createdDate?.convertApiDateToSimple() ?: getCurrentDateString(),
                                        // FIXED: Handle null paymentMethod and other fields
                                        paymentMethod = summary.paymentMethod ?: "",
                                        customerAccount = summary.customerAccount ?: "",
                                        staff = summary.staff ?: "",
                                        comment = summary.comment ?: "",
                                        currency = summary.currency ?: "PHP",
                                        receiptEmail = summary.receiptEmail ?: "",
                                        markupDescription = summary.markupDescription ?: "",
                                        discountType = summary.discountType ?: "",
                                        customerName = summary.customerName ?: "",
                                        refundReceiptId = summary.refundReceiptId ?: "",
                                        zReportId = summary.zReportId ?: "",
                                        storeKey = summary.storeKey ?: "",
                                        storeSequence = summary.storeSequence ?: ""
                                    )
                                }

                                // FIXED: Use insertOrReplace to prevent duplicates
                                transactionDao.insertOrReplaceTransactionSummaries(convertedSummaries)
                                transactionCount = convertedSummaries.size

                                Log.d("LoginSync", "Inserted ${convertedSummaries.size} NEW transaction summaries")
                            }

                            if (detailsResponse.isSuccessful) {
                                val apiDetails = detailsResponse.body() ?: emptyList()

                                // FIXED: Convert dates and handle null values during insert
                                val convertedDetails = apiDetails.map { record ->
                                    record.copy(
                                        createdDate = record.createdDate?.convertApiDateToSimple() ?: getCurrentDateString(),
                                        // FIXED: Handle other potential null fields in TransactionRecord
                                        name = record.name ?: "",
                                        receiptNumber = record.receiptNumber ?: "",
                                        paymentMethod = record.paymentMethod ?: "",
                                        comment = record.comment ?: "",
                                        receiptId = record.receiptId ?: "",
                                        itemId = record.itemId ?: "",
                                        itemGroup = record.itemGroup ?: "",
                                        customerAccount = record.customerAccount ?: "",
                                        store = record.store ?: "",
                                        staff = record.staff ?: "",
                                        discountOfferId = record.discountOfferId ?: "",
                                        unit = record.unit ?: "PCS",
                                        remarks = record.remarks ?: "",
                                        inventoryBatchId = record.inventoryBatchId ?: "",
                                        inventoryBatchExpiryDate = record.inventoryBatchExpiryDate ?: "",
                                        giftCard = record.giftCard ?: "",
                                        returnTransactionId = record.returnTransactionId ?: "",
                                        creditMemoNumber = record.creditMemoNumber ?: "",
                                        description = record.description ?: "",
                                        storeTaxGroup = record.storeTaxGroup ?: "",
                                        currency = record.currency ?: "PHP",
                                        storeKey = record.storeKey ?: "",
                                        storeSequence = record.storeSequence ?: ""
                                    )
                                }

                                // FIXED: Use insertOrReplace to prevent duplicates
                                transactionDao.insertOrReplaceAll(convertedDetails)
                                recordCount = convertedDetails.size

                                Log.d("LoginSync", "Inserted ${convertedDetails.size} NEW transaction records")
                            }

                            withContext(Dispatchers.Main) {
                                showLoading("Loaded $transactionCount transactions, $recordCount records")
                            }

                        } catch (e: Exception) {
                            Log.e("LoginSync", "Error syncing transactions: ${e.message}", e)
                            withContext(Dispatchers.Main) {
                                showLoading("Transaction sync failed, continuing...")
                            }
                        }
                    }

                    // Small delay to show the status
                    delay(1000)

                    isTransactionDataLoaded = true
                    isDateConversionComplete = true
                    checkLoginCompletion()

                } catch (e: Exception) {
                    Log.e("LoginSync", "Error in transaction sync process: ${e.message}", e)
                    isTransactionDataLoaded = true
                    isDateConversionComplete = true
                    checkLoginCompletion()
                }
            }
        } else {
            Log.w("LoginActivity", "No store ID available, skipping transaction sync")
            isTransactionDataLoaded = true
            isDateConversionComplete = true
            checkLoginCompletion()
        }
    }

    // ADD: Helper method to convert existing data dates without duplicating
    private suspend fun convertExistingDataDates(transactionDao: TransactionDao) {
        try {
            Log.d("LoginSync", "Converting dates for existing data...")

            // Get all existing data
            val existingSummaries = transactionDao.getAllTransactionSummaries()
            val existingRecords = transactionDao.getAllTransactionRecords()

            var summariesConverted = 0
            var recordsConverted = 0

            // Convert summary dates if needed
            existingSummaries.forEach { summary ->
                if (summary.createdDate.contains("T") && summary.createdDate.contains("Z")) {
                    try {
                        val convertedDate = summary.createdDate.convertApiDateToSimple()
                        val updatedSummary = summary.copy(
                            createdDate = convertedDate,
                            // Handle nulls during conversion
                            paymentMethod = summary.paymentMethod ?: "",
                            customerAccount = summary.customerAccount ?: "",
                            staff = summary.staff ?: "",
                            comment = summary.comment ?: "",
                            currency = summary.currency ?: "PHP",
                            receiptEmail = summary.receiptEmail ?: "",
                            markupDescription = summary.markupDescription ?: "",
                            discountType = summary.discountType ?: "",
                            customerName = summary.customerName ?: "",
                            refundReceiptId = summary.refundReceiptId ?: "",
                            zReportId = summary.zReportId ?: "",
                            storeKey = summary.storeKey ?: "",
                            storeSequence = summary.storeSequence ?: ""
                        )
                        transactionDao.updateTransactionSummary(updatedSummary)
                        summariesConverted++
                    } catch (e: Exception) {
                        Log.e("LoginSync", "Error converting summary date: ${e.message}")
                    }
                }
            }

            // Convert record dates if needed
            existingRecords.forEach { record ->
                if (record.createdDate?.contains("T") == true && record.createdDate.contains("Z")) {
                    try {
                        val convertedDate = record.createdDate.convertApiDateToSimple()
                        val updatedRecord = record.copy(
                            createdDate = convertedDate,
                            // Handle nulls during conversion
                            name = record.name ?: "",
                            receiptNumber = record.receiptNumber ?: "",
                            paymentMethod = record.paymentMethod ?: "",
                            comment = record.comment ?: "",
                            receiptId = record.receiptId ?: "",
                            itemId = record.itemId ?: "",
                            itemGroup = record.itemGroup ?: "",
                            customerAccount = record.customerAccount ?: "",
                            store = record.store ?: "",
                            staff = record.staff ?: "",
                            discountOfferId = record.discountOfferId ?: "",
                            unit = record.unit ?: "PCS",
                            remarks = record.remarks ?: "",
                            inventoryBatchId = record.inventoryBatchId ?: "",
                            inventoryBatchExpiryDate = record.inventoryBatchExpiryDate ?: "",
                            giftCard = record.giftCard ?: "",
                            returnTransactionId = record.returnTransactionId ?: "",
                            creditMemoNumber = record.creditMemoNumber ?: "",
                            description = record.description ?: "",
                            storeTaxGroup = record.storeTaxGroup ?: "",
                            currency = record.currency ?: "PHP",
                            storeKey = record.storeKey ?: "",
                            storeSequence = record.storeSequence ?: ""
                        )
                        transactionDao.updateTransactionRecord(updatedRecord)
                        recordsConverted++
                    } catch (e: Exception) {
                        Log.e("LoginSync", "Error converting record date: ${e.message}")
                    }
                }
            }

            Log.d("LoginSync", "Date conversion complete: $summariesConverted summaries, $recordsConverted records")

        } catch (e: Exception) {
            Log.e("LoginSync", "Error in date conversion: ${e.message}", e)
        }
    }

    // ADD: Date conversion utility functions
    private fun String.convertApiDateToSimple(): String {
        return try {
            val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US)
            apiFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = apiFormat.parse(this)

            val simpleFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            simpleFormat.timeZone = TimeZone.getTimeZone("Asia/Manila")
            simpleFormat.format(date ?: Date())
        } catch (e: Exception) {
            Log.w("DateConverter", "Could not convert date: $this")
            this
        }
    }

    private fun getCurrentDateString(): String {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            format.timeZone = TimeZone.getTimeZone("Asia/Manila")
            format.format(Date())
        } catch (e: Exception) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        }
    }

    // UPDATE your existing fetchAttendanceData method to also fetch transactions
    private fun fetchAttendanceData(storeId: String) {
        if (storeId.isNotEmpty()) {
            showLoading("Loading attendance data...")
            viewModel.fetchAttendanceData(storeId)

            // Also fetch and convert transaction data
            fetchAndConvertTransactionData(storeId)
        } else {
            Log.w("LoginActivity", "No store ID available, skipping attendance and transaction fetch")
            isAttendanceDataLoaded = true
            isTransactionDataLoaded = true
            isDateConversionComplete = true
            checkLoginCompletion()
        }
    }

    // UPDATE your checkLoginCompletion method
    private fun initializeRepositories() {
        val database = AppDatabase.getDatabase(this)
        val numberSequenceRemoteDao = database.numberSequenceRemoteDao()
        val transactionDao = database.transactionDao()

        numberSequenceRemoteRepository = NumberSequenceRemoteRepository(
            numberSequenceApi = RetrofitClient.numberSequenceApi,
            numberSequenceRemoteDao = numberSequenceRemoteDao,
            transactionDao = transactionDao  // Add this parameter
        )
    }

    // Update your checkLoginCompletion method
    private fun checkLoginCompletion() {
        val webStatus = if (isWebLoginComplete) "completed" else "pending"
        val attendanceStatus = if (isAttendanceDataLoaded) "loaded" else "loading"
        val transactionStatus = if (isTransactionDataLoaded) "loaded" else "loading"
        val dateConversionStatus = if (isDateConversionComplete) "completed" else "converting"

        Log.d("LoginActivity", "Login status - Native: $isNativeLoginComplete, Web: $webStatus, Attendance: $attendanceStatus, Transactions: $transactionStatus, Dates: $dateConversionStatus")

        if (isNativeLoginComplete && isAttendanceDataLoaded && isTransactionDataLoaded && isDateConversionComplete) {
            if (!isWebLoginComplete) {
                Log.w("LoginActivity", "Proceeding with login despite incomplete web login")
                isWebLoginComplete = true
            }

            Log.d("LoginActivity", "Login completed successfully with all data synced and dates converted")

            // Initialize number sequence before starting main activity
            initializeNumberSequenceAfterLogin()
        }
    }

    private fun initializeNumberSequenceAfterLogin() {
        lifecycleScope.launch {
            try {
                val currentUser = SessionManager.getCurrentUser()
                val storeId = currentUser?.storeid

                if (!storeId.isNullOrEmpty()) {
                    Log.d("LoginActivity", "Initializing number sequence for store: $storeId")
                    showLoading("Initializing transaction numbering...")

                    val result = numberSequenceRemoteRepository.initializeNumberSequence(storeId)
                    result.onSuccess {
                        Log.d("LoginActivity", "Number sequence initialized successfully")
                        // Also sync with server
                        numberSequenceRemoteRepository.syncNumberSequenceWithServer(storeId)
                    }.onFailure { error ->
                        Log.e("LoginActivity", "Failed to initialize number sequence", error)
                    }
                }

                // Proceed to main activity
                SessionManager.refreshSession()
                startMainActivity()

            } catch (e: Exception) {
                Log.e("LoginActivity", "Error during number sequence initialization", e)
                // Still proceed to main activity even if sequence initialization fails
                SessionManager.refreshSession()
                startMainActivity()
            }
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
