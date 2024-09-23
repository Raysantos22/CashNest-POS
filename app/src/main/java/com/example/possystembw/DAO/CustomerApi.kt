package com.example.possystembw.DAO

import com.example.possystembw.database.Customer
import retrofit2.http.GET
import retrofit2.http.Query

interface CustomerApi {
    @GET("api/customers")
    suspend fun getAllCustomers(): List<Customer>

    @GET("api/customers/search")
    suspend fun searchCustomers(@Query("query") query: String): List<Customer>
}
