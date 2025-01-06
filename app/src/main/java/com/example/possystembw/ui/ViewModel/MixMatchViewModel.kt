package com.example.possystembw.ui.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.possystembw.data.MixMatchRepository
import com.example.possystembw.database.CartItem
import com.example.possystembw.database.MixMatchWithDetails
import com.example.possystembw.database.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MixMatchViewModel(
    private val repository: MixMatchRepository
) : ViewModel() {
    private val _mixMatches = MutableStateFlow<List<MixMatchWithDetails>>(emptyList())
    val mixMatches: StateFlow<List<MixMatchWithDetails>> = _mixMatches.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun refreshMixMatches() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                repository.refreshMixMatches()
                _mixMatches.value = repository.getAllMixMatches()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }
    fun getAvailablePromos(): List<MixMatchWithDetails> {
        return mixMatches.value.take(3) // Take only first 3 promos
    }

    fun findPromoSuggestionsForCart(cartItems: List<CartItem>): List<MixMatchWithDetails> {
        return mixMatches.value
            .filter { mixMatch ->
                cartItems.any { cartItem ->
                    mixMatch.lineGroups.any { lineGroup ->
                        lineGroup.discountLines.any { line ->
                            line.itemId == cartItem.itemId
                        }
                    }
                }
            }
            .take(3) // Limit to 3 promos
    }
}

// ViewModel Factory
class MixMatchViewModelFactory(
    private val repository: MixMatchRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MixMatchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MixMatchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
