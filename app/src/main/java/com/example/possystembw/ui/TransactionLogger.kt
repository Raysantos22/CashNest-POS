package com.example.possystembw.ui

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.possystembw.database.TenderDeclaration
import com.example.possystembw.database.TransactionRecord
import com.example.possystembw.database.TransactionSummary
import com.example.possystembw.ui.ViewModel.BluetoothPrinterHelper
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class TransactionLogger(private val context: Context) {

    companion object {
        private const val FOLDER_NAME = "BIR"
        private const val TAG = "TransactionLogger"

    }

    private fun createDirectory() {
        val folder = File(context.getExternalFilesDir(null), FOLDER_NAME)
        if (!folder.exists()) {
            val created = folder.mkdirs()
            Log.d(
                TAG, if (created) {
                    "Created BIR folder at: ${folder.absolutePath}"
                } else {
                    "Failed to create BIR folder"
                }
            )
        }
    }

    private fun getCurrentDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun getCurrentDateTime(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    private fun getDailyLogFile(): File {
        createDirectory()
        val currentDate = getCurrentDate()
        val file = File(
            File(context.getExternalFilesDir(null), FOLDER_NAME),
            "$currentDate.txt"
        )

        if (!file.exists()) {
            file.createNewFile()
            Log.d(TAG, "Created new log file for today at: ${file.absolutePath}")
        }

        return file
    }

    private fun formatCurrency(amount: Double): String =
        String.format(Locale.getDefault(), "%.2f", amount)

    private fun logToFile(content: String) {
        val file = getDailyLogFile()
        try {
            FileWriter(file, true).use { writer ->
                writer.append(content)
                writer.append("\n")
                writer.append("=".repeat(32))
                writer.append("\n\n")
            }
            Log.d(TAG, "Transaction logged to: ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write to log file: ${e.message}")
        }
    }

    fun logPayment(
        transactionSummary: TransactionSummary,
        paymentMethod: String,
        items: List<TransactionRecord>
    ) {
        val logEntry = buildString {
            appendLine("TRANSACTION TYPE: PAYMENT")
            appendLine(getCurrentDateTime())
            appendLine("-".repeat(32))

            appendLine("ELJIN CORP")
            appendLine("Address Line 1")
            appendLine("Address Line 2")
            appendLine("TIN: Your TIN Number")
            appendLine("ACC: Your ACC Number")
            appendLine("Contact #: Your Contact Number")
            appendLine("-".repeat(32))

            appendLine("Date Issued: ${getCurrentDateTime()}")
            appendLine("Receipt ID: ${transactionSummary.receiptId}")
            appendLine("Transaction ID: ${transactionSummary.transactionId}")
            appendLine("Store: ${transactionSummary.store}")
            appendLine("Cashier: ${transactionSummary.staff}")
            appendLine("-".repeat(32))

            // Items section
            appendLine("Item Name              Price    Qty    Total")
            items.forEach { item ->
                appendLine(
                    "${item.name.take(20).padEnd(20)} " +
                            "${formatCurrency(item.price).padStart(8)} " +
                            "${item.quantity.toString().padStart(4)} " +
                            "${formatCurrency(item.quantity * item.price).padStart(8)}"
                )

                // Show discount if any
                if (item.discountAmount > 0) {
                    appendLine("   Discount: ${formatCurrency(item.discountAmount)}")
                }
            }
            appendLine("-".repeat(32))

            appendLine("Gross Amount:        ${formatCurrency(transactionSummary.grossAmount)}")
            appendLine("Total Discounts:     ${formatCurrency(transactionSummary.totalDiscountAmount)}")
            appendLine("Net Amount:          ${formatCurrency(transactionSummary.netAmount)}")
            appendLine("Amount Paid:         ${formatCurrency(transactionSummary.totalAmountPaid)}")
            appendLine("Change:              ${formatCurrency(transactionSummary.changeGiven)}")
            appendLine("-".repeat(32))

            // Payment Method Section
            appendLine("Payment Method: ${transactionSummary.paymentMethod}")
            if (transactionSummary.cash > 0) {
                appendLine("Cash Payment:         ${formatCurrency(transactionSummary.cash)}")
            }
            if (transactionSummary.card > 0) {
                appendLine("Card Payment:         ${formatCurrency(transactionSummary.card)}")
            }
            if (transactionSummary.gCash > 0) {
                appendLine("GCash Payment:        ${formatCurrency(transactionSummary.gCash)}")
            }
            if (transactionSummary.payMaya > 0) {
                appendLine("PayMaya Payment:      ${formatCurrency(transactionSummary.payMaya)}")
            }
            appendLine("-".repeat(32))

            // VAT Information
            appendLine("VAT Analysis")
            appendLine("Vatable Sales:       ${formatCurrency(transactionSummary.vatableSales)}")
            appendLine("VAT Amount (12%):    ${formatCurrency(transactionSummary.vatAmount)}")
            appendLine("VAT Exempt:          ${formatCurrency(0.0)}")
            appendLine("Zero Rated:          ${formatCurrency(0.0)}")
            appendLine("-".repeat(32))
        }

        logToFile(logEntry)
    }

    fun logReturn(
        returnTransaction: TransactionSummary,
        originalTransaction: TransactionSummary,
        returnedItems: List<TransactionRecord>,
        remarks: String
    ) {
        val logEntry = buildString {
            appendLine("TRANSACTION TYPE: RETURN")
            appendLine(getCurrentDateTime())
            appendLine("-".repeat(32))

            appendLine("ELJIN CORP")
            appendLine("Address Line 1")
            appendLine("Address Line 2")
            appendLine("TIN: Your TIN Number")
            appendLine("ACC: Your ACC Number")
            appendLine("Contact #: Your Contact Number")
            appendLine("-".repeat(32))

            appendLine("*** RETURN TRANSACTION ***")
            appendLine("Original Receipt #: ${originalTransaction.receiptId}")
            appendLine("Date Issued: ${getCurrentDateTime()}")
            appendLine("Return Transaction ID: ${returnTransaction.transactionId}")
            appendLine("Store: ${returnTransaction.store}")
            appendLine("Cashier: ${returnTransaction.staff}")
            appendLine("-".repeat(32))

            // Items section
            appendLine("Item Name              Price    Qty    Total")
            returnedItems.forEach { item ->
                appendLine(
                    "${item.name.take(20).padEnd(20)} " +
                            "${formatCurrency(item.price).padStart(8)} " +
                            "${abs(item.quantity).toString().padStart(4)} " +
                            "${formatCurrency(abs(item.quantity * item.price)).padStart(8)}"
                )
            }
            appendLine("-".repeat(32))

            appendLine("Return Amount:        ${formatCurrency(abs(returnTransaction.grossAmount))}")
            appendLine("VAT Amount (12%):    ${formatCurrency(abs(returnTransaction.vatAmount))}")
            appendLine("Vatable Return:      ${formatCurrency(abs(returnTransaction.vatableSales))}")
            appendLine("-".repeat(32))

            appendLine("Return Reason: $remarks")
            appendLine("-".repeat(32))
        }

        logToFile(logEntry)
    }

    fun logReprint(
        transaction: TransactionSummary,
        items: List<TransactionRecord>
    ) {
        val logEntry = buildString {
            appendLine("TRANSACTION TYPE: REPRINT")
            appendLine(getCurrentDateTime())
            appendLine("-".repeat(32))

            appendLine("ELJIN CORP")
            appendLine("Address Line 1")
            appendLine("Address Line 2")
            appendLine("TIN: Your TIN Number")
            appendLine("ACC: Your ACC Number")
            appendLine("Contact #: Your Contact Number")
            appendLine("-".repeat(32))

            appendLine("*** REPRINT ***")
            appendLine("Original Transaction ID: ${transaction.transactionId}")
            appendLine("Original Receipt ID: ${transaction.receiptId}")
            appendLine("Date Issued: ${getCurrentDateTime()}")
            appendLine("Store: ${transaction.store}")
            appendLine("Cashier: ${transaction.staff}")
            appendLine("-".repeat(32))

            // Items section
            appendLine("Item Name              Price    Qty    Total")
            items.forEach { item ->
                appendLine(
                    "${item.name.take(20).padEnd(20)} " +
                            "${formatCurrency(item.price).padStart(8)} " +
                            "${item.quantity.toString().padStart(4)} " +
                            "${formatCurrency(item.quantity * item.price).padStart(8)}"
                )

                // Show discount if any
                if (item.discountAmount > 0) {
                    appendLine("   Discount: ${formatCurrency(item.discountAmount)}")
                }
            }
            appendLine("-".repeat(32))

            appendLine("Gross Amount:        ${formatCurrency(transaction.grossAmount)}")
            appendLine("Total Discounts:     ${formatCurrency(transaction.totalDiscountAmount)}")
            appendLine("Net Amount:          ${formatCurrency(transaction.netAmount)}")
            appendLine("VAT Amount (12%):    ${formatCurrency(transaction.vatAmount)}")
            appendLine("-".repeat(32))

            // Payment Method
            appendLine("Payment Method: ${transaction.paymentMethod}")
            when (transaction.paymentMethod) {
                "CASH" -> appendLine("Cash Amount: ${formatCurrency(transaction.cash)}")
                "CARD" -> appendLine("Card Amount: ${formatCurrency(transaction.card)}")
                "GCASH" -> appendLine("GCash Amount: ${formatCurrency(transaction.gCash)}")
                "PAYMAYA" -> appendLine("PayMaya Amount: ${formatCurrency(transaction.payMaya)}")
            }
            appendLine("-".repeat(32))
        }

        logToFile(logEntry)
    }

    fun logXRead(
        transactions: List<TransactionSummary>,
        tenderDeclaration: TenderDeclaration?
    ) {
        val logEntry = buildString {
            appendLine("TRANSACTION TYPE: X-READ")
            appendLine(getCurrentDateTime())
            appendLine("-".repeat(32))

            appendLine("ELJIN CORP")
            appendLine("Address Line 1")
            appendLine("Address Line 2")
            appendLine("TIN: Your TIN Number")
            appendLine("ACC: Your ACC Number")
            appendLine("Contact #: Your Contact Number")
            appendLine("-".repeat(32))

            appendLine("X-READ REPORT")
            appendLine("-".repeat(32))

            appendLine("Date: ${getCurrentDate()}")
            appendLine("Time: ${getCurrentDateTime()}")
            appendLine("-".repeat(32))

            if (transactions.isEmpty()) {
                appendLine("No transactions for this period")
                appendLine("-".repeat(32))
                appendLine("TRANSACTION SUMMARY")
                appendLine("Total Transactions: 0")
                appendLine("Sales Count: 0")
                appendLine("Return Count: 0")
            } else {
                val sales = transactions.filter { it.grossAmount > 0 }
                val returns = transactions.filter { it.grossAmount < 0 }

                appendLine("TRANSACTION SUMMARY")
                appendLine("Total Transactions: ${transactions.size}")
                appendLine("Sales Count: ${sales.size}")
                appendLine("Return Count: ${returns.size}")
                appendLine("-".repeat(32))

                appendLine("GROSS SALES")
                appendLine("Total Sales:         ${formatCurrency(sales.sumOf { it.grossAmount })}")
                appendLine("Total Returns:       ${formatCurrency(abs(returns.sumOf { it.grossAmount }))}")
                appendLine("Net Sales:           ${formatCurrency(sales.sumOf { it.grossAmount } + returns.sumOf { it.grossAmount })}")
            }
            appendLine("-".repeat(32))

            if (transactions.isNotEmpty() && tenderDeclaration != null) {
                appendLine("-".repeat(32))
                appendLine("TENDER DECLARATION")
                appendLine("Total Cash:                ${formatCurrency(tenderDeclaration.cashAmount)}")
                appendLine("Total AR:            ${formatCurrency(tenderDeclaration.arPayAmount)}")
            }
            // Always include VAT section even if zero
            appendLine("VAT ANALYSIS")
            appendLine("VAT Sales:           ${formatCurrency(transactions.sumOf { it.vatableSales })}")
            appendLine("VAT Amount:          ${formatCurrency(transactions.sumOf { it.vatAmount })}")
            appendLine("VAT Exempt:          ${formatCurrency(0.0)}")
            appendLine("Zero Rated:          ${formatCurrency(0.0)}")
            appendLine("-".repeat(32))
        }

        logToFile(logEntry)
    }

    fun logZRead(
        zReportId: String,
        transactions: List<TransactionSummary>,
        tenderDeclaration: TenderDeclaration?
    ) {
        val logEntry = buildString {
            appendLine("TRANSACTION TYPE: Z-READ")
            appendLine(getCurrentDateTime())
            appendLine("-".repeat(32))

            appendLine("ELJIN CORP")
            appendLine("Address Line 1")
            appendLine("Address Line 2")
            appendLine("TIN: Your TIN Number")
            appendLine("ACC: Your ACC Number")
            appendLine("Contact #: Your Contact Number")
            appendLine("-".repeat(32))

            appendLine("Z-READ REPORT")
            appendLine("-".repeat(32))

            appendLine("Date: ${getCurrentDate()}")
            appendLine("Time: ${getCurrentDateTime()}")
            appendLine("Z-Report ID: $zReportId")
            appendLine("-".repeat(32))

            if (transactions.isEmpty()) {
                appendLine("SALES SUMMARY")
                appendLine("Total Transactions: 0")
                appendLine("Sales Count: 0")
                appendLine("Return Count: 0")
                appendLine("-".repeat(32))

                appendLine("GROSS SALES")
                appendLine("Total Sales:         ${formatCurrency(0.0)}")
                appendLine("Total Returns:       ${formatCurrency(0.0)}")
                appendLine("Net Sales:           ${formatCurrency(0.0)}")
            } else {
                val sales = transactions.filter { it.grossAmount > 0 }
                val returns = transactions.filter { it.grossAmount < 0 }

                appendLine("SALES SUMMARY")
                appendLine("Total Transactions: ${transactions.size}")
                appendLine("Sales Count: ${sales.size}")
                appendLine("Return Count: ${returns.size}")
                appendLine("-".repeat(32))

                appendLine("GROSS SALES")
                appendLine("Total Sales:         ${formatCurrency(sales.sumOf { it.grossAmount })}")
                appendLine("Total Returns:       ${formatCurrency(abs(returns.sumOf { it.grossAmount }))}")
                appendLine("Net Sales:           ${formatCurrency(sales.sumOf { it.grossAmount } + returns.sumOf { it.grossAmount })}")

                // Only show payment summary if there are transactions
                if (transactions.isNotEmpty() && tenderDeclaration != null) {
                    appendLine("-".repeat(32))
                    appendLine("TENDER DECLARATION")
                    appendLine("Total Cash:                ${formatCurrency(tenderDeclaration.cashAmount)}")
                    appendLine("Total AR:            ${formatCurrency(tenderDeclaration.arPayAmount)}")
                }
            }
            appendLine("-".repeat(32))

            // Always include VAT section even if zero
            appendLine("VAT ANALYSIS")
            appendLine("VAT Sales:           ${formatCurrency(transactions.sumOf { it.vatableSales })}")
            appendLine("VAT Amount:          ${formatCurrency(transactions.sumOf { it.vatAmount })}")
            appendLine("VAT Exempt:          ${formatCurrency(0.0)}")
            appendLine("Zero Rated:          ${formatCurrency(0.0)}")
            appendLine("-".repeat(32))
        }

        logToFile(logEntry)
    }
}