package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventjournaltables")
data class Inventjournaltables(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "JOURNALID") val journalId: String,
    @ColumnInfo(name = "DESCRIPTION") val description: String,
    @ColumnInfo(name = "POSTED") val posted: Boolean,
    @ColumnInfo(name = "POSTEDDATETIME") val postedDateTime: Long,
    @ColumnInfo(name = "JOURNALTYPE") val journalType: String,
    @ColumnInfo(name = "DELETEPOSTEDLINES") val deletePostedLines: Boolean,
    @ColumnInfo(name = "CREATEDDATETIME") val createdDateTime: Long,
    @ColumnInfo(name = "STOREID") val storeId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "OPICPOSTED") val opicPosted: Boolean,
    @ColumnInfo(name = "FGENCODER") val fgEncoder: String,
    @ColumnInfo(name = "PLENCODER") val plEncoder: String,
    @ColumnInfo(name = "DISPATCHER") val dispatcher: String,
    @ColumnInfo(name = "LOGISTICS") val logistics: String,
    @ColumnInfo(name = "DELIVERYDATE") val deliveryDate: Long,
    @ColumnInfo(name = "orangecrates") val orangeCrates: Int,
    @ColumnInfo(name = "bluecrates") val blueCrates: Int,
    @ColumnInfo(name = "empanadacrates") val empanadaCrates: Int,
    @ColumnInfo(name = "box") val box: Int,
    @ColumnInfo(name = "sent") val sent: Boolean
)
