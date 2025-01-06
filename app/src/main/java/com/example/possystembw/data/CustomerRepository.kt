package com.example.possystembw.data

import android.util.Log
import com.example.possystembw.DAO.CustomerApi
import com.example.possystembw.DAO.CustomerDao
import com.example.possystembw.database.Customer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Date

class CustomerRepository(private val customerApi: CustomerApi, private val customerDao: CustomerDao) {
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomersFlow()


    suspend fun getCustomerPurchaseHistory(customerName: String): List<CustomerPurchaseHistory> {
        return withContext(Dispatchers.IO) {
            customerDao.getCustomerPurchaseHistory(customerName)
        }
    }
    suspend fun getAllCustomers(): List<Customer> {
        return withContext(Dispatchers.IO) {
            try {
                val customers = customerApi.getAllCustomers()
                // Store in database
                customerDao.insertCustomers(customers)
                customers
            } catch (e: Exception) {
                Log.e("CustomerRepository", "Error fetching customers from API: ${e.message}")
                // Fallback to local data
                customerDao.getAllCustomers()
            }
        }
    }

    suspend fun searchCustomers(query: String): List<Customer> {
        return withContext(Dispatchers.IO) {
            try {
                // Try API first
                customerApi.searchCustomers(query)
            } catch (e: Exception) {
                Log.e("CustomerRepository", "Error searching customers from API: ${e.message}")
                // Fallback to local search
                customerDao.searchCustomers("%$query%")
            }
        }
    }
}


data class CustomerPurchaseHistory(
    val id: Int,
    val itemid: String,
    val itemName: String,
    val itemGroup: String,
    val price: Double,
    val foodpanda: Double,
    val grabfood: Double,
    val manilaprice: Double,
    val categoryId: Long,
    val purchaseCount: Int,
    val lastPurchaseDate: Date,
    val averageQuantity: Double
)
