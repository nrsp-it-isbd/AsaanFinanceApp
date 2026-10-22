package com.assan.finance.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.assan.finance.R
import com.assan.finance.model.assan.PaymentChannel

data class PaymentCategory(
    val title: String,
    val channels: List<PaymentChannel>
)

class ChannelCategoryAdapter(
    private val categories: List<PaymentCategory>,
    private val onChannelClick: ((PaymentChannel) -> Unit)?
): RecyclerView.Adapter<ChannelCategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val recyclerViewChannels: RecyclerView = itemView.findViewById(R.id.recyclerViewChannels)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        val context = holder.itemView.context
        val translatedTitle = when (category.title.lowercase()) {
            "bank", "banks" -> context.getString(R.string.str_banks_1)
            "agent", "agents" -> context.getString(R.string.str_agents_1)
            "payment network", "payment networks" -> context.getString(R.string.str_payment_network)
            else -> category.title
        }
        holder.tvCategoryName.text = translatedTitle

        // Setup the inner RecyclerView
        holder.recyclerViewChannels.layoutManager = GridLayoutManager(holder.itemView.context, 4)
        
        val channelsAdapter = PaymentChannelsAdapter(category.channels, onChannelClick)
        holder.recyclerViewChannels.adapter = channelsAdapter
    }

    override fun getItemCount(): Int {
        return categories.size
    }
}
