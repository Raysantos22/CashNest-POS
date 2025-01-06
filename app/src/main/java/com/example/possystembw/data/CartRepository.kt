package com.example.possystembw.data

import android.util.Log
import com.example.possystembw.DAO.CartDao
import com.example.possystembw.DAO.ProductDao
import com.example.possystembw.RetrofitClient.categoryApi
import com.example.possystembw.database.CartItem
import com.example.possystembw.database.Category
import com.example.possystembw.database.Product
import kotlinx.coroutines.flow.Flow

class CartRepository(private val cartDao: CartDao) {

    suspend fun insert(cartItem: CartItem) {
        cartDao.insert(cartItem)
    }
    suspend fun clearCartComment(windowId: Int) {
        cartDao.clearCartComment(windowId)
    }
    suspend fun deleteAllForWindow(windowId: Int) {
        cartDao.deleteAllForWindow(windowId)
    }
    fun getPartialPaymentForWindow(windowId: Int): Flow<Double> {
        return cartDao.getPartialPaymentForWindow(windowId)
    }

    suspend fun update(cartItem: CartItem) {
        cartDao.update(cartItem)
    }


    suspend fun delete(cartItem: CartItem) {
        cartDao.delete(cartItem)
    }

    suspend fun deleteAll(windowId: Int) {
        cartDao.deleteAll(windowId)
    }
    fun getAllCartItems(): Flow<List<CartItem>> {
        return cartDao.getAllCartItems()
    }
    suspend fun deleteById(id: Int) {
        cartDao.deleteById(id)
    }

    suspend fun getCartItemByProductId(productId: Int, windowId: Int): CartItem? {
        return cartDao.getCartItemByProductId(productId, windowId)
    }
    suspend fun getCartItemById(id: Int): CartItem? {
        return cartDao.getCartItemById(id)
    }
    suspend fun updatePriceoverride(cartItemId: Int, newPrice: Double) {
        cartDao.updatePriceoverride(cartItemId, newPrice)
    }
    suspend fun deleteCartItem(cartItem: CartItem) {
        cartDao.deleteCartItem(cartItem)
    }
    suspend fun resetPrice(cartItemId: Int) {
        cartDao.resetPrice(cartItemId)
    }
    suspend fun updateCartItemPrice(id: Int, newPrice: Double) {
        cartDao.updatePrice(id, newPrice)

    }

    suspend fun overridePrice(cartItemId: Int, newPrice: Double) {
        cartDao.overridePrice(cartItemId, newPrice)
    }

    suspend fun addPartialPayment(windowId: Int, amount: Double) {
        cartDao.addPartialPayment(windowId, amount)
    }

    suspend fun getTotalPartialPayment(windowId: Int): Double {
        return cartDao.getTotalPartialPayment(windowId) ?: 0.0
    }
    suspend fun updateComment(cartItemId: Int, comment: String?) {
        cartDao.updateComment(cartItemId, comment)
    }

    suspend fun getComment(cartItemId: Int): String? {
        return cartDao.getComment(cartItemId)
    }

    suspend fun resetPartialPayment(windowId: Int) {
        cartDao.resetPartialPayment(windowId)
    }
    suspend fun applyDiscount(cartItemId: Int, discount: Double, discountType: String) {
        cartDao.applyDiscount(cartItemId, discount, discountType)
    }

    suspend fun resetPriceAndDiscount(cartItemId: Int) {
        cartDao.resetPriceAndDiscount(cartItemId)
    }
    suspend fun updateCartComment(windowId: Int, comment: String?) {
        cartDao.updateCartComment(windowId, comment)
    }

    // Method to retrieve the cart comment from the database
    suspend fun getCartComment(windowId: Int): String? {
        return cartDao.getCartComment(windowId)
    }

    // Method to update the item-specific comment
    suspend fun updateItemComment(cartItemId: Int, comment: String?) {
        cartDao.updateItemComment(cartItemId, comment)
    }


    // Method to retrieve an item-specific comment from the database
    suspend fun getItemComment(cartItemId: Int): String? {
        return cartDao.getItemComment(cartItemId)
    }

    fun getAllCartItems(windowId: Int): Flow<List<CartItem>> {
        return cartDao.getAllCartItems(windowId)
        Log.d("CartRepository", "Fetched items for window $windowId")

    }
    suspend fun getCategories(): Result<List<Category>> {
        return try {
            val response = categoryApi.getCategories()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("API call failed with code ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

