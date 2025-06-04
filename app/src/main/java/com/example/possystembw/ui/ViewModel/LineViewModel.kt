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
import com.example.possystembw.data.LineTransactionVisibilityRepository
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
import java.util.concurrent.TimeUnit

class LineViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LineRepository
    private val _lineDetailsResult = MutableLiveData<Result<List<LineTransaction>>?>()
    val lineDetailsResult: LiveData<Result<List<LineTransaction>>?> = _lineDetailsResult
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private var currentData: List<LineTransaction> = emptyList()

    private val _syncStatus = MutableLiveData<SyncStatus>()
    val syncStatus: LiveData<SyncStatus> = _syncStatus

    private var storeId: String? = null
    private var journalId: String? = null
    private val _syncProgress = MutableLiveData<SyncProgress>()
    val syncProgress: LiveData<SyncProgress> = _syncProgress

    private val database = AppDatabase.getDatabase(application)
    private val lineTransactionDao = database.lineTransactionDao()
    private val stockCountingDao = database.stockCountingDao()
    private val lineTransactionVisibilityDao = database.lineTransactionVisibilityDao()
    private val visibilityRepository = LineTransactionVisibilityRepository(lineTransactionVisibilityDao)

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
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES)) // Allow multiple connections
            .dispatcher(Dispatcher().apply {
                maxRequestsPerHost = 5 // Allow multiple simultaneous requests to the same host
                maxRequests = 5
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://eljin.org/")
//            .baseUrl("http://10.151.5.239:8000/")
//            .baseUrl("https://ecposmiddleware-aj1882pz3-progenxs-projects.vercel.app/")


            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val lineDetailsApi = retrofit.create(LineDetailsApi::class.java)
        val stockCountingApi = retrofit.create(StockCountingApi::class.java)

        repository = LineRepository(lineDetailsApi, stockCountingApi, lineTransactionDao,visibilityRepository  )
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
    fun hideLineTransaction(itemId: String) {
        viewModelScope.launch {
            repository.hideLineTransaction(itemId)
        }
    }

    fun showLineTransaction(itemId: String) {
        viewModelScope.launch {
            repository.showLineTransaction(itemId)
        }
    }

    suspend fun isLineTransactionHidden(itemId: String): Boolean {
        return repository.isLineTransactionHidden(itemId)
    }

    fun getLineTransactionsWithVisibility(journalId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.getLineTransactionsWithVisibility(journalId)
                // You can create a new LiveData for this if needed
                // _lineTransactionsWithVisibility.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Error getting line transactions with visibility", e)
            } finally {
                _isLoading.value = false
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
                _lineDetailsResult.value = result
                result.onSuccess { data ->
                    currentData = data
                }.onFailure { error ->
                    Log.e(TAG, "Error getting line details", error)
                }
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
                    // Refresh data after sync
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

                // Get unsynced transactions
                val unsyncedItems = repository.getUnsyncedTransactions(currentJournalId)
                if (unsyncedItems.isEmpty()) {
                    _syncProgress.value = SyncProgress(isComplete = true, totalItems = 0)
                    return@launch
                }

                // Initialize progress
                _syncProgress.value = SyncProgress(
                    isComplete = false,
                    totalItems = unsyncedItems.size,
                    currentItem = 0,
                    currentItemId = ""
                )

                // Process each item
                unsyncedItems.forEachIndexed { index, item ->
                    try {
                        // Update progress for current item
                        _syncProgress.value = _syncProgress.value?.copy(
                            currentItem = index + 1,
                            currentItemId = item.itemId ?: "Unknown"
                        )

                        // Post line details
                        val response = withTimeout(5000) {
                            repository.postLineDetails(
                                itemId = item.itemId.orEmpty(),
                                storeId = currentStoreId,
                                journalId = currentJournalId,
                                adjustment = (item.adjustment?.toDoubleOrNull() ?: 0.0).toInt()
                                    .toString(),
                                receivedCount = (item.receivedCount?.toDoubleOrNull()
                                    ?: 0.0).toInt()
                                    .toString(),
                                transferCount = (item.transferCount?.toDoubleOrNull()
                                    ?: 0.0).toInt()
                                    .toString(),
                                wasteCount = (item.wasteCount?.toDoubleOrNull() ?: 0.0).toInt()
                                    .toString(),
                                wasteType = item.wasteType ?: "none",
                                counted = (item.counted?.toDoubleOrNull() ?: 0.0).toInt().toString()
                            )
                        }
                        if (response.isSuccessful) {
                            // Update sync status in database
                            repository.updateSyncStatus(currentJournalId, item.itemId.orEmpty(), 1)
                            Log.d(TAG, "Successfully synced item: ${item.itemId}")
                        } else {
                            Log.e(
                                TAG,
                                "Failed to sync item: ${item.itemId}, code: ${response.code()}"
                            )
                            _syncProgress.value = SyncProgress(
                                isComplete = true,
                                totalItems = unsyncedItems.size,
                                currentItem = index + 1,
                                errorMessage = "Failed to sync item: ${item.itemId}"
                            )
                            return@launch
                        }

                        // Add delay to prevent overwhelming the server
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
    // Add this to LineViewModel
    suspend fun getLocalLineDetails(journalId: String): Result<List<LineTransaction>> =
        repository.getLocalLineDetails(journalId)

    // Add this to LineRepository

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

    suspend fun saveLineDetails(storeId: String, journalId: String, items: List<LineTransaction>): Boolean {
            return try {
                Log.d(TAG, "Saving ${items.size} line transactions for journal: $journalId")

                // Convert all LineTransaction items to LineTransactionEntity
                val entities = items.map { it.toEntity() }

                // Save to repository
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


    fun fetchLineDetails(storeId: String, journalId: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.getLineDetails(storeId, journalId)
                result.onSuccess { transactions ->
                    currentData = transactions
/*
                    LineDataManager.cacheData(journalId, transactions)
*/
                }
                _lineDetailsResult.value = result
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