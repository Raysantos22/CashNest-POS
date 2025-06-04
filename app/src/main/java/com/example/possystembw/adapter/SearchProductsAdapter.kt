//package com.example.possystembw.adapter
//
//import android.graphics.Color
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.TextView
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.ListAdapter
//import androidx.recyclerview.widget.RecyclerView
//import com.example.possystembw.R
//import com.example.possystembw.database.Product
//import com.example.possystembw.ui.ProductWithVisibility
//
//class SearchProductsAdapter(
//    private val onProductClick: (Product, Boolean) -> Unit
//) : ListAdapter<ProductWithVisibility, SearchProductsAdapter.ViewHolder>(DiffCallback()) {
//
//    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
//        val tvProductGroup: TextView = itemView.findViewById(R.id.tvProductGroup)
//        val tvProductPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
//        val tvVisibilityStatus: TextView = itemView.findViewById(R.id.tvVisibilityStatus)
//        val btnToggleVisibility: Button = itemView.findViewById(R.id.btnToggleVisibility)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_search_product, parent, false)
//        return ViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val item = getItem(position)
//        holder.tvProductName.text = item.product.itemName
//        holder.tvProductGroup.text = item.product.itemGroup
//        holder.tvProductPrice.text = "₱${item.product.price}"
//
//        if (item.isHidden) {
//            holder.tvVisibilityStatus.text = "Hidden"
//            holder.tvVisibilityStatus.setTextColor(Color.RED)
//            holder.btnToggleVisibility.text = "Show"
//            holder.btnToggleVisibility.setBackgroundColor(Color.GREEN)
//        } else {
//            holder.tvVisibilityStatus.text = "Visible"
//            holder.tvVisibilityStatus.setTextColor(Color.GREEN)
//            holder.btnToggleVisibility.text = "Hide"
//            holder.btnToggleVisibility.setBackgroundColor(Color.RED)
//        }
//
//        holder.btnToggleVisibility.setOnClickListener {
//            onProductClick(item.product, item.isHidden)
//        }
//    }
//
//    class DiffCallback : DiffUtil.ItemCallback<ProductWithVisibility>() {
//        override fun areItemsTheSame(oldItem: ProductWithVisibility, newItem: ProductWithVisibility): Boolean {
//            return oldItem.product.id == newItem.product.id
//        }
//
//        override fun areContentsTheSame(oldItem: ProductWithVisibility, newItem: ProductWithVisibility): Boolean {
//            return oldItem == newItem
//        }
//    }
//}