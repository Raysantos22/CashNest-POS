package com.example.possystembw.ui

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.possystembw.database.TenderDeclaration
import com.example.possystembw.database.TransactionRecord
import com.example.possystembw.database.TransactionSummary
import com.example.possystembw.ui.SecureTime.getCurrentTime
import com.example.possystembw.ui.ViewModel.BluetoothPrinterHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
                writer.append("=".repeat(45))
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
        val logEntry = generateReceiptContent(
            transactionSummary,
            items,
            BluetoothPrinterHelper.ReceiptType.ORIGINAL
        )
        logToFile(logEntry)
    }

    fun logReturn(
        returnTransaction: TransactionSummary,
        originalTransaction: TransactionSummary,
        returnedItems: List<TransactionRecord>,
        remarks: String
    ) {
        val logEntry = generateReceiptContent(
            returnTransaction,
            returnedItems,
            BluetoothPrinterHelper.ReceiptType.RETURN
        )
        logToFile(logEntry)
    }

    fun logReprint(
        transaction: TransactionSummary,
        items: List<TransactionRecord>
    ) {
        val logEntry = generateReceiptContent(
            transaction,
            items,
            BluetoothPrinterHelper.ReceiptType.REPRINT
        )
        logToFile(logEntry)
    }

    fun logXRead(
        transactions: List<TransactionSummary>,
        tenderDeclaration: TenderDeclaration?
    ) {
        val logEntry = buildReadReport(transactions, false, "", tenderDeclaration)
        logToFile(logEntry)
    }

    fun logZRead(
        zReportId: String,
        transactions: List<TransactionSummary>,
        tenderDeclaration: TenderDeclaration?
    ) {
        val logEntry = buildReadReport(transactions, true, zReportId, tenderDeclaration)
        logToFile(logEntry)
    }

    private fun generateReceiptContent(
        transaction: TransactionSummary,
        items: List<TransactionRecord>,
        receiptType: BluetoothPrinterHelper.ReceiptType,
        isAR: Boolean = false,
        copyType: String? = null
    ): String {
        val sb = StringBuilder()
        val isReturn = receiptType == BluetoothPrinterHelper.ReceiptType.RETURN

        // Filter items
        val filteredItems = if (isReturn) {
            items.filter { it.isReturned }
        } else {
            items.groupBy { it.itemId }.map { (_, groupedItems) ->
                val originalItem = groupedItems.first { !it.isReturned }
                val returnedQuantity = groupedItems
                    .filter { it.isReturned }
                    .sumOf { it.quantity }
                originalItem.copy(
                    quantity = originalItem.quantity + returnedQuantity,
                    isReturned = false
                )
            }
        }

        // Required BIR Header
        sb.appendLine("ELJIN CORP")
        sb.appendLine("TIN: Your TIN Number")
        sb.appendLine("MIN: Your MIN")
        sb.appendLine("Store: ${transaction.store}")
        sb.appendLine("-".repeat(45))

        // Receipt Type
        when (receiptType) {
            BluetoothPrinterHelper.ReceiptType.AR -> {
                sb.appendLine("AR RECEIPT - $copyType")
            }
            BluetoothPrinterHelper.ReceiptType.REPRINT -> sb.appendLine("REPRINT RECEIPT")
            BluetoothPrinterHelper.ReceiptType.RETURN -> {
                sb.appendLine("RETURN RECEIPT")
            }
            else -> sb.appendLine("OFFICIAL RECEIPT")
        }

        // Transaction Info
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sb.appendLine("Cashier: ${transaction.staff}")
        sb.appendLine("Date: ${dateFormat.format(transaction.createdDate)}")
        sb.appendLine("SI#: ${transaction.receiptId}")
        sb.appendLine("-".repeat(45))

        // Items Section
        sb.appendLine("Item                    Price   Qty    Total")
        sb.appendLine("-".repeat(45))

        items.forEach { item ->
            val effectivePrice = if (item.priceOverride!! > 0.0) item.priceOverride else item.price
            val itemTotal = effectivePrice * item.quantity

            // Item format for 45 character width
            sb.appendLine(
                "${item.name.take(22).padEnd(22)} ${
                    String.format(
                        "%7.2f",
                        effectivePrice
                    )
                } ${String.format("%3d", item.quantity)} ${String.format("%9.2f", itemTotal)}"
            )

            // Discounts
            when (item.discountType.uppercase()) {
                "PERCENTAGE", "PWD", "SC" -> {
                    val discountAmount = itemTotal * (item.discountRate)
                    if (discountAmount > 0) {
                        sb.appendLine(
                            " Disc(${item.discountType}):${
                                String.format(
                                    "%22.2f",
                                    discountAmount
                                )
                            }"
                        )
                    }
                }
                "FIXED", "FIXEDTOTAL" -> {
                    if (item.discountAmount > 0) {
                        sb.appendLine(
                            " Disc(Fixed):${
                                String.format(
                                    "%25.2f",
                                    item.discountAmount
                                )
                            }"
                        )
                    }
                }
            }
        }

        sb.appendLine("-".repeat(45))

        // Totals
        sb.appendLine("Gross Amount:${String.format("%27.2f", transaction.grossAmount)}")
        if (transaction.totalDiscountAmount > 0) {
            sb.appendLine(
                "Less Discount:${
                    String.format(
                        "%26.2f",
                        transaction.totalDiscountAmount
                    )
                }"
            )
        }
        sb.appendLine("Net Amount:${String.format("%29.2f", transaction.netAmount)}")

        sb.appendLine("Amount Paid:${String.format("%28.2f", transaction.totalAmountPaid)}")

        // Payment Details
        if (!isAR) {
            sb.appendLine("Change:${String.format("%33.2f", transaction.changeGiven)}")

            // Add Total Paid for partial payments
            if (transaction.partialPayment > 0) {
                sb.appendLine(
                    "Total Paid:${
                        String.format(
                            "%30.2f",
                            (transaction.partialPayment + transaction.totalAmountPaid) - transaction.changeGiven
                        )
                    }"
                )
            }
        }

        // Payment Method information
        sb.appendLine("-".repeat(45))
        sb.appendLine("Payment Method: ${transaction.paymentMethod}")
        if (transaction.cash > 0) {
            sb.appendLine("Cash Payment:${String.format("%28.2f", transaction.cash)}")
        }
        if (transaction.card > 0) {
            sb.appendLine("Card Payment:${String.format("%28.2f", transaction.card)}")
        }
        if (transaction.gCash > 0) {
            sb.appendLine("GCash Payment:${String.format("%27.2f", transaction.gCash)}")
        }
        if (transaction.payMaya > 0) {
            sb.appendLine("PayMaya Payment:${String.format("%25.2f", transaction.payMaya)}")
        }
        if (transaction.charge > 0) {
            sb.appendLine("Charge Payment:${String.format("%26.2f", transaction.charge)}")
        }
        if (transaction.foodpanda > 0) {
            sb.appendLine("Foodpanda Payment:${String.format("%23.2f", transaction.foodpanda)}")
        }
        if (transaction.grabfood > 0) {
            sb.appendLine("Grabfood Payment:${String.format("%24.2f", transaction.grabfood)}")
        }
        if (transaction.representation > 0) {
            sb.appendLine("Representation Payment:${String.format("%18.2f", transaction.representation)}")
        }

        // VAT Information
        sb.appendLine("-".repeat(45))
        sb.appendLine("VATable Sales:${String.format("%26.2f", transaction.vatableSales)}")
        sb.appendLine("VAT Amount:${String.format("%29.2f", transaction.vatAmount)}")
        sb.appendLine("VAT Exempt:${String.format("%29.2f", 0.0)}")
        sb.appendLine("Zero Rated:${String.format("%29.2f", 0.0)}")

        // Footer
        sb.appendLine("-".repeat(45))
        if (!isReturn) {
            sb.appendLine("ID/PWD/OSCA#:")
            sb.appendLine("Name:")
            sb.appendLine("Signature:")
        }
        if ((!transaction.customerAccount.isNullOrBlank() && transaction.customerAccount != "Walk-in Customer") ||
            (!transaction.customerName.isNullOrBlank() && transaction.customerName != "Walk-in Customer")
        ) {
            sb.appendLine("-".repeat(45))
            sb.appendLine("Customer Account: ${transaction.customerAccount}")
            sb.appendLine("Customer Name: ${transaction.customerName ?: "N/A"}")
        }

        sb.appendLine("-".repeat(45))
        sb.appendLine("Valid for 5 years from PTU date")
        sb.appendLine("POS Provider: Ray and Mark")
        sb.appendLine("-".repeat(45))
        sb.appendLine("Thank you for shopping at BW Superbakeshop!")

        // Additional info for AR receipts
        if (isAR && copyType != null) {
            sb.appendLine("-".repeat(45))
            sb.appendLine(copyType)
        }

        // Add original receipt info for returns
        if (isReturn) {
            sb.appendLine("-".repeat(45))
            sb.appendLine("Original Receipt #: ${transaction.receiptId}")
            if (!transaction.comment.isNullOrBlank()) {
                sb.appendLine("Return Reason: ${transaction.comment}")
            }
        }

        return sb.toString()
    }

    private fun buildReadReport(
        transactions: List<TransactionSummary>,
        isZRead: Boolean,
        zReportId: String = "",
        tenderDeclaration: TenderDeclaration? = null
    ): String {
        // Calculate all totals from TransactionSummary records
        val totalSales = transactions.sumOf { it.netAmount }
        val totalGross = transactions.sumOf { it.grossAmount }
        val totalVat = transactions.sumOf { it.taxIncludedInPrice }
        val totalDiscount = transactions.sumOf { it.totalDiscountAmount }
        val vatableSales = transactions.sumOf { it.vatableSales }
        val vatExemptSales = transactions.sumOf { it.vatExemptAmount }

        // Payment method totals using specific fields
        val cashTotal = transactions.sumOf { it.cash }
        val cardTotal = transactions.sumOf { it.card }
        val gCashTotal = transactions.sumOf { it.gCash }
        val payMayaTotal = transactions.sumOf { it.payMaya }
        val loyaltyCardTotal = transactions.sumOf { it.loyaltyCard }
        val chargeTotal = transactions.sumOf { it.charge }
        val foodPandaTotal = transactions.sumOf { it.foodpanda }
        val grabFoodTotal = transactions.sumOf { it.grabfood }
        val representationTotal = transactions.sumOf { it.representation }
        val arSales = transactions.filter { it.type == 3 }.sumOf { it.netAmount }

        // Transaction statistics
        val totalReturns = transactions.filter { it.type == 2 }.sumOf { it.netAmount }
        val totalItemsSold = transactions.filter { it.type != 2 }.sumOf { it.numberOfItems.toInt() }
        val totalItemsReturned = transactions.filter { it.type == 2 }.sumOf { it.numberOfItems.toInt() }

        val returnCommentsCount = transactions.count { transaction ->
            transaction.comment.contains("Returned:", ignoreCase = true) ||
                    transaction.comment.contains("Return processed:", ignoreCase = true)
        }

        return buildString {
            appendLine("BRANCH: ${getBranchName()}")
            appendLine("TARLAC CITY")
            appendLine("REG TIN: ${getRegTin()}")
            appendLine("Permit: ${getPermitNumber()}")
            appendLine("Min: ${getMinNumber()}")
            appendLine("Serial: ${getSerialNumber()}")
            appendLine("Operator: ${getOperatorName()}")
            appendLine("Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}")
            appendLine()

            if (isZRead) {
                appendLine("Z-READ ID: $zReportId")
                appendLine("Zreading")
            } else {
                appendLine("Xreading")
            }

            appendLine("_".repeat(45))
            appendLine("SALES REPORT AMT INC                  AMOUNT")
            appendLine("Gross sales${String.format("%29.2f", totalGross)}")
            appendLine("Purchase${String.format("%32.2f", totalSales)}")
            appendLine("Total Discount${String.format("%26.2f", totalDiscount)}")
            appendLine("Total netsales${String.format("%27.2f", totalSales)}")

            appendLine("-".repeat(45))
            appendLine("Statistics                                  qty")
            appendLine("-".repeat(45))
            appendLine("No. of sales trans${String.format("%24d", transactions.size)}")
            appendLine("No. of items sold${String.format("%25d", totalItemsSold)}")
            appendLine("No. of void trans${String.format("%25d", returnCommentsCount)}")

            appendLine("-".repeat(45))
            appendLine("Tender reports")
            appendLine("-".repeat(45))
            appendLine("Tender name                          amount")
            appendLine("_".repeat(45))

            if (cashTotal > 0) appendLine("CASH".padEnd(35) + String.format("%10.2f", cashTotal))
            if (cardTotal > 0) appendLine("CARD".padEnd(35) + String.format("%10.2f", cardTotal))
            if (gCashTotal > 0) appendLine("GCASH".padEnd(35) + String.format("%10.2f", gCashTotal))
            if (payMayaTotal > 0) appendLine("PAYMAYA".padEnd(35) + String.format("%10.2f", payMayaTotal))
            if (loyaltyCardTotal > 0) appendLine("LOYALTY CARD".padEnd(35) + String.format("%10.2f", loyaltyCardTotal))
            if (chargeTotal > 0) appendLine("CHARGE".padEnd(35) + String.format("%10.2f", chargeTotal))
            if (foodPandaTotal > 0) appendLine("FOODPANDA".padEnd(35) + String.format("%10.2f", foodPandaTotal))
            if (grabFoodTotal > 0) appendLine("GRABFOOD".padEnd(35) + String.format("%10.2f", grabFoodTotal))
            if (representationTotal > 0) appendLine("REPRESENTATION".padEnd(35) + String.format("%10.2f", representationTotal))
            if (arSales > 0) appendLine("AR".padEnd(35) + String.format("%10.2f", arSales))

            appendLine()
            appendLine("total amount${String.format("%29.2f", totalSales)}")

            appendLine("-".repeat(45))
            appendLine("Tax report")
            appendLine("-".repeat(45))
            appendLine("Vatable sales${String.format("%28.2f", vatableSales)}")
            appendLine("vat amount${String.format("%30.2f", totalVat)}")
            appendLine("vat exempt sales${String.format("%25.2f", vatExemptSales)}")
            appendLine("zero Rated sales${String.format("%25.2f", 0.0)}")

            if (tenderDeclaration != null) {
                appendLine("-".repeat(45))
                appendLine("Tender Declaration")

                // Function to format amount entries with proper spacing
                fun formatAmountEntry(label: String, amount: Double): String {
                    val maxLabelLength = 35
                    val truncatedLabel = if (label.length > maxLabelLength) {
                        label.substring(0, maxLabelLength - 3) + "..."
                    } else {
                        label
                    }

                    return "${truncatedLabel.padEnd(35)}${String.format("%10.2f", amount)}"
                }

                // Print cash amount
                appendLine(formatAmountEntry("Cash Amount", tenderDeclaration.cashAmount))

                // Process AR amounts
                val gson = Gson()
                val arAmounts = gson.fromJson<Map<String, Double>>(
                    tenderDeclaration.arAmounts,
                    object : TypeToken<Map<String, Double>>() {}.type
                )
                var totalAr = 0.0

                // Print AR entries
                arAmounts.forEach { (arType, amount) ->
                    if (arType != "Cash") {
                        appendLine(formatAmountEntry(arType, amount))
                        totalAr += amount
                    }
                }

                // Print totals with consistent alignment
                appendLine("-".repeat(45))
                appendLine(formatAmountEntry("Total AR", totalAr))

                val totalDeclared = tenderDeclaration.cashAmount + totalAr
                appendLine(formatAmountEntry("Total Declared", totalDeclared))
                appendLine(formatAmountEntry("Short/Over", totalDeclared - totalSales))
            }

            appendLine("-".repeat(45))
            appendLine("Pos Provider: IT WARRIOR")
            appendLine("ELJIN CORP")
            appendLine("tin: ${getPosProviderTin()}")
            appendLine("acc No: ${getPosProviderAccNo()}")
            appendLine("Date issued: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}")
            appendLine("valid until: ${getValidUntilDate()}")

            if (isZRead) {
                appendLine()
                appendLine("-".repeat(45))
                appendLine("End of Z-Read Report")
            }
        }
    }

    fun logTenderDeclaration(tenderDeclaration: TenderDeclaration) {
        val gson = Gson()
        val arAmounts = gson.fromJson<Map<String, Double>>(
            tenderDeclaration.arAmounts,
            object : TypeToken<Map<String, Double>>() {}.type
        )

        val logEntry = buildString {
            appendLine("TENDER DECLARATION RECEIPT")
            appendLine("Date: ${getCurrentDate()}")
            appendLine("Time: ${getCurrentTime()}")
            appendLine("-".repeat(45))
            appendLine("Cash Amount:${String.format("%30.2f", tenderDeclaration.cashAmount)}")
            appendLine("-".repeat(45))
            appendLine("AR Amounts:")
            var totalAr = 0.0
            arAmounts.forEach { (arType, amount) ->
                if (arType != "Cash" && amount > 0) {
                    appendLine("${arType}:${String.format("%38.2f", amount)}")
                    totalAr += amount
                }
            }
            appendLine("-".repeat(45))
            appendLine("Total AR:${String.format("%33.2f", totalAr)}")
            appendLine("Total Amount:${String.format("%29.2f", tenderDeclaration.cashAmount + totalAr)}")
            appendLine("-".repeat(45))
            appendLine("ELJIN CORP")
            appendLine("POS Provider: IT WARRIOR")
        }

        logToFile(logEntry)
    }

    fun logCashFund(cashFund: Double, status: String) {
        val logEntry = buildString {
            appendLine("CASH FUND RECEIPT")
            appendLine("Date: ${getCurrentDate()}")
            appendLine("Time: ${getCurrentTime()}")
            appendLine("Amount:${String.format("%34.2f", cashFund)}")
            appendLine("Status: $status")
            appendLine("-".repeat(45))
            appendLine("ELJIN CORP")
            appendLine("POS Provider: IT WARRIOR")
        }

        logToFile(logEntry)
    }

    fun logPulloutCash(pulloutAmount: Double) {
        val logEntry = buildString {
            appendLine("PULL-OUT CASH FUND RECEIPT")
            appendLine("Date: ${getCurrentDate()}")
            appendLine("Time: ${getCurrentTime()}")
            appendLine("Amount:${String.format("%34.2f", pulloutAmount)}")
            appendLine("-".repeat(45))
            appendLine("ELJIN CORP")
            appendLine("POS Provider: IT WARRIOR")
        }

        logToFile(logEntry)
    }

    // Helper functions
    private fun getBranchName(): String = "ELJIN CORP"
    private fun getRegTin(): String = "Your Reg TIN"
    private fun getPermitNumber(): String = "Permit Number"
    private fun getMinNumber(): String = "MIN Number"
    private fun getSerialNumber(): String = "Serial Number"
    private fun getOperatorName(): String = "Operator Name"
    private fun getPosProviderTin(): String = "POS Provider TIN"
    private fun getPosProviderAccNo(): String = "POS Provider ACC Number"
    private fun getValidUntilDate(): String = SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.getDefault()
    ).format(Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L))
}