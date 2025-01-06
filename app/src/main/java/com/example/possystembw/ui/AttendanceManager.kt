package com.example.possystembw.ui

import android.content.Context
import com.example.possystembw.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AttendanceManager(private val context: Context) {
    private val attendanceDao = AppDatabase.getDatabase(context).attendanceDao()
    
    suspend fun cleanupIncorrectAttendance(staffId: String, date: String) {
        withContext(Dispatchers.IO) {
            attendanceDao.deleteAttendanceForDate(staffId, date)
        }
    }

    suspend fun validateAndCleanupAttendance(staffId: String, date: String) {
        withContext(Dispatchers.IO) {
            val attendance = attendanceDao.getAttendanceForStaffOnDate(staffId, date)
            
            // If there's an attendance record but no timeIn, it's invalid
            if (attendance != null && attendance.timeIn == null) {
                cleanupIncorrectAttendance(staffId, date)
            }
            
            // If dates don't match exactly, clean up
            if (attendance != null && attendance.date != date) {
                cleanupIncorrectAttendance(staffId, date)
            }
        }
    }

    suspend fun validateAttendanceSequence(staffId: String, date: String): Boolean {
        val attendance = attendanceDao.getAttendanceForStaffOnDate(staffId, date)
        
        return when {
            attendance == null -> true // Can time in
            attendance.timeIn == null -> false // Invalid state
            attendance.breakIn != null && attendance.breakOut == null -> true // Can break out
            attendance.breakOut != null && attendance.timeOut == null -> true // Can time out
            attendance.timeOut != null -> false // Day complete
            else -> true // Can break in or time out
        }
    }
}