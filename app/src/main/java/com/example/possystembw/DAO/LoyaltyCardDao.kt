package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.possystembw.database.LoyaltyCard
import kotlinx.coroutines.flow.Flow


@Dao
interface LoyaltyCardDao {
    @Query("SELECT * FROM loyalty_cards")
    fun getAllLoyaltyCards(): Flow<List<LoyaltyCard>>

    @Query("SELECT * FROM loyalty_cards")
    suspend fun getAllLoyaltyCardsOneShot(): List<LoyaltyCard>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(loyaltyCards: List<LoyaltyCard>)

    @Query("DELETE FROM loyalty_cards")
    suspend fun deleteAll()


    @Query("SELECT * FROM loyalty_cards WHERE cardNumber = :cardNumber")
    suspend fun getLoyaltyCardByNumber(cardNumber: String): LoyaltyCard?


    @Query("UPDATE loyalty_cards SET points = :newPoints WHERE id = :cardId")
    suspend fun updatePoints(cardId: Int, newPoints: Int)

    @Query("UPDATE loyalty_cards SET points = :newPoints, cumulativeAmount = :cumulativeAmount WHERE id = :cardId")
    suspend fun updateLoyaltyCardState(cardId: Int, newPoints: Int, cumulativeAmount: Double)

    @Query("SELECT * FROM loyalty_cards WHERE syncStatus = 0")
    suspend fun getUnsyncedCards(): List<LoyaltyCard>

    @Query("UPDATE loyalty_cards SET points = :points, syncStatus = :syncStatus WHERE cardNumber = :cardNumber")
    suspend fun updatePointsAndSyncStatus(cardNumber: String, points: Int, syncStatus: Int)

    @Query("UPDATE loyalty_cards SET syncStatus = :syncStatus WHERE cardNumber = :cardNumber")
    suspend fun updateSyncStatus(cardNumber: String, syncStatus: Int)

    @Query("UPDATE loyalty_cards SET points = :newPoints, syncStatus = :syncStatus WHERE cardNumber = :cardNumber")
    suspend fun updatePointsAndSync(cardNumber: String, newPoints: Int, syncStatus: Int)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(loyaltyCard: LoyaltyCard)
}