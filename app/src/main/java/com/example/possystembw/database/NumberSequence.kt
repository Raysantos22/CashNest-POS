package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "number_sequences",
    indices = [Index(value = ["sequenceType", "storeId"], unique = true)]
)
data class NumberSequence(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sequenceType: String,
    val storeId: String,
    var currentValue: Long,
    var prefix: String = "",
    var suffix: String = "",
    var paddingLength: Int = 10,  // Changed to 10 for 0000000001 format
    var lastResetDate: Date? = null,
    @ColumnInfo(defaultValue = "1")
    var increment: Int = 1,
    @ColumnInfo(defaultValue = "true")
    var isActive: Boolean = true,
    var storeKey: String = "" // Format: transactionId + storeId
)