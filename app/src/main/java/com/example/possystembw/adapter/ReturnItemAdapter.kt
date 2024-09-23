/*
package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.TransactionRecord

class ReturnItemAdapter(
    private val items: List<TransactionRecord>,
    private val onItemCheckedChanged: (TransactionRecord, Boolean) -> Unit
) : RecyclerView.Adapter<ReturnItemAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.returnCheckBox)
        val itemName: TextView = view.findViewById(R.id.itemNameTextView)
        val itemQuantity: TextView = view.findViewById(R.id.itemQuantityTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.return_item_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.itemName.text = item.name
        holder.itemQuantity.text = "Qty: ${item.quantity}"
        holder.checkBox.isChecked = item.isSelected
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            item.isSelected = isChecked
            onItemCheckedChanged(item, isChecke)
        }
    }

    override fun getItemCount() = items.size
}*/
