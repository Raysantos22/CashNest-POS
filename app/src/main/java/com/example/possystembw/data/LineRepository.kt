package com.example.possystembw.data

import android.util.Log
import com.example.possystembw.DAO.LineDetailsApi
import com.example.possystembw.DAO.LineDetailsData
import com.example.possystembw.DAO.LineTransaction
import com.example.possystembw.DAO.LineTransactionDao
import com.example.possystembw.DAO.StockCountingApi
import com.example.possystembw.database.LineTransactionEntity
import com.example.possystembw.database.hasAnyValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.Response

class LineRepository(
    private val api: LineDetailsApi,
    private val stockCountingApi: StockCountingApi,
    private val dao: LineTransactionDao
) {
    companion object {
        private const val TAG = "LineRepository"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L // Base delay
        private const val MAX_DELAY_MS = 10000L // Maximum delay cap
    }

    suspend fun getUnsyncedTransactions(journalId: String): List<LineTransaction> {
        return dao.getUnsyncedTransactions(journalId).map { it.toModel() }
    }

    private suspend fun isNetworkAvailable(): Boolean {
        return try {
            // You can implement actual network connectivity check here
            // For now, we'll assume network is available
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun hasUnsynced(journalId: String): Boolean {
        return dao.getUnsyncedCount(journalId) > 0
    }

    suspend fun getUnsyncedCount(journalId: String): Int {
        return dao.getUnsyncedCount(journalId)
    }

    suspend fun updateSyncStatus(journalId: String, itemId: String, syncStatus: Int) {
        dao.updateSyncStatus(journalId, itemId, syncStatus)
    }

//    suspend fun getLocalLineDetails(journalId: String): Result<List<LineTransaction>> =
//        runCatching {
//            val localData = dao.getLineTransactionsByJournal(journalId)
//            if (localData.isNotEmpty()) {
//                localData.map { it.toModel() }
//            } else {
//                emptyList()
//            }
//        }

    suspend fun syncTransactions(storeId: String, journalId: String): Result<Unit> = runCatching {
        // Get only unsynced transactions (syncStatus = 0)
        val unsyncedTransactions = dao.getUnsyncedTransactions(journalId)

        if (unsyncedTransactions.isEmpty()) {
            return@runCatching Unit  // Return Unit instead of just returning
        }

        // Post stock counting first with posted = 1
        val stockCountingResult = postStockCounting(storeId, journalId)
        if (!stockCountingResult.isSuccessful) {
            throw Exception("Failed to update stock counting")
        }

        // Process items in parallel batches
        val batchSize = 5
        coroutineScope {
            unsyncedTransactions
                .filter { it.hasAnyValue() }
                .chunked(batchSize)
                .forEach { batch ->
                    batch.map { item ->
                        async {
                            try {
                                val response = api.postLineDetails(
                                    itemId = item.itemId,
                                    storeId = storeId,
                                    journalId = journalId,
                                    adjustment = (item.adjustment.toDoubleOrNull() ?: 0.0).toInt()
                                        .toString(),
                                    receivedCount = (item.receivedCount.toDoubleOrNull()
                                        ?: 0.0).toInt().toString(),
                                    transferCount = (item.transferCount?.toDoubleOrNull()
                                        ?: 0.0).toInt().toString(),
                                    wasteCount = (item.wasteCount.toDoubleOrNull() ?: 0.0).toInt()
                                        .toString(),
                                    wasteType = item.wasteType ?: "none",
                                    counted = (item.counted.toDoubleOrNull() ?: 0.0).toInt()
                                        .toString()
                                )

                                if (response.isSuccessful) {
                                    // Mark as synced in database
                                    dao.markAsSynced(journalId, item.itemId)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error syncing item ${item.itemId}", e)
                                throw e
                            }
                        }
                    }.awaitAll()
                }
        }
        Unit  // Explicit return type at the end of runCatching block
    }


    suspend fun getLineDetails(storeId: String, journalId: String): Result<List<LineTransaction>> =
        runCatching {
            // Validate input parameters
            if (storeId.isBlank() || journalId.isBlank()) {
                Log.e(TAG, "Invalid parameters - storeId: '$storeId', journalId: '$journalId'")
                throw IllegalArgumentException("StoreId and JournalId cannot be blank")
            }

            // Try to get from local database first
            val localData = dao.getLineTransactionsByJournal(journalId)
            if (localData.isNotEmpty()) {
                Log.d(
                    TAG,
                    "Found ${localData.size} records in local database for journal: $journalId"
                )
                return@runCatching localData.map { it.toModel() }
            }

            Log.d(TAG, "No local data found for journal: $journalId, attempting API fetch")

            var lastException: Exception? = null

            for (attempt in 1..MAX_RETRIES) {
                try {
                    Log.d(
                        TAG,
                        "API attempt $attempt/$MAX_RETRIES for storeId: '$storeId', journalId: '$journalId'"
                    )

                    val response = withContext(Dispatchers.IO) {
                        api.getLineDetails(storeId, journalId)
                    }

                    Log.d(
                        TAG,
                        "API Response - Code: ${response.code()}, Success: ${response.isSuccessful}, Message: ${response.message()}"
                    )

                    when {
                        response.isSuccessful -> {
                            val responseBody = response.body()
                            Log.d(TAG, "Response body is null: ${responseBody == null}")

                            when {
                                responseBody == null -> {
                                    Log.e(
                                        TAG,
                                        "Response body is null despite successful HTTP response"
                                    )
                                    lastException = Exception("Server returned null response body")
                                }

                                responseBody.transactions.isEmpty() -> {
                                    Log.i(
                                        TAG,
                                        "API returned empty transactions list for journal: $journalId"
                                    )
                                    // Return empty list instead of null
                                    return@runCatching emptyList<LineTransaction>()
                                }

                                else -> {
                                    val transactions = responseBody.transactions
                                    Log.d(
                                        TAG,
                                        "Successfully retrieved ${transactions.size} transactions from API"
                                    )

                                    // Validate transaction data before saving
                                    val validTransactions = transactions.filter { transaction ->
                                        val isValid = !transaction.itemId.isNullOrBlank()
                                        if (!isValid) {
                                            Log.w(
                                                TAG,
                                                "Filtering out invalid transaction with null/blank itemId"
                                            )
                                        }
                                        isValid
                                    }

                                    if (validTransactions.size != transactions.size) {
                                        Log.w(
                                            TAG,
                                            "Filtered out ${transactions.size - validTransactions.size} invalid transactions"
                                        )
                                    }

                                    // Save to local database with syncStatus = 1
                                    try {
                                        val entities = validTransactions.map {
                                            it.toEntity().copy(syncStatus = 1)
                                        }
                                        dao.saveLineTransactionsWithTransaction(journalId, entities)
                                        Log.d(
                                            TAG,
                                            "Successfully saved ${entities.size} transactions to database"
                                        )
                                    } catch (dbException: Exception) {
                                        Log.e(
                                            TAG,
                                            "Failed to save to database, but returning API data",
                                            dbException
                                        )
                                        // Continue and return the data even if saving fails
                                    }

                                    // Always return non-null list
                                    return@runCatching validTransactions
                                }
                            }
                        }

                        response.code() in 400..499 -> {
                            // Client errors (4xx) - don't retry these
                            val errorBody = try {
                                response.errorBody()?.string() ?: "No error details"
                            } catch (e: Exception) {
                                "Error reading error body: ${e.message}"
                            }
                            val errorMsg =
                                "Client error ${response.code()}: ${response.message()}. Details: $errorBody"
                            Log.e(TAG, errorMsg)
                            throw Exception(errorMsg)
                        }

                        response.code() in 500..599 -> {
                            // Server errors (5xx) - retry these
                            val errorBody = try {
                                response.errorBody()?.string() ?: "No error details"
                            } catch (e: Exception) {
                                "Error reading error body: ${e.message}"
                            }
                            val errorMsg =
                                "Server error ${response.code()}: ${response.message()}. Details: $errorBody"
                            Log.e(TAG, "Server error (will retry): $errorMsg")
                            lastException = Exception(errorMsg)
                        }

                        else -> {
                            val errorMsg =
                                "Unexpected response ${response.code()}: ${response.message()}"
                            Log.e(TAG, errorMsg)
                            lastException = Exception(errorMsg)
                        }
                    }

                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Exception in API attempt $attempt: ${e.javaClass.simpleName} - ${e.message}"
                    )
                    lastException = e

                    // Categorize exceptions for better retry logic
                    val shouldRetry = when (e) {
                        is java.net.UnknownHostException -> {
                            Log.d(TAG, "Network host not found - will retry if attempts remaining")
                            true
                        }

                        is java.net.SocketTimeoutException -> {
                            Log.d(TAG, "Socket timeout - will retry if attempts remaining")
                            true
                        }

                        is java.net.ConnectException -> {
                            Log.d(TAG, "Connection failed - will retry if attempts remaining")
                            true
                        }

                        is java.io.IOException -> {
                            Log.d(TAG, "IO Exception - will retry if attempts remaining")
                            true
                        }

                        is kotlinx.coroutines.TimeoutCancellationException -> {
                            Log.d(TAG, "Coroutine timeout - will retry if attempts remaining")
                            true
                        }

                        is retrofit2.HttpException -> {
                            Log.d(TAG, "HTTP Exception - will retry if attempts remaining")
                            true
                        }

                        else -> {
                            Log.d(TAG, "Non-retryable error: ${e.javaClass.simpleName}")
                            false
                        }
                    }

                    // If it's the last attempt or non-retryable error, throw immediately
                    if (attempt == MAX_RETRIES || !shouldRetry) {
                        throw e
                    }
                }

                // Calculate delay with exponential backoff
                if (attempt < MAX_RETRIES) {
                    val delayTime = RETRY_DELAY_MS * attempt // Exponential backoff
                    Log.d(TAG, "Waiting ${delayTime}ms before attempt ${attempt + 1}...")
                    delay(delayTime)
                }
            }

            // If we reach here, all attempts failed
            val finalErrorMessage =
                "Failed to get data after $MAX_RETRIES attempts. Last error: ${lastException?.message ?: "Unknown error"}"
            Log.e(TAG, finalErrorMessage)
            throw Exception(finalErrorMessage)
        }

    // Add this debugging function to your LineRepository class
    suspend fun testApiConnectivity(storeId: String, journalId: String): String {
        return try {
            Log.d(TAG, "Testing API connectivity for storeId: $storeId, journalId: $journalId")
            val response = api.getLineDetails(storeId, journalId)

            buildString {
                appendLine("API Test Results:")
                appendLine("- Response Code: ${response.code()}")
                appendLine("- Is Successful: ${response.isSuccessful}")
                appendLine("- Message: ${response.message()}")
                appendLine("- Body is null: ${response.body() == null}")
                if (response.body() != null) {
                    appendLine("- Transactions count: ${response.body()?.transactions?.size ?: 0}")
                }
                appendLine("- Raw URL: ${response.raw().request.url}")
                appendLine("- Content-Type: ${response.headers()["Content-Type"]}")

                if (!response.isSuccessful) {
                    try {
                        val errorBody = response.errorBody()?.string()
                        appendLine("- Error Body: $errorBody")
                    } catch (e: Exception) {
                        appendLine("- Error reading error body: ${e.message}")
                    }
                }
            }

        } catch (e: Exception) {
            "API Test Failed: ${e.javaClass.simpleName} - ${e.message}\nStack trace: ${e.stackTraceToString()}"
        }
    }

    private suspend fun fetchFromLocalDatabase(journalId: String): List<LineTransaction> {
        Log.d(TAG, "Attempting to fetch from local database for journal: $journalId")
        val localData = dao.getLineTransactionsByJournal(journalId)
        Log.d(TAG, "Local database query completed. Found ${localData.size} records")

        if (localData.isNotEmpty()) {
            return localData.map { it.toModel() }
        }

        Log.e(TAG, "No data found in local database")
        throw Exception("No data available from API or local database for journal: $journalId")
    }

    suspend fun saveLineTransactions(
        journalId: String,
        entities: List<LineTransactionEntity>
    ): Boolean {
        return try {
            Log.d(TAG, "Starting database transaction for ${entities.size} entities")
            dao.saveLineTransactionsWithTransaction(journalId, entities)
            Log.d(TAG, "Successfully completed database transaction")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Database transaction failed", e)
            false
        }
    }

//    suspend fun postStockCounting(storeId: String, posted: Int, journalId: String): Response<Unit> {
//        return stockCountingApi.postStockCounting(storeId, posted, journalId)
//    }


    suspend fun postLineDetails(
        itemId: String,
        storeId: String,
        journalId: String,
        adjustment: String,
        receivedCount: String,
        transferCount: String,
        wasteCount: String,
        wasteType: String,
        counted: String
    ): Response<Unit> {
        Log.d(
            TAG, """
            Attempting to post line details:
            itemId: $itemId
            storeId: $storeId
            journalId: $journalId
            adjustment: $adjustment
            receivedCount: $receivedCount
            transferCount: $transferCount
            wasteCount: $wasteCount
            wasteType: $wasteType
            counted: $counted
        """.trimIndent()
        )

        try {
            val response = api.postLineDetails(
                itemId, storeId, journalId, adjustment,
                receivedCount, transferCount, wasteCount,
                wasteType, counted
            )

            Log.d(
                TAG, """
                API Response:
                isSuccessful: ${response.isSuccessful}
                code: ${response.code()}
                message: ${response.message()}
                errorBody: ${response.errorBody()?.string()}
                raw url: ${response.raw().request.url}
            """.trimIndent()
            )

            return response
        } catch (e: Exception) {
            Log.e(TAG, "Error posting line details", e)
            throw e
        }
    }
    suspend fun forceApiCall(storeId: String, journalId: String): Result<List<LineTransaction>> =
        runCatching {
            // Validate input parameters
            if (storeId.isBlank() || journalId.isBlank()) {
                Log.e(TAG, "Invalid parameters - storeId: '$storeId', journalId: '$journalId'")
                throw IllegalArgumentException("StoreId and JournalId cannot be blank")
            }

            Log.d(TAG, "Force API call - bypassing local data for journal: $journalId")

            var lastException: Exception? = null

            for (attempt in 1..MAX_RETRIES) {
                try {
                    Log.d(TAG, "Force API attempt $attempt/$MAX_RETRIES for storeId: '$storeId', journalId: '$journalId'")

                    val response = withContext(Dispatchers.IO) {
                        api.getLineDetails(storeId, journalId)
                    }

                    Log.d(TAG, "Force API Response - Code: ${response.code()}, Success: ${response.isSuccessful}")

                    when {
                        response.isSuccessful -> {
                            val responseBody = response.body()

                            when {
                                responseBody == null -> {
                                    Log.e(TAG, "Force API: Response body is null despite successful HTTP response")
                                    lastException = Exception("Server returned null response body")
                                }
                                responseBody.transactions.isEmpty() -> {
                                    Log.i(TAG, "Force API: Server returned empty transactions list for journal: $journalId")
                                    return@runCatching emptyList<LineTransaction>()
                                }
                                else -> {
                                    val transactions = responseBody.transactions
                                    Log.d(TAG, "Force API: Successfully retrieved ${transactions.size} transactions")

                                    // Validate transaction data
                                    val validTransactions = transactions.filter { transaction ->
                                        val isValid = !transaction.itemId.isNullOrBlank()
                                        if (!isValid) {
                                            Log.w(TAG, "Force API: Filtering out invalid transaction with null/blank itemId")
                                        }
                                        isValid
                                    }

                                    if (validTransactions.size != transactions.size) {
                                        Log.w(TAG, "Force API: Filtered out ${transactions.size - validTransactions.size} invalid transactions")
                                    }

                                    // Save fresh data to local database with syncStatus = 1
                                    try {
                                        val entities = validTransactions.map { it.toEntity().copy(syncStatus = 1) }
                                        dao.saveLineTransactionsWithTransaction(journalId, entities)
                                        Log.d(TAG, "Force API: Successfully saved ${entities.size} fresh transactions to database")
                                    } catch (dbException: Exception) {
                                        Log.e(TAG, "Force API: Failed to save to database, but returning API data", dbException)
                                        // Continue and return the data even if saving fails
                                    }

                                    return@runCatching validTransactions
                                }
                            }
                        }
                        response.code() in 400..499 -> {
                            val errorBody = try {
                                response.errorBody()?.string() ?: "No error details"
                            } catch (e: Exception) {
                                "Error reading error body: ${e.message}"
                            }
                            val errorMsg = "Force API: Client error ${response.code()}: ${response.message()}. Details: $errorBody"
                            Log.e(TAG, errorMsg)
                            throw Exception(errorMsg)
                        }
                        response.code() in 500..599 -> {
                            val errorBody = try {
                                response.errorBody()?.string() ?: "No error details"
                            } catch (e: Exception) {
                                "Error reading error body: ${e.message}"
                            }
                            val errorMsg = "Force API: Server error ${response.code()}: ${response.message()}. Details: $errorBody"
                            Log.e(TAG, "Force API: Server error (will retry): $errorMsg")
                            lastException = Exception(errorMsg)
                        }
                        else -> {
                            val errorMsg = "Force API: Unexpected response ${response.code()}: ${response.message()}"
                            Log.e(TAG, errorMsg)
                            lastException = Exception(errorMsg)
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Force API: Exception in attempt $attempt: ${e.javaClass.simpleName} - ${e.message}")
                    lastException = e

                    val shouldRetry = when (e) {
                        is java.net.UnknownHostException,
                        is java.net.SocketTimeoutException,
                        is java.net.ConnectException,
                        is java.io.IOException,
                        is kotlinx.coroutines.TimeoutCancellationException,
                        is retrofit2.HttpException -> {
                            Log.d(TAG, "Force API: Retryable error - will retry if attempts remaining")
                            true
                        }
                        else -> {
                            Log.d(TAG, "Force API: Non-retryable error: ${e.javaClass.simpleName}")
                            false
                        }
                    }

                    if (attempt == MAX_RETRIES || !shouldRetry) {
                        throw e
                    }
                }

                if (attempt < MAX_RETRIES) {
                    val delayTime = RETRY_DELAY_MS * attempt
                    Log.d(TAG, "Force API: Waiting ${delayTime}ms before attempt ${attempt + 1}...")
                    delay(delayTime)
                }
            }

            val finalErrorMessage = "Force API: Failed to get fresh data after $MAX_RETRIES attempts. Last error: ${lastException?.message ?: "Unknown error"}"
            Log.e(TAG, finalErrorMessage)
            throw Exception(finalErrorMessage)
        }
    suspend fun postStockCounting(storeId: String, journalId: String): Response<Unit> {
        return stockCountingApi.postStockCounting(storeId, "1", journalId)  // Pass "1" as String
    }

    suspend fun getLocalLineDetails(journalId: String): Result<List<LineTransaction>> =
        runCatching {
            val localData = dao.getLineTransactionsByJournal(journalId)
            // Always return non-null list, even if empty
            localData.map { it.toModel() }
        }
}


fun LineTransaction.toEntity() = LineTransactionEntity(
    journalId = journalId ?: "",  // Use safe call with default
    lineNum = lineNum,
    transDate = transDate ?: "",
    itemId = itemId ?: "",
    itemDepartment = itemDepartment ?: "",
    storeName = storeName ?: "",
    adjustment = adjustment ?: "",
    costPrice = costPrice,
    priceUnit = priceUnit,
    salesAmount = salesAmount,
    inventOnHand = inventOnHand,
    counted = counted ?: "",
    reasonRefRecId = reasonRefRecId,
    variantId = variantId,
    posted = posted ?: 0,
    postedDateTime = postedDateTime,
    createdAt = createdAt,
    updatedAt = updatedAt,
    wasteCount = wasteCount ?: "",
    receivedCount = receivedCount ?: "",
    wasteType = wasteType,
    transferCount = transferCount,
    wasteDate = transDate ?: "",
    itemGroupId = itemGroupId ?: "",
    itemName = itemName ?: "",
    itemType = itemType ?: 0,
    nameAlias = nameAlias ?: "",
    notes = notes ?: "",
    itemGroup = itemGroup ?: "",
    itemDepartmentLower = itemDepartmentLower ?: "",
    zeroPriceValid = zeroPriceValid ?: 0,
    dateBlocked = dateBlocked ?: "",
    dateToBeBlocked = dateToBeBlocked ?: "",
    blockedOnPos = blockedOnPos ?: 0,
    activeOnDelivery = activeOnDelivery ?: 0,
    barcode = barcode ?: "",
    dateToActivateItem = dateToActivateItem,
    mustSelectUom = mustSelectUom ?: 0,
    production = production,
    moq = moq ?: 0,
    fgCount = fgCount,
    transparentStocks = transparentStocks,
    stocks = stocks,
    postedLower = postedLower ?: 0,
    syncStatus = syncStatus ?: 1
)

fun LineTransactionEntity.toModel() = LineTransaction(
    journalId = journalId,
    lineNum = lineNum,
    transDate = transDate,
    itemId = itemId,
    itemDepartment = itemDepartment,
    storeName = storeName,
    adjustment = adjustment,
    costPrice = costPrice,
    priceUnit = priceUnit,
    salesAmount = salesAmount,
    inventOnHand = inventOnHand,
    counted = counted,
    reasonRefRecId = reasonRefRecId,
    variantId = variantId,
    posted = posted,
    postedDateTime = postedDateTime,
    createdAt = createdAt,
    updatedAt = updatedAt,
    wasteCount = wasteCount,
    receivedCount = receivedCount,
    wasteType = wasteType,
    transferCount = transferCount,
    wasteDate = transDate,
    itemGroupId = itemGroupId,
    itemIdLower = itemId.lowercase(),
    itemName = itemName,
    itemType = itemType,
    nameAlias = nameAlias,
    notes = notes,
    itemGroup = itemGroup,
    itemDepartmentLower = itemDepartmentLower,
    zeroPriceValid = zeroPriceValid,
    dateBlocked = dateBlocked,
    dateToBeBlocked = dateToBeBlocked,
    blockedOnPos = blockedOnPos,
    activeOnDelivery = activeOnDelivery,
    barcode = barcode,
    dateToActivateItem = dateToActivateItem,
    mustSelectUom = mustSelectUom,
    production = production,
    moq = moq,
    fgCount = fgCount,
    transparentStocks = transparentStocks,
    stocks = stocks,
    postedLower = postedLower,
    syncStatus = syncStatus
)