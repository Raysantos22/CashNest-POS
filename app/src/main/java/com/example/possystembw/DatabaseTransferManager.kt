package com.example.possystembw

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.possystembw.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutoDatabaseTransferManager(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope
) {
    private val TAG = "AutoDatabaseTransferManager"
    private var isTransferring = false
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available")
            lifecycleScope.launch {
                val result = transferDatabasesToApi()
                Log.d(TAG, "Database transfer result: $result")
            }
        }
    }

    fun startMonitoringConnectivity() {
        Log.d(TAG, "Starting to monitor connectivity")
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    fun stopMonitoringConnectivity() {
        Log.d(TAG, "Stopping connectivity monitoring")
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private suspend fun transferDatabasesToApi(): Boolean {
        if (isTransferring) {
            Log.d(TAG, "Transfer already in progress, skipping")
            return false
        }
        isTransferring = true
        return withContext(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(context)
                val productDao = database.productDao()
                val cartItemDao = database.cartDao()
                val transactionDao = database.transactionDao()
                val api = RetrofitClient.instance

                // Transfer products
                val products = productDao.getAllProducts().first()
                Log.d(TAG, "Uploading ${products.size} products")
                val productsResponse = api.uploadProducts(products)
                if (!productsResponse.isSuccessful) {
                    Log.e(
                        TAG,
                        "Failed to upload products. Error: ${
                            productsResponse.errorBody()?.string()
                        }"
                    )
                    return@withContext false
                }
                Log.d(TAG, "Products uploaded successfully: ${productsResponse.body()?.message}")

                // Transfer cart items
                val cartItems = cartItemDao.getAllCartItems().first()
                Log.d(TAG, "Uploading ${cartItems.size} cart items")
                val cartItemsResponse = api.uploadCartItems(cartItems)
                if (!cartItemsResponse.isSuccessful) {
                    Log.e(
                        TAG,
                        "Failed to upload cart items. Error: ${
                            cartItemsResponse.errorBody()?.string()
                        }"
                    )
                    return@withContext false
                }
                Log.d(TAG, "Cart items uploaded successfully: ${cartItemsResponse.body()?.message}")

                // Transfer transactions
                val transactions = transactionDao.getAllTransactions().first()
                Log.d(TAG, "Uploading ${transactions.size} transactions")
                val transactionsResponse = api.uploadTransactions(transactions)
                if (!transactionsResponse.isSuccessful) {
                    Log.e(
                        TAG,
                        "Failed to upload transactions. Error: ${
                            transactionsResponse.errorBody()?.string()
                        }"
                    )
                    return@withContext false
                }
                Log.d(
                    TAG,
                    "Transactions uploaded successfully: ${transactionsResponse.body()?.message}"
                )

                // After uploading, fetch data from MySQL to Room
                transferDataFromMySQLToRoom()

                Log.i(TAG, "All databases successfully synced")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error transferring databases: ${e.message}", e)
                false
            } finally {
                isTransferring = false
            }
        }
    }

    private suspend fun transferDataFromMySQLToRoom() {
        withContext(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(context)

            try {
                // Fetch and store products
                val productResponse = RetrofitClient.instance.getProducts()
                if (productResponse.isSuccessful) {
                    val products = productResponse.body() ?: emptyList()
                    Log.d(TAG, "Fetched ${products.size} products from MySQL")
                    database.productDao().insertAll(products)
                    Log.d(TAG, "Products transferred successfully from MySQL to Room")
                } else {
                    Log.e(TAG, "Failed to fetch products: ${productResponse.errorBody()?.string()}")
                }
                // Fetch and store cart items
                val cartResponse = RetrofitClient.instance.getCartItems()
                if (cartResponse.isSuccessful) {
                    val cartItems = cartResponse.body() ?: emptyList()
                    Log.d(TAG, "Fetched ${cartItems.size} cart items from MySQL")
                    database.cartDao().insertAll(cartItems)
                    Log.d(TAG, "Cart items transferred successfully from MySQL to Room")
                } else {
                    Log.e(TAG, "Failed to fetch cart items: ${cartResponse.errorBody()?.string()}")
                }

                // Fetch and store transactions
                val transactionResponse = RetrofitClient.instance.getTransactions()
                if (transactionResponse.isSuccessful) {
                    val transactions = transactionResponse.body() ?: emptyList()
                    Log.d(TAG, "Fetched ${transactions.size} transactions from MySQL")
                    database.transactionDao().insertAll(transactions)
                    Log.d(TAG, "Transactions transferred successfully from MySQL to Room")
                } else {
                    Log.e(TAG, "Failed to fetch transactions: ${transactionResponse.errorBody()?.string()}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error transferring data from MySQL to Room: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}