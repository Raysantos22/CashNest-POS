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

    fun fetchDiscounts() {
        viewModelScope.launch {
            repository.getAllDiscounts().collect { result ->
                result.onSuccess { discountList ->
                    Log.d("DiscountViewModel", "Fetched ${discountList.size} discounts")
                    // Sort discounts by PARAMETER in ascending order
                    val sortedDiscounts = discountList.sortedBy { it.PARAMETER }
                    _discounts.value = sortedDiscounts
                }.onFailure { e ->
                    Log.e("DiscountViewModel", "Error fetching discounts", e)
                    _error.value = "Error fetching discounts: ${e.message}"
                    // Fallback to local data, also sorted
                    val localDiscounts = repository.getLocalDiscounts().sortedBy { it.PARAMETER }
                    Log.d("DiscountViewModel", "Fetched ${localDiscounts.size} local discounts")
                    _discounts.value = localDiscounts
                }
            }
        }
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