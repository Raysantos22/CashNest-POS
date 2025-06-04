package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.DAO.LineTransaction
import com.example.possystembw.R
import com.example.possystembw.data.LineTransactionWithVisibility

class LineTransactionVisibilityAdapter(
    private val onToggleVisibility: (LineTransaction, Boolean) -> Unit
) : ListAdapter<LineTransactionWithVisibility, LineTransactionVisibilityAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_line_transaction_visibility, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvItemId: TextView = itemView.findViewById(R.id.tvItemId)
        private val tvCounted: TextView = itemView.findViewById(R.id.tvCounted)
        private val tvAdjustment: TextView = itemView.findViewById(R.id.tvAdjustment)
        private val switchVisibility: Switch = itemView.findViewById(R.id.switchVisibility)

        fun bind(lineTransactionWithVisibility: LineTransactionWithVisibility) {
            val transaction = lineTransactionWithVisibility.lineTransaction

            tvItemName.text = transaction.itemName ?: "Unknown Item"
            tvItemId.text = "ID: ${transaction.itemId ?: "N/A"}"
            tvCounted.text = "Counted: ${transaction.counted ?: "0"}"
            tvAdjustment.text = "Adjustment: ${transaction.adjustment ?: "0"}"

            // Set switch state without triggering listener
            switchVisibility.setOnCheckedChangeListener(null)
            switchVisibility.isChecked = lineTransactionWithVisibility.isVisible

            // Set the listener after setting the state
            switchVisibility.setOnCheckedChangeListener { _, isChecked ->
                onToggleVisibility(transaction, isChecked)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<LineTransactionWithVisibility>() {
        override fun areItemsTheSame(
            oldItem: LineTransactionWithVisibility,
            newItem: LineTransactionWithVisibility
        ): Boolean {
            return oldItem.lineTransaction.itemId == newItem.lineTransaction.itemId
        }

        override fun areContentsTheSame(
            oldItem: LineTransactionWithVisibility,
            newItem: LineTransactionWithVisibility
        ): Boolean {
            return oldItem == newItem
        }
    }
}
