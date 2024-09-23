package com.example.possystembw.DAO

import com.example.possystembw.database.AR
import com.google.android.gms.common.api.Response
import retrofit2.http.GET


interface ARApi {
    @GET("api/ar-types")
    suspend fun getARTypes(): List<AR>
}