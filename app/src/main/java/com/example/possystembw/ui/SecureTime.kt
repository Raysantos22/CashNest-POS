package com.example.possystembw.ui

import android.content.Context
import android.os.SystemClock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object SecureTime {
    private val philippinesTimeZone = TimeZone.getTimeZone("Asia/Manila")
    private var bootTime: Long = 0
    private var baseTime: Long = 0
    
    fun initialize(context: Context) {
        // Get the device boot time which cannot be changed without root access
        bootTime = SystemClock.elapsedRealtime()
        // Store the initial time as base
        baseTime = System.currentTimeMillis()
    }

    fun getCurrentTime(): Long {
        // Calculate time based on unchangeable elapsedRealtime
        val elapsedSinceBoot = SystemClock.elapsedRealtime() - bootTime
        return baseTime + elapsedSinceBoot
    }

    fun formatDisplayTime(): String {
        val calendar = Calendar.getInstance(philippinesTimeZone).apply {
            timeInMillis = getCurrentTime()
        }
        return SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).apply {
            timeZone = philippinesTimeZone
        }.format(calendar.time)
    }

    fun formatDisplayDate(): String {
        val calendar = Calendar.getInstance(philippinesTimeZone).apply {
            timeInMillis = getCurrentTime()
        }
        return SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).apply {
            timeZone = philippinesTimeZone
        }.format(calendar.time)
    }

    fun formatDatabaseTime(): String {
        val calendar = Calendar.getInstance(philippinesTimeZone).apply {
            timeInMillis = getCurrentTime()
        }
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply {
            timeZone = philippinesTimeZone
        }.format(calendar.time)
    }

    fun formatDatabaseDate(): String {
        val calendar = Calendar.getInstance(philippinesTimeZone).apply {
            timeInMillis = getCurrentTime()
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = philippinesTimeZone
        }.format(calendar.time)
    }
}