package com.example.possystembw.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ar_table")
data class AR(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ar: String
)
