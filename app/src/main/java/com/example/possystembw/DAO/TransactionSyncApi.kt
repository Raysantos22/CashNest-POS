package com.example.possystembw.DAO

import android.util.Log
import com.example.possystembw.database.TransactionRecord
import com.example.possystembw.database.TransactionSummary
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

interface TransactionSyncApi {
    @GET("api/getsummary/{storeId}")
    suspend fun getTransactionSummaries(
        @Path("storeId") storeId: String
    ): Response<List<TransactionSummary>>  // Changed from JsonObject to List

    @GET("api/getdetails/{storeId}")
    suspend fun getTransactionDetails(
        @Path("storeId") storeId: String
    ): Response<List<TransactionRecord>>  // Changed from JsonObject to List

}



// Create response data classes to match the server response
data class TransactionSummaryResponse(
    val transaction_id: String?,
    val type: Int?,
    val receiptid: String?,
    val store: String?,
    val staff: String?,
    val custaccount: String?,
    val netamount: Double?,
    val costamount: Double?,
    val grossamount: Double?,
    val partial_payment: Double?,
    val transactionstatus: Int?,
    val discamount: Double?,
    val custdiscamount: Double?,
    val totaldiscamount: Double?,
    val numberofitems: Double?,
    val refundreceiptid: String?,
    val currency: String?,
    val zreportid: String?,
    val createddate: String?,
    val priceoverride: Double?,
    val comment: String?,
    val receiptemail: String?,
    val markupamount: Double?,
    val markupdescription: String?,
    val taxinclinprice: Double?,
    val window_number: Int?,
    val total_amount_paid: Double?,
    val change_given: Double?,
    val paymentMethod: String?,
    val customerName: String?,
    val vatAmount: Double?,
    val vatExemptAmount: Double?,
    val vatableSales: Double?,
    val discountType: String?,
    val gcash: Double?,
    val paymaya: Double?,
    val cash: Double?,
    val card: Double?,
    val loyaltycard: Double?,
    val charge: Double?,
    val foodpanda: Double?,
    val grabfood: Double?,
    val representation: Double?,
    val store_key: String?,
    val store_sequence: String?,
    val syncStatus: Boolean?
)

data class TransactionDetailResponse(
    val id: Int?,
    val transactionId: String?,
    val name: String?,
    val price: Double?,
    val quantity: Int?,
    val subtotal: Double?,
    val vat_rate: Double?,
    val vat_amount: Double?,
    val discount_rate: Double?,
    val discount_amount: Double?,
    val total: Double?,
    val receipt_number: String?,
    val timestamp: Long?,
    val paymentMethod: String?,
    val ar: Double?,
    val window_number: Int?,
    val partial_payment_amount: Double?,
    val comment: String?,
    val linenum: Int?,
    val receipted: String?,
    val itemid: String?,
    val itemgroup: String?,
    val netprice: Double?,
    val costamount: Double?,
    val netamount: Double?,
    val grossamount: Double?,
    val custaccount: String?,
    val store: String?,
    val priceoverride: Double?,
    val staff: String?,
    val discofferid: String?,
    val linedscamount: Double?,
    val linediscpct: Double?,
    val custdiscamount: Double?,
    val unit: String?,
    val unitqty: Double?,
    val unitprice: Double?,
    val taxamount: Double?,
    val createddate: String?,
    val remarks: String?,
    val inventbatchid: String?,
    val inventbatchexpdate: String?,
    val giftcard: String?,
    val returntransactionid: String?,
    val returnqty: Double?,
    val creditmemonumber: String?,
    val taxinclinprice: Double?,
    val description: String?,
    val returnlineid: Double?,
    val priceunit: Double?,
    val netamountnotincltax: Double?,
    val storetaxgroup: String?,
    val currency: String?,
    val taxexempt: Double?,
    val store_key: String?,
    val store_sequence: String?,
    val syncstatusrecord: Boolean?

)
fun TransactionSummaryResponse.toTransactionSummary(): TransactionSummary {
    return TransactionSummary(
        transactionId = transaction_id ?: "",
        type = type ?: 0,
        receiptId = receiptid ?: "",
        store = store ?: "",
        staff = staff ?: "",
        customerAccount = custaccount ?: "",
        netAmount = netamount ?: 0.0,
        costAmount = costamount ?: 0.0,
        grossAmount = grossamount ?: 0.0,
        partialPayment = partial_payment ?: 0.0,
        transactionStatus = transactionstatus ?: 0,
        discountAmount = discamount ?: 0.0,
        customerDiscountAmount = custdiscamount ?: 0.0,
        totalDiscountAmount = totaldiscamount ?: 0.0,
        numberOfItems = numberofitems ?: 0.0,
        refundReceiptId = refundreceiptid,
        currency = currency ?: "PHP",
        zReportId = zreportid,
        // FIXED: Use enhanced date conversion that handles multiple formats
        createdDate = normalizeApiDate(createddate ?: ""),
        priceOverride = priceoverride ?: 0.0,
        comment = comment ?: "",
        receiptEmail = receiptemail,
        markupAmount = markupamount ?: 0.0,
        markupDescription = markupdescription,
        taxIncludedInPrice = taxinclinprice ?: 0.0,
        windowNumber = window_number ?: 1,
        totalAmountPaid = total_amount_paid ?: 0.0,
        changeGiven = change_given ?: 0.0,
        paymentMethod = paymentMethod ?: "",
        customerName = customerName,
        vatAmount = vatAmount ?: 0.0,
        vatExemptAmount = vatExemptAmount ?: 0.0,
        vatableSales = vatableSales ?: 0.0,
        discountType = discountType ?: "",
        gCash = gcash ?: 0.0,
        payMaya = paymaya ?: 0.0,
        cash = cash ?: 0.0,
        card = card ?: 0.0,
        loyaltyCard = loyaltycard ?: 0.0,
        charge = charge ?: 0.0,
        foodpanda = foodpanda ?: 0.0,
        grabfood = grabfood ?: 0.0,
        representation = representation ?: 0.0,
        storeKey = store_key ?: "",
        storeSequence = store_sequence ?: "",
        syncStatus = syncStatus ?: false
    )
}

fun TransactionDetailResponse.toTransactionRecord(): TransactionRecord {
    return TransactionRecord(
        id = id ?: 0,
        transactionId = transactionId ?: "",
        name = name ?: "",
        price = price ?: 0.0,
        quantity = quantity ?: 0,
        subtotal = subtotal ?: 0.0,
        vatRate = vat_rate ?: 0.0,
        vatAmount = vat_amount ?: 0.0,
        discountRate = discount_rate ?: 0.0,
        discountAmount = discount_amount ?: 0.0,
        total = total ?: 0.0,
        receiptNumber = receipt_number ?: "",
        timestamp = timestamp ?: createddate?.toTimestamp() ?: System.currentTimeMillis(),
        paymentMethod = paymentMethod ?: "",
        ar = ar ?: 0.0,
        windowNumber = window_number ?: 1,
        partialPaymentAmount = partial_payment_amount ?: 0.0,
        comment = comment ?: "",
        lineNum = linenum,
        receiptId = receipted,
        itemId = itemid,
        itemGroup = itemgroup,
        netPrice = netprice,
        costAmount = costamount,
        netAmount = netamount,
        grossAmount = grossamount ?: 0.0,
        customerAccount = custaccount,
        store = store,
        priceOverride = priceoverride,
        staff = staff,
        discountOfferId = discofferid,
        lineDiscountAmount = linedscamount,
        lineDiscountPercentage = linediscpct,
        customerDiscountAmount = custdiscamount,
        unit = unit,
        unitQuantity = unitqty,
        unitPrice = unitprice,
        taxAmount = taxamount,
        // FIXED: Use enhanced date conversion
        createdDate = normalizeApiDate(createddate ?: ""),
        remarks = remarks,
        inventoryBatchId = inventbatchid,
        // FIXED: Use enhanced date conversion
        inventoryBatchExpiryDate = normalizeApiDate(inventbatchexpdate ?: ""),
        giftCard = giftcard,
        returnTransactionId = returntransactionid,
        returnQuantity = returnqty,
        creditMemoNumber = creditmemonumber,
        taxIncludedInPrice = taxinclinprice,
        description = description,
        returnLineId = returnlineid,
        priceUnit = priceunit,
        netAmountNotIncludingTax = netamountnotincltax,
        storeTaxGroup = storetaxgroup,
        currency = currency,
        taxExempt = taxexempt,
        storeKey = store_key ?: "",
        storeSequence = store_sequence ?: "",
        syncStatusRecord = syncstatusrecord ?: false
    )
}

// NEW: Function to normalize API dates to consistent format
fun normalizeApiDate(dateString: String): String {
    if (dateString.isEmpty()) return getCurrentDateString()

    // First try to parse the date in any format
    val date = dateString.toDateObject()

    // Then convert it to our standard format
    return formatDateToString(date)
}

// UPDATED: Enhanced date range comparison for reports
fun isDateInRange(dateString: String, startDate: Date, endDate: Date): Boolean {
    return try {
        val transactionDate = dateString.toDateObject()
        transactionDate.time >= startDate.time && transactionDate.time <= endDate.time
    } catch (e: Exception) {
        false
    }
}

// DEBUGGING: Add this function to help debug date issues
fun debugDateFormats(dateString: String) {
    Log.d("DateDebug", "Original date string: '$dateString'")

    val formats = listOf(
        "yyyy-MM-dd HH:mm:ss" to "API Format",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'" to "ISO Microseconds",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" to "ISO Milliseconds",
        "yyyy-MM-dd'T'HH:mm:ss'Z'" to "ISO Basic",
        "yyyy-MM-dd'T'HH:mm:ss" to "ISO No Z"
    )

    for ((format, name) in formats) {
        try {
            val sdf = SimpleDateFormat(format, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(dateString)
            if (date != null) {
                Log.d("DateDebug", "$name: SUCCESS -> ${formatDateToString(date)}")
                return
            }
        } catch (e: Exception) {
            Log.d("DateDebug", "$name: FAILED -> ${e.message}")
        }
    }

    Log.e("DateDebug", "ALL FORMATS FAILED for: '$dateString'")
}

// ENHANCED: Date range queries for reports
suspend fun getTransactionsInDateRange(
    transactionDao: TransactionDao,
    startDate: Date,
    endDate: Date
): List<TransactionSummary> {
    return try {
        // Get all transactions and filter by date
        val allTransactions = transactionDao.getAllTransactionSummaries()

        allTransactions.filter { transaction ->
            isDateInRange(transaction.createdDate, startDate, endDate)
        }.also { filteredTransactions ->
            Log.d("DateFilter", "Original count: ${allTransactions.size}")
            Log.d("DateFilter", "Filtered count: ${filteredTransactions.size}")
            Log.d("DateFilter", "Date range: ${formatDateToString(startDate)} to ${formatDateToString(endDate)}")
        }
    } catch (e: Exception) {
        Log.e("DateFilter", "Error filtering transactions by date range", e)
        emptyList()
    }
}
fun formatDateToString(date: Date): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        format.format(date)
    } catch (e: Exception) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
    }
}
fun getCurrentDateString(): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        format.format(Date())
    } catch (e: Exception) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
    }
}


fun String?.toDateObject(): Date {
    if (this.isNullOrEmpty()) return Date()

    val formats = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd",
        "MM/dd/yyyy HH:mm:ss",
        "dd/MM/yyyy HH:mm:ss"
    )

    for (format in formats) {
        try {
            val sdf = SimpleDateFormat(format, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC") // parse in UTC
            val parsedDate = sdf.parse(this)
            if (parsedDate != null) {
                // Convert to Philippine Time
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"))
                calendar.time = parsedDate
                return calendar.time
            }
        } catch (_: Exception) {
            // Try next format
        }
    }

    return Date()
}

fun String?.toTimestamp(): Long {
    if (this.isNullOrEmpty()) return System.currentTimeMillis()

    val formats = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd",
        "MM/dd/yyyy HH:mm:ss",
        "dd/MM/yyyy HH:mm:ss"
    )

    for (format in formats) {
        try {
            val sdf = SimpleDateFormat(format, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(this)
            if (date != null) {
                // Convert UTC to Philippine time (UTC+8)
                return date.time + TimeZone.getTimeZone("Asia/Manila").rawOffset
            }
        } catch (_: Exception) {
            // Try next format
        }
    }

    return System.currentTimeMillis()
}
