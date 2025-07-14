package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.ui.WindowWithVisibility

class WindowVisibilityAdapter(
    private val onToggleVisibility: (WindowWithVisibility, Boolean) -> Unit
) : ListAdapter<WindowWithVisibility, WindowVisibilityAdapter.WindowVisibilityViewHolder>(
    WindowVisibilityDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WindowVisibilityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_window_visibility, parent, false)
        return WindowVisibilityViewHolder(view)
    }

    override fun onBindViewHolder(holder: WindowVisibilityViewHolder, position: Int) {
        val windowWithVisibility = getItem(position)
        holder.bind(windowWithVisibility)
    }

    inner class WindowVisibilityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textViewWindowName)
        private val typeTextView: TextView = itemView.findViewById(R.id.textViewWindowType)
        private val visibilitySwitch: Switch = itemView.findViewById(R.id.switchVisibility)

        fun bind(windowWithVisibility: WindowWithVisibility) {
            nameTextView.text = windowWithVisibility.description
            typeTextView.text = windowWithVisibility.type
            
            // Set switch state without triggering listener
            visibilitySwitch.setOnCheckedChangeListener(null)
            visibilitySwitch.isChecked = windowWithVisibility.isVisible
            
            // Set the listener after setting the initial state
            visibilitySwitch.setOnCheckedChangeListener { _, isChecked ->
                onToggleVisibility(windowWithVisibility, isChecked)
            }

            // Optional: Update UI based on visibility state
            val alpha = if (windowWithVisibility.isVisible) 1.0f else 0.6f
            nameTextView.alpha = alpha
            typeTextView.alpha = alpha
        }
    }

    class WindowVisibilityDiffCallback : DiffUtil.ItemCallback<WindowWithVisibility>() {
        override fun areItemsTheSame(oldItem: WindowWithVisibility, newItem: WindowWithVisibility): Boolean {
            return oldItem.id == newItem.id && oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: WindowWithVisibility, newItem: WindowWithVisibility): Boolean {
            return oldItem == newItem
        }
    }
}