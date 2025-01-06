package com.example.possystembw.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.database.MixMatchWithDetails

class PromoSuggestionAdapter(
    private val onPromoClick: (MixMatchWithDetails) -> Unit
) : ListAdapter<MixMatchWithDetails, PromoSuggestionAdapter.ViewHolder>(PromoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_promo_suggestion, parent, false)
        return ViewHolder(view, onPromoClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onPromoClick: (MixMatchWithDetails) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val promoTitleText: TextView = itemView.findViewById(R.id.promoTitleText)
        private val promoDescriptionText: TextView = itemView.findViewById(R.id.promoDescriptionText)
        private val viewPromoButton: Button = itemView.findViewById(R.id.viewPromoButton)

        fun bind(mixMatch: MixMatchWithDetails) {
            promoTitleText.text = mixMatch.mixMatch.description
            
            val discountText = when (mixMatch.mixMatch.discountType) {
                0 -> "Deal Price: ₱${mixMatch.mixMatch.dealPriceValue}"
                1 -> "${mixMatch.mixMatch.discountPctValue}% Off"
                2 -> "₱${mixMatch.mixMatch.discountAmountValue} Off"
                else -> "Special Offer"
            }
            promoDescriptionText.text = discountText
            
            viewPromoButton.setOnClickListener {
                onPromoClick(mixMatch)
            }
        }
    }

    private class PromoDiffCallback : DiffUtil.ItemCallback<MixMatchWithDetails>() {
        override fun areItemsTheSame(
            oldItem: MixMatchWithDetails,
            newItem: MixMatchWithDetails
        ): Boolean {
            return oldItem.mixMatch.id == newItem.mixMatch.id
        }

        override fun areContentsTheSame(
            oldItem: MixMatchWithDetails,
            newItem: MixMatchWithDetails
        ): Boolean {
            return oldItem == newItem
        }
    }
}
