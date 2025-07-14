package com.example.possystembw.ui.ViewModel

import android.app.Application
import android.util.Log
import androidx.constraintlayout.helper.widget.MotionEffect.TAG
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.possystembw.DAO.StockCountingApi
import com.example.possystembw.DAO.StockCountingDao
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.data.StockCountingRepository
import com.example.possystembw.database.StockCountingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class StockCountingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StockCountingRepository
    private val _stockCountingResult = MutableLiveData<Result<List<StockCountingEntity>>?>()
    val stockCountingResult: LiveData<Result<List<StockCountingEntity>>?> = _stockCountingResult
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val database = AppDatabase.getDatabase(application)
    private val stockCountingDao = database.stockCountingDao()
    init {
        val database = AppDatabase.getDatabase(application)
        val stockCountingDao = database.stockCountingDao()

        val retrofit = Retrofit.Builder()
//            .baseUrl("https://eljin.org/")
            .baseUrl("http://10.151.5.239:8000/")
//            .baseUrl("https://ecposmiddleware-aj1882pz3-progenxs-projects.vercel.app/")

            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(StockCountingApi::class.java)
        repository = StockCountingRepository(api, stockCountingDao)
    }

    suspend fun deleteStockCountingData(journalId: Long) {
        withContext(Dispatchers.IO) {
            try {
                stockCountingDao.deleteStockCountingByJournal(journalId)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting stock counting data", e)
                throw e
            }
        }
    }
    fun fetchStockCounting(storeId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _stockCountingResult.value = repository.getStockCounting(storeId)
            } finally {
                _isLoading.value = false
            }
        }
    }


    // Add function to get local data
    fun getLocalStockCounting(storeId: String) {
        viewModelScope.launch {
            try {
                val localData = repository.getLocalStockCounting(storeId)
                _stockCountingResult.value = Result.success(localData)
            } catch (e: Exception) {
                _stockCountingResult.value = Result.failure(e)
            }
        }
    }
}
// StockCountingViewModelFactory.kt
class StockCountingViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockCountingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StockCountingViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}