package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.LineTransactionEntity

class LineTransactionAdapter : ListAdapter<LineTransactionEntity, LineTransactionAdapter.ViewHolder>(
    LineTransactionDiffCallback()
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_line_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvItemName: TextView = view.findViewById(R.id.tvItemName)
        private val tvItemId: TextView = view.findViewById(R.id.tvItemId)
        private val tvBarcode: TextView = view.findViewById(R.id.tvBarcode)
        private val tvReceived: TextView = view.findViewById(R.id.tvReceived)
        private val tvWaste: TextView = view.findViewById(R.id.tvWaste)
        private val tvCounted: TextView = view.findViewById(R.id.tvCounted)

        fun bind(item: LineTransactionEntity) {
            tvItemName.text = item.itemName
            tvItemId.text = "Item ID: ${item.itemId}"
            tvBarcode.text = "Barcode: ${item.barcode}"
            tvReceived.text = "Received: ${item.receivedCount}"
            tvWaste.text = "Waste: ${item.wasteCount}"
            tvCounted.text = "Counted: ${item.counted}"
        }
    }
}

class LineTransactionDiffCallback : DiffUtil.ItemCallback<LineTransactionEntity>() {
    override fun areItemsTheSame(oldItem: LineTransactionEntity, newItem: LineTransactionEntity): Boolean {
        return oldItem.journalId == newItem.journalId && oldItem.itemId == newItem.itemId
    }

    override fun areContentsTheSame(oldItem: LineTransactionEntity, newItem: LineTransactionEntity): Boolean {
        return oldItem == newItem
    }
}