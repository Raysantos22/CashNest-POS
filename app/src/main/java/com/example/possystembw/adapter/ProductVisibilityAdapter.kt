package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.Product
import com.example.possystembw.ui.ProductWithVisibility

class ProductVisibilityAdapter(
    private val onToggleVisibility: (Product, Boolean) -> Unit
) : ListAdapter<ProductWithVisibility, ProductVisibilityAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_visibility, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvProductGroup: TextView = itemView.findViewById(R.id.tvProductGroup)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val switchVisibility: Switch = itemView.findViewById(R.id.switchVisibility)

        fun bind(productWithVisibility: ProductWithVisibility) {
            val product = productWithVisibility.product

            tvProductName.text = product.itemName
            tvProductGroup.text = product.itemGroup
            tvPrice.text = "₱${String.format("%.2f", product.price)}"

            // Set switch state without triggering listener
            switchVisibility.setOnCheckedChangeListener(null)
            switchVisibility.isChecked = productWithVisibility.isVisible

            // Set the listener after setting the state
            switchVisibility.setOnCheckedChangeListener { _, isChecked ->
                onToggleVisibility(product, isChecked)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ProductWithVisibility>() {
        override fun areItemsTheSame(
            oldItem: ProductWithVisibility,
            newItem: ProductWithVisibility
        ): Boolean {
            return oldItem.product.id == newItem.product.id
        }

        override fun areContentsTheSame(
            oldItem: ProductWithVisibility,
            newItem: ProductWithVisibility
        ): Boolean {
            return oldItem == newItem
        }
    }
}