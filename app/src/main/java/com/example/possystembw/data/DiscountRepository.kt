package com.example.possystembw.data

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
            val response = discountApiService.getAllDiscounts()
            if (response.isSuccessful) {
                val discounts = response.body()?.map { it.toDiscount() } ?: emptyList()
                discountDao.insertAll(discounts)
                emit(Result.success(discounts))
            } else {
                emit(Result.failure(Exception("Error fetching discounts: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun getLocalDiscounts(): List<Discount> {
        return discountDao.getAllDiscounts()
    }

    private fun DiscountResponse.toDiscount(): Discount {
        return Discount(
            id = id ?: 0,
            DISCOFFERNAME = DISCOFFERNAME.orEmpty(),
            PARAMETER = PARAMETER ?: 0,
            DISCOUNTTYPE = DISCOUNTTYPE.orEmpty()
        )
    }
}
