package com.example.possystembw.data

import android.util.Log
import com.example.possystembw.DAO.CustomerApi
import com.example.possystembw.DAO.CustomerDao
import com.example.possystembw.database.Customer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CustomerRepository(private val customerApi: CustomerApi, private val customerDao: CustomerDao) {
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomersFlow()

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


