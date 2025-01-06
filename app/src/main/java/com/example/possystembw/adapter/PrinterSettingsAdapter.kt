package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.PrinterSettings


class PrinterSettingsAdapter(
    private val onDeleteClick: (PrinterSettings) -> Unit,
    private val onDefaultChanged: (PrinterSettings, Boolean) -> Unit,
    private val onTestPrint: (PrinterSettings) -> Unit
) : ListAdapter<PrinterSettings, PrinterSettingsAdapter.PrinterViewHolder>(PrinterDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrinterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_printer, parent, false)
        return PrinterViewHolder(view)
    }

    override fun onBindViewHolder(holder: PrinterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PrinterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val printerNameText: TextView = itemView.findViewById(R.id.printerNameText)
        private val macAddressText: TextView = itemView.findViewById(R.id.macAddressText)
        private val windowIdText: TextView = itemView.findViewById(R.id.windowIdText)
        private val defaultCheckbox: CheckBox = itemView.findViewById(R.id.defaultCheckbox)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        private val testButton: Button = itemView.findViewById(R.id.testButton)

        fun bind(printer: PrinterSettings) {
            printerNameText.text = printer.printerName
            macAddressText.text = printer.macAddress
            windowIdText.text = if (printer.windowId != null) {
                "Window: ${printer.windowId}"
            } else {
                "No window assigned"
            }
            defaultCheckbox.isChecked = printer.isDefault

            defaultCheckbox.setOnCheckedChangeListener { _, isChecked ->
                onDefaultChanged(printer, isChecked)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(printer)
            }

            testButton.setOnClickListener {
                onTestPrint(printer)
            }
        }
    }
}

class PrinterDiffCallback : DiffUtil.ItemCallback<PrinterSettings>() {
    override fun areItemsTheSame(oldItem: PrinterSettings, newItem: PrinterSettings): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PrinterSettings, newItem: PrinterSettings): Boolean {
        return oldItem == newItem
    }
}
