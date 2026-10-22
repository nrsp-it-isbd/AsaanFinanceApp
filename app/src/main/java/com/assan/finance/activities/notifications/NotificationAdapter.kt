package com.assan.finance.activities.notifications

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.assan.finance.R

data @android.annotation.SuppressLint("InflateParams")
class NotificationModel(
    val logId: Int,
    val title: String,
    val message: String,
    val date: String,
    val category: String,
    val emoji: String,
    val bgColorHex: String,
    var isUnread: Boolean
)

class NotificationAdapter(
    private val notifications: List<NotificationModel>,
    private val onNotificationClick: ((NotificationModel, Int) -> Unit)? = null
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val flIcon: FrameLayout = view.findViewById(R.id.flIcon)
        val tvIconEmoji: TextView = view.findViewById(R.id.tvIconEmoji)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val vUnreadDot: View = view.findViewById(R.id.vUnreadDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = notifications[position]

        holder.tvTitle.text = item.title
        holder.tvMessage.text = item.message
        holder.tvCategory.text = when (item.category.uppercase()) {
            "GENERAL" -> holder.itemView.context.getString(R.string.auto_general_688)
            "RECOVERY" -> holder.itemView.context.getString(R.string.auto_recovery_689)
            "INSTALLMENT" -> holder.itemView.context.getString(R.string.auto_installment_690)
            "FINANCING", "LOAN" -> holder.itemView.context.getString(R.string.auto_loan_691)
            "PAYMENT" -> holder.itemView.context.getString(R.string.auto_payment_692)
            "APPROVED" -> holder.itemView.context.getString(R.string.auto_approved_693)
            "REJECTED" -> holder.itemView.context.getString(R.string.auto_rejected_694)
            "FEEDBACK" -> holder.itemView.context.getString(R.string.feedback)
            "PROMOTION" -> holder.itemView.context.getString(R.string.auto_promotion_695)
            else -> item.category
        }
        holder.tvDate.text = item.date
        holder.tvIconEmoji.text = item.emoji
        holder.vUnreadDot.visibility = if (item.isUnread) View.VISIBLE else View.GONE

        // Set dynamic background color for icon
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(item.bgColorHex))
        }
        holder.flIcon.background = bgDrawable

        holder.itemView.setOnClickListener {
            if (item.isUnread) {
                onNotificationClick?.invoke(item, position)
            }
            
            // Show custom details dialog
            val context = holder.itemView.context
            val dialog = android.app.Dialog(context)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_notification_detail, null)
            dialog.setContentView(dialogView)
            
            val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
            val tvDialogDate = dialogView.findViewById<TextView>(R.id.tvDialogDate)
            val tvDialogMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
            val btnClose = dialogView.findViewById<View>(R.id.btnClose)
            
            tvDialogTitle.text = item.title
            tvDialogDate.text = item.date
            tvDialogMessage.text = item.message
            
            btnClose.setOnClickListener { dialog.dismiss() }
            
            dialog.show()
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun getItemCount() = notifications.size
}
