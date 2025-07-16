package com.example.possystembw.database

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "line_transactions",
    primaryKeys = ["journalId", "itemId"],
    indices = [
        Index(value = ["journalId"]),
        Index(value = ["itemId"]),
        Index(value = ["barcode"]),
        Index(value = ["syncStatus"])  // Add index for faster queries
    ]
)
data class LineTransactionEntity(
    val journalId: String,
    val lineNum: Int?,
    val transDate: String,
    val itemId: String,
    val itemDepartment: String,
    val storeName: String,
    val adjustment: String,
    val costPrice: String?,
    val priceUnit: String?,
    val salesAmount: String?,
    val inventOnHand: String?,
    val counted: String,
    val reasonRefRecId: String?,
    val variantId: String?,
    val posted: Int?,
    val postedDateTime: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val wasteCount: String,
    val receivedCount: String,
    val wasteType: String?,
    val transferCount: String?,
    val wasteDate: String?,
    val itemGroupId: String,
    val itemName: String,
    val itemType: Int,
    val nameAlias: String,
    val notes: String,
    val itemGroup: String,
    val itemDepartmentLower: String,
    val zeroPriceValid: Int,
    val dateBlocked: String,
    val dateToBeBlocked: String,
    val blockedOnPos: Int,
    val activeOnDelivery: Int,
    val barcode: String,
    val dateToActivateItem: String?,
    val mustSelectUom: Int,
    val production: String?,
    val moq: Int,
    val fgCount: String?,
    val transparentStocks: String?,
    val stocks: String?,
    val postedLower: Int,
    val syncStatus: Int = 1  // 1 = synced, 0 = needs sync

)

fun LineTransactionEntity.hasAnyValue(): Boolean {
    return (adjustment.toDoubleOrNull() ?: 0.0) > 0 ||
            (receivedCount.toDoubleOrNull() ?: 0.0) > 0 ||
            (transferCount?.toDoubleOrNull() ?: 0.0) > 0 ||
            (wasteCount.toDoubleOrNull() ?: 0.0) > 0 ||
            (counted.toDoubleOrNull() ?: 0.0) > 0 ||
            (!wasteType.isNullOrBlank() && wasteType != "none")
}