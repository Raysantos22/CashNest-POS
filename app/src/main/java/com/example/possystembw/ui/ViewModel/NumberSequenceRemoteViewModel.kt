package com.example.possystembw.ui.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.possystembw.DAO.NumberSequenceValue
import com.example.possystembw.data.NumberSequenceRemoteRepository
import kotlinx.coroutines.launch

class NumberSequenceRemoteViewModel(
    private val repository: NumberSequenceRemoteRepository
) : ViewModel() {
    private val _numberSequenceResult = MutableLiveData<Result<NumberSequenceValue>>()
    val numberSequenceResult: LiveData<Result<NumberSequenceValue>> = _numberSequenceResult

    private val _cartSequenceUpdateResult = MutableLiveData<Result<Boolean>>()
    val cartSequenceUpdateResult: LiveData<Result<Boolean>> = _cartSequenceUpdateResult
    fun updateCartSequence(storeId: String) {
        viewModelScope.launch {
            val result = repository.updateCartSequence(storeId)
            _cartSequenceUpdateResult.value = result
        }
    }

    /**
     * Gets the next cart sequence number
     */
    suspend fun getNextCartSequenceNumber(storeId: String): String {
        return repository.getNextCartSequenceNumber(storeId)
    }

    /**
     * Gets current cart sequence
     */
    suspend fun getCurrentCartSequence(storeId: String): Int {
        return repository.getCurrentCartSequence(storeId)
    }
    fun fetchNumberSequence(storeId: String) {
        viewModelScope.launch {
            val result = repository.fetchAndUpdateNumberSequence(storeId)
            _numberSequenceResult.value = result
        }
    }

    class NumberSequenceRemoteViewModelFactory(
        private val repository: NumberSequenceRemoteRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NumberSequenceRemoteViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return NumberSequenceRemoteViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

