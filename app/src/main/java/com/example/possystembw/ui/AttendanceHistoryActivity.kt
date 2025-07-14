package com.example.possystembw.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.possystembw.DAO.AttendanceService
import com.example.possystembw.DAO.ServerAttendanceRecord
import com.example.possystembw.DAO.toLocalAttendanceRecord
import com.example.possystembw.R
import com.example.possystembw.database.AttendanceRecord
import com.example.possystembw.databinding.ActivityAttendanceHistoryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AttendanceHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAttendanceHistoryBinding
    private lateinit var attendanceService: AttendanceService
    private var staffId: String? = null
    private var staffName: String? = null
    private var storeId: String? = null
    private var allAttendanceRecords: List<ServerAttendanceRecord> = emptyList()
    private var attendanceDates: Set<String> = emptySet()
    private lateinit var calendar: Calendar
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//    private val BASE_URL = "http://10.151.5.239:8000" // Your server base URL
    private val BASE_URL = "http://10.151.5.239:8000" // Your server base URL


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        setupToolbar()
        setupCalendarView()
        loadAttendanceData()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = "Attendance History - $staffName"
            setNavigationOnClickListener { finish() }
        }
    }

    private fun setupCalendarView() {
        binding.calendarView.apply {
            firstDayOfWeek = Calendar.MONDAY
            setOnDateChangeListener { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val selectedDate = dateFormat.format(calendar.time)
                loadAttendanceForDate(selectedDate)
            }
        }
    }

    private fun loadAttendanceData() {
        // First, try to use cached data
        val cachedData = SessionManager.getAttendanceData()
        if (cachedData.isNotEmpty() && SessionManager.isAttendanceDataFresh()) {
            Log.d("AttendanceHistory", "Using cached attendance data: ${cachedData.size} records")
            processCachedAttendanceData(cachedData)
            return
        }

        // If no cached data or data is stale, fetch from server
        showLoading(true)
        lifecycleScope.launch {
            try {
                val result = attendanceService.getStoreAttendanceRecords(storeId!!)

                if (result.isSuccess) {
                    allAttendanceRecords = result.getOrNull() ?: emptyList()

                    // Update cache
                    SessionManager.setAttendanceData(allAttendanceRecords)

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
    private fun processCachedAttendanceData(cachedData: List<ServerAttendanceRecord>) {
        allAttendanceRecords = cachedData

        // Filter records for this specific staff member
        val staffRecords = allAttendanceRecords.filter { record ->
            record.staffId == staffId
        }

        // Store dates with attendance
        attendanceDates = staffRecords.map { it.date }.toSet()

        updateAttendanceIndicators(staffRecords)

        // Load current date's attendance
        val currentDate = dateFormat.format(Date())
        loadAttendanceForDate(currentDate)

        showToast("Loaded ${staffRecords.size} cached attendance records")
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
        binding.presentCountText.text = "${currentMonthRecords.size} days present this month"

        // Calculate absent days for current month
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val absentDays = daysInMonth - currentMonthRecords.size
        binding.absentCountText.text = "$absentDays days absent this month"

        // Update progress indicator
        val attendanceRatio = if (daysInMonth > 0) {
            (currentMonthRecords.size.toFloat() / daysInMonth.toFloat() * 100).toInt()
        } else 0
        binding.attendanceProgressBar.progress = attendanceRatio
    }

    private fun loadAttendanceForDate(date: String) {
        val record = allAttendanceRecords.find { it.staffId == staffId && it.date == date }

        binding.apply {
            if (record != null) {
                // Show attendance details
                attendanceDetails.apply {
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

                imagesContainer.visibility = View.VISIBLE

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
                attendanceDetails.apply {
                    text = if (attendanceDates.contains(date)) {
                        "No attendance record for ${formatDate(date)}"
                    } else {
                        "No attendance record for ${formatDate(date)}"
                    }
                    visibility = View.VISIBLE
                }
                imagesContainer.visibility = View.GONE
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
            timeInImage.setImageResource(R.drawable.placeholder_image)
            breakInImage.setImageResource(R.drawable.placeholder_image)
            breakOutImage.setImageResource(R.drawable.placeholder_image)
            timeOutImage.setImageResource(R.drawable.placeholder_image)

            // Hide all text views
            timeInStaffText.visibility = View.GONE
            timeInTimeText.visibility = View.GONE
            breakInStaffText.visibility = View.GONE
            breakInTimeText.visibility = View.GONE
            breakOutStaffText.visibility = View.GONE
            breakOutTimeText.visibility = View.GONE
            timeOutStaffText.visibility = View.GONE
            timeOutTimeText.visibility = View.GONE
        }
    }

    private fun updateAttendanceSection(
        time: String?,
        photoPath: String?,
        staffText: TextView,
        timeText: TextView,
        imageView: ImageView,
        sectionType: String
    ) {
        if (!time.isNullOrEmpty()) {
            staffText.apply {
                text = "$sectionType - ${extractStaffNameFromId(staffId!!)}"
                visibility = View.VISIBLE
            }
            timeText.apply {
                text = time
                visibility = View.VISIBLE
            }
            loadImageFromServer(imageView, photoPath)
        } else {
            staffText.visibility = View.GONE
            timeText.visibility = View.GONE
            imageView.setImageResource(R.drawable.placeholder_image)
        }
    }

    private fun loadImageFromServer(imageView: ImageView, photoPath: String?) {
        if (!photoPath.isNullOrEmpty()) {
            val fullImageUrl = BASE_URL + photoPath

            Glide.with(this)
                .load(fullImageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .centerCrop()
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
                attendanceDetails.apply {
                    text = "Loading attendance data..."
                    visibility = View.VISIBLE
                }
                imagesContainer.visibility = View.GONE
            }
        }
    }

    private fun handleError() {
        binding.apply {
            attendanceDetails.apply {
                text = "Error loading attendance data. Please check your internet connection and try again."
                visibility = View.VISIBLE
            }
            imagesContainer.visibility = View.GONE
            resetAttendanceViews()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}