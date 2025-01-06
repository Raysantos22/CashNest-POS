package com.example.possystembw.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.possystembw.DAO.AttendanceDao
import com.example.possystembw.R
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.database.AttendanceRecord
import com.example.possystembw.databinding.ActivityAttendanceHistoryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AttendanceHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAttendanceHistoryBinding
    private lateinit var attendanceDao: AttendanceDao
    private var staffId: String? = null
    private var staffName: String? = null
    private var storeId: String? = null
    private var attendanceDates: Set<String> = emptySet()
    private lateinit var calendar: Calendar
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        staffId = intent.getStringExtra("staffId")
        staffName = intent.getStringExtra("staffName")
        storeId = intent.getStringExtra("storeId")

        if (staffId == null || staffName == null || storeId == null) {
            finish()
            return
        }

        attendanceDao = AppDatabase.getDatabase(application).attendanceDao()
        calendar = Calendar.getInstance()

        setupToolbar()
        setupCalendarView()
        initializeCurrentMonth()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = "Attendance History"
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

    private fun initializeCurrentMonth() {
        lifecycleScope.launch {
            try {
                val calendar = Calendar.getInstance()
                val firstDayOfMonth = calendar.apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                }.time
                val lastDayOfMonth = calendar.apply {
                    set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                }.time

                val startDate = dateFormat.format(firstDayOfMonth)
                val endDate = dateFormat.format(lastDayOfMonth)

                // Get attendance records for the month
                val records = withContext(Dispatchers.IO) {
                    attendanceDao.getAttendanceForStaffBetweenDates(staffId!!, startDate, endDate)
                }

                // Store dates with attendance
                attendanceDates = records.map { it.date }.toSet()

                // Update the calendar with current date's attendance
                val currentDate = dateFormat.format(Date())
                loadAttendanceForDate(currentDate)

                // Add date change listener
                binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    val selectedDate = dateFormat.format(calendar.time)

                    if (attendanceDates.contains(selectedDate)) {
                        loadAttendanceForDate(selectedDate)
                        binding.imagesContainer.visibility = View.VISIBLE
                        binding.attendanceDetails.visibility = View.VISIBLE
                    } else {
                        binding.imagesContainer.visibility = View.GONE
                        binding.attendanceDetails.apply {
                            text = "No attendance record for ${formatDate(selectedDate)}"
                            visibility = View.VISIBLE
                        }
                        resetAttendanceViews()
                    }
                }

                // Update attendance indicator views
                updateAttendanceIndicators(records)

            } catch (e: Exception) {
                Log.e("AttendanceHistory", "Error loading month data", e)
            }
        }
    }

    private fun updateAttendanceIndicators(records: List<AttendanceRecord>) {
        // Update the legend count
        binding.presentCountText.text = "${records.size} days present"

        // Calculate absent days
        val calendar = Calendar.getInstance()
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val absentDays = daysInMonth - records.size
        binding.absentCountText.text = "$absentDays days absent"

        // Update progress indicator
        val attendanceRatio = (records.size.toFloat() / daysInMonth.toFloat() * 100).toInt()
        binding.attendanceProgressBar.progress = attendanceRatio
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

    private fun loadAttendanceForDate(date: String) {
        lifecycleScope.launch {
            try {
                val record = withContext(Dispatchers.IO) {
                    attendanceDao.getAttendanceForStaffOnDate(staffId!!, date)
                }

                binding.apply {
                    if (record != null) {
                        // Show attendance details with improved styling
                        attendanceDetails.apply {
                            text = buildString {
                                append("Date: ${formatDate(date)}\n")
                                append("Status: ${record.status}\n")
                                if (record.timeIn != null) append("Time In: ${record.timeIn}\n")
                                if (record.breakIn != null) append("Break In: ${record.breakIn}\n")
                                if (record.breakOut != null) append("Break Out: ${record.breakOut}\n")
                                if (record.timeOut != null) append("Time Out: ${record.timeOut}")
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
                            timeInImage
                        )

                        updateAttendanceSection(
                            record.breakIn,
                            record.breakInPhoto,
                            breakInStaffText,
                            breakInTimeText,
                            breakInImage
                        )

                        updateAttendanceSection(
                            record.breakOut,
                            record.breakOutPhoto,
                            breakOutStaffText,
                            breakOutTimeText,
                            breakOutImage
                        )

                        updateAttendanceSection(
                            record.timeOut,
                            record.timeOutPhoto,
                            timeOutStaffText,
                            timeOutTimeText,
                            timeOutImage
                        )
                    } else {
                        resetAttendanceViews()
                        attendanceDetails.apply {
                            text = "No attendance record for ${formatDate(date)}"
                            visibility = View.VISIBLE
                        }
                        imagesContainer.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e("AttendanceHistory", "Error loading date data", e)
                handleError()
            }
        }
    }

    private fun handleError() {
        binding.apply {
            attendanceDetails.apply {
                text = "Error loading attendance data"
                visibility = View.VISIBLE
            }
            imagesContainer.visibility = View.GONE
            resetAttendanceViews()
        }
    }

    private fun updateAttendanceSection(
        time: String?,
        photoPath: String?,
        staffText: TextView,
        timeText: TextView,
        imageView: ImageView
    ) {
        if (time != null) {
            staffText.apply {
                text = staffName
                visibility = View.VISIBLE
            }
            timeText.apply {
                text = time
                visibility = View.VISIBLE
            }
            loadImage(imageView, photoPath)
        } else {
            staffText.visibility = View.GONE
            timeText.visibility = View.GONE
            imageView.setImageResource(R.drawable.placeholder_image)
        }
    }

    private fun loadImage(imageView: ImageView, path: String?) {
        if (!path.isNullOrEmpty()) {
            Glide.with(this)
                .load(File(path))
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
}

// Interface for calendar day decoration
interface DayDecorator {
    fun shouldDecorate(day: Calendar): Boolean
    fun decorate(view: View)
}