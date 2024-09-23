package com.example.possystembw.ui.ViewModel
import android.util.Log
import androidx.constraintlayout.helper.widget.MotionEffect.TAG
import androidx.lifecycle.*
import com.example.possystembw.database.Product
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.possystembw.data.CartRepository
import com.example.possystembw.data.ProductRepository
import com.example.possystembw.database.CartItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class CartViewModel(private val repository: CartRepository) : ViewModel() {
    private val _currentWindowId = MutableStateFlow<Int?>(null)
    val currentWindowId: StateFlow<Int?> = _currentWindowId.asStateFlow()
    val allCartItems: Flow<List<CartItem>> = _currentWindowId.flatMapLatest { windowId ->
        windowId?.let { repository.getAllCartItems(it) } ?: kotlinx.coroutines.flow.emptyFlow()

    }
    private val _vatAmount = MutableStateFlow(0.0)
    val vatAmount: StateFlow<Double> = _vatAmount.asStateFlow()
    private var _cartComment = MutableStateFlow<String?>(null)
    val cartComment: StateFlow<String?> = _cartComment.asStateFlow()
    private val _totalWithVat = MutableStateFlow(0.0)
    val totalWithVat: StateFlow<Double> = _totalWithVat.asStateFlow()

    init {
        viewModelScope.launch {
            currentWindowCartItems.collect { cartItems ->
                val subtotal = cartItems.sumOf { it.price * it.quantity }
                _vatAmount.value = subtotal * 0.12
                _totalWithVat.value = subtotal + _vatAmount.value
            }
        }
    }

    val currentWindowCartItems: StateFlow<List<CartItem>> = _currentWindowId
        .filterNotNull()
        .flatMapLatest { window_Id ->
            repository.getAllCartItems(window_Id)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun setCurrentWindow(windowId: Int) {
        _currentWindowId.value = windowId
    }

    suspend fun getCartItemByProductId(productId: Int, windowId: Int): CartItem? {
        return repository.getCartItemByProductId(productId, windowId)
    }
    fun getPartialPaymentForWindow(windowId: Int): Flow<Double> {
        return repository.getPartialPaymentForWindow(windowId)
    }
    fun getCartItemById(id: Int): Flow<CartItem?> = flow {
        emit(repository.getCartItemById(id))
    }

    fun insert(cartItem: CartItem) = viewModelScope.launch {
        repository.insert(cartItem)
    }

    fun update(cartItem: CartItem) = viewModelScope.launch {
        repository.update(cartItem)
    }

    fun delete(cartItem: CartItem) = viewModelScope.launch {
        repository.delete(cartItem)
    }


    fun deleteAll(windowId: Int) = viewModelScope.launch {
        repository.deleteAll(windowId)
        repository.deleteAllForWindow(windowId)

    }

    fun clearCartComment(windowId: Int) = viewModelScope.launch {
        repository.clearCartComment(windowId)
        _cartComment.value = null // Clear the local state as well
    }
    fun updateComment(cartItemId: Int, comment: String?) = viewModelScope.launch {
        repository.updateComment(cartItemId, comment)
    }

    suspend fun getComment(cartItemId: Int): String? {
        return repository.getComment(cartItemId)
    }

    fun updatePriceoverride(cartItemId: Int, newPrice: Double) = viewModelScope.launch {
        repository.updatePriceoverride(cartItemId, newPrice)
    }

    fun resetPrice(cartItemId: Int) = viewModelScope.launch {
        repository.resetPrice(cartItemId)
    }
    fun overridePrice(cartItemId: Int, newPrice: Double) = viewModelScope.launch {
        repository.overridePrice(cartItemId, newPrice)
    }
    fun applyDiscount(cartItemId: Int, discount: Double, discountType: String) = viewModelScope.launch {
        repository.applyDiscount(cartItemId, discount, discountType)
    }

    fun resetPriceAndDiscount(cartItemId: Int) = viewModelScope.launch {
        repository.resetPriceAndDiscount(cartItemId)
    }

    fun getAllCartItems(windowId: Int): Flow<List<CartItem>> {
        return repository.getAllCartItems(windowId)

    }

    fun deleteCartItem(cartItem: CartItem) = viewModelScope.launch {
        repository.deleteById(cartItem.id)
    }

    suspend fun addPartialPayment(windowId: Int, amount: Double) {
        repository.addPartialPayment(windowId, amount)
    }

    suspend fun getTotalPartialPayment(windowId: Int): Double {
        return repository.getTotalPartialPayment(windowId) ?: 0.0
    }

    suspend fun resetPartialPayment(windowId: Int) {
        repository.resetPartialPayment(windowId)
    }
    fun updateCartComment(windowId: Int, comment: String?) {
        viewModelScope.launch {
            repository.updateCartComment(windowId, comment)
            _cartComment.value = comment
        }
    }

    fun getCartComment(windowId: Int) {
        viewModelScope.launch {
            _cartComment.value = repository.getCartComment(windowId)
        }
    }

    fun updateItemComment(cartItemId: Int, comment: String?) {
        viewModelScope.launch {
            repository.updateItemComment(cartItemId, comment)
        }
    }


    suspend fun getItemComment(cartItemId: Int): String? {
        return repository.getItemComment(cartItemId)
    }

        fun update(id: Int, newPrice: Double) {
            viewModelScope.launch {
                repository.updateCartItemPrice(id, newPrice)
            }
        }

    }


    class CartViewModelFactory(private val repository: CartRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CartViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

