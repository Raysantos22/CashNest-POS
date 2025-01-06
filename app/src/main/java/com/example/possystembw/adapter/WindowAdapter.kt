package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.Window
import com.example.possystembw.databinding.ItemWindowBinding  // Updated import
import com.example.possystembw.ui.ViewModel.CartViewModel

class WindowAdapter(
    private val onClick: (Window) -> Unit,
    private val cartViewModel: CartViewModel // Add CartViewModel parameter
) : ListAdapter<Window, WindowAdapter.WindowViewHolder>(WindowDiffCallback()) {

    // Map to store window IDs and their cart status
    private var windowsWithCartItems = mutableMapOf<Int, Boolean>()

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
        holder.bind(window, windowsWithCartItems[window.id] == true)
    }

    // Function to update the cart status for windows
    fun updateWindowsWithCartItems(windowIds: Set<Int>) {
        windowsWithCartItems.clear()
        windowIds.forEach { windowId ->
            windowsWithCartItems[windowId] = true
        }
        notifyDataSetChanged()
    }
    fun updateCartStatus(windowId: Int, hasItems: Boolean) {
        windowsWithCartItems[windowId] = hasItems
        notifyDataSetChanged()
    }
    inner class WindowViewHolder(
        private val binding: ItemWindowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Window, hasCartItems: Boolean) {
            binding.apply {
                windowName.text = item.description

                // Change CardView background color based on cart status
                root.setCardBackgroundColor(
                    ContextCompat.getColor(
                        root.context,
                        if (hasCartItems) R.color.window_with_cart_items
                        else R.color.window_default
                    )
                )

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