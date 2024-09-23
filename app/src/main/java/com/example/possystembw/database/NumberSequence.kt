package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "number_sequences")
data class NumberSequence(
    @PrimaryKey
    val sequenceType: String,  // e.g., "TRANSACTION", "RECEIPT", etc.
    var currentValue: Long,
    var prefix: String = "",
    var suffix: String = "",
    var paddingLength: Int = 9,
    var lastResetDate: Date? = null,
    @ColumnInfo(defaultValue = "1")
    var increment: Int = 1,
    @ColumnInfo(defaultValue = "false")
    var isActive: Boolean = true
)