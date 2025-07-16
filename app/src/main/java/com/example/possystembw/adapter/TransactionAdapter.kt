package com.example.possystembw.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.database.TransactionSummary
import com.example.possystembw.ui.SessionManager
import com.google.android.material.imageview.ShapeableImageView
import java.text.SimpleDateFormat
import java.util.*
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionAdapter(private val onItemClick: (TransactionSummary) -> Unit) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private var transactions: List<TransactionSummary> = emptyList()
    private var filteredTransactions: List<TransactionSummary> = emptyList()
    private var comparator: Comparator<TransactionSummary>? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    fun setSortComparator(comp: Comparator<TransactionSummary>) {
        comparator = comp
        sortAndUpdateLists()
    }

    private fun sortAndUpdateLists() {
        comparator?.let { comp ->
            transactions = transactions.sortedWith(comp)
            filteredTransactions = filteredTransactions.sortedWith(comp)
            notifyDataSetChanged()
        }
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val staffImageView: ShapeableImageView = itemView.findViewById(R.id.staffImageView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val staffTextView: TextView = itemView.findViewById(R.id.staffTextView)
        private val storeTextView: TextView = itemView.findViewById(R.id.storeTextView)
        private val transactionIdTextView: TextView = itemView.findViewById(R.id.transactionIdTextView)
        private val totalAmountTextView: TextView = itemView.findViewById(R.id.totalAmountTextView)

        fun bind(transaction: TransactionSummary) {
            // FIXED: Better date formatting with debug info
            val formattedDate = formatDateForDisplay(transaction.createdDate)
            dateTextView.text = formattedDate

            staffTextView.text = transaction.staff
            storeTextView.text = transaction.store
            transactionIdTextView.text = transaction.transactionId
            totalAmountTextView.text = "₱${String.format("%.2f", transaction.netAmount)}"

            loadStaffImage(transaction.staff)
            itemView.setOnClickListener { onItemClick(transaction) }
        }
        // FIXED: Helper function to format string date for display
        private fun formatDateForDisplay(dateString: String): String {
            return try {
                if (dateString.isEmpty()) {
                    "Unknown Date"
                } else {
                    // Try multiple input formats
                    val inputFormats = listOf(
                        "yyyy-MM-dd HH:mm:ss",                    // Your API format
                        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",       // ISO with microseconds
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",          // ISO with milliseconds
                        "yyyy-MM-dd'T'HH:mm:ss'Z'",              // ISO without milliseconds
                        "yyyy-MM-dd'T'HH:mm:ss"                  // ISO without Z
                    )

                    val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

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
                        }
                    }

                    // If all parsing fails, return the original string
                    dateString
                }
            } catch (e: Exception) {
                dateString.ifEmpty { "Unknown Date" }
            }
        }


        private fun loadStaffImage(staffName: String) {
            // Set placeholder image first
            staffImageView.setImageResource(R.drawable.placeholder_image)

            // Try to load actual image
            coroutineScope.launch {
                try {
                    val staffDao = AppDatabase.getDatabase(itemView.context).staffDao()
                    val currentStoreId = SessionManager.getCurrentUser()?.storeid ?: return@launch

                    withContext(Dispatchers.IO) {
                        val staff = staffDao.getStaffByStore(currentStoreId)
                            .find { it.name == staffName }

                        if (staff?.image != null) {
                            val decodedBytes = Base64.decode(staff.image, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                            withContext(Dispatchers.Main) {
                                staffImageView.setImageBitmap(bitmap)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Keep placeholder image if there's an error
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(filteredTransactions[position])
    }

    override fun getItemCount(): Int = filteredTransactions.size

    fun setTransactions(newTransactions: List<TransactionSummary>) {
        transactions = newTransactions
        filter("") // This will also apply sorting
    }

    fun filter(query: String) {
        filteredTransactions = if (query.isEmpty()) {
            transactions
        } else {
            transactions.filter {
                it.receiptId.contains(query, ignoreCase = true) ||
                        it.staff.contains(query, ignoreCase = true) ||
                        it.transactionId.contains(query, ignoreCase = true)
            }
        }
        sortAndUpdateLists()
    }

    // Add cleanup method
    fun cleanup() {
        coroutineScope.cancel()
    }
}

// ALSO UPDATE: Any other adapters or UI components that display dates
// Add these helper functions to any class that needs to display dates:

// Extension function for easy date formatting
fun String.toDisplayDate(): String {
    return try {
        if (this.isEmpty()) return "Unknown Date"
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val date = inputFormat.parse(this)
        if (date != null) {
            outputFormat.format(date)
        } else {
            this
        }
    } catch (e: Exception) {
        this.ifEmpty { "Unknown Date" }
    }
}

// Extension function for sorting by date
fun String.toTimestampForSorting(): Long {
    return try {
        if (this.isEmpty()) return 0L
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val date = format.parse(this)
        date?.time ?: 0L
    } catch (e: Exception) {
        0L
    }
}

// FIXED: Update sorting comparator to handle string dates
// If you have any sorting logic that needs to be updated:
class TransactionSorting {
    companion object {
        // Sort by date descending (newest first)
        val byDateDescending = Comparator<TransactionSummary> { t1, t2 ->
            t2.createdDate.toTimestampForSorting().compareTo(t1.createdDate.toTimestampForSorting())
        }

        // Sort by date ascending (oldest first)
        val byDateAscending = Comparator<TransactionSummary> { t1, t2 ->
            t1.createdDate.toTimestampForSorting().compareTo(t2.createdDate.toTimestampForSorting())
        }

        // Sort by amount descending
        val byAmountDescending = Comparator<TransactionSummary> { t1, t2 ->
            t2.netAmount.compareTo(t1.netAmount)
        }
    }
}

// EXAMPLE USAGE: How to apply the fixed sorting in your dialog
/*
// In your showTransactionListDialog function:
val transactionAdapter = TransactionAdapter { transaction ->
    showTransactionDetailsDialog(transaction)
}.apply {
    setSortComparator(TransactionSorting.byDateDescending)
}
*/