package com.example.possystembw.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast


import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.possystembw.DeviceUtils
import com.example.possystembw.DAO.AttendanceService
import com.example.possystembw.DAO.ServerAttendanceRecord
import com.example.possystembw.MainActivity
import com.example.possystembw.R
import com.example.possystembw.databinding.ActivityAttendanceHistoryBinding
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AttendanceHistoryActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityAttendanceHistoryBinding
    private lateinit var attendanceService: AttendanceService
    private var staffId: String? = null
    private var staffName: String? = null
    private var storeId: String? = null
    private var allAttendanceRecords: List<ServerAttendanceRecord> = emptyList()
    private var attendanceDates: Set<String> = emptySet()
    private lateinit var calendar: Calendar
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val BASE_URL = "https://eljin.org/"

    // Mobile layout components
    private var drawerLayout: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    private var hamburgerButton: android.widget.ImageButton? = null
    private var isMobileLayout = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set orientation based on device
        DeviceUtils.setOrientationBasedOnDevice(this)

        binding = ActivityAttendanceHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            DeviceUtils.setOrientationBasedOnDevice(this)

            // Detect layout type
            detectLayoutType()

            // Initialize layout-specific views
            initializeLayoutSpecificViews()

            staffId = intent.getStringExtra("staffId")
            staffName = intent.getStringExtra("staffName")
            storeId = intent.getStringExtra("storeId")

            if (staffId == null || staffName == null || storeId == null) {
                showToast("Missing required information")
                finish()
                return
            }

            attendanceService = AttendanceService()
            calendar = Calendar.getInstance()

            // Setup layout-specific components
            if (isMobileLayout) {
                setupMobileSpecificFeatures()
            } else {
                setupToolbar()
            }

            setupCalendarView()
            loadAttendanceData()

            Log.d(TAG, "✅ onCreate completed successfully for ${if (isMobileLayout) "mobile" else "tablet"} mode")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during onCreate", e)
            Toast.makeText(this, "Initialization Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun detectLayoutType() {
        // Check what views actually exist in the loaded layout
        val drawerLayoutView = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toolbarView = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)

        val isTabletDevice = DeviceUtils.isTablet(this)
        val hasDrawer = drawerLayoutView != null
        val hasToolbar = toolbarView != null

        Log.d(TAG, "=== LAYOUT DETECTION ===")
        Log.d(TAG, "Device type: ${if (isTabletDevice) "Tablet" else "Phone"}")
        Log.d(TAG, "Has DrawerLayout: $hasDrawer")
        Log.d(TAG, "Has Toolbar: $hasToolbar")

        // Determine layout type based on actual layout loaded
        isMobileLayout = hasDrawer && !hasToolbar

        Log.d(TAG, "Final decision: ${if (isMobileLayout) "Mobile" else "Tablet"} mode")
    }

    private fun initializeLayoutSpecificViews() {
        if (isMobileLayout) {
            // Mobile-specific views
            drawerLayout = findViewById(R.id.drawer_layout)
            navigationView = findViewById(R.id.nav_view)
            hamburgerButton = findViewById(R.id.hamburgerButton)

            Log.d(TAG, "✅ Mobile views initialized")
            Log.d(TAG, "DrawerLayout: ${drawerLayout != null}")
            Log.d(TAG, "NavigationView: ${navigationView != null}")
            Log.d(TAG, "HamburgerButton: ${hamburgerButton != null}")
        } else {
            // Tablet-specific views
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

            // Update mobile header
            findViewById<TextView>(R.id.mobileTitle)?.text = "Attendance History"
            findViewById<TextView>(R.id.mobileSubtitle)?.text = staffName ?: "Staff Member"

            Log.d(TAG, "✅ Mobile features setup complete")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Mobile features setup failed", e)
        }
    }

    private fun setupToolbar() {
        if (!isMobileLayout) {
            binding.toolbar?.apply {
                title = "Attendance History - $staffName"
                setNavigationOnClickListener { finish() }
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_pos_system -> {
                val intent = Intent(this, Window1::class.java)
                startActivity(intent)
            }
            R.id.nav_attendance -> {
                val intent = Intent(this, AttendanceActivity::class.java)
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

    private fun createMobileDialog(title: String, message: String): AlertDialog {
        val titleView = TextView(this@AttendanceHistoryActivity).apply {
            text = title
            textSize = if (isMobileLayout) 16f else 18f
            setTextColor(ContextCompat.getColor(this@AttendanceHistoryActivity, android.R.color.black))
            setPadding(
                if (isMobileLayout) 20 else 24,  // left
                if (isMobileLayout) 16 else 20,  // top
                if (isMobileLayout) 20 else 24,  // right
                if (isMobileLayout) 8 else 12    // bottom
            )
            gravity = Gravity.CENTER_VERTICAL
            setTypeface(null, Typeface.BOLD)
        }

        val builder = if (isMobileLayout) {
            AlertDialog.Builder(this@AttendanceHistoryActivity, R.style.CustomDialogStyle3)
        } else {
            AlertDialog.Builder(this@AttendanceHistoryActivity)
        }

        val dialog = builder
            .setCustomTitle(titleView)
            .setMessage(message)
            .create()

        if (isMobileLayout) {
            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
            dialog.setOnShowListener {
                applyMobileDialogStyling(dialog)
            }
        }

        return dialog
    }

    private fun setupCalendarView() {
        binding.calendarView?.apply {
            firstDayOfWeek = Calendar.MONDAY
            setOnDateChangeListener { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val selectedDate = dateFormat.format(calendar.time)
                loadAttendanceForDate(selectedDate)
            }
        }
    }

    private fun loadAttendanceData() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val result = attendanceService.getStoreAttendanceRecords(storeId!!)

                if (result.isSuccess) {
                    allAttendanceRecords = result.getOrNull() ?: emptyList()

                    // Filter records for this specific staff member
                    val staffRecords = allAttendanceRecords.filter { record ->
                        record.staffId == staffId
                    }

                    // Store dates with attendance
                    attendanceDates = staffRecords.map { it.date }.toSet()

                    withContext(Dispatchers.Main) {
                        updateAttendanceIndicators(staffRecords)
                        showLoading(false)

                        // Load current date's attendance
                        val currentDate = dateFormat.format(Date())
                        loadAttendanceForDate(currentDate)

                        showToast("Loaded ${staffRecords.size} attendance records")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        showToast("Failed to load attendance data: ${result.exceptionOrNull()?.message}")
                        handleError()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Log.e("AttendanceHistory", "Error loading attendance data", e)
                    showToast("Error loading data: ${e.message}")
                    handleError()
                }
            }
        }
    }

    private fun updateAttendanceIndicators(records: List<ServerAttendanceRecord>) {
        // Calculate current month statistics
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val currentMonthRecords = records.filter { record ->
            try {
                val recordDate = dateFormat.parse(record.date)
                val recordCalendar = Calendar.getInstance().apply { time = recordDate!! }
                recordCalendar.get(Calendar.MONTH) == currentMonth &&
                        recordCalendar.get(Calendar.YEAR) == currentYear
            } catch (e: Exception) {
                false
            }
        }

        // Update the legend count
        binding.presentCountText?.text = "${currentMonthRecords.size} days present this month"

        // Calculate absent days for current month
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val absentDays = daysInMonth - currentMonthRecords.size
        binding.absentCountText?.text = "$absentDays days absent this month"

        // Update progress indicator
        val attendanceRatio = if (daysInMonth > 0) {
            (currentMonthRecords.size.toFloat() / daysInMonth.toFloat() * 100).toInt()
        } else 0
        binding.attendanceProgressBar?.progress = attendanceRatio
    }

    private fun loadAttendanceForDate(date: String) {
        val record = allAttendanceRecords.find { it.staffId == staffId && it.date == date }

        binding.apply {
            if (record != null) {
                // Show attendance details
                attendanceDetails?.apply {
                    text = buildString {
                        append("Date: ${formatDate(date)}\n")
                        append("Staff: ${extractStaffNameFromId(record.staffId)}\n")
                        append("Status: ${record.status}\n")
                        if (!record.timeIn.isNullOrEmpty()) append("Time In: ${record.timeIn}\n")
                        if (!record.breakIn.isNullOrEmpty()) append("Break In: ${record.breakIn}\n")
                        if (!record.breakOut.isNullOrEmpty()) append("Break Out: ${record.breakOut}\n")
                        if (!record.timeOut.isNullOrEmpty()) append("Time Out: ${record.timeOut}")
                    }
                    setBackgroundResource(R.drawable.attendance_details_background)
                    visibility = View.VISIBLE
                }

                imagesContainer?.visibility = View.VISIBLE

                // Update each section with appropriate visibility and data
                updateAttendanceSection(
                    record.timeIn,
                    record.timeInPhoto,
                    timeInStaffText,
                    timeInTimeText,
                    timeInImage,
                    "Time In"
                )

                updateAttendanceSection(
                    record.breakIn,
                    record.breakInPhoto,
                    breakInStaffText,
                    breakInTimeText,
                    breakInImage,
                    "Break In"
                )

                updateAttendanceSection(
                    record.breakOut,
                    record.breakOutPhoto,
                    breakOutStaffText,
                    breakOutTimeText,
                    breakOutImage,
                    "Break Out"
                )

                updateAttendanceSection(
                    record.timeOut,
                    record.timeOutPhoto,
                    timeOutStaffText,
                    timeOutTimeText,
                    timeOutImage,
                    "Time Out"
                )
            } else {
                resetAttendanceViews()
                attendanceDetails?.apply {
                    text = if (attendanceDates.contains(date)) {
                        "No attendance record for ${formatDate(date)}"
                    } else {
                        "No attendance record for ${formatDate(date)}"
                    }
                    visibility = View.VISIBLE
                }
                imagesContainer?.visibility = View.GONE
            }
        }
    }

    private fun extractStaffNameFromId(staffId: String): String {
        // Extract staff name from staffId format "Name_STOREID"
        return staffId.substringBeforeLast("_")
    }

    private fun resetAttendanceViews() {
        binding.apply {
            // Reset all images to placeholder
            timeInImage?.setImageResource(R.drawable.placeholder_image)
            breakInImage?.setImageResource(R.drawable.placeholder_image)
            breakOutImage?.setImageResource(R.drawable.placeholder_image)
            timeOutImage?.setImageResource(R.drawable.placeholder_image)

            // Hide all text views
            timeInStaffText?.visibility = View.GONE
            timeInTimeText?.visibility = View.GONE
            breakInStaffText?.visibility = View.GONE
            breakInTimeText?.visibility = View.GONE
            breakOutStaffText?.visibility = View.GONE
            breakOutTimeText?.visibility = View.GONE
            timeOutStaffText?.visibility = View.GONE
            timeOutTimeText?.visibility = View.GONE
        }
    }

    private fun updateAttendanceSection(
        time: String?,
        photoPath: String?,
        staffText: TextView?,
        timeText: TextView?,
        imageView: ImageView?,
        sectionType: String
    ) {
        if (!time.isNullOrEmpty()) {
            staffText?.apply {
                text = "$sectionType - ${extractStaffNameFromId(staffId!!)}"
                visibility = View.VISIBLE
            }
            timeText?.apply {
                text = time
                visibility = View.VISIBLE
            }
            if (photoPath != null && imageView != null) {
                loadImageFromServer(imageView, photoPath)
            }
        } else {
            staffText?.visibility = View.GONE
            timeText?.visibility = View.GONE
            imageView?.setImageResource(R.drawable.placeholder_image)
        }
    }

    private fun loadImageFromServer(imageView: ImageView, photoPath: String) {
        if (!photoPath.isNullOrEmpty()) {
            val fullImageUrl = if (photoPath.startsWith("http")) {
                photoPath // Already a full URL
            } else {
                BASE_URL + photoPath // Append to base URL
            }

            Log.d("AttendanceHistory", "Loading image from: $fullImageUrl")

            Glide.with(this)
                .load(fullImageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .centerCrop()
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("AttendanceHistory", "Failed to load image: $fullImageUrl", e)
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("AttendanceHistory", "Successfully loaded image: $fullImageUrl")
                        return false
                    }
                })
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.placeholder_image)
        }
    }

    private fun formatDate(date: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        return try {
            val parsedDate = inputFormat.parse(date)
            outputFormat.format(parsedDate!!)
        } catch (e: Exception) {
            date
        }
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            if (show) {
                attendanceDetails?.apply {
                    text = "Loading attendance data..."
                    visibility = View.VISIBLE
                }
                imagesContainer?.visibility = View.GONE
            }
        }
    }

    private fun handleError() {
        binding.apply {
            attendanceDetails?.apply {
                text = "Error loading attendance data. Please check your internet connection and try again."
                visibility = View.VISIBLE
            }
            imagesContainer?.visibility = View.GONE
            resetAttendanceViews()
        }

        // Show error dialog with mobile styling
        val dialog = createMobileDialog(
            "Error Loading Data",
            "Could not load attendance data. Please check your internet connection and try again."
        )
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Retry") { _, _ ->
            loadAttendanceData()
        }
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Close") { _, _ ->
            finish()
        }
        dialog.show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "AttendanceHistoryActivity"
    }
}