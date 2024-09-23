package com.example.possystembw.DAO

import com.example.possystembw.database.Category
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path



interface CategoryApi {
    @GET("api/categories/get-all-categories")
    suspend fun getCategories(): Response<List<Category>>
}
data class Category(
    val groupId: Int,
    val name: String
)