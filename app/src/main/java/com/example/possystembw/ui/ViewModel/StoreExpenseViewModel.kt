package com.example.possystembw.ui.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.possystembw.DAO.StoreExpenseApi
import com.example.possystembw.DAO.StoreExpenseDao
import com.example.possystembw.DAO.StoreExpenseRequest
import com.example.possystembw.database.StoreExpense
import com.example.possystembw.ui.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StoreExpenseViewModel(
    private val expenseDao: StoreExpenseDao,
    private val expenseApi: StoreExpenseApi
) : ViewModel() {
    private val currentStoreId: String
        get() = SessionManager.getCurrentUser()?.storeid
            ?: throw IllegalStateException("No store ID available")

    val allExpenses = expenseDao.getAllExpenses(currentStoreId).asLiveData()

    init {
        fetchRemoteExpenses()
    }

    fun fetchRemoteExpenses() {
        viewModelScope.launch {
            try {
                val response = expenseApi.getStoreExpenses(currentStoreId)
                if (response.isSuccessful) {
                    response.body()?.storeExpense?.let { remoteExpenses ->
                        // Clear local expenses for this store and insert remote ones
                        expenseDao.deleteExpensesByStoreId(currentStoreId)
                        remoteExpenses.forEach { remoteExpense ->
                            val localExpense = StoreExpense(
                                name = remoteExpense.name,
                                expenseType = remoteExpense.expenseType ?: remoteExpense.expense_type ?: "",
                                amount = remoteExpense.amount.toDoubleOrNull() ?: 0.0,
                                receivedBy = remoteExpense.receivedBy ?: remoteExpense.received_by ?: "",
                                approvedBy = remoteExpense.approvedBy ?: remoteExpense.approved_by ?: "",
                                effectDate = remoteExpense.effectDate ?: remoteExpense.effect_date ?: "",
                                storeId = remoteExpense.storeId ?: remoteExpense.store_id ?: "",
                                syncStatus = 1
                            )
                            expenseDao.insertExpense(localExpense)
                        }
                    }
                } else {
                    Log.e("ViewModel", "Error fetching expenses: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Network error", e)
            }
        }
    }

    fun addExpense(
        name: String,
        expenseType: String,
        amount: Double,
        receivedBy: String,
        approvedBy: String,
        effectDate: String
    ) {
        viewModelScope.launch {
            try {
                val expense = StoreExpense(
                    name = name,
                    expenseType = expenseType, // Ensure this matches exactly
                    amount = amount,
                    receivedBy = receivedBy,
                    approvedBy = approvedBy,
                    effectDate = effectDate,
                    storeId = currentStoreId,
                    syncStatus = 0
                )

                val id = expenseDao.insertExpense(expense)

                // Prepare request for API
                val request = StoreExpenseRequest(
                    name = name,
                    expense_type = expenseType, // Use snake_case for API
                    amount = amount.toString(),
                    received_by = receivedBy,
                    approved_by = approvedBy,
                    effect_date = effectDate,
                    store_id = currentStoreId,
                    timestamp = System.currentTimeMillis()
                )

                // Post to API
                val response = expenseApi.postStoreExpense(request)
                if (response.isSuccessful) {
                    // Update sync status
                    expenseDao.updateSyncStatus(id, 1)

                    // Optional: Fetch updated expenses
                    fetchRemoteExpenses()
                } else {
                    Log.e("ViewModel", "Error posting expense: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error adding expense", e)
            }
        }
    }

    fun updateExpense(expense: StoreExpense) {
        viewModelScope.launch {
            try {
                // Prepare request for API
                val request = StoreExpenseRequest(
                    id = expense.id,
                    name = expense.name,
                    expense_type = expense.expenseType,
                    amount = expense.amount.toString(),
                    received_by = expense.receivedBy,
                    approved_by = expense.approvedBy,
                    effect_date = expense.effectDate,
                    store_id = expense.storeId,
                    timestamp = System.currentTimeMillis()
                )

                // Post to API
                val response = expenseApi.postStoreExpense(request)
                if (response.isSuccessful) {
                    // Update local database
                    val updatedExpense = expense.copy(syncStatus = 1)
                    expenseDao.updateExpense(updatedExpense)

                    // Optional: Fetch updated expenses
                    fetchRemoteExpenses()
                } else {
                    Log.e("ViewModel", "Error updating expense: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error updating expense", e)
            }
        }
    }

    fun deleteExpense(expense: StoreExpense) {
        viewModelScope.launch {
            if (expense.storeId == currentStoreId) {
                expenseDao.deleteExpense(expense)
            }
        }
    }

    // Add the Factory class back
    class Factory(
        private val expenseDao: StoreExpenseDao,
        private val expenseApi: StoreExpenseApi
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StoreExpenseViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return StoreExpenseViewModel(expenseDao, expenseApi) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}