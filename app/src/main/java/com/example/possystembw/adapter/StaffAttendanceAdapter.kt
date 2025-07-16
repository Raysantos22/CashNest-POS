package com.example.possystembw.adapter

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import com.example.possystembw.R
import com.example.possystembw.DAO.PhilippinesServerTime
import com.example.possystembw.DAO.ServerAttendanceRecord
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.database.StaffEntity
import com.example.possystembw.databinding.StaffAttendanceCardBinding
import com.example.possystembw.ui.AttendanceHistoryActivity
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
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
    private var serverAttendanceData: Map<String, ServerAttendanceRecord> = emptyMap()
    private val BASE_URL = "http://10.151.5.239:8000"

    // Cache for loading states
    private val loadingStates = mutableMapOf<String, Set<String>>() // staffId -> set of loading image types

    fun updateDate(newDate: String) {
        selectedDate = newDate
        notifyDataSetChanged()
    }

    fun updateServerAttendanceData(data: Map<String, ServerAttendanceRecord>) {
        serverAttendanceData = data
        notifyDataSetChanged()

        // Start background image loading for all visible items
        loadImagesInBackground()
    }
    fun updateStaffAttendanceImmediately(staffId: String, attendanceData: com.example.possystembw.DAO.ServerAttendanceRecord) {
        // Update server data map
        val key = "${staffId}_${attendanceData.date}"
        val updatedMap = serverAttendanceData.toMutableMap()
        updatedMap[key] = attendanceData
        serverAttendanceData = updatedMap

        // Find and update specific staff position
        val position = getPositionForStaffId(staffId)
        if (position != -1) {
            Log.d("StaffAttendanceAdapter", "Updating position $position for staff $staffId")
            notifyItemChanged(position)
        } else {
            Log.w("StaffAttendanceAdapter", "Could not find position for staff $staffId")
            notifyDataSetChanged() // Fallback to full refresh
        }
    }
    private fun loadImagesInBackground() {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            serverAttendanceData.values.forEach { record ->
                if (record.date == selectedDate) {
                    // Load time in photo
                    record.timeInPhoto?.let { photoPath ->
                        if (photoPath.startsWith("/storage")) {
                            downloadAndCacheImage(record.staffId, "timeIn", photoPath)
                        }
                    }

                    // Load time out photo
                    record.timeOutPhoto?.let { photoPath ->
                        if (photoPath.startsWith("/storage")) {
                            downloadAndCacheImage(record.staffId, "timeOut", photoPath)
                        }
                    }

                    // Load break photos if needed
                    record.breakInPhoto?.let { photoPath ->
                        if (photoPath.startsWith("/storage")) {
                            downloadAndCacheImage(record.staffId, "breakIn", photoPath)
                        }
                    }

                    record.breakOutPhoto?.let { photoPath ->
                        if (photoPath.startsWith("/storage")) {
                            downloadAndCacheImage(record.staffId, "breakOut", photoPath)
                        }
                    }
                }
            }
        }
    }

    private suspend fun downloadAndCacheImage(staffId: String, imageType: String, serverPath: String) {
        try {
            // Check if already cached locally
            val localFile = getLocalImageFile(staffId, imageType, selectedDate)
            if (localFile.exists()) {
                return
            }

            // Set loading state
            withContext(Dispatchers.Main) {
                setImageLoadingState(staffId, imageType, true)
            }

            val imageUrl = BASE_URL + serverPath

            // Download image
            val client = OkHttpClient()
            val request = Request.Builder().url(imageUrl).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.let { responseBody ->
                    localFile.parentFile?.mkdirs()

                    FileOutputStream(localFile).use { outputStream ->
                        responseBody.byteStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    // Update database with local path
                    updateAttendanceRecordWithLocalPath(staffId, imageType, localFile.absolutePath)

                    // Notify UI to refresh this specific item
                    withContext(Dispatchers.Main) {
                        setImageLoadingState(staffId, imageType, false)
                        notifyItemChanged(getPositionForStaffId(staffId))
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    setImageLoadingState(staffId, imageType, false)
                }
            }
        } catch (e: Exception) {
            Log.e("StaffAttendanceAdapter", "Error downloading image", e)
            withContext(Dispatchers.Main) {
                setImageLoadingState(staffId, imageType, false)
            }
        }
    }

    private fun getLocalImageFile(staffId: String, imageType: String, date: String): File {
        val cacheDir = File(context.cacheDir, "attendance_images")
        return File(cacheDir, "${staffId}_${imageType}_${date}.jpg")
    }

    private suspend fun updateAttendanceRecordWithLocalPath(staffId: String, imageType: String, localPath: String) {
        withContext(Dispatchers.IO) {
            try {
                val attendanceDao = AppDatabase.getDatabase(context).attendanceDao()
                val existingRecord = attendanceDao.getAttendanceForStaffOnDate(staffId, selectedDate)

                existingRecord?.let { record ->
                    val updatedRecord = when (imageType) {
                        "timeIn" -> record.copy(timeInPhoto = localPath)
                        "timeOut" -> record.copy(timeOutPhoto = localPath)
                        "breakIn" -> record.copy(breakInPhoto = localPath)
                        "breakOut" -> record.copy(breakOutPhoto = localPath)
                        else -> record
                    }
                    attendanceDao.updateAttendance(updatedRecord)
                }
            } catch (e: Exception) {
                Log.e("StaffAttendanceAdapter", "Error updating database with local path", e)
            }
        }
    }

    private fun setImageLoadingState(staffId: String, imageType: String, isLoading: Boolean) {
        val currentSet = loadingStates[staffId]?.toMutableSet() ?: mutableSetOf()
        if (isLoading) {
            currentSet.add(imageType)
        } else {
            currentSet.remove(imageType)
        }
        loadingStates[staffId] = currentSet
    }

    private fun isImageLoading(staffId: String, imageType: String): Boolean {
        return loadingStates[staffId]?.contains(imageType) == true
    }

    private fun getPositionForStaffId(staffId: String): Int {
        for (i in 0 until itemCount) {
            val staff = getItem(i)
            if ("${staff.name}_${staff.storeId}" == staffId) {
                return i
            }
        }
        return -1
    }

    inner class ViewHolder(private val binding: StaffAttendanceCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(staff: StaffEntity) {
            binding.apply {
                staffInfo.text = staff.name
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
                    val attendance = getAttendanceRecord(staff)
                    val today = PhilippinesServerTime.formatDatabaseDate()
                    val isToday = selectedDate == today

                    binding.apply {
                        // Time In handling
                        val hasTimeIn = attendance?.timeIn != null && attendance.timeIn.isNotEmpty()
                        timeInImage.apply {
                            isClickable = !hasTimeIn && isToday
                            setOnClickListener {
                                if (isClickable) {
                                    onImageClick(staff, AttendanceType.TIME_IN)
                                } else if (hasTimeIn) {
                                    // Show zoom dialog for existing time in
                                    showImageZoomDialog(
                                        "Time In",
                                        attendance?.timeIn,
                                        getImagePath(attendance, "timeIn")
                                    )
                                }
                            }
                            alpha = if (isClickable) 1.0f else 0.7f
                        }

                        // Break Status handling - FIXED to prevent clicks on completed breaks
                        val hasTimeOut = attendance?.timeOut != null && attendance.timeOut.isNotEmpty()
                        val breakCompleted = attendance?.breakIn != null && attendance.breakOut != null
                        val isOnBreak = attendance?.breakIn != null && attendance.breakOut == null

                        val canUseBreak = hasTimeIn && !hasTimeOut && !breakCompleted && isToday

                        breakButton.apply {
                            // Make completely non-clickable if break is completed or shift is done
                            isClickable = canUseBreak
                            isEnabled = canUseBreak

                            setOnClickListener {
                                if (canUseBreak) {
                                    // Only allow action if break is not completed
                                    showBreakConfirmationDialog(staff, attendance)
                                } else {
                                    // Show simple status for completed/non-actionable breaks
                                    showSimpleBreakStatus(attendance)
                                }
                            }
                            alpha = if (canUseBreak) 1.0f else 0.6f
                        }

                        // Time Out handling
                        val canTimeOut = hasTimeIn && !hasTimeOut && isToday &&
                                (attendance?.breakIn == null || attendance.breakOut != null)

                        timeOutImage.apply {
                            isClickable = canTimeOut
                            setOnClickListener {
                                if (isClickable) {
                                    onImageClick(staff, AttendanceType.TIME_OUT)
                                } else if (hasTimeOut) {
                                    // Show zoom dialog for existing time out
                                    showImageZoomDialog(
                                        "Time Out",
                                        attendance?.timeOut,
                                        getImagePath(attendance, "timeOut")
                                    )
                                }
                            }
                            alpha = if (isClickable) 1.0f else 0.7f
                        }
                    }
                } catch (e: Exception) {
                    Log.e("StaffAttendanceAdapter", "Error setting up click listeners", e)
                }
            }
        }

        private fun showSimpleBreakStatus(attendance: ServerAttendanceRecord?) {
            val message = when {
                attendance?.breakIn != null && attendance.breakOut != null -> {
                    val duration = calculateBreakDuration(attendance.breakIn, attendance.breakOut)
                    "Break completed\n\nBreak In: ${attendance.breakIn}\nBreak Out: ${attendance.breakOut}\nDuration: $duration"
                }
                attendance?.breakIn != null && attendance.breakOut == null -> {
                    "Currently on break since ${attendance.breakIn}"
                }
                attendance?.timeOut != null -> {
                    "Shift completed\nNo break actions available"
                }
                attendance?.timeIn == null -> {
                    "Please time in first to manage breaks"
                }
                else -> {
                    "Break not started yet"
                }
            }

            AlertDialog.Builder(context)
                .setTitle("Break Status")
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        private fun getImagePath(attendance: ServerAttendanceRecord?, imageType: String): String? {
            return when (imageType) {
                "timeIn" -> attendance?.timeInPhoto
                "timeOut" -> attendance?.timeOutPhoto
                "breakIn" -> attendance?.breakInPhoto
                "breakOut" -> attendance?.breakOutPhoto
                else -> null
            }
        }

        private fun showImageZoomDialog(title: String, time: String?, imagePath: String?) {
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_image_zoom)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val titleText = dialog.findViewById<TextView>(R.id.dialogTitle)
            val timeText = dialog.findViewById<TextView>(R.id.dialogTime)
            val imageView = dialog.findViewById<ImageView>(R.id.dialogImage)
            val closeButton = dialog.findViewById<TextView>(R.id.closeButton)

            titleText.text = title
            timeText.text = time ?: "No time recorded"

            // Load image
            if (!imagePath.isNullOrEmpty()) {
                if (imagePath.startsWith("/storage")) {
                    // Server image
                    Glide.with(context)
                        .load(BASE_URL + imagePath)
                        .placeholder(R.drawable.ic_camera_placeholder)
                        .error(R.drawable.ic_camera_placeholder)
                        .into(imageView)
                } else {
                    // Local image
                    Glide.with(context)
                        .load(File(imagePath))
                        .placeholder(R.drawable.ic_camera_placeholder)
                        .error(R.drawable.ic_camera_placeholder)
                        .into(imageView)
                }
            } else {
                imageView.setImageResource(R.drawable.ic_camera_placeholder)
            }

            closeButton.setOnClickListener { dialog.dismiss() }
            dialog.show()
        }

        private fun showBreakStatusDialog(attendance: ServerAttendanceRecord?) {
            val breakInfo = when {
                attendance?.breakIn != null && attendance.breakOut == null ->
                    "Currently on break since ${attendance.breakIn}"
                attendance?.breakIn != null && attendance.breakOut != null ->
                    "Break taken from ${attendance.breakIn} to ${attendance.breakOut}\nDuration: ${calculateBreakDuration(attendance.breakIn, attendance.breakOut)}"
                else -> "No break taken today"
            }

            AlertDialog.Builder(context)
                .setTitle("Break Status")
                .setMessage(breakInfo)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        // Replace your getAttendanceRecord method in the ViewHolder class with this:
        private suspend fun getAttendanceRecord(staff: StaffEntity): com.example.possystembw.DAO.ServerAttendanceRecord? {
            return try {
                val staffId = "${staff.name}_${staff.storeId}"
                // FIXED: Use the selectedDate from the adapter class, not from local scope
                val dateStr = this@StaffAttendanceAdapter.selectedDate
                val key = "${staffId}_${dateStr}"

                Log.d("StaffAttendanceAdapter", "Getting attendance record for key: $key")

                // FIXED: Always check server data first (this is updated immediately after photo capture)
                serverAttendanceData[key]?.let {
                    Log.d("StaffAttendanceAdapter", "Found server record: timeIn=${it.timeIn}, timeOut=${it.timeOut}")
                    return it
                }

                // FIXED: Also check local database and create server record format
                val localRecord = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(context).attendanceDao()
                        .getAttendanceForStaffOnDate(staffId, dateStr)
                }

                localRecord?.let { record ->
                    Log.d("StaffAttendanceAdapter", "Found local record: timeIn=${record.timeIn}, timeOut=${record.timeOut}")
                    com.example.possystembw.DAO.ServerAttendanceRecord(
                        id = record.id.toInt(),
                        staffId = record.staffId,
                        storeId = record.storeId,
                        date = record.date,
                        timeIn = record.timeIn?.takeIf { it.isNotEmpty() },
                        timeInPhoto = record.timeInPhoto?.takeIf { it.isNotEmpty() },
                        breakIn = record.breakIn,
                        breakInPhoto = record.breakInPhoto,
                        breakOut = record.breakOut,
                        breakOutPhoto = record.breakOutPhoto,
                        timeOut = record.timeOut?.takeIf { it.isNotEmpty() },
                        timeOutPhoto = record.timeOutPhoto?.takeIf { it.isNotEmpty() },
                        status = record.status,
                        created_at = "",
                        updated_at = ""
                    )
                }
            } catch (e: Exception) {
                Log.e("StaffAttendanceAdapter", "Error getting attendance record", e)
                null
            }
        }

        private fun showBreakConfirmationDialog(staff: StaffEntity, attendance: ServerAttendanceRecord?) {
            val isOnBreak = attendance?.breakIn != null && attendance.breakOut == null
            val message = if (isOnBreak) {
                "Are you sure you want to end your break?"
            } else {
                "Are you sure you want to start your break?"
            }

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
                    val attendance = getAttendanceRecord(staff)
                    val staffId = "${staff.name}_${staff.storeId}"

                    attendance?.let { record ->
                        binding.apply {
                            // Time In
                            record.timeIn?.let { time ->
                                timeInTime.text = time
                                loadAttendanceImage(timeInImage, record.timeInPhoto, staffId, "timeIn")
                            }

                            // Break Status
                            updateBreakStatus(record)

                            // Time Out
                            record.timeOut?.let { time ->
                                timeOutTime.text = time
                                loadAttendanceImage(timeOutImage, record.timeOutPhoto, staffId, "timeOut")
                            }

                            // Update time breakdown
                            updateTimeBreakdown(record)
                        }
                    } ?: run {
                        // No attendance record found
                        binding.apply {
                            staffStatus.text = "Not Started"
                            staffStatus.setTextColor(ContextCompat.getColor(context, R.color.gray))
                            workDuration.text = "0 hrs 0 mins"
                            breakDuration.text = "0 mins"
                            totalHours.text = "0 hrs 0 mins"
                        }
                    }
                } catch (e: Exception) {
                    Log.e("StaffAttendanceAdapter", "Error loading attendance data", e)
                }
            }
        }

        private fun loadAttendanceImage(imageView: CircleImageView, photoPath: String?, staffId: String, imageType: String) {
            Log.d("StaffAttendanceAdapter", "Loading image - staffId: $staffId, type: $imageType, path: $photoPath")

            if (photoPath.isNullOrEmpty()) {
                Log.d("StaffAttendanceAdapter", "Photo path is null or empty")
                imageView.setImageResource(R.drawable.ic_camera_placeholder)
                imageView.borderColor = ContextCompat.getColor(context, R.color.gray)
                return
            }

            // CRITICAL: Always check if file exists first
            val photoFile = File(photoPath)
            if (photoFile.exists()) {
                Log.d("StaffAttendanceAdapter", "File exists, loading: $photoPath")

                // Clear any previous image first
                imageView.setImageResource(R.drawable.ic_camera_placeholder)

                Glide.with(context)
                    .load(photoFile)
                    .apply(
                        RequestOptions()
                            .placeholder(R.drawable.ic_camera_placeholder)
                            .error(R.drawable.ic_camera_placeholder)
                            .centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.NONE) // Don't cache
                            .skipMemoryCache(true) // Force reload from file
                            .signature(ObjectKey(System.currentTimeMillis())) // Force refresh
                    )
                    .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<android.graphics.drawable.Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.e("StaffAttendanceAdapter", "Failed to load image: $photoPath", e)
                            imageView.borderColor = ContextCompat.getColor(context, R.color.red)
                            return false
                        }

                        override fun onResourceReady(
                            resource: android.graphics.drawable.Drawable?,
                            model: Any?,
                            target: Target<android.graphics.drawable.Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.d("StaffAttendanceAdapter", "Successfully loaded image: $photoPath")
                            imageView.borderColor = ContextCompat.getColor(context, R.color.green)
                            return false
                        }
                    })
                    .into(imageView)
                return
            }

            // If server path, load from URL
            if (photoPath.startsWith("/storage")) {
                val fullImageUrl = BASE_URL + photoPath
                Log.d("StaffAttendanceAdapter", "Loading server image: $fullImageUrl")

                Glide.with(context)
                    .load(fullImageUrl)
                    .apply(
                        RequestOptions()
                            .placeholder(R.drawable.ic_camera_placeholder)
                            .error(R.drawable.ic_camera_placeholder)
                            .centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                    )
                    .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<android.graphics.drawable.Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.e("StaffAttendanceAdapter", "Failed to load server image: $fullImageUrl", e)
                            imageView.borderColor = ContextCompat.getColor(context, R.color.red)
                            return false
                        }

                        override fun onResourceReady(
                            resource: android.graphics.drawable.Drawable?,
                            model: Any?,
                            target: Target<android.graphics.drawable.Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.d("StaffAttendanceAdapter", "Successfully loaded server image: $fullImageUrl")
                            imageView.borderColor = ContextCompat.getColor(context, R.color.green)
                            return false
                        }
                    })
                    .into(imageView)
            } else {
                Log.w("StaffAttendanceAdapter", "Unknown photo path format: $photoPath")
                imageView.setImageResource(R.drawable.ic_camera_placeholder)
                imageView.borderColor = ContextCompat.getColor(context, R.color.gray)
            }
        }

        private fun showLoadingState(imageView: CircleImageView) {
            imageView.setImageResource(R.drawable.ic_camera_placeholder)
            imageView.borderColor = ContextCompat.getColor(context, R.color.orange)
        }

        private fun updateTimeBreakdown(record: ServerAttendanceRecord) {
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

                    // Time calculations with accurate computation
                    if (record.timeIn != null) {
                        val result = calculateAccurateWorkTime(record)

                        workDuration.text = formatDuration(result.workMinutes)
                        breakDuration.text = if (result.breakMinutes > 0) {
                            formatDuration(result.breakMinutes)
                        } else {
                            "0 mins"
                        }
                        totalHours.text = formatDuration(result.workMinutes + result.breakMinutes)
                    } else {
                        workDuration.text = "0 hrs 0 mins"
                        breakDuration.text = "0 mins"
                        totalHours.text = "0 hrs 0 mins"
                    }
                }
            } catch (e: Exception) {
                Log.e("StaffAttendanceAdapter", "Error updating time breakdown", e)
            }
        }

        private fun calculateAccurateWorkTime(record: ServerAttendanceRecord): TimeCalculationResult {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            var totalWorkMinutes = 0L
            var totalBreakMinutes = 0L

            try {
                if (record.timeIn.isNullOrEmpty()) {
                    return TimeCalculationResult(0, 0)
                }

                val timeInDate = timeFormat.parse(record.timeIn) ?: return TimeCalculationResult(0, 0)
                val timeInCalendar = Calendar.getInstance().apply { time = timeInDate }

                val currentTime = Calendar.getInstance()

                // Convert to minutes since start of day for accurate calculation
                fun timeToMinutesSinceStartOfDay(timeStr: String): Long {
                    return try {
                        val time = timeFormat.parse(timeStr) ?: return 0L
                        val cal = Calendar.getInstance().apply { this.time = time }
                        (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)).toLong()
                    } catch (e: Exception) {
                        Log.e("StaffAttendanceAdapter", "Error parsing time: $timeStr", e)
                        0L
                    }
                }

                val timeInMinutes = timeToMinutesSinceStartOfDay(record.timeIn)
                var workEndMinutes: Long

                when {
                    // Case 1: Has time out (shift completed)
                    !record.timeOut.isNullOrEmpty() -> {
                        workEndMinutes = timeToMinutesSinceStartOfDay(record.timeOut)
                        totalWorkMinutes = workEndMinutes - timeInMinutes

                        // Subtract break time if any
                        if (!record.breakIn.isNullOrEmpty() && !record.breakOut.isNullOrEmpty()) {
                            val breakInMinutes = timeToMinutesSinceStartOfDay(record.breakIn)
                            val breakOutMinutes = timeToMinutesSinceStartOfDay(record.breakOut)
                            totalBreakMinutes = breakOutMinutes - breakInMinutes
                            totalWorkMinutes -= totalBreakMinutes
                        }
                    }

                    // Case 2: Currently on break
                    !record.breakIn.isNullOrEmpty() && record.breakOut.isNullOrEmpty() -> {
                        val breakInMinutes = timeToMinutesSinceStartOfDay(record.breakIn)
                        totalWorkMinutes = breakInMinutes - timeInMinutes

                        // Calculate current break duration
                        val currentMinutes = currentTime.get(Calendar.HOUR_OF_DAY) * 60 + currentTime.get(Calendar.MINUTE)
                        totalBreakMinutes = currentMinutes - breakInMinutes
                    }

                    // Case 3: Returned from break, still working
                    !record.breakIn.isNullOrEmpty() && !record.breakOut.isNullOrEmpty() && record.timeOut.isNullOrEmpty() -> {
                        val breakInMinutes = timeToMinutesSinceStartOfDay(record.breakIn)
                        val breakOutMinutes = timeToMinutesSinceStartOfDay(record.breakOut)
                        val currentMinutes = currentTime.get(Calendar.HOUR_OF_DAY) * 60 + currentTime.get(Calendar.MINUTE)

                        // Work before break + work after break
                        val workBeforeBreak = breakInMinutes - timeInMinutes
                        val workAfterBreak = currentMinutes - breakOutMinutes
                        totalWorkMinutes = workBeforeBreak + workAfterBreak
                        totalBreakMinutes = breakOutMinutes - breakInMinutes
                    }

                    // Case 4: Still working, no break
                    else -> {
                        val currentMinutes = currentTime.get(Calendar.HOUR_OF_DAY) * 60 + currentTime.get(Calendar.MINUTE)
                        totalWorkMinutes = currentMinutes - timeInMinutes
                    }
                }

                // Ensure no negative values and reasonable limits
                totalWorkMinutes = maxOf(0, minOf(totalWorkMinutes, 24 * 60)) // Max 24 hours
                totalBreakMinutes = maxOf(0, minOf(totalBreakMinutes, 12 * 60)) // Max 12 hours break

                Log.d("StaffAttendanceAdapter",
                    "Time calculation - Work: ${totalWorkMinutes}min, Break: ${totalBreakMinutes}min for ${record.staffId}")

            } catch (e: Exception) {
                Log.e("StaffAttendanceAdapter", "Error calculating work time for ${record.staffId}", e)
                return TimeCalculationResult(0, 0)
            }

            return TimeCalculationResult(totalWorkMinutes, totalBreakMinutes)
        }

        private fun formatDuration(minutes: Long): String {
            val hours = minutes / 60
            val mins = minutes % 60
            return when {
                hours > 0 -> "${hours} hrs ${mins} mins"
                else -> "${mins} mins"
            }
        }

        private fun updateBreakStatus(record: ServerAttendanceRecord) {
            binding.apply {
                val breakStarted = record.breakIn != null && record.breakOut == null
                val breakCompleted = record.breakIn != null && record.breakOut != null
                val hasTimeOut = record.timeOut != null && record.timeOut.isNotEmpty()

                breakButton.apply {
                    // Set background color based on status
                    setCardBackgroundColor(ContextCompat.getColor(context, when {
                        breakCompleted -> R.color.green  // Completed - green (non-clickable)
                        breakStarted -> R.color.red      // On break - red (clickable to end)
                        hasTimeOut -> R.color.gray       // Shift completed - gray (non-clickable)
                        else -> R.color.orange           // Available - orange (clickable to start)
                    }))

                    // Ensure visual feedback matches clickability
                    alpha = when {
                        breakCompleted || hasTimeOut -> 0.6f  // Visually disabled
                        else -> 1.0f  // Visually enabled
                    }
                }

                breakButtonText.text = when {
                    breakCompleted -> "Break\nCompleted"
                    breakStarted -> "End\nBreak"
                    hasTimeOut -> "Shift\nCompleted"
                    else -> "Start\nBreak"
                }

                breakStatus.apply {
                    text = when {
                        breakCompleted -> "Break: ${calculateBreakDuration(record.breakIn!!, record.breakOut!!)}"
                        breakStarted -> "On Break (${record.breakIn})"
                        hasTimeOut -> "Shift completed"
                        else -> "No break taken"
                    }
                    setTextColor(ContextCompat.getColor(context, when {
                        breakCompleted -> R.color.green
                        breakStarted -> R.color.red
                        hasTimeOut -> R.color.gray
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
                    return formatDuration(durationMinutes)
                }
            } catch (e: Exception) {
                Log.e("StaffAttendanceAdapter", "Error calculating break duration", e)
            }
            return "Unknown duration"
        }

        private fun loadImageWithGlide(imageView: CircleImageView, photoPath: String) {
            Glide.with(context)
                .load(File(photoPath))
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.ic_camera_placeholder)
                        .error(R.drawable.ic_camera_placeholder)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.NONE) // Don't cache locally captured images
                        .skipMemoryCache(false) // Allow memory cache for better performance
                )
                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("StaffAttendanceAdapter", "Failed to load image: $photoPath", e)
                        // Set a visual indicator that image failed to load
                        imageView.borderColor = ContextCompat.getColor(context, R.color.red)
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?,
                        model: Any?,
                        target: Target<android.graphics.drawable.Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Set border color to indicate successful load
                        imageView.borderColor = ContextCompat.getColor(context, R.color.green)
                        return false
                    }
                })
                .into(imageView)
        }
    }
    fun refreshStaffAttendance(staffId: String) {
        val position = getPositionForStaffId(staffId)
        if (position != -1) {
            notifyItemChanged(position)
        }
    }
    data class TimeCalculationResult(
        val workMinutes: Long,
        val breakMinutes: Long
    )

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