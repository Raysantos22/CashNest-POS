package com.example.possystembw.DAO

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import com.google.gson.annotations.SerializedName
import retrofit2.http.POST
import kotlin.Result
import kotlin.collections.List

interface StockCountingApi {
    @GET("api/stock-counting/{storeId}")
    suspend fun getStockCounting(@Path("storeId") storeId: String): Response<StockCountingResponse>

    @POST("api/stock-counting/{storeId}/{posted}/{journalId}")
    suspend fun postStockCounting(
        @Path("storeId") storeId: String,
        @Path("posted") posted: String,  // Changed to String
        @Path("journalId") journalId: String
    ): Response<Unit>

}

data class StockCountingResponse(
    val success: Boolean,
    val data: List<StockCountingData>
)

data class StockCountingData(
    @SerializedName("journalid") val journalId: Long,
    @SerializedName("storeid") val storeId: String,
    @SerializedName("description") val description: String,
    @SerializedName("qty") val quantity: String,
    @SerializedName("amount") val amount: String,
    @SerializedName("posted") val posted: Int,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("journaltype") val journalType: Int,
    @SerializedName("createddatetime") val createdDateTime: String
)
