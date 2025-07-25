package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.TransactionRecord
import android.util.Log


class TransactionItemsAdapter(
    private var items: MutableList<TransactionRecord>,
    private val onItemSelected: (TransactionRecord, Boolean) -> Unit,
    private val onSelectionChanged: (Int) -> Unit = {} // Callback for selection count changes
) : RecyclerView.Adapter<TransactionItemsAdapter.ItemViewHolder>() {

    private var isUpdating = false
    private val TAG = "Window1"

    init {
        // ADD THIS LOG to verify adapter receives correct data:
        Log.d("TransactionItemsAdapter", "=== ADAPTER INITIALIZED ===")
        Log.d("TransactionItemsAdapter", "Received ${items.size} items")
        items.forEachIndexed { index, item ->
            Log.d("TransactionItemsAdapter", "Item $index: ${item.name} - isReturned: ${item.isReturned}")
        }
        Log.d("TransactionItemsAdapter", "=== END ADAPTER INIT ===")

        // Check if any item has partial payment
        val hasPartialPayment = items.any { it.partialPaymentAmount > 0 }
        if (hasPartialPayment) {
            items.forEach { it.isSelected = true }
        }
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.itemCheckBox)
        private val nameTextView: TextView = itemView.findViewById(R.id.itemNameTextView)
        private val quantityTextView: TextView = itemView.findViewById(R.id.quantityTextView)
        private val priceTextView: TextView = itemView.findViewById(R.id.priceTextView)

        fun bind(transaction: TransactionRecord) {
            // FIXED: Prevent recursive updates
            if (isUpdating) return

            // FIXED: Clear any existing listener first to prevent unwanted triggers
            checkBox.setOnCheckedChangeListener(null)

            // Set the basic item information
            nameTextView.text = transaction.name
            quantityTextView.text = "Qty: ${transaction.quantity}"

            // FIXED: Show effective price (considering overrides)
            val effectivePrice = transaction.priceOverride?.takeIf { it > 0.0 } ?: transaction.price
            val itemTotal = effectivePrice * transaction.quantity
            priceTextView.text = String.format("₱%.2f", itemTotal)

            // FIXED: Set checkbox state BEFORE setting the listener
            checkBox.isChecked = transaction.isSelected

            // Handle partial payment logic
            if (transaction.partialPaymentAmount > 0) {
                checkBox.isEnabled = false
                checkBox.isChecked = true
                // Ensure the transaction is marked as selected for partial payments
                if (!transaction.isSelected) {
                    transaction.isSelected = true
                    onSelectionChanged(getSelectedItemCount())
                }

                // Visual indication for locked items
                itemView.alpha = 0.7f
                nameTextView.text = "${transaction.name} (Partial Payment - Required)"
            } else {
                checkBox.isEnabled = true
                itemView.alpha = 1.0f

                // FIXED: Set listener AFTER setting the state to prevent unwanted triggers
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    // FIXED: Use adapterPosition instead of bindingAdapterPosition
                    val currentPosition = adapterPosition
                    if (currentPosition != RecyclerView.NO_POSITION && currentPosition < items.size) {
                        val currentItem = items[currentPosition]
                        if (currentItem.isSelected != isChecked) {
                            currentItem.isSelected = isChecked
                            onItemSelected(currentItem, isChecked)
                            onSelectionChanged(getSelectedItemCount())
                        }
                    }
                }

                // FIXED: Add click listener to the entire item view for better UX
                itemView.setOnClickListener {
                    if (checkBox.isEnabled) {
                        checkBox.isChecked = !checkBox.isChecked
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction_detail, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        Log.d("TransactionItemsAdapter", "Binding item at position $position: ${item.name} - isReturned: ${item.isReturned}")
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    // FIXED: Enhanced update method that preserves selection states better
    fun updateItems(newItems: List<TransactionRecord>) {
        isUpdating = true

        // Create a map of existing selections for preservation
        val existingSelections = items.associate {
            "${it.itemId}_${it.lineNum}" to it.isSelected
        }

        // Clear and update items, preserving existing selections where possible
        items.clear()
        items.addAll(newItems.map { newItem ->
            val key = "${newItem.itemId}_${newItem.lineNum}"
            newItem.apply {
                isSelected = existingSelections[key] ?: isSelected
            }
        })

        isUpdating = false
        notifyDataSetChanged()

        // Notify selection change
        onSelectionChanged(getSelectedItemCount())
    }

    // FIXED: More reliable method to get selected items
    fun getSelectedItems(): List<TransactionRecord> {
        return items.filter { it.isSelected }
    }

    // FIXED: Enhanced select/deselect all method
    fun selectAllItems(select: Boolean) {
        isUpdating = true
        var hasChanges = false

        items.forEachIndexed { index, item ->
            if (item.partialPaymentAmount <= 0) { // Only change non-partial payment items
                if (item.isSelected != select) {
                    item.isSelected = select
                    hasChanges = true
                }
            }
        }

        isUpdating = false

        if (hasChanges) {
            notifyDataSetChanged()
            onSelectionChanged(getSelectedItemCount())
        }
    }

    // FIXED: Get selection count
    fun getSelectedItemCount(): Int {
        return items.count { it.isSelected }
    }

    // ADDED: Get total value of selected items
    fun getSelectedItemsValue(): Double {
        return items.filter { it.isSelected }.sumOf { item ->
            val effectivePrice = item.priceOverride?.takeIf { it > 0.0 } ?: item.price
            (effectivePrice * item.quantity) - (item.discountAmount ?: 0.0)
        }
    }

    // ADDED: Check if all selectable items are selected
    fun areAllSelectableItemsSelected(): Boolean {
        val selectableItems = items.filter { it.partialPaymentAmount <= 0 }
        return selectableItems.isNotEmpty() && selectableItems.all { it.isSelected }
    }

    // ADDED: Check if any items are selected
    fun hasSelectedItems(): Boolean {
        return items.any { it.isSelected }
    }

    // ADDED: Get items with partial payments
    fun getPartialPaymentItems(): List<TransactionRecord> {
        return items.filter { it.partialPaymentAmount > 0 }
    }
    fun hasReturnedItems(): Boolean {
        return items.any { it.isReturned || !it.returnTransactionId.isNullOrEmpty() }
    }

    // DEBUGGING: Add this function to help debug return status
    private fun debugItemReturnStatus(items: List<TransactionRecord>) {
        Log.d(TAG, "=== DEBUGGING ITEM RETURN STATUS ===")
        items.forEachIndexed { index, item ->
            Log.d(TAG, "Item $index: ${item.name}")
            Log.d(TAG, "  - ID: ${item.itemId}")
            Log.d(TAG, "  - LineNum: ${item.lineNum}")
            Log.d(TAG, "  - Quantity: ${item.quantity}")
            Log.d(TAG, "  - isReturned: ${item.isReturned}")
            Log.d(TAG, "  - returnTransactionId: ${item.returnTransactionId}")
            Log.d(TAG, "  - returnQuantity: ${item.returnQuantity}")
            Log.d(TAG, "  - Should show in return dialog: ${item.quantity > 0 && !item.isReturned && item.returnTransactionId.isNullOrEmpty()}")
            Log.d(TAG, "  ---")
        }
        Log.d(TAG, "=== END DEBUG ===")
    }
    // ADDED: Validate selection (for business rules)
    fun validateSelection(): String? {
        val selectedItems = getSelectedItems()

        when {
            selectedItems.isEmpty() -> return "Please select at least one item to return"
            selectedItems.any { it.quantity <= 0 } -> return "Cannot return items with zero or negative quantity"
            selectedItems.any { it.isReturned } -> return "Some selected items have already been returned"
            else -> return null // Valid selection
        }
    }
}
