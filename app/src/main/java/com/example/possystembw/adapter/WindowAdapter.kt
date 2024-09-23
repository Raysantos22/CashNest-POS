package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.database.Window
import com.example.possystembw.databinding.ItemWindowBinding  // Updated import

class WindowAdapter(private val onClick: (Window) -> Unit) :
    ListAdapter<Window, WindowAdapter.WindowViewHolder>(WindowDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WindowViewHolder {
        return WindowViewHolder(
            ItemWindowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: WindowViewHolder, position: Int) {
        val window = getItem(position)
        holder.bind(window)
    }

    inner class WindowViewHolder(
        private val binding: ItemWindowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Window) {
            binding.apply {
                windowName.text = item.description  // Make sure this matches your layout file
                root.setOnClickListener {
                    onClick(item)
                }
            }
        }
    }

    class WindowDiffCallback : DiffUtil.ItemCallback<Window>() {
        override fun areItemsTheSame(oldItem: Window, newItem: Window): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Window, newItem: Window): Boolean {
            return oldItem == newItem
        }
    }
}