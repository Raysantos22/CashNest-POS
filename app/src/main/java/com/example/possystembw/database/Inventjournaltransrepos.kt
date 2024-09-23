package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventjournaltransrepos")
data class Inventjournaltransrepos(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "JOURNALID") val journalId: String,
    @ColumnInfo(name = "LINENUM") val lineNum: Int,
    @ColumnInfo(name = "TRANSDATE") val transDate: Long,
    @ColumnInfo(name = "ITEMID") val itemId: String,
    @ColumnInfo(name = "COUNTED") val counted: Int,
    @ColumnInfo(name = "STORENAME") val storeName: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "moq") val moq: Int,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "itemdepartment") val itemDepartment: String
)
