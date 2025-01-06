package com.example.possystembw.ui.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.possystembw.Repository.StaffRepository
import com.example.possystembw.database.StaffEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StaffViewModel(private val repository: StaffRepository) : ViewModel() {
    private val _staffData = MutableStateFlow<List<StaffEntity>>(emptyList())
    val staffData: StateFlow<List<StaffEntity>> = _staffData.asStateFlow()

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun refreshStaffData(storeId: String) {
        viewModelScope.launch {
            try {
                val result = repository.fetchAndStoreStaff(storeId)
                result.onSuccess { staff ->
                    _staffData.value = staff
                }.onFailure { error ->
                    _error.value = error.message ?: "Failed to fetch staff data"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            }
        }
    }

    class StaffViewModelFactory(private val repository: StaffRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StaffViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return StaffViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}