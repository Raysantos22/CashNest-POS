package com.example.possystembw.DAO

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NoInternetException(message: String) : Exception(message)
class ServerTimeException(message: String) : Exception(message)

object PhilippinesServerTime {
    private var timeOffset: Long = 0
    private const val PHILIPPINES_TIMEZONE = "Asia/Manila"
    private const val TAG = "PhilippinesServerTime"
    private const val TIMEOUT_MS = 5000

    private val TIME_SERVERS = listOf(
        "https://www.google.com",
        "https://www.microsoft.com",
        "https://www.apple.com"
    )

    suspend fun initialize(context: Context) {
        if (!isInternetAvailable(context)) {
            throw NoInternetException("Internet connection required for attendance")
        }

        try {
            val serverTime = getAccurateTime()
            if (serverTime > 0) {
                // Convert to Philippines time
                val philippinesTime = convertToPhilippinesTime(serverTime)
                timeOffset = philippinesTime - System.currentTimeMillis()
                Log.d(TAG, "Time sync successful. Server time: ${Date(serverTime)}")
                Log.d(TAG, "Philippines time: ${Date(philippinesTime)}")
                Log.d(TAG, "Offset: $timeOffset")
            } else {
                throw ServerTimeException("Could not get accurate time")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing time service", e)
            throw ServerTimeException("Failed to initialize server time: ${e.message}")
        }
    }

    private suspend fun getAccurateTime(): Long = withContext(Dispatchers.IO) {
        var mostAccurateTime: Long = 0
        var successCount = 0
        val times = mutableListOf<Long>()

        // Try each server multiple times
        repeat(3) { // Try 3 times
            for (serverUrl in TIME_SERVERS) {
                try {
                    val url = URL(serverUrl)
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = TIMEOUT_MS
                        readTimeout = TIMEOUT_MS
                        setRequestProperty("User-Agent", "Android")
                    }

                    try {
                        val startTime = System.nanoTime()
                        connection.connect()
                        val serverTime = connection.date
                        val endTime = System.nanoTime()
                        val roundTripTime = TimeUnit.NANOSECONDS.toMillis(endTime - startTime)

                        if (serverTime > 0 && roundTripTime < 1000) { // Only accept responses faster than 1 second
                            times.add(serverTime + (roundTripTime / 2)) // Add half RTT for better accuracy
                            successCount++
                        }
                    } finally {
                        connection.disconnect()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get time from $serverUrl: ${e.message}")
                    continue
                }
            }
        }

        // Get median time for better accuracy
        if (times.isNotEmpty()) {
            times.sort()
            mostAccurateTime = if (times.size % 2 == 0) {
                (times[times.size / 2] + times[(times.size / 2) - 1]) / 2
            } else {
                times[times.size / 2]
            }
        }

        return@withContext mostAccurateTime
    }

    private fun convertToPhilippinesTime(serverTime: Long): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(PHILIPPINES_TIMEZONE))
        calendar.timeInMillis = serverTime

        // Ensure we're in Philippines timezone
        calendar.timeZone = TimeZone.getTimeZone(PHILIPPINES_TIMEZONE)

        return calendar.timeInMillis
    }

    fun getCurrentTime(): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(PHILIPPINES_TIMEZONE))
        calendar.timeInMillis = System.currentTimeMillis() + timeOffset
        return calendar.timeInMillis
    }

    fun formatDatabaseTime(): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(PHILIPPINES_TIMEZONE))
        calendar.timeInMillis = getCurrentTime()
        return SimpleDateFormat("hh:mm a", Locale.getDefault())
            .apply { timeZone = TimeZone.getTimeZone(PHILIPPINES_TIMEZONE) }
            .format(calendar.time)
    }
    fun formatDatabaseTimeForServer(): String {
        // Keep 24-hour format for server communication if needed
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(PHILIPPINES_TIMEZONE))
        calendar.timeInMillis = getCurrentTime()
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            .apply { timeZone = TimeZone.getTimeZone(PHILIPPINES_TIMEZONE) }
            .format(calendar.time)
    }
    fun formatDatabaseDate(): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(PHILIPPINES_TIMEZONE))
        calendar.timeInMillis = getCurrentTime()
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .apply { timeZone = TimeZone.getTimeZone(PHILIPPINES_TIMEZONE) }
            .format(calendar.time)
    }

    fun formatDisplayTime(): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(PHILIPPINES_TIMEZONE))
        calendar.timeInMillis = getCurrentTime()
        return SimpleDateFormat("hh:mm a", Locale.getDefault())
            .apply { timeZone = TimeZone.getTimeZone(PHILIPPINES_TIMEZONE) }
            .format(calendar.time)
    }

    fun formatDisplayDate(): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(PHILIPPINES_TIMEZONE))
        calendar.timeInMillis = getCurrentTime()
        return SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            .apply { timeZone = TimeZone.getTimeZone(PHILIPPINES_TIMEZONE) }
            .format(calendar.time)
    }

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun syncTimeWithRetry(context: Context, maxRetries: Int = 3): Boolean {
        repeat(maxRetries) { attempt ->
            try {
                initialize(context)
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Sync attempt ${attempt + 1} failed", e)
                if (attempt == maxRetries - 1) {
                    throw e
                }
                kotlinx.coroutines.delay(1000)
            }
        }
        return false
    }
}