package com.example.possystembw.ui

import android.content.Context
import android.content.SharedPreferences
import com.example.possystembw.DAO.NumberSequenceValue
import com.example.possystembw.DAO.ServerAttendanceRecord
import com.example.possystembw.database.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SessionManager {
    private const val PREF_NAME = "AppSession"
    private const val KEY_USER = "user"
    private const val KEY_NUMBER_SEQUENCE = "number_sequence"
    private const val KEY_WEB_COOKIES = "web_cookies"
    private const val KEY_SESSION_TIMESTAMP = "session_timestamp"
    private const val KEY_ATTENDANCE_DATA = "attendance_data"
    private const val KEY_ATTENDANCE_TIMESTAMP = "attendance_timestamp"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setCurrentUser(user: User) {
        if (user.storeid.isNullOrEmpty()) {
            throw IllegalArgumentException("Cannot set user without store ID")
        }
        val userJson = gson.toJson(user)
        prefs.edit()
            .putString(KEY_USER, userJson)
            .putLong(KEY_SESSION_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun getCurrentUser(): User? {
        val userJson = prefs.getString(KEY_USER, null)
        return if (userJson != null) {
            try {
                gson.fromJson(userJson, User::class.java)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    fun setWebSessionCookies(cookies: String?) {
        cookies?.let {
            prefs.edit()
                .putString(KEY_WEB_COOKIES, it)
                .putLong(KEY_SESSION_TIMESTAMP, System.currentTimeMillis())
                .apply()
        }
    }

    fun getWebSessionCookies(): String? {
        return prefs.getString(KEY_WEB_COOKIES, null)
    }

    fun setAttendanceData(attendanceList: List<ServerAttendanceRecord>) {
        val attendanceJson = gson.toJson(attendanceList)
        prefs.edit()
            .putString(KEY_ATTENDANCE_DATA, attendanceJson)
            .putLong(KEY_ATTENDANCE_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun getAttendanceData(): List<ServerAttendanceRecord> {
        val attendanceJson = prefs.getString(KEY_ATTENDANCE_DATA, null)
        return if (attendanceJson != null) {
            try {
                val type = object : TypeToken<List<ServerAttendanceRecord>>() {}.type
                gson.fromJson(attendanceJson, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()
    }

    fun getAttendanceForStaff(staffId: String): List<ServerAttendanceRecord> {
        return getAttendanceData().filter { it.staffId == staffId }
    }

    fun getAttendanceForDate(date: String): List<ServerAttendanceRecord> {
        return getAttendanceData().filter { it.date == date }
    }

    fun getAttendanceForStaffAndDate(staffId: String, date: String): ServerAttendanceRecord? {
        return getAttendanceData().find { it.staffId == staffId && it.date == date }
    }

    fun isAttendanceDataFresh(maxAgeMinutes: Int = 30): Boolean {
        val timestamp = prefs.getLong(KEY_ATTENDANCE_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()
        val ageInMinutes = (currentTime - timestamp) / (1000 * 60)
        return ageInMinutes <= maxAgeMinutes
    }

    fun clearAttendanceData() {
        prefs.edit()
            .remove(KEY_ATTENDANCE_DATA)
            .remove(KEY_ATTENDANCE_TIMESTAMP)
            .apply()
    }

    fun clearCurrentUser() {
        prefs.edit()
            .remove(KEY_USER)
            .remove(KEY_WEB_COOKIES)
            .remove(KEY_SESSION_TIMESTAMP)
            .remove(KEY_ATTENDANCE_DATA)
            .remove(KEY_ATTENDANCE_TIMESTAMP)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return getCurrentUser() != null
    }

    fun isWebSessionActive(): Boolean {
        return getWebSessionCookies() != null
    }

    fun setCurrentNumberSequence(numberSequence: NumberSequenceValue) {
        val numberSequenceJson = gson.toJson(numberSequence)
        prefs.edit()
            .putString(KEY_NUMBER_SEQUENCE, numberSequenceJson)
            .apply()
    }

    fun getCurrentNumberSequence(): NumberSequenceValue? {
        val numberSequenceJson = prefs.getString(KEY_NUMBER_SEQUENCE, null)
        return if (numberSequenceJson != null) {
            try {
                gson.fromJson(numberSequenceJson, NumberSequenceValue::class.java)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    // New method to refresh session
    fun refreshSession() {
        if (isLoggedIn()) {
            prefs.edit()
                .putLong(KEY_SESSION_TIMESTAMP, System.currentTimeMillis())
                .apply()
        }
    }

    // Method to check if session is valid (always returns true unless explicitly logged out)
    fun isSessionValid(): Boolean {
        return isLoggedIn()
    }

    // Helper method to get current store ID
    fun getCurrentStoreId(): String? {
        return getCurrentUser()?.storeid
    }

    // Helper method to refresh attendance data if needed
    fun shouldRefreshAttendanceData(): Boolean {
        return !isAttendanceDataFresh() || getAttendanceData().isEmpty()
    }
}