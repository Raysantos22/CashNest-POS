package com.example.possystembw.DAO// DiscountApiService.kt
import retrofit2.http.GET
import retrofit2.Response


interface DiscountApiService {
    @GET("api/discounts/get-all-discounts")
    suspend fun getAllDiscounts(): Response<List<DiscountResponse>>
}

data class DiscountResponse(
    val id: Int?,
    val DISCOFFERNAME: String?,
    val PARAMETER: Int?,
    val DISCOUNTTYPE: String?
)

