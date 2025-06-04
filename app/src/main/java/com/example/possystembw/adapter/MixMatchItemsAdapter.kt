// Create a new file: MixMatchItemsAdapter.kt
package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.ui.ViewModel.MixMatchItemRequest

class MixMatchItemsAdapter(
    private val items: MutableList<MixMatchItemRequest>,
    private val onRemoveClick: (MixMatchItemRequest) -> Unit,
    private val onQuantityChanged: (MixMatchItemRequest, Int) -> Unit
) : RecyclerView.Adapter<MixMatchItemsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvQty: TextView = itemView.findViewById(R.id.tvQty)
        val tvSubtotal: TextView = itemView.findViewById(R.id.tvSubtotal)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)
        val btnIncrease: ImageButton = itemView.findViewById(R.id.btnIncrease)
        val btnDecrease: ImageButton = itemView.findViewById(R.id.btnDecrease)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mix_match, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvItemName.text = item.itemName
        holder.tvPrice.text = String.format("₱%.2f", item.price)
        holder.tvQty.text = item.qty.toString()
        holder.tvSubtotal.text = String.format("₱%.2f", item.price * item.qty)

        holder.btnRemove.setOnClickListener {
            onRemoveClick(item)
        }

        holder.btnIncrease.setOnClickListener {
            if (position < items.size) {
                val newQty = item.qty + 1
                val updatedItem = item.copy(qty = newQty)
                items[position] = updatedItem
                holder.tvQty.text = newQty.toString()
                holder.tvSubtotal.text = String.format("₱%.2f", updatedItem.price * newQty)
                onQuantityChanged(updatedItem, newQty)
            }
        }

        holder.btnDecrease.setOnClickListener {
            if (position < items.size && item.qty > 1) {
                val newQty = item.qty - 1
                val updatedItem = item.copy(qty = newQty)
                items[position] = updatedItem
                holder.tvQty.text = newQty.toString()
                holder.tvSubtotal.text = String.format("₱%.2f", updatedItem.price * newQty)
                onQuantityChanged(updatedItem, newQty)
            }
        }
    }

    override fun getItemCount(): Int = items.size
}