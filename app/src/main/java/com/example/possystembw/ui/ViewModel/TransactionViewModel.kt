package com.example.possystembw.ui.ViewModel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.constraintlayout.helper.widget.MotionEffect.TAG
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.possystembw.DAO.TransactionRecordRequest
import com.example.possystembw.DAO.TransactionSummaryRequest
import com.example.possystembw.DAO.TransactionSyncRequest
import com.example.possystembw.DAO.TransactionSyncResponse
import com.example.possystembw.RetrofitClient
import com.example.possystembw.data.NumberSequenceRemoteRepository
import com.example.possystembw.data.NumberSequenceRepository
import com.example.possystembw.data.TransactionRepository
import com.example.possystembw.database.TransactionRecord
import com.example.possystembw.database.TransactionSummary
import com.example.possystembw.ui.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TransactionViewModel(
    val repository: TransactionRepository,
    private val numberSequenceRemoteRepository: NumberSequenceRemoteRepository // Changed from NumberSequenceRepository
) : ViewModel() {
    private val _transactions = MutableLiveData<List<TransactionSummary>>()
    val transactions: LiveData<List<TransactionSummary>> = _transactions
    private val _syncStatus = MutableLiveData<Result<TransactionSyncResponse>>()
    val syncStatus: LiveData<Result<TransactionSyncResponse>> = _syncStatus

    init {
        repository.startPeriodicSync(viewModelScope)
    }
    private val _transactionItems = MutableLiveData<List<TransactionRecord>>()
    val transactionItems: LiveData<List<TransactionRecord>> = _transactionItems



    suspend fun generateTransactionId(storeId: String): String {
        return repository.generateTransactionId(storeId)
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _transactions.value = repository.getTransactions()
        }
    }

    fun loadTransactionItems(transactionId: String) {
        viewModelScope.launch {
            try {
                val items = repository.getTransactionItems(transactionId)
                _transactionItems.postValue(items)
                Log.d(
                    "TransactionViewModel",
                    "Loaded ${items.size} items for transaction $transactionId"
                )
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error loading transaction items", e)
                _transactionItems.postValue(emptyList())
            }
        }
    }

    suspend fun getTransactionItems(transactionId: String): List<TransactionRecord> {
        return withContext(Dispatchers.IO) {
            repository.getTransactionItems(transactionId)
        }
    }

    fun clearTransactionItems() {
        _transactionItems.value = emptyList()
    }

    fun updateTransactionSummary(transaction: TransactionSummary) {
        viewModelScope.launch {
            try {
                repository.updateTransactionSummary(transaction)
                Log.d(
                    "TransactionViewModel",
                    "Updated transaction summary for ${transaction.transactionId}"
                )
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error updating transaction summary", e)
            }
        }
    }

    fun updateTransactionRecords(records: List<TransactionRecord>) {
        viewModelScope.launch {
            try {
                repository.updateTransactionRecords(records)
                Log.d("TransactionViewModel", "Updated ${records.size} transaction records")
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error updating transaction records", e)
            }
        }
    }


    suspend fun insertTransactionSummary(transaction: TransactionSummary) {
        withContext(Dispatchers.IO) {
            repository.insertTransactionSummary(transaction)
        }
    }
    suspend fun insertTransactionRecords(records: List<TransactionRecord>) {
        withContext(Dispatchers.IO) {
            repository.insertTransactionRecords(records)
        }
    }
    fun updateRefundReceiptId(transactionId: String, refundReceiptId: String) {
        viewModelScope.launch {
            try {
                repository.updateRefundReceiptId(transactionId, refundReceiptId)
                Log.d("TransactionViewModel", "Updated refund receipt ID for $transactionId")
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error updating refund receipt ID", e)
            }
        }
    }

    fun startAutoSync(context: Context) {
        viewModelScope.launch {
            while (true) {
                try {
                    // Remove network connectivity check to avoid blocking sync
                    val currentStore = SessionManager.getCurrentUser()?.storeid ?: continue

                    // Get ALL transactions, not just for the current store
                    val allTransactions = repository.getTransactions()

                    // Sync ALL transactions in a single batch
                    allTransactions.forEach { transaction ->
                        try {
                            // Get all records for the transaction
                            val records = repository.getTransactionRecords(transaction.transactionId)

                            // Sync without extensive preprocessing
                            val syncRequest = TransactionSyncRequest(
                                transactionSummary = TransactionSummaryRequest(
                                    transactionid = transaction.transactionId,
                                    type = transaction.type,
                                    receiptid = transaction.receiptId,
                                    store = transaction.store,
                                    storeKey = transaction.storeKey,
                                    storeSequence = transaction.storeSequence,
                                    staff = transaction.staff,
                                    custaccount = transaction.customerAccount,
                                    netamount = formatDecimal(transaction.netAmount),
                                    costamount = formatDecimal(transaction.costAmount),
                                    grossamount = formatDecimal(transaction.grossAmount),
                                    partialpayment = formatDecimal(transaction.partialPayment),
                                    transactionstatus = transaction.transactionStatus,
                                    discamount = formatDecimal(transaction.discountAmount),
                                    cashamount = formatDecimal(transaction.totalAmountPaid),
                                    custdiscamount = formatDecimal(transaction.customerDiscountAmount),
                                    totaldiscamount = formatDecimal(transaction.totalDiscountAmount),
                                    numberofitems = transaction.numberOfItems.toString(),
                                    currency = transaction.currency,
                                    createddate = formatDate(transaction.createdDate),
                                    priceoverride = transaction.priceOverride?.toInt(),
                                    comment = transaction.comment,
                                    taxinclinprice = formatDecimal(transaction.taxIncludedInPrice),
                                    netamountnotincltax = formatDecimal(transaction.vatableSales),
                                    window_number = transaction.windowNumber,
                                    cash = formatDecimal(transaction.cash),
                                    gcash = formatDecimal(transaction.gCash),
                                    paymaya = formatDecimal(transaction.payMaya),
                                    card = formatDecimal(transaction.card),
                                    loyaltycard = formatDecimal(transaction.loyaltyCard),
                                    charge = formatDecimal(transaction.charge),
                                    foodpanda = formatDecimal(transaction.foodpanda),
                                    grabfood = formatDecimal(transaction.grabfood),
                                    representation = formatDecimal(transaction.representation)
                                ),
                                transactionRecords = records.map { record ->
                                    TransactionRecordRequest(
                                        transactionid = record.transactionId,
                                        linenum = record.lineNum.toString(),
                                        receiptid = record.receiptId ?: "",
                                        itemid = record.itemId ?: "",
                                        storeKey = record.storeKey,
                                        storeSequence = record.storeSequence,
                                        itemname = record.name ?: "",
                                        itemgroup = record.itemGroup ?: "",
                                        price = formatDecimal(record.price),
                                        netprice = formatDecimal(record.netPrice),
                                        qty = record.quantity.toString(),
                                        discamount = formatDecimal(record.discountAmount),
                                        costamount = formatDecimal(record.costAmount),
                                        netamount = formatDecimal(record.netAmount),
                                        grossamount = formatDecimal(record.grossAmount),
                                        custaccount = record.customerAccount ?: "WALK-IN",
                                        store = transaction.store,
                                        priceoverride = record.priceOverride?.toInt() ?: 0,
                                        paymentmethod = transaction.paymentMethod,
                                        staff = record.staff ?: "Unknown Staff",
                                        linedscamount = formatDecimal(record.lineDiscountAmount ?: 0.0),
                                        linediscpct = formatDecimal(record.lineDiscountPercentage ?: 0.0),
                                        custdiscamount = formatDecimal(record.customerDiscountAmount ?: 0.0),
                                        unit = record.unit ?: "PCS",
                                        unitqty = formatDecimal(record.unitQuantity ?: record.quantity.toDouble()),
                                        unitprice = formatDecimal(record.unitPrice ?: record.price),
                                        taxamount = formatDecimal(record.taxAmount),
                                        createddate = formatDate(record.createdDate ?: Date()),
                                        remarks = record.remarks ?: "",
                                        taxinclinprice = formatDecimal(record.taxIncludedInPrice),
                                        description = record.description ?: "",
                                        discofferid = record.discountOfferId?.takeIf { it.isNotBlank() } ?:  "",
                                        inventbatchid = null,
                                        inventbatchexpdate = null,
                                        giftcard = null,
                                        returntransactionid = null,
                                        returnqty = null,
                                        creditmemonumber = null,
                                        returnlineid = null,
                                        priceunit = null,
                                        netamountnotincltax = null,
                                        storetaxgroup = null,
                                        currency = null,
                                        taxexempt = null
                                    )
                                }
                            )

                            // Sync without extensive error handling
                            val response = repository.syncTransaction(transaction, records)
                            response.onSuccess {
                                repository.transactionDao.updateSyncStatus(transaction.transactionId, true)
                            }
                        } catch (e: Exception) {
                            Log.e("AutoSync", "Error syncing transaction ${transaction.transactionId}", e)
                        }
                    }

                    // Reduced delay to make sync more frequent
                    delay(2000)
                } catch (e: Exception) {
                    Log.e("AutoSync", "Error during sync cycle", e)
                    delay(2000)
                }
            }
        }
    }
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
    }

    private fun formatDecimal(value: Double?): String {
        return try {
            String.format(Locale.US, "%.2f", value ?: 0.0)
        } catch (e: Exception) {
            "0.00"
        }
    }

    private fun formatDate(date: Date?): String {
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Manila")
            }.format(date ?: Date())
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date: $date", e)
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Manila")
            }.format(Date())
        }
    }



    fun syncTransaction(transactionId: String) {
        viewModelScope.launch {
            try {
                val summary = repository.getTransactionSummary(transactionId)
                val records = repository.getTransactionRecords(transactionId)
                val result = repository.syncTransaction(summary, records)

                result.onSuccess {
                    // Only mark as synced if truly successful
                    repository.transactionDao.updateSyncStatus(transactionId, true)
                }.onFailure { error ->
                    // If sync fails, mark as unsynced and log the error
                    repository.transactionDao.updateSyncStatus(transactionId, false)
                    Log.e("TransactionViewModel", "Sync failed for transaction $transactionId", error)
                }

                _syncStatus.value = result
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error during sync", e)
                _syncStatus.value = Result.failure(e)

                // Ensure the transaction is marked as unsynced in case of any exception
                repository.transactionDao.updateSyncStatus(transactionId, false)
            }
        }
    }
    fun syncUnsentTransactions() {
        viewModelScope.launch {
            try {
                repository.syncUnsentTransactions()
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error syncing unsent transactions", e)
            }
        }
    }

    class TransactionViewModelFactory(
        private val repository: TransactionRepository,
        private val numberSequenceRemoteRepository: NumberSequenceRemoteRepository // Changed from NumberSequenceRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TransactionViewModel(repository, numberSequenceRemoteRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
