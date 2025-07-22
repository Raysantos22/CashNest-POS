package com.example.possystembw.DAO

import android.util.Log
import com.example.possystembw.DAO.DateUtils.getCurrentDateString
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

fun TransactionSummaryResponse.getFormattedDate(): String {
    return createddate?.let { dateString ->
        try {
            // Parse the API format
            val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US)
            apiFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = apiFormat.parse(dateString)

            // Convert to your simple format
            val simpleFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            simpleFormat.timeZone = TimeZone.getTimeZone("Asia/Manila")
            simpleFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString.convertApiDateToSimple() // Fallback to extension function
        }
    } ?: getCurrentDateString()
}

// KEEP your existing convertApiDateToSimple extension - it's perfect
fun String.convertApiDateToSimple(): String {
    return try {
        // Parse the API format: 2025-07-01T01:22:35.000000Z
        val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US)
        apiFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = apiFormat.parse(this)

        // Convert to simple format
        val simpleFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        simpleFormat.timeZone = TimeZone.getTimeZone("Asia/Manila")
        simpleFormat.format(date ?: Date())
    } catch (e: Exception) {
        Log.w("DateConverter", "Could not convert date: $this")
        this // Return original if conversion fails
    }
}

// UPDATE your normalizeApiDate function to use the extension:
fun normalizeApiDate(dateString: String): String {
    if (dateString.isEmpty()) return getCurrentDateString()

    try {
        // Use the extension function to convert API date to simple format
        return dateString.convertApiDateToSimple()
    } catch (e: Exception) {
        Log.e("DateNormalization", "Error normalizing date '$dateString': ${e.message}")
        return getCurrentDateString()
    }
}
fun String?.toDateObject(): Date {
    if (this.isNullOrEmpty()) return Date()

    val formats = listOf(
        "yyyy-MM-dd HH:mm:ss",           // API format: 2025-07-16 19:01:33
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", // ISO with microseconds
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",    // ISO with milliseconds
        "yyyy-MM-dd'T'HH:mm:ss'Z'",        // ISO basic
        "yyyy-MM-dd'T'HH:mm:ss",           // ISO without Z
        "yyyy-MM-dd",                      // Date only
        "MM/dd/yyyy HH:mm:ss",
        "dd/MM/yyyy HH:mm:ss"
    )

    for (format in formats) {
        try {
            val sdf = SimpleDateFormat(format, Locale.US)

            // FIXED: Set timezone based on format
            if (format.contains("'Z'")) {
                sdf.timeZone = TimeZone.getTimeZone("UTC")
            } else {
                // For API format like "2025-07-16 19:01:33", treat as Philippine time
                sdf.timeZone = TimeZone.getTimeZone("Asia/Manila")
            }

            val parsedDate = sdf.parse(this)
            if (parsedDate != null) {
                Log.d("DateConversion", "Successfully parsed '$this' using format '$format' -> $parsedDate")
                return parsedDate
            }
        } catch (e: Exception) {
            Log.d("DateConversion", "Failed to parse '$this' with format '$format': ${e.message}")
        }
    }

    Log.e("DateConversion", "Failed to parse date: '$this', using current date")
    return Date()
}

fun String?.toTimestamp(): Long {
    if (this.isNullOrEmpty()) return System.currentTimeMillis()

    val formats = listOf(
        "yyyy-MM-dd HH:mm:ss",           // API format: 2025-07-16 19:01:33
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

            // FIXED: Set timezone based on format
            if (format.contains("'Z'")) {
                sdf.timeZone = TimeZone.getTimeZone("UTC")
            } else {
                // For API format, treat as Philippine time already
                sdf.timeZone = TimeZone.getTimeZone("Asia/Manila")
            }

            val date = sdf.parse(this)
            if (date != null) {
                Log.d("TimestampConversion", "Successfully converted '$this' to timestamp: ${date.time}")
                return date.time
            }
        } catch (e: Exception) {
            Log.d("TimestampConversion", "Failed to convert '$this' with format '$format': ${e.message}")
        }
    }

    Log.e("TimestampConversion", "Failed to convert timestamp: '$this', using current time")
    return System.currentTimeMillis()
}

// FIXED: Updated normalizeApiDate function
//fun normalizeApiDate(dateString: String): String {
//    if (dateString.isEmpty()) return getCurrentDateString()
//
//    try {
//        // Parse the date using our enhanced function
//        val date = dateString.toDateObject()
//
//        // Format it to our standard format in Philippine time
//        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
//        format.timeZone = TimeZone.getTimeZone("Asia/Manila")
//
//        val normalizedDate = format.format(date)
//        Log.d("DateNormalization", "Normalized '$dateString' -> '$normalizedDate'")
//
//        return normalizedDate
//    } catch (e: Exception) {
//        Log.e("DateNormalization", "Error normalizing date '$dateString': ${e.message}")
//        return getCurrentDateString()
//    }
//}

// FIXED: Updated formatDateToString function
fun formatDateToString(date: Date): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        format.timeZone = TimeZone.getTimeZone("Asia/Manila") // Always format in Philippine time
        format.format(date)
    } catch (e: Exception) {
        Log.e("DateFormatting", "Error formatting date: ${e.message}")
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
    }
}

// FIXED: Updated getCurrentDateString function
fun getCurrentDateString(): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        format.timeZone = TimeZone.getTimeZone("Asia/Manila") // Philippine time
        format.format(Date())
    } catch (e: Exception) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
    }
}

// ENHANCED: Date comparison for date ranges
fun isDateInRange(dateString: String, startDate: Date, endDate: Date): Boolean {
    return try {
        val transactionDate = dateString.toDateObject()

        // Create calendar instances for proper date comparison
        val transactionCal = Calendar.getInstance().apply { time = transactionDate }
        val startCal = Calendar.getInstance().apply { time = startDate }
        val endCal = Calendar.getInstance().apply { time = endDate }

        // Compare just the date part (ignore time)
        val transactionDateOnly = Calendar.getInstance().apply {
            set(transactionCal.get(Calendar.YEAR),
                transactionCal.get(Calendar.MONTH),
                transactionCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startDateOnly = Calendar.getInstance().apply {
            set(startCal.get(Calendar.YEAR),
                startCal.get(Calendar.MONTH),
                startCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endDateOnly = Calendar.getInstance().apply {
            set(endCal.get(Calendar.YEAR),
                endCal.get(Calendar.MONTH),
                endCal.get(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val isInRange = transactionDateOnly.time >= startDateOnly.time &&
                transactionDateOnly.time <= endDateOnly.time

        Log.d("DateRange", "Checking if '$dateString' is between ${formatDateToString(startDate)} and ${formatDateToString(endDate)}: $isInRange")

        return isInRange
    } catch (e: Exception) {
        Log.e("DateRange", "Error checking date range for '$dateString': ${e.message}")
        return false
    }
}

// FIXED: Update your TransactionDetailResponse.toTransactionRecord() function
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

        // FIXED: Handle timestamp properly - use createddate string if timestamp is null
        timestamp = if (timestamp != null && timestamp != 0L) {
            timestamp
        } else {
            createddate?.toTimestamp() ?: System.currentTimeMillis()
        },

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

        // FIXED: Use normalizeApiDate for consistent formatting
        createdDate = normalizeApiDate(createddate ?: ""),

        remarks = remarks,
        inventoryBatchId = inventbatchid,

        // FIXED: Use normalizeApiDate for expiry date too
        inventoryBatchExpiryDate = if (inventbatchexpdate.isNullOrEmpty()) "" else normalizeApiDate(inventbatchexpdate),

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
