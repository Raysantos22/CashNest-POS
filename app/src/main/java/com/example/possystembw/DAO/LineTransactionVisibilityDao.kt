package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.possystembw.database.LineTransactionVisibility
import kotlinx.coroutines.flow.Flow

@Dao
interface LineTransactionVisibilityDao {
    @Query("SELECT * FROM line_transaction_visibility WHERE itemId = :itemId LIMIT 1")
    suspend fun getVisibility(itemId: String): LineTransactionVisibility?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisibility(visibility: LineTransactionVisibility)

    @Query("UPDATE line_transaction_visibility SET is_hidden = :isHidden WHERE itemId = :itemId")
    suspend fun updateVisibility(itemId: String, isHidden: Boolean)

    @Query("SELECT * FROM line_transaction_visibility WHERE is_hidden = 1")
    fun getHiddenLineTransactions(): Flow<List<LineTransactionVisibility>>

    @Query("DELETE FROM line_transaction_visibility WHERE itemId = :itemId")
    suspend fun deleteVisibility(itemId: String)
}
