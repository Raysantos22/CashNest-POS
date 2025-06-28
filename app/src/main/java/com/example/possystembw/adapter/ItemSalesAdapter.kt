package com.example.possystembw.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.ui.ReportsActivity
import com.example.possystembw.ui.Window1
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale




// Enhanced adapter with grouping functionality
class ItemSalesAdapter(
    private var allItemGroups: List<ReportsActivity.ItemGroupSummary>,
    private val totalSales: Double,
    private val totalQuantity: Int,
    private val totalTransactions: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_GROUP_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    private var filteredItems: List<Any> = flattenItemGroups(allItemGroups)

    private fun flattenItemGroups(itemGroups: List<ReportsActivity.ItemGroupSummary>): List<Any> {
        val flatList = mutableListOf<Any>()
        itemGroups.forEach { group ->
            flatList.add(group) // Add group header
            flatList.addAll(group.items) // Add items in group
        }
        return flatList
    }

    inner class GroupHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val groupNameTextView: TextView = view.findViewById(R.id.groupNameTextView)
        private val groupQuantityTextView: TextView = view.findViewById(R.id.groupQuantityTextView)
        private val groupAmountTextView: TextView = view.findViewById(R.id.groupAmountTextView)

        fun bind(groupSummary: ReportsActivity.ItemGroupSummary) {
            groupNameTextView.text = groupSummary.groupName
            groupQuantityTextView.text = "Qty: ${groupSummary.totalQuantity}"
            groupAmountTextView.text = String.format("₱%.2f", groupSummary.totalAmount)
        }
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameTextView: TextView = view.findViewById(R.id.itemNameTextView)
        private val quantityTextView: TextView = view.findViewById(R.id.quantityTextView)
        private val amountTextView: TextView = view.findViewById(R.id.amountTextView)
        private val groupTextView: TextView = view.findViewById(R.id.itemGroupTextView)

        fun bind(item: ReportsActivity.ItemSalesSummary) {
            nameTextView.text = item.name
            quantityTextView.text = "Qty: ${item.quantity}"
            amountTextView.text = String.format("₱%.2f", item.totalAmount)
            groupTextView.text = item.itemGroup
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (filteredItems[position]) {
            is ReportsActivity.ItemGroupSummary -> VIEW_TYPE_GROUP_HEADER
            is ReportsActivity.ItemSalesSummary -> VIEW_TYPE_ITEM
            else -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_GROUP_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_group_header, parent, false)
                GroupHeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_sales_summary, parent, false)
                ItemViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is GroupHeaderViewHolder -> holder.bind(filteredItems[position] as ReportsActivity.ItemGroupSummary)
            is ItemViewHolder -> holder.bind(filteredItems[position] as ReportsActivity.ItemSalesSummary)
        }
    }

    override fun getItemCount() = filteredItems.size

    fun filter(query: String) {
        filteredItems = if (query.isEmpty()) {
            flattenItemGroups(allItemGroups)
        } else {
            val filteredGroups = allItemGroups.mapNotNull { group ->
                val filteredGroupItems = group.items.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            group.groupName.contains(query, ignoreCase = true)
                }
                if (filteredGroupItems.isNotEmpty()) {
                    group.copy(
                        items = filteredGroupItems,
                        totalQuantity = filteredGroupItems.sumOf { it.quantity },
                        totalAmount = filteredGroupItems.sumOf { it.totalAmount }
                    )
                } else null
            }
            flattenItemGroups(filteredGroups)
        }
        notifyDataSetChanged()
    }

    fun printItemSalesReport(context: Context) {
        val content = StringBuilder()
        content.append(0x1B.toChar()) // ESC
        content.append('!'.toChar())  // Select print mode
        content.append(0x01.toChar()) // Smallest text size

        // Set minimum line spacing
        content.append(0x1B.toChar()) // ESC
        content.append('3'.toChar())  // Select line spacing
        content.append(50.toChar())

        content.append("==============================\n")
        content.append("        ITEM SALES REPORT     \n")
        content.append("==============================\n")
        content.append(
            "Date: ${
                SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault()).format(
                    Date()
                )
            }\n"
        )
        content.append("------------------------------\n")
        content.append("Items Sold by Group:\n")
        content.append("------------------------------\n")

        // Print items grouped by category
        allItemGroups.forEach { group ->
            content.append("\n--- ${group.groupName.uppercase()} ---\n")
            content.append("Group Total: ${group.totalQuantity} items, ₱${String.format("%.2f", group.totalAmount)}\n")
            content.append("------------------------------\n")

            group.items.forEach { item ->
                val nameWidth = 22
                val quantityWidth = 4
                val amountWidth = 9

                val truncatedName = if (item.name.length > nameWidth) {
                    item.name.substring(0, nameWidth - 3) + "..."
                } else {
                    item.name.padEnd(nameWidth)
                }

                val paddedQuantity = item.quantity.toString().padStart(quantityWidth)
                val formattedAmount = String.format("%.2f", item.totalAmount).padStart(amountWidth)

                content.append("$truncatedName $paddedQuantity x ₱$formattedAmount\n")
            }
            content.append("------------------------------\n")
        }

        content.append("\nGRAND TOTAL SUMMARY\n")
        content.append("==============================\n")
        content.append("Total Transactions: $totalTransactions\n")
        content.append("Total Items Sold: $totalQuantity\n")
        content.append("Total Sales: ₱${String.format("%.2f", totalSales)}\n")
        content.append("Total Groups: ${allItemGroups.size}\n")
        content.append("==============================\n")

        content.append(0x1B.toChar()) // ESC
        content.append('!'.toChar())  // Select print mode
        content.append(0x00.toChar()) // Reset to normal size

        content.append(0x1B.toChar()) // ESC
        content.append('2'.toChar())

        // Print the receipt using your existing printer function
        (context as? Window1)?.printReceiptWithBluetoothPrinter(content.toString())
    }
}