package com.example.possystembw.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.StockCountingEntity
import com.google.android.material.button.MaterialButton

class StockCountingAdapter(
    private val onCountClick: (String, String) -> Unit  // Changed Long to String
) : RecyclerView.Adapter<StockCountingAdapter.ViewHolder>() {

    private var items: List<StockCountingEntity> = emptyList()

    fun updateItems(newItems: List<StockCountingEntity>) {
        Log.d("StockCountingAdapter", "Updating items: ${newItems.size}")
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stock_counting, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d("StockCountingAdapter", "Binding item at position $position")
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvBatchNumber: TextView = view.findViewById(R.id.tvBatchNumber)
        private val tvQty: TextView = view.findViewById(R.id.tvQty)
        private val tvDate: TextView = view.findViewById(R.id.tvDate)
        private val btnCount: Button = view.findViewById(R.id.btnCount)

        fun bind(item: StockCountingEntity) {
            tvBatchNumber.text = item.description
            tvQty.text = item.quantity
            tvDate.text = item.createdDateTime
            btnCount.setOnClickListener {
                onCountClick(item.storeId, item.journalId.toString()) // Convert journalId to String
            }
        }
    }
}