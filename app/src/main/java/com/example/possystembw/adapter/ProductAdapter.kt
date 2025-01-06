package com.example.possystembw.adapter

import android.graphics.Rect
import android.graphics.Typeface
import android.util.Log
import android.util.SparseArray
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.possystembw.R
import com.example.possystembw.database.Product


class ProductAdapter(private val onItemClick: (Product) -> Unit) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    class ProductViewHolder(itemView: View, private val onItemClick: (Product) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        private val productNameView: TextView = itemView.findViewById(R.id.textViewProductName)
        private val productPriceView: TextView = itemView.findViewById(R.id.textViewProductPrice)

        fun bind(product: Product) {
            val words = product.itemName.trim().split("\\s+".toRegex())
            val formattedText = StringBuilder()
            var currentLineLength = 0

            words.forEachIndexed { index, word ->
                // Check if adding this word would exceed 10 characters
                if (currentLineLength + word.length > 15 && currentLineLength > 0) {
                    // Start a new line
                    formattedText.append("\n")
                    currentLineLength = 0
                }

                // Add the word
                formattedText.append(word)
                currentLineLength += word.length

                // Add space if not last word and space won't exceed line limit
                if (index < words.size - 1) {
                    if (currentLineLength + 1 <= 12) {
                        formattedText.append("\u00A0") // Non-breaking space
                        currentLineLength += 1
                    } else {
                        formattedText.append("\n")
                        currentLineLength = 0
                    }
                }
            }

            productNameView.apply {
                text = formattedText.toString()
                post { requestLayout() }
            }

            productPriceView.text = "₱${product.price}"
            itemView.setOnClickListener { onItemClick(product) }
        }
    }
}

class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
    override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
        return oldItem.itemid== newItem.itemid
    }

    override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
        return oldItem == newItem
    }
}