package com.example.possystembw.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.CartItem

class PriceOverrideAdapter(
    private val isMobileLayout: Boolean = false,  // Add mobile layout flag
    private val onPriceOverride: (CartItem, Double) -> Unit,
    private val onPriceReset: (CartItem) -> Unit,
    private val onEditTextClicked: (EditText) -> Unit,
) : ListAdapter<CartItem, PriceOverrideAdapter.ViewHolder>(CartDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_price_override, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productNameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
        private val currentPriceTextView: TextView = itemView.findViewById(R.id.currentPriceTextView)
        private val originalPriceTextView: TextView = itemView.findViewById(R.id.originalPriceTextView)
        private val effectivePriceTextView: TextView = itemView.findViewById(R.id.effectivePriceTextView)
        private val overridePriceEditText: EditText = itemView.findViewById(R.id.overridePriceEditText)
        private val applyOverrideButton: Button = itemView.findViewById(R.id.applyOverrideButton)
        private val resetPriceButton: Button = itemView.findViewById(R.id.resetPriceButton)

        fun bind(cartItem: CartItem) {
            // Apply mobile-specific styling to TextViews
            if (isMobileLayout) {
                productNameTextView.textSize = 14f
                originalPriceTextView.textSize = 11f
                currentPriceTextView.textSize = 11f
                effectivePriceTextView.textSize = 12f
                overridePriceEditText.textSize = 12f

                // Adjust button text sizes
                applyOverrideButton.textSize = 10f
                resetPriceButton.textSize = 10f

                // Adjust padding for mobile
                val paddingDp = (8 * itemView.context.resources.displayMetrics.density).toInt()
                itemView.setPadding(paddingDp, paddingDp/2, paddingDp, paddingDp/2)
            }

            productNameTextView.text = cartItem.productName
            originalPriceTextView.text = "Original: P${String.format("%.2f", cartItem.price)}"

            val currentBasePrice = cartItem.overriddenPrice ?: cartItem.price
            currentPriceTextView.text = "Current: P${String.format("%.2f", currentBasePrice)}"

            val effectivePrice = when (cartItem.discountType) {
                "percentage" -> currentBasePrice * (1 - cartItem.discount / 100)
                "fixed" -> currentBasePrice - cartItem.discount
                else -> currentBasePrice
            }
            effectivePriceTextView.text = "Effective Price: P${String.format("%.2f", effectivePrice)}"

            overridePriceEditText.setText(currentBasePrice.toString())

            // Set input type to number with decimal
            overridePriceEditText.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

            // Enable focusing and editing
            overridePriceEditText.isFocusableInTouchMode = true
            overridePriceEditText.isFocusable = true

            overridePriceEditText.setOnClickListener {
                it.requestFocus()
                showKeyboard(it)
            }

            overridePriceEditText.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    showKeyboard(v)
                }
            }
            overridePriceEditText.setOnClickListener {
                onEditTextClicked(overridePriceEditText)
            }

            overridePriceEditText.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    onEditTextClicked(overridePriceEditText)
                }
            }

            applyOverrideButton.setOnClickListener {
                val newPrice = overridePriceEditText.text.toString().toDoubleOrNull()
                if (newPrice != null) {
                    // Check if the new price is different from the original price
                    if (newPrice != cartItem.price) {
                        onPriceOverride(cartItem, newPrice)
                        // The dialog will be closed in the onPriceOverride lambda
                    } else {
                        // Optionally show a message that the price is the same
                        Toast.makeText(itemView.context, "The new price must be different from the original price.", Toast.LENGTH_SHORT).show()
                    }
                }
                hideKeyboard(overridePriceEditText)
            }

            resetPriceButton.setOnClickListener {
                onPriceReset(cartItem)
                overridePriceEditText.setText(cartItem.price.toString())
                currentPriceTextView.text = "Current: P${String.format("%.2f", cartItem.price)}"
                effectivePriceTextView.text = "Effective Price: P${String.format("%.2f", cartItem.price)}"
                resetPriceButton.isEnabled = false
                hideKeyboard(overridePriceEditText)
            }

            resetPriceButton.isEnabled = cartItem.overriddenPrice != null
        }

        private fun showKeyboard(view: View) {
            val imm = itemView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }

        private fun hideKeyboard(view: View) {
            val imm = itemView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}