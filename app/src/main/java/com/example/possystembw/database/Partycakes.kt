package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "partycakes")
data class Partycakes(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "COSNO") val cosNo: String,
    @ColumnInfo(name = "BRANCH") val branch: String,
    @ColumnInfo(name = "DATEORDER") val dateOrder: Long,
    @ColumnInfo(name = "CUSTOMERNAME") val customerName: String,
    @ColumnInfo(name = "ADDRESS") val address: String,
    @ColumnInfo(name = "TELNO") val telNo: String,
    @ColumnInfo(name = "DATEPICKEDUP") val datePickedUp: Long?,
    @ColumnInfo(name = "TIMEPICKEDUP") val timePickedUp: String?,
    @ColumnInfo(name = "DELIVERED") val delivered: Boolean,
    @ColumnInfo(name = "TIMEDELIVERED") val timeDelivered: String?,
    @ColumnInfo(name = "DEDICATION") val dedication: String?,
    @ColumnInfo(name = "BDAYCODENO") val bdayCodeNo: String?,
    @ColumnInfo(name = "FLAVOR") val flavor: String?,
    @ColumnInfo(name = "MOTIF") val motif: String?,
    @ColumnInfo(name = "ICING") val icing: String?,
    @ColumnInfo(name = "OTHERS") val others: String?,
    @ColumnInfo(name = "SRP") val srp: Double,
    @ColumnInfo(name = "DISCOUNT") val discount: Double,
    @ColumnInfo(name = "PARTIALPAYMENT") val partialPayment: Double,
    @ColumnInfo(name = "NETAMOUNT") val netAmount: Double,
    @ColumnInfo(name = "BALANCEAMOUNT") val balanceAmount: Double,
    @ColumnInfo(name = "STATUS") val status: String,
    @ColumnInfo(name = "file_path") val filePath: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "TRANSACTSTORE") val transactStore: String
)
