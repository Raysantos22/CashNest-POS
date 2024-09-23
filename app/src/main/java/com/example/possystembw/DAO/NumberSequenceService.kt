package com.example.possystembw.DAO

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface NumberSequenceService {
    @GET("api/numbersequencevalues")
    suspend fun getNumberSequence(): Response<NumberSequenceResponse>

    @POST("api/numbersequencevalues")
    suspend fun updateNumberSequence(@Body request: NumberSequenceRequest): Response<NumberSequenceResponse>
}

// Data classes for API requests/responses
data class NumberSequenceResponse(
    @SerializedName("nubersequencevalues")
    val numberSequenceValues: List<NumberSequenceValue>
)

data class NumberSequenceValue(
    @SerializedName("NUMBERSEQUENCE") val numberSequence: String,
    @SerializedName("NEXTREC") val nextRec: Int,
    @SerializedName("CARTNEXTREC") val cartNextRec: Int,
    @SerializedName("BUNDLENEXTREC") val bundleNextRec: Int,
    @SerializedName("DISCOUNTNEXTREC") val discountNextRec: Int,
    @SerializedName("STOREID") val storeId: String,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("wasterec") val wasteRec: Int,
    @SerializedName("TONEXTREC") val toNextRec: Int,
    @SerializedName("STOCKNEXTREC") val stockNextRec: Int
)

data class NumberSequenceRequest(
    @SerializedName("nubersequencevalues")
    val numberSequenceValues: List<NumberSequenceValue>
)
