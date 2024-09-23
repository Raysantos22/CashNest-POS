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
                    showQuantityEditDialog(cartItem)
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