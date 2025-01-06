package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.StoreExpense

class StoreExpenseAdapter(
    private val onDeleteClick: (StoreExpense) -> Unit,
    private val onEditClick: (StoreExpense) -> Unit
) : ListAdapter<StoreExpense, StoreExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view, onDeleteClick, onEditClick)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ExpenseViewHolder(
        itemView: View,
        private val onDeleteClick: (StoreExpense) -> Unit,
        private val onEditClick: (StoreExpense) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvExpenseName = itemView.findViewById<TextView>(R.id.tvExpenseName)
        private val tvExpenseTypes = itemView.findViewById<TextView>(R.id.tvExpenseTypes)
        private val tvAmount = itemView.findViewById<TextView>(R.id.tvAmount)
        private val tvDetails = itemView.findViewById<TextView>(R.id.tvDetails)
        private val tvDate = itemView.findViewById<TextView>(R.id.tvDate)

        private var currentExpense: StoreExpense? = null

        init {
            itemView.setOnLongClickListener {
                currentExpense?.let { expense -> showPopupMenu(it, expense) }
                true
            }
        }

        private fun showPopupMenu(view: View, expense: StoreExpense) {
            PopupMenu(view.context, view).apply {
                menu.add("Edit").setOnMenuItemClickListener {
                    onEditClick(expense)
                    true
                }
                menu.add("Delete").setOnMenuItemClickListener {
                    onDeleteClick(expense)
                    true
                }
                show()
            }
        }

        fun bind(expense: StoreExpense) {
            currentExpense = expense
            tvExpenseName.text = "Name: ${expense.name}"
            tvExpenseTypes.text = "Types: ${expense.expenseType}"
            tvAmount.text = "Amount: ₱%.2f".format(expense.amount)
            tvDetails.text = "Received by: ${expense.receivedBy}\nApproved by: ${expense.approvedBy}"
            tvDate.text = "Date: ${expense.effectDate}"
        }
    }

    private class ExpenseDiffCallback : DiffUtil.ItemCallback<StoreExpense>() {
        override fun areItemsTheSame(oldItem: StoreExpense, newItem: StoreExpense): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StoreExpense, newItem: StoreExpense): Boolean {
            return oldItem == newItem
        }
    }
}