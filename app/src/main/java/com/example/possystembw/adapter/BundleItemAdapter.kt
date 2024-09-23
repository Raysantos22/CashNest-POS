package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.Product

class BundleItemAdapter(
    private val onItemSelected: (Product) -> Unit
) : ListAdapter<Product, BundleItemAdapter.BundleItemViewHolder>(BundleItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BundleItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bundle_product, parent, false)
        return BundleItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: BundleItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BundleItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.productNameText)
        private val priceText: TextView = itemView.findViewById(R.id.productPriceText)

        fun bind(product: Product) {
            nameText.text = product.itemName
            priceText.text = String.format("₱%.2f", product.price)
            itemView.setOnClickListener { onItemSelected(product) }
        }
    }

    class BundleItemDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Product, newItem: Product) =
            oldItem == newItem
    }
}