package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.WindowTable

class WindowTableAdapter(private val onItemClick: (WindowTable) -> Unit) :
    ListAdapter<WindowTable, WindowTableAdapter.WindowTableViewHolder>(WindowTableDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WindowTableViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_window_table, parent, false)
        return WindowTableViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: WindowTableViewHolder, position: Int) {
        val windowTable = getItem(position)
        holder.bind(windowTable)
    }

    class WindowTableViewHolder(itemView: View, private val onItemClick: (WindowTable) -> Unit) :
        RecyclerView.ViewHolder(itemView) {

        private val idTextView: TextView = itemView.findViewById(R.id.tableInfoTextView)

        fun bind(windowTable: WindowTable) {
            idTextView.text = "${windowTable.description}"
            itemView.setOnClickListener { onItemClick(windowTable) }
        }
    }
    class WindowTableDiffCallback : DiffUtil.ItemCallback<WindowTable>() {
        override fun areItemsTheSame(oldItem: WindowTable, newItem: WindowTable): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WindowTable, newItem: WindowTable): Boolean {
            return oldItem == newItem
        }
    }
}