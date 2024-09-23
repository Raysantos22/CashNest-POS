package com.example.possystembw.ui.ViewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.possystembw.DAO.TransactionSyncRequest
import com.example.possystembw.DAO.TransactionSyncResponse
import com.example.possystembw.RetrofitClient
import com.example.possystembw.data.TransactionRepository
import com.example.possystembw.database.TransactionRecord
import com.example.possystembw.database.TransactionSummary
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionViewModel(private val repository: TransactionRepository) : ViewModel() {
    private val _transactions = MutableLiveData<List<TransactionSummary>>()
    val transactions: LiveData<List<TransactionSummary>> = _transactions
    private val _syncStatus = MutableLiveData<Result<TransactionSyncResponse>>()
    val syncStatus: LiveData<Result<TransactionSyncResponse>> = _syncStatus

    private val _transactionItems = MutableLiveData<List<TransactionRecord>>()
    val transactionItems: LiveData<List<TransactionRecord>> = _transactionItems

    fun loadTransactions() {
        viewModelScope.launch {
            _transactions.value = repository.getTransactions()
        }
    }

    fun loadTransactionItems(transactionId: String) {
        viewModelScope.launch {
            try {
                val items = repository.getTransactionItems(transactionId)
                _transactionItems.postValue(items)
                Log.d("TransactionViewModel", "Loaded ${items.size} items for transaction $transactionId")
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error loading transaction items", e)
                _transactionItems.postValue(emptyList())
            }
        }
    }
    suspend fun getTransactionItems(transactionId: String): List<TransactionRecord> {
        return withContext(Dispatchers.IO) {
            repository.getTransactionItems(transactionId)
        }
    }

    fun clearTransactionItems() {
        _transactionItems.value = emptyList()
    }

    fun updateTransactionSummary(transaction: TransactionSummary) {
        viewModelScope.launch {
            try {
                repository.updateTransactionSummary(transaction)
                Log.d("TransactionViewModel", "Updated transaction summary for ${transaction.transactionId}")
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error updating transaction summary", e)
            }
        }
    }

    fun updateTransactionRecords(records: List<TransactionRecord>) {
        viewModelScope.launch {
            try {
                repository.updateTransactionRecords(records)
                Log.d("TransactionViewModel", "Updated ${records.size} transaction records")
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error updating transaction records", e)
            }
        }
    }
    suspend fun insertTransactionRecords(records: List<TransactionRecord>) {
        withContext(Dispatchers.IO) {
            repository.insertTransactionRecords(records)
        }
    }
    fun startAutoSync(context: Context) {
        viewModelScope.launch {
            while (true) {
                repository.syncAllPendingTransactions(context)
                delay(5 * 1000) // Wait 5 seconds before next sync attempt
            }
        }
    }

    fun syncTransaction(transactionId: String) {
        viewModelScope.launch {
            try {
                val summary = repository.getTransactionSummary(transactionId)
                val records = repository.getTransactionRecords(transactionId)
                val result = repository.syncTransaction(summary, records)

                result.onSuccess {
                    repository.transactionDao.updateSyncStatus(transactionId, true)
                }

                _syncStatus.value = result
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error during sync", e)
                _syncStatus.value = Result.failure(e)
            }
        }
    }




class TransactionViewModelFactory(private val repository: TransactionRepository) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TransactionViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}