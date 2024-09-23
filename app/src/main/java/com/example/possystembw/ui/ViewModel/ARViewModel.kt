package com.example.possystembw.ui.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.possystembw.data.ARRepository
import com.example.possystembw.database.AR
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ARViewModel(private val arRepository: ARRepository) : ViewModel() {
    private val _arTypes = MutableStateFlow<List<AR>>(emptyList())
    val arTypes: StateFlow<List<AR>> = _arTypes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Collect local AR types immediately
        viewModelScope.launch {
            arRepository.allARTypes.collect {
                _arTypes.value = it
            }
        }
    }

    fun refreshARTypes() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                arRepository.refreshARTypes()
                // Local data will be updated automatically through Flow
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
                // Load local data as fallback
                _arTypes.value = arRepository.getLocalARTypes()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class ARViewModelFactory(private val arRepository: ARRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ARViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ARViewModel(arRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
