package com.example.possystembw.ui

import com.example.possystembw.DAO.ApiResponse
import com.example.possystembw.ui.ViewModel.DiscountRequest
import com.example.possystembw.ui.ViewModel.MixMatchRequest
import com.example.possystembw.ui.ViewModel.RequestItem
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// RequestApiService.kt
interface RequestApiService {
    @POST("api/requests/item")
    suspend fun submitItemRequest(@Body request: RequestItem): Response<ApiResponse>
    
    @POST("api/requests/mixmatch")
    suspend fun submitMixMatchRequest(@Body request: MixMatchRequest): Response<ApiResponse>
    
    @POST("api/requests/discount")
    suspend fun submitDiscountRequest(@Body request: DiscountRequest): Response<ApiResponse>
}