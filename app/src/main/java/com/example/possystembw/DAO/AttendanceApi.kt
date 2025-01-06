package com.example.possystembw.DAO

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.http.*
import java.io.File

// API Interface
interface AttendanceApi {
    @Multipart
    @POST("/api/attendance")
    suspend fun uploadAttendance(
        @Part("staffId") staffId: RequestBody,
        @Part("storeId") storeId: RequestBody,
        @Part("date") date: RequestBody,
        @Part("time") time: RequestBody,
        @Part("type") type: RequestBody,
        @Part photo: MultipartBody.Part
    ): Response<AttendanceResponse>
}
data class AttendanceRequest(
    val staffId: String,
    val storeId: String,
    val date: String,
    val time: String,
    val type: String,
    val photo: String // Base64 encoded photo
)


// Response data class
data class AttendanceResponse(
    val success: Boolean,
    val message: String,
    val data: Any? = null
)
