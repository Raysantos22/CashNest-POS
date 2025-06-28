package com.example.possystembw.data

import com.example.possystembw.DAO.SyncStatusResponse
import com.example.possystembw.DAO.TransactionDao
import com.example.possystembw.DAO.TransactionRecordResponse
import com.example.possystembw.DAO.TransactionSummaryResponse
import com.example.possystembw.database.TransactionRecord
import com.example.possystembw.database.TransactionSummary

class TabletDataRepository(private val transactionDao: TransactionDao) {
    
    fun mapTransactionSummaryToResponse(summary: TransactionSummary): TransactionSummaryResponse {
        return TransactionSummaryResponse(
            transaction_id = summary.transactionId,
            type = summary.type,
            receiptid = summary.receiptId,
            store = summary.store,
            staff = summary.staff,
            customerName = summary.customerAccount,
            netamount = summary.netAmount,
            costamount = summary.costAmount,
            grossamount = summary.grossAmount,
            partial_payment = summary.partialPayment,
            transactionstatus = summary.transactionStatus,
            discamount = summary.discountAmount,
            custdiscamount = summary.customerDiscountAmount,
            totaldiscamount = summary.totalDiscountAmount,
            numberofitems = summary.numberOfItems,
            refundreceiptid = summary.refundReceiptId,
            currency = summary.currency,
            zreportid = summary.zReportId,
            createddate = summary.createdDate.toString(),
            priceoverride = summary.priceOverride,
            comment = summary.comment,
            receiptemail = summary.receiptEmail,
            markupamount = summary.markupAmount,
            markupdescription = summary.markupDescription,
            taxinclinprice = summary.taxIncludedInPrice,
            window_number = summary.windowNumber,
            total_amount_paid = summary.totalAmountPaid,
            change_given = summary.changeGiven,
            paymentMethod = summary.paymentMethod,
            custaccount = summary.customerName,
            vatAmount = summary.vatAmount,
            vatExemptAmount = summary.vatExemptAmount,
            vatableSales = summary.vatableSales,
            discountType = summary.discountType,
            gcash = summary.gCash,
            paymaya = summary.payMaya,
            cash = summary.cash,
            card = summary.card,
            loyaltycard = summary.loyaltyCard,
            charge = summary.charge,
            foodpanda = summary.foodpanda,
            grabfood = summary.grabfood,
            representation = summary.representation,
            store_key = summary.storeKey,
            store_sequence = summary.storeSequence,
            syncStatus = summary.syncStatus

        )
    }
    
    fun mapTransactionRecordToResponse(record: TransactionRecord): TransactionRecordResponse {
        return TransactionRecordResponse(
            id = record.id,
            transactionId = record.transactionId,
            name = record.name,
            price = record.price,
            quantity = record.quantity,
            subtotal = record.subtotal,
            vatRate = record.vatRate,
            vatAmount = record.vatAmount,
            discountRate = record.discountRate,
            discountAmount = record.discountAmount,
            total = record.total,
            receiptNumber = record.receiptNumber,
            timestamp = record.timestamp,
            paymentMethod = record.paymentMethod,
            ar = record.ar,
            windowNumber = record.windowNumber,
            partialPaymentAmount = record.partialPaymentAmount,
            comment = record.comment,
            lineNum = record.lineNum,
            receiptId = record.receiptId,
            itemId = record.itemId,
            itemGroup = record.itemGroup,
            netPrice = record.netPrice,
            costAmount = record.costAmount,
            netAmount = record.netAmount,
            grossAmount = record.grossAmount,
            customerAccount = record.customerAccount,
            store = record.store,
            priceOverride = record.priceOverride,
            staff = record.staff,
            discountOfferId = record.discountOfferId,
            lineDiscountAmount = record.lineDiscountAmount,
            lineDiscountPercentage = record.lineDiscountPercentage,
            customerDiscountAmount = record.customerDiscountAmount,
            unit = record.unit,
            unitQuantity = record.unitQuantity,
            unitPrice = record.unitPrice,
            taxAmount = record.taxAmount,
            createdDate = record.createdDate?.toString(),
            remarks = record.remarks,
            inventoryBatchId = record.inventoryBatchId,
            inventoryBatchExpiryDate = record.inventoryBatchExpiryDate?.toString(),
            giftCard = record.giftCard,
            returnTransactionId = record.returnTransactionId,
            returnQuantity = record.returnQuantity,
            creditMemoNumber = record.creditMemoNumber,
            taxIncludedInPrice = record.taxIncludedInPrice,
            description = record.description,
            returnLineId = record.returnLineId,
            priceUnit = record.priceUnit,
            netAmountNotIncludingTax = record.netAmountNotIncludingTax,
            storeTaxGroup = record.storeTaxGroup,
            currency = record.currency,
            taxExempt = record.taxExempt,
            isSelected = record.isSelected,
            isReturned = record.isReturned,
            discountType = record.discountType,
            overriddenPrice = record.overriddenPrice,
            originalPrice = record.originalPrice,
            storeKey = record.storeKey,
            storeSequence = record.storeSequence,
            syncStatusRecord = record.syncStatusRecord
        )
    }
    
    suspend fun getAllTransactionSummariesForApi(): List<TransactionSummaryResponse> {
        return transactionDao.getAllTransactionSummaries().map { mapTransactionSummaryToResponse(it) }
    }
    
    suspend fun getTransactionSummaryForApi(transactionId: String): TransactionSummaryResponse? {
        return transactionDao.getTransactionSummary(transactionId)?.let { mapTransactionSummaryToResponse(it) }
    }
    
    suspend fun getAllTransactionRecordsForApi(): List<TransactionRecordResponse> {
        return transactionDao.getAllTransactionRecords().map { mapTransactionRecordToResponse(it) }
    }
    
    suspend fun getTransactionRecordsForApi(transactionId: String): List<TransactionRecordResponse> {
        return transactionDao.getTransactionRecordsByTransactionId(transactionId).map { mapTransactionRecordToResponse(it) }
    }
    
    suspend fun getTransactionsByStoreForApi(storeId: String): List<TransactionSummaryResponse> {
        return transactionDao.getTransactionsByStore(storeId).map { mapTransactionSummaryToResponse(it) }
    }
    
    suspend fun getSyncStatusForApi(): SyncStatusResponse {
        val allSummaries = transactionDao.getAllTransactionSummaries()
        val allRecords = transactionDao.getAllTransactionRecords()
        
        return SyncStatusResponse(
            totalTransactions = allSummaries.size,
            syncedTransactions = allSummaries.count { it.syncStatus },
            pendingTransactions = allSummaries.count { !it.syncStatus },
            totalRecords = allRecords.size,
            syncedRecords = allRecords.count { it.syncStatusRecord },
            pendingRecords = allRecords.count { !it.syncStatusRecord },
            lastSyncTime = null // You can implement this if needed
        )
    }
}