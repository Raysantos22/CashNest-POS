package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.TransactionSummary
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(private val onItemClick: (TransactionSummary) -> Unit) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private var transactions: List<TransactionSummary> = emptyList()
    private var filteredTransactions: List<TransactionSummary> = emptyList()

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val staffTextView: TextView = itemView.findViewById(R.id.staffTextView)
        private val storeTextView: TextView = itemView.findViewById(R.id.storeTextView)
        private val transactionIdTextView: TextView = itemView.findViewById(R.id.transactionIdTextView)
        private val totalAmountTextView: TextView = itemView.findViewById(R.id.totalAmountTextView)

        fun bind(transaction: TransactionSummary) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateTextView.text = dateFormat.format(transaction.createdDate)
            staffTextView.text = transaction.staff
            storeTextView.text = transaction.store
            transactionIdTextView.text = transaction.transactionId
            totalAmountTextView.text = "₱${String.format("%.2f", transaction.netAmount)}"

            itemView.setOnClickListener { onItemClick(transaction) }
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
        filteredTransactions = newTransactions
        notifyDataSetChanged()
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
        notifyDataSetChanged()
    }
}