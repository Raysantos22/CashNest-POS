package com.example.possystembw.ui.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.possystembw.data.LoyaltyCardRepository
import com.example.possystembw.database.LoyaltyCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoyaltyCardViewModel(private val repository: LoyaltyCardRepository) : ViewModel() {
    val allLoyaltyCards: Flow<List<LoyaltyCard>> = repository.allLoyaltyCards
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Load initial data when ViewModel is created
    init {
        loadLoyaltyCards()
    }

    fun loadLoyaltyCards() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                repository.loadInitialData()
            } catch (e: Exception) {
                Log.e("LoyaltyCardViewModel", "Error loading loyalty cards", e)
                _error.value = "Failed to load loyalty cards: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFromLocalDatabase() {
        viewModelScope.launch {
            repository.loadFromLocalDatabase()
        }
    }
    suspend fun getLoyaltyCardByNumber(cardNumber: String): LoyaltyCard? {
        return repository.getLoyaltyCardByNumber(cardNumber)
    }

    suspend fun updateCardPoints(cardNumber: String, newPoints: Int) {
        repository.updateCardPoints(cardNumber, newPoints)
    }
    suspend fun updateLoyaltyCardState(cardId: Int, newPoints: Int, cumulativeAmount: Double) {
        repository.updateLoyaltyCardState(cardId, newPoints, cumulativeAmount)
    }

//    fun syncUnsyncedCards() {
//        viewModelScope.launch {
//            repository.syncUnsyncedCards()
//        }
//    }
    fun refreshLoyaltyCards() {
        viewModelScope.launch {
            repository.refreshLoyaltyCards()
        }
    }
}

class LoyaltyCardViewModelFactory(private val repository: LoyaltyCardRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoyaltyCardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoyaltyCardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}