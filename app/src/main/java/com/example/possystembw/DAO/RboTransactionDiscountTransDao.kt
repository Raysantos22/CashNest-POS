package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.possystembw.database.RboTransactionDiscountTrans

@Dao
interface RboTransactionDiscountTransDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionDiscount(transDiscount: RboTransactionDiscountTrans)

    @Update
    suspend fun updateTransactionDiscount(transDiscount: RboTransactionDiscountTrans)

    @Query("SELECT * FROM rbotransactiondiscounttrans WHERE TRANSACTIONID = :transactionId")
    suspend fun getTransactionDiscountsByTransactionId(transactionId: String): List<RboTransactionDiscountTrans>

    @Query("DELETE FROM rbotransactiondiscounttrans WHERE TRANSACTIONID = :transactionId")
    suspend fun deleteTransactionDiscounts(transactionId: String)
}