package com.example.possystembw.DAO

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

data class StaffResponse(
    @SerializedName("name") val name: String?,
    @SerializedName("passcode") val passcode: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("role") val role: String?,
    @SerializedName("storeid") val storeid: String?  // Note: API returns "storeid" not "storeId"
)

interface StaffApi {
    @GET("api/getStaffData/{storeId}")
    suspend fun getStaffData(
        @Path("storeId") storeId: String
    ): Response<List<StaffResponse>>
}