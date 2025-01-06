package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.possystembw.database.AttendanceRecord
import com.example.possystembw.database.StaffEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Dao
interface AttendanceDao {
    @Insert
    suspend fun insertAttendance(attendance: AttendanceRecord)

    @Update
    suspend fun updateAttendance(attendance: AttendanceRecord)

    @Query("""
        SELECT * FROM attendance_records 
        WHERE staffId = :staffId 
        AND date = :date 
        AND timeIn IS NOT NULL 
        LIMIT 1
    """)
    suspend fun getAttendanceForStaffOnDate(staffId: String, date: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE storeId = :storeId AND date = :date")
    suspend fun getAttendanceForStore(storeId: String, date: String): List<AttendanceRecord>

    @Query("DELETE FROM attendance_records WHERE storeId = :storeId AND date < :date")
    suspend fun deleteOldRecords(storeId: String, date: String)
    @Query("SELECT * FROM attendance_records WHERE staffId = :staffId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getAttendanceForStaffBetweenDates(staffId: String, startDate: String, endDate: String): List<AttendanceRecord>

    @Query("SELECT s.* FROM staff s JOIN attendance_records a ON s.storeId = a.storeId WHERE s.storeId = :storeId")
    suspend fun getStaffByStoreAttendance(storeId: String): List<StaffEntity>

    @Query("DELETE FROM attendance_records WHERE date = :date AND staffId = :staffId")
    suspend fun deleteAttendanceForDate(staffId: String, date: String)

    // Use this to clean up if needed


    @Transaction
    suspend fun refreshStaffData(storeId: String, records: List<AttendanceRecord>) {
        deleteOldRecords(
            storeId,
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
        records.forEach { record ->
            insertAttendance(record)
        }
    }
}