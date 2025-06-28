package com.example.possystembw.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.Product

class ProductSearchAdapter(
    private val onItemSelected: (Product) -> Unit
) : RecyclerView.Adapter<ProductSearchAdapter.ViewHolder>() {

    private var products: List<Product> = emptyList()
    private var itemClickListener: ((Product) -> Unit)? = null

    fun submitList(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (Product) -> Unit) {
        itemClickListener = listener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvItemName: TextView =   itemView.findViewById(R.id.tvItemName)
        val tvItemGroup: TextView = itemView.findViewById(R.id.tvItemGroup)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]

        holder.tvItemName.text = product.itemName
        holder.tvItemGroup.text = product.itemGroup
        holder.tvPrice.text = String.format("₱%.2f", product.price)

        holder.itemView.setOnClickListener {
            itemClickListener?.invoke(product) ?: onItemSelected(product)
        }
    }

    override fun getItemCount(): Int = products.size
}