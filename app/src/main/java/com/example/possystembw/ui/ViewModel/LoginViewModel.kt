package com.example.possystembw.ui.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.possystembw.RetrofitClient
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.data.UserRepository
import com.example.possystembw.database.User
import kotlinx.coroutines.launch
import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.possystembw.DAO.TransactionApi
import com.example.possystembw.DAO.TransactionDao
import com.example.possystembw.DAO.TransactionSyncApi
import com.example.possystembw.Repository.StaffRepository
import com.example.possystembw.data.NumberSequenceRemoteRepository
import com.example.possystembw.database.StaffEntity
import com.example.possystembw.database.TransactionRecord
import com.example.possystembw.database.TransactionSummary
import com.example.possystembw.ui.SessionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository: UserRepository
    private val _loginResult = MutableLiveData<Result<User>>()
    val loginResult: LiveData<Result<User>> = _loginResult
    private val _loginDataState = MutableLiveData<LoginDataState>()
    val loginDataState: LiveData<LoginDataState> = _loginDataState

    private val _fetchUsersResult = MutableLiveData<Result<List<User>>>()
    val fetchUsersResult: LiveData<Result<List<User>>> = _fetchUsersResult

    private val _transactionLoadingState = MutableLiveData<TransactionLoadingState>()
    val transactionLoadingState: LiveData<TransactionLoadingState> = _transactionLoadingState

    private val transactionSyncApi = RetrofitClient.transactionSyncApi
    private val transactionDao = AppDatabase.getDatabase(application).transactionDao()
    private val numberSequenceRemoteRepository: NumberSequenceRemoteRepository
    private val numberSequenceRemoteViewModel: NumberSequenceRemoteViewModel

    private val staffRepository: StaffRepository
    private val _staffData = MutableLiveData<Result<List<StaffEntity>>>()
    val staffData: LiveData<Result<List<StaffEntity>>> = _staffData

    init {

        val staffDao = AppDatabase.getDatabase(application).staffDao()
        staffRepository = StaffRepository(RetrofitClient.staffApi, staffDao)

        val userDao = AppDatabase.getDatabase(application).userDao()
        val userApi = RetrofitClient.userApi
        val numberSequenceApi = RetrofitClient.numberSequenceApi
        val numberSequenceRemoteDao = AppDatabase.getDatabase(application).numberSequenceRemoteDao()

        userRepository = UserRepository(userDao, userApi)
        numberSequenceRemoteRepository =
            NumberSequenceRemoteRepository(numberSequenceApi, numberSequenceRemoteDao)
        numberSequenceRemoteViewModel =
            NumberSequenceRemoteViewModel(numberSequenceRemoteRepository)
    }
    private suspend fun fetchStaffData(storeId: String) {
        try {
            _loginDataState.value = LoginDataState.Loading
            val result = staffRepository.fetchAndStoreStaff(storeId)
            result.onSuccess { staffList ->
                Log.d("LoginViewModel", "Successfully fetched ${staffList.size} staff members")
            }.onFailure { error ->
                Log.e("LoginViewModel", "Error fetching staff data", error)
                // Don't fail the entire login process if staff data fetch fails
                if (error.message?.contains("503") == true) {
                    _loginDataState.value = LoginDataState.Warning(
                        "Staff data sync delayed - will retry automatically"
                    )
                }
            }
            _staffData.postValue(result)
        } catch (e: Exception) {
            Log.e("LoginViewModel", "Exception in fetchStaffData", e)
            _staffData.postValue(Result.failure(e))
        }
    }


    fun fetchUsers() {
        viewModelScope.launch {
            val result = userRepository.fetchAndStoreUsers()
            result.onSuccess { users ->
                Log.d("LoginViewModel", "Successfully fetched ${users.size} users")
                users.forEach { user ->
                    Log.d(
                        "LoginViewModel",
                        "User: ${user.name}, Email: ${user.email}, Role: ${user.role}"
                    )
                }
            }.onFailure { error ->
                Log.e("LoginViewModel", "Failed to fetch users", error)
            }
            _fetchUsersResult.value = result
        }
    }

    private fun determinePaymentMethod(summary: TransactionSummary): String {
        return when {
            summary.cash > 0 -> "CASH"
            summary.gCash > 0 -> "GCASH"
            summary.payMaya > 0 -> "PAYMAYA"
            summary.card > 0 -> "CARD"
            summary.charge > 0 -> "CHARGE"
            summary.foodpanda > 0 -> "FOODPANDA"
            summary.grabfood > 0 -> "GRABFOOD"
            else -> "Cash" // Default to Cash
        }
    }

    private fun calculateTotalAmount(summary: TransactionSummary): Double {
        return listOfNotNull(
            summary.cash,
            summary.gCash,
            summary.payMaya,
            summary.card,
            summary.charge,
            summary.foodpanda,
            summary.grabfood
        ).sum()
    }

    private suspend fun loadTransactions(storeId: String) = withContext(Dispatchers.IO) {
        try {
            _transactionLoadingState.postValue(TransactionLoadingState.Loading)

            coroutineScope {
                val summariesDeferred = async { fetchTransactionSummaries(storeId) }
                val detailsDeferred = async { fetchTransactionDetails(storeId) }

                val summaries = summariesDeferred.await()
                val details = detailsDeferred.await()

                Log.d(
                    "LoginViewModel",
                    "Fetched ${summaries.size} summaries and ${details.size} details"
                )

                // Process summaries
                summaries.forEach { summary ->
                    try {
                        val processedSummary = summary.copy(
                            paymentMethod = determinePaymentMethod(summary),
                            storeKey = if (summary.storeKey.isNullOrEmpty()) storeId else summary.storeKey,
                            storeSequence = if (summary.storeSequence.isNullOrEmpty()) "0" else summary.storeSequence,
                            comment = summary.comment ?: "",
                            customerName = summary.customerName ?: "Walk-in Customer",
                            currency = if (summary.currency.isNullOrEmpty()) "PHP" else summary.currency,
                            discountType = summary.discountType ?: "",
                            markupDescription = summary.markupDescription ?: "",
                            totalAmountPaid = calculateTotalAmount(summary),
                            changeGiven = 0.0
                        )
                        transactionDao.insertTransactionSummary(processedSummary)
                    } catch (e: Exception) {
                        Log.e("LoginViewModel", "Error saving summary: ${summary.transactionId}", e)
                    }
                }

                // Process details with null safety
                details.forEach { detail ->
                    try {
                        if (detail.transactionId != null) {
                            // Create a new detail with required fields
                            val processedDetail = detail.copy(
                                paymentMethod = detail.paymentMethod, // Set default payment method
                                storeKey = storeId,
                                storeSequence = "0",
                                windowNumber = 1, // Ensure default window number
                                comment = detail.comment ?: "",
                                name = detail.name ?: "",
                                receiptNumber = detail.receiptNumber ?: detail.receiptId ?: ""
                            )
                            transactionDao.insertAll(listOf(processedDetail))
                            Log.d(
                                "LoginViewModel",
                                "Successfully inserted detail: ${detail.transactionId}"
                            )
                        } else {
                            Log.e(
                                "LoginViewModel",
                                "Skipping detail with null transactionId: $detail"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("LoginViewModel", "Error processing detail", e)
                        Log.e("LoginViewModel", "Detail that caused error: $detail")
                        e.printStackTrace()
                    }
                }

                _transactionLoadingState.postValue(
                    TransactionLoadingState.Success(
                        summaryCount = summaries.size,
                        detailsCount = details.size
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("LoginViewModel", "Error loading transactions", e)
            _transactionLoadingState.postValue(
                TransactionLoadingState.Error(
                    "Failed to load transactions: ${e.localizedMessage}"
                )
            )
        }
    }


    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val user = userRepository.getLocalUserByEmail(email)
                if (user != null) {
                    val result = BCrypt.verifyer().verify(password.toCharArray(), user.password)
                    if (result.verified) {
                        if (user.storeid.isNullOrEmpty()) {
                            _loginResult.value = Result.failure(Exception("User has no associated store ID"))
                            return@launch
                        }

                        _loginDataState.value = LoginDataState.Loading

                        try {
                            // Start fetching staff data but don't wait for it
                            launch { fetchStaffData(user.storeid) }

                            // Continue with other data loading
                            val dataLoadSuccess = loadAllRequiredData(user.storeid)

                            if (dataLoadSuccess) {
                                SessionManager.setCurrentUser(user)
                                _loginResult.value = Result.success(user)
                            } else {
                                _loginResult.value = Result.failure(Exception("Failed to load all required data"))
                            }
                        } catch (e: Exception) {
                            _loginDataState.value = LoginDataState.Error("Error loading data: ${e.message}")
                            _loginResult.value = Result.failure(e)
                        }
                    } else {
                        _loginResult.value = Result.failure(Exception("Invalid credentials"))
                    }
                } else {
                    _loginResult.value = Result.failure(Exception("User not found"))
                }
            } catch (e: Exception) {
                _loginResult.value = Result.failure(e)
            }
        }
    }


private suspend fun loadAllRequiredData(storeId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            var success = true
            var summaryCount = 0
            var detailCount = 0
            var hasNumberSequence = false

            // 1. Load Transactions
            try {
                val summaries = fetchTransactionSummaries(storeId)
                val details = fetchTransactionDetails(storeId)

                // Process and save summaries
                summaries.forEach { summary ->
                    try {
                        val processedSummary = summary.copy(
                            paymentMethod = determinePaymentMethod(summary),
                            storeKey = summary.storeKey ?: storeId,
                            storeSequence = summary.storeSequence ?: "0",
                            comment = summary.comment ?: "",
                            customerName = summary.customerName ?: "Walk-in Customer",
                            currency = summary.currency ?: "PHP",
                            discountType = summary.discountType?: "",
                            totalAmountPaid = calculateTotalAmount(summary),
                            grossAmount = summary.grossAmount,
                            netAmount = summary.netAmount,
                            costAmount = summary.costAmount,
                            changeGiven = 0.0,
                            zReportId = summary.zReportId

                        )
                        transactionDao.insertTransactionSummary(processedSummary)
                        summaryCount++
                    } catch (e: Exception) {
                        Log.e("LoginViewModel", "Error saving summary: ${summary.transactionId}", e)
                        success = false
                    }
                }

                // Process and save details
                details.forEach { detail ->
                    try {
                        if (detail.transactionId != null) {
                            val processedDetail = detail.copy(
                                storeKey = storeId,
                                storeSequence = "0",
                                windowNumber = 1,
                                grossAmount = detail.grossAmount,
                                netAmount = detail.netAmount,
                                costAmount = detail.costAmount,
                                discountAmount = detail.discountAmount,
                                discountType = detail.discountType?: ""


                                )
                            transactionDao.insertAll(listOf(processedDetail))
                            detailCount++
                        }
                    } catch (e: Exception) {
                        Log.e("LoginViewModel", "Error saving detail", e)
                        success = false
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error loading transactions", e)
                success = false
            }

            // 2. Load Number Sequence
            try {
                val numberSequenceResult = numberSequenceRemoteRepository.fetchAndUpdateNumberSequence(storeId)
                numberSequenceResult.onSuccess {
                    SessionManager.setCurrentNumberSequence(it)
                    hasNumberSequence = true
                }.onFailure {
                    success = false
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error loading number sequence", e)
                success = false
            }

            // Update state with results
            _loginDataState.postValue(
                if (success) {
                    LoginDataState.Success(
                        transactionSummaryCount = summaryCount,
                        transactionRecordCount = detailCount,
                        hasNumberSequence = hasNumberSequence
                    )
                } else {
                    LoginDataState.Error("Failed to load all required data")
                }
            )

            success
        } catch (e: Exception) {
            _loginDataState.postValue(LoginDataState.Error(e.message ?: "Unknown error"))
            false
        }
    }

    private suspend fun fetchTransactionSummaries(storeId: String): List<TransactionSummary> {
        return try {
            Log.d("LoginViewModel", "Fetching summaries for store: $storeId")
            val response = transactionSyncApi.getTransactionSummaries(storeId)

            if (response.isSuccessful) {
                val summaries = response.body() ?: emptyList()
                Log.d("LoginViewModel", "Successfully fetched ${summaries.size} summaries")
                summaries
            } else {
                Log.e("LoginViewModel", "Error response: ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("LoginViewModel", "Exception fetching summaries", e)
            emptyList()
        }
    }

    private suspend fun fetchTransactionDetails(storeId: String): List<TransactionRecord> {
        return try {
            Log.d("LoginViewModel", "Fetching details for store: $storeId")
            val response = transactionSyncApi.getTransactionDetails(storeId)

            if (response.isSuccessful) {
                val details = response.body() ?: emptyList()
                Log.d("LoginViewModel", "Successfully fetched ${details.size} details")
                // Log first detail for debugging
                if (details.isNotEmpty()) {
                    Log.d("LoginViewModel", "First detail: ${details.first()}")
                }
                details
            } else {
                Log.e("LoginViewModel", "Error response: ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("LoginViewModel", "Exception fetching details", e)
            emptyList()
        }
    }
    }

sealed class LoginDataState {
    object Loading : LoginDataState()
    data class Error(val message: String) : LoginDataState()
    data class Warning(val message: String) : LoginDataState()
    data class Success(
        val transactionSummaryCount: Int,
        val transactionRecordCount: Int,
        val hasNumberSequence: Boolean = false
    ) : LoginDataState()
}
// Extension functions to convert response objects to database entities

/*
class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository: UserRepository
    private val _loginResult = MutableLiveData<Result<User>>()
    val loginResult: LiveData<Result<User>> = _loginResult

    private val _fetchUsersResult = MutableLiveData<Result<List<User>>>()
    val fetchUsersResult: LiveData<Result<List<User>>> = _fetchUsersResult

    init {
        val userDao = AppDatabase.getDatabase(application).userDao()
        val userApi = RetrofitClient.userApi
        userRepository = UserRepository(userDao, userApi)
    }

    fun fetchUsers() {
        viewModelScope.launch {
            val result = userRepository.fetchAndStoreUsers()
            result.onSuccess { users ->
                Log.d("LoginViewModel", "Successfully fetched ${users.size} users")
                users.forEach { user ->
                    Log.d("LoginViewModel", "User: ${user.name}, Email: ${user.email}, Role: ${user.role}")
                }
            }.onFailure { error ->
                Log.e("LoginViewModel", "Failed to fetch users", error)
            }
            _fetchUsersResult.value = result
        }
    }


    fun login(email: String, password: String) {
        viewModelScope.launch {
            val user = userRepository.getLocalUserByEmail(email)
            if (user != null) {
                val result = BCrypt.verifyer().verify(password.toCharArray(), user.password)
                if (result.verified) {
                    _loginResult.value = Result.success(user)
                } else {
                    _loginResult.value = Result.failure(Exception("Invalid credentials"))
                }
            } else {
                _loginResult.value = Result.failure(Exception("User not found"))
            }
        }
    }
}*/
//    fun login(email: String, password: String) {
//        viewModelScope.launch {
//            try {
//                val user = userRepository.getLocalUserByEmail(email)
//                if (user != null) {
//                    val result = BCrypt.verifyer().verify(password.toCharArray(), user.password)
//                    if (result.verified) {
//                        if (user.storeid.isNullOrEmpty()) {
//                            _loginResult.value = Result.failure(Exception("User has no associated store ID"))
//                            return@launch
//                        }
//
//                        try {
//                            // Load transactions first
//                            loadTransactions(user.storeid)
//
//                            // Then fetch number sequence
//                            val numberSequenceResult = numberSequenceRemoteRepository.fetchAndUpdateNumberSequence(user.storeid)
//                            numberSequenceResult.onSuccess { numberSequence ->
//                                SessionManager.setCurrentUser(user)
//                                SessionManager.setCurrentNumberSequence(numberSequence)
//                                _loginResult.value = Result.success(user)
//                            }.onFailure { error ->
//                                _loginResult.value = Result.failure(Exception("Failed to fetch number sequence: ${error.message}"))
//                            }
//                        } catch (e: Exception) {
//                            // Still proceed with login if transaction loading fails
//                            Log.e("LoginViewModel", "Error during transaction loading", e)
//                            SessionManager.setCurrentUser(user)
//                            _loginResult.value = Result.success(user)
//                        }
//                    } else {
//                        _loginResult.value = Result.failure(Exception("Invalid credentials"))
//                    }
//                } else {
//                    _loginResult.value = Result.failure(Exception("User not found"))
//                }
//            } catch (e: Exception) {
//                _loginResult.value = Result.failure(e)
//            }
//        }
//    }