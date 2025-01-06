package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.possystembw.R
import com.example.possystembw.database.AttendanceRecord
import com.example.possystembw.databinding.AttendanceHistoryItemBinding

class AttendanceHistoryAdapter :
    ListAdapter<AttendanceRecord, AttendanceHistoryAdapter.ViewHolder>(AttendanceDiffCallback()) {

    class ViewHolder(private val binding: AttendanceHistoryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(attendance: AttendanceRecord) {
            binding.apply {
                staffNameText.text = attendance.staffId.split("_")[0]
                attendanceDate.text = attendance.date
                
                // Set the latest attendance action
                val latestAction = when {
                    attendance.timeOut != null -> "Time Out: ${attendance.timeOut}"
                    attendance.breakOut != null -> "Break Out: ${attendance.breakOut}"
                    attendance.breakIn != null -> "Break In: ${attendance.breakIn}"
                    else -> "Time In: ${attendance.timeIn}"
                }
                attendanceTypeTime.text = latestAction

                // Load the corresponding photo
                val photoPath = when {
                    attendance.timeOutPhoto != null -> attendance.timeOutPhoto
                    attendance.breakOutPhoto != null -> attendance.breakOutPhoto
                    attendance.breakInPhoto != null -> attendance.breakInPhoto
                    else -> attendance.timeInPhoto
                }

                Glide.with(attendancePhoto)
                    .load(photoPath)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(attendancePhoto)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AttendanceHistoryItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class AttendanceDiffCallback :  DiffUtil.ItemCallback<AttendanceRecord>() {
        override fun areItemsTheSame(oldItem: AttendanceRecord, newItem: AttendanceRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AttendanceRecord, newItem: AttendanceRecord): Boolean {
            return oldItem == newItem
        }
    }
}