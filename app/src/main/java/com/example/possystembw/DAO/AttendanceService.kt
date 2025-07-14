package com.example.possystembw.DAO

import android.util.Log
import com.example.possystembw.RetrofitClient
import com.example.possystembw.database.AttendanceRecord
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

// Service class for easier API calls
class AttendanceService {

    suspend fun uploadAttendanceRecord(
        staffId: String,
        storeId: String,
        date: String,
        time: String,
        type: String,
        photoFile: java.io.File
    ): Result<AttendanceResponse> {
        return try {
            val staffIdBody = RequestBody.create(MultipartBody.FORM, staffId)
            val storeIdBody = RequestBody.create(MultipartBody.FORM, storeId)
            val dateBody = RequestBody.create(MultipartBody.FORM, date)
            val timeBody = RequestBody.create(MultipartBody.FORM, time)
            val typeBody = RequestBody.create(MultipartBody.FORM, type)

            val photoPart = MultipartBody.Part.createFormData(
                "photo",
                photoFile.name,
                RequestBody.create(MultipartBody.FORM, photoFile)
            )

            val response = RetrofitClient.attendanceApi.uploadAttendance(
                staffIdBody, storeIdBody, dateBody, timeBody, typeBody, photoPart
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Upload failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStoreAttendanceRecords(storeId: String): Result<List<ServerAttendanceRecord>> {
        return try {
            val response = RetrofitClient.attendanceApi.getStoreAttendanceRecords(storeId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to get attendance: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}