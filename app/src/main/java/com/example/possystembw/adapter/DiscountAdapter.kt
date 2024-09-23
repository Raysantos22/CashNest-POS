package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.Discount
class DiscountAdapter(
    private val allDiscounts: List<Discount>,
    private val onDiscountSelected: (Discount) -> Unit
) : RecyclerView.Adapter<DiscountAdapter.DiscountViewHolder>() {

    private var filteredDiscounts = allDiscounts.toList()
    private var selectedPosition = RecyclerView.NO_POSITION

    inner class DiscountViewHolder(val button: Button) : RecyclerView.ViewHolder(button)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscountViewHolder {
        val button = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_discount_button, parent, false) as Button
        return DiscountViewHolder(button)
    }

    override fun onBindViewHolder(holder: DiscountViewHolder, position: Int) {
        val discount = filteredDiscounts[position]

        // Format the discount text based on type
        val discountText = when (discount.DISCOUNTTYPE.uppercase()) {
            "FIXED" -> "${discount.DISCOFFERNAME}\nFixed P${discount.PARAMETER} off"
            "FIXEDTOTAL" -> "${discount.DISCOFFERNAME}\nFixed Total: $${discount.PARAMETER}"
            "PERCENTAGE" -> "${discount.DISCOFFERNAME}\nPercentage${discount.PARAMETER}% off"
            else -> "${discount.DISCOFFERNAME}\n${discount.PARAMETER}"
        }

        holder.button.text = discountText

        holder.button.isSelected = position == selectedPosition

        holder.button.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            onDiscountSelected(discount)
        }
    }

    override fun getItemCount() = filteredDiscounts.size

    fun filter(query: String) {
        filteredDiscounts = if (query.isEmpty()) {
            allDiscounts
        } else {
            allDiscounts.filter {
                it.DISCOFFERNAME.contains(query, ignoreCase = true) ||
                        it.PARAMETER.toString().contains(query, ignoreCase = true) ||
                        it.DISCOUNTTYPE.contains(query, ignoreCase = true)
            }
        }
        selectedPosition = RecyclerView.NO_POSITION  // Reset selection when filtering
        notifyDataSetChanged()
    }

    fun getItem(position: Int): Discount = filteredDiscounts[position]
}