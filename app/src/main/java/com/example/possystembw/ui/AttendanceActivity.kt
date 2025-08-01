package com.example.possystembw.ui

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.media.ExifInterface
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.example.possystembw.DeviceUtils
import com.example.possystembw.R
import com.example.possystembw.DAO.AttendanceDao
import com.example.possystembw.DAO.NoInternetException
import com.example.possystembw.DAO.PhilippinesServerTime
import com.example.possystembw.DAO.ServerTimeException
import com.example.possystembw.MainActivity
import com.example.possystembw.RetrofitClient
import com.example.possystembw.adapter.StaffAttendanceAdapter
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.database.AttendanceRecord
import com.example.possystembw.database.StaffEntity
import com.example.possystembw.databinding.ActivityAttendanceBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AttendanceActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityAttendanceBinding
    private lateinit var attendanceDao: AttendanceDao
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraExecutor: ExecutorService
    private var currentAttendance: AttendanceRecord? = null
    private var selectedStaff: StaffEntity? = null
    private var selectedAttendanceType: StaffAttendanceAdapter.AttendanceType? = null
    private lateinit var adapter: StaffAttendanceAdapter
    private var capturedPhotoFile: File? = null
    private var selectedDate: Calendar = Calendar.getInstance()
    private var selectedButton: Button? = null

    private lateinit var timeUpdateHandler: Handler
    private lateinit var timeUpdateRunnable: Runnable

    private lateinit var attendanceManager: AttendanceManager

    // Server attendance data
    private var serverAttendanceData: Map<String, com.example.possystembw.DAO.ServerAttendanceRecord> = emptyMap()

    // Mobile layout components
    private var drawerLayout: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    private var hamburgerButton: ImageButton? = null
    private var optionsButton: ImageButton? = null
    private var isMobileLayout = false

    // Tablet layout components
    private lateinit var sidebarLayout: ConstraintLayout
    private lateinit var toggleButton: ImageButton
    private lateinit var buttonContainer: LinearLayout
    private lateinit var ecposTitle: TextView
    private var isSidebarExpanded = true
    private var isAnimating = false

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set orientation based on device
        DeviceUtils.setOrientationBasedOnDevice(this)

        binding = ActivityAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            DeviceUtils.setOrientationBasedOnDevice(this)

            // Detect layout type
            detectLayoutType()

            // Initialize layout-specific views
            initializeLayoutSpecificViews()

            attendanceManager = AttendanceManager(this)

            // Initialize Handler and Runnable first
            initializeTimeUpdate()

            // Setup layout-specific components
            if (isMobileLayout) {
                setupMobileSpecificFeatures()
            } else {
                initializeSidebarComponents()
                setupSidebar()
            }

            swipeRefreshLayout = binding.swipeRefreshLayout
            setupSwipeRefresh()

            // Show loading state
            showLoading("Initializing...")

            // Initialize basic components
            attendanceDao = AppDatabase.getDatabase(application).attendanceDao()
            cameraExecutor = Executors.newSingleThreadExecutor()

            // Start initialization process
            lifecycleScope.launch {
                try {
                    initializeAttendanceSystem()
                } catch (e: Exception) {
                    handleInitializationError(e)
                }
            }

            Log.d(TAG, "✅ onCreate completed successfully for ${if (isMobileLayout) "mobile" else "tablet"} mode")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during onCreate", e)
            Toast.makeText(this, "Initialization Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun detectLayoutType() {
        // Check what views actually exist in the loaded layout
        val drawerLayoutView = findViewById<DrawerLayout>(R.id.drawer_layout)
        val sidebarLayoutView = findViewById<ConstraintLayout>(R.id.sidebarLayout)

        val isTabletDevice = DeviceUtils.isTablet(this)
        val hasDrawer = drawerLayoutView != null
        val hasSidebar = sidebarLayoutView != null

        Log.d(TAG, "=== LAYOUT DETECTION ===")
        Log.d(TAG, "Device type: ${if (isTabletDevice) "Tablet" else "Phone"}")
        Log.d(TAG, "Has DrawerLayout: $hasDrawer")
        Log.d(TAG, "Has SidebarLayout: $hasSidebar")

        // Determine layout type based on actual layout loaded
        isMobileLayout = hasDrawer && !hasSidebar

        Log.d(TAG, "Final decision: ${if (isMobileLayout) "Mobile" else "Tablet"} mode")
    }

    private fun initializeLayoutSpecificViews() {
        if (isMobileLayout) {
            // Mobile-specific views
            drawerLayout = findViewById(R.id.drawer_layout)
            navigationView = findViewById(R.id.nav_view)
            hamburgerButton = findViewById(R.id.hamburgerButton)
            optionsButton = findViewById(R.id.optionsButton)

            Log.d(TAG, "✅ Mobile views initialized")
            Log.d(TAG, "DrawerLayout: ${drawerLayout != null}")
            Log.d(TAG, "NavigationView: ${navigationView != null}")
            Log.d(TAG, "HamburgerButton: ${hamburgerButton != null}")
            Log.d(TAG, "OptionsButton: ${optionsButton != null}")
        } else {
            // Tablet-specific views will be initialized in existing methods
            Log.d(TAG, "✅ Tablet layout detected")
        }
    }

    private fun setupMobileSpecificFeatures() {
        try {
            // Setup navigation drawer
            navigationView?.setNavigationItemSelectedListener(this)

            // Update store name in navigation header
            navigationView?.getHeaderView(0)?.let { headerView ->
                val navStoreName = headerView.findViewById<TextView>(R.id.nav_store_name)
                val currentStore = SessionManager.getCurrentUser()?.storeid ?: "Unknown Store"
                navStoreName?.text = "Store: $currentStore"
            }

            // Setup hamburger button
            hamburgerButton?.setOnClickListener {
                drawerLayout?.openDrawer(GravityCompat.START)
            }

            // Setup options button
            optionsButton?.setOnClickListener {
                showMobileOptionsDialog()
            }

            Log.d(TAG, "✅ Mobile features setup complete")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Mobile features setup failed", e)
        }
    }

    private fun showMobileOptionsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_attendance_options, null)

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle4)
            .setView(dialogView)
            .create()

        // Apply custom background
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set up click listeners for each option card
        setupAttendanceOptionClickListeners(dialogView, dialog)

        // Cancel button
        dialogView.findViewById<ImageButton>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        // Add entrance animation
        dialogView.alpha = 0f
        dialogView.scaleX = 0.8f
        dialogView.scaleY = 0.8f
        dialogView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun setupAttendanceOptionClickListeners(dialogView: View, dialog: AlertDialog) {
        // Refresh Data option
        dialogView.findViewById<CardView>(R.id.refreshCard)?.setOnClickListener {
            animateCardClick(it) {
                dialog.dismiss()
                refreshAttendanceData()
            }
        }

        // Attendance History option
        dialogView.findViewById<CardView>(R.id.historyCard)?.setOnClickListener {
            animateCardClick(it) {
                dialog.dismiss()
                // Show staff selection for history
                showStaffSelectionForHistory()
            }
        }

        // Settings option
        dialogView.findViewById<CardView>(R.id.settingsCard)?.setOnClickListener {
            animateCardClick(it) {
                dialog.dismiss()
                val intent = Intent(this, PrinterSettingsActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun showStaffSelectionForHistory() {
        lifecycleScope.launch {
            try {
                val storeId = SessionManager.getCurrentUser()?.storeid ?: return@launch
                val staffList = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(application).staffDao().getStaffByStore(storeId)
                }

                val staffNames = staffList.map { it.name }.toTypedArray()

                val builder = AlertDialog.Builder(this@AttendanceActivity, R.style.CustomDialogStyle3)
                    .setTitle("Select Staff for History")
                    .setItems(staffNames) { _, which ->
                        val selectedStaff = staffList[which]
                        openAttendanceHistory(selectedStaff)
                    }
                    .setNegativeButton("Cancel", null)

                val dialog = builder.create()
                dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                dialog.show()

                // Apply mobile styling
                applyMobileDialogStyling(dialog)

            } catch (e: Exception) {
                Log.e(TAG, "Error showing staff selection", e)
                showToast("Error loading staff list")
            }
        }
    }

    private fun animateCardClick(view: View, action: () -> Unit) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .withEndAction {
                        action()
                    }
                    .start()
            }
            .start()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_pos_system -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_reports -> {
                val intent = Intent(this, ReportsActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_stock_counting -> {
                val intent = Intent(this, StockCountingActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_web_reports -> {
                navigateToMainWithUrl("https://eljin.org/reports", "REPORTS")
            }
            R.id.nav_customers -> {
                navigateToMainWithUrl("https://eljin.org/customers", "CUSTOMER")
            }
            R.id.nav_loyalty_card -> {
                navigateToMainWithUrl("https://eljin.org/loyalty-cards", "Loyalty Card")
            }
            R.id.nav_stock_transfer -> {
                navigateToMainWithUrl("https://eljin.org/StockTransfer", "Stock Transfer")
            }
            R.id.nav_printer_settings -> {
                val intent = Intent(this, PrinterSettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_logout -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        drawerLayout?.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        when {
            isMobileLayout && drawerLayout?.isDrawerOpen(GravityCompat.START) == true -> {
                drawerLayout?.closeDrawer(GravityCompat.START)
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    private fun navigateToMainWithUrl(url: String?, message: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            url?.let { putExtra("web_url", it) }
        }
        message?.let { showToast(it) }
        startActivity(intent)
    }

    private fun applyMobileDialogStyling(dialog: AlertDialog) {
        if (!isMobileLayout) return

        try {
            // Adjust button text sizes and padding
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { button ->
                button.textSize = 12f
                button.setPadding(12, 8, 12, 8)
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { button ->
                button.textSize = 12f
                button.setPadding(12, 8, 12, 8)
                button.text = "Cancel"
            }

            // Adjust dialog title
            val titleView = dialog.findViewById<TextView>(android.R.id.title)
            titleView?.let { title ->
                title.textSize = 14f
                title.setPadding(16, 12, 16, 8)
            }

            // Adjust dialog width for mobile
            dialog.window?.let { window ->
                val layoutParams = window.attributes
                val displayMetrics = resources.displayMetrics
                layoutParams.width = (displayMetrics.widthPixels * 0.9).toInt()
                window.attributes = layoutParams
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error applying mobile dialog styling", e)
        }
    }

    private fun createMobileDialog(title: String, content: View): AlertDialog {
        val titleView = TextView(this@AttendanceActivity).apply {
            text = title
            textSize = if (isMobileLayout) 16f else 18f
            setTextColor(ContextCompat.getColor(this@AttendanceActivity, android.R.color.black))
            setPadding(
                if (isMobileLayout) 20 else 24,  // left
                if (isMobileLayout) 16 else 20,  // top
                if (isMobileLayout) 20 else 24,  // right
                if (isMobileLayout) 8 else 12    // bottom
            )
            gravity = Gravity.CENTER_VERTICAL
            setTypeface(null, Typeface.BOLD)
        }

        val dialog = AlertDialog.Builder(this@AttendanceActivity, R.style.CustomDialogStyle3)
            .setCustomTitle(titleView)
            .setView(content)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        // Apply mobile-specific sizing
        if (isMobileLayout) {
            dialog.setOnShowListener {
                applyMobileDialogStyling(dialog)
            }
        }

        return dialog
    }

    private fun openAttendanceHistory(staff: StaffEntity) {
        val intent = Intent(this, AttendanceHistoryActivity::class.java).apply {
            putExtra("staffId", "${staff.name}_${staff.storeId}")
            putExtra("staffName", staff.name)
            putExtra("storeId", staff.storeId)
        }
        startActivity(intent)
    }

    private fun initializeTimeUpdate() {
        timeUpdateHandler = Handler(Looper.getMainLooper())
        timeUpdateRunnable = object : Runnable {
            override fun run() {
                try {
                    binding.currentTime.text = PhilippinesServerTime.formatDisplayTime()
                    binding.currentDate.text = PhilippinesServerTime.formatDisplayDate()
                    timeUpdateHandler.postDelayed(this, 1000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating time display", e)
                }
            }
        }
    }

    private suspend fun initializeAttendanceSystem() {
        withContext(Dispatchers.Main) {
            showLoading("Checking internet connection...")
        }

        // Check internet connection
        if (!PhilippinesServerTime.isInternetAvailable(this)) {
            throw NoInternetException("Internet connection required for attendance")
        }

        // Load server attendance data first
        withContext(Dispatchers.Main) {
            showLoading("Loading attendance data...")
        }
        loadServerAttendanceData()

        // Initialize components
        withContext(Dispatchers.Main) {
            showLoading("Setting up attendance system...")

            setupButtonListeners()
            setupRecyclerView()
            setupTimeAndDate()
            setupWeekDayButtons()
            setupInitialViews()
            checkPermissions()

            // Hide loading and show main content
            hideLoading()
            binding.mainContent.visibility = View.VISIBLE
        }

        // Load initial data
        loadStaffList()
    }

    // Continue with all your existing methods but update dialogs to use mobile styling...

    private fun verifyPasscode(
        staff: StaffEntity,
        action: AttendanceAction,
        onSuccess: () -> Unit
    ) {
        val dialogView = layoutInflater.inflate(R.layout.passcode_dialog, null)
        val titleText = dialogView.findViewById<TextView>(R.id.titleText)
        val passcodeInput = dialogView.findViewById<EditText>(R.id.passcodeInput)
        val confirmButton = dialogView.findViewById<Button>(R.id.confirmButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        // Apply mobile-specific styling
        if (isMobileLayout) {
            passcodeInput.textSize = 16f
            confirmButton.textSize = 14f
            cancelButton.textSize = 14f
            titleText.textSize = 16f
        }

        // Set title based on action
        titleText.text = when (action) {
            AttendanceAction.TIME_IN -> "Enter Passcode to Time In"
            AttendanceAction.TIME_OUT -> "Enter Passcode to Time Out"
            AttendanceAction.BREAK -> "Enter Passcode for Break"
        }

        val dialog = createMobileDialog("Passcode Verification", dialogView)

        confirmButton.setOnClickListener {
            val enteredPasscode = passcodeInput.text.toString()
            if (enteredPasscode == staff.passcode) {
                dialog.dismiss()
                onSuccess()
            } else {
                passcodeInput.error = "Invalid passcode"
                passcodeInput.setText("")
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAttendanceInfoDialog(title: String, message: String, note: String) {
        val builder = AlertDialog.Builder(this, R.style.CustomDialogStyle3)
            .setTitle(title)
            .setMessage("$message\n\n$note")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setIcon(R.drawable.baseline_person_24)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()

        // Apply mobile styling
        applyMobileDialogStyling(dialog)
    }

    // Include all your existing methods here, but make sure to apply mobile dialog styling
    // to any AlertDialog.Builder calls by adding:
    // dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
    // dialog.show()
    // applyMobileDialogStyling(dialog)

    private suspend fun loadServerAttendanceData() {
        try {
            val storeId = SessionManager.getCurrentUser()?.storeid ?: return

            // During swipe refresh, always fetch from server (skip cache)
            val isSwipeRefresh = swipeRefreshLayout.isRefreshing

            if (!isSwipeRefresh) {
                // Try to get cached data first (only when not doing swipe refresh)
                val cachedData = SessionManager.getAttendanceData()
                if (cachedData.isNotEmpty() && SessionManager.isAttendanceDataFresh()) {
                    Log.d(TAG, "Using cached attendance data: ${cachedData.size} records")
                    serverAttendanceData = cachedData.associateBy { "${it.staffId}_${it.date}" }
                    syncToLocalDatabase(cachedData)
                    return
                }
            }

            // Fetch from server
            Log.d(TAG, "Fetching fresh attendance data from server...")
            val result = RetrofitClient.attendanceService.getStoreAttendanceRecords(storeId)

            if (result.isSuccess) {
                val attendanceList = result.getOrNull() ?: emptyList()
                Log.d(TAG, "Loaded ${attendanceList.size} attendance records from server")

                // Cache the data
                SessionManager.setAttendanceData(attendanceList)

                // Create a map for quick lookup: "staffId_date" -> AttendanceRecord
                serverAttendanceData = attendanceList.associateBy { "${it.staffId}_${it.date}" }

                // Sync to local database
                syncToLocalDatabase(attendanceList)

            } else {
                Log.e(TAG, "Failed to load attendance data: ${result.exceptionOrNull()?.message}")
                if (!isSwipeRefresh) {
                    withContext(Dispatchers.Main) {
                        showToast("Failed to load attendance data from server")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading server attendance data", e)
            if (!swipeRefreshLayout.isRefreshing) {
                withContext(Dispatchers.Main) {
                    showToast("Error loading attendance data: ${e.message}")
                }
            }
        }
    }

    private suspend fun syncToLocalDatabase(serverData: List<com.example.possystembw.DAO.ServerAttendanceRecord>) {
        withContext(Dispatchers.IO) {
            try {
                serverData.forEach { serverRecord ->
                    // Convert server record to local record
                    val localRecord = AttendanceRecord(
                        id = serverRecord.id.toLong(),
                        staffId = serverRecord.staffId,
                        storeId = serverRecord.storeId,
                        date = serverRecord.date,
                        timeIn = serverRecord.timeIn ?: "",
                        timeInPhoto = serverRecord.timeInPhoto ?: "",
                        breakIn = serverRecord.breakIn,
                        breakInPhoto = serverRecord.breakInPhoto,
                        breakOut = serverRecord.breakOut,
                        breakOutPhoto = serverRecord.breakOutPhoto,
                        timeOut = serverRecord.timeOut,
                        timeOutPhoto = serverRecord.timeOutPhoto,
                        status = serverRecord.status
                    )

                    // Check if record exists
                    val existing = attendanceDao.getAttendanceForStaffOnDate(
                        serverRecord.staffId,
                        serverRecord.date
                    )

                    if (existing == null) {
                        attendanceDao.insertAttendance(localRecord)
                    } else {
                        attendanceDao.updateAttendance(localRecord)
                    }
                }
                Log.d(TAG, "Synced ${serverData.size} records to local database")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing to local database", e)
            }
        }
    }

    private fun loadAttendanceForDate(date: Calendar) {
        lifecycleScope.launch {
            try {
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date.time)
                val storeId = SessionManager.getCurrentUser()?.storeid ?: return@launch

                withContext(Dispatchers.IO) {
                    val staffList = AppDatabase.getDatabase(application)
                        .staffDao()
                        .getStaffByStore(storeId)

                    withContext(Dispatchers.Main) {
                        adapter.updateDate(dateStr)
                        adapter.updateServerAttendanceData(serverAttendanceData)
                        adapter.submitList(staffList)
                        adapter.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading attendance for date", e)
                showToast("Failed to load attendance data")
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            refreshAttendanceData()
        }

        // Customize colors
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        // Set background color
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(android.R.color.white)
    }

    private fun refreshAttendanceData() {
        lifecycleScope.launch {
            try {
                // Don't show the main loading dialog during swipe refresh
                Log.d(TAG, "Starting swipe refresh...")

                // Check internet connection first
                if (!PhilippinesServerTime.isInternetAvailable(this@AttendanceActivity)) {
                    swipeRefreshLayout.isRefreshing = false
                    showToast("No internet connection")
                    return@launch
                }

                // Clear cached attendance data to force fresh fetch
                SessionManager.clearAttendanceData()

                // Reload server attendance data
                loadServerAttendanceData()

                // Refresh the current view
                loadAttendanceForDate(selectedDate)

                // Stop the refresh animation
                swipeRefreshLayout.isRefreshing = false

                showToast("Attendance data refreshed")
                Log.d(TAG, "Swipe refresh completed successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error during swipe refresh", e)
                swipeRefreshLayout.isRefreshing = false
                showToast("Failed to refresh: ${e.message}")
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = StaffAttendanceAdapter(this) { staff, attendanceType ->
            swipeRefreshLayout.isEnabled = false

            selectedStaff = staff
            selectedAttendanceType = attendanceType

            // Check attendance status first before any action
            lifecycleScope.launch {
                val attendanceRecord = getAttendanceRecord(staff)
                val today = PhilippinesServerTime.formatDatabaseDate()
                val selectedDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
                val isToday = selectedDateStr == today

                when (attendanceType) {
                    StaffAttendanceAdapter.AttendanceType.TIME_IN -> {
                        val hasTimeIn = attendanceRecord?.timeIn != null && attendanceRecord.timeIn.isNotEmpty()

                        when {
                            hasTimeIn -> {
                                // Re-enable swipe refresh
                                swipeRefreshLayout.isEnabled = true
                                if (attendanceRecord != null) {
                                    showAttendanceInfoDialog(
                                        "Time In Already Recorded",
                                        "Time In: ${attendanceRecord.timeIn}\nDate: ${attendanceRecord.date}",
                                        ""
                                    )
                                }
                                return@launch
                            }
                            !isToday -> {
                                swipeRefreshLayout.isEnabled = true
                                showToast("Can only record Time In for today")
                                return@launch
                            }
                            else -> {
                                verifyPasscode(staff, AttendanceAction.TIME_IN) {
                                    if (allPermissionsGranted()) {
                                        binding.viewFinder.visibility = View.VISIBLE
                                        setupCamera()
                                    } else {
                                        ActivityCompat.requestPermissions(
                                            this@AttendanceActivity,
                                            REQUIRED_PERMISSIONS,
                                            REQUEST_CODE_PERMISSIONS
                                        )
                                    }
                                }
                            }
                        }
                    }

                    StaffAttendanceAdapter.AttendanceType.TIME_OUT -> {
                        val hasTimeIn = attendanceRecord?.timeIn != null && attendanceRecord.timeIn.isNotEmpty()
                        val hasTimeOut = attendanceRecord?.timeOut != null && attendanceRecord.timeOut.isNotEmpty()
                        val isOnBreak = attendanceRecord?.breakIn != null && attendanceRecord.breakOut == null

                        when {
                            !hasTimeIn -> {
                                showToast("Please Time In first")
                                return@launch
                            }
                            hasTimeOut -> {
                                // Already has time out - show info, DO NOT allow override
                                if (attendanceRecord != null) {
                                    showAttendanceInfoDialog(
                                        "Time Out Already Recorded",
                                        "Time Out: ${attendanceRecord.timeOut}\nDate: ${attendanceRecord.date}",
                                        ""
                                    )
                                }
                                return@launch
                            }
                            isOnBreak -> {
                                showToast("Please end break first before timing out")
                                return@launch
                            }
                            !isToday -> {
                                showToast("Can only record Time Out for today")
                                return@launch
                            }
                            else -> {
                                // Can actually time out - verify passcode
                                verifyPasscode(staff, AttendanceAction.TIME_OUT) {
                                    if (allPermissionsGranted()) {
                                        binding.viewFinder.visibility = View.VISIBLE
                                        setupCamera()
                                    } else {
                                        ActivityCompat.requestPermissions(
                                            this@AttendanceActivity,
                                            REQUIRED_PERMISSIONS,
                                            REQUEST_CODE_PERMISSIONS
                                        )
                                    }
                                }
                            }
                        }
                    }

                    StaffAttendanceAdapter.AttendanceType.BREAK_STATUS -> {
                        val hasTimeIn = attendanceRecord?.timeIn != null && attendanceRecord.timeIn.isNotEmpty()
                        val hasTimeOut = attendanceRecord?.timeOut != null && attendanceRecord.timeOut.isNotEmpty()
                        val breakCompleted = attendanceRecord?.breakIn != null && attendanceRecord.breakOut != null
                        val isOnBreak = attendanceRecord?.breakIn != null && attendanceRecord.breakOut == null

                        when {
                            !hasTimeIn -> {
                                showToast("Please Time In first")
                                return@launch
                            }
                            hasTimeOut -> {
                                if (attendanceRecord != null) {
                                    showAttendanceInfoDialog(
                                        "Shift Already Completed",
                                        "Time Out: ${attendanceRecord.timeOut}\nDate: ${attendanceRecord.date}",
                                        ""
                                    )
                                }
                                return@launch
                            }
                            breakCompleted -> {
                                // Break already completed - show info, DO NOT allow override
                                val breakDuration = calculateBreakDuration(attendanceRecord?.breakIn!!, attendanceRecord.breakOut!!)
                                if (attendanceRecord != null) {
                                    showAttendanceInfoDialog(
                                        "Break Already Completed",
                                        "Break In: ${attendanceRecord.breakIn}\nBreak Out: ${attendanceRecord.breakOut}\nDuration: $breakDuration",
                                        ""
                                    )
                                }
                                return@launch
                            }
                            !isToday -> {
                                showToast("Can only manage break status for today")
                                return@launch
                            }
                            else -> {
                                // Can actually manage break - verify passcode
                                val action = if (isOnBreak) "End Break" else "Start Break"
                                val currentStatus = if (isOnBreak) "Currently on break since ${attendanceRecord?.breakIn}" else "Ready to start break"

                                val builder = AlertDialog.Builder(this@AttendanceActivity, R.style.CustomDialogStyle3)
                                    .setTitle("Break Status")
                                    .setMessage("$currentStatus\n\nDo you want to $action?")
                                    .setPositiveButton("Yes") { _, _ ->
                                        verifyPasscode(staff, AttendanceAction.BREAK) {
                                            handleBreakStatus(staff)
                                        }
                                    }
                                    .setNegativeButton("No") { dialog, _ ->
                                        dialog.dismiss()
                                    }

                                val dialog = builder.create()
                                dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                                dialog.show()
                                applyMobileDialogStyling(dialog)
                            }
                        }
                    }

                    else -> {}
                }
            }
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@AttendanceActivity)
            adapter = this@AttendanceActivity.adapter
        }
    }

    private fun calculateBreakDuration(breakIn: String, breakOut: String): String {
        return try {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val startTime = timeFormat.parse(breakIn)
            val endTime = timeFormat.parse(breakOut)

            if (startTime != null && endTime != null) {
                val durationMinutes = (endTime.time - startTime.time) / (60 * 1000)
                val hours = durationMinutes / 60
                val minutes = durationMinutes % 60

                when {
                    hours > 0 -> "${hours}h ${minutes}m"
                    else -> "${minutes}m"
                }
            } else {
                "Unknown duration"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating break duration", e)
            "Unknown duration"
        }
    }

    private suspend fun getAttendanceRecord(staff: StaffEntity): com.example.possystembw.DAO.ServerAttendanceRecord? {
        return try {
            val staffId = "${staff.name}_${staff.storeId}"
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
            val key = "${staffId}_${dateStr}"

            // First try server data
            serverAttendanceData[key]?.let { return it }

            // Fallback to local database
            val localRecord = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(application).attendanceDao()
                    .getAttendanceForStaffOnDate(staffId, dateStr)
            }

            localRecord?.let {
                com.example.possystembw.DAO.ServerAttendanceRecord(
                    id = it.id.toInt(),
                    staffId = it.staffId,
                    storeId = it.storeId,
                    date = it.date,
                    timeIn = it.timeIn.takeIf { time -> time.isNotEmpty() },
                    timeInPhoto = it.timeInPhoto.takeIf { photo -> photo.isNotEmpty() },
                    breakIn = it.breakIn,
                    breakInPhoto = it.breakInPhoto,
                    breakOut = it.breakOut,
                    breakOutPhoto = it.breakOutPhoto,
                    timeOut = it.timeOut,
                    timeOutPhoto = it.timeOutPhoto,
                    status = it.status,
                    created_at = "",
                    updated_at = ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting attendance record", e)
            null
        }
    }

    private fun handleInitializationError(error: Exception) {
        when (error) {
            is NoInternetException -> showNoInternetDialog()
            is ServerTimeException -> showErrorDialog(
                "Time Sync Failed",
                "Could not synchronize with server time. This is required to prevent attendance manipulation."
            )
            else -> showErrorDialog(
                "Initialization Failed",
                "Could not initialize attendance system: ${error.message}"
            )
        }
    }

    private fun showNoInternetDialog() {
        val builder = AlertDialog.Builder(this, R.style.CustomDialogStyle3)
            .setTitle("No Internet Connection")
            .setMessage("Internet connection is required to ensure accurate attendance timing. Please connect to the internet and try again.")
            .setPositiveButton("Retry") { _, _ ->
                recreate()
            }
            .setNegativeButton("Close") { _, _ ->
                finish()
            }
            .setCancelable(false)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
        applyMobileDialogStyling(dialog)
    }

    private fun showErrorDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this, R.style.CustomDialogStyle3)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Retry") { _, _ ->
                recreate()
            }
            .setNegativeButton("Close") { _, _ ->
                finish()
            }
            .setCancelable(false)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
        applyMobileDialogStyling(dialog)
    }

    private fun setupInitialViews() {
        binding.apply {
            // Hide camera-related views initially
            viewFinder.visibility = View.GONE
            previewImage.visibility = View.GONE
            captureButton.visibility = View.GONE
            confirmButton.visibility = View.GONE
            retakeButton.visibility = View.GONE

            // Set current time and date
            val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

            currentTime.text = timeFormat.format(Date())
            currentDate.text = dateFormat.format(Date())
        }
    }

    private fun setupWeekDayButtons() {
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY

        // Get today's day of week (1 = Sunday, 2 = Monday, etc.)
        val today = calendar.get(Calendar.DAY_OF_WEEK)
        // Convert to index (0 = Monday, 6 = Sunday)
        val todayIndex = if (today == Calendar.SUNDAY) 6 else today - 2

        binding.weekDaysContainer.removeAllViews()

        days.forEachIndexed { index, dayName ->
            val button = Button(this).apply {
                id = View.generateViewId()
                text = dayName
                background = ContextCompat.getDrawable(
                    this@AttendanceActivity,
                    R.drawable.day_button_background
                )
                setTextColor(
                    ContextCompat.getColorStateList(
                        this@AttendanceActivity,
                        R.color.black
                    )
                )

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = resources.getDimensionPixelSize(R.dimen.day_button_margin)
                    marginEnd = resources.getDimensionPixelSize(R.dimen.day_button_margin)
                }

                // Apply mobile-specific styling
                if (isMobileLayout) {
                    textSize = 10f
                    setPadding(8, 4, 8, 4)
                }

                // Set initial selection for current day
                if (index == todayIndex) {
                    isSelected = true
                    selectedButton = this
                    // Trigger the selection handler for today's date
                    handleDaySelection(this, index)
                }

                setOnClickListener {
                    handleDaySelection(this, index)
                }
            }
            binding.weekDaysContainer.addView(button)
        }

        // Scroll to today's button
        binding.weekDaysScroll.post {
            // Calculate scroll position to center today's button
            val todayButton = binding.weekDaysContainer.getChildAt(todayIndex)
            val scrollX = todayButton.left - (binding.weekDaysScroll.width - todayButton.width) / 2
            binding.weekDaysScroll.smoothScrollTo(scrollX.coerceAtLeast(0), 0)
        }
    }

    private fun handleDaySelection(button: Button, dayIndex: Int) {
        // Reset previous selection
        selectedButton?.isSelected = false

        // Set new selection
        button.isSelected = true
        selectedButton = button

        // Calculate selected date
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY

        // Get the current week's Monday
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        // Add days to get to selected day
        calendar.add(Calendar.DAY_OF_WEEK, dayIndex)

        selectedDate = calendar

        // Format date for adapter
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        // Update adapter with new date
        adapter.updateDate(dateStr)

        // Load attendance for selected date
        loadAttendanceForDate(selectedDate)
    }

    private fun setupButtonListeners() {
        binding.apply {
            captureButton.setOnClickListener {
                captureImage()
            }

            confirmButton.setOnClickListener {
                capturedPhotoFile?.let { file ->
                    when (selectedAttendanceType) {
                        StaffAttendanceAdapter.AttendanceType.TIME_IN -> handleTimeIn(file)
                        StaffAttendanceAdapter.AttendanceType.TIME_OUT -> handleTimeOut(file)
                        else -> cleanupCamera()
                    }
                }
                hidePreviewAndButtons()
            }

            retakeButton.setOnClickListener {
                // Hide preview and show camera
                previewImage.visibility = View.GONE
                confirmButton.visibility = View.GONE
                retakeButton.visibility = View.GONE
                viewFinder.visibility = View.VISIBLE
                captureButton.visibility = View.VISIBLE

                // Delete the previous capture
                capturedPhotoFile?.delete()
                capturedPhotoFile = null
            }
        }
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraPreview()
            } catch (e: Exception) {
                Log.e(TAG, "Camera setup failed", e)
                showToast("Failed to setup camera")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraPreview() {
        try {
            // Unbind all previous use cases
            cameraProvider.unbindAll()

            val preview = Preview.Builder().build()
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(binding.viewFinder.display.rotation)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture
            )

            binding.viewFinder.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)

            // Show capture button
            binding.captureButton.visibility = View.VISIBLE

        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed", e)
            showToast("Failed to initialize camera")
        }
    }

    private fun captureImage() {
        val imageCaptureInstance = imageCapture
        if (imageCaptureInstance == null) {
            showToast("Camera is not ready. Please try again.")
            return
        }

        val photoFile = createImageFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        binding.progressIndicator.visibility = View.VISIBLE

        imageCaptureInstance.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Add timestamp overlay to the captured image
                    lifecycleScope.launch {
                        try {
                            val timestampedFile = addTimestampToImage(photoFile)
                            capturedPhotoFile = timestampedFile
                            showPreviewAndConfirmation(timestampedFile)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error adding timestamp to image", e)
                            // Fallback to original image if timestamp overlay fails
                            capturedPhotoFile = photoFile
                            showPreviewAndConfirmation(photoFile)
                        }
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed", exc)
                    binding.progressIndicator.visibility = View.GONE
                    showToast("Failed to capture photo: ${exc.message}")
                }
            }
        )
    }

    private suspend fun addTimestampToImage(originalFile: File): File = withContext(Dispatchers.IO) {
        try {
            // Read the original image
            val originalBitmap = BitmapFactory.decodeFile(originalFile.absolutePath)
                ?: throw Exception("Could not decode image file")

            // Create a mutable copy of the bitmap
            val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)

            // Get current date and time using your Philippines server time
            val currentDateTime = getCurrentDisplayDateTime()
            val staffName = selectedStaff?.name ?: "Unknown Staff"
            val attendanceType = when (selectedAttendanceType) {
                StaffAttendanceAdapter.AttendanceType.TIME_IN -> "TIME IN"
                StaffAttendanceAdapter.AttendanceType.TIME_OUT -> "TIME OUT"
                StaffAttendanceAdapter.AttendanceType.BREAK_STATUS -> "BREAK"
                else -> "ATTENDANCE"
            }

            // Create timestamp text
            val timestampText = """
            $currentDateTime
            $staffName - $attendanceType
            Store: ${SessionManager.getCurrentUser()?.storeid ?: ""}
        """.trimIndent()

            // Setup paint for the background rectangle
            val backgroundPaint = Paint().apply {
                color = Color.parseColor("#CC000000") // Semi-transparent black
                style = Paint.Style.FILL
            }

            // Setup paint for the text
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = mutableBitmap.width * 0.04f // Responsive text size based on image width
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
                textAlign = Paint.Align.LEFT
            }

            // Measure text dimensions
            val textBounds = Rect()
            val lines = timestampText.split("\n")
            var maxTextWidth = 0f
            val lineHeight = textPaint.fontMetrics.let { it.descent - it.ascent }
            val totalTextHeight = lineHeight * lines.size

            // Find the maximum width among all lines
            lines.forEach { line ->
                textPaint.getTextBounds(line, 0, line.length, textBounds)
                maxTextWidth = maxOf(maxTextWidth, textBounds.width().toFloat())
            }

            // Calculate position (bottom-left corner with some padding)
            val padding = mutableBitmap.width * 0.02f
            val rectLeft = padding
            val rectTop = mutableBitmap.height - totalTextHeight - (padding * 2)
            val rectRight = rectLeft + maxTextWidth + (padding * 2)
            val rectBottom = mutableBitmap.height.toFloat() - padding

            // Draw background rectangle
            canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, backgroundPaint)

            // Draw each line of text
            var currentY = rectTop + padding + textPaint.textSize
            lines.forEach { line ->
                canvas.drawText(line, rectLeft + padding, currentY, textPaint)
                currentY += lineHeight
            }

            // Create new file for timestamped image
            val timestampedFile = File(
                originalFile.parent,
                "timestamped_${originalFile.name}"
            )

            // Save the bitmap with timestamp overlay
            FileOutputStream(timestampedFile).use { out ->
                mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            // Copy EXIF data from original to timestamped image
            try {
                val originalExif = ExifInterface(originalFile.absolutePath)
                val timestampedExif = ExifInterface(timestampedFile.absolutePath)

                // Copy important EXIF attributes
                val attributes = arrayOf(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.TAG_DATETIME,
                    ExifInterface.TAG_MAKE,
                    ExifInterface.TAG_MODEL,
                    ExifInterface.TAG_GPS_LATITUDE,
                    ExifInterface.TAG_GPS_LONGITUDE
                )

                attributes.forEach { attribute ->
                    val value = originalExif.getAttribute(attribute)
                    if (value != null) {
                        timestampedExif.setAttribute(attribute, value)
                    }
                }
                timestampedExif.saveAttributes()
            } catch (e: Exception) {
                Log.w(TAG, "Could not copy EXIF data", e)
            }

            // Clean up original file
            originalFile.delete()

            timestampedFile
        } catch (e: Exception) {
            Log.e(TAG, "Error adding timestamp overlay", e)
            throw e
        }
    }

    private fun getCurrentDisplayDateTime(): String {
        return try {
            // Use your existing Philippines server time if available
            val currentTime = PhilippinesServerTime.formatDisplayTime()
            val currentDate = PhilippinesServerTime.formatDisplayDate()
            "$currentDate $currentTime"
        } catch (e: Exception) {
            // Fallback to local time
            SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault()).format(Date())
        }
    }

    private fun showPreviewAndConfirmation(photoFile: File) {
        binding.apply {
            // Hide camera preview and capture button
            viewFinder.visibility = View.GONE
            captureButton.visibility = View.GONE
            progressIndicator.visibility = View.GONE

            // Load and show the captured image
            Glide.with(this@AttendanceActivity)
                .load(photoFile)
                .into(previewImage)

            // Show preview and confirmation buttons
            previewImage.visibility = View.VISIBLE
            confirmButton.visibility = View.VISIBLE
            retakeButton.visibility = View.VISIBLE
        }
    }

    private fun hidePreviewAndButtons() {
        binding.apply {
            viewFinder.visibility = View.GONE
            previewImage.visibility = View.GONE
            captureButton.visibility = View.GONE
            confirmButton.visibility = View.GONE
            retakeButton.visibility = View.GONE
            progressIndicator.visibility = View.GONE
        }
    }

    private fun handleTimeIn(photoFile: File) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    showLoading("Recording Time In...")
                    // Disable swipe refresh during upload
                    swipeRefreshLayout.isEnabled = false
                }

                val currentTime = PhilippinesServerTime.formatDatabaseTime()
                val today = PhilippinesServerTime.formatDatabaseDate()
                val storeId = SessionManager.getCurrentUser()?.storeid ?: return@launch
                val staffId = "${selectedStaff?.name}_$storeId"

                // Save to local database FIRST
                val attendance = AttendanceRecord(
                    staffId = staffId,
                    storeId = storeId,
                    date = today,
                    timeIn = currentTime,
                    timeInPhoto = photoFile.absolutePath
                )

                withContext(Dispatchers.IO) {
                    val existing = attendanceDao.getAttendanceForStaffOnDate(staffId, today)
                    if (existing != null) {
                        val updated = existing.copy(
                            timeIn = currentTime,
                            timeInPhoto = photoFile.absolutePath
                        )
                        attendanceDao.updateAttendance(updated)
                    } else {
                        attendanceDao.insertAttendance(attendance)
                    }
                }

                // Create server record format for immediate UI update
                val serverRecord = com.example.possystembw.DAO.ServerAttendanceRecord(
                    id = System.currentTimeMillis().toInt(),
                    staffId = staffId,
                    storeId = storeId,
                    date = today,
                    timeIn = currentTime,
                    timeInPhoto = photoFile.absolutePath,
                    breakIn = null,
                    breakInPhoto = null,
                    breakOut = null,
                    breakOutPhoto = null,
                    timeOut = null,
                    timeOutPhoto = null,
                    status = "ACTIVE",
                    created_at = "",
                    updated_at = ""
                )

                withContext(Dispatchers.Main) {
                    // Update adapter immediately with new data
                    adapter.updateStaffAttendanceImmediately(staffId, serverRecord)

                    showToast("Time In recorded successfully!")
                    hideLoading()
                    cleanupCamera() // This will re-enable swipe refresh
                }

                // Clear cached data so next refresh gets fresh data
                SessionManager.clearAttendanceData()

                // Background server upload
                try {
                    val result = RetrofitClient.attendanceService.uploadAttendanceRecord(
                        staffId = staffId,
                        storeId = storeId,
                        date = today,
                        time = currentTime,
                        type = "TIME_IN",
                        photoFile = photoFile
                    )

                    if (result.isFailure) {
                        Log.e(TAG, "Failed to upload to server: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Server upload error", e)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideLoading()
                    showToast("Failed to record Time In: ${e.message}")
                    cleanupCamera() // This will re-enable swipe refresh
                }
                Log.e(TAG, "Error during time in", e)
            }
        }
    }

    private fun handleTimeOut(photoFile: File) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    showLoading("Recording Time Out...")
                    swipeRefreshLayout.isEnabled = false
                }

                val currentTime = PhilippinesServerTime.formatDatabaseTime()
                val today = PhilippinesServerTime.formatDatabaseDate()
                val storeId = SessionManager.getCurrentUser()?.storeid ?: return@launch
                val staffId = "${selectedStaff?.name}_$storeId"

                // Get existing attendance
                val existingAttendance = withContext(Dispatchers.IO) {
                    attendanceDao.getAttendanceForStaffOnDate(staffId, today)
                }

                if (existingAttendance == null || existingAttendance.timeIn.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        showToast("Please time in first")
                        hideLoading()
                        cleanupCamera()
                    }
                    return@launch
                }

                // Update local database
                val updatedAttendance = existingAttendance.copy(
                    timeOut = currentTime,
                    timeOutPhoto = photoFile.absolutePath,
                    status = "COMPLETED"
                )

                withContext(Dispatchers.IO) {
                    attendanceDao.updateAttendance(updatedAttendance)
                }

                // Create complete server record for immediate UI update
                val serverRecord = com.example.possystembw.DAO.ServerAttendanceRecord(
                    id = existingAttendance.id.toInt(),
                    staffId = staffId,
                    storeId = storeId,
                    date = today,
                    timeIn = existingAttendance.timeIn,
                    timeInPhoto = existingAttendance.timeInPhoto,
                    breakIn = existingAttendance.breakIn,
                    breakInPhoto = existingAttendance.breakInPhoto,
                    breakOut = existingAttendance.breakOut,
                    breakOutPhoto = existingAttendance.breakOutPhoto,
                    timeOut = currentTime,
                    timeOutPhoto = photoFile.absolutePath, // Local path for immediate display
                    status = "COMPLETED",
                    created_at = "",
                    updated_at = ""
                )

                withContext(Dispatchers.Main) {
                    // CRITICAL: Update adapter immediately
                    adapter.updateStaffAttendanceImmediately(staffId, serverRecord)

                    showToast("Time Out recorded successfully!")
                    hideLoading()
                    cleanupCamera()
                }
                SessionManager.clearAttendanceData()

                // Background server upload
                try {
                    val result = RetrofitClient.attendanceService.uploadAttendanceRecord(
                        staffId = staffId,
                        storeId = storeId,
                        date = today,
                        time = currentTime,
                        type = "TIME_OUT",
                        photoFile = photoFile
                    )

                    if (result.isFailure) {
                        Log.e(TAG, "Failed to upload to server: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Server upload error", e)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideLoading()
                    showToast("Failed to record Time Out: ${e.message}")
                    cleanupCamera()
                }
                Log.e(TAG, "Error during time out", e)
            }
        }
    }

    private fun handleBreakStatus(staff: StaffEntity) {
        lifecycleScope.launch {
            try {
                showLoading("Updating break status...")

                if (!PhilippinesServerTime.isInternetAvailable(this@AttendanceActivity)) {
                    throw NoInternetException("Internet connection required to verify time")
                }

                val timeSync = PhilippinesServerTime.syncTimeWithRetry(this@AttendanceActivity)
                if (!timeSync) {
                    throw ServerTimeException("Failed to sync with server time")
                }

                val currentTime = PhilippinesServerTime.formatDatabaseTime()
                val today = PhilippinesServerTime.formatDatabaseDate()
                val storeId = SessionManager.getCurrentUser()?.storeid ?: return@launch
                val staffId = "${staff.name}_$storeId"

                val attendance = withContext(Dispatchers.IO) {
                    attendanceDao.getAttendanceForStaffOnDate(staffId, today)
                }

                if (attendance == null || attendance.timeIn == null) {
                    showToast("Please time in first")
                    hideLoading()
                    return@launch
                }

                val isStartingBreak = attendance.breakIn == null

                // Create a temp file from drawable resource
                val defaultPhotoFile = File(cacheDir, "temp_break.jpg")
                withContext(Dispatchers.IO) {
                    try {
                        val drawable = ContextCompat.getDrawable(
                            this@AttendanceActivity,
                            R.drawable.ic_camera_placeholder
                        )
                        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        drawable?.setBounds(0, 0, canvas.width, canvas.height)
                        drawable?.draw(canvas)

                        FileOutputStream(defaultPhotoFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating default photo", e)
                    }
                }

                try {
                    val result = RetrofitClient.attendanceService.uploadAttendanceRecord(
                        staffId = staffId,
                        storeId = storeId,
                        date = today,
                        time = currentTime,
                        type = if (isStartingBreak) "BREAK_IN" else "BREAK_OUT",
                        photoFile = defaultPhotoFile
                    )

                    if (result.isSuccess) {
                        val updatedAttendance = if (isStartingBreak) {
                            attendance.copy(
                                breakIn = currentTime,
                                breakInPhoto = ""
                            )
                        } else {
                            attendance.copy(
                                breakOut = currentTime,
                                breakOutPhoto = ""
                            )
                        }

                        withContext(Dispatchers.IO) {
                            attendanceDao.updateAttendance(updatedAttendance)
                        }

                        withContext(Dispatchers.Main) {
                            showToast(if (isStartingBreak) "Break started" else "Break ended")
                            // Clear cached attendance data to force refresh
                            SessionManager.clearAttendanceData()
                        }
                    } else {
                        throw result.exceptionOrNull() ?: Exception("Failed to update break status")
                    }
                } finally {
                    withContext(Dispatchers.IO) {
                        if (defaultPhotoFile.exists()) {
                            defaultPhotoFile.delete()
                        }
                    }
                }

                hideLoading()
                refreshAttendanceData()

            } catch (e: Exception) {
                Log.e(TAG, "Error handling break status", e)
                showToast("Failed to update break status: ${e.message}")
                hideLoading()
                refreshAttendanceData()
            }
        }
    }

    private fun cleanupAndRefresh() {
        binding.progressIndicator.visibility = View.GONE
        binding.viewFinder.visibility = View.GONE

        // Re-enable swipe refresh after camera actions
        swipeRefreshLayout.isEnabled = true

        // Only cleanup camera if it's a photo-requiring action
        if (selectedAttendanceType == StaffAttendanceAdapter.AttendanceType.TIME_IN ||
            selectedAttendanceType == StaffAttendanceAdapter.AttendanceType.TIME_OUT
        ) {
            cleanupCamera()
        }

        // Refresh the current view
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
        adapter.updateDate(dateStr)
        adapter.notifyDataSetChanged()
    }

    private fun loadStaffList() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val storeId = SessionManager.getCurrentUser()?.storeid ?: return@withContext
                    // Get unique staff list directly from staff table instead of attendance records
                    val staffList = AppDatabase.getDatabase(application).staffDao().getStaffByStore(storeId)
                    withContext(Dispatchers.Main) {
                        adapter.updateServerAttendanceData(serverAttendanceData)
                        adapter.submitList(staffList)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading staff list", e)
                showToast("Failed to load staff list")
            }
        }
    }

    private fun cleanupCamera() {
        binding.apply {
            viewFinder.visibility = View.GONE
            previewImage.visibility = View.GONE
            captureButton.visibility = View.GONE
            confirmButton.visibility = View.GONE
            retakeButton.visibility = View.GONE
            progressIndicator.visibility = View.GONE
        }

        // Re-enable swipe refresh
        swipeRefreshLayout.isEnabled = true

        try {
            if (::cameraProvider.isInitialized) {
                cameraProvider.unbindAll()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up camera", e)
        }

        // Reset selected staff and type
        selectedStaff = null
        selectedAttendanceType = null
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "ATTENDANCE_${selectedStaff?.name}_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all { permission ->
        ContextCompat.checkSelfPermission(
            baseContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun checkPermissions() {
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun setupTimeAndDate() {
        binding.currentDate.text = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            .format(Date())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_PERMISSIONS -> {
                if (allPermissionsGranted()) {
                    Log.d(TAG, "All permissions granted")
                    // Only initialize camera if user has clicked on an attendance action
                    if (selectedStaff != null && selectedAttendanceType != null) {
                        binding.viewFinder.visibility = View.VISIBLE
                        setupCamera()
                    }
                } else {
                    showToast("Camera and storage permissions are required for attendance")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Now safe to call because timeUpdateHandler is initialized in onCreate
        timeUpdateHandler.post(timeUpdateRunnable)
    }

    override fun onPause() {
        super.onPause()
        timeUpdateHandler.removeCallbacks(timeUpdateRunnable)
    }

    private fun showLoading(message: String) {
        binding.apply {
            loadingProgressBar.visibility = View.VISIBLE
            loadingMessage.visibility = View.VISIBLE
            loadingMessage.text = message
            mainContent.visibility = View.GONE
        }
    }

    private fun hideLoading() {
        binding.apply {
            loadingProgressBar.visibility = View.GONE
            loadingMessage.visibility = View.GONE
            mainContent.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()

        // Clean up default photo file if it exists
        try {
            val defaultPhotoFile = File(cacheDir, "default_photo.jpg")
            if (defaultPhotoFile.exists()) {
                defaultPhotoFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up default photo file", e)
        }
    }

    // Tablet-specific sidebar methods
    private fun initializeSidebarComponents() {
        if (!isMobileLayout) {
            try {
                sidebarLayout = findViewById(R.id.sidebarLayout)
                toggleButton = findViewById(R.id.toggleButton)
                buttonContainer = findViewById(R.id.buttonContainer)
                ecposTitle = findViewById(R.id.ecposTitle)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing sidebar components", e)
            }
        }
    }

    private fun setupSidebar() {
        if (!isMobileLayout) {
            try {
                toggleButton.setOnClickListener {
                    if (isSidebarExpanded) {
                        collapseSidebar()
                    } else {
                        expandSidebar()
                    }
                }
                setupSidebarButtons()
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up sidebar", e)
            }
        }
    }

    private fun setupSidebarButtons() {
        if (!isMobileLayout) {
            try {
                findViewById<ImageButton>(R.id.button2)?.setOnClickListener {
                    val intent = Intent(this, ReportsActivity::class.java)
                    startActivity(intent)
                }

                findViewById<ImageButton>(R.id.button3)?.setOnClickListener {
                    navigateToMainWithUrl("https://eljin.org/order", "ORDERING")
                }

                findViewById<ImageButton>(R.id.stockcounting)?.setOnClickListener {
                    val intent = Intent(this, StockCountingActivity::class.java)
                    startActivity(intent)
                    showToast("Stock Counting")
                }

                findViewById<ImageButton>(R.id.button5)?.setOnClickListener {
                    navigateToMainWithUrl("https://eljin.org/StockTransfer", "Stock Transfer")
                }

                findViewById<ImageButton>(R.id.button6)?.setOnClickListener {
                    navigateToMainWithUrl("https://eljin.org/reports", "REPORTS")
                }

                findViewById<ImageButton>(R.id.waste)?.setOnClickListener {
                    navigateToMainWithUrl("https://eljin.org/waste", "WASTE")
                }

                findViewById<ImageButton>(R.id.partycakes)?.setOnClickListener {
                    navigateToMainWithUrl("https://eljin.org/loyalty-cards", "PARTYCAKES")
                }

                findViewById<ImageButton>(R.id.customer)?.setOnClickListener {
                    navigateToMainWithUrl("https://eljin.org/customers", "CUSTOMER")
                }

                findViewById<ImageButton>(R.id.button7)?.setOnClickListener {
                    val intent = Intent(this, Window1::class.java)
                    startActivity(intent)
                }

                findViewById<ImageButton>(R.id.printerSettingsButton)?.setOnClickListener {
                    val intent = Intent(this, PrinterSettingsActivity::class.java)
                    startActivity(intent)
                    showToast("PRINTER SETTINGS")
                }

                findViewById<ImageButton>(R.id.button8)?.setOnClickListener {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up sidebar buttons", e)
            }
        }
    }

    private fun collapseSidebar() {
        if (!isMobileLayout && isSidebarExpanded && !isAnimating) {
            isAnimating = true

            val animatorSet = AnimatorSet()

            val collapseWidth = ValueAnimator.ofInt(sidebarLayout.width, dpToPx(24)).apply {
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animator ->
                    val value = animator.animatedValue as Int
                    sidebarLayout.layoutParams = sidebarLayout.layoutParams.apply {
                        width = value
                    }
                }
            }

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

            animatorSet.playTogether(
                collapseWidth,
                toggleButtonMargin,
                ObjectAnimator.ofFloat(toggleButton, View.ROTATION, 0f, 180f).apply {
                    duration = 300
                },
                ObjectAnimator.ofFloat(buttonContainer, View.ALPHA, 1f, 0f).apply {
                    duration = 150
                }
            )

            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    buttonContainer.visibility = View.GONE
                    ecposTitle.visibility = View.GONE
                    isSidebarExpanded = false
                    isAnimating = false
                }
            })

            animatorSet.start()
        }
    }

    private fun expandSidebar() {
        if (!isMobileLayout && !isSidebarExpanded && !isAnimating) {
            isAnimating = true

            val animatorSet = AnimatorSet()

            val expandWidth = ValueAnimator.ofInt(sidebarLayout.width, dpToPx(100)).apply {
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animator ->
                    val value = animator.animatedValue as Int
                    sidebarLayout.layoutParams = sidebarLayout.layoutParams.apply {
                        width = value
                    }
                }
            }

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

            animatorSet.playTogether(
                expandWidth,
                toggleButtonMargin,
                ObjectAnimator.ofFloat(toggleButton, View.ROTATION, 180f, 0f).apply {
                    duration = 300
                }
            )

            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    buttonContainer.visibility = View.VISIBLE
                    ecposTitle.visibility = View.VISIBLE
                    buttonContainer.alpha = 0f
                }

                override fun onAnimationEnd(animation: Animator) {
                    buttonContainer.animate()
                        .alpha(1f)
                        .setDuration(150)
                        .start()
                    isSidebarExpanded = true
                    isAnimating = false
                }
            })

            animatorSet.start()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    enum class AttendanceAction {
        TIME_IN, TIME_OUT, BREAK
    }

    companion object {
        const val TAG = "AttendanceActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10

        private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA
            )
        }
    }
}
