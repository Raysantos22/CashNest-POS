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
import com.example.possystembw.RetrofitClient
import com.example.possystembw.database.TransactionRecord
import com.example.possystembw.database.TransactionSummary
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TransactionRepository(val transactionDao: TransactionDao) {
    private val api: TransactionApi = RetrofitClient.transactionApi
    private val TAG = "TransactionRepository"


    suspend fun getTransactions(): List<TransactionSummary> {
        return transactionDao.getAllTransactionSummaries()
    }

    suspend fun getTransactionItems(transactionId: String): List<TransactionRecord> {
        return transactionDao.getTransactionRecordsByTransactionId(transactionId)
    }

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


    // Update the repository's syncTransaction method

    // Update the repository's syncTransaction method
    suspend fun syncTransaction(
        summary: TransactionSummary,
        records: List<TransactionRecord>
    ): Result<TransactionSyncResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting sync process for transaction: ${summary.transactionId}")
                Log.d(TAG, "Number of records to sync: ${records.size}")

                // Convert TransactionSummary to TransactionSummaryRequest
                val summaryRequest = TransactionSummaryRequest(
                    transactionid = summary.transactionId,
                    type = summary.type,
                    receiptid = summary.receiptId,
                    store = summary.store,
                    staff = summary.staff.ifBlank { "Unknown Staff" },
                    custaccount = summary.customerAccount.ifBlank { "000000" },
                    netamount = formatDecimal(summary.netAmount),
                    costamount = formatDecimal(summary.costAmount),
                    grossamount = formatDecimal(summary.grossAmount),
                    partialpayment = formatDecimal(summary.partialPayment),
                    transactionstatus = summary.transactionStatus,
                    discamount = formatDecimal(summary.discountAmount),
                    cashamount = formatDecimal(summary.totalAmountPaid),
                    custdiscamount = formatDecimal(summary.customerDiscountAmount),
                    totaldiscamount = formatDecimal(summary.totalDiscountAmount),
                    numberofitems = formatQuantity(summary.numberOfItems.toInt()),
                    currency = summary.currency.ifBlank { "PHP" },
                    createddate = formatDate(summary.createdDate),
                    priceoverride = summary.priceOverride.toInt(),
                    comment = summary.comment,
                    taxinclinprice = formatDecimal(summary.taxIncludedInPrice),
                    window_number = summary.windowNumber,
                    gcash = formatDecimal(summary.gCash),
                    paymaya = formatDecimal(summary.payMaya),
                    cash = formatDecimal(summary.cash),
                    card = formatDecimal(summary.card),
                    loyaltycard = formatDecimal(summary.loyaltyCard),
                    charge = "0.00",
                    foodpanda = "0.00",
                    grabfood = "0.00"
                )

                // Convert TransactionRecords to TransactionRecordRequest
                val recordsRequest = records.mapIndexed { index, record ->
                    TransactionRecordRequest(
                        transactionid = summary.transactionId,
                        receiptid = summary.receiptId,
                        store = summary.store,
                        linenum = index + 1,
                        itemid = record.itemId ?: "",
                        itemname = record.name ?:  "", // Add fallback logic
                        itemgroup = record.itemGroup ?: "",
                        price = formatDecimal(record.price),
                        netprice = formatDecimal(record.netPrice ?: record.price),
                        qty = formatQuantity(record.quantity),
                        discamount = formatDecimal(record.discountAmount),
                        costamount = formatDecimal(record.costAmount ?: 0.0),
                        netamount = formatDecimal(record.netAmount ?: record.total),
                        grossamount = formatDecimal(record.grossAmount ?: record.subtotal),
                        custaccount = record.customerAccount ?: "WALK-IN",
                        priceoverride = record.priceOverride?.toInt() ?: 0,
                        paymentmethod = record.paymentMethod.ifBlank { "Cash" },
                        staff = record.staff ?: "Unknown Staff",
                        linedscamount = formatDecimal(record.lineDiscountAmount ?: 0.0),
                        linediscpct = formatDecimal(record.lineDiscountPercentage ?: 0.0),
                        custdiscamount = formatDecimal(record.customerDiscountAmount ?: 0.0),
                        unit = record.unit ?: "PCS",
                        unitqty = formatDecimal(record.unitQuantity ?: record.quantity.toDouble()),
                        unitprice = formatDecimal(record.unitPrice ?: record.price),
                        taxamount = formatDecimal(record.taxAmount),
                        createddate = formatDate(record.createdDate ?: Date()),
                        remarks = record.remarks ?: "",
                        taxinclinprice = record.taxIncludedInPrice?.toInt() ?: 5,
                        description = record.description ?: ""
                    )
                }


                // Create and log the request object
                val request = TransactionSyncRequest(
                    transactionSummary = summaryRequest,
                    transactionRecords = recordsRequest
                )

                Log.d(TAG, "Sending sync request: $request")

                try {
                    val response = api.syncTransaction(request)
                    Log.d(TAG, "Received response: ${response.isSuccessful}, Code: ${response.code()}")

                    if (response.isSuccessful) {
                        response.body()?.let { body ->
                            Log.d(TAG, "Sync successful for transaction: ${body.transactionId}")
                            Result.success(body)
                        } ?: run {
                            Log.e(TAG, "Empty response body")
                            Result.failure(Exception("Empty response body"))
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Sync failed. Error: $errorBody")
                        Result.failure(Exception("Sync failed: ${response.code()} - $errorBody"))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Network error during sync", e)
                    Result.failure(e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during sync process", e)
                Result.failure(e)
            }
        }
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
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.type == ConnectivityManager.TYPE_WIFI
        }
    }

    private fun formatDate(date: Date?): String {
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(date ?: Date())
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date: $date", e)
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())
        }
    }
}