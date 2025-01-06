package com.example.possystembw.adapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.StaffEntity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log

class StaffAdapter : RecyclerView.Adapter<StaffAdapter.StaffViewHolder>() {
    private var staffList = listOf<StaffEntity>()
    private var selectedPosition = -1
    var onStaffSelected: ((StaffEntity) -> Unit)? = null

    inner class StaffViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val staffImage: ShapeableImageView = itemView.findViewById(R.id.staffImage)
        private val staffName: TextView = itemView.findViewById(R.id.staffName)
        private val staffRole: TextView = itemView.findViewById(R.id.staffRole)
        private val radioButton: RadioButton = itemView.findViewById(R.id.staffRadioButton)

        fun bind(staff: StaffEntity, position: Int) {
            staffName.text = staff.name
            staffRole.text = when(staff.role) {
                "SV" -> "Supervisor"
                "ST" -> "Staff"
                "CH" -> "Cluster Head"
                else -> staff.role
            }
            radioButton.isChecked = position == selectedPosition

            // Handle profile image
            if (!staff.image.isNullOrEmpty()) {
                // Convert Base64 to Bitmap
                val bitmap = base64ToBitmap(staff.image)
                if (bitmap != null) {
                    staffImage.setImageBitmap(bitmap)
                } else {
                    staffImage.setImageResource(R.drawable.placeholder_image)
                }
            } else {
                staffImage.setImageResource(R.drawable.placeholder_image)
            }

            itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onStaffSelected?.invoke(staff)
            }
        }

        private fun base64ToBitmap(base64String: String): Bitmap? {
            return try {
                val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                Log.e("Base64ToBitmap", "Error converting base64 to bitmap", e)
                null
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StaffViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_staff, parent, false)
        return StaffViewHolder(view)
    }

    override fun onBindViewHolder(holder: StaffViewHolder, position: Int) {
        holder.bind(staffList[position], position)
    }

    override fun getItemCount(): Int = staffList.size

    fun updateStaff(newStaffList: List<StaffEntity>) {
        staffList = newStaffList
        notifyDataSetChanged()
    }

    fun clearSelection() {
        val prev = selectedPosition
        selectedPosition = -1
        notifyItemChanged(prev)
    }
}