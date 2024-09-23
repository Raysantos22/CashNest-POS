package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.TransactionRecord


class TransactionItemsAdapter(
    private var items: List<TransactionRecord>,
    private val onItemSelected: (TransactionRecord, Boolean) -> Unit
) : RecyclerView.Adapter<TransactionItemsAdapter.ItemViewHolder>() {

    init {
        // Check if any item has partial payment
        val hasPartialPayment = items.any { it.partialPaymentAmount > 0 }
        if (hasPartialPayment) {
            items.forEach { it.isSelected = true }
        }
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.itemCheckBox)
        private val nameTextView: TextView = itemView.findViewById(R.id.itemNameTextView)
        private val quantityTextView: TextView = itemView.findViewById(R.id.quantityTextView)
        private val priceTextView: TextView = itemView.findViewById(R.id.priceTextView)

        fun bind(transaction: TransactionRecord) {
            nameTextView.text = transaction.name
            quantityTextView.text = transaction.quantity.toString()
            priceTextView.text = String.format("₱%.2f", transaction.netAmount)

            // Set checkbox state
            checkBox.isChecked = transaction.isSelected

            // If there's a partial payment, disable the checkbox and force it to be checked
            if (transaction.partialPaymentAmount > 0) {
                checkBox.isEnabled = false
                checkBox.isChecked = true
            } else {
                checkBox.isEnabled = true
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    transaction.isSelected = isChecked
                    onItemSelected(transaction, isChecked)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction_detail, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<TransactionRecord>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<TransactionRecord> {
        return items.filter { it.isSelected }
    }
}