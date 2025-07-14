package com.example.possystembw.ui.ViewModel

import android.util.Log
import androidx.lifecycle.*
import com.example.possystembw.data.DiscountRepository
import com.example.possystembw.database.Discount
import kotlinx.coroutines.launch

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class DiscountViewModel(private val repository: DiscountRepository) : ViewModel() {
    private val _discounts = MutableLiveData<List<Discount>>()
    val discounts: LiveData<List<Discount>> = _discounts

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun fetchDiscounts() {
        _isLoading.value = true
        viewModelScope.launch {
            repository.getAllDiscounts().collect { result ->
                _isLoading.value = false
                result.onSuccess { discountList ->
                    Log.d("DiscountViewModel", "✅ Successfully fetched ${discountList.size} discounts")

                    // Sort discounts by PARAMETER in ascending order
                    val sortedDiscounts = discountList.sortedBy { it.PARAMETER }
                    _discounts.value = sortedDiscounts

                    // Enhanced debugging - show all discount details
                    sortedDiscounts.forEach { discount ->
                        Log.d("DiscountViewModel",
                            "Discount: ${discount.DISCOFFERNAME} | " +
                                    "Default: ${discount.PARAMETER} | " +
                                    "Type: ${discount.DISCOUNTTYPE} | " +
                                    "GF: ${discount.GRABFOOD_PARAMETER} | " +
                                    "FP: ${discount.FOODPANDA_PARAMETER} | " +
                                    "MR: ${discount.MANILAPRICE_PARAMETER} | " +
                                    "Mall: ${discount.MALLPRICE_PARAMETER} | " +
                                    "GF Mall: ${discount.GRABFOODMALL_PARAMETER} | " +
                                    "FP Mall: ${discount.FOODPANDAMALL_PARAMETER}"
                        )
                    }

                }.onFailure { e ->
                    Log.e("DiscountViewModel", "❌ Error fetching discounts", e)
                    _error.value = "Error fetching discounts: ${e.message}"
                }
            }
        }
    }

    // Method to force refresh discounts
    fun refreshDiscounts() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.refreshDiscounts()
            _isLoading.value = false

            result.onSuccess { discountList ->
                Log.d("DiscountViewModel", "✅ Force refresh successful: ${discountList.size} discounts")
                val sortedDiscounts = discountList.sortedBy { it.PARAMETER }
                _discounts.value = sortedDiscounts
                _error.value = null // Clear any previous errors
            }.onFailure { e ->
                Log.e("DiscountViewModel", "❌ Force refresh failed", e)
                _error.value = "Failed to refresh discounts: ${e.message}"
            }
        }
    }

    fun getCurrentDiscounts(): List<Discount>? {
        val current = _discounts.value
        Log.d("DiscountViewModel", "getCurrentDiscounts() returned ${current?.size ?: 0} discounts")
        return current
    }

    // Method to clear errors
    fun clearError() {
        _error.value = null
    }
}

class DiscountViewModelFactory(private val repository: DiscountRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiscountViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiscountViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}