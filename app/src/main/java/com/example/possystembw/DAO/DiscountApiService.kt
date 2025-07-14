package com.example.possystembw.DAO// DiscountApiService.kt
import retrofit2.http.GET
import retrofit2.Response


interface DiscountApiService {
    @GET("api/discounts/get-all-discounts")
    suspend fun getAllDiscounts(): Response<List<DiscountResponse>>
}


//data class DiscountResponse(
//    val id: Int?,
//    val DISCOFFERNAME: String?,
//    val PARAMETER: Int?,
//    val DISCOUNTTYPE: String?,
//    val GRABFOOD_PARAMETER: Int?,
//    val FOODPANDA_PARAMETER: Int?,
//    val MANILAPRICE_PARAMETER: Int?
//)

data class DiscountResponse(
    val id: Int?,
    val DISCOFFERNAME: String?,
    val PARAMETER: Int?,
    val DISCOUNTTYPE: String?,


    val GRABFOOD_PARAMETER: Int?,
    val FOODPANDA_PARAMETER: Int?,
    val MANILAPRICE_PARAMETER: Int?,

    // NEW PARAMETER FIELDS
    val MALLPRICE_PARAMETER: Int?,
    val GRABFOODMALL_PARAMETER: Int?,
    val FOODPANDAMALL_PARAMETER: Int?
)



//data class DiscountResponse(
//    val id: Int?,
//    val DISCOFFERNAME: String?,
//    val PARAMETER: Int?,
//    val DISCOUNTTYPE: String?
//)
