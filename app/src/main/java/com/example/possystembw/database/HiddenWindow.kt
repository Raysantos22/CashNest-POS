
package com.example.possystembw.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_windows")
data class HiddenWindow(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val windowId: Int? = null,
    val windowTableId: Int? = null
)