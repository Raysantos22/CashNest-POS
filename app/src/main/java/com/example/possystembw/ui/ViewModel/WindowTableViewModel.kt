package com.example.possystembw.ui.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.possystembw.data.WindowTableRepository
import com.example.possystembw.database.WindowTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class WindowTableViewModel(private val repository: WindowTableRepository) : ViewModel() {
    val allWindowTables: Flow<List<WindowTable>> = repository.allWindowTables


    fun loadFromLocalDatabase() {
        viewModelScope.launch {
            repository.loadFromLocalDatabase()
        }
    }
    fun refreshWindowTables() {
        viewModelScope.launch {
            repository.refreshWindowTables()
        }
    }
}


class WindowTableViewModelFactory(private val repository: WindowTableRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WindowTableViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WindowTableViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
