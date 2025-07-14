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
) : RecyclerView.Adapter<DiscountAdapter.DiscountViewHolder>() {

    private var discounts = listOf<Discount>()
    private var filteredDiscounts = listOf<Discount>()
    private var selectedPosition = -1
    private var windowType: String = "" // Add this to track current window type

    fun setDiscounts(newDiscounts: List<Discount>) {
        discounts = newDiscounts
        filteredDiscounts = newDiscounts
        notifyDataSetChanged()
    }

    fun setWindowType(type: String) {
        windowType = type
        notifyDataSetChanged() // Refresh to show appropriate parameters
    }

    fun filter(query: String) {
        filteredDiscounts = if (query.isEmpty()) {
            discounts
        } else {
            discounts.filter {
                it.DISCOFFERNAME.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscountViewHolder {
        val button = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_discount_button, parent, false) as Button
        return DiscountViewHolder(button)
    }

    override fun onBindViewHolder(holder: DiscountViewHolder, position: Int) {
        holder.bind(filteredDiscounts[position], position)
    }

    override fun getItemCount(): Int = filteredDiscounts.size

    inner class DiscountViewHolder(private val button: Button) : RecyclerView.ViewHolder(button) {

        fun bind(discount: Discount, position: Int) {
            // Get the appropriate parameter based on current window type
            val (parameter, windowLabel) = getParameterForWindow(discount, windowType)

            // Format the button text to show discount name and appropriate parameter
            val buttonText = buildString {
                append(discount.DISCOFFERNAME)
                append("\n")

                // Show the parameter value with type
                when (discount.DISCOUNTTYPE.uppercase()) {
                    "PERCENTAGE" -> append("${parameter} (${discount.DISCOUNTTYPE})")
                    "FIXED" -> append("₱${parameter} (${discount.DISCOUNTTYPE})")
                    "FIXEDTOTAL" -> append("₱${parameter} (${discount.DISCOUNTTYPE})")
                    else -> append("${parameter} (${discount.DISCOUNTTYPE})")
                }

                // Add window type indicator if using specific parameter
                if (windowLabel != "Default") {
                    append(" ($windowLabel)")
                }
            }

            button.text = buttonText

            // Highlight if this discount has a specific parameter for current window type
            val hasSpecificParameter = when {
                windowType.contains("GRABFOOD") -> discount.GRABFOOD_PARAMETER != null
                windowType.contains("FOODPANDA") -> discount.FOODPANDA_PARAMETER != null
                windowType.contains("MANILARATE") -> discount.MANILAPRICE_PARAMETER != null
                else -> false
            }

            // Update button appearance based on selection and specific parameter availability
            button.isSelected = selectedPosition == position

            if (hasSpecificParameter) {
                // Use a different background or text color to indicate specific parameter
                button.setBackgroundResource(R.drawable.discount_button_selected)
            } else {
                button.setBackgroundResource(R.drawable.discount_button_selector)
            }

            button.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = position

                // Notify adapter to update button states
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)

                onDiscountSelected(discount)
            }
        }

        private fun getParameterForWindow(discount: Discount, windowType: String): Pair<Int, String> {
            return when {
                windowType.contains("GRABFOOD 1") -> {
                    Pair(
                        discount.GRABFOOD_PARAMETER ?: discount.PARAMETER,
                        if (discount.GRABFOOD_PARAMETER != null) "GF" else "Default"
                    )
                }
                windowType.contains("FOODPANDA 1") -> {
                    Pair(
                        discount.FOODPANDA_PARAMETER ?: discount.PARAMETER,
                        if (discount.FOODPANDA_PARAMETER != null) "FP" else "Default"
                    )
                }
                windowType.contains("MANILARATE 1") -> {
                    Pair(
                        discount.MANILAPRICE_PARAMETER ?: discount.PARAMETER,
                        if (discount.MANILAPRICE_PARAMETER != null) "MR" else   "Default"
                    )
                }
                windowType.contains("MALLPRICE 1") -> {
                    Pair(
                        discount.MANILAPRICE_PARAMETER ?: discount.PARAMETER,
                        if (discount.MANILAPRICE_PARAMETER != null) "MP" else   "Default"
                    )
                }
                windowType.contains("FOODPANDAMALL 1") -> {
                    Pair(
                        discount.MANILAPRICE_PARAMETER ?: discount.PARAMETER,
                        if (discount.MANILAPRICE_PARAMETER != null) "FPM" else   "Default"
                    )
                }
                windowType.contains("GRABFOODMALL 1") -> {
                    Pair(
                        discount.MANILAPRICE_PARAMETER ?: discount.PARAMETER,
                        if (discount.MANILAPRICE_PARAMETER != null) "GFM" else   "Default"
                    )
                }
                else -> {
                    Pair(discount.PARAMETER, "Default")
                }
            }
        }
    }
}

//class DiscountAdapter(
//    private val onDiscountSelected: (Discount) -> Unit
//) : ListAdapter<Discount, DiscountAdapter.DiscountViewHolder>(DiscountDiffCallback()) {
//
//    private var allDiscounts = listOf<Discount>()
//    private var selectedPosition = RecyclerView.NO_POSITION
//
//    inner class DiscountViewHolder(val button: Button) : RecyclerView.ViewHolder(button)
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscountViewHolder {
//        val button = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_discount_button, parent, false) as Button
//        return DiscountViewHolder(button)
//    }
//
//    override fun onBindViewHolder(holder: DiscountViewHolder, position: Int) {
//        val discount = getItem(position)
//
//        // Format the discount text based on type
//        val discountText = when (discount.DISCOUNTTYPE.uppercase()) {
//            "FIXED" -> "${discount.DISCOFFERNAME}\nFixed ₱${discount.PARAMETER} off"
//            "FIXEDTOTAL" -> "${discount.DISCOFFERNAME}\nFixed Total: ₱${discount.PARAMETER}"
//            "PERCENTAGE" -> "${discount.DISCOFFERNAME}\nPercentage ${discount.PARAMETER}% off"
//            else -> "${discount.DISCOFFERNAME}\n${discount.PARAMETER}"
//        }
//
//        holder.button.text = discountText
//        holder.button.isSelected = position == selectedPosition
//
//        holder.button.setOnClickListener {
//            val previousPosition = selectedPosition
//            selectedPosition = holder.adapterPosition
//
//            // Only notify if positions are valid and different
//            if (previousPosition != RecyclerView.NO_POSITION && previousPosition < itemCount) {
//                notifyItemChanged(previousPosition)
//            }
//            if (selectedPosition != RecyclerView.NO_POSITION && selectedPosition < itemCount) {
//                notifyItemChanged(selectedPosition)
//            }
//
//            onDiscountSelected(discount)
//        }
//    }
//
//    fun setDiscounts(discounts: List<Discount>) {
//        allDiscounts = discounts
//        submitList(discounts)
//    }
//
//    fun filter(query: String) {
//        val filteredList = if (query.isEmpty()) {
//            allDiscounts
//        } else {
//            allDiscounts.filter {
//                it.DISCOFFERNAME.contains(query, ignoreCase = true) ||
//                        it.PARAMETER.toString().contains(query, ignoreCase = true) ||
//                        it.DISCOUNTTYPE.contains(query, ignoreCase = true)
//            }
//        }
//
//        selectedPosition = RecyclerView.NO_POSITION  // Reset selection when filtering
//        submitList(filteredList)
//    }
//
//    fun clearSelection() {
//        val previousPosition = selectedPosition
//        selectedPosition = RecyclerView.NO_POSITION
//        if (previousPosition != RecyclerView.NO_POSITION && previousPosition < itemCount) {
//            notifyItemChanged(previousPosition)
//        }
//    }
//
//    private class DiscountDiffCallback : DiffUtil.ItemCallback<Discount>() {
//        override fun areItemsTheSame(oldItem: Discount, newItem: Discount): Boolean {
//            return oldItem.DISCOFFERNAME == newItem.DISCOFFERNAME &&
//                    oldItem.DISCOUNTTYPE == newItem.DISCOUNTTYPE
//        }
//
//        override fun areContentsTheSame(oldItem: Discount, newItem: Discount): Boolean {
//            return oldItem == newItem
//        }
//    }
//}
