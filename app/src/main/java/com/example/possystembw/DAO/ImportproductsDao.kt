package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Importproducts

@Dao
interface ImportproductsDao {
    @Insert
    suspend fun insert(importProduct: Importproducts)

    @Query("SELECT * FROM importproducts")
    suspend fun getAll(): List<Importproducts>

    @Query("SELECT * FROM importproducts WHERE id = :id")
    suspend fun getById(id: Int): Importproducts?

    @Query("DELETE FROM importproducts")
    suspend fun deleteAll()
}
