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
    private val dao: LineTransactionDao,
    private val visibilityRepository: LineTransactionVisibilityRepository // Add this


) {
    companion object {
        private const val TAG = "LineRepository"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L // 1 second
    }

    suspend fun getUnsyncedTransactions(journalId: String): List<LineTransaction> {
        return dao.getUnsyncedTransactions(journalId).map { it.toModel() }
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
            // Try to get from local database first
            val localData = dao.getLineTransactionsByJournal(journalId)
            if (localData.isNotEmpty()) {
                Log.d(TAG, "Found ${localData.size} records in local database")
                return@runCatching localData.map { it.toModel() }
            }

            // If no local data, try API with retries
            for (attempt in 1..MAX_RETRIES) {
                try {
                    Log.d(TAG, "API attempt $attempt for storeId: $storeId, journalId: $journalId")
                    val response = api.getLineDetails(storeId, journalId)

                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody != null && responseBody.transactions.isNotEmpty()) {
                            val transactions = responseBody.transactions
                            Log.d(
                                TAG,
                                "Successfully retrieved ${transactions.size} transactions from API"
                            )

                            // Save to local database with syncStatus = 1
                            val entities = transactions.map { it.toEntity().copy(syncStatus = 1) }
                            dao.saveLineTransactionsWithTransaction(journalId, entities)
                            Log.d(TAG, "Saved ${entities.size} transactions to database")

                            return@runCatching transactions
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in attempt $attempt", e)
                    if (attempt == MAX_RETRIES) throw e
                    delay(RETRY_DELAY_MS)
                }
            }
            throw Exception("Failed to get data after $MAX_RETRIES attempts")
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
    suspend fun hideLineTransaction(itemId: String) {
        visibilityRepository.hideLineTransaction(itemId)
    }

    suspend fun showLineTransaction(itemId: String) {
        visibilityRepository.showLineTransaction(itemId)
    }

    suspend fun isLineTransactionHidden(itemId: String): Boolean {
        return visibilityRepository.isLineTransactionHidden(itemId)
    }

    // Get line transactions with visibility information
    suspend fun getLineTransactionsWithVisibility(journalId: String): Result<List<LineTransactionWithVisibility>> =
        runCatching {
            // Get line transactions from local database only (since they're already loaded from batch count)
            val localData = dao.getLineTransactionsByJournal(journalId)

            if (localData.isEmpty()) {
                throw Exception("No local data found for journal: $journalId. Please ensure data is loaded first.")
            }

            val lineTransactions = localData.map { it.toModel() }

            val lineTransactionsWithVisibility = lineTransactions.map { transaction ->
                val isVisible = !transaction.itemId?.let {
                    visibilityRepository.isLineTransactionHidden(it)
                }!! ?: false // Default to visible if itemId is null

                LineTransactionWithVisibility(transaction, isVisible)
            }

            lineTransactionsWithVisibility
        }
    suspend fun postStockCounting(storeId: String, journalId: String): Response<Unit> {
        return stockCountingApi.postStockCounting(storeId, "1", journalId)  // Pass "1" as String
    }

    suspend fun getLocalLineDetails(journalId: String): Result<List<LineTransaction>> =
        runCatching {
            val localData = dao.getLineTransactionsByJournal(journalId)
            if (localData.isNotEmpty()) {
                localData.map { it.toModel() }
            } else {
                emptyList()
            }
        }
}


fun LineTransaction.toEntity() = LineTransactionEntity(
    journalId = journalId.orEmpty(),
    lineNum = lineNum,
    transDate = transDate.orEmpty(),
    itemId = itemId.orEmpty(),
    itemDepartment = itemDepartment.orEmpty(),
    storeName = storeName.orEmpty(),
    adjustment = adjustment.orEmpty(),
    costPrice = costPrice,
    priceUnit = priceUnit,
    salesAmount = salesAmount,
    inventOnHand = inventOnHand,
    counted = counted.orEmpty(),
    reasonRefRecId = reasonRefRecId,
    variantId = variantId,
    posted = posted ?: 0,
    postedDateTime = postedDateTime,
    createdAt = createdAt,
    updatedAt = updatedAt,
    wasteCount = wasteCount.orEmpty(),
    receivedCount = receivedCount.orEmpty(),
    wasteType = wasteType,
    transferCount = transferCount,
    wasteDate = transDate.orEmpty(),
    itemGroupId = itemGroupId.orEmpty(),
    itemName = itemName.orEmpty(),
    itemType = itemType ?: 0,
    nameAlias = nameAlias.orEmpty(),  // Provide default empty string for nullable field
    notes = notes.orEmpty(),          // Provide default empty string for nullable field
    itemGroup = itemGroup.orEmpty(),
    itemDepartmentLower = itemDepartmentLower.orEmpty(),
    zeroPriceValid = zeroPriceValid ?: 0,
    dateBlocked = dateBlocked.orEmpty(),
    dateToBeBlocked = dateToBeBlocked.orEmpty(),
    blockedOnPos = blockedOnPos ?: 0,
    activeOnDelivery = activeOnDelivery ?: 0,
    barcode = barcode.orEmpty(),
    dateToActivateItem = dateToActivateItem,
    mustSelectUom = mustSelectUom ?: 0,
    production = production,
    moq = moq ?: 0,
    fgCount = fgCount,
    transparentStocks = transparentStocks,
    stocks = stocks,
    postedLower = postedLower ?: 0,
    syncStatus = syncStatus ?: 1  // Default to 1 if not set



)

// Extension function to convert Entity to API model
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
    wasteDate = transDate.orEmpty(),
    itemGroupId = itemGroupId,
    itemIdLower = itemId.lowercase(), // Convert itemId to lowercase for itemIdLower
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

)