package com.example.possystembw.ui.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.possystembw.data.CustomerPurchaseHistory
import com.example.possystembw.data.CustomerRepository
import com.example.possystembw.database.Customer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomerViewModel(private val customerRepository: CustomerRepository) : ViewModel() {
    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Customer>>(emptyList())
    val searchResults: StateFlow<List<Customer>> = _searchResults.asStateFlow()

    private val _purchaseHistory = MutableStateFlow<List<CustomerPurchaseHistory>>(emptyList())
    val purchaseHistory: StateFlow<List<CustomerPurchaseHistory>> = _purchaseHistory.asStateFlow()

    init {
        // Collect local customers immediately
        viewModelScope.launch {
            customerRepository.allCustomers.collect {
                _customers.value = it
            }
        }
    }

    fun refreshCustomers() {
        viewModelScope.launch {
            try {
                val allCustomers = customerRepository.getAllCustomers()
                _customers.value = allCustomers
            } catch (e: Exception) {
                Log.e("CustomerViewModel", "Error refreshing customers", e)
                // Local data will still be available through Flow
            }
        }
    }
    fun clearPurchaseHistory() {
        viewModelScope.launch {
            _purchaseHistory.value = emptyList()
        }
    }

    fun loadCustomerPurchaseHistory(customerName: String) {
        viewModelScope.launch {
            try {
                if (customerName == "Walk-in Customer") {
                    clearPurchaseHistory()
                    return@launch
                }
                val history = customerRepository.getCustomerPurchaseHistory(customerName)
                _purchaseHistory.value = history
            } catch (e: Exception) {
                Log.e("CustomerViewModel", "Error loading purchase history", e)
                _purchaseHistory.value = emptyList()
            }
        }
    }

    fun searchCustomers(query: String) {
        viewModelScope.launch {
            try {
                val results = customerRepository.searchCustomers(query)
                _searchResults.value = results
            } catch (e: Exception) {
                Log.e("CustomerViewModel", "Error searching customers", e)
                // Keep existing search results
            }
        }
    }
}

class CustomerViewModelFactory(private val customerRepository: CustomerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CustomerViewModel(customerRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}