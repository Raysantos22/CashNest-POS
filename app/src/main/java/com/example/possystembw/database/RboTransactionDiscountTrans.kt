package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rbotransactiondiscounttrans")
data class RboTransactionDiscountTrans(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Auto-generated ID (unique to each record)
    @ColumnInfo(name = "TRANSACTIONID") val transactionId: String,
    @ColumnInfo(name = "LINENUM") val lineNum: Double,
    @ColumnInfo(name = "DISCLINENUM") val discLineNum: Double,
    @ColumnInfo(name = "STORE") val store: String,
    @ColumnInfo(name = "DISCOUNTTYPE") val discountType: Int?,
    @ColumnInfo(name = "DISCOUNTPCT") val discountPct: Double?,
    @ColumnInfo(name = "DISCOUNTAMT") val discountAmt: Double?,
    @ColumnInfo(name = "DISCOUNTAMTWITHTAX") val discountAmtWithTax: Double?,
    @ColumnInfo(name = "PERIODICDISCTYPE") val periodicDiscType: Int?,
    @ColumnInfo(name = "DISCOFFERID") val discOfferId: String?,
    @ColumnInfo(name = "DISCOFFERNAME") val discOfferName: String?,
    @ColumnInfo(name = "QTYDISCOUNTED") val qtyDiscounted: Int?,
    @ColumnInfo(name = "created_at") val createdAt: String?, // Consider using `Date` or `LocalDate` here
    @ColumnInfo(name = "updated_at") val updatedAt: String? // Consider using `Date` or `LocalDate` here
)
