package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rbospecialgroups")
data class Rbospecialgroups(
    @PrimaryKey(autoGenerate = true) val groupId: Int = 0,
    @ColumnInfo(name = "NAME") val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
