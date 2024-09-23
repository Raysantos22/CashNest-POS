package com.example.possystembw.DAO

import androidx.room.*
import com.example.possystembw.database.CartItem
import com.example.possystembw.database.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {

    @Query("UPDATE cart_items SET overriddenPrice = :newPrice WHERE id = :cartItemId")
    suspend fun updatePriceoverride(cartItemId: Int, newPrice: Double)
    @Query("SELECT * FROM cart_items WHERE window_id = :windowId")
    fun getCartItemsByWindow(windowId: Int): Flow<List<CartItem>>
    @Query("UPDATE cart_items SET overriddenPrice = NULL WHERE id = :cartItemId")
    suspend fun resetPrice(cartItemId: Int)

    @Query("SELECT * FROM cart_items")
    fun getAllCartItems(): Flow<List<CartItem>>
    @Query("SELECT * FROM cart_items WHERE product_id = :productId AND window_id = :windowId LIMIT 1")
    suspend fun getCartItemByProductId(productId: Int, windowId: Int): CartItem?

    @Query("SELECT COALESCE(SUM(partialPayment), 0) FROM cart_items WHERE window_Id = :windowId")
    fun getPartialPaymentForWindow(windowId: Int): Flow<Double>

    @Delete
    suspend fun deleteCartItem(cartItem: CartItem)

    @Update
    suspend fun update(cartItem: CartItem)

    @Query("DELETE FROM cart_items WHERE window_id = :windowId")
    suspend fun deleteAll(windowId: Int)

    @Delete
    suspend fun delete(cartItem: CartItem)

    @Query("DELETE FROM cart_items WHERE window_id = :windowId")
    suspend fun deleteAllForWindow(windowId: Int)

    @Query("DELETE FROM cart_items")
    suspend fun deleteAll()

    @Query("SELECT * FROM cart_items WHERE product_id = :productId")
    suspend fun getCartItemByProductId(productId: Int): CartItem?

    // Add this function
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cartItems: List<CartItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cartItem: CartItem)

    @Query("SELECT * FROM cart_items WHERE window_id = :windowId")
    fun getAllCartItems(windowId: Int): Flow<List<CartItem>>

    @Query("SELECT * FROM cart_items WHERE id = :id")
    suspend fun getCartItemById(id: Int): CartItem?

   @Query("UPDATE cart_items SET price = :newPrice WHERE id = :id")
    suspend fun updatePrice(id: Int, newPrice: Double)
    @Query("UPDATE cart_items SET overriddenprice = :newPrice WHERE id = :cartItemId")
    suspend fun overridePrice(cartItemId: Int, newPrice: Double)

    @Query("UPDATE cart_items SET discount = :discount, discounttype = :discountType WHERE id = :cartItemId")
    suspend fun applyDiscount(cartItemId: Int, discount: Double, discountType: String)

    @Query("UPDATE cart_items SET overriddenprice = NULL, discount = 0, discounttype = NULL WHERE id = :cartItemId")
    suspend fun resetPriceAndDiscount(cartItemId: Int)
    @Query("DELETE FROM cart_items WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("UPDATE cart_items SET partialPayment = partialPayment + :amount WHERE window_id = :windowId")
    suspend fun addPartialPayment(windowId: Int, amount: Double)

    @Query("SELECT SUM(partialPayment) FROM cart_items WHERE window_id = :windowId")
    suspend fun getTotalPartialPayment(windowId: Int): Double?

    @Query("UPDATE cart_items SET partialPayment = 0 WHERE window_id = :windowId")
    suspend fun resetPartialPayment(windowId: Int)

    @Query("UPDATE cart_items SET comment = :comment WHERE id = :cartItemId")
    suspend fun updateComment(cartItemId: Int, comment: String?)

    @Query("SELECT comment FROM cart_items WHERE id = :cartItemId")
    suspend fun getComment(cartItemId: Int): String?

    @Query("UPDATE cart_items SET cart_comment = :comment WHERE window_id = :windowId")
    suspend fun updateCartComment(windowId: Int, comment: String?)

    @Query("SELECT cart_comment FROM cart_items WHERE window_id = :windowId LIMIT 1")
    suspend fun getCartComment(windowId: Int): String?

    @Query("UPDATE cart_items SET comment = :comment WHERE id = :cartItemId")
    suspend fun updateItemComment(cartItemId: Int, comment: String?)

    @Query("SELECT comment FROM cart_items WHERE id = :cartItemId")
    suspend fun getItemComment(cartItemId: Int): String?

    @Query("SELECT * FROM cart_items WHERE window_Id = :windowId")
    suspend fun getCartItemsForWindow(windowId: Int): List<CartItem>

    @Query("UPDATE cart_items SET cart_comment = '' WHERE window_id = :windowId")
    suspend fun clearCartComment(windowId: Int)


}