package com.example.possystembw.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.ui.Window1
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ItemSalesAdapter(
    private var allItems: List<Window1.ItemSalesSummary>,
    private val totalSales: Double,
    private val totalQuantity: Int,
    private val totalTransactions: Int
) : RecyclerView.Adapter<ItemSalesAdapter.ViewHolder>() {

    private var filteredItems: List<Window1.ItemSalesSummary> = allItems

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameTextView: TextView = view.findViewById(R.id.itemNameTextView)
        private val quantityTextView: TextView = view.findViewById(R.id.quantityTextView)
        private val amountTextView: TextView = view.findViewById(R.id.amountTextView)

        fun bind(item: Window1.ItemSalesSummary) {
            nameTextView.text = item.name
            quantityTextView.text = "Qty: ${item.quantity}"
            amountTextView.text = String.format("₱%.2f", item.totalAmount)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sales_summary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filteredItems[position])
    }

    override fun getItemCount() = filteredItems.size

    fun filter(query: String) {
        filteredItems = if (query.isEmpty()) {
            allItems
        } else {
            allItems.filter {
                it.name.contains(query, ignoreCase = true)
            }
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
        content.append("Items Sold:\n")
        content.append("------------------------------\n")

        // Print each item with proper alignment
        filteredItems.forEach { item ->
            val nameWidth = 37 // Adjust this value based on your needs
            val quantityWidth = 4 // Width for quantity
            val amountWidth = 9 // Width for amount

            val paddedName = item.name.padEnd(nameWidth)
            val paddedQuantity = item.quantity.toString().padStart(quantityWidth)
            val formattedAmount = String.format("%.2f", item.totalAmount).padStart(amountWidth)

            content.append("$paddedName $paddedQuantity x :$formattedAmount [ ] ___\n")
        }

        content.append("------------------------------\n")
        content.append("SUMMARY\n")
        content.append("------------------------------\n")
        content.append("Total Transactions: $totalTransactions\n")
        content.append("Total Items Sold: $totalQuantity\n")
        content.append("Total Sales: ${String.format("%.2f", totalSales)}\n")
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