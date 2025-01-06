package com.example.possystembw.DAO

import com.example.possystembw.database.TransactionRecord
import com.example.possystembw.database.TransactionSummary
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import java.math.BigInteger

interface TransactionApi {
    @POST("api/sync-transactions")
    suspend fun syncTransaction(
        @Body request: TransactionSyncRequest
    ): Response<TransactionSyncResponse>  // Added generic type

    @GET("api/getsummary/{storeId}")
    suspend fun getTransactionSummaries(@Path("storeId") storeId: String): Response<List<TransactionSummary>>

    @GET("api/getdetails/{storeId}")
    suspend fun getTransactionDetails(@Path("storeId") storeId: String): Response<List<TransactionRecord>>


        @POST("api/getdata/{storeId}/{getsummary}/{getdetails}")
        suspend fun checkTransactionMatch(
            @Path("storeId") storeId: String,
            @Path("getsummary") getsummary: String,
            @Path("getdetails") getdetails: String,
            @Body requestBody: RequestBody = RequestBody.create(
                "application/json".toMediaTypeOrNull(),
                "{}"
            )
        ): Response<TransactionMatchResponse>


    @POST("api/rbotransactiontables/{storeId}/{zReportId}")
    suspend fun updateTransactionsZReport(
        @Path("storeId") storeId: String,
        @Path("zReportId") zReportId: String
    ): Response<ZReportUpdateResponse>
}


data class ZReportUpdateResponse(
    val success: Boolean = false,
    val message: String = "",
    val data: ZReportData? = null
)

data class ZReportData(
    val updatedTransactions: Int = 0,
    val storeId: String = "",
    val zReportId: String = ""
)
data class TransactionMatchResponse(
    val message: String
)

data class TransactionSyncResponse(
    val message: String,
    val storeKey: String,
    val storeSequence: String,
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
    val storeKey: String,
    val storeSequence: String,
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
    val refundreceiptid: String? = null,
    val refunddate: String? = null,
    val returnedby: String? = null,
    val currency: String = "PHP",
    val zreportid: String? = null,
    val createddate: String,
    val priceoverride: Int? = null,
    val comment: String? = null,
    val receiptemail: String? = null,
    val markupamount: String? = null,
    val markupdescription: String? = null,
    val taxinclinprice: String,
    val netamountnotincltax: String? = null,
    val window_number: Int,
    val charge: String? = null,
    val gcash: String? = null,
    val paymaya: String? = null,
    val cash: String? = null,
    val card: String? = null,
    val loyaltycard: String? = null,
    val foodpanda: String? = null,
    val grabfood: String? = null,
    val representation: String? = null

)

// Match the server-side expected format for transaction records
data class TransactionRecordRequest(
    val transactionid: String,
    val linenum: String,
    val receiptid: String,
    val storeKey: String,    // Added field
    val storeSequence: String,
    val itemid: String,
    val itemname: String,
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
    val taxinclinprice: String,
    val description: String,
    // Additional fields to match API response
    val discofferid: String = "",
    val inventbatchid: String?,
    val inventbatchexpdate: String?,
    val giftcard: String?,
    val returntransactionid: String?,
    val returnqty: String?,
    val creditmemonumber: String?,
    val returnlineid: String?,
    val priceunit: String?,
    val netamountnotincltax: String?,
    val storetaxgroup: String?,
    val currency: String?,
    val taxexempt: String?
)
fun Number.toLineNumString(): String {
    return when (this) {
        is Long -> this.toString()
        is Int -> this.toString()
        is Short -> this.toString()
        is Byte -> this.toString()
        is BigInteger -> this.toString()
        else -> this.toString()
    }
}