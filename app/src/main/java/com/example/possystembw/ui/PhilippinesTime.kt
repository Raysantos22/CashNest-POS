package com.example.possystembw.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object PhilippinesTime {
    private val philippinesTimeZone = TimeZone.getTimeZone("Asia/Manila")

    fun getCurrentDateTime(): Calendar {
        return Calendar.getInstance(philippinesTimeZone)
    }

    fun formatDisplayTime(calendar: Calendar = getCurrentDateTime()): String {
        return SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).apply {
            timeZone = philippinesTimeZone
        }.format(calendar.time)
    }

    fun formatDisplayDate(calendar: Calendar = getCurrentDateTime()): String {
        return SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).apply {
            timeZone = philippinesTimeZone
        }.format(calendar.time)
    }

    fun formatDatabaseTime(calendar: Calendar = getCurrentDateTime()): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply {
            timeZone = philippinesTimeZone
        }.format(calendar.time)
    }

    fun formatDatabaseDate(calendar: Calendar = getCurrentDateTime()): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = philippinesTimeZone
        }.format(calendar.time)
    }
}