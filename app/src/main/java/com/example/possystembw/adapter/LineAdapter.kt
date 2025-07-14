package com.example.possystembw.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.DAO.LineTransaction
import com.example.possystembw.R

class LineAdapter : RecyclerView.Adapter<LineAdapter.LineViewHolder>() {
    private var items: List<LineTransaction> = emptyList()
    private val modifiedItems = HashMap<Int, LineTransaction>()
    private val successfullySentItems = HashSet<String>()
    private var onItemModified: ((List<LineTransaction>) -> Unit)? = null
    private val inputCache = mutableMapOf<String, InputValues>()
    private var originalItems: List<LineTransaction> = emptyList()
    private var filteredItems: List<LineTransaction> = emptyList()

    data class InputValues(
        var adjustment: String = "",
        var receivedCount: String = "",
        var transferCount: String = "",
        var wasteCount: String = "",
        var counted: String = "",
        var wasteType: String? = null
    )

    companion object {
        private const val TAG = "LineAdapter"
    }

    fun setItems(newItems: List<LineTransaction>) {
        originalItems = newItems
        filteredItems = newItems
        items = newItems // Add this line to keep items in sync

        // Clear input cache when setting new items to prevent stale data
        inputCache.clear()
        modifiedItems.clear()

        notifyDataSetChanged()
    }

    fun filterItems(query: String) {
        filteredItems = if (query.isEmpty()) {
            originalItems
        } else {
            originalItems.filter {
                (it.itemId?.contains(query, ignoreCase = true) == true) ||
                        (it.itemName?.contains(query, ignoreCase = true) == true) ||
                        (it.itemDepartment?.contains(query, ignoreCase = true) == true)
            }
        }
        notifyDataSetChanged()
    }

    fun updateItems(newItems: List<LineTransaction>) {
        items = newItems
        filteredItems = newItems
        // Don't clear caches on update to preserve user input
        notifyDataSetChanged()
    }

    fun setOnItemModifiedListener(listener: (List<LineTransaction>) -> Unit) {
        onItemModified = listener
    }

    fun getAllItems(): List<LineTransaction> = originalItems

    fun getCurrentList(): List<LineTransaction> = filteredItems

    fun getUpdatedItems(): List<LineTransaction> {
        return originalItems.map { original ->
            val itemId = original.itemId
            val cachedInput = inputCache[itemId]
            if (cachedInput != null) {
                // Check if any values have actually changed
                val hasChanged = cachedInput.adjustment != formatNumber(original.adjustment) ||
                        cachedInput.receivedCount != formatNumber(original.receivedCount) ||
                        cachedInput.transferCount != formatNumber(original.transferCount) ||
                        cachedInput.wasteCount != formatNumber(original.wasteCount) ||
                        cachedInput.counted != formatNumber(original.counted) ||
                        cachedInput.wasteType != original.wasteType

                if (hasChanged) {
                    original.copy(
                        adjustment = cachedInput.adjustment,
                        receivedCount = cachedInput.receivedCount,
                        transferCount = cachedInput.transferCount,
                        wasteCount = cachedInput.wasteCount,
                        counted = cachedInput.counted,
                        wasteType = cachedInput.wasteType,
                        syncStatus = 0 // Mark as unsynced only if changed
                    )
                } else {
                    original // Keep original if no changes
                }
            } else {
                original // Keep original if no input cached
            }
        }
    }

    fun hasModifications(): Boolean {
        // Check if there are any real changes in the input cache
        for ((itemId, cachedValues) in inputCache) {
            val originalItem = originalItems.find { it.itemId == itemId } ?: continue

            // Compare values to see if anything actually changed
            if (cachedValues.adjustment != formatNumber(originalItem.adjustment) ||
                cachedValues.receivedCount != formatNumber(originalItem.receivedCount) ||
                cachedValues.transferCount != formatNumber(originalItem.transferCount) ||
                cachedValues.wasteCount != formatNumber(originalItem.wasteCount) ||
                cachedValues.counted != formatNumber(originalItem.counted) ||
                cachedValues.wasteType != originalItem.wasteType) {
                return true
            }
        }
        return false
    }

    // Helper method to format numbers consistently
    private fun formatNumber(value: String?): String {
        if (value.isNullOrEmpty()) return "0"
        return try {
            val number = value.toDouble()
            number.toInt().toString()
        } catch (e: NumberFormatException) {
            "0"
        }
    }

    fun markItemAsSent(itemId: String) {
        if (!itemId.isNullOrEmpty()) {
            successfullySentItems.add(itemId)

            // Find and update the item in originalItems to mark as synced
            originalItems = originalItems.map { item ->
                if (item.itemId == itemId) {
                    item.copy(syncStatus = 1)
                } else {
                    item
                }
            }

            // Update filtered items as well
            filteredItems = filteredItems.map { item ->
                if (item.itemId == itemId) {
                    item.copy(syncStatus = 1)
                } else {
                    item
                }
            }

            notifyDataSetChanged()
        }
    }

    fun refreshItem(position: Int) {
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_line_detail, parent, false)
        return LineViewHolder(
            view,
            { position, item ->
                modifiedItems[position] = item
                onItemModified?.invoke(getUpdatedItems())
            },
            { itemId -> inputCache[itemId] },
            { itemId, values ->
                inputCache[itemId] = values
                // Notify that item was modified but don't auto-save
                onItemModified?.invoke(getUpdatedItems())
            }
        )
    }

    override fun onBindViewHolder(holder: LineViewHolder, position: Int) {
        holder.bind(filteredItems[position], position)
    }

    override fun getItemCount(): Int = filteredItems.size

    class LineViewHolder(
        view: View,
        private val onItemUpdated: (position: Int, item: LineTransaction) -> Unit,
        private val getInputCache: (String) -> InputValues?,
        private val saveToInputCache: (String, InputValues) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val tvItemId: TextView = view.findViewById(R.id.tvItemId)
        private val tvItemName: TextView = view.findViewById(R.id.tvItemName)
        private val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        private val etOrder: EditText = view.findViewById(R.id.etOrder)
        private val etActualReceived: EditText = view.findViewById(R.id.etActualReceived)
        private val tvVariance: TextView = view.findViewById(R.id.tvVariance)
        private val etTransfer: EditText = view.findViewById(R.id.etTransfer)
        private val etWasteCount: EditText = view.findViewById(R.id.etWasteCount)
        private val spinnerWasteType: Spinner = view.findViewById(R.id.spinnerWasteType)
        private val etActualCount: EditText = view.findViewById(R.id.etActualCount)
        private val tvSyncStatus: TextView = view.findViewById(R.id.tvSyncStatus)

        private var currentItem: LineTransaction? = null
        private var currentPosition: Int = -1
        private var textWatchersEnabled = true

        private val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (textWatchersEnabled) {
                    saveCurrentValues()
                    updateItemValues()
                }
            }
        }

        init {
            // Make etOrder non-editable
            etOrder.isEnabled = false
            etOrder.isFocusable = false
            etOrder.isFocusableInTouchMode = false
            etOrder.setBackgroundResource(R.drawable.edit_text_background)

            // Add text watchers for editable fields
            etActualReceived.addTextChangedListener(textWatcher)
            etTransfer.addTextChangedListener(textWatcher)
            etWasteCount.addTextChangedListener(textWatcher)
            etActualCount.addTextChangedListener(textWatcher)

            // Improved focus handling - don't auto-set to "0"
            etActualReceived.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus && etActualReceived.text.toString() == "0") {
                    etActualReceived.selectAll()
                }
            }

            etTransfer.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus && etTransfer.text.toString() == "0") {
                    etTransfer.selectAll()
                }
            }

            etWasteCount.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus && etWasteCount.text.toString() == "0") {
                    etWasteCount.selectAll()
                }
            }

            etActualCount.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus && etActualCount.text.toString() == "0") {
                    etActualCount.selectAll()
                }
            }

            spinnerWasteType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (textWatchersEnabled && position > 0) {
                        saveCurrentValues()
                        updateItemValues()
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        private fun saveCurrentValues() {
            currentItem?.itemId?.let { itemId ->
                saveToInputCache(itemId, InputValues(
                    adjustment = etOrder.text.toString(),
                    receivedCount = etActualReceived.text.toString(),
                    transferCount = etTransfer.text.toString(),
                    wasteCount = etWasteCount.text.toString(),
                    counted = etActualCount.text.toString(),
                    wasteType = if (spinnerWasteType.selectedItemPosition > 0)
                        spinnerWasteType.selectedItem.toString() else null
                ))
            }
        }

        fun bind(item: LineTransaction, position: Int) {
            textWatchersEnabled = false
            currentItem = item
            currentPosition = position

            // Set basic info
            tvItemId.text = item.itemId.orEmpty()
            tvItemName.text = item.itemName.orEmpty()
            tvCategory.text = item.itemDepartment.orEmpty()

            // Get cached values or use item values
            val cachedValues = getInputCache(item.itemId.orEmpty())
            if (cachedValues != null) {
                etOrder.setText(cachedValues.adjustment)
                etActualReceived.setText(cachedValues.receivedCount)
                etTransfer.setText(cachedValues.transferCount)
                etWasteCount.setText(cachedValues.wasteCount)
                etActualCount.setText(cachedValues.counted)

                // Calculate variance from cached values
                val orderValue = cachedValues.adjustment.toIntOrNull() ?: 0
                val receivedValue = cachedValues.receivedCount.toIntOrNull() ?: 0
                tvVariance.text = (orderValue - receivedValue).toString()
            } else {
                etOrder.setText(formatNumber(item.adjustment))
                etActualReceived.setText(formatNumber(item.receivedCount))
                etTransfer.setText(formatNumber(item.transferCount))
                etWasteCount.setText(formatNumber(item.wasteCount))
                etActualCount.setText(formatNumber(item.counted))

                // Calculate initial variance
                val orderValue = formatNumber(item.adjustment).toIntOrNull() ?: 0
                val receivedValue = formatNumber(item.receivedCount).toIntOrNull() ?: 0
                tvVariance.text = (orderValue - receivedValue).toString()
            }

            // Setup waste type spinner
            val adapter = ArrayAdapter(
                itemView.context,
                R.layout.spinner_item_black_text,
                itemView.context.resources.getStringArray(R.array.waste_types)
            ).apply {
                setDropDownViewResource(R.layout.spinner_dropdown_item_black_text)
            }
            spinnerWasteType.adapter = adapter

            // Set waste type
            val wasteType = cachedValues?.wasteType ?: item.wasteType
            if (!wasteType.isNullOrEmpty() && wasteType != "Select type") {
                val wasteTypes = itemView.context.resources.getStringArray(R.array.waste_types)
                val position = wasteTypes.indexOf(wasteType)
                if (position >= 0) {
                    spinnerWasteType.setSelection(position)
                }
            }

            // Update sync status dot based on actual sync status
            when {
                item.syncStatus == 0 -> {
                    tvSyncStatus.text = "●"
                    tvSyncStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.orange))
                }
                else -> {
                    tvSyncStatus.text = "●"
                    tvSyncStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.green))
                }
            }

            textWatchersEnabled = true
        }

        private fun updateItemValues() {
            currentItem?.let { item ->
                val newAdjustment = etOrder.text.toString()
                val newReceivedCount = etActualReceived.text.toString()
                val newTransferCount = etTransfer.text.toString()
                val newWasteCount = etWasteCount.text.toString()
                val newCounted = etActualCount.text.toString()
                val newWasteType = if (spinnerWasteType.selectedItemPosition > 0)
                    spinnerWasteType.selectedItem.toString() else null

                // Calculate variance
                val orderValue = newAdjustment.toIntOrNull() ?: 0
                val receivedValue = newReceivedCount.toIntOrNull() ?: 0
                val variance = orderValue - receivedValue
                tvVariance.text = variance.toString()

                // Validate waste count and type
                val wasteCountValue = newWasteCount.toIntOrNull() ?: 0
                if (wasteCountValue > 0 && newWasteType == null) {
                    etWasteCount.error = "Please select waste type"
                    return
                } else if (wasteCountValue == 0 && newWasteType != null) {
                    etWasteCount.error = "Please enter waste count"
                    return
                } else {
                    etWasteCount.error = null
                }

                // Check if values have actually changed from original
                val hasChanged = newAdjustment != formatNumber(item.adjustment) ||
                        newReceivedCount != formatNumber(item.receivedCount) ||
                        newTransferCount != formatNumber(item.transferCount) ||
                        newWasteCount != formatNumber(item.wasteCount) ||
                        newCounted != formatNumber(item.counted) ||
                        newWasteType != item.wasteType

                if (hasChanged) {
                    val updatedItem = item.copy(
                        adjustment = newAdjustment,
                        receivedCount = newReceivedCount,
                        transferCount = newTransferCount,
                        wasteCount = newWasteCount,
                        counted = newCounted,
                        wasteType = newWasteType,
                        variantId = variance.toString(),
                        syncStatus = 0 // Mark as unsynced when modified
                    )
                    onItemUpdated(currentPosition, updatedItem)

                    // Update sync status dot immediately to show unsaved changes
                    tvSyncStatus.text = "●"
                    tvSyncStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.orange))
                }
            }
        }

        private fun formatNumber(value: String?): String {
            if (value.isNullOrEmpty()) return "0"
            return try {
                val number = value.toDouble()
                number.toInt().toString()
            } catch (e: NumberFormatException) {
                "0"
            }
        }
    }
}