package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.Discount

class DiscountAdapter(
    private val onDiscountSelected: (Discount) -> Unit
) : ListAdapter<Discount, DiscountAdapter.DiscountViewHolder>(DiscountDiffCallback()) {

    private var allDiscounts = listOf<Discount>()
    private var selectedPosition = RecyclerView.NO_POSITION

    inner class DiscountViewHolder(val button: Button) : RecyclerView.ViewHolder(button)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscountViewHolder {
        val button = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_discount_button, parent, false) as Button
        return DiscountViewHolder(button)
    }

    override fun onBindViewHolder(holder: DiscountViewHolder, position: Int) {
        val discount = getItem(position)

        // Format the discount text based on type
        val discountText = when (discount.DISCOUNTTYPE.uppercase()) {
            "FIXED" -> "${discount.DISCOFFERNAME}\nFixed ₱${discount.PARAMETER} off"
            "FIXEDTOTAL" -> "${discount.DISCOFFERNAME}\nFixed Total: ₱${discount.PARAMETER}"
            "PERCENTAGE" -> "${discount.DISCOFFERNAME}\nPercentage ${discount.PARAMETER}% off"
            else -> "${discount.DISCOFFERNAME}\n${discount.PARAMETER}"
        }

        holder.button.text = discountText
        holder.button.isSelected = position == selectedPosition

        holder.button.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition

            // Only notify if positions are valid and different
            if (previousPosition != RecyclerView.NO_POSITION && previousPosition < itemCount) {
                notifyItemChanged(previousPosition)
            }
            if (selectedPosition != RecyclerView.NO_POSITION && selectedPosition < itemCount) {
                notifyItemChanged(selectedPosition)
            }

            onDiscountSelected(discount)
        }
    }

    fun setDiscounts(discounts: List<Discount>) {
        allDiscounts = discounts
        submitList(discounts)
    }

    fun filter(query: String) {
        val filteredList = if (query.isEmpty()) {
            allDiscounts
        } else {
            allDiscounts.filter {
                it.DISCOFFERNAME.contains(query, ignoreCase = true) ||
                        it.PARAMETER.toString().contains(query, ignoreCase = true) ||
                        it.DISCOUNTTYPE.contains(query, ignoreCase = true)
            }
        }

        selectedPosition = RecyclerView.NO_POSITION  // Reset selection when filtering
        submitList(filteredList)
    }

    fun clearSelection() {
        val previousPosition = selectedPosition
        selectedPosition = RecyclerView.NO_POSITION
        if (previousPosition != RecyclerView.NO_POSITION && previousPosition < itemCount) {
            notifyItemChanged(previousPosition)
        }
    }

    private class DiscountDiffCallback : DiffUtil.ItemCallback<Discount>() {
        override fun areItemsTheSame(oldItem: Discount, newItem: Discount): Boolean {
            return oldItem.DISCOFFERNAME == newItem.DISCOFFERNAME &&
                    oldItem.DISCOUNTTYPE == newItem.DISCOUNTTYPE
        }

        override fun areContentsTheSame(oldItem: Discount, newItem: Discount): Boolean {
            return oldItem == newItem
        }
    }
}