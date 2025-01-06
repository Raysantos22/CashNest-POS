package com.example.possystembw.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.TransactionSummary
import com.example.possystembw.databinding.ItemTransactionResyncBinding
import com.example.possystembw.ui.PrinterSettingsActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class TransactionResyncAdapter :
    ListAdapter<PrinterSettingsActivity.TransactionData, TransactionResyncAdapter.ViewHolder>(DIFF_CALLBACK) {
    private var selectAllState = false
    private var onSelectAllChangeListener: ((Boolean) -> Unit)? = null

    fun setOnSelectAllChangeListener(listener: (Boolean) -> Unit) {
        onSelectAllChangeListener = listener
    }

    companion object {
        private val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<PrinterSettingsActivity.TransactionData>() {
                override fun areItemsTheSame(
                    oldItem: PrinterSettingsActivity.TransactionData,
                    newItem: PrinterSettingsActivity.TransactionData
                ) = oldItem.summary.transactionId == newItem.summary.transactionId

                override fun areContentsTheSame(
                    oldItem: PrinterSettingsActivity.TransactionData,
                    newItem: PrinterSettingsActivity.TransactionData
                ) = oldItem == newItem
            }
    }

    inner class ViewHolder(private val binding: ItemTransactionResyncBinding) : RecyclerView.ViewHolder(binding.root) {
        val checkbox: CheckBox = binding.checkbox
        val transactionInfo: TextView = binding.transactionInfo
        val recordsList: TextView = binding.recordsList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionResyncBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    fun selectAll(selected: Boolean) {
        selectAllState = selected
        val mutableList = currentList.toMutableList()
        mutableList.forEach { it.isSelected = selected }
        submitList(mutableList)
        onSelectAllChangeListener?.invoke(selected)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = getItem(position)
        val summary = data.summary
        val records = data.records

        // Update checkbox state
        holder.checkbox.isChecked = data.isSelected

        // Format summary information
        holder.transactionInfo.text = """
            Transaction ID: ${summary.transactionId}
            Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(summary.createdDate)}
            Gross Amount: ₱${String.format("%.2f", summary.grossAmount)}
            Sync Status: ${if (summary.syncStatus) "Synced" else "Not Synced"}
        """.trimIndent()

        // Format records information
        val recordsText = StringBuilder()
        records.forEach { record ->
            recordsText.append("""
                - ${record.name} (${record.quantity} x ₱${String.format("%.2f", record.price)})
                  Total: ₱${String.format("%.2f", record.total)}
                  Sync Status: ${if (record.syncStatusRecord) "Synced" else "Not Synced"}
            """.trimIndent())
            recordsText.append("\n")
        }
        holder.recordsList.text = recordsText.toString()

        // Set up checkbox listener
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            val mutableList = currentList.toMutableList()
            mutableList[position].isSelected = isChecked
            submitList(mutableList)

            // Update select all state
            if (!isChecked && selectAllState) {
                selectAllState = false
                onSelectAllChangeListener?.invoke(false)
            }
        }

        // Apply alternating background colors
        holder.itemView.setBackgroundColor(
            if (position % 2 == 0)
                ContextCompat.getColor(holder.itemView.context, R.color.list_item_even)
            else
                ContextCompat.getColor(holder.itemView.context, R.color.list_item_odd)
        )
    }
}
// Extension function to format Calendar as string
fun Calendar.toFormattedString(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(this.time)
}

