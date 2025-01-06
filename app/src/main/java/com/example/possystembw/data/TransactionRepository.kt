package com.example.possystembw.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.constraintlayout.helper.widget.MotionEffect.TAG

import com.example.possystembw.DAO.TransactionApi
import com.example.possystembw.DAO.TransactionDao
import com.example.possystembw.DAO.TransactionRecordRequest
import com.example.possystembw.DAO.TransactionSummaryRequest
import com.example.possystembw.DAO.TransactionSyncRequest
import com.example.possystembw.DAO.TransactionSyncResponse
import com.example.possystembw.DAO.ZReportUpdateResponse
import com.example.possystembw.RetrofitClient
import com.example.possystembw.database.TransactionRecord
import com.example.possystembw.database.TransactionSummary
import com.example.possystembw.ui.SessionManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

class TransactionRepository(
    val transactionDao: TransactionDao,
//    private val numberSequenceRepository: NumberSequenceRepository,
    private val numberSequenceRemoteRepository: NumberSequenceRemoteRepository

) {
    val api: TransactionApi = RetrofitClient.transactionApi
    private val TAG = "TransactionRepository"

//
//    suspend fun getTransactions(): List<TransactionSummary> {
//        return transactionDao.getAllTransactionSummaries()
//    }
    suspend fun getTransactions(): List<TransactionSummary> {
        return transactionDao.getAllTransactionSummaries().sortedByDescending { it.transactionId }
    }

    suspend fun getTransactionItems(transactionId: String): List<TransactionRecord> {
        return transactionDao.getTransactionRecordsByTransactionId(transactionId)
    }
//    suspend fun getTransactionItems(transactionId: String): List<TransactionRecord> {
//        return transactionDao.getTransactionItems(transactionId)
//    }
    suspend fun updateTransactionSummary(transaction: TransactionSummary) {
        transactionDao.updateTransactionSummary(transaction)
    }

    suspend fun getTransactionSummary(transactionId: String): TransactionSummary {
        return transactionDao.getTransactionSummary(transactionId)
            ?: throw Exception("Transaction summary not found for ID: $transactionId")
    }

    suspend fun getTransactionRecords(transactionId: String): List<TransactionRecord> {
        return transactionDao.getTransactionRecordsByTransactionId(transactionId)
    }

    suspend fun updateTransactionRecords(records: List<TransactionRecord>) {
        transactionDao.updateTransactionRecords(records)
    }

    suspend fun insertTransactionRecords(records: List<TransactionRecord>) {
        transactionDao.insertAll(records)
    }
    suspend fun insertTransactionSummary(transaction: TransactionSummary) {
        transactionDao.insertTransactionSummary(transaction)
    }

    suspend fun updateRefundReceiptId(transactionId: String, refundReceiptId: String) {
        transactionDao.updateRefundReceiptId(transactionId, refundReceiptId)
    }

    suspend fun generateTransactionId(storeId: String): String {
        return numberSequenceRemoteRepository.getNextTransactionNumber(storeId)
    }
    // Add this function to TransactionRepository class
    private fun validateTransaction(summary: TransactionSummary, records: List<TransactionRecord>): Boolean {
        if (summary.transactionId.isEmpty() || summary.receiptId.isEmpty()) {
            Log.e("VALIDATION", "Invalid summary IDs - transactionId: ${summary.transactionId}, receiptId: ${summary.receiptId}")
            return false
        }

        if (records.isEmpty()) {
            Log.e("VALIDATION", "No records to sync for transaction ${summary.transactionId}")
            return false
        }

        for (record in records) {
            if (record.transactionId.isEmpty()) {
                Log.e("VALIDATION", "Record missing transactionId at line ${record.lineNum}")
                return false
            }
            if (record.itemId.isNullOrEmpty()) {
                Log.e("VALIDATION", "Record missing itemId at line ${record.lineNum}")
                return false
            }
        }

        return true
    }


    // Add to TransactionViewModel for periodic sync
    fun startPeriodicSync(scope: CoroutineScope) {
        scope.launch {
            while (true) {
                try {
                    syncUnsentTransactions()
                    // Wait for 5 minutes before next sync attempt
                    delay(5 * 60 * 1000)
                } catch (e: Exception) {
                    Log.e("PeriodicSync", "Error in periodic sync", e)
                    delay(60 * 1000) // Wait 1 minute before retrying on error
                }
            }
        }
    }



    fun generateStoreKey(storeId: String, transactionId: String): String {
        return "${storeId}_${transactionId}_${System.currentTimeMillis()}"
    }


fun createTransactionRecordRequest(record: TransactionRecord, summary: TransactionSummary): TransactionRecordRequest {
    return TransactionRecordRequest(
        transactionid = record.transactionId,
        linenum = record.lineNum?.toString() ?: "0",
        receiptid = record.receiptId ?: "",
        storeKey = record.storeKey,
        storeSequence = record.storeSequence,
        itemid = record.itemId ?: "",
        itemname = record.name,
        itemgroup = record.itemGroup ?: "",
        price = formatDecimal(record.price),
        netprice = formatDecimal(record.netPrice),
        qty = record.quantity.toString(),
        discamount = formatDecimal(record.discountAmount),
        costamount = formatDecimal(record.costAmount),
        netamount = formatDecimal(record.netAmount),
        grossamount = formatDecimal(record.grossAmount),
        custaccount = summary.customerAccount,
        store = summary.store,
        priceoverride = record.priceOverride?.toInt() ?: 0,
        paymentmethod = summary.paymentMethod,
        staff = record.staff ?: "Unknown Staff",
        linedscamount = formatDecimal(record.lineDiscountAmount ?: 0.0),
        linediscpct = formatDecimal(record.lineDiscountPercentage ?: 0.0),
        custdiscamount = formatDecimal(record.customerDiscountAmount ?: 0.0),
        unit = record.unit ?: "PCS",
        unitqty = formatDecimal(record.unitQuantity ?: record.quantity.toDouble()),
        unitprice = formatDecimal(record.unitPrice ?: record.price),
        taxamount = formatDecimal(record.taxAmount),
        createddate = formatDate(record.createdDate ?: Date()).toString(),
        remarks = record.remarks ?: "",
        taxinclinprice = formatDecimal(record.taxIncludedInPrice),
        description = record.description ?: "",
        discofferid = record.discountOfferId?.takeIf { it.isNotBlank() } ?: "",
        inventbatchid = null,
        inventbatchexpdate = null,
        giftcard = null,
        returntransactionid = null,
        returnqty = null,
        creditmemonumber = null,
        returnlineid = null,
        priceunit = null,
        netamountnotincltax = formatDecimal(record.netAmountNotIncludingTax),
        storetaxgroup = null,
        currency = "PHP",
        taxexempt = null
    )
}

    fun createTransactionSummaryRequest(summary: TransactionSummary): TransactionSummaryRequest {
        return TransactionSummaryRequest(
            transactionid = summary.transactionId,
            type = summary.type,
            receiptid = summary.receiptId,
            storeKey = summary.storeKey,
            storeSequence = summary.storeSequence,
            store = summary.store,
            staff = summary.staff,
            custaccount = summary.customerAccount,
            netamount = formatDecimal(summary.netAmount),
            costamount = formatDecimal(summary.costAmount),
            grossamount = formatDecimal(summary.grossAmount),
            partialpayment = formatDecimal(summary.partialPayment),
            transactionstatus = summary.transactionStatus,
            discamount = formatDecimal(summary.discountAmount),
            cashamount = formatDecimal(summary.totalAmountPaid),
            custdiscamount = formatDecimal(summary.customerDiscountAmount),
            totaldiscamount = formatDecimal(summary.totalDiscountAmount),
            numberofitems = summary.numberOfItems.toString(),
            currency = summary.currency,
            createddate = formatDate(summary.createdDate).toString(),
            priceoverride = summary.priceOverride.toInt(),
            comment = summary.comment,
            taxinclinprice = formatDecimal(summary.taxIncludedInPrice),
            netamountnotincltax = formatDecimal(summary.vatableSales),
            window_number = summary.windowNumber,
            cash = formatDecimal(summary.cash),
            gcash = formatDecimal(summary.gCash),
            paymaya = formatDecimal(summary.payMaya),
            card = formatDecimal(summary.card),
            loyaltycard = formatDecimal(summary.loyaltyCard),
            charge = formatDecimal(summary.charge),
            foodpanda = formatDecimal(summary.foodpanda),
            grabfood = formatDecimal(summary.grabfood),
            representation = formatDecimal(summary.representation)
        )
    }

    suspend fun syncUnsentTransactions() {
        try {
            // Only get transactions where syncStatus is 0
            val unsyncedSummaries = transactionDao.getUnsyncedTransactionSummaries()
            Log.d(TAG, "Found ${unsyncedSummaries.size} unsynced transactions")

            for (summary in unsyncedSummaries) {
                try {
                    // Check if the summary is already synced (double-check)
                    val currentSummary = transactionDao.getTransactionSummary(summary.transactionId)
                    if (currentSummary?.syncStatus == true) {
                        Log.d(TAG, "Skipping already synced transaction ${summary.transactionId}")
                        continue
                    }

                    // Only get unsynced records for this transaction
                    val unsyncedRecords = transactionDao.getUnsyncedTransactionRecords(summary.transactionId)
                    if (unsyncedRecords.isEmpty()) {
                        Log.d(TAG, "No unsynced records found for transaction ${summary.transactionId}")
                        continue
                    }

                    Log.d(TAG, "Attempting to sync transaction ${summary.transactionId} with ${unsyncedRecords.size} records")

                    val request = TransactionSyncRequest(
                        transactionSummary = createTransactionSummaryRequest(summary),
                        transactionRecords = unsyncedRecords.map { record ->
                            createTransactionRecordRequest(record, summary)
                        }
                    )

                    val result = api.syncTransaction(request)

                    if (result.isSuccessful && result.body() != null) {
                        // Mark both summary and records as synced
                        transactionDao.markTransactionSummaryAsSynced(summary.transactionId)
                        transactionDao.markTransactionRecordsAsSynced(summary.transactionId)
                        Log.d(TAG, "Successfully synced transaction ${summary.transactionId}")
                    } else {
                        Log.e(TAG, "Failed to sync transaction ${summary.transactionId}: ${result.message()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing transaction ${summary.transactionId}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in sync process", e)
        }
    }

//    suspend fun syncTransaction(
//        summary: TransactionSummary,
//        records: List<TransactionRecord>
//    ): Result<TransactionSyncResponse> {
//        return withContext(Dispatchers.IO) {
//            try {
//                // Check if transaction is already synced
//                if (summary.syncStatus) {
//                    Log.d(TAG, "Transaction ${summary.transactionId} is already synced, skipping")
//                    return@withContext Result.success(
//                        TransactionSyncResponse(
//                            message = "Transaction already synced",
//                            storeKey = summary.storeKey,
//                            storeSequence = summary.storeSequence,
//                            summaryResponse = Any(),
//                            recordsResponse = Any()
//                        )
//                    )
//                }
//
//                // Filter out any already synced records
//                val unsyncedRecords = records.filter { !it.syncStatusRecord }
//                if (unsyncedRecords.isEmpty()) {
//                    Log.d(TAG, "No unsynced records for transaction ${summary.transactionId}")
//                    return@withContext Result.success(
//                        TransactionSyncResponse(
//                            message = "No unsynced records",
//                            storeKey = summary.storeKey,
//                            storeSequence = summary.storeSequence,
//                            summaryResponse = Any(),
//                            recordsResponse = Any()
//                        )
//                    )
//                }
//
//                val summaryRequest = createTransactionSummaryRequest(summary)
//                val recordRequests = unsyncedRecords.map { record ->
//                    createTransactionRecordRequest(record, summary)
//                }
//
//                val request = TransactionSyncRequest(
//                    transactionSummary = summaryRequest,
//                    transactionRecords = recordRequests
//                )
//
//                val response = api.syncTransaction(request)
//
//                if (response.isSuccessful) {
//                    response.body()?.let { syncResponse ->
//                        // Mark as synced only on successful response
//                        transactionDao.markTransactionSummaryAsSynced(summary.transactionId)
//                        transactionDao.markTransactionRecordsAsSynced(summary.transactionId)
//                        Result.success(syncResponse)
//                    } ?: Result.failure(Exception("Empty response body"))
//                } else {
//                    Result.failure(Exception("Sync failed: ${response.code()}"))
//                }
//            } catch (e: Exception) {
//                Result.failure(e)
//            }
//        }
//    }
suspend fun syncTransaction(
    summary: TransactionSummary,
    records: List<TransactionRecord>
): Result<TransactionSyncResponse> {
    return withContext(Dispatchers.IO) {
        try {
            // Validate data before attempting to sync
            if (!validateTransaction(summary, records)) {
                return@withContext Result.failure(Exception("Validation failed for transaction ${summary.transactionId}"))
            }

            // Check if transaction is already synced
            if (summary.syncStatus) {
                Log.d(TAG, "Transaction ${summary.transactionId} is already synced, skipping")
                return@withContext Result.success(
                    TransactionSyncResponse(
                        message = "Transaction already synced",
                        storeKey = summary.storeKey,
                        storeSequence = summary.storeSequence,
                        summaryResponse = Any(),
                        recordsResponse = Any()
                    )
                )
            }

            // Filter out any already synced records
            val unsyncedRecords = records.filter { !it.syncStatusRecord }
            if (unsyncedRecords.isEmpty()) {
                Log.d(TAG, "No unsynced records for transaction ${summary.transactionId}")
                return@withContext Result.success(
                    TransactionSyncResponse(
                        message = "No unsynced records",
                        storeKey = summary.storeKey,
                        storeSequence = summary.storeSequence,
                        summaryResponse = Any(),
                        recordsResponse = Any()
                    )
                )
            }

            val summaryRequest = createTransactionSummaryRequest(summary)
            val recordRequests = unsyncedRecords.map { record ->
                createTransactionRecordRequest(record, summary)
            }

            val request = TransactionSyncRequest(
                transactionSummary = summaryRequest,
                transactionRecords = recordRequests
            )

            // Log the request for debugging
            val gson = GsonBuilder().setPrettyPrinting().create()
            val jsonRequest = gson.toJson(request)
            Log.d("API_DEBUG", "Syncing transaction ${summary.transactionId} - Request JSON: $jsonRequest")

            val response = api.syncTransaction(request)

            if (response.isSuccessful) {
                response.body()?.let { syncResponse ->
                    // Mark as synced only on successful response
                    transactionDao.markTransactionSummaryAsSynced(summary.transactionId)
                    transactionDao.markTransactionRecordsAsSynced(summary.transactionId)
                    Result.success(syncResponse)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("API_ERROR", "Error code: ${response.code()}, Body: $errorBody")
                Result.failure(Exception("Sync failed: ${response.code()}, Error: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("API_ERROR", "Exception during sync: ${e.message}", e)
            Result.failure(e)
        }
    }
}
    suspend fun getTransactionsByStore(storeId: String): List<TransactionSummary> {
        return transactionDao.getTransactionsByStore(storeId)
    }
    suspend fun getTransactionsByDateRange(startDate: Date, endDate: Date): List<TransactionSummary> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate)
    }

    // Updated helper functions with null safety and proper formatting
    private fun formatDecimal(value: Double?): String {
        return try {
            String.format(Locale.US, "%.2f", value ?: 0.0)
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting decimal value: $value", e)
            "0.00"
        }
    }

    private fun formatDecimalOrNull(value: Double?): String? {
        return if (value == null || value == 0.0) {
            null
        } else {
            formatDecimal(value)
        }
    }

    private fun formatQuantity(value: Int?): String {
        return (value ?: 0).toString()
    }

    suspend fun getUnsyncedTransactions(): List<TransactionSummary> {
        return transactionDao.getUnsyncedTransactions()
    }

    suspend fun syncAllPendingTransactions(context: Context) {
        if (!isWifiConnected(context)) {
            return
        }

        val unsynced = getUnsyncedTransactions()
        unsynced.forEach { summary ->
            try {
                val records = getTransactionRecords(summary.transactionId)
                val result = syncTransaction(summary, records)

                result.onSuccess {
                    transactionDao.updateSyncStatus(summary.transactionId, true)
                }
            } catch (e: Exception) {
                Log.e("TransactionRepository", "Failed to sync ${summary.transactionId}", e)
            }
        }
    }

    private fun isWifiConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.type == ConnectivityManager.TYPE_WIFI
        }
    }
    suspend fun updateTransactionsZReport(storeId: String, zReportId: String): Result<ZReportUpdateResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.updateTransactionsZReport(storeId, zReportId)
                if (response.isSuccessful) {
                    response.body()?.let { result ->
                        // Update local database regardless of network status
                        try {
                            transactionDao.updateTransactionsZReportId(storeId, zReportId)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating local database", e)
                        }
                        Result.success(result)
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    // If server update fails, still update local database
                    try {
                        transactionDao.updateTransactionsZReportId(storeId, zReportId)
                        Result.success(ZReportUpdateResponse(
                            success = true,
                            message = "Updated locally only",
                            data = null
                        ))
                    } catch (e: Exception) {
                        Result.failure(Exception("Failed to update locally: ${e.message}"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in updateTransactionsZReport", e)
                // On network error, try to update locally
                try {
                    transactionDao.updateTransactionsZReportId(storeId, zReportId)
                    Result.success(ZReportUpdateResponse(
                        success = true,
                        message = "Updated locally due to network error",
                        data = null
                    ))
                } catch (dbError: Exception) {
                    Result.failure(Exception("Failed to update: ${e.message}"))
                }
            }
        }
    }

    private fun formatDate(date: Date?): String {
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Manila")
            }.format(date ?: Date())
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date: $date", e)
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Manila")
            }.format(Date())
        }
    }
}

