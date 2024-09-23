package com.example.possystembw.database
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "product_id") val productId: Int,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "quantity") var quantity: Int,
    @ColumnInfo(name = "price") val price: Double,
    @ColumnInfo(name = "window_id") val windowId: Int,
    @ColumnInfo(name = "discount") val discount: Double = 0.0,
    @ColumnInfo(name = "discountType") val discountType: String = "",
    @ColumnInfo(name = "overriddenPrice") val overriddenPrice: Double? = null,
    @ColumnInfo(name = "partialPayment") var partialPayment: Double = 0.0,
    @ColumnInfo(name = "comment") var comment: String? = null,
    @ColumnInfo(name = "cart_comment") var cartComment: String? = null,
    @ColumnInfo(name = "amountPaid") var amountPaid: Double = 0.0,
    @ColumnInfo(name = "transactionId")var transactionId: String? = null,
    @ColumnInfo(name = "receiptNumber")var receiptNumber: String? = null,
    @ColumnInfo(name = "originalTransactionId")var originalTransactionId: String? = null,
    @ColumnInfo(name = "originalReceiptNumber")var originalReceiptNumber: String? = null,
    @ColumnInfo(name = "isVatExempt")var isVatExempt: Boolean = false,
    @ColumnInfo(name = "store") var store: String? = null,
    @ColumnInfo(name = "staff") var staff: String? = null,
    @ColumnInfo(name = "customerName") var customerName: String? = null,
    @ColumnInfo(name = "customerAccName") var customerAccName: String? = null,
    @ColumnInfo(name = "netAmount") var netAmount: Double = 0.0,
    @ColumnInfo(name = "grossAmount") var grossAmount: Double = 0.0,
    @ColumnInfo(name = "discountAmount") var discountAmount: Double = 0.0,
    @ColumnInfo(name = "numberOfItems") var numberOfItems: Double = 0.0,
    @ColumnInfo(name = "createdDate") var createdDate: Date? = null,
    @ColumnInfo(name = "taxIncludedInPrice") var taxIncludedInPrice: Double = 0.0,
    @ColumnInfo(name = "gCash") var gCash: Double = 0.0,
    @ColumnInfo(name = "cash") var cash: Double = 0.0,
    @ColumnInfo(name = "card") var card: Double = 0.0,
    @ColumnInfo(name = "totalAmountPaid") var totalAmountPaid: Double = 0.0,
    @ColumnInfo(name = "paymentMethod") var paymentMethod: String? = null,
    @ColumnInfo(name = "vatAmount") val vatAmount: Double,  // Added parameter
    @ColumnInfo(name = "vatExemptAmount") val vatExemptAmount: Double,
    @ColumnInfo(name = "bundle_id") val bundleId: Int? = null,
    @ColumnInfo(name = "bundle_selections") val bundleSelections: String? = null,
    @ColumnInfo(name = "bundleName")val mixMatchId: String = "",
    @ColumnInfo(name = "itemGroup")val itemGroup: String,  // Added field
    @ColumnInfo(name = "itemId")val itemId: String

)
