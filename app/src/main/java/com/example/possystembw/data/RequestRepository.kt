package com.example.possystembw.data

import com.example.possystembw.DAO.ApiResponse
import com.example.possystembw.ui.RequestApiService
import com.example.possystembw.ui.ViewModel.DiscountRequest
import com.example.possystembw.ui.ViewModel.MixMatchRequest
import com.example.possystembw.ui.ViewModel.RequestItem

// RequestRepository.kt
class RequestRepository(
    private val requestApiService: RequestApiService
) {
    suspend fun submitItemRequest(request: RequestItem): Result<ApiResponse> {
        return try {
            val response = requestApiService.submitItemRequest(request)
            if (response.isSuccessful) {
                Result.success(response.body() ?: ApiResponse("Request submitted successfully", null, 0, "", 0.0, ""))
            } else {
                Result.failure(Exception("Failed to submit request: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun submitMixMatchRequest(request: MixMatchRequest): Result<ApiResponse> {
        return try {
            val response = requestApiService.submitMixMatchRequest(request)
            if (response.isSuccessful) {
                Result.success(response.body() ?: ApiResponse("Request submitted successfully", null, 0, "", 0.0, ""))
            } else {
                Result.failure(Exception("Failed to submit request: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun submitDiscountRequest(request: DiscountRequest): Result<ApiResponse> {
        return try {
            val response = requestApiService.submitDiscountRequest(request)
            if (response.isSuccessful) {
                Result.success(response.body() ?: ApiResponse("Request submitted successfully", null, 0, "", 0.0, ""))
            } else {
                Result.failure(Exception("Failed to submit request: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}