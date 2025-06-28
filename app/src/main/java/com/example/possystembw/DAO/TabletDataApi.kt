package com.example.possystembw.DAO

import retrofit2.Response
import retrofit2.http.*

interface TabletDataApi {
    
    // Get all transaction summaries
    @GET("tablet/transactions")
    suspend fun getAllTransactionSummaries(): Response<List<TransactionSummaryResponse>>
    
    // Get transaction summary by ID
    @GET("tablet/transactions/{transactionId}")
    suspend fun getTransactionSummary(@Path("transactionId") transactionId: String): Response<TransactionSummaryResponse>
    
    // Get all transaction records
    @GET("tablet/transaction-records")
    suspend fun getAllTransactionRecords(): Response<List<TransactionRecordResponse>>
    
    // Get transaction records by transaction ID
    @GET("tablet/transaction-records/{transactionId}")
    suspend fun getTransactionRecords(@Path("transactionId") transactionId: String): Response<List<TransactionRecordResponse>>
    
    // Get transactions by store
    @GET("tablet/transactions/store/{storeId}")
    suspend fun getTransactionsByStore(@Path("storeId") storeId: String): Response<List<TransactionSummaryResponse>>
    
    // Get transactions by date range
    @GET("tablet/transactions/date-range")
    suspend fun getTransactionsByDateRange(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<List<TransactionSummaryResponse>>
    
    // Get sync status
    @GET("tablet/sync-status")
    suspend fun getSyncStatus(): Response<SyncStatusResponse>
}




data class TransactionRecordResponse(
    val id: Int,
    val transactionId: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    val subtotal: Double,
    val vatRate: Double,
    val vatAmount: Double,
    val discountRate: Double,
    val discountAmount: Double,
    val total: Double,
    val receiptNumber: String,
    val timestamp: Long,
    val paymentMethod: String,
    val ar: Double,
    val windowNumber: Int,
    val partialPaymentAmount: Double,
    val comment: String,
    val lineNum: Int?,
    val receiptId: String?,
    val itemId: String?,
    val itemGroup: String?,
    val netPrice: Double?,
    val costAmount: Double?,
    val netAmount: Double?,
    val grossAmount: Double?,
    val customerAccount: String?,
    val store: String?,
    val priceOverride: Double?,
    val staff: String?,
    val discountOfferId: String?,
    val lineDiscountAmount: Double?,
    val lineDiscountPercentage: Double?,
    val customerDiscountAmount: Double?,
    val unit: String?,
    val unitQuantity: Double?,
    val unitPrice: Double?,
    val taxAmount: Double?,
    val createdDate: String?,
    val remarks: String?,
    val inventoryBatchId: String?,
    val inventoryBatchExpiryDate: String?,
    val giftCard: String?,
    val returnTransactionId: String?,
    val returnQuantity: Double?,
    val creditMemoNumber: String?,
    val taxIncludedInPrice: Double?,
    val description: String?,
    val returnLineId: Double?,
    val priceUnit: Double?,
    val netAmountNotIncludingTax: Double?,
    val storeTaxGroup: String?,
    val currency: String?,
    val taxExempt: Double?,
    val isSelected: Boolean,
    val isReturned: Boolean,
    val discountType: String,
    val overriddenPrice: Double?,
    val originalPrice: Double?,
    val storeKey: String,
    val storeSequence: String,
    val syncStatusRecord: Boolean
)

data class SyncStatusResponse(
    val totalTransactions: Int,
    val syncedTransactions: Int,
    val pendingTransactions: Int,
    val totalRecords: Int,
    val syncedRecords: Int,
    val pendingRecords: Int,
    val lastSyncTime: String?
)
