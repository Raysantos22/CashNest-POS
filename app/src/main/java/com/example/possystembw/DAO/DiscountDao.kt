package com.example.possystembw.DAO

import androidx.room.*
import com.example.possystembw.database.Discount
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscountDao {
    @Query("SELECT * FROM discounts")
    suspend fun getAllDiscounts(): List<Discount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(discounts: List<Discount>)

    @Query("SELECT * FROM discounts WHERE DISCOFFERNAME = :name LIMIT 1")
    suspend fun getDiscountByName(name: String): Discount?

    @Update
    suspend fun update(discount: Discount)

    @Delete
    suspend fun delete(discount: Discount)

    @Query("DELETE FROM discounts")
    suspend fun deleteAll()

    // Existing platform-specific queries
    @Query("SELECT * FROM discounts WHERE GRABFOOD_PARAMETER IS NOT NULL")
    suspend fun getGrabFoodDiscounts(): List<Discount>

    @Query("SELECT * FROM discounts WHERE FOODPANDA_PARAMETER IS NOT NULL")
    suspend fun getFoodPandaDiscounts(): List<Discount>

    @Query("SELECT * FROM discounts WHERE MANILAPRICE_PARAMETER IS NOT NULL")
    suspend fun getManilaPriceDiscounts(): List<Discount>

    // NEW PLATFORM-SPECIFIC QUERIES
    @Query("SELECT * FROM discounts WHERE MALLPRICE_PARAMETER IS NOT NULL")
    suspend fun getMallPriceDiscounts(): List<Discount>

    @Query("SELECT * FROM discounts WHERE GRABFOODMALL_PARAMETER IS NOT NULL")
    suspend fun getGrabFoodMallDiscounts(): List<Discount>

    @Query("SELECT * FROM discounts WHERE FOODPANDAMALL_PARAMETER IS NOT NULL")
    suspend fun getFoodPandaMallDiscounts(): List<Discount>
}