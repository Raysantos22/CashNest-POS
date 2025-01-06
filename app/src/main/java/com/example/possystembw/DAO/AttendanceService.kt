package com.example.possystembw.DAO

import android.util.Log
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class AttendanceService(private val api: AttendanceApi) {
    suspend fun uploadAttendanceRecord(
        staffId: String,
        storeId: String,
        date: String,
        time: String,
        type: String,
        photoFile: File
    ): Result<AttendanceResponse> {
        return try {
            // Create RequestBody instances with explicit content type
            val staffIdBody = staffId.toRequestBody("text/plain".toMediaTypeOrNull())
            val storeIdBody = storeId.toRequestBody("text/plain".toMediaTypeOrNull())
            val dateBody = date.toRequestBody("text/plain".toMediaTypeOrNull())
            val timeBody = time.toRequestBody("text/plain".toMediaTypeOrNull())
            val typeBody = type.toRequestBody("text/plain".toMediaTypeOrNull())

            // Add logging to see what's being sent
            Log.d("AttendanceService", "Sending attendance data: staffId=$staffId, storeId=$storeId, date=$date, time=$time, type=$type")

            // Create photo part
            val photoRequestBody = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val photoPart = MultipartBody.Part.createFormData("photo", photoFile.name, photoRequestBody)

            // Make API call with the properly formatted bodies
            val response = api.uploadAttendance(
                staffId = staffIdBody,
                storeId = storeIdBody,
                date = dateBody,
                time = timeBody,
                type = typeBody,
                photo = photoPart
            )

            if (response.isSuccessful) {
                Result.success(response.body() ?: AttendanceResponse(false, "No response body"))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("AttendanceService", "Error response: $errorBody")
                Result.failure(Exception("API call failed: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("AttendanceService", "Exception during upload", e)
            Result.failure(e)
        }
    }
}