package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.ProductBundle

// First, let's create the BundleAdapter
class BundleAdapter(
    private val onBundleSelected: (ProductBundle) -> Unit
) : ListAdapter<ProductBundle, BundleAdapter.BundleViewHolder>(BundleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BundleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bundle, parent, false)
        return BundleViewHolder(view)
    }

    override fun onBindViewHolder(holder: BundleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BundleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.bundleNameText)
        private val descriptionText: TextView = itemView.findViewById(R.id.bundleDescriptionText)

        fun bind(bundle: ProductBundle) {
            nameText.text = bundle.name
            descriptionText.text = bundle.description
            itemView.setOnClickListener { onBundleSelected(bundle) }
        }
    }

    class BundleDiffCallback : DiffUtil.ItemCallback<ProductBundle>() {
        override fun areItemsTheSame(oldItem: ProductBundle, newItem: ProductBundle) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ProductBundle, newItem: ProductBundle) =
            oldItem == newItem
    }
}