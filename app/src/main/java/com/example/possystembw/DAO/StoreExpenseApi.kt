package com.example.possystembw.DAO

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

import retrofit2.Response
import retrofit2.http.Query

interface StoreExpenseApi {
    @GET("api/store-expenses")
    suspend fun getStoreExpenses(@Query("store_id") storeId: String): Response<StoreExpenseListResponse>

    @POST("api/store-expenses")
    suspend fun postStoreExpense(@Body expense: StoreExpenseRequest): Response<StoreExpenseResponse>
}
data class StoreExpenseListResponse(
    val storeExpense: List<StoreExpenseResponse> // Changed from 'data' to 'storeExpense'
)
data class StoreExpensesData(
    val store_expenses: List<StoreExpenseResponse>
)
data class StoreExpenseResponse(
    val id: Long? = null,
    val name: String = "",
    val expense_type: String? = null,
    val expenseType: String? = null,
    val amount: String = "0.0",
    val received_by: String? = null,
    val receivedBy: String? = null,
    val approved_by: String? = null,
    val approvedBy: String? = null,
    val effect_date: String? = null,
    val effectDate: String? = null,
    val store_id: String? = null,
    val storeId: String? = null,
    val sync_status: Int? = null
)

data class StoreExpenseRequest(
    val id: Long? = null,
    val name: String,
    val expense_type: String,
    val amount: String, // Changed to String
    val received_by: String,
    val approved_by: String,
    val effect_date: String,
    val store_id: String,
    val timestamp: Long
)
