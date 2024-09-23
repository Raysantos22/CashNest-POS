package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "windows")
data class Window(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val windownum: Int
)