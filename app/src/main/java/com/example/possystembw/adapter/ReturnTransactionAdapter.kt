import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.TransactionRecord
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class ReturnTransactionAdapter : RecyclerView.Adapter<ReturnTransactionAdapter.ReturnViewHolder>() {
    private var items: List<TransactionRecord> = emptyList()
    private val selectedItems = mutableSetOf<TransactionRecord>()
    private var onItemSelectedListener: OnItemSelectedListener? = null

    interface OnItemSelectedListener {
        fun onItemSelected(selectedItems: List<TransactionRecord>)
    }

    inner class ReturnViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        private val nameTextView: TextView = itemView.findViewById(R.id.itemNameTextView)
        private val quantityTextView: TextView = itemView.findViewById(R.id.quantityTextView)
        private val priceTextView: TextView = itemView.findViewById(R.id.priceTextView)

        fun bind(item: TransactionRecord) {
            nameTextView.text = item.name
            quantityTextView.text = "Qty: ${item.quantity}"
            priceTextView.text = String.format("₱%.2f", item.total)

            checkBox.isChecked = selectedItems.contains(item)
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedItems.add(item)
                } else {
                    selectedItems.remove(item)
                }
                onItemSelectedListener?.onItemSelected(selectedItems.toList())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReturnViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_return_transaction, parent, false)
        return ReturnViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReturnViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun setItems(newItems: List<TransactionRecord>) {
        items = newItems
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun setOnItemSelectedListener(listener: OnItemSelectedListener) {
        onItemSelectedListener = listener
    }

    fun getSelectedItems(): List<TransactionRecord> = selectedItems.toList()
}