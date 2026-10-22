package com.assan.finance.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.assan.finance.R

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.widget.ImageView
import android.widget.Toast

class ChatAdapter(
    private val onRetry: ((ChatMessage) -> Unit)? = null,
    private val onReport: ((ChatMessage) -> Unit)? = null
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private var messages: List<ChatMessage> = emptyList()

    fun submitList(newList: List<ChatMessage>) {
        messages = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutUser: View = itemView.findViewById(R.id.layoutUser)
        private val tvUserMsg: TextView = itemView.findViewById(R.id.tvUserMsg)
        private val tvUserTime: TextView = itemView.findViewById(R.id.tvUserTime)
        private val ivRetry: ImageView? = itemView.findViewById(R.id.ivRetry)
        private val layoutAi: View = itemView.findViewById(R.id.layoutAi)
        private val tvAiMsg: TextView = itemView.findViewById(R.id.tvAiMsg)
        private val tvAiTime: TextView = itemView.findViewById(R.id.tvAiTime)
        private val tvAiReport: TextView? = itemView.findViewById(R.id.tvAiReport)

        private var typingAnimator: android.animation.ValueAnimator? = null

        fun bind(message: ChatMessage) {
            if (message.isUser) {
                layoutUser.visibility = View.VISIBLE
                layoutAi.visibility = View.GONE
                tvUserMsg.text = message.text
                tvUserTime.text = message.timestamp
                
                if (message.isFailed) {
                    tvUserMsg.setTextColor(Color.parseColor("#D32F2F"))
                    ivRetry?.visibility = View.VISIBLE
                    ivRetry?.setOnClickListener {
                        onRetry?.invoke(message)
                    }
                } else {
                    tvUserMsg.setTextColor(Color.BLACK)
                    ivRetry?.visibility = View.GONE
                    ivRetry?.setOnClickListener(null)
                }
                
                layoutUser.setOnLongClickListener {
                    copyToClipboard(itemView.context, message.text)
                    true
                }
            } else {
                layoutUser.visibility = View.GONE
                layoutAi.visibility = View.VISIBLE
                tvAiTime.text = message.timestamp
                
                typingAnimator?.cancel()
                if (message.text == "Typing...") {
                    typingAnimator = android.animation.ValueAnimator.ofInt(0, 4).apply {
                        duration = 1000
                        repeatCount = android.animation.ValueAnimator.INFINITE
                        addUpdateListener { animator ->
                            val dotCount = animator.animatedValue as Int
                            // The assistant reasons before answering, so the placeholder reads "Thinking..."
                            val text = itemView.context.getString(com.assan.finance.R.string.str_thinking)
                            val spannable = android.text.SpannableString(text)
                            // Animate the trailing 3 dots: dotCount visible, the rest transparent.
                            val dotsStart = (text.length - 3).coerceAtLeast(0)
                            if (dotCount < 3) {
                                spannable.setSpan(
                                    android.text.style.ForegroundColorSpan(android.graphics.Color.TRANSPARENT),
                                    dotsStart + dotCount,
                                    text.length,
                                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                            }
                            tvAiMsg.text = spannable
                        }
                        start()
                    }
                } else {
                    tvAiMsg.text = message.text
                }

                val canReport = onReport != null && message.text != "Typing..."
                tvAiReport?.visibility = if (canReport) View.VISIBLE else View.GONE
                tvAiReport?.setOnClickListener(if (canReport) View.OnClickListener { onReport?.invoke(message) } else null)

                layoutAi.setOnLongClickListener {
                    copyToClipboard(itemView.context, message.text)
                    true
                }
            }
        }
        
        private fun copyToClipboard(context: Context, text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Chat Message", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, itemView.context.getString(R.string.msg_text_copied), Toast.LENGTH_SHORT).show()
        }
    }
}
