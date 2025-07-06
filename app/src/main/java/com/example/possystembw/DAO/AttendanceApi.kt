package com.example.possystembw.DAO

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

// API Interface
interface AttendanceApi {
    @Multipart
    @POST("api/attendance") // Fixed: Added /api/ prefix
    suspend fun uploadAttendance(
        @Part("staffId") staffId: RequestBody,
        @Part("storeId") storeId: RequestBody,
        @Part("date") date: RequestBody,
        @Part("time") time: RequestBody,
        @Part("type") type: RequestBody,
        @Part photo: MultipartBody.Part
    ): Response<AttendanceResponse>

    @GET("api/attendance") // Fixed: Added /api/ prefix
    suspend fun getAttendanceRecords(): Response<List<AttendanceRecord>>

    @GET("api/attendance/{id}") // Fixed: Added /api/ prefix
    suspend fun getAttendanceRecord(@Path("id") id: String): Response<AttendanceRecord>

    @PUT("api/attendance/{id}") // Fixed: Added /api/ prefix
    suspend fun updateAttendanceRecord(
        @Path("id") id: String,
        @Body attendance: AttendanceRecord
    ): Response<AttendanceRecord>

    @DELETE("api/attendance/{id}") // Fixed: Added /api/ prefix
    suspend fun deleteAttendanceRecord(@Path("id") id: String): Response<Unit>
}

// Request data class (for non-multipart requests if needed)
data class AttendanceRequest(
    val staffId: String,
    val storeId: String,
    val date: String,
    val time: String,
    val type: String,
    val photo: String? = null // Base64 encoded photo or file path
)

// Response data class
data class AttendanceResponse(
    val success: Boolean,
    val message: String,
    val data: AttendanceData? = null,
    val errors: Map<String, List<String>>? = null
)

// Data class for the attendance data in response
data class AttendanceData(
    val id: Int? = null,
    val staffId: String,
    val storeId: String,
    val date: String,
    val type: String,
    val time: String,
    val photo_path: String? = null,
    val status: String? = null
)

// AttendanceRecord data class to match your database model
data class AttendanceRecord(
    val id: Int? = null,
    val staffId: String,
    val storeId: String,
    val date: String,
    val timeIn: String? = null,
    val timeInPhoto: String? = null,
    val breakIn: String? = null,
    val breakInPhoto: String? = null,
    val breakOut: String? = null,
    val breakOutPhoto: String? = null,
    val timeOut: String? = null,
    val timeOutPhoto: String? = null,
    val status: String? = null
)