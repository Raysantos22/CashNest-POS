package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.possystembw.database.Cashfund

@Dao
interface CashfundDao {
   /* @Insert
    suspend fun insert(cashFund: Cashfund)*/

    @Update
    suspend fun update(cashFund: Cashfund)

    @Query("SELECT * FROM cash_fund")
    suspend fun getAllCashFunds(): List<Cashfund>

    @Query("SELECT * FROM cash_fund WHERE id = :id")
    suspend fun getCashFundById(id: Int): Cashfund?

    @Query("DELETE FROM cash_fund")
    suspend fun deleteAll()

    // Optional: Delete by date if you want more specific control
    @Query("DELETE FROM cash_fund WHERE date = :date")
    suspend fun deleteByDate(date: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cashFund: Cashfund)

    @Query("SELECT * FROM cash_fund WHERE date = :currentDate LIMIT 1")
    suspend fun getCashFundByDate(currentDate: String): Cashfund?
}
