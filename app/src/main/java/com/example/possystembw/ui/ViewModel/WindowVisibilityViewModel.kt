package com.example.possystembw.ui.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.data.WindowVisibilityRepository
import com.example.possystembw.database.HiddenWindow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class WindowVisibilityViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: WindowVisibilityRepository

    init {
        val hiddenWindowDao = AppDatabase.getDatabase(application).hiddenWindowDao()
        repository = WindowVisibilityRepository(hiddenWindowDao)
    }

    fun getHiddenWindows(): Flow<List<HiddenWindow>> {
        return repository.getHiddenWindows()
    }

    fun hideWindow(windowId: Int) {
        viewModelScope.launch {
            repository.hideWindow(windowId)
        }
    }

    fun showWindow(windowId: Int) {
        viewModelScope.launch {
            repository.showWindow(windowId)
        }
    }

    fun hideWindowTable(windowTableId: Int) {
        viewModelScope.launch {
            repository.hideWindowTable(windowTableId)
        }
    }

    fun showWindowTable(windowTableId: Int) {
        viewModelScope.launch {
            repository.showWindowTable(windowTableId)
        }
    }

    class WindowVisibilityViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WindowVisibilityViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return WindowVisibilityViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}