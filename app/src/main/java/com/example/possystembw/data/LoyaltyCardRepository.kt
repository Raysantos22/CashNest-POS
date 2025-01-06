package com.example.possystembw.data

import android.util.Log
import com.example.possystembw.DAO.LoyaltyCardApi
import com.example.possystembw.DAO.LoyaltyCardDao
import com.example.possystembw.database.LoyaltyCard
import kotlinx.coroutines.flow.Flow

class LoyaltyCardRepository(
    private val loyaltyCardDao: LoyaltyCardDao,
    private val loyaltyCardApi: LoyaltyCardApi
) {
    val allLoyaltyCards: Flow<List<LoyaltyCard>> = loyaltyCardDao.getAllLoyaltyCards()

    suspend fun loadInitialData() {
        try {
            Log.d("LoyaltyCardRepository", "Loading initial loyalty card data")
            val response = loyaltyCardApi.getLoyaltyCards()
            if (response.isSuccessful) {
                response.body()?.data?.loyaltyCards?.let { cards ->
                    Log.d("LoyaltyCardRepository", "Received ${cards.size} loyalty cards")
                    loyaltyCardDao.deleteAll()
                    loyaltyCardDao.insertAll(cards)
                    Log.d("LoyaltyCardRepository", "Successfully loaded loyalty cards into database")
                }
            } else {
                Log.e("LoyaltyCardRepository", "Failed to load initial data: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("LoyaltyCardRepository", "Error loading initial data", e)
            throw e
        }
    }
    suspend fun refreshLoyaltyCards() {
        try {
            val response = loyaltyCardApi.getLoyaltyCards()
            if (response.isSuccessful) {
                val loyaltyCards = response.body()?.data?.loyaltyCards
                if (loyaltyCards != null) {
                    Log.d("LoyaltyCardRepository", "Received ${loyaltyCards.size} loyalty cards from API")

                    // Get current local cards
                    val localCards = loyaltyCardDao.getAllLoyaltyCardsOneShot()

                    // Compare and update only changed cards
                    loyaltyCards.forEach { apiCard ->
                        val localCard = localCards.find { it.cardNumber == apiCard.cardNumber }
                        if (localCard == null ||
                            localCard.points != apiCard.points ||
                            localCard.cumulativeAmount != apiCard.cumulativeAmount) {
                            // Card is new or has changes
                            loyaltyCardDao.insertOrUpdate(apiCard)
                            Log.d("LoyaltyCardRepository", "Updated card ${apiCard.cardNumber}")
                        }
                    }

                    Log.d("LoyaltyCardRepository", "Successfully updated local database with changed loyalty cards")
                }
            } else {
                Log.e("LoyaltyCardRepository", "Error refreshing loyalty cards: ${response.code()} - ${response.message()}")
                throw Exception("API error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("LoyaltyCardRepository", "Exception refreshing loyalty cards", e)
            throw e
        }
    }
    suspend fun getLoyaltyCardByNumber(cardNumber: String): LoyaltyCard? {
        return loyaltyCardDao.getLoyaltyCardByNumber(cardNumber)
    }

    suspend fun updateLoyaltyCardState(cardId: Int, newPoints: Int, cumulativeAmount: Double) {
        loyaltyCardDao.updateLoyaltyCardState(cardId, newPoints, cumulativeAmount)
        try {
//            loyaltyCardApi.updateCardState(cardId, newPoints, cumulativeAmount)
        } catch (e: Exception) {
            Log.e("LoyaltyCardRepository", "Error updating remote card state", e)
            // Continue even if remote update fails
        }
    }
    suspend fun updateCardPoints(cardNumber: String, newPoints: Int) {
        try {
            Log.d("LoyaltyCardRepository", "Updating points for card $cardNumber to $newPoints")

            // Update local database first
            loyaltyCardDao.updatePointsAndSync(cardNumber, newPoints, 0)

            // Try to sync with server
            try {
                val response = loyaltyCardApi.updatePoints(cardNumber, newPoints)
                if (response.isSuccessful) {
                    loyaltyCardDao.updatePointsAndSync(cardNumber, newPoints, 1)
                    Log.d("LoyaltyCardRepository", "Successfully synced with server")
                } else {
                    Log.e("LoyaltyCardRepository", "Server sync failed: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("LoyaltyCardRepository", "Error syncing with server", e)
            }
        } catch (e: Exception) {
            Log.e("LoyaltyCardRepository", "Error updating points", e)
            throw e
        }
    }


    //    suspend fun syncUnsyncedCards() {
//        val unsyncedCards = loyaltyCardDao.getUnsyncedCards()
//        unsyncedCards.forEach { card ->
//            try {
//                val response = loyaltyCardApi.updatePoints(card.cardNumber, card.points)
//                if (response.isSuccessful) {
//                    loyaltyCardDao.updatePointsAndSync(card.id, card.points, 1)
//                    Log.d("LoyaltyCardRepository", "Synced points for card ${card.cardNumber}")
//                }
//            } catch (e: Exception) {
//                Log.e("LoyaltyCardRepository", "Error syncing card ${card.cardNumber}", e)
//            }
//        }
//    }
    suspend fun loadFromLocalDatabase() {
        try {
            Log.d("LoyaltyCardRepository", "Loading loyalty cards from local database")
            val localCards = loyaltyCardDao.getAllLoyaltyCardsOneShot()
            Log.d("LoyaltyCardRepository", "Loaded ${localCards.size} loyalty cards from local database")

            if (localCards.isEmpty()) {
                Log.w("LoyaltyCardRepository", "No loyalty cards found in local database")
                throw Exception("No data available in local database")
            }
        } catch (e: Exception) {
            Log.e("LoyaltyCardRepository", "Error loading from local database", e)
            throw e
        }
    }
}