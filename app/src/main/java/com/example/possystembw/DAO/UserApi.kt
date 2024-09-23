package com.example.possystembw.DAO


import com.example.possystembw.database.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


// UserApi interface
interface UserApi {
    @GET("api/users")
    suspend fun getAllUsers(): Response<List<User>>

    @POST("api/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<User>
}

data class LoginRequest(
    val email: String,
    val password: String
)