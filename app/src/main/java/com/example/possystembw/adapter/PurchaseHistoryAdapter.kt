package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.data.CustomerPurchaseHistory
import com.example.possystembw.database.Product
import java.text.SimpleDateFormat
import java.util.Locale

class PurchaseHistoryAdapter(
    private val onItemClick: (Product) -> Unit
) : ListAdapter<CustomerPurchaseHistory, PurchaseHistoryAdapter.ViewHolder>(PurchaseHistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_purchase_history, parent, false)
        return ViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onItemClick: (Product) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val itemNameText: TextView = itemView.findViewById(R.id.itemNameText)
        private val purchaseInfoText: TextView = itemView.findViewById(R.id.purchaseInfoText)
        private val lastPurchaseText: TextView = itemView.findViewById(R.id.lastPurchaseText)
        private val addToCartButton: Button = itemView.findViewById(R.id.addToCartButton)

        fun bind(item: CustomerPurchaseHistory) {
            itemNameText.text = item.itemName
            purchaseInfoText.text = "${item.purchaseCount} times • Avg qty: %.1f".format(item.averageQuantity)

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            lastPurchaseText.text = "Last bought: ${dateFormat.format(item.lastPurchaseDate)}"

            addToCartButton.setOnClickListener {
                val product = Product(
                    id = item.id,
                    itemid = item.itemid,
                    itemName = item.itemName,
                    itemGroup = item.itemGroup,
                    price = item.price,
                    foodpanda = item.foodpanda,
                    grabfood = item.grabfood,
                    manilaprice = item.manilaprice,
                    categoryId = item.categoryId,
                    // Set default values for other required fields
                    activeOnDelivery = false,
                    specialGroup = "",
                    production = "",
                    moq = 0,
                    cost = 0.0,
                    barcode = 0
                )
                onItemClick(product)
            }
        }
    }

    private class PurchaseHistoryDiffCallback : DiffUtil.ItemCallback<CustomerPurchaseHistory>() {
        override fun areItemsTheSame(oldItem: CustomerPurchaseHistory, newItem: CustomerPurchaseHistory): Boolean {
            return oldItem.itemid == newItem.itemid
        }

        override fun areContentsTheSame(oldItem: CustomerPurchaseHistory, newItem: CustomerPurchaseHistory): Boolean {
            return oldItem == newItem
        }
    }
}