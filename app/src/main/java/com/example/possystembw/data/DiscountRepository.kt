package com.example.possystembw.data

import android.util.Log
import com.example.possystembw.DAO.DiscountApiService
import com.example.possystembw.DAO.DiscountDao
import com.example.possystembw.DAO.DiscountResponse
import com.example.possystembw.database.Discount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


class DiscountRepository(
    private val discountApiService: DiscountApiService,
    private val discountDao: DiscountDao
) {
    fun getAllDiscounts(): Flow<Result<List<Discount>>> = flow {
        try {
            // Always try to fetch from API first
            Log.d("DiscountRepository", "🔄 Fetching discounts from API...")
            val response = discountApiService.getAllDiscounts()

            if (response.isSuccessful) {
                val apiDiscounts = response.body()?.map { it.toDiscount() } ?: emptyList()
                Log.d("DiscountRepository", "✅ API returned ${apiDiscounts.size} discounts")

                // Clear existing data and insert fresh data from API
                discountDao.deleteAll()
                discountDao.insertAll(apiDiscounts)

                emit(Result.success(apiDiscounts))
            } else {
                Log.e("DiscountRepository", "❌ API Error: ${response.code()} - ${response.message()}")

                // Fallback to local data
                val localDiscounts = discountDao.getAllDiscounts()
                Log.d("DiscountRepository", "📱 Using ${localDiscounts.size} local discounts as fallback")
                emit(Result.success(localDiscounts))
            }
        } catch (e: Exception) {
            Log.e("DiscountRepository", "❌ Exception during API call", e)

            // Fallback to local data
            try {
                val localDiscounts = discountDao.getAllDiscounts()
                Log.d("DiscountRepository", "📱 Exception fallback: Using ${localDiscounts.size} local discounts")
                emit(Result.success(localDiscounts))
            } catch (localException: Exception) {
                Log.e("DiscountRepository", "❌ Local data also failed", localException)
                emit(Result.failure(e))
            }
        }
    }

    suspend fun getLocalDiscounts(): List<Discount> {
        return discountDao.getAllDiscounts()
    }

    // Fixed toDiscount extension function - made it public
    fun DiscountResponse.toDiscount(): Discount {
        return Discount(
            id = id ?: 0,
            DISCOFFERNAME = DISCOFFERNAME.orEmpty(),
            PARAMETER = PARAMETER ?: 0,
            DISCOUNTTYPE = DISCOUNTTYPE.orEmpty(),
            GRABFOOD_PARAMETER = GRABFOOD_PARAMETER,
            FOODPANDA_PARAMETER = FOODPANDA_PARAMETER,
            MANILAPRICE_PARAMETER = MANILAPRICE_PARAMETER,
            MALLPRICE_PARAMETER = MALLPRICE_PARAMETER,
            GRABFOODMALL_PARAMETER = GRABFOODMALL_PARAMETER,
            FOODPANDAMALL_PARAMETER = FOODPANDAMALL_PARAMETER
        )
    }

    // Method to force refresh from API
    suspend fun refreshDiscounts(): Result<List<Discount>> {
        return try {
            Log.d("DiscountRepository", "🔄 Force refreshing discounts from API...")
            val response = discountApiService.getAllDiscounts()

            if (response.isSuccessful) {
                val apiDiscounts = response.body()?.map { it.toDiscount() } ?: emptyList()
                Log.d("DiscountRepository", "✅ Force refresh: API returned ${apiDiscounts.size} discounts")

                // Clear and insert fresh data
                discountDao.deleteAll()
                discountDao.insertAll(apiDiscounts)

                Result.success(apiDiscounts)
            } else {
                Log.e("DiscountRepository", "❌ Force refresh API Error: ${response.code()}")
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("DiscountRepository", "❌ Force refresh exception", e)
            Result.failure(e)
        }
    }
}
//    private fun DiscountResponse.toDiscount(): Discount {
//        return Discount(
//            id = id ?: 0,
//            DISCOFFERNAME = DISCOFFERNAME.orEmpty(),
//            PARAMETER = PARAMETER ?: 0,
//            DISCOUNTTYPE = DISCOUNTTYPE.orEmpty()
//        )
//    }
//}