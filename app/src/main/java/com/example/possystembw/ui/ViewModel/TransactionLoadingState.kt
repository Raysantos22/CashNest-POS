package com.example.possystembw.ui.ViewModel

sealed class TransactionLoadingState {
    object Loading : TransactionLoadingState()
    data class Success(val summaryCount: Int, val detailsCount: Int) : TransactionLoadingState()
    data class Error(val message: String) : TransactionLoadingState()
}
