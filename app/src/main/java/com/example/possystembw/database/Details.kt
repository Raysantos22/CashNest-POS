package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "details")
data class Details(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "fgencoder") val fgEncoder: String,
    @ColumnInfo(name = "plencoder") val plEncoder: String,
    @ColumnInfo(name = "dispatcher") val dispatcher: String,
    @ColumnInfo(name = "logistics") val logistics: String,
    @ColumnInfo(name = "routes") val routes: String,
    @ColumnInfo(name = "createddate") val createdDate: Long,
    @ColumnInfo(name = "deliverydate") val deliveryDate: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
