package com.example.possystembw.ui.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.possystembw.DAO.LineDetailsApi
import com.example.possystembw.DAO.LineDetailsData
import com.example.possystembw.DAO.LineTransaction
import com.example.possystembw.DAO.LineTransactionDao
import com.example.possystembw.DAO.StockCountingApi
import com.example.possystembw.DAO.StockCountingDao
import com.example.possystembw.RetrofitClient
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.data.LineRepository
import com.example.possystembw.data.toEntity
import com.example.possystembw.database.LineTransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class LineViewModel(application: Application) : AndroidViewModel(application) {
    val repository: LineRepository
    private val _lineDetailsResult = MutableLiveData<Result<List<LineTransaction>>?>()
    val lineDetailsResult: LiveData<Result<List<LineTransaction>>?> = _lineDetailsResult
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private var currentData: List<LineTransaction> = emptyList()  // Keep non-null with default

    private val _syncStatus = MutableLiveData<SyncStatus>()
    val syncStatus: LiveData<SyncStatus> = _syncStatus

    private var storeId: String? = null
    private var journalId: String? = null
    private val _syncProgress = MutableLiveData<SyncProgress>()
    val syncProgress: LiveData<SyncProgress> = _syncProgress

    private val database = AppDatabase.getDatabase(application)
    private val lineTransactionDao = database.lineTransactionDao()
    private val stockCountingDao = database.stockCountingDao()

    companion object {
        private const val TAG = "LineViewModel"
    }

    data class SyncProgress(
        val isComplete: Boolean = false,
        val totalItems: Int = 0,
        val currentItem: Int = 0,
        val currentItemId: String = "",
        val errorMessage: String? = null
    )

    sealed class SyncStatus {
        data class Success(val itemId: String) : SyncStatus()
        data class Error(val itemId: String, val error: String) : SyncStatus()
    }

    init {
        val database = AppDatabase.getDatabase(application)
        val lineTransactionDao = database.lineTransactionDao()

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .dispatcher(Dispatcher().apply {
                maxRequestsPerHost = 5
                maxRequests = 5
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://eljin.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val lineDetailsApi = retrofit.create(LineDetailsApi::class.java)
        val stockCountingApi = retrofit.create(StockCountingApi::class.java)

        repository = LineRepository(lineDetailsApi, stockCountingApi, lineTransactionDao)
    }

    suspend fun getUnsyncedTransactions(journalId: String): List<LineTransaction> {
        return repository.getUnsyncedTransactions(journalId)
    }

    fun setIds(storeId: String, journalId: String) {
        this.storeId = storeId
        this.journalId = journalId
    }

    suspend fun deleteAllData(journalId: String) {
        withContext(Dispatchers.IO) {
            try {
                lineTransactionDao.deleteLineTransactionsByJournal(journalId)
                stockCountingDao.deleteStockCountingByJournal(journalId.toLong())
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting data", e)
                throw e
            }
        }
    }

    fun getLineDetails(storeId: String, journalId: String) {
        this.storeId = storeId
        this.journalId = journalId

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.getLineDetails(storeId, journalId)

                // Handle the result properly with null safety
                val processedResult = result.fold(
                    onSuccess = { transactions ->
                        // transactions is guaranteed to be non-null List<LineTransaction>
                        currentData = transactions
                        Result.success(transactions)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error getting line details", error)
                        Result.failure<List<LineTransaction>>(error)
                    }
                )

                _lineDetailsResult.value = processedResult
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun syncTransactions() {
        if (storeId == null || journalId == null) {
            Log.e(TAG, "Store ID or Journal ID is null")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.syncTransactions(storeId!!, journalId!!)
                result.onSuccess {
                    Log.d(TAG, "Sync completed successfully")
                    getLineDetails(storeId!!, journalId!!)
                }.onFailure { error ->
                    Log.e(TAG, "Sync failed", error)
                    _syncStatus.value = SyncStatus.Error("", error.message ?: "Unknown error")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateLineTransaction(transaction: LineTransaction) {
        currentData = currentData.map {
            if (it.itemId == transaction.itemId) transaction else it
        }
        _lineDetailsResult.value = Result.success(currentData)
    }

    fun hasUnsyncedChanges(): Boolean {
        return currentData.any { it.syncStatus == 0 }
    }

    fun getUnsyncedCount(): Int {
        return currentData.count { it.syncStatus == 0 }
    }

    fun syncModifiedData() {
        viewModelScope.launch {
            try {
                val currentStoreId = storeId ?: run {
                    _syncProgress.value = SyncProgress(
                        isComplete = true,
                        errorMessage = "Store ID not set"
                    )
                    return@launch
                }

                val currentJournalId = journalId ?: run {
                    _syncProgress.value = SyncProgress(
                        isComplete = true,
                        errorMessage = "Journal ID not set"
                    )
                    return@launch
                }

                val unsyncedItems = repository.getUnsyncedTransactions(currentJournalId)
                if (unsyncedItems.isEmpty()) {
                    _syncProgress.value = SyncProgress(isComplete = true, totalItems = 0)
                    return@launch
                }

                _syncProgress.value = SyncProgress(
                    isComplete = false,
                    totalItems = unsyncedItems.size,
                    currentItem = 0,
                    currentItemId = ""
                )

                unsyncedItems.forEachIndexed { index, item ->
                    try {
                        _syncProgress.value = _syncProgress.value?.copy(
                            currentItem = index + 1,
                            currentItemId = item.itemId ?: "Unknown"
                        )

                        val response = withTimeout(5000) {
                            repository.postLineDetails(
                                itemId = item.itemId.orEmpty(),
                                storeId = currentStoreId,
                                journalId = currentJournalId,
                                adjustment = (item.adjustment?.toDoubleOrNull() ?: 0.0).toInt().toString(),
                                receivedCount = (item.receivedCount?.toDoubleOrNull() ?: 0.0).toInt().toString(),
                                transferCount = (item.transferCount?.toDoubleOrNull() ?: 0.0).toInt().toString(),
                                wasteCount = (item.wasteCount?.toDoubleOrNull() ?: 0.0).toInt().toString(),
                                wasteType = item.wasteType ?: "none",
                                counted = (item.counted?.toDoubleOrNull() ?: 0.0).toInt().toString()
                            )
                        }

                        if (response.isSuccessful) {
                            repository.updateSyncStatus(currentJournalId, item.itemId.orEmpty(), 1)
                            Log.d(TAG, "Successfully synced item: ${item.itemId}")
                        } else {
                            Log.e(TAG, "Failed to sync item: ${item.itemId}, code: ${response.code()}")
                            _syncProgress.value = SyncProgress(
                                isComplete = true,
                                totalItems = unsyncedItems.size,
                                currentItem = index + 1,
                                errorMessage = "Failed to sync item: ${item.itemId}"
                            )
                            return@launch
                        }

                        delay(100)

                    } catch (e: Exception) {
                        when {
                            e is UnknownHostException ||
                                    e is ConnectException ||
                                    e is SocketTimeoutException -> {
                                _syncProgress.value = SyncProgress(
                                    isComplete = true,
                                    errorMessage = "No internet connection"
                                )
                            }
                            else -> {
                                _syncProgress.value = SyncProgress(
                                    isComplete = true,
                                    errorMessage = "Error syncing data: ${e.message}"
                                )
                            }
                        }
                        return@launch
                    }
                }

                _syncProgress.value = SyncProgress(
                    isComplete = true,
                    totalItems = unsyncedItems.size,
                    currentItem = unsyncedItems.size
                )

            } catch (e: Exception) {
                _syncProgress.value = SyncProgress(
                    isComplete = true,
                    errorMessage = if (e is UnknownHostException ||
                        e is ConnectException ||
                        e is SocketTimeoutException
                    ) {
                        "No internet connection"
                    } else {
                        "Error: ${e.message}"
                    }
                )
            }
        }
    }

    suspend fun getLocalLineDetails(journalId: String): Result<List<LineTransaction>> {
        return repository.getLocalLineDetails(journalId).fold(
            onSuccess = { transactions ->
                // transactions could be nullable, handle it safely
                Result.success(transactions ?: emptyList())
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    private suspend fun updateItemSyncStatus(journalId: String, itemId: String, synced: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                val syncStatus = if (synced) 1 else 0
                repository.updateSyncStatus(journalId, itemId, syncStatus)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating sync status", e)
            }
        }
    }
    suspend fun getItemsWithTransactions(storeId: String, journalId: String): List<LineTransaction> {
        return withContext(Dispatchers.IO) {
            try {
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                Log.d("LineViewModel", "Starting filter query")
                Log.d("LineViewModel", "Parameters - storeId: $storeId, journalId: $journalId, currentDate: $currentDate")

                // First, let's get all line transactions for debugging
                val allLineTransactions = database.lineTransactionDao().getAllLineTransactions(journalId)
                Log.d("LineViewModel", "Total line transactions for journal: ${allLineTransactions.size}")

                // Get all transactions for debugging
                val allTransactions = database.transactionDao().getAllTransactionsForDate(currentDate)
                Log.d("LineViewModel", "Total transactions for date $currentDate: ${allTransactions.size}")

                // Check for specific itemId from transaction in line_transactions
                val transactionItemIds = allTransactions.mapNotNull { it.itemId }.distinct()
                Log.d("LineViewModel", "Transaction itemIds: $transactionItemIds")

                transactionItemIds.forEach { itemId ->
                    val foundInLineTransactions = allLineTransactions.any { it.itemId == itemId }
                    Log.d("LineViewModel", "ItemId $itemId exists in line_transactions: $foundInLineTransactions")
                }

                // Log some sample data
                allLineTransactions.take(3).forEachIndexed { index, item ->
                    Log.d("LineViewModel", "Sample LineTransaction $index: itemId=${item.itemId}, adjustment=${item.adjustment}, receivedCount=${item.receivedCount}, wasteCount=${item.wasteCount}, posted=${item.posted}")
                }

                allTransactions.take(3).forEachIndexed { index, transaction ->
                    Log.d("LineViewModel", "Sample Transaction $index: itemId=${transaction.itemId}, quantity=${transaction.quantity}, timestamp=${transaction.timestamp}")
                }

                // Check for items with modified quantities (regardless of posted status)
                val itemsWithActivity = allLineTransactions.filter { item ->
                    val adjustment = item.adjustment.toDoubleOrNull() ?: 0.0
                    val receivedCount = item.receivedCount.toDoubleOrNull() ?: 0.0
                    val wasteCount = item.wasteCount.toDoubleOrNull() ?: 0.0
                    val transferCount = item.transferCount?.toDoubleOrNull() ?: 0.0
                    val counted = item.counted.toDoubleOrNull() ?: 0.0

                    (receivedCount - adjustment != 0.0) ||
                            wasteCount != 0.0 ||
                            transferCount != 0.0 ||
                            counted != 0.0 ||
                            item.syncStatus == 0
                }
                Log.d("LineViewModel", "Items with any activity (ignoring posted status): ${itemsWithActivity.size}")

                // Check if the POS transaction itemId exists in line_transactions
                val posItemId = "PAS-PRO-053" // From your log
                val lineTransactionExists = database.lineTransactionDao().findLineTransactionByItemId(posItemId, journalId)
                Log.d("LineViewModel", "POS ItemId '$posItemId' exists in line_transactions: ${lineTransactionExists != null}")

                // Get all itemIds from line_transactions to see the pattern
                val allItemIds = database.lineTransactionDao().getAllItemIdsForJournal(journalId)
                Log.d("LineViewModel", "First 10 itemIds in line_transactions: ${allItemIds.take(10)}")

                // Check matching count between tables
                val matchingCount = database.lineTransactionDao().getMatchingItemCount(journalId)
                Log.d("LineViewModel", "Matching itemIds between transactions and line_transactions: $matchingCount")

                // Use the corrected query
                val filteredLineTransactions = database.lineTransactionDao().getItemsWithTransactions(
                    journalId = journalId,
                    currentDate = currentDate
                )

                Log.d("LineViewModel", "Filtered line transactions count: ${filteredLineTransactions.size}")

                // If no results, try the alternative query (without date filter)
                val alternativeResults = if (filteredLineTransactions.isEmpty()) {
                    Log.d("LineViewModel", "No results with date filter, trying alternative query")
                    database.lineTransactionDao().getItemsWithQuantities(journalId)
                } else {
                    filteredLineTransactions
                }

                Log.d("LineViewModel", "Alternative query results: ${alternativeResults.size}")

                // Convert to LineTransaction objects
                val result = alternativeResults.map { entity ->
                    Log.d("LineViewModel", "Converting entity: itemId=${entity.itemId}, adjustment=${entity.adjustment}, receivedCount=${entity.receivedCount}")

                    LineTransaction(
                        journalId = entity.journalId,
                        lineNum = entity.lineNum,
                        transDate = entity.transDate,
                        itemId = entity.itemId,
                        itemDepartment = entity.itemDepartment,
                        storeName = entity.storeName,
                        adjustment = entity.adjustment,
                        costPrice = entity.costPrice,
                        priceUnit = entity.priceUnit,
                        salesAmount = entity.salesAmount,
                        inventOnHand = entity.inventOnHand,
                        counted = entity.counted,
                        reasonRefRecId = entity.reasonRefRecId,
                        variantId = entity.variantId,
                        posted = entity.posted,
                        postedDateTime = entity.postedDateTime,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt,
                        wasteCount = entity.wasteCount,
                        receivedCount = entity.receivedCount,
                        wasteType = entity.wasteType,
                        transferCount = entity.transferCount,
                        wasteDate = entity.wasteDate,
                        itemGroupId = entity.itemGroupId,
                        itemName = entity.itemName,
                        itemType = entity.itemType,
                        nameAlias = entity.nameAlias,
                        notes = entity.notes,
                        itemGroup = entity.itemGroup,
                        itemDepartmentLower = entity.itemDepartmentLower,
                        zeroPriceValid = entity.zeroPriceValid,
                        dateBlocked = entity.dateBlocked,
                        dateToBeBlocked = entity.dateToBeBlocked,
                        blockedOnPos = entity.blockedOnPos,
                        activeOnDelivery = entity.activeOnDelivery,
                        barcode = entity.barcode,
                        dateToActivateItem = entity.dateToActivateItem,
                        mustSelectUom = entity.mustSelectUom,
                        production = entity.production,
                        moq = entity.moq,
                        fgCount = entity.fgCount,
                        transparentStocks = entity.transparentStocks,
                        stocks = entity.stocks,
                        postedLower = entity.postedLower,
                        syncStatus = entity.syncStatus
                    )
                }

                Log.d("LineViewModel", "Final result count: ${result.size}")
                result

            } catch (e: Exception) {
                Log.e("LineViewModel", "Error filtering items with transactions", e)
                Log.e("LineViewModel", "Exception details: ${e.message}")
                Log.e("LineViewModel", "Stack trace: ${e.stackTrace.joinToString("\n")}")
                emptyList()
            }
        }
    }

    suspend fun saveLineDetails(storeId: String, journalId: String, items: List<LineTransaction>): Boolean {
        return try {
            Log.d(TAG, "Saving ${items.size} line transactions for journal: $journalId")
            val entities = items.map { it.toEntity() }
            repository.saveLineTransactions(journalId, entities)
        } catch (e: Exception) {
            Log.e(TAG, "Error in saveLineDetails", e)
            false
        }
    }

    suspend fun hasUnsynced(journalId: String): Boolean {
        return repository.hasUnsynced(journalId)
    }

    suspend fun getUnsyncedCount(journalId: String): Int {
        return repository.getUnsyncedCount(journalId)
    }

    suspend fun postStockCounting(storeId: String, journalId: String): Boolean {
        return try {
            val response = repository.postStockCounting(storeId, journalId)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error posting stock counting", e)
            false
        }
    }

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
    ): Boolean {
        return try {
            val response = repository.postLineDetails(
                itemId, storeId, journalId, adjustment, receivedCount,
                transferCount, wasteCount, wasteType, counted
            )
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error posting line details", e)
            false
        }
    }

    fun getCurrentData(): List<LineTransaction> = currentData

    suspend fun clearLocalData(journalId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Clearing local data for journal: $journalId")
                lineTransactionDao.deleteLineTransactionsByJournal(journalId)
                Log.d(TAG, "Successfully cleared local data for journal: $journalId")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing local data for journal: $journalId", e)
                throw e
            }
        }
    }

    fun forceRefreshFromApi(storeId: String, journalId: String) {
        this.storeId = storeId
        this.journalId = journalId

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Clear local data first
                clearLocalData(journalId)

                // Force API call by calling repository directly (bypassing local check)
                val result = repository.forceApiCall(storeId, journalId)

                val processedResult = result.fold(
                    onSuccess = { transactions ->
                        val safeTransactions = transactions ?: emptyList()
                        currentData = safeTransactions
                        Log.d(TAG, "Force refresh successful: ${safeTransactions.size} items")
                        Result.success(safeTransactions)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Force refresh failed", error)
                        Result.failure<List<LineTransaction>>(error)
                    }
                )

                _lineDetailsResult.value = processedResult
            } catch (e: Exception) {
                Log.e(TAG, "Exception in force refresh", e)
                _lineDetailsResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Enhanced fetchLineDetails with better force refresh handling
    fun fetchLineDetails(storeId: String, journalId: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = if (forceRefresh) {
                    // Clear local data first when forcing refresh
                    clearLocalData(journalId)
                    // Then get fresh data from API
                    repository.forceApiCall(storeId, journalId)
                } else {
                    // Normal flow (check local first, then API)
                    repository.getLineDetails(storeId, journalId)
                }

                val processedResult = result.fold(
                    onSuccess = { transactions ->
                        val safeTransactions = transactions ?: emptyList()
                        currentData = safeTransactions
                        if (forceRefresh) {
                            Log.d(TAG, "Force refresh completed: ${safeTransactions.size} items")
                        }
                        Result.success(safeTransactions)
                    },
                    onFailure = { error ->
                        if (forceRefresh) {
                            Log.e(TAG, "Force refresh failed", error)
                        }
                        Result.failure<List<LineTransaction>>(error)
                    }
                )

                _lineDetailsResult.value = processedResult
            } catch (e: Exception) {
                _lineDetailsResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    }

// LineViewModelFactory.kt
class LineViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LineViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}