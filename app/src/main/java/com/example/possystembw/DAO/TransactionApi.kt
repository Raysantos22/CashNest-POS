package com.example.possystembw.DAO

import com.example.possystembw.database.TransactionRecord
import com.example.possystembw.database.TransactionSummary
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface TransactionApi {
    @POST("api/sync-transactions")
    suspend fun syncTransaction(
        @Body request: TransactionSyncRequest
    ): Response<TransactionSyncResponse>

    @POST("api/rbotransactiontables")
    suspend fun syncTransactionSummary(@Body summary: TransactionSummaryRequest): Response<Any>

    @POST("api/rbotransactionsalestrans")
    suspend fun syncTransactionRecords(@Body records: TransactionRecordRequest): Response<Any>
}

data class TransactionSyncResponse(
    val message: String,
    val transactionId: String,
    val summaryResponse: Any,
    val recordsResponse: Any
)
data class TransactionSyncRequest(
    @SerializedName("transactionSummary")
    val transactionSummary: TransactionSummaryRequest,
    @SerializedName("transactionRecords")
    val transactionRecords: List<TransactionRecordRequest>
)


// Match the server-side expected format for transaction summary
data class TransactionSummaryRequest(
    val transactionid: String,
    val type: Int,
    val receiptid: String,
    val store: String,
    val staff: String,
    val custaccount: String,
    val netamount: String,
    val costamount: String,
    val grossamount: String,
    val partialpayment: String,
    val transactionstatus: Int,
    val discamount: String,
    val cashamount: String,
    val custdiscamount: String,
    val totaldiscamount: String,
    val numberofitems: String,
    val currency: String = "PHP",
    val createddate: String,
    val priceoverride: Int,
    val comment: String,
    val taxinclinprice: String,
    val window_number: Int,
    val gcash: String = "0.00",
    val paymaya: String = "0.00",
    val cash: String = "0.00",
    val card: String = "0.00",
    val loyaltycard: String = "0.00",
    val charge: String = "0.00",
    val foodpanda: String = "0.00",
    val grabfood: String = "0.00"
)

// Match the server-side expected format for transaction records
data class TransactionRecordRequest(
    val transactionid: String,
    val linenum: Int,
    val receiptid: String,
    val itemid: String,
    val itemname: String,        // Added required itemname field
    val itemgroup: String,
    val price: String,
    val netprice: String,
    val qty: String,
    val discamount: String,
    val costamount: String,
    val netamount: String,
    val grossamount: String,
    val custaccount: String,
    val store: String,
    val priceoverride: Int,
    val paymentmethod: String,
    val staff: String,
    val linedscamount: String,
    val linediscpct: String,
    val custdiscamount: String,
    val unit: String,
    val unitqty: String,
    val unitprice: String,
    val taxamount: String,
    val createddate: String,
    val remarks: String,
    val taxinclinprice: Int,
    val description: String
)
