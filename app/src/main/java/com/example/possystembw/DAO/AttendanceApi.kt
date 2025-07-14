package com.example.possystembw.DAO

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import java.io.File

// Updated API Interface using your existing endpoint
interface AttendanceApi {

    // Existing POST endpoint for uploading attendance
    @Multipart
    @POST("api/attendance")
    suspend fun uploadAttendance(
        @Part("staffId") staffId: RequestBody,
        @Part("storeId") storeId: RequestBody,
        @Part("date") date: RequestBody,
        @Part("time") time: RequestBody,
        @Part("type") type: RequestBody,
        @Part photo: MultipartBody.Part
    ): Response<AttendanceResponse>

    // GET endpoint using your existing API structure
    @GET("api/api-attendance/store/{storeId}")
    suspend fun getStoreAttendanceRecords(
        @Path("storeId") storeId: String
    ): Response<StoreAttendanceResponse>
}

// Response data classes matching your API response structure
data class AttendanceGetResponse(
    val success: Boolean,
    val message: String,
    val data: List<AttendanceServerRecord>,
    val count: Int,
    val store_id: String
)

// Data class matching your server response structure
data class AttendanceServerRecord(
    val id: Int,
    val staffId: String,
    val storeId: String,
    val date: String,
    val timeIn: String?,
    val timeInPhoto: String?,
    val breakIn: String?,
    val breakInPhoto: String?,
    val breakOut: String?,
    val breakOutPhoto: String?,
    val timeOut: String?,
    val timeOutPhoto: String?,
    val status: String,
    val created_at: String,
    val updated_at: String
)

// Keep existing data classes for upload functionality
data class AttendanceResponse(
    val success: Boolean,
    val message: String,
    val data: AttendanceData? = null,
    val errors: Map<String, List<String>>? = null
)
data class StoreAttendanceResponse(
    val success: Boolean,
    val message: String,
    val data: List<ServerAttendanceRecord>,
    val count: Int,
    val store_id: String
)

data class ServerAttendanceRecord(
    val id: Int,
    val staffId: String,
    val storeId: String,
    val date: String,
    val timeIn: String?,
    val timeInPhoto: String?,
    val breakIn: String?,
    val breakInPhoto: String?,
    val breakOut: String?,
    val breakOutPhoto: String?,
    val timeOut: String?,
    val timeOutPhoto: String?,
    val status: String,
    val created_at: String,
    val updated_at: String
)

// Extension function to convert server record to local record

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

fun ServerAttendanceRecord.toLocalAttendanceRecord(): com.example.possystembw.database.AttendanceRecord {
    return com.example.possystembw.database.AttendanceRecord(
        id = this.id.toLong(),
        staffId = this.staffId,
        storeId = this.storeId,
        date = this.date,
        timeIn = this.timeIn ?: "",
        timeInPhoto = this.timeInPhoto ?: "",
        breakIn = this.breakIn,
        breakInPhoto = this.breakInPhoto,
        breakOut = this.breakOut,
        breakOutPhoto = this.breakOutPhoto,
        timeOut = this.timeOut,
        timeOutPhoto = this.timeOutPhoto,
        status = this.status
    )
}