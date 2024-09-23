package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.Category

class CategoryAdapter(private val onCategoryClick: (Category) -> Unit) :
    RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var categories: List<Category> = emptyList()
    private var selectedCategoryPosition: Int = RecyclerView.NO_POSITION

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val oldSelectedPosition = selectedCategoryPosition
                    selectedCategoryPosition = position
                    notifyItemChanged(oldSelectedPosition)
                    notifyItemChanged(selectedCategoryPosition)
                    onCategoryClick(categories[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.tvCategoryName.text = category.name
        holder.itemView.isSelected = position == selectedCategoryPosition
    }

    override fun getItemCount() = categories.size

    fun setCategories(newCategories: List<Category>) {
        val diffResult = DiffUtil.calculateDiff(CategoryDiffCallback(categories, newCategories))
        categories = newCategories
        diffResult.dispatchUpdatesTo(this)
    }

    fun setSelectedCategory(category: Category?) {
        val newSelectedPosition = categories.indexOf(category)
        if (newSelectedPosition != selectedCategoryPosition) {
            val oldSelectedPosition = selectedCategoryPosition
            selectedCategoryPosition = newSelectedPosition
            notifyItemChanged(oldSelectedPosition)
            notifyItemChanged(selectedCategoryPosition)
        }
    }
    private class CategoryDiffCallback(
        private val oldList: List<Category>,
        private val newList: List<Category>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition].groupId == newList[newItemPosition].groupId
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition] == newList[newItemPosition]
    }
}