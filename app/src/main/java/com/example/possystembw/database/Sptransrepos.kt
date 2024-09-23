package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sptransrepos")
data class Sptransrepos(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "JOURNALID") val journalId: Int,
    @ColumnInfo(name = "LINENUM") val lineNum: Int,
    @ColumnInfo(name = "TRANSDATE") val transDate: Long,
    @ColumnInfo(name = "ITEMID") val itemId: String,
    @ColumnInfo(name = "COUNTED") val counted: Int,
    @ColumnInfo(name = "STORENAME") val storeName: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
