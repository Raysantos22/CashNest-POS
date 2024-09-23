package com.example.possystembw.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "windowtable")
data class WindowTable(
    @PrimaryKey val id: Int,
    val description: String
)
data class WindowTableResponse(
    val windowtables: List<WindowTable>
)