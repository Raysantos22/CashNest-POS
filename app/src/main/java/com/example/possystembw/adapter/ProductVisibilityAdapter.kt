package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
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
        private val tvPlatformPrices: TextView = itemView.findViewById(R.id.tvPlatformPrices)
        private val switchVisibility: Switch = itemView.findViewById(R.id.switchVisibility)
        private val progressBarItem: ProgressBar = itemView.findViewById(R.id.progressBarItem)

        private var isUpdating = false

        fun bind(productWithVisibility: ProductWithVisibility) {
            val product = productWithVisibility.product

            // Set product details
            tvProductName.text = product.itemName
            tvProductGroup.text = product.itemGroup
            tvPrice.text = "₱${String.format("%.2f", product.price)}"

            // Show platform-specific prices
            setupPlatformPrices(product)

            // Reset loading state
            showItemLoading(false)

            // Setup switch with proper state management
            setupSwitch(productWithVisibility)
        }

        private fun setupPlatformPrices(product: Product) {
            val platformPricesText = buildString {
                if (product.foodpanda > 0) {
                    append("FP: ₱${String.format("%.2f", product.foodpanda)}")
                }
                if (product.grabfood > 0) {
                    if (isNotEmpty()) append(" | ")
                    append("GF: ₱${String.format("%.2f", product.grabfood)}")
                }
                if (product.manilaprice > 0) {
                    if (isNotEmpty()) append(" | ")
                    append("MR: ₱${String.format("%.2f", product.manilaprice)}")
                }
            }

            if (platformPricesText.isNotEmpty()) {
                tvPlatformPrices.text = platformPricesText
                tvPlatformPrices.visibility = View.VISIBLE
            } else {
                tvPlatformPrices.visibility = View.GONE
            }
        }

        private fun setupSwitch(productWithVisibility: ProductWithVisibility) {
            val product = productWithVisibility.product

            // Remove any existing listener to prevent unwanted triggers
            switchVisibility.setOnCheckedChangeListener(null)

            // Set the switch state based on visibility
            switchVisibility.isChecked = productWithVisibility.isVisible

            // Enable/disable switch appearance based on state
            switchVisibility.alpha = if (productWithVisibility.isVisible) 1.0f else 0.7f

            // Set the listener after setting the state
            switchVisibility.setOnCheckedChangeListener { _, isChecked ->
                if (!isUpdating) {
                    handleSwitchToggle(product, isChecked)
                }
            }
        }

        private fun handleSwitchToggle(product: Product, isChecked: Boolean) {
            isUpdating = true
            showItemLoading(true)

            // Update the visual state immediately for better UX
            switchVisibility.alpha = if (isChecked) 1.0f else 0.7f

            // Call the callback
            onToggleVisibility(product, isChecked)

            // Reset updating state after a delay
            itemView.postDelayed({
                isUpdating = false
                showItemLoading(false)
            }, 5000)
        }

        private fun showItemLoading(loading: Boolean) {
            if (loading) {
                progressBarItem.visibility = View.VISIBLE
                switchVisibility.visibility = View.INVISIBLE
            } else {
                progressBarItem.visibility = View.GONE
                switchVisibility.visibility = View.VISIBLE
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
            return oldItem.product == newItem.product && oldItem.isVisible == newItem.isVisible
        }
    }
}