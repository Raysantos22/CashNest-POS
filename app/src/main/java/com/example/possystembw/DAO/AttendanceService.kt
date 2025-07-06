package com.example.possystembw.DAO

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class AttendanceService(private val api: AttendanceApi) {
    companion object {
        private const val TAG = "AttendanceService"
    }

    suspend fun uploadAttendanceRecord(
        staffId: String,
        storeId: String,
        date: String,
        time: String,
        type: String,
        photoFile: File
    ): Result<AttendanceResponse> {
        return try {
            Log.d(TAG, "Starting attendance upload...")
            Log.d(TAG, "Parameters: staffId=$staffId, storeId=$storeId, date=$date, time=$time, type=$type")
            Log.d(TAG, "Photo file: ${photoFile.absolutePath}, exists=${photoFile.exists()}, size=${photoFile.length()}")

            // Validate inputs
            if (staffId.isBlank()) throw IllegalArgumentException("Staff ID cannot be blank")
            if (storeId.isBlank()) throw IllegalArgumentException("Store ID cannot be blank")
            if (date.isBlank()) throw IllegalArgumentException("Date cannot be blank")
            if (time.isBlank()) throw IllegalArgumentException("Time cannot be blank")
            if (type.isBlank()) throw IllegalArgumentException("Type cannot be blank")
            if (!photoFile.exists()) throw IllegalArgumentException("Photo file does not exist")
            if (photoFile.length() == 0L) throw IllegalArgumentException("Photo file is empty")

            // Create RequestBody instances for form fields
            val staffIdBody = staffId.toRequestBody("text/plain".toMediaTypeOrNull())
            val storeIdBody = storeId.toRequestBody("text/plain".toMediaTypeOrNull())
            val dateBody = date.toRequestBody("text/plain".toMediaTypeOrNull())
            val timeBody = time.toRequestBody("text/plain".toMediaTypeOrNull())
            val typeBody = type.toRequestBody("text/plain".toMediaTypeOrNull())

            // Create photo part with proper MIME type detection
            val mimeType = when (photoFile.extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                else -> "image/jpeg" // Default fallback
            }

            val photoRequestBody = photoFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val photoPart = MultipartBody.Part.createFormData(
                "photo",
                photoFile.name,
                photoRequestBody
            )

            Log.d(TAG, "Making API call...")
            Log.d(TAG, "Photo MIME type: $mimeType")

            // Make the API call
            val response = api.uploadAttendance(
                staffId = staffIdBody,
                storeId = storeIdBody,
                date = dateBody,
                time = timeBody,
                type = typeBody,
                photo = photoPart
            )

            Log.d(TAG, "API call completed. Response code: ${response.code()}")

            if (response.isSuccessful) {
                val responseBody = response.body()
                Log.d(TAG, "Upload successful: $responseBody")
                Result.success(responseBody ?: AttendanceResponse(false, "No response body"))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Upload failed with code ${response.code()}: $errorBody")
                Result.failure(Exception("API call failed: ${response.code()} - $errorBody"))
            }

        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid input: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Upload exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}