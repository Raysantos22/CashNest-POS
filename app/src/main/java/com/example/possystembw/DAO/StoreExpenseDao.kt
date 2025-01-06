package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.possystembw.database.StoreExpense
import kotlinx.coroutines.flow.Flow



@Dao
interface StoreExpenseDao {
    @Query("SELECT * FROM store_expenses WHERE storeId = :storeId ORDER BY timestamp DESC")
    fun getAllExpenses(storeId: String): Flow<List<StoreExpense>>

    @Query("SELECT * FROM store_expenses WHERE syncStatus = 0")
    fun getUnsyncedExpenses(): Flow<List<StoreExpense>>

    @Insert
    suspend fun insertExpense(expense: StoreExpense): Long

    @Update
    suspend fun updateExpense(expense: StoreExpense)

    @Delete
    suspend fun deleteExpense(expense: StoreExpense)

    @Query("UPDATE store_expenses SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateExpense(expense: StoreExpense): Long

    @Query("DELETE FROM store_expenses WHERE storeId = :storeId")
    suspend fun deleteExpensesByStoreId(storeId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpense(expense: StoreExpense)
}
