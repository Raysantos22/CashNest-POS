package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventtables")
data class Inventtables(
    @PrimaryKey(autoGenerate = true) val itemGroupId: Int = 0,
    @ColumnInfo(name = "itemid") val itemId: String,
    @ColumnInfo(name = "itemname") val itemName: String,
    @ColumnInfo(name = "itemtype") val itemType: String,
    @ColumnInfo(name = "namealias") val nameAlias: String,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
