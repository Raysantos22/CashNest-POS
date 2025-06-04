package com.example.possystembw.data

import com.example.possystembw.DAO.ProductStatusDao
import com.example.possystembw.database.ProductStatus
import kotlinx.coroutines.flow.Flow

class ProductStatusRepository(
    private val productStatusDao: ProductStatusDao
) {
    fun getAllProductStatuses(): Flow<List<ProductStatus>> = productStatusDao.getAllProductStatuses()

    suspend fun getProductStatus(productId: String): ProductStatus? {
        return productStatusDao.getProductStatus(productId)
    }

    suspend fun updateProductStatus(productId: String, isEnabled: Boolean) {
        val existingStatus = productStatusDao.getProductStatus(productId)
        if (existingStatus != null) {
            productStatusDao.updateProductStatusById(productId, isEnabled)
        } else {
            productStatusDao.insertProductStatus(
                ProductStatus(productId = productId, isEnabled = isEnabled)
            )
        }
    }

    suspend fun setMultipleProductStatuses(productIds: List<String>, isEnabled: Boolean) {
        productIds.forEach { productId ->
            updateProductStatus(productId, isEnabled)
        }
    }

    suspend fun deleteProductStatus(productId: String) {
        productStatusDao.deleteProductStatus(productId)
    }
}