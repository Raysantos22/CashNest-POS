package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.possystembw.data.CustomerPurchaseHistory
import com.example.possystembw.database.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers")
    suspend fun getAllCustomers(): List<Customer>

    @Query("SELECT * FROM customers WHERE name LIKE :query OR accountNum LIKE :query")
    suspend fun searchCustomers(query: String): List<Customer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>)

    @Query("SELECT * FROM customers")
    fun getAllCustomersFlow(): Flow<List<Customer>>


    @Query("""
        SELECT 
            p.id,
            p.itemid,
            p.itemname as itemName,
            p.itemgroup as itemGroup,
            p.price,
            p.foodpanda,
            p.grabfood,
            p.manilaprice,
            p.categoryId,
            COUNT(*) as purchaseCount,
            MAX(ts.createdDate) as lastPurchaseDate,
            AVG(CAST(tr.quantity as FLOAT)) as averageQuantity
        FROM transactions tr
        JOIN transaction_summary ts ON tr.transaction_id = ts.transaction_id
        JOIN products p ON tr.itemId = p.itemid
        WHERE ts.customerName = :customerName
        GROUP BY p.id, p.itemid, p.itemname, p.itemgroup, p.price, 
                 p.foodpanda, p.grabfood, p.manilaprice, p.categoryId
        ORDER BY purchaseCount DESC, lastPurchaseDate DESC
        LIMIT 3
    """)
    suspend fun getCustomerPurchaseHistory(customerName: String): List<CustomerPurchaseHistory>
}

