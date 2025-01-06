package com.example.possystembw.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val staffId: String,
    val storeId: String,
    val date: String,
    val timeIn: String,
    val timeInPhoto: String,
    val breakIn: String? = null,
    val breakInPhoto: String? = null,
    val breakOut: String? = null,
    val breakOutPhoto: String? = null,
    val timeOut: String? = null,
    val timeOutPhoto: String? = null,
    val status: String = "ACTIVE"
)