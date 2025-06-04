//package com.example.possystembw.ui.ViewModel
//
//import android.app.Application
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.viewModelScope
//import com.example.possystembw.DAO.LineDetailsApi
//import com.example.possystembw.DAO.StockCountingApi
//import com.example.possystembw.data.AppDatabase
//import com.example.possystembw.data.LineRepository
//import com.example.possystembw.data.LineTransactionVisibilityRepository
//import com.example.possystembw.data.LineTransactionWithVisibility
//import kotlinx.coroutines.launch
//import okhttp3.OkHttpClient
//import okhttp3.logging.HttpLoggingInterceptor
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import java.util.concurrent.TimeUnit
//
//class LineTransactionVisibilityViewModel(application: Application) : AndroidViewModel(application) {
//    private val database = AppDatabase.getDatabase(application)
//    private val lineTransactionDao = database.lineTransactionDao()
//    private val lineTransactionVisibilityDao = database.lineTransactionVisibilityDao()
//    private val visibilityRepository = LineTransactionVisibilityRepository(lineTransactionVisibilityDao)
//
//    // Initialize LineRepository
//    private val repository: LineRepository by lazy {
//        val loggingInterceptor = HttpLoggingInterceptor().apply {
//            level = HttpLoggingInterceptor.Level.BODY
//        }
//
//        val client = OkHttpClient.Builder()
//            .connectTimeout(30, TimeUnit.SECONDS)
//            .readTimeout(30, TimeUnit.SECONDS)
//            .writeTimeout(30, TimeUnit.SECONDS)
//            .build()
//
//        val retrofit = Retrofit.Builder()
//            .baseUrl("https://eljin.org/")
//            .client(client)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//
//        val lineDetailsApi = retrofit.create(LineDetailsApi::class.java)
//        val stockCountingApi = retrofit.create(StockCountingApi::class.java)
//
//        LineRepository(lineDetailsApi, stockCountingApi, lineTransactionDao, visibilityRepository)
//    }
//
//    private val _lineTransactionsWithVisibility = MutableLiveData<List<LineTransactionWithVisibility>>()
//    val lineTransactionsWithVisibility: LiveData<List<LineTransactionWithVisibility>> = _lineTransactionsWithVisibility
//
//    private val _isLoading = MutableLiveData<Boolean>()
//    val isLoading: LiveData<Boolean> = _isLoading
//
//    private val _error = MutableLiveData<String?>()
//    val error: LiveData<String?> = _error
//
//    fun loadLineTransactionsWithVisibility(journalId: String) {
//        viewModelScope.launch {
//            _isLoading.value = true
//            _error.value = null
//
//            try {
//                val result = repository.getLineTransactionsWithVisibility(journalId)
//                result.onSuccess { transactions ->
//                    _lineTransactionsWithVisibility.value = transactions
//                }.onFailure { throwable ->
//                    _error.value = throwable.message
//                }
//            } catch (e: Exception) {
//                _error.value = e.message
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }
//
//    fun hideLineTransaction(itemId: String) {
//        viewModelScope.launch {
//            repository.hideLineTransaction(itemId)
//            // Update the current list immediately for better UX
//            updateVisibilityInCurrentList(itemId, false)
//        }
//    }
//
//    fun showLineTransaction(itemId: String) {
//        viewModelScope.launch {
//            repository.showLineTransaction(itemId)
//            // Update the current list immediately for better UX
//            updateVisibilityInCurrentList(itemId, true)
//        }
//    }
//
//    private fun updateVisibilityInCurrentList(itemId: String, isVisible: Boolean) {
//        val currentList = _lineTransactionsWithVisibility.value
//        if (currentList != null) {
//            val updatedList = currentList.map { transactionWithVisibility ->
//                if (transactionWithVisibility.lineTransaction.itemId == itemId) {
//                    transactionWithVisibility.copy(isVisible = isVisible)
//                } else {
//                    transactionWithVisibility
//                }
//            }
//            _lineTransactionsWithVisibility.value = updatedList
//        }
//    }
//
//    // Factory for the ViewModel
//    class Factory(private val application: Application) : ViewModelProvider.Factory {
//        @Suppress("UNCHECKED_CAST")
//        override fun <T : ViewModel> create(modelClass: Class<T>): T {
//            if (modelClass.isAssignableFrom(LineTransactionVisibilityViewModel::class.java)) {
//                return LineTransactionVisibilityViewModel(application) as T
//            }
//            throw IllegalArgumentException("Unknown ViewModel class")
//        }
//    }
//}