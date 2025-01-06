package com.example.possystembw.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "printer_settings")
data class PrinterSettings(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val printerName: String,
    val macAddress: String,
    val isDefault: Boolean = false,
    val windowId: Int? = null,
    val lastConnected: Date? = null
)