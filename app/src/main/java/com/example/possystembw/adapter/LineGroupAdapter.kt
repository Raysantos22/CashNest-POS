package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.CartItem
import com.example.possystembw.database.LineGroupWithDiscounts
import com.example.possystembw.database.MixMatch
import com.example.possystembw.database.MixMatchWithDetails
import com.example.possystembw.ui.ViewModel.ProductViewModel
import java.util.UUID

class LineGroupAdapter(
    private val lineGroups: List<LineGroupWithDiscounts>,
    private val productViewModel: ProductViewModel
) : RecyclerView.Adapter<LineGroupAdapter.ViewHolder>() {

    private val selections = mutableMapOf<Int, String>() // lineGroupId to selected itemId

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mix_match_line_group, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lineGroup = lineGroups[position]
        holder.bind(lineGroup)
    }

    override fun getItemCount() = lineGroups.size

    fun getSelections(): Map<Int, String> = selections.toMap()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.textViewLineGroupTitle)
        private val itemsNeededView: TextView = itemView.findViewById(R.id.textViewItemsNeeded)
        private val selectButton: Button = itemView.findViewById(R.id.buttonSelectProduct)
        private val selectedProductView: TextView = itemView.findViewById(R.id.textViewSelectedProduct)

        fun bind(lineGroup: LineGroupWithDiscounts) {
            titleView.text = lineGroup.lineGroup.description
            itemsNeededView.text = "Select ${lineGroup.lineGroup.noOfItemsNeeded} item(s)"

            // Reset button text if no selection
            if (!selections.containsKey(lineGroup.lineGroup.id)) {
                selectButton.text = "Select Product"
                selectedProductView.visibility = View.GONE
            } else {
                // Show selected product name if there's a selection
                val selectedItemId = selections[lineGroup.lineGroup.id]
                val selectedProduct = productViewModel.getProductByItemId(selectedItemId ?: "")
                selectButton.text = "Change Selection"
                selectedProductView.apply {
                    text = "Selected: ${selectedProduct?.itemName ?: "Unknown Product"}"
                    visibility = View.VISIBLE
                }
            }

            selectButton.setOnClickListener {
                showProductSelectionDialog(lineGroup)
            }
        }

        private fun showProductSelectionDialog(lineGroup: LineGroupWithDiscounts) {
            val context = itemView.context

            // Create list of product items with names
            val productItems = lineGroup.discountLines.mapNotNull { discountLine ->
                val product = productViewModel.getProductByItemId(discountLine.itemId?.toString() ?: "")
                if (product != null) {
                    ProductSelectionItem(product.itemid, product.itemName, product.price)
                } else null
            }

            val productNames = productItems.map {
                "${it.name} - P${String.format("%.2f", it.price)}"
            }.toTypedArray()

            AlertDialog.Builder(context, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog_Alert)
                .setTitle("Select Product")
                .setItems(productNames) { dialog, which ->
                    val selectedProduct = productItems[which]
                    selections[lineGroup.lineGroup.id] = selectedProduct.itemId

                    // Update UI to show selection
                    selectButton.text = "Change Selection"
                    selectedProductView.apply {
                        text = "Selected: ${selectedProduct.name}"
                        visibility = View.VISIBLE
                    }

                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    fun clearSelections() {
        selections.clear()
        notifyDataSetChanged()
    }

    private data class ProductSelectionItem(
        val itemId: String,
        val name: String,
        val price: Double
    )
}