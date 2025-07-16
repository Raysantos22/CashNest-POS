package com.example.possystembw.DAO

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private const val TIMEZONE_MANILA = "Asia/Manila"
    private const val DATE_FORMAT_ISO = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    private const val DATE_FORMAT_DISPLAY = "yyyy-MM-dd HH:mm:ss"
    
    val philippineTimeZone: TimeZone = TimeZone.getTimeZone(TIMEZONE_MANILA)
    
    fun getCurrentPhilippineTime(): Date {
        val calendar = Calendar.getInstance(philippineTimeZone)
        return calendar.time
    }
    
    fun formatToPhilippineTime(date: Date?): String {
        return try {
            SimpleDateFormat(DATE_FORMAT_ISO, Locale.US).apply {
                timeZone = philippineTimeZone
            }.format(date ?: getCurrentPhilippineTime())
        } catch (e: Exception) {
            SimpleDateFormat(DATE_FORMAT_ISO, Locale.US).apply {
                timeZone = philippineTimeZone
            }.format(getCurrentPhilippineTime())
        }
    }
    
    fun formatToDisplayTime(date: Date?): String {
        return try {
            SimpleDateFormat(DATE_FORMAT_DISPLAY, Locale.US).apply {
                timeZone = philippineTimeZone
            }.format(date ?: getCurrentPhilippineTime())
        } catch (e: Exception) {
            SimpleDateFormat(DATE_FORMAT_DISPLAY, Locale.US).apply {
                timeZone = philippineTimeZone
            }.format(getCurrentPhilippineTime())
        }
    }
    
    fun parseFromString(dateString: String?): Date? {
        return try {
            if (dateString.isNullOrBlank()) return null
            SimpleDateFormat(DATE_FORMAT_ISO, Locale.US).apply {
                timeZone = philippineTimeZone
            }.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
    
    fun getPhilippineTimestamp(): Long {
        return getCurrentPhilippineTime().time
    }
}
