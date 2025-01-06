package com.example.possystembw.adapter

import android.content.Context
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.CartItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


class CartAdapter(
    private val onItemClick: (CartItem) -> Unit,
    private val onDeleteClick: (CartItem) -> Unit,
    private val onQuantityChange: (CartItem, Int) -> Unit,
    private val onDiscountLongPress: (CartItem) -> Unit
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    private var deletionEnabled: Boolean = true
    private var partialPaymentApplied: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.cart_item_layout, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val cartItem = getItem(position)
        holder.bind(cartItem, deletionEnabled)
    }

    fun setDeletionEnabled(enabled: Boolean) {
        deletionEnabled = enabled
        notifyDataSetChanged()
    }

    fun setPartialPaymentApplied(applied: Boolean) {
        partialPaymentApplied = applied
        notifyDataSetChanged()
    }

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productNameView: TextView = itemView.findViewById(R.id.textViewCartProductName)
        private val quantityView: TextView = itemView.findViewById(R.id.textViewCartProductQuantity)
        private val priceView: TextView = itemView.findViewById(R.id.textViewCartProductPrice)
        private val originalPriceView: TextView = itemView.findViewById(R.id.textViewOriginalPrice)
        private val discountInfoView: TextView = itemView.findViewById(R.id.discountInfoTextView)
        private val bundleInfoView: TextView = itemView.findViewById(R.id.bundleInfoTextView)
        val deleteButton: ImageView = itemView.findViewById(R.id.imageViewDelete)

        fun bind(cartItem: CartItem, deletionEnabled: Boolean) {
            productNameView.text = cartItem.productName
            quantityView.text = "x${cartItem.quantity}"

            // Add click listener to the quantity view
            quantityView.setOnClickListener {
                if (!partialPaymentApplied) {
                    if (cartItem.bundleId != null) {
                        showBundleQuantityDialog(cartItem)
                    } else {
                        showQuantityEditDialog(cartItem)
                    }
                }
            }

            // Get the base price and quantity
            val basePrice = cartItem.price
            val overriddenPrice = cartItem.overriddenPrice
            val quantity = cartItem.quantity
            val baseTotal = basePrice * quantity

            // Calculate final price based on discount type and bundle status
            val finalPrice = when {
                overriddenPrice != null -> overriddenPrice * quantity
                cartItem.bundleId != null -> when (cartItem.discountType.uppercase()) {
                    "FIXED" -> cartItem.discount * quantity
                    "PERCENTAGE" -> baseTotal * (1 - cartItem.discount / 100)
                    "FIXEDTOTAL" -> baseTotal - cartItem.discount
                    else -> baseTotal
                }
                cartItem.discount > 0 -> when (cartItem.discountType.uppercase()) {
                    "PERCENTAGE" -> baseTotal * (1 - cartItem.discount / 100)
                    "FIXED" -> (basePrice - cartItem.discount) * quantity
                    "FIXEDTOTAL" -> baseTotal - cartItem.discount
                    else -> baseTotal
                }
                else -> baseTotal
            }

            // Display current price
            priceView.text = "P${String.format("%.2f", finalPrice)}"

            // Show original price if there's a price override, bundle, or discount
            if (overriddenPrice != null || cartItem.bundleId != null || cartItem.discount > 0) {
                originalPriceView.visibility = View.VISIBLE
                originalPriceView.text = "P${String.format("%.2f", baseTotal)}"
                originalPriceView.paintFlags = originalPriceView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                originalPriceView.visibility = View.GONE
            }

            // Show bundle information if item is part of a bundle
            bundleInfoView.visibility = if (cartItem.bundleId != null) {
                bundleInfoView.text = cartItem.mixMatchId
                View.VISIBLE
            } else {
                View.GONE
            }

            // Show discount information
            when {
                cartItem.bundleId != null -> {
                    discountInfoView.visibility = View.VISIBLE
                    discountInfoView.text = when (cartItem.discountType.uppercase()) {
                        "FIXED" -> "Bundle Price: P${String.format("%.2f", cartItem.discount)}"
                        "PERCENTAGE" -> "Bundle Discount: ${cartItem.discount}%"
                        "FIXEDTOTAL" -> "Bundle Discount: P${String.format("%.2f", cartItem.discount)}"
                        else -> "Bundle Applied"
                    }
                }
                cartItem.discount > 0 -> {
                    discountInfoView.visibility = View.VISIBLE
                    discountInfoView.text = when (cartItem.discountType.uppercase()) {
                        "PERCENTAGE" -> "Discount: ${cartItem.discount}%"
                        "FIXED" -> "Discount: P${String.format("%.2f", cartItem.discount)}"
                        "FIXEDTOTAL" -> "Discount: P${String.format("%.2f", cartItem.discount)}"
                        else -> "Discount Applied"
                    }
                }
                else -> {
                    discountInfoView.visibility = View.GONE
                }
            }

            // Set delete button visibility and action
            deleteButton.isVisible = deletionEnabled && !partialPaymentApplied
            deleteButton.isEnabled = deletionEnabled && !partialPaymentApplied
            deleteButton.setOnClickListener {
                if (deletionEnabled && !partialPaymentApplied) {
                    onDeleteClick(cartItem)
                }
            }

            // Simple long click listener for discount dialog
            itemView.setOnLongClickListener {
                onDiscountLongPress(cartItem)
                true
            }
        }
        private fun showBundleQuantityDialog(cartItem: CartItem) {
            val currentList = currentList

            // Get all items from the same bundle
            val bundleItems = currentList.filter {
                it.bundleId == cartItem.bundleId && it.mixMatchId == cartItem.mixMatchId
            }

            val builder = AlertDialog.Builder(itemView.context)
            val dialogView = LayoutInflater.from(itemView.context)
                .inflate(R.layout.dialog_edit_bundle_quantity, null)

            val editTextQuantity = dialogView.findViewById<EditText>(R.id.editTextQuantity)
            val bundleInfoText = dialogView.findViewById<TextView>(R.id.bundleInfoText)

            // Calculate current bundle totals - for FIXEDTOTAL, use the original discount without multiplication
            val currentFixedTotalDiscount = if (cartItem.discountType.uppercase() == "FIXEDTOTAL") {
                bundleItems.firstOrNull()?.discount ?: 0.0
            } else 0.0

            // Show current items and discount in bundle
            val itemsList = bundleItems.joinToString("\n") { item ->
                "${item.productName} x${item.quantity}"
            }

            bundleInfoText.text = """
        Bundle: ${cartItem.mixMatchId}
        Items in bundle:
        $itemsList
        Current Discount: P${String.format("%.2f", currentFixedTotalDiscount)}
    """.trimIndent()

            editTextQuantity.setText("1")
            editTextQuantity.selectAll()

            editTextQuantity.requestFocus()
            val imm = itemView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)

            builder.setView(dialogView)
                .setTitle("Edit Bundle Quantity")
                .setPositiveButton("Update") { dialog, _ ->
                    val multiplier = editTextQuantity.text.toString().toIntOrNull() ?: 1
                    if (multiplier > 0) {
                        // Update all items in the bundle with their existing proportions
                        bundleItems.forEach { bundleItem ->
                            // Get original mix match quantity from the item
                            val originalMixMatchQty = bundleItem.quantity / (bundleItem.lineNum ?: 1)
                            val newQuantity = originalMixMatchQty * multiplier

                            // Calculate new discount based on the discount type
                            val updatedCartItem = when (bundleItem.discountType.uppercase()) {
                                "FIXED" -> {
                                    bundleItem.copy(
                                        quantity = newQuantity,
                                        discount = bundleItem.discount * newQuantity,  // Keep original discount
                                        discountAmount = bundleItem.discount * newQuantity  // Total discount
                                    )
                                }
                                "PERCENTAGE" -> {
                                    val baseTotal = bundleItem.price * newQuantity
                                    bundleItem.copy(
                                        quantity = newQuantity,
                                        discount = bundleItem.discount,  // Keep original percentage
                                        discountAmount = baseTotal * (bundleItem.discount / 100)
                                    )
                                }
                                "FIXEDTOTAL" -> {
                                    // For FIXEDTOTAL, keep the original discount as is - don't multiply
                                    bundleItem.copy(
                                        quantity = newQuantity,
                                        discount = bundleItem.discount * multiplier,  // Keep original bundle discount
                                        discountAmount = bundleItem.discount // Keep original discount amount
                                    )
                                }
                                else -> bundleItem.copy(quantity = newQuantity)
                            }
                            onQuantityChange(updatedCartItem, updatedCartItem.quantity)
                        }
                    } else {
                        Toast.makeText(
                            itemView.context,
                            "Quantity must be greater than 0",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    imm.hideSoftInputFromWindow(editTextQuantity.windowToken, 0)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    imm.hideSoftInputFromWindow(editTextQuantity.windowToken, 0)
                }
                .setOnCancelListener {
                    imm.hideSoftInputFromWindow(editTextQuantity.windowToken, 0)
                }
                .show()
        }
        private fun showQuantityEditDialog(cartItem: CartItem) {
            val builder = AlertDialog.Builder(itemView.context)
            val inflater = LayoutInflater.from(itemView.context)
            val dialogView = inflater.inflate(R.layout.dialog_edit_quantity, null)
            val editTextQuantity = dialogView.findViewById<EditText>(R.id.editTextQuantity)

            editTextQuantity.setText(cartItem.quantity.toString())
            editTextQuantity.selectAll()

            editTextQuantity.requestFocus()
            val imm = itemView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)

            builder.setView(dialogView)
                .setTitle("Edit Quantity")
                .setPositiveButton("Update") { dialog, _ ->
                    val newQuantity = editTextQuantity.text.toString().toIntOrNull()
                    if (newQuantity != null && newQuantity > 0) {
                        onQuantityChange(cartItem, newQuantity)
                    }
                    imm.hideSoftInputFromWindow(editTextQuantity.windowToken, 0)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    imm.hideSoftInputFromWindow(editTextQuantity.windowToken, 0)
                }
                .setOnCancelListener {
                    imm.hideSoftInputFromWindow(editTextQuantity.windowToken, 0)
                }
                .show()
        }
    }
}

class CartDiffCallback : DiffUtil.ItemCallback<CartItem>() {
    override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
        return oldItem == newItem
    }
}
//private fun showBundleQuantityDialog(cartItem: CartItem) {
//    val currentList = currentList
//
//    // Get all items from the same bundle
//    val bundleItems = currentList.filter {
//        it.bundleId == cartItem.bundleId && it.mixMatchId == cartItem.mixMatchId
//    }
//
//    val builder = AlertDialog.Builder(itemView.context)
//    val dialogView = LayoutInflater.from(itemView.context)
//        .inflate(R.layout.dialog_edit_bundle_quantity, null)
//
//    val editTextQuantity = dialogView.findViewById<EditText>(R.id.editTextQuantity)
//    val bundleInfoText = dialogView.findViewById<TextView>(R.id.bundleInfoText)
//
//    // Calculate current bundle totals
//    val currentFixedTotalDiscount = if (cartItem.discountType.uppercase() == "FIXEDTOTAL") {
//        bundleItems.firstOrNull()?.discount ?: 0.0
//    } else 0.0
//
//    // Show current items and discount in bundle
//    val itemsList = bundleItems.joinToString("\n") { item ->
//        "${item.productName} x${item.quantity}"
//    }
//
//    bundleInfoText.text = """
//        Bundle: ${cartItem.mixMatchId}
//        Items in bundle:
//        $itemsList
//        Current Discount: P${String.format("%.2f", currentFixedTotalDiscount)}
//    """.trimIndent()
//
//    editTextQuantity.setText("1")
//    editTextQuantity.selectAll()
//
//    editTextQuantity.requestFocus()
//    val imm = itemView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
//
//    builder.setView(dialogView)
//        .setTitle("Edit Bundle Quantity")
//        .setPositiveButton("Update") { dialog, _ ->
//            val multiplier = editTextQuantity.text.toString().toIntOrNull() ?: 1
//            if (multiplier > 0) {
//                // Update all items in the bundle with their existing proportions
//                bundleItems.forEach { bundleItem ->
//                    // Get original mix match quantity from the item
//                    val originalMixMatchQty = bundleItem.quantity / (bundleItem.lineNum ?: 1)
//                    val newQuantity = originalMixMatchQty * multiplier
//
//                    // Calculate new discount based on the discount type using direct multiplier
//                    val updatedCartItem = when (bundleItem.discountType.uppercase()) {
//                        "FIXED" -> {
//                            // Multiply original discount directly by user's input multiplier
//                            val totalDiscount = bundleItem.discount * multiplier
//                            bundleItem.copy(
//                                quantity = newQuantity,
//                                discount = bundleItem.discount,  // Keep original discount
//                                discountAmount = totalDiscount  // Multiply discount by user input
//                            )
//                        }
//                        "PERCENTAGE" -> {
//                            // Keep original percentage, apply to new base total
//                            val baseTotal = bundleItem.price * newQuantity
//                            val totalDiscount = baseTotal * (bundleItem.discount / 100)
//                            bundleItem.copy(
//                                quantity = newQuantity,
//                                discount = bundleItem.discount,  // Keep original percentage
//                                discountAmount = totalDiscount
//                            )
//                        }
//                        "FIXEDTOTAL" -> {
//                            // Simply multiply the original discount by user's input multiplier
//                            val totalDiscount = bundleItem.discount * multiplier
//                            bundleItem.copy(
//                                quantity = newQuantity,
//                                discount = bundleItem.discount,  // Keep original discount
//                                discountAmount = totalDiscount  // Multiply by user input
//                            )
//                        }
//                        else -> bundleItem.copy(quantity = newQuantity)
//                    }
//                    onQuantityChange(updatedCartItem, updatedCartItem.quantity)
//                }
//            } else {
//                Toast.makeText(
//                    itemView.context,
//                    "Quantity must be greater than 0",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//            imm.hideSoftInputFromWindow(editTextQuantity.windowToken, 0)
//        }
//        .setNegativeButton("Cancel") { dialog, _ ->
//            imm.hideSoftInputFromWindow(editTextQuantity.windowToken, 0)
//        }
//        .setOnCancelListener {
//            imm.hideSoftInputFromWindow(editTextQuantity.windowToken, 0)
//        }
//        .show()
//}