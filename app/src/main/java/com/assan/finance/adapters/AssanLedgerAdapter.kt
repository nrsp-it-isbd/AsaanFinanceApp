package com.assan.finance.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.assan.finance.R
import com.assan.finance.model.assan.RecoveryScheduleRecord

interface OnLedgerRowClickListener {
    fun onRowClick(item: RecoveryScheduleRecord)
}

class AssanLedgerAdapter(
    private val scheduleList: List<RecoveryScheduleRecord>,
    private val listener: OnLedgerRowClickListener
) : RecyclerView.Adapter<AssanLedgerAdapter.LedgerViewHolder>() {

    class LedgerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvInstNo: TextView = itemView.findViewById(R.id.tvInstNo)
        val tvDueDate: TextView = itemView.findViewById(R.id.tvDueDate)
        val tvDueTotal: TextView = itemView.findViewById(R.id.tvDueTotal)
        val tvRecDate: TextView = itemView.findViewById(R.id.tvRecDate)
        val tvRecTotal: TextView = itemView.findViewById(R.id.tvRecTotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LedgerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ledger_receipt_v2, parent, false)
        return LedgerViewHolder(view)
    }

    override fun onBindViewHolder(holder: LedgerViewHolder, position: Int) {
        val item = scheduleList[position]

        holder.tvInstNo.text = item.installmentNo.toString()
        holder.tvDueDate.text = item.dueDate?.substringBefore("T") ?: "N/A"
        holder.tvDueTotal.text = String.format(java.util.Locale.US, "%,.0f", item.totalDue ?: 0.0)
        holder.tvRecDate.text = item.receiptDate?.substringBefore("T") ?: "N/A"
        val totalRec = (item.recoveredPrincipal ?: 0.0) + (item.recoveredServiceCharge ?: 0.0)
        holder.tvRecTotal.text = String.format(java.util.Locale.US, "%,.0f", totalRec)

        // Alternate row colors for readability
        if (position % 2 == 1) {
            holder.itemView.setBackgroundColor(Color.parseColor("#F9FAFB")) // Very light grey
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE)
        }

        holder.itemView.setOnClickListener {
            listener.onRowClick(item)
        }
    }

    override fun getItemCount(): Int {
        return scheduleList.size
    }
}
