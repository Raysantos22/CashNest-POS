package com.example.possystembw.DAO

import com.example.possystembw.database.Window
import retrofit2.http.GET

interface WindowApiService {
    @GET("api/windows/get-all-windows")
    suspend fun getAllWindows(): List<Window>
}
