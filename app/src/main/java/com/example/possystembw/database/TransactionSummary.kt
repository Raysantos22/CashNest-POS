package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "transaction_summary")
data class TransactionSummary(
    @PrimaryKey @ColumnInfo(name = "transaction_id") val transactionId: String,
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "receiptid") val receiptId: String,
    @ColumnInfo(name = "store") val store: String,
    @ColumnInfo(name = "staff") val staff: String,
    @ColumnInfo(name = "custaccount") val customerAccount: String,
    @ColumnInfo(name = "netamount") val netAmount: Double,
    @ColumnInfo(name = "costamount") val costAmount: Double,
    @ColumnInfo(name = "grossamount") val grossAmount: Double,
    @ColumnInfo(name = "partial_payment") val partialPayment: Double,
    @ColumnInfo(name = "transactionstatus") val transactionStatus: Int,
    @ColumnInfo(name = "discamount") val discountAmount: Double,
    @ColumnInfo(name = "custdiscamount") val customerDiscountAmount: Double,
    @ColumnInfo(name = "totaldiscamount") val totalDiscountAmount: Double,
    @ColumnInfo(name = "numberofitems") val numberOfItems: Double,
    @ColumnInfo(name = "refundreceiptid") val refundReceiptId: String?,
    @ColumnInfo(name = "currency") val currency: String,
    @ColumnInfo(name = "zreportid") var zReportId: String?,
    @ColumnInfo(name = "createddate") val createdDate: Date,
    @ColumnInfo(name = "priceoverride") val priceOverride: Double,
    @ColumnInfo(name = "comment") val comment: String,
    @ColumnInfo(name = "receiptemail") val receiptEmail: String?,
    @ColumnInfo(name = "markupamount") val markupAmount: Double,
    @ColumnInfo(name = "markupdescription") val markupDescription: String?,
    @ColumnInfo(name = "taxinclinprice") val taxIncludedInPrice: Double,
    @ColumnInfo(name = "window_number") val windowNumber: Int,
    @ColumnInfo(name = "gcash") val gCash: Double,
    @ColumnInfo(name = "paymaya") val payMaya: Double,
    @ColumnInfo(name = "cash") val cash: Double,
    @ColumnInfo(name = "card") val card: Double,
    @ColumnInfo(name = "loyaltycard") val loyaltyCard: Double,
    @ColumnInfo(name = "total_amount_paid") val totalAmountPaid: Double,
    @ColumnInfo(name = "change_given") val changeGiven: Double,
    @ColumnInfo(name = "paymentMethod")val paymentMethod: String,
    @ColumnInfo(name = "customerName") val customerName: String? = null,
    @ColumnInfo(name = "vatAmount") val vatAmount: Double,  // Added parameter
    @ColumnInfo(name = "vatExemptAmount") val vatExemptAmount: Double,  // Added parameter
    @ColumnInfo(name = "vatableSales") val vatableSales: Double,  // Added
    @ColumnInfo(name = "discountType") val discountType: String = "",
    @ColumnInfo(name = "syncStatus")var syncStatus: Boolean = false


// Or nullable String?
)