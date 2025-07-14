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

        private suspend fun getAttendanceRecord(staff: StaffEntity): ServerAttendanceRecord? {
            val staffId = "${staff.name}_${staff.storeId}"
            val key = "${staffId}_${selectedDate}"

            // First try server data
            serverAttendanceData[key]?.let { return it }

            // Fallback to local database
            return try {
                val localRecord = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(context).attendanceDao()
                        .getAttendanceForStaffOnDate(staffId, selectedDate)
                }

                localRecord?.let {
                    ServerAttendanceRecord(
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
                Log.e("StaffAttendanceAdapter", "Error getting local attendance record", e)
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
            if (photoPath.isNullOrEmpty()) return

            // Check if currently loading
            if (isImageLoading(staffId, imageType)) {
                showLoadingState(imageView)
                return
            }

            // Try local file first
            val localFile = getLocalImageFile(staffId, imageType, selectedDate)
            if (localFile.exists()) {
                loadImageWithGlide(imageView, localFile.absolutePath)
                imageView.borderColor = ContextCompat.getColor(context, R.color.green)
                return
            }

            // If server path, check if it's being downloaded
            if (photoPath.startsWith("/storage")) {
                showLoadingState(imageView)
            } else {
                // Local path - load directly
                loadImageWithGlide(imageView, photoPath)
                imageView.borderColor = ContextCompat.getColor(context, R.color.green)
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
                val timeInDate = timeFormat.parse(record.timeIn!!) ?: return TimeCalculationResult(0, 0)
                val currentTime = Calendar.getInstance().time

                // Determine end time for work calculation
                val workEndTime = when {
                    record.timeOut != null -> timeFormat.parse(record.timeOut)
                    record.breakIn != null && record.breakOut == null -> timeFormat.parse(record.breakIn) // Currently on break
                    else -> currentTime // Still working
                } ?: currentTime

                // Calculate total time from time in to end
                val totalMinutes = (workEndTime.time - timeInDate.time) / (60 * 1000)

                // Calculate break time
                if (record.breakIn != null) {
                    val breakInDate = timeFormat.parse(record.breakIn) ?: timeInDate

                    if (record.breakOut != null) {
                        // Break completed
                        val breakOutDate = timeFormat.parse(record.breakOut) ?: breakInDate
                        totalBreakMinutes = (breakOutDate.time - breakInDate.time) / (60 * 1000)

                        // If there's time after break out
                        if (record.timeOut != null) {
                            val timeOutDate = timeFormat.parse(record.timeOut) ?: breakOutDate
                            val workAfterBreak = (timeOutDate.time - breakOutDate.time) / (60 * 1000)
                            totalWorkMinutes = totalMinutes - totalBreakMinutes
                        } else {
                            // Still working after break
                            val workAfterBreak = (currentTime.time - breakOutDate.time) / (60 * 1000)
                            totalWorkMinutes = totalMinutes - totalBreakMinutes + workAfterBreak
                        }
                    } else {
                        // Currently on break
                        totalBreakMinutes = (currentTime.time - breakInDate.time) / (60 * 1000)
                        totalWorkMinutes = (breakInDate.time - timeInDate.time) / (60 * 1000)
                    }
                } else {
                    // No break taken
                    totalWorkMinutes = totalMinutes
                }

                // Ensure no negative values
                totalWorkMinutes = maxOf(0, totalWorkMinutes)
                totalBreakMinutes = maxOf(0, totalBreakMinutes)

            } catch (e: Exception) {
                Log.e("StaffAttendanceAdapter", "Error calculating work time", e)
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
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                )
                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("StaffAttendanceAdapter", "Failed to load image: $photoPath", e)
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?,
                        model: Any?,
                        target: Target<android.graphics.drawable.Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                })
                .into(imageView)
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