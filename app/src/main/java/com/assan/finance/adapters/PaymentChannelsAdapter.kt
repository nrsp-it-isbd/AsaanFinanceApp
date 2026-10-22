package com.assan.finance.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.assan.finance.R
import com.assan.finance.model.assan.PaymentChannel
import com.assan.finance.apis.ApiManager
import com.bumptech.glide.Glide

class PaymentChannelsAdapter(
    private val channels: List<PaymentChannel>,
    private val onChannelClick: ((PaymentChannel) -> Unit)? = null
) : RecyclerView.Adapter<PaymentChannelsAdapter.ChannelViewHolder>() {

    class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivChannelLogo: ImageView = itemView.findViewById(R.id.ivChannelLogo)
        val tvChannelName: TextView = itemView.findViewById(R.id.tvChannelName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = channels[position]
        holder.tvChannelName.text = channel.name

        // The backend API is returning broken hardcoded URLs like "/api/images/uploads/easypaisa.webp"
        // We need to replace "uploads" with the correct folder based on the channel type.
        val logoUrl = if (channel.logoUrl != null) {
            var path = channel.logoUrl
            
            if (path.contains("/api/images/uploads/", ignoreCase = true)) {
                val type = channel.channelType?.lowercase() ?: ""
                val correctFolder = when {
                    type.contains("bank") -> "banks"
                    type.contains("agent") -> "agents"
                    type.contains("network") -> "payment_networks"
                    else -> "uploads" // fallback
                }
                path = path.replace("/api/images/uploads/", "/api/images/$correctFolder/", ignoreCase = true)
            }
            
            // Legacy relative path handling just in case the backend reverts
            if (!path.startsWith("http", ignoreCase = true)) {
                if (path.startsWith("uploads/", ignoreCase = true)) {
                    path = path.replaceFirst("uploads/", "images/", ignoreCase = true)
                    path = path.replace("/channels/", "/", ignoreCase = true)
                }
                ApiManager.IMAGE_BASE_URL + path
            } else {
                path
            }
        } else {
            ""
        }

        val shimmer = com.facebook.shimmer.Shimmer.AlphaHighlightBuilder()
            .setDuration(1200)
            .setBaseAlpha(0.7f)
            .setHighlightAlpha(0.9f)
            .setDirection(com.facebook.shimmer.Shimmer.Direction.LEFT_TO_RIGHT)
            .setAutoStart(true)
            .build()
        val shimmerDrawable = com.facebook.shimmer.ShimmerDrawable().apply {
            setShimmer(shimmer)
        }

        Glide.with(holder.itemView.context)
            .load(logoUrl)
            .placeholder(shimmerDrawable)
            .error(android.R.drawable.ic_menu_report_image) // Error fallback
            .into(holder.ivChannelLogo)

        // No click handler = display only (no ripple, no tap)
        if (onChannelClick != null) {
            holder.itemView.setOnClickListener { onChannelClick.invoke(channel) }
        } else {
            holder.itemView.setOnClickListener(null)
            holder.itemView.isClickable = false
        }
    }

    override fun getItemCount(): Int {
        return channels.size
    }
}
