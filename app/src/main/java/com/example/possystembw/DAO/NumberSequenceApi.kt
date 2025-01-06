package com.example.possystembw.DAO

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface NumberSequenceApi {
    @GET("api/getsequence/{storeId}")
    suspend fun getNumberSequence(@Path("storeId") storeId: String): Response<Map<String, List<NumberSequenceValue>>>

    @POST("api/getsequence/{storeId}/{nextRec}")
    suspend fun updateNumberSequence(
    @Path("storeId") storeId: String,
    @Path("nextRec") nextRec: Int
): Response<UpdateSequenceResponse>
}

// Add response data class
data class UpdateSequenceResponse(
    val success: Boolean,
    val message: String,
    val data: NumberSequenceValue?
)
// Update the NumberSequenceValue data class
data class NumberSequenceValue(
    @SerializedName("NUMBERSEQUENCE") val numberSequence: String,
    @SerializedName("NEXTREC") val nextRec: Long,
    @SerializedName("CARTNEXTREC") val cartNextRec: Long,
    @SerializedName("BUNDLENEXTREC") val bundleNextRec: Long,
    @SerializedName("DISCOUNTNEXTREC") val discountNextRec: Long,
    @SerializedName("STOREID") val storeId: String,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("wasterec") val wasteRec: Long,
    @SerializedName("TONEXTREC") val toNextRec: Long,
    @SerializedName("STOCKNEXTREC") val stockNextRec: Long
)
