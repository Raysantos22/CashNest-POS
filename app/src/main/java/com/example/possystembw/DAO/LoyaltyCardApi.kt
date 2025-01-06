package com.example.possystembw.DAO

import com.example.possystembw.database.LoyaltyCard
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class LoyaltyCardResponse(
    @SerializedName("data")
    val data: LoyaltyCardData
)

data class LoyaltyCardData(
    @SerializedName("loyalty_cards")
    val loyaltyCards: List<LoyaltyCard>
)

interface LoyaltyCardApi {
    @GET("api/loyalty-cards")
    suspend fun getLoyaltyCards(): Response<LoyaltyCardResponse>


    @POST("api/updatepoints/updatepoints/{cardNumber}/{points}")
    suspend fun updatePoints(
        @Path("cardNumber") cardNumber: String,
        @Path("points") points: Int
    ): Response<UpdatePointsResponse>
}

data class UpdatePointsResponse(
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: UpdatePointsData
)

data class UpdatePointsData(
    @SerializedName("card_number") val cardNumber: String,
    @SerializedName("old_points") val oldPoints: Int,
    @SerializedName("new_points") val newPoints: String,
    @SerializedName("updated_at") val updatedAt: String
)