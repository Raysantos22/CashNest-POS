package com.example.possystembw.DAO

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val philippineTimeZone = TimeZone.getTimeZone("Asia/Manila")

    fun formatToPhilippineTime(date: Date?): String {
        if (date == null) return getCurrentDateString()

        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            format.timeZone = philippineTimeZone
            format.format(date)
        } catch (e: Exception) {
            Log.e("DateUtils", "Error formatting date: ${e.message}")
            getCurrentDateString()
        }
    }

    fun getCurrentDateString(): String {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            format.timeZone = philippineTimeZone
            format.format(Date())
        } catch (e: Exception) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        }
    }

    fun isDateInRange(dateString: String, startDate: Date, endDate: Date): Boolean {
        return try {
            val transactionDate = dateString.toDateObject()
            transactionDate.time >= startDate.time && transactionDate.time <= endDate.time
        } catch (e: Exception) {
            Log.e("DateUtils", "Error checking date range: ${e.message}")
            false
        }
    }
}