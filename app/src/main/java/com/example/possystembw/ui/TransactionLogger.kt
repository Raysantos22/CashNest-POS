package com.example.possystembw.ui

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.possystembw.DAO.TransactionDao
import com.example.possystembw.database.TenderDeclaration
import com.example.possystembw.database.TransactionRecord
import com.example.possystembw.database.TransactionSummary
import com.example.possystembw.ui.SecureTime.getCurrentTime
import com.example.possystembw.ui.ViewModel.BluetoothPrinterHelper
import com.example.possystembw.ui.ViewModel.BluetoothPrinterHelper.ItemCalculationResult
import com.example.possystembw.ui.ViewModel.BluetoothPrinterHelper.ItemSalesSummary
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

class TransactionLogger(private val context: Context) {
    private lateinit var transactionDao: TransactionDao

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
    private fun formatTransactionDate(dateString: String): String {
        return try {
            if (dateString.isEmpty()) {
                return "Unknown Date"
            }

            // Try multiple input formats for the string date
            val inputFormats = listOf(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss"
            )

            val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            for (format in inputFormats) {
                try {
                    val inputFormat = SimpleDateFormat(format, Locale.US)
                    inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                    val date = inputFormat.parse(dateString)
                    if (date != null) {
                        return outputFormat.format(date)
                    }
                } catch (e: Exception) {
                    // Try next format
                    continue
                }
            }

            // If all parsing fails, return the original string
            dateString
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date: ${e.message}")
            dateString.ifEmpty { "Unknown Date" }
        }
    }
    fun generateReceiptContent(
        transaction: TransactionSummary,
        items: List<TransactionRecord>,
        receiptType: BluetoothPrinterHelper.ReceiptType,
        isARReceipt: Boolean = false,
        copyType: String? = null
    ): String {
        val sb = StringBuilder()
        sb.append(0x1B.toChar()) // ESC
        sb.append('!'.toChar())  // Select print mode
        sb.append(0x01.toChar()) // Smallest text size

        // Set minimum line spacing
        sb.append(0x1B.toChar()) // ESC
        sb.append('3'.toChar())  // Select line spacing
        sb.append(50.toChar())
        sb.append("ELJIN CORP")
        val isReturn = receiptType == BluetoothPrinterHelper.ReceiptType.RETURN

        // Determine if this is a pure AR transaction or has mixed payments
        val hasOtherPayments = transaction.cash > 0 || transaction.gCash > 0 ||
                transaction.payMaya > 0 || transaction.card > 0 ||
                transaction.loyaltyCard > 0 || transaction.charge > 0 ||
                transaction.foodpanda > 0 || transaction.grabfood > 0 ||
                transaction.representation > 0

        val isPureAR = transaction.type == 3 && !hasOtherPayments
        val isMixedPayment = hasOtherPayments && (transaction.type == 3 || isARReceipt)

        // Required BIR Header
        sb.appendLine()
        sb.appendLine("TIN: Your TIN Number")
        sb.appendLine("MIN: Your MIN")
        sb.appendLine("Store: ${transaction.store}")
        sb.appendLine("-".repeat(45))

        // Receipt Type
        when (receiptType) {
            BluetoothPrinterHelper.ReceiptType.AR -> {
                if (isMixedPayment) {
                    sb.appendLine("MIXED PAYMENT RECEIPT - $copyType")
                } else {
                    sb.appendLine("AR RECEIPT - $copyType")
                }
            }
            BluetoothPrinterHelper.ReceiptType.REPRINT -> sb.appendLine("REPRINT RECEIPT")
            BluetoothPrinterHelper.ReceiptType.RETURN -> {
                sb.appendLine("RETURN RECEIPT")
            }
            else -> {
                if (isMixedPayment) {
                    sb.appendLine("MIXED PAYMENT RECEIPT")
                } else if (isPureAR) {
                    sb.appendLine("AR RECEIPT")
                } else {
                    sb.appendLine("OFFICIAL RECEIPT")
                }
            }
        }

        // Transaction Info
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sb.appendLine("Cashier: ${transaction.staff}")
        sb.appendLine("Date: ${formatTransactionDate(transaction.createdDate)}")
        sb.appendLine("SI#: ${transaction.receiptId}")

        // For return receipts, show original transaction reference
        if (isReturn && !transaction.refundReceiptId.isNullOrEmpty()) {
            sb.appendLine("Original SI#: ${transaction.refundReceiptId}")
        }

        sb.appendLine("-".repeat(45))

        // Items Section
        sb.appendLine("Item                    Price   Qty    Total")
        sb.appendLine("-".repeat(45))

        items.forEach { item ->
            val effectivePrice = if (item.priceOverride != null && item.priceOverride!! > 0.0) {
                item.priceOverride!!
            } else {
                item.price
            }

            // For returns, show the actual returned quantity and amounts
            val displayQuantity = if (isReturn) kotlin.math.abs(item.quantity) else item.quantity
            val displayPrice = if (isReturn) effectivePrice else effectivePrice
            val itemTotal = if (isReturn) {
                kotlin.math.abs(effectivePrice * item.quantity) // Show positive amount for display
            } else {
                effectivePrice * item.quantity
            }

            // Item format for 45 character width
            sb.appendLine(
                "${item.name.take(22).padEnd(22)} ${
                    String.format("%7.2f", displayPrice)
                } ${String.format("%3d", displayQuantity)} ${String.format("%9.2f", itemTotal)}"
            )

            // Discounts (show positive amounts for readability)
            when (item.discountType?.uppercase()) {
                "PERCENTAGE", "PWD", "SC" -> {
                    val discountAmount = if (isReturn) {
                        kotlin.math.abs(itemTotal * (item.discountRate ?: 0.0))
                    } else {
                        itemTotal * (item.discountRate ?: 0.0)
                    }
                    if (discountAmount > 0) {
                        sb.appendLine(
                            " Disc(${item.discountType}):${String.format("%22.2f", discountAmount)}"
                        )
                    }
                }
                "FIXED", "FIXEDTOTAL" -> {
                    val discountAmount = if (isReturn) {
                        kotlin.math.abs(item.discountAmount ?: 0.0)
                    } else {
                        item.discountAmount ?: 0.0
                    }
                    if (discountAmount > 0) {
                        sb.appendLine(
                            " Disc(Fixed):${String.format("%25.2f", discountAmount)}"
                        )
                    }
                }
            }
        }

        sb.appendLine("-".repeat(45))

        // Totals - for returns, show positive amounts for readability but indicate they are returns
        if (isReturn) {
            sb.appendLine("Return Amount:${String.format("%25.2f", kotlin.math.abs(transaction.grossAmount))}")
            if (transaction.totalDiscountAmount != 0.0) {
                sb.appendLine("Less Discount:${String.format("%26.2f", kotlin.math.abs(transaction.totalDiscountAmount))}")
            }
            sb.appendLine("Net Return:${String.format("%27.2f", kotlin.math.abs(transaction.netAmount))}")
            sb.appendLine("Refund Amount:${String.format("%25.2f", kotlin.math.abs(transaction.totalAmountPaid))}")
        } else {
            sb.appendLine("Gross Amount:${String.format("%27.2f", transaction.grossAmount)}")
            if (transaction.totalDiscountAmount > 0) {
                sb.appendLine("Less Discount:${String.format("%26.2f", transaction.totalDiscountAmount)}")
            }
            sb.appendLine("Net Amount:${String.format("%29.2f", transaction.netAmount)}")
            sb.appendLine("Amount Paid:${String.format("%28.2f", transaction.totalAmountPaid)}")

            // Payment Method Details Section - Show for all non-return transactions
            if (!isReturn) {
                sb.appendLine("-".repeat(45))
                sb.appendLine("PAYMENT DETAILS")
                sb.appendLine("-".repeat(45))

                // Check for all payment methods used - Include AR calculation
                val paymentMethods = mutableListOf<Pair<String, Double>>()

                if (transaction.cash > 0) paymentMethods.add("Cash" to transaction.cash)
                if (transaction.gCash > 0) paymentMethods.add("GCash" to transaction.gCash)
                if (transaction.payMaya > 0) paymentMethods.add("PayMaya" to transaction.payMaya)
                if (transaction.card > 0) paymentMethods.add("Card" to transaction.card)
                if (transaction.loyaltyCard > 0) paymentMethods.add("Loyalty Card" to transaction.loyaltyCard)
                if (transaction.charge > 0) paymentMethods.add("Charge" to transaction.charge)
                if (transaction.foodpanda > 0) paymentMethods.add("FoodPanda" to transaction.foodpanda)
                if (transaction.grabfood > 0) paymentMethods.add("GrabFood" to transaction.grabfood)
                if (transaction.representation > 0) paymentMethods.add("Representation" to transaction.representation)

                // Calculate AR amount (difference between net amount and cash payments)
                val totalCashPayments = paymentMethods.sumOf { it.second }
                val arAmount = transaction.netAmount - totalCashPayments

                // Add AR payment if there's an outstanding amount or if it's an AR transaction
                if (arAmount > 0.01 || (transaction.type == 3 && paymentMethods.isEmpty())) {
                    paymentMethods.add("Accounts Receivable" to if (arAmount > 0.01) arAmount else transaction.netAmount)
                }

                // Check if this is a split payment or has partial payment
                val isSplitPayment = paymentMethods.size > 1
                val hasPartialPayment = transaction.partialPayment > 0

                if (isSplitPayment) {
                    sb.appendLine("SPLIT PAYMENT:")
                    paymentMethods.forEach { (method, amount) ->
                        sb.appendLine("${method.padEnd(25)} ${String.format("%12.2f", amount)}")
                    }
                    sb.appendLine("-".repeat(45))
                    sb.appendLine("Total Amount:${String.format("%26.2f", transaction.netAmount)}")

                    // Show breakdown of payments
                    val cashPayments = paymentMethods.filter { it.first != "Accounts Receivable" }
                    val arPayments = paymentMethods.filter { it.first == "Accounts Receivable" }

                    if (cashPayments.isNotEmpty()) {
                        val totalPaid = cashPayments.sumOf { it.second }
                        sb.appendLine("Amount Paid:${String.format("%28.2f", totalPaid)}")
                    }
                    if (arPayments.isNotEmpty()) {
                        val totalAR = arPayments.sumOf { it.second }
                        sb.appendLine("On Account:${String.format("%30.2f", totalAR)}")
                    }

                } else if (paymentMethods.isNotEmpty()) {
                    val (method, amount) = paymentMethods.first()
                    sb.appendLine("Payment Method: $method")
                    if (method == "Accounts Receivable") {
                        sb.appendLine("On Account:${String.format("%30.2f", amount)}")
                        sb.appendLine("Amount Paid:${String.format("%28.2f", 0.0)}")
                    } else {
                        sb.appendLine("Amount Paid:${String.format("%28.2f", amount)}")
                    }
                }

                // Handle partial payments
                if (hasPartialPayment) {
                    sb.appendLine("-".repeat(45))
                    sb.appendLine("PARTIAL PAYMENT SUMMARY:")
                    sb.appendLine("Previous Payment:${String.format("%24.2f", transaction.partialPayment)}")
                    sb.appendLine("This Payment:${String.format("%28.2f", transaction.totalAmountPaid)}")
                    sb.appendLine("Total Paid:${String.format("%30.2f", transaction.partialPayment + transaction.totalAmountPaid)}")
                }

                // Only show change for non-AR transactions or when change is actually given
                if (!isPureAR || transaction.changeGiven > 0) {
                    sb.appendLine("Change:${String.format("%33.2f", transaction.changeGiven)}")
                }
            } else if (isReturn) {
                // For return transactions, show refund method details
                sb.appendLine("-".repeat(45))
                sb.appendLine("REFUND DETAILS")
                sb.appendLine("-".repeat(45))

                val refundMethods = mutableListOf<Pair<String, Double>>()

                if (transaction.cash < 0) refundMethods.add("Cash Refund" to kotlin.math.abs(transaction.cash))
                if (transaction.gCash < 0) refundMethods.add("GCash Refund" to kotlin.math.abs(transaction.gCash))
                if (transaction.payMaya < 0) refundMethods.add("PayMaya Refund" to kotlin.math.abs(transaction.payMaya))
                if (transaction.card < 0) refundMethods.add("Card Refund" to kotlin.math.abs(transaction.card))
                if (transaction.loyaltyCard < 0) refundMethods.add("Loyalty Refund" to kotlin.math.abs(transaction.loyaltyCard))
                if (transaction.charge < 0) refundMethods.add("Charge Refund" to kotlin.math.abs(transaction.charge))
                if (transaction.foodpanda < 0) refundMethods.add("FoodPanda Refund" to kotlin.math.abs(transaction.foodpanda))
                if (transaction.grabfood < 0) refundMethods.add("GrabFood Refund" to kotlin.math.abs(transaction.grabfood))
                if (transaction.representation < 0) refundMethods.add("Representation Refund" to kotlin.math.abs(transaction.representation))

                if (refundMethods.size > 1) {
                    sb.appendLine("SPLIT REFUND:")
                    refundMethods.forEach { (method, amount) ->
                        sb.appendLine("${method.padEnd(25)} ${String.format("%12.2f", amount)}")
                    }
                    sb.appendLine("-".repeat(45))
                    sb.appendLine("Total Refund:${String.format("%26.2f", refundMethods.sumOf { it.second })}")
                } else if (refundMethods.isNotEmpty()) {
                    val (method, amount) = refundMethods.first()
                    sb.appendLine("Refund Method: ${method.replace(" Refund", "")}")
                    sb.appendLine("Refund Amount:${String.format("%26.2f", amount)}")
                }
            }
        }

        // VAT Information - show positive amounts for readability
        sb.appendLine("-".repeat(45))
        if (isReturn) {
            sb.appendLine("VATable Sales:${String.format("%26.2f", kotlin.math.abs(transaction.vatableSales))}")
            sb.appendLine("VAT Amount:${String.format("%29.2f", kotlin.math.abs(transaction.vatAmount))}")
        } else {
            sb.appendLine("VATable Sales:${String.format("%26.2f", transaction.vatableSales)}")
            sb.appendLine("VAT Amount:${String.format("%29.2f", transaction.vatAmount)}")
        }
        sb.appendLine("VAT Exempt:${String.format("%29.2f", 0.0)}")

        // Footer
        sb.appendLine("-".repeat(45))
        if (!isReturn) {
            sb.appendLine("ID/PWD/OSCA#:")
            sb.appendLine("Name:")
            sb.appendLine("Signature:")
            sb.appendLine("Comment: ${transaction.comment ?: "N/A"}")

        } else {
            sb.appendLine("Return processed on: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            if (!transaction.comment.isNullOrEmpty()) {
                sb.appendLine("Return Reason: ${transaction.comment}")
            }
        }

        if ((!transaction.customerAccount.isNullOrBlank() && transaction.customerAccount != "Walk-in Customer") ||
            (!transaction.customerName.isNullOrBlank() && transaction.customerName != "Walk-in Customer")
        ) {
            sb.appendLine("-".repeat(45))
            sb.appendLine("Customer Account: ${transaction.customerAccount}")
            sb.appendLine("Customer Name: ${transaction.customerName ?: "N/A"}")

            sb.appendLine("-".repeat(45))
        }

        sb.appendLine("Valid for 5 years from PTU date")
        sb.appendLine("POS Provider: Ray and Mark")
        sb.appendLine("-".repeat(45))

        // Thank you message (different for returns)
        if (isReturn) {
            sb.appendLine("Return processed successfully.")
            sb.appendLine("Thank you for your understanding.")
        } else {
            sb.appendLine("Thank you for shopping at BW Superbakeshop!")
            sb.appendLine("Please rate your experience:")
            sb.appendLine()

            // Generate QR Code (only for regular receipts)
            // ESC/POS commands for QR Code
            sb.append(0x1D.toChar())  // GS
            sb.append('('.toChar())   // (
            sb.append('k'.toChar())   // k
            sb.append(4.toChar())     // pl
            sb.append(0.toChar())     // ph
            sb.append(49.toChar())    // cn
            sb.append(65.toChar())    // fn
            sb.append(50.toChar())    // Error correction level [1-4] (2 = 15%)

            // Set QR Code module size
            sb.append(0x1D.toChar())
            sb.append('('.toChar())
            sb.append('k'.toChar())
            sb.append(3.toChar())
            sb.append(0.toChar())
            sb.append(49.toChar())
            sb.append(67.toChar())
            sb.append(4.toChar())     // Size (1-16) - using 4 for medium size

            // Store QR Code data
            val qrData = "https://feedback.eljincorp.com/${transaction.receiptId}"
            sb.append(0x1D.toChar())
            sb.append('('.toChar())
            sb.append('k'.toChar())
            sb.append((qrData.length + 3).toChar())
            sb.append(0.toChar())
            sb.append(49.toChar())
            sb.append(80.toChar())
            sb.append(48.toChar())
            sb.append(qrData)

            // Print QR Code
            sb.append(0x1D.toChar())
            sb.append('('.toChar())
            sb.append('k'.toChar())
            sb.append(3.toChar())
            sb.append(0.toChar())
            sb.append(49.toChar())
            sb.append(81.toChar())
            sb.append(48.toChar())

            sb.appendLine()
            sb.appendLine("Scan to rate us!")
        }

        // Reset to normal text size
        sb.append(0x1B.toChar()) // ESC
        sb.append('!'.toChar())  // Select print mode
        sb.append(0x00.toChar()) // Reset to normal size

        sb.append(0x1B.toChar()) // ESC

        return sb.toString().trimEnd()
    }
    private suspend fun calculateItemTotals(transactions: List<TransactionSummary>): ItemCalculationResult {
        return withContext(Dispatchers.IO) {
            val itemSales = mutableMapOf<String, ItemSalesSummary>()
            var totalGross = 0.0
            var totalQuantity = 0

            transactions.forEach { transaction ->
                val items = transactionDao.getTransactionRecordsByTransactionId(transaction.transactionId)
                items.forEach { item ->
                    val key = item.name

                    // Use consistent pricing logic (same as ReportsActivity)
                    val effectivePrice = if (item.priceOverride != null && item.priceOverride > 0.0) {
                        item.priceOverride
                    } else {
                        item.price
                    }

                    val itemTotal = effectivePrice * item.quantity

                    // Add to gross total
                    totalGross += itemTotal
                    totalQuantity += item.quantity

                    // Update item sales summary
                    val currentSummary = itemSales.getOrDefault(key, ItemSalesSummary(
                        name = item.name,
                        quantity = 0,
                        totalAmount = 0.0
                    )
                    )

                    itemSales[key] = currentSummary.copy(
                        quantity = currentSummary.quantity + item.quantity,
                        totalAmount = currentSummary.totalAmount + itemTotal
                    )
                }
            }

            ItemCalculationResult(
                totalGross = totalGross,
                totalQuantity = totalQuantity,
                itemSales = itemSales.values.sortedByDescending { it.totalAmount }
            )
        }
    }

    fun buildReadReport(
        transactions: List<TransactionSummary>,
        isZRead: Boolean,
        zReportId: String = "",
        tenderDeclaration: TenderDeclaration? = null
    ): String = runBlocking {

        // Separate regular transactions and returns for proper calculation
        val regularTransactions = transactions.filter { it.transactionStatus == 1 && it.type != 2 }
        val returnTransactions = transactions.filter { it.type == 2 }

        // Calculate item totals using the same method as ReportsActivity
        val regularItemCalculations = calculateItemTotals(regularTransactions)
        val returnItemCalculations = calculateItemTotals(returnTransactions)

        // Net calculations (regular sales minus returns)
        val totalGross = regularItemCalculations.totalGross + returnItemCalculations.totalGross // returns are already negative
        val totalSales = regularTransactions.sumOf { it.netAmount } + returnTransactions.sumOf { it.netAmount }
        val totalVat = regularTransactions.sumOf { it.taxIncludedInPrice } + returnTransactions.sumOf { it.taxIncludedInPrice }
        val totalDiscount = regularTransactions.sumOf { it.totalDiscountAmount } + returnTransactions.sumOf { it.totalDiscountAmount }
        val vatableSales = regularTransactions.sumOf { it.vatableSales } + returnTransactions.sumOf { it.vatableSales }
        val vatExemptSales = regularTransactions.sumOf { it.vatExemptAmount } + returnTransactions.sumOf { it.vatExemptAmount }

        // Calculate total return amount (absolute value for display)
        val totalReturnAmount = kotlin.math.abs(returnTransactions.sumOf { it.netAmount })
        val totalReturnDiscount = kotlin.math.abs(returnTransactions.sumOf { it.totalDiscountAmount })

        // Payment method totals including returns (returns should reduce totals)
        val cashTotal = regularTransactions.sumOf { it.cash } + returnTransactions.sumOf { it.cash }
        val cardTotal = regularTransactions.sumOf { it.card } + returnTransactions.sumOf { it.card }
        val gCashTotal = regularTransactions.sumOf { it.gCash } + returnTransactions.sumOf { it.gCash }
        val payMayaTotal = regularTransactions.sumOf { it.payMaya } + returnTransactions.sumOf { it.payMaya }
        val loyaltyCardTotal = regularTransactions.sumOf { it.loyaltyCard } + returnTransactions.sumOf { it.loyaltyCard }
        val chargeTotal = regularTransactions.sumOf { it.charge } + returnTransactions.sumOf { it.charge }
        val foodPandaTotal = regularTransactions.sumOf { it.foodpanda } + returnTransactions.sumOf { it.foodpanda }
        val grabFoodTotal = regularTransactions.sumOf { it.grabfood } + returnTransactions.sumOf { it.grabfood }
        val representationTotal = regularTransactions.sumOf { it.representation } + returnTransactions.sumOf { it.representation }

        // AR Sales calculation (only regular AR transactions minus returned AR)
        val arSales = regularTransactions.filter { it.type == 3 }.sumOf { it.netAmount } +
                returnTransactions.filter { it.type == 3 }.sumOf { it.netAmount }

        // Transaction statistics
        val totalItemsSold = regularItemCalculations.totalQuantity + returnItemCalculations.totalQuantity // returns have negative quantities
        val totalItemsReturned = kotlin.math.abs(returnTransactions.sumOf { it.numberOfItems.toInt() })
        val returnTransactionCount = returnTransactions.size
        val customerTransactions = regularTransactions.count { it.customerAccount.isNotBlank() }

        // FIXED: Get OR numbers for starting and ending (handle string dates properly)
        val startingOR = if (regularTransactions.isNotEmpty()) {
            try {
                // Sort by createdDate string and get first transaction ID
                val sortedTransactions = regularTransactions.sortedBy {
                    parseTransactionDateForSorting(it.createdDate)
                }
                extractORFromTransactionId(sortedTransactions.first().transactionId) ?: "N/A"
            } catch (e: Exception) {
                "N/A"
            }
        } else "N/A"

        val endingOR = if (regularTransactions.isNotEmpty()) {
            try {
                // Sort by createdDate string and get last transaction ID
                val sortedTransactions = regularTransactions.sortedBy {
                    parseTransactionDateForSorting(it.createdDate)
                }
                extractORFromTransactionId(sortedTransactions.last().transactionId) ?: "N/A"
            } catch (e: Exception) {
                "N/A"
            }
        } else "N/A"

        return@runBlocking buildString {
            append(0x1B.toChar()) // ESC
            append('!'.toChar())  // Select print mode
            append(0x01.toChar()) // Smallest text size

            // Set minimum line spacing
            append(0x1B.toChar()) // ESC
            append('3'.toChar())  // Select line spacing
            append(50.toChar())
            appendLine("BRANCH: ${getBranchName()}")
            appendLine("TARLAC CITY")
            appendLine("REG TIN: ${getRegTin()}")
            appendLine("Permit: ${getPermitNumber()}")
            appendLine("Min: ${getMinNumber()}")
            appendLine("Serial: ${getSerialNumber()}")
            appendLine("Operator: ${getOperatorName()}")
            appendLine(
                "Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}"
            )
            appendLine()

            if (isZRead) {
                appendLine("Z-READ ID: $zReportId")
                appendLine("-".repeat(46))
                appendLine("Zreading")
                appendLine("-".repeat(46))
                appendLine("Starting OR: $startingOR")
                appendLine("Ending   OR: $endingOR")
            } else {
                appendLine("Xreading")
            }

            appendLine("_".repeat(46))
            appendLine("SALES REPORT AMT INC                  AMOUNT")
            appendLine("Gross sales                           ${"%.2f".format(regularItemCalculations.totalGross- (totalReturnAmount + totalReturnDiscount))}")
            appendLine("Total netsales                        ${"%.2f".format(totalSales)}")
            appendLine("Total Discount                        ${"%.2f".format(kotlin.math.abs(totalDiscount))}")

            // Add return information if there are returns
            if (returnTransactionCount > 0) {
                appendLine("Total Returns                         ${"%.2f".format(totalReturnAmount)}")
                appendLine("Return Discounts                      ${"%.2f".format(totalReturnDiscount)}")
            }

            appendLine("-".repeat(46))
            appendLine("Statistics                            qty")
            appendLine("-".repeat(46))
            appendLine("No. of sales trans                    ${regularTransactions.size}")
            appendLine("No. of items sold                     ${regularItemCalculations.totalQuantity-totalItemsReturned}")

            // Add return information
            if (returnTransactionCount > 0) {
                appendLine("No. of return trans                   $returnTransactionCount")
                appendLine("No. of items returned                 $totalItemsReturned")
            }

            appendLine("-".repeat(46))
            appendLine("Tender reports")
            appendLine("-".repeat(46))
            appendLine("Tender name                          amount")
            appendLine("_".repeat(46))

            // Only show payment methods with non-zero amounts
            if (cashTotal != 0.0) appendLine("CASH".padEnd(38) + "%.2f".format(cashTotal))
            if (cardTotal != 0.0) appendLine("CARD".padEnd(38) + "%.2f".format(cardTotal))
            if (gCashTotal != 0.0) appendLine("GCASH".padEnd(38) + "%.2f".format(gCashTotal))
            if (payMayaTotal != 0.0) appendLine("PAYMAYA".padEnd(38) + "%.2f".format(payMayaTotal))
            if (loyaltyCardTotal != 0.0) appendLine("LOYALTY CARD".padEnd(38) + "%.2f".format(loyaltyCardTotal))
            if (chargeTotal != 0.0) appendLine("CHARGE".padEnd(38) + "%.2f".format(chargeTotal))
            if (foodPandaTotal != 0.0) appendLine("FOODPANDA".padEnd(38) + "%.2f".format(foodPandaTotal))
            if (grabFoodTotal != 0.0) appendLine("GRABFOOD".padEnd(38) + "%.2f".format(grabFoodTotal))
            if (representationTotal != 0.0) appendLine("REPRESENTATION".padEnd(38) + "%.2f".format(representationTotal))

            appendLine()
            appendLine("total amount                          ${"%.2f".format(totalSales)}")

            appendLine("-".repeat(46))
            appendLine("Tax report")
            appendLine("-".repeat(46))
            val vatRate = 0.12
            val vatableSalesCalculated = totalSales / (1 + vatRate)
            val vatAmountCalculated = totalSales - vatableSalesCalculated

            appendLine("Vatable sales                         ${"%.2f".format(vatableSalesCalculated)}")
            appendLine("VAT amount                            ${"%.2f".format(vatAmountCalculated)}")
            appendLine("vat exempt sales                      ${"%.2f".format(vatExemptSales)}")
            appendLine("zero Rated sales                      ${"%.2f".format(0.0)}")

            if (tenderDeclaration != null) {
                appendLine("-".repeat(46))
                appendLine("Tender Declaration")
                // ... existing tender declaration code ...
            }

            appendLine("-".repeat(46))
            appendLine("Pos Provider: IT WARRIOR")
            appendLine("ELJIN CORP")
            appendLine("tin: ${getPosProviderTin()}")
            appendLine("acc No: ${getPosProviderAccNo()}")
            appendLine(
                "Date issued: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}"
            )
            appendLine("valid until: ${getValidUntilDate()}")

            if (isZRead) {
                appendLine()
                appendLine("-".repeat(46))
                appendLine("End of Z-Read Report")
                append(0x1B.toChar()) // ESC
                append('!'.toChar())  // Select print mode
                append(0x00.toChar()) // Reset to normal size

                append(0x1B.toChar()) // ESC
                append('2'.toChar())
            }
        }
    }

    // FIXED: Helper function to parse transaction date string for sorting
    private fun parseTransactionDateForSorting(dateString: String): Long {
        return try {
            if (dateString.isEmpty()) return 0L

            val formats = listOf(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss"
            )

            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    val date = sdf.parse(dateString)
                    if (date != null) {
                        return date.time
                    }
                } catch (e: Exception) {
                    // Try next format
                }
            }

            0L
        } catch (e: Exception) {
            0L
        }
    }

    // FIXED: Helper function to extract OR number from transaction ID
    private fun extractORFromTransactionId(transactionId: String): String? {
        return try {
            // Extract numeric part from transaction ID
            val numericPart = transactionId.filter { it.isDigit() }
            if (numericPart.isNotEmpty()) {
                numericPart
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    // Helper functions for calculations
    private fun getSeniorCitizenDiscount(transactions: List<TransactionSummary>): Double {
        return transactions.filter { it.discountType.equals("SC", ignoreCase = true) }
            .sumOf { it.customerDiscountAmount }
    }

    private fun getPWDDiscount(transactions: List<TransactionSummary>): Double {
        return transactions.filter { it.discountType.equals("PWD", ignoreCase = true) }
            .sumOf { it.customerDiscountAmount }
    }

    private fun getOtherDiscount(transactions: List<TransactionSummary>): Double {
        return transactions.sumOf { it.discountAmount } - (getSeniorCitizenDiscount(transactions) + getPWDDiscount(
            transactions
        ))
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