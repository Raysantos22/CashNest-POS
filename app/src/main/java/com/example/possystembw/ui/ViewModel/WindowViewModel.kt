package com.example.possystembw.ui.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.possystembw.RetrofitClient
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.data.WindowRepository
import com.example.possystembw.database.Window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WindowViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: WindowRepository
    val allWindows: Flow<List<Window>>

    private val _alignedWindows = MutableStateFlow<List<Window>>(emptyList())
    val alignedWindows: StateFlow<List<Window>> = _alignedWindows

    private val _errorState = MutableLiveData<String?>()
    val errorState: LiveData<String?> = _errorState

    init {
        val windowDao = AppDatabase.getDatabase(application).windowDao()
        val apiService = RetrofitClient.apiService
        repository = WindowRepository(windowDao, apiService)
        allWindows = repository.allWindows
    }

    fun refreshWindows() = viewModelScope.launch {
        try {
            repository.refreshWindows()
            _errorState.value = null  // Clear any previous errors
        } catch (e: Exception) {
            Log.e("WindowViewModel", "Failed to refresh windows: ${e.message}", e)
            _errorState.value = "Failed to refresh windows: ${e.message}"
        }
    }

    fun alignWindowsWithTable(tableId: Int) = viewModelScope.launch {
        try {
            val alignedWindows = repository.getWindowsAlignedWithTable(tableId)
            _alignedWindows.value = alignedWindows
        } catch (e: Exception) {
            Log.e("WindowViewModel", "Failed to align windows: ${e.message}", e)
            _errorState.value = "Failed to align windows: ${e.message}"
        }
    }
    fun loadFromLocalDatabase() = viewModelScope.launch {
        try {
            // This will trigger the Flow to emit the latest data from the local database
            repository.loadFromLocalDatabase()
            _errorState.value = null
        } catch (e: Exception) {
            Log.e("WindowViewModel", "Failed to load from local database: ${e.message}", e)
            _errorState.value = "Failed to load from local database: ${e.message}"
        }
    }


    fun getWindowById(id: Int): LiveData<Window?> {
        val windowLiveData = MutableLiveData<Window?>()
        viewModelScope.launch {
            val window = repository.getWindowById(id)
            windowLiveData.postValue(window)
        }
        return windowLiveData
    }

    fun insert(window: Window) = viewModelScope.launch {
        repository.insert(window)
    }

    fun update(window: Window) = viewModelScope.launch {
        repository.update(window)
    }

    fun delete(window: Window) = viewModelScope.launch {
        repository.delete(window)
    }

    fun clearAlignedWindows() {
        _alignedWindows.value = emptyList()
    }
}

class WindowViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WindowViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WindowViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}