package com.example.possystembw.adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.possystembw.R
import com.example.possystembw.DAO.PhilippinesServerTime
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.database.AttendanceRecord
import com.example.possystembw.database.StaffEntity
import com.example.possystembw.databinding.StaffAttendanceCardBinding
import com.example.possystembw.ui.AttendanceHistoryActivity
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class StaffAttendanceAdapter(
    private val context: Context,
    private val onImageClick: (StaffEntity, AttendanceType) -> Unit
) : ListAdapter<StaffEntity, StaffAttendanceAdapter.ViewHolder>(StaffDiffCallback()) {

    enum class AttendanceType {
        TIME_IN, BREAK_STATUS, TIME_OUT
    }

    private var selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    fun updateDate(newDate: String) {
        selectedDate = newDate
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: StaffAttendanceCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(staff: StaffEntity) {
            binding.apply {
                val displayDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)!!
                )
                staffInfo.text = "${staff.name}"

                setupDefaultImages()
                setupClickListeners(staff)
                loadAttendanceData(staff)

                historyButton.setOnClickListener {
                    val intent = Intent(context, AttendanceHistoryActivity::class.java).apply {
                        putExtra("staffId", "${staff.name}_${staff.storeId}")
                        putExtra("staffName", staff.name)
                        putExtra("storeId", staff.storeId)
                    }
                    context.startActivity(intent)
                }
            }
        }

        private fun setupDefaultImages() {
            binding.apply {
                timeInImage.apply {
                    setImageResource(R.drawable.ic_camera_placeholder)
                    borderColor = ContextCompat.getColor(context, R.color.gray)
                    borderWidth = resources.getDimensionPixelSize(R.dimen.grid_spacing)
                }

                timeOutImage.apply {
                    setImageResource(R.drawable.ic_camera_placeholder)
                    borderColor = ContextCompat.getColor(context, R.color.gray)
                    borderWidth = resources.getDimensionPixelSize(R.dimen.grid_spacing)
                }

                timeInTime.text = ""
                breakStatus.text = "Not on break"
                timeOutTime.text = ""
            }
        }


        private fun setupClickListeners(staff: StaffEntity) {
            val scope = (context as? AppCompatActivity)?.lifecycleScope ?: CoroutineScope(Dispatchers.Main)

            scope.launch {
                try {
                    val attendance = withContext(Dispatchers.IO) {
                        AppDatabase.getDatabase(context).attendanceDao()
                            .getAttendanceForStaffOnDate(
                                "${staff.name}_${staff.storeId}",
                                PhilippinesServerTime.formatDatabaseDate()
                            )
                    }

                    binding.apply {
                        // Time In handling
                        timeInImage.apply {
                            isClickable = attendance?.timeIn == null
                            setOnClickListener {
                                if (isClickable) onImageClick(staff, AttendanceType.TIME_IN)
                            }
                            alpha = if (isClickable) 1.0f else 0.5f
                        }

                        // Break Status handling
                        breakButton.apply {
                            isClickable = attendance?.timeIn != null && attendance.timeOut == null
                            setOnClickListener {
                                if (isClickable) {
                                    showBreakConfirmationDialog(staff, attendance)
                                }
                            }
                            alpha = if (isClickable) 1.0f else 0.5f
                        }

                        // Time Out handling
                        timeOutImage.apply {
                            isClickable = attendance?.timeIn != null && attendance.timeOut == null
                            setOnClickListener {
                                if (isClickable) onImageClick(staff, AttendanceType.TIME_OUT)
                            }
                            alpha = if (isClickable) 1.0f else 0.5f
                        }
                    }
                } catch (e: Exception) {
                    Log.e("StaffAttendanceAdapter", "Error setting up click listeners", e)
                }
            }
        }

        private fun showBreakConfirmationDialog(staff: StaffEntity, attendance: AttendanceRecord?) {
            val isOnBreak = attendance?.breakIn != null && attendance.breakOut == null
            val message = if (isOnBreak) "Are you sure you want to end your break?" else "Are you sure you want to start your break?"

            AlertDialog.Builder(context)
                .setTitle("Break Status")
                .setMessage(message)
                .setPositiveButton("Yes") { dialog, _ ->
                    onImageClick(staff, AttendanceType.BREAK_STATUS)
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
        private fun loadAttendanceData(staff: StaffEntity) {
            val scope = (context as? AppCompatActivity)?.lifecycleScope ?: CoroutineScope(Dispatchers.Main)

            scope.launch {
                try {
                    val attendance = withContext(Dispatchers.IO) {
                        AppDatabase.getDatabase(context).attendanceDao()
                            .getAttendanceForStaffOnDate(
                                "${staff.name}_${staff.storeId}",
                                selectedDate
                            )
                    }

                    attendance?.let { record ->
                        binding.apply {
                            // Time In
                            record.timeIn?.let { time ->
                                timeInTime.text = time
                                if (record.timeInPhoto.isNotEmpty()) {
                                    loadImageWithGlide(timeInImage, record.timeInPhoto)
                                    timeInImage.borderColor = ContextCompat.getColor(context, R.color.green)
                                }
                            }

                            // Break Status
                            updateBreakStatus(record)

                            // Time Out
                            record.timeOut?.let { time ->
                                timeOutTime.text = time
                                if (record.timeOutPhoto?.isNotEmpty() == true) {
                                    record.timeOutPhoto?.let {
                                        loadImageWithGlide(timeOutImage, it)
                                    }
                                    timeOutImage.borderColor = ContextCompat.getColor(context, R.color.green)
                                }
                            }

                            // Add this line to update the time breakdown
                            updateTimeBreakdown(record)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("StaffAttendanceAdapter", "Error loading attendance data", e)
                }
            }
        }
        private fun updateTimeBreakdown(record: AttendanceRecord) {
            try {
                binding.apply {
                    // Update staff status
                    staffStatus.apply {
                        text = when {
                            record.timeOut != null -> "Shift Completed"
                            record.breakIn != null && record.breakOut == null -> "On Break"
                            record.timeIn != null -> "Currently Working"
                            else -> "Not Started"
                        }
                        setTextColor(
                            ContextCompat.getColor(
                                context, when {
                                    record.timeOut != null -> R.color.green
                                    record.breakIn != null && record.breakOut == null -> R.color.red
                                    record.timeIn != null -> R.color.navy
                                    else -> R.color.gray
                                }
                            )
                        )
                    }

                    // Time calculations
                    if (record.timeIn != null) {
                        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

                        // Convert times to minutes since midnight for easier calculation
                        fun getMinutesSinceMidnight(timeStr: String): Long {
                            val time = timeFormat.parse(timeStr) ?: return 0
                            val calendar = Calendar.getInstance().apply { this.time = time }
                            return (calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)).toLong()
                        }

                        // Calculate work minutes
                        var totalWorkMinutes = 0L
                        var breakMinutes = 0L

                        val timeInMinutes = getMinutesSinceMidnight(record.timeIn)

                        if (record.breakIn != null) {
                            // First work period: time in to break in
                            val breakInMinutes = getMinutesSinceMidnight(record.breakIn)
                            totalWorkMinutes = breakInMinutes - timeInMinutes

                            if (record.breakOut != null) {
                                // Break duration
                                val breakOutMinutes = getMinutesSinceMidnight(record.breakOut)
                                breakMinutes = breakOutMinutes - breakInMinutes

                                // Second work period if applicable
                                if (record.timeOut != null) {
                                    val timeOutMinutes = getMinutesSinceMidnight(record.timeOut)
                                    totalWorkMinutes += timeOutMinutes - breakOutMinutes
                                } else {
                                    // Still working after break
                                    val currentMinutes = getMinutesSinceMidnight(
                                        timeFormat.format(Date())
                                    )
                                    totalWorkMinutes += currentMinutes - breakOutMinutes
                                }
                            } else {
                                // Currently on break
                                val currentMinutes = getMinutesSinceMidnight(
                                    timeFormat.format(Date())
                                )
                                breakMinutes = currentMinutes - breakInMinutes
                            }
                        } else if (record.timeOut != null) {
                            // Completed work without break
                            val timeOutMinutes = getMinutesSinceMidnight(record.timeOut)
                            totalWorkMinutes = timeOutMinutes - timeInMinutes
                        } else {
                            // Still working, no break
                            val currentMinutes = getMinutesSinceMidnight(
                                timeFormat.format(Date())
                            )
                            totalWorkMinutes = currentMinutes - timeInMinutes
                        }

                        // Format and display durations
                        fun formatDuration(minutes: Long): String {
                            val hours = minutes / 60
                            val mins = minutes % 60
                            return String.format("%d hrs %d mins", hours, mins)
                        }

                        workDuration.text = formatDuration(totalWorkMinutes)
                        breakDuration.text = if (breakMinutes > 0) {
                            formatDuration(breakMinutes)
                        } else {
                            "No break taken"
                        }
                        totalHours.text = formatDuration(totalWorkMinutes + breakMinutes)
                    } else {
                        // Reset displays if no time in
                        workDuration.text = "Not started"
                        breakDuration.text = "No break taken"
                        totalHours.text = "0 hrs 0 mins"
                    }

                    // Strictly enforce clickable states
                    val today = PhilippinesServerTime.formatDatabaseDate()
                    val isToday = record.date == today
                    val breakCompleted = record.breakIn != null && record.breakOut != null

                    // Time In: Only clickable if no time in record exists and it's today
                    timeInImage.isEnabled = record.timeIn == null && isToday
                    timeInImage.isClickable = timeInImage.isEnabled
//                    timeInImage.alpha = if (timeInImage.isEnabled) 1.0f else 0.5f

                    // Break Button: Never clickable if break is completed
                    val canBreak = record.timeIn != null && record.timeOut == null &&
                            !breakCompleted && isToday
                    breakButton.isEnabled = canBreak
                    breakButton.isClickable = canBreak
//                    breakButton.alpha = if (canBreak) 1.0f else 0.5f

                    // Time Out: Only clickable if time in exists, no time out, not on break, and it's today
                    val canTimeOut = record.timeIn != null && record.timeOut == null &&
                            (record.breakIn == null || record.breakOut != null) && isToday
                    timeOutImage.isEnabled = canTimeOut
                    timeOutImage.isClickable = canTimeOut
//                    timeOutImage.alpha = if (canTimeOut) 1.0f else 0.5f
                }
            } catch (e: Exception) {
                Log.e("StaffAttendanceAdapter", "Error updating time breakdown", e)
            }
        }
        private fun updateBreakStatus(record: AttendanceRecord) {
            binding.apply {
                val breakStarted = record.breakIn != null && record.breakOut == null
                val breakCompleted = record.breakIn != null && record.breakOut != null

                // Update break button appearance
                breakButton.apply {
                    setCardBackgroundColor(ContextCompat.getColor(context, when {
                        breakStarted -> R.color.red
                        breakCompleted -> R.color.green
                        else -> R.color.orange
                    }))

                    // Break button should not be clickable if completed
                    isClickable = false
                    isEnabled = false
//                    alpha = 0.5f

                    if (!breakCompleted && record.timeIn != null && record.timeOut == null) {
                        isClickable = true
                        isEnabled = true
                        alpha = 1.0f
                    }
                }

                // Update break button text
                breakButtonText.text = when {
                    breakStarted -> "End\nBreak"
                    breakCompleted -> "Break\nCompleted"
                    else -> "Start\nBreak"
                }

                // Update break status text
                breakStatus.apply {
                    text = when {
                        breakStarted -> "On Break (${record.breakIn})"
                        breakCompleted -> calculateBreakDuration(record.breakIn!!, record.breakOut!!)
                        else -> "Not on break"
                    }
                    setTextColor(ContextCompat.getColor(context, when {
                        breakStarted -> R.color.red
                        breakCompleted -> R.color.green
                        else -> R.color.gray
                    }))
                }
            }
        }
        private fun calculateBreakDuration(breakIn: String, breakOut: String): String {
            try {
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val startTime = timeFormat.parse(breakIn)
                val endTime = timeFormat.parse(breakOut)

                if (startTime != null && endTime != null) {
                    val durationMinutes = (endTime.time - startTime.time) / (60 * 1000)
                    val hours = durationMinutes / 60
                    val minutes = durationMinutes % 60

                    return if (hours > 0) {
                        "Break taken: ${hours}h ${minutes}m"
                    } else {
                        "Break taken: ${minutes}m"
                    }
                }
            } catch (e: Exception) {
                Log.e("StaffAttendanceAdapter", "Error calculating break duration", e)
            }
            return "Break completed"
        }
//        private fun updateWorkDuration(record: AttendanceRecord) {
//            try {
//                if (record.timeIn != null) {
//                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
//                    val startTime = timeFormat.parse(record.timeIn)
//                    val endTime = if (record.timeOut != null) {
//                        timeFormat.parse(record.timeOut)
//                    } else {
//                        Date() // Current time for ongoing work
//                    }
//
//                    if (startTime != null && endTime != null) {
//                        var totalMinutes = (endTime.time - startTime.time) / (60 * 1000)
//
//                        // Subtract break time if any
//                        if (record.breakIn != null && record.breakOut != null) {
//                            val breakStart = timeFormat.parse(record.breakIn)
//                            val breakEnd = timeFormat.parse(record.breakOut)
//                            if (breakStart != null && breakEnd != null) {
//                                val breakMinutes = (breakEnd.time - breakStart.time) / (60 * 1000)
//                                totalMinutes -= breakMinutes
//                            }
//                        }
//
//                        val hours = totalMinutes / 60
//                        val minutes = totalMinutes % 60
//
//                        workTimeText.text = when {
//                            record.timeOut != null -> "Total: ${hours}h ${minutes}m"
//                            hours > 0 -> "Working: ${hours}h ${minutes}m"
//                            else -> "Working: ${minutes}m"
//                        }
//                        workTimeText.visibility = View.VISIBLE
//                    }
//                } else {
//                    workTimeText.visibility = View.GONE
//                }
//            } catch (e: Exception) {
//                Log.e("StaffAttendanceAdapter", "Error calculating work duration", e)
//                workTimeText.visibility = View.GONE
//            }
//        }

//            private fun updateAccumulatedTime(record: AttendanceRecord) {
//                try {
//                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
//                        .apply { timeZone = TimeZone.getTimeZone("Asia/Manila") }
//
//                    var totalMinutes = 0L
//                    var breakMinutes = 0L
//
//                    if (record.timeIn != null) {
//                        val timeIn = timeFormat.parse(record.timeIn)
//                        val currentPhTime =
//                            Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila")).time
//                        val timeOut = if (record.timeOut != null) {
//                            timeFormat.parse(record.timeOut)
//                        } else {
//                            currentPhTime // Current Philippine time if not timed out
//                        }
//
//                        if (timeIn != null && timeOut != null) {
//                            totalMinutes = (timeOut.time - timeIn.time) / (60 * 1000)
//
//                            // Calculate break time if applicable
//                            if (record.breakIn != null && record.breakOut != null) {
//                                val breakIn = timeFormat.parse(record.breakIn)
//                                val breakOut = timeFormat.parse(record.breakOut)
//                                if (breakIn != null && breakOut != null) {
//                                    breakMinutes = (breakOut.time - breakIn.time) / (60 * 1000)
//                                }
//                            } else if (record.breakIn != null) {
//                                // Currently on break
//                                val breakIn = timeFormat.parse(record.breakIn)
//                                if (breakIn != null) {
//                                    breakMinutes = (currentPhTime.time - breakIn.time) / (60 * 1000)
//                                }
//                            }
//
//                            // Subtract break time from total time
//                            val workMinutes = totalMinutes - breakMinutes
//                            val hours = workMinutes / 60
//                            val minutes = workMinutes % 60
//
//                            binding.accumulatedTime.text = if (record.timeOut != null) {
//                                String.format(
//                                    "%d hrs %d mins",
//                                    hours,
//                                    minutes
//                                )
//                            } else {
//                                String.format(
//                                    "%d hrs %d mins",
//                                    hours,
//                                    minutes
//                                )
//                            }
//                        }
//                    }
//                } catch (e: Exception) {
//                    Log.e("StaffAttendanceAdapter", "Error calculating accumulated time", e)
//                    binding.accumulatedTime.text = ""
//                }
//            }




        private fun loadImageWithGlide(imageView: CircleImageView, photoPath: String) {
            Glide.with(context)
                .load(File(photoPath))
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.ic_camera_placeholder)
                        .error(R.drawable.ic_camera_placeholder)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                )
                .into(imageView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = StaffAttendanceCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class StaffDiffCallback : DiffUtil.ItemCallback<StaffEntity>() {
        override fun areItemsTheSame(oldItem: StaffEntity, newItem: StaffEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: StaffEntity, newItem: StaffEntity) =
            oldItem == newItem
    }
}

