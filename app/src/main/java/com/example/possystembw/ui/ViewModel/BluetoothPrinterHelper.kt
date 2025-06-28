package com.example.possystembw.ui.ViewModel

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.possystembw.DAO.TransactionDao
import com.example.possystembw.database.CartItem
import com.example.possystembw.database.Customer
import com.example.possystembw.database.TenderDeclaration
import com.example.possystembw.database.TransactionRecord
import com.example.possystembw.database.TransactionSummary
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class BluetoothPrinterHelper(private val context: Context) {
    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothSocket: BluetoothSocket? = null
    var outputStream: OutputStream? = null
    private var printerAddress: String? = null

    private var isConnecting = false
    private var connectionError: String? = null
    private val prefs =
        context.applicationContext.getSharedPreferences("BluetoothPrinter", Context.MODE_PRIVATE)
    private var disconnectRequested = false

    private lateinit var transactionDao: TransactionDao

    // Add method to inject transactionDao
    fun setTransactionDao(dao: TransactionDao) {
        this.transactionDao = dao
    }

    enum class ReceiptType {
        ORIGINAL,
        REPRINT,
        RETURN,
        AR  // New AR type
    }

    companion object {
        private const val TAG = "BluetoothPrinterHelper"
        private const val PREF_LAST_PRINTER = "last_printer_address"

        @Volatile
        private var instance: BluetoothPrinterHelper? = null
        private lateinit var applicationContext: Context

        fun initialize(context: Context) {
            applicationContext = context.applicationContext
        }

        fun getInstance(): BluetoothPrinterHelper {
            return instance ?: synchronized(this) {
                instance ?: BluetoothPrinterHelper(applicationContext).also { instance = it }
            }
        }
    }

    fun connect(address: String): Boolean {
        Log.d(TAG, "Attempting to connect to printer: $address")

        disconnectRequested = false  // Reset the flag on new connection attempt

        if (!checkBluetoothPermissions()) {
            Log.e(TAG, "Bluetooth permission not granted")
            return false
        }

        if (isConnected() && address == printerAddress) {
            Log.d(TAG, "Already connected to this printer")
            return true
        }


        if (isConnecting) {
            Log.d(TAG, "Connection already in progress")
            return false
        }

        try {
            isConnecting = true

            if (isConnected()) {
                Log.d(TAG, "Disconnecting from previous printer")
                disconnect()
            }

            val device = bluetoothAdapter?.getRemoteDevice(address)
                ?: throw IOException("Device not found")

            Log.d(TAG, "Creating socket for device: ${device.name ?: "Unknown"} ($address)")

            bluetoothSocket = device.createRfcommSocketToServiceRecord(
                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            )

            Log.d(TAG, "Attempting socket connection")
            bluetoothSocket?.connect()

            outputStream = bluetoothSocket?.outputStream
            printerAddress = address

            Log.d(TAG, "Successfully connected to printer: $address")
            connectionError = null
            return true
        } catch (e: IOException) {
            connectionError = e.message
            Log.e(TAG, "Error connecting to printer: ${e.message}")
            disconnect()
            return false
        } catch (se: SecurityException) {
            connectionError = se.message
            Log.e(TAG, "SecurityException: ${se.message}")
            disconnect()
            return false
        } finally {
            isConnecting = false
        }
    }


    fun isConnected(): Boolean {
        val connected = bluetoothSocket?.isConnected == true && outputStream != null
        Log.d(TAG, "Checking connection status: $connected, address: $printerAddress")
        return connected
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting printer: $printerAddress")
        disconnectRequested = true  // Set the flag
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error disconnecting: ${e.message}")
        } finally {
            outputStream = null
            bluetoothSocket = null
            printerAddress = null
            connectionError = null
        }
    }


    fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    private fun addFooterSpacingAndCut(sb: StringBuilder) {
        // Add consistent spacing before cut
        repeat(1) {
            sb.appendLine()
        }
    }

    fun getCurrentPrinterAddress(): String? {
        return if (isConnected()) {
            Log.d(TAG, "Current printer address: $printerAddress")
            printerAddress
        } else {
            Log.d(TAG, "No printer connected")
            null
        }
    }

    fun getLastError(): String? = connectionError

    fun printReceipt(
        transaction: TransactionSummary,
        items: List<TransactionRecord>,
        receiptType: ReceiptType = ReceiptType.ORIGINAL
    ): Boolean {
        if (!checkBluetoothPermissions()) {
            Log.e("BluetoothPrinterHelper", "Bluetooth permission not granted")
            return false
        }

        if (!isConnected()) {
            Log.e("BluetoothPrinterHelper", "Not connected to a printer")
            return false
        }

        try {
            val receiptContent = generateReceiptContent(transaction, items, receiptType)
            outputStream?.write(receiptContent.toByteArray())
            outputStream?.flush()

            // Add cut command after printing the receipt
            cutPaper()

            return true
        } catch (e: IOException) {
            Log.e("BluetoothPrinterHelper", "Error printing receipt: ${e.message}")
            disconnect()  // Disconnect on error to allow reconnection attempt
            return false
        } catch (se: SecurityException) {
            Log.e("BluetoothPrinterHelper", "SecurityException: ${se.message}")
            return false
        }
    }

    fun cutPaper() {
        try {
            // Add proper spacing before cut
            outputStream?.write("\n\n\n\n\n\n\n\n".toByteArray())

            // ESC/POS command for full cut with feed
            val cutCommand = byteArrayOf(0x1D, 0x56, 0x41, 0x00)
            outputStream?.write(cutCommand)
            outputStream?.flush()
        } catch (e: IOException) {
            Log.e("BluetoothPrinterHelper", "Error cutting paper: ${e.message}")
        }
    }

    fun printGenericReceipt(receiptContent: String): Boolean {
        if (!checkBluetoothPermissions() || !isConnected()) {
            return false
        }

        try {
            val sb = StringBuilder(receiptContent)
            addFooterSpacingAndCut(sb)

            // Write the content
            outputStream?.write(sb.toString().toByteArray())
            outputStream?.flush()

            // Add cut command
            val cutCommand = byteArrayOf(0x1D, 0x56, 0x41, 0x00)
            outputStream?.write(cutCommand)
            outputStream?.flush()

            return true
        } catch (e: IOException) {
            Log.e("BluetoothPrinterHelper", "Error printing receipt: ${e.message}")
            disconnect()
            return false
        }
    }


    fun printCashFundReceipt(cashFund: Double, status: String): Boolean {
        if (!isConnected()) {
            Log.e("BluetoothPrinterHelper", "Not connected to a printer")
            return false
        }

        val receiptContent = buildString {
            append(0x1B.toChar()) // ESC
            append('!'.toChar())  // Select print mode
            append(0x01.toChar()) // Smallest text size

            // Set minimum line spacing
            append(0x1B.toChar()) // ESC
            append('3'.toChar())  // Select line spacing
            append(50.toChar())
            appendLine("CASH FUND RECEIPT")
            appendLine("Date: ${getCurrentDate()}")
            appendLine("Time: ${getCurrentTime()}")
            appendLine("Amount: ${"%.2f".format(cashFund)}")
            appendLine("Status: $status")
            append(0x1B.toChar()) // ESC
            append('!'.toChar())  // Select print mode
            append(0x00.toChar()) // Reset to normal size

            append(0x1B.toChar()) // ESC
            append('2'.toChar())
        }

        return printGenericReceipt(receiptContent)
    }

    fun printPulloutCashFundReceipt(pulloutAmount: Double): Boolean {
        if (!isConnected()) {
            Log.e("BluetoothPrinterHelper", "Not connected to a printer")
            return false
        }

        val receiptContent = buildString {
            append(0x1B.toChar()) // ESC
            append('!'.toChar())  // Select print mode
            append(0x01.toChar()) // Smallest text size

            // Set minimum line spacing
            append(0x1B.toChar()) // ESC
            append('3'.toChar())  // Select line spacing
            append(50.toChar())
            appendLine("PULL-OUT CASH FUND RECEIPT")
            appendLine("Date: ${getCurrentDate()}")
            appendLine("Time: ${getCurrentTime()}")
            appendLine("Amount: ${"%.2f".format(pulloutAmount)}")
            append(0x1B.toChar()) // ESC
            append('!'.toChar())  // Select print mode
            append(0x00.toChar()) // Reset to normal size

            append(0x1B.toChar()) // ESC
            append('2'.toChar())
        }

        return printGenericReceipt(receiptContent)
    }

    fun printTenderDeclarationReceipt(cashAmount: Double, arAmounts: Map<String, Double>): Boolean {
        if (!isConnected()) {
            Log.e("BluetoothPrinterHelper", "Not connected to a printer")
            return false
        }

        val receiptContent = buildString {
            append(0x1B.toChar()) // ESC
            append('!'.toChar())  // Select print mode
            append(0x01.toChar()) // Smallest text size

            // Set minimum line spacing
            append(0x1B.toChar()) // ESC
            append('3'.toChar())  // Select line spacing
            append(50.toChar())
            appendLine("TENDER DECLARATION RECEIPT")
            appendLine("Date: ${getCurrentDate()}")
            appendLine("Time: ${getCurrentTime()}")
            appendLine("-".repeat(46))
            appendLine("Cash Amount: ${String.format("%.2f", cashAmount)}")
            appendLine("-".repeat(46))
            appendLine("AR Amounts:")
            arAmounts.forEach { (arType, amount) ->
                if (arType != "Cash") {  // Exclude Cash from AR amounts
                    appendLine("$arType: ${String.format("%.2f", amount)}")
                }
            }
            appendLine("-".repeat(46))
            val totalAmount = cashAmount + arAmounts.filterKeys { it != "Cash" }.values.sum()
            appendLine("Total Amount: ${String.format("%.2f", totalAmount)}")
            append(0x1B.toChar()) // ESC
            append('!'.toChar())  // Select print mode
            append(0x00.toChar()) // Reset to normal size

            append(0x1B.toChar()) // ESC
            append('2'.toChar())
        }

        return printGenericReceipt(receiptContent)
    }

    data class ItemCalculationResult(
        val totalGross: Double,
        val totalQuantity: Int,
        val itemSales: List<ItemSalesSummary>
    )

    data class ItemSalesSummary(
        val name: String,
        val quantity: Int,
        val totalAmount: Double
    )

    // Calculate item totals using the same logic as ReportsActivity
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
                    ))

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

    // Count void transactions using the same logic as ReportsActivity
    private fun getVoidedTransactionsCount(transactions: List<TransactionSummary>): Int {
        // Count transactions with return comments (same logic as ReportsActivity)
        return transactions.count { transaction ->
            transaction.comment.contains("Returned:", ignoreCase = true) ||
                    transaction.comment.contains("Return processed:", ignoreCase = true)
        }
    }
    fun buildReadReport(
        transactions: List<TransactionSummary>,
        isZRead: Boolean,
        zReportId: String = "",
        tenderDeclaration: TenderDeclaration? = null
    ): String = runBlocking {

        // Filter only completed transactions (same as ReportsActivity)
        val validTransactions = transactions.filter { it.transactionStatus == 1 }
        val returnTransactions = transactions.filter { it.type == 2 } // Return transactions

        // Calculate item totals using the same method as ReportsActivity
        val itemCalculations = calculateItemTotals(validTransactions)

        // Use calculated totals instead of transaction summary totals
        val totalGross = itemCalculations.totalGross  // From item calculations
        val totalSales = validTransactions.sumOf { it.netAmount }  // Net sales from transactions
        val totalVat = validTransactions.sumOf { it.taxIncludedInPrice }
        val totalDiscount = validTransactions.sumOf { it.totalDiscountAmount }
        val vatableSales = validTransactions.sumOf { it.vatableSales }
        val vatExemptSales = validTransactions.sumOf { it.vatExemptAmount }

        // Payment method totals using specific fields
        val cashTotal = validTransactions.sumOf { it.cash }
        val cardTotal = validTransactions.sumOf { it.card }
        val gCashTotal = validTransactions.sumOf { it.gCash }
        val payMayaTotal = validTransactions.sumOf { it.payMaya }
        val loyaltyCardTotal = validTransactions.sumOf { it.loyaltyCard }
        val chargeTotal = validTransactions.sumOf { it.charge }
        val foodPandaTotal = validTransactions.sumOf { it.foodpanda }
        val grabFoodTotal = validTransactions.sumOf { it.grabfood }
        val representationTotal = validTransactions.sumOf { it.representation }
        val arSales = validTransactions.filter { it.type == 3 }.sumOf { it.netAmount }

        // Transaction statistics using consistent logic
        val totalReturns = returnTransactions.sumOf { it.netAmount }
        val totalItemsSold = itemCalculations.totalQuantity  // From item calculations
        val totalItemsReturned = returnTransactions.sumOf { it.numberOfItems.toInt() }
        val returnTransactionCount = returnTransactions.size
        val customerTransactions = transactions.count { it.customerAccount.isNotBlank() }

        // Use consistent void transaction counting
        val voidedTransactions = getVoidedTransactionsCount(transactions)

        // Get OR numbers for starting and ending
        val startingOR = if (validTransactions.isNotEmpty()) {
            validTransactions.minByOrNull { it.transactionId }?.transactionId?.toString()?.filter { it.isDigit() } ?: "N/A"
        } else "N/A"

        val endingOR = if (validTransactions.isNotEmpty()) {
            validTransactions.maxByOrNull { it.transactionId }?.transactionId?.toString()?.filter { it.isDigit() } ?: "N/A"
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
            appendLine("Gross sales                           ${"%.2f".format(totalGross)}")  // Now uses calculated gross
            appendLine("Total netsales                        ${"%.2f".format(totalGross - totalDiscount)}")
            appendLine("Total Discount                        ${"%.2f".format(totalDiscount)}")



            appendLine("-".repeat(46))
            appendLine("Statistics                            qty")
            appendLine("-".repeat(46))
            appendLine("No. of sales trans                    ${validTransactions.size}")  // Only valid transactions
            appendLine("No. of items sold                     $totalItemsSold")  // From item calculations
//            appendLine("No. of void trans                     $voidedTransactions")  // Consistent counting

            // Add return information like in ReportsActivity
            if (returnTransactionCount > 0) {
                appendLine("No. of return trans                   $returnTransactionCount")
//                appendLine("No. of items returned                 $totalItemsReturned")
            }

            appendLine("-".repeat(46))
            appendLine("Tender reports")
            appendLine("-".repeat(46))
            appendLine("Tender name                          amount")
            appendLine("_".repeat(46))

            if (cashTotal > 0) appendLine("CASH".padEnd(38) + "%.2f".format(cashTotal))
            if (cardTotal > 0) appendLine("CARD".padEnd(38) + "%.2f".format(cardTotal))
            if (gCashTotal > 0) appendLine("GCASH".padEnd(38) + "%.2f".format(gCashTotal))
            if (payMayaTotal > 0) appendLine("PAYMAYA".padEnd(38) + "%.2f".format(payMayaTotal))
            if (loyaltyCardTotal > 0) appendLine(
                "LOYALTY CARD".padEnd(38) + "%.2f".format(loyaltyCardTotal)
            )
            if (chargeTotal > 0) appendLine("CHARGE".padEnd(38) + "%.2f".format(chargeTotal))
            if (foodPandaTotal > 0) appendLine("FOODPANDA".padEnd(38) + "%.2f".format(foodPandaTotal))
            if (grabFoodTotal > 0) appendLine("GRABFOOD".padEnd(38) + "%.2f".format(grabFoodTotal))
            if (representationTotal > 0) appendLine(
                "REPRESENTATION".padEnd(38) + "%.2f".format(representationTotal)
            )
            if (arSales > 0) appendLine("AR".padEnd(38) + "%.2f".format(arSales))

            appendLine()
            appendLine("total amount                          ${"%.2f".format(totalGross - totalDiscount)}")

            appendLine("-".repeat(46))
            appendLine("Tax report")
            appendLine("-".repeat(46))
            val vatRate = 0.12
            val vatableSalesManual = (totalGross - totalDiscount) / (1 + vatRate)
            val vatAmountManual = (totalGross - totalDiscount) - vatableSalesManual

            appendLine("Vatable sales                         ${"%.2f".format(vatableSalesManual)}")
            appendLine("VAT amount                            ${"%.2f".format(vatAmountManual)}")
            appendLine("vat exempt sales                      ${"%.2f".format(vatExemptSales)}")
            appendLine("zero Rated sales                      ${"%.2f".format(0.0)}")

            if (tenderDeclaration != null) {
                appendLine("-".repeat(46))
                appendLine("Tender Declaration")

                // Function to format amount entries with proper spacing
                fun formatAmountEntry(label: String, amount: Double): String {
                    val maxLabelLength = 25  // Maximum length for the label portion
                    val truncatedLabel = if (label.length > maxLabelLength) {
                        label.substring(0, maxLabelLength - 3) + "..."
                    } else {
                        label
                    }

                    return buildString {
                        append(truncatedLabel)
                        // Calculate remaining spaces needed to align amount
                        val spacesNeeded =
                            45 - truncatedLabel.length - String.format("%.2f", amount).length
                        append(" ".repeat(spacesNeeded))
                        append(String.format("%.2f", amount))
                    }
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
                appendLine("-".repeat(46))
                appendLine(formatAmountEntry("Total AR", totalAr))

                val totalDeclared = tenderDeclaration.cashAmount + totalAr
                appendLine(formatAmountEntry("Total Declared", totalDeclared))
                appendLine(formatAmountEntry("Short/Over", totalDeclared - totalSales))
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

//    private fun getVoidedTransactionsCount(transactions: List<TransactionSummary>): Int {
//        return transactions.count { it.transactionStatus == 0 } // Assuming 0 represents voided transactions
//    }

    private fun getReturnTransactionsCount(transactions: List<TransactionSummary>): Int {
        return transactions.count { it.type == 2 } // Assuming type 2 represents return transactions
    }

    private fun getCustomerTransactionsCount(transactions: List<TransactionSummary>): Int {
        return transactions.count { it.customerAccount.isNotBlank() }
    }

    private fun getVatExemptSales(transactions: List<TransactionSummary>): Double {
        return transactions.filter { it.vatExemptAmount > 0 }.sumOf { it.vatExemptAmount }
    }

    private fun getZeroRatedSales(transactions: List<TransactionSummary>): Double {
        // Implement this based on your business logic for zero-rated sales
        return 0.0
    }

    // Helper functions to be implemented
    private fun getBranchName(): String {
        // Implement this to return the branch name
        return "ELJIN CORP"
    }

    private fun getRegTin(): String {
        // Implement this to return the registration TIN
        return "Your Reg TIN"
    }

    private fun getPermitNumber(): String {
        // Implement this to return the permit number
        return "Permit Number"
    }

    private fun getMinNumber(): String {
        // Implement this to return the MIN number
        return "MIN Number"
    }

    private fun getSerialNumber(): String {
        // Implement this to return the serial number
        return "Serial Number"
    }

    private fun getOperatorName(): String {
        // Implement this to return the operator name
        return "Operator Name"
    }

    private fun getStoreName(): String {
        // Implement this to return the store name
        return "Store Name"
    }

    private fun getTerminalNumber(): String {
        // Implement this to return the terminal number
        return "Terminal Number"
    }

    private fun getStartOrNo(): String {
        // Implement this to return the start OR number
        return "Start OR Number"
    }

    private fun getEndOrNo(): String {
        // Implement this to return the end OR number
        return "End OR Number"
    }

    private fun getReturnsRefundAmount(transactions: List<TransactionSummary>): Double {
        // Implement this to calculate and return the total returns/refund amount
        return 0.0
    }


    private fun getItemsSoldCount(transactions: List<TransactionSummary>): Int {
        // Implement this to return the total count of items sold
        return 0
    }

    private fun getItemsReturnedCount(transactions: List<TransactionSummary>): Int {
        // Implement this to return the total count of items returned
        return 0
    }

    private fun getDrawerOpeningCount(): Int {
        // Implement this to return the count of drawer openings
        return 0
    }

    private fun getLoginsCount(): Int {
        // Implement this to return the count of logins
        return 0
    }

    private fun getSuspendedTransactionsCount(): Int {
        // Implement this to return the count of suspended transactions
        return 0
    }

    private fun getPosProviderTin(): String {
        // Implement this to return the POS provider's TIN
        return "POS Provider TIN"
    }

    private fun getPosProviderAccNo(): String {
        // Implement this to return the POS provider's ACC number
        return "POS Provider ACC Number"
    }

    private fun getValidUntilDate(): String {
        // Implement this to return the valid until date
        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L))
    }

//
//    fun generateReceiptContent(
//        transaction: TransactionSummary,
//        items: List<TransactionRecord>,
//        receiptType: BluetoothPrinterHelper.ReceiptType,
//        isAR: Boolean = false,
//        copyType: String? = null
//    ): String {
//        val sb = StringBuilder()
//        val isReturn = receiptType == BluetoothPrinterHelper.ReceiptType.RETURN
//
//        // For return receipts, only show returned items
//        // For regular receipts, show all items but handle returned items differently
//        val filteredItems = if (isReturn) {
//            items.filter { it.isReturned }
//        } else {
//            // Show all items, but calculate net quantities after returns
//            items.groupBy { it.itemId }.map { (_, groupedItems) ->
//                val originalItem = groupedItems.first { !it.isReturned }
//                val returnedQuantity = groupedItems
//                    .filter { it.isReturned }
//                    .sumOf { it.quantity }
//
//                // Create a new item with adjusted quantity
//                originalItem.copy(
//                    quantity = originalItem.quantity + returnedQuantity, // returnedQuantity is negative
//                    isReturned = false
//                )
//            }
//        }
//        // Store Header
//        sb.appendLine("ELJIN CORP")
//        sb.appendLine("Address Line 1")
//        sb.appendLine("Address Line 2")
//        sb.appendLine("TIN: Your TIN Number")
//        sb.appendLine("ACC: Your ACC Number")
//        sb.appendLine("Contact #: Your Contact Number")
//        sb.appendLine("-".repeat(32))
//
//        // Receipt Type Header (unchanged)
//        when (receiptType) {
//            BluetoothPrinterHelper.ReceiptType.AR -> {
//                sb.appendLine("       ACCOUNTS RECEIVABLE        ")
//                sb.appendLine("         $copyType          ")
//            }
//
//            BluetoothPrinterHelper.ReceiptType.REPRINT -> sb.appendLine("*** REPRINT ***")
//            BluetoothPrinterHelper.ReceiptType.RETURN -> {
//                sb.appendLine("*** RETURN TRANSACTION ***")
//                sb.appendLine("Original Receipt #: ${transaction.receiptId}")
//            }
//
//            else -> {} // No special header for ORIGINAL
//        }
//        sb.appendLine("-".repeat(32))
//
//        // Transaction Details
//        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//        sb.appendLine("Date Issued: ${dateFormat.format(transaction.createdDate)}")
//        sb.appendLine("Receipt ID: ${transaction.receiptId}")
//        sb.appendLine("Transaction ID: ${transaction.transactionId}")
//        sb.appendLine("Store: ${transaction.store}")
//        sb.appendLine("Cashier: ${transaction.staff}")
//        sb.appendLine("Window Number: ${transaction.windowNumber}")
//
//
//        // AR Customer Details if applicable
//        if (isAR) {
//            sb.appendLine("-".repeat(32))
//            sb.appendLine("Customer Account: ${transaction.customerAccount}")
//            sb.appendLine("Customer Name: ${transaction.customerName ?: "N/A"}")
//            sb.appendLine("AR Type: ${transaction.paymentMethod}")
//        }
//        sb.appendLine("-".repeat(32))
//
//        // Items Section
//        sb.appendLine("Item Name              Price    Qty    Total")
//
//        // Display items and calculate totals
//        items.forEach { item ->
//            val effectivePrice = if (item.priceOverride!! > 0.0) item.priceOverride else item.price
//            val itemTotal = effectivePrice * item.quantity
//
//            // Display item line
//            sb.appendLine(
//                "${item.name.take(20).padEnd(20)} ${
//                    String.format(
//                        "%10.2f",
//                        effectivePrice
//                    )
//                } ${String.format("%5d", item.quantity)} ${String.format("%8.2f", itemTotal)}"
//            )
//
//            // Show original price if overridden
//            if (item.priceOverride > 0.0 && item.priceOverride != item.price) {
//                sb.appendLine("  Original Price: ${String.format("%.2f", item.price)}")
//                sb.appendLine("  Price Override: ${String.format("%.2f", item.priceOverride)}")
//            }
//
//            // Modified discount display logic
//            when (item.discountType.uppercase()) {
//                "PERCENTAGE", "PWD", "SC" -> {
//                    val discountAmount = itemTotal * (item.discountRate)
//                    if (discountAmount > 0) {
//                        sb.appendLine(
//                            "  Discount (${item.discountType}): ${
//                                String.format(
//                                    "%.2f",
//                                    discountAmount
//                                )
//                            }"
//                        )
//                        sb.appendLine(
//                            "  Discount Rate: ${
//                                String.format(
//                                    "%.1f",
//                                    item.discountRate * 100
//                                )
//                            }%"
//                        )
//                    }
//                }
//                "FIXED" -> {
//                    val perItemDiscount = item.discountAmount
//                    val totalDiscount = perItemDiscount * item.quantity
//                    if (perItemDiscount > 0) {
//                        sb.appendLine(
//                            "  Discount Per Item: ${
//                                String.format(
//                                    "%.2f",
//                                    perItemDiscount
//                                )
//                            }"
//                        )
//                    }
//                }
//                "FIXEDTOTAL" -> {
//                    // Always show FIXEDTOTAL discount for items that have it
//                    if (item.discountAmount > 0 || item.lineDiscountAmount!! > 0) {
//                        val discountToShow = if (item.discountAmount > 0) item.discountAmount else item.lineDiscountAmount
//                        sb.appendLine(
//                            "  Discount (Fixed Total): ${
//                                String.format(
//                                    "%.2f",
//                                    discountToShow
//                                )
//                            }"
//                        )
//                    }
//                }
//            }
//        }
//
//        sb.appendLine("-".repeat(32))
//
//        // Display totals section
//        sb.appendLine("Gross Amount:        ${String.format("%12.2f", transaction.grossAmount)}")
//        sb.appendLine(
//            "Total Discounts:     ${
//                String.format(
//                    "%12.2f",
//                    transaction.totalDiscountAmount
//                )
//            }"
//        )
//
//        /* if (transaction.partialPayment > 0) {
//             sb.appendLine(
//                 "Partial Payment:     ${
//                     String.format(
//                         "%12.2f",
//                         transaction.partialPayment
//                     )
//                 }"
//             )
//         }*/
//
//        sb.appendLine("Net Amount:          ${String.format("%12.2f", transaction.netAmount)}")
//
//        // Only show amount paid and change for non-AR transactions
//        if (!isAR) {
//            sb.appendLine(
//                "Amount Paid:         ${
//                    String.format(
//                        "%12.2f",
//                        transaction.totalAmountPaid
//                    )
//                }"
//            )
//            sb.appendLine(
//                "Change:              ${
//                    String.format(
//                        "%12.2f",
//                        transaction.changeGiven
//                    )
//                }"
//            )
//        }
//
//        // Payment Method Section
//        sb.appendLine("-".repeat(32))
//        sb.appendLine("Payment Method")
//
//        // Show payment method amounts
//        if (transaction.cash > 0) {
//            sb.appendLine("Cash Payment:         ${String.format("%12.2f", transaction.cash)}")
//        }
//        if (transaction.card > 0) {
//            sb.appendLine("Card Payment:         ${String.format("%12.2f", transaction.card)}")
//        }
//        if (transaction.gCash > 0) {
//            sb.appendLine("GCash Payment:        ${String.format("%12.2f", transaction.gCash)}")
//        }
//        if (transaction.payMaya > 0) {
//            sb.appendLine("PayMaya Payment:      ${String.format("%12.2f", transaction.payMaya)}")
//        }
//        if (transaction.charge > 0) {
//            sb.appendLine("charge Payment:      ${String.format("%12.2f", transaction.charge)}")
//        }
//        if (transaction.foodpanda > 0) {
//            sb.appendLine("foodpanda Payment:      ${String.format("%12.2f", transaction.foodpanda)}")
//        }
//        if (transaction.grabfood > 0) {
//            sb.appendLine("grabfood Payment:      ${String.format("%12.2f", transaction.grabfood)}")
//        }
//        if (transaction.representation > 0) {
//            sb.appendLine("representation Payment:      ${String.format("%12.2f", transaction.representation)}")
//        }
//
//        // Payment Summary Section
//        sb.appendLine("-".repeat(32))
//        sb.appendLine("Payment Summary")
//        sb.appendLine("-".repeat(32))
//
//        if (transaction.partialPayment > 0) {
//            sb.appendLine("Total Bill Amount:    ${String.format("%12.2f", transaction.netAmount)}")
//            sb.appendLine("Previous Payment:     ${String.format("%12.2f", transaction.partialPayment)}")
//            sb.appendLine("Current Payment:      ${String.format("%12.2f", transaction.totalAmountPaid)}")
//            sb.appendLine("Total Paid:           ${String.format("%12.2f", (transaction.partialPayment + transaction.totalAmountPaid) - transaction.changeGiven)}")
//        } else {
//            sb.appendLine("Total Amount:         ${String.format("%12.2f", transaction.netAmount)}")
//            sb.appendLine("Amount Paid:          ${String.format("%12.2f", transaction.totalAmountPaid)}")
//            sb.appendLine("Change:               ${String.format("%12.2f", transaction.changeGiven)}")
//        }
//
//        // VAT Information
//        sb.appendLine("-".repeat(32))
//        sb.appendLine("Vatable Sales:       ${String.format("%12.2f", transaction.vatableSales)}")
//        sb.appendLine("VAT Amount (12%):    ${String.format("%12.2f", transaction.vatAmount)}")
//        sb.appendLine("Vat Exempt           ${String.format("%12.2f", 0.0)}")
//        sb.appendLine("Zero Rated Sales:    ${String.format("%12.2f", 0.0)}")
//
//        // Transaction Details
//        sb.appendLine("-".repeat(32))
//
//
//        // Transaction Comment
//        if (!transaction.comment.isNullOrBlank()) {
//            sb.appendLine("-".repeat(32))
//            sb.appendLine("Comment: ${transaction.comment}")
//        }
//
//        // Footer
//        sb.appendLine("-".repeat(32))
//        if (!isReturn) {
//            sb.appendLine("ID/OSCA/PWD: ")
//            sb.appendLine("NAME: ")
//            sb.appendLine("Signature: ")
//        }
//        sb.appendLine("-".repeat(32))
//        sb.appendLine("This serves as your official receipt")
//        sb.appendLine("This invoice/receipt shall be valid for")
//        sb.appendLine("five (5) years from the date of the")
//        sb.appendLine("permit to use")
//        sb.appendLine("-".repeat(32))
//        sb.appendLine("POS Provider: IT WARRIORS")
//        sb.appendLine("ELJIN CORP")
//        sb.appendLine("ADDRESS")
//        sb.appendLine("-".repeat(32))
//        sb.appendLine("Thank you for your business!")
//
//        // AR Copy Type
//        if (isAR) {
//            sb.appendLine("-".repeat(32))
//            sb.appendLine(copyType)
//        }
//
//        // Add extra spacing at the bottom
//
//
//        return sb.toString()
//    }
//}

    fun generateReceiptContent(
        transaction: TransactionSummary,
        items: List<TransactionRecord>,
        receiptType: BluetoothPrinterHelper.ReceiptType,
        isAR: Boolean = false,
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
        sb.appendLine()
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

        // VAT Information
        sb.appendLine("-".repeat(45))
        sb.appendLine("VATable Sales:${String.format("%26.2f", transaction.vatableSales)}")
        sb.appendLine("VAT Amount:${String.format("%29.2f", transaction.vatAmount)}")
        sb.appendLine("VAT Exempt:${String.format("%29.2f", 0.0)}")

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
            sb.appendLine("-".repeat(45))
        }

        sb.appendLine("Valid for 5 years from PTU date")
        sb.appendLine("POS Provider: Ray and Mark")
        sb.appendLine("-".repeat(45))

        // Thank you message and rating request
//        sb.appendLine()
        sb.appendLine("Thank you for shopping at BW Superbakeshop!")
        sb.appendLine("Please rate your experience:")
        sb.appendLine()

        // Generate QR Code
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

        // Reset to normal text size
        sb.append(0x1B.toChar()) // ESC
        sb.append('!'.toChar())  // Select print mode
        sb.append(0x00.toChar()) // Reset to normal size

        sb.append(0x1B.toChar()) // ESC
//        sb.append('2'.toChar())  // Default line spacing

        return sb.toString().trimEnd()
    }
}