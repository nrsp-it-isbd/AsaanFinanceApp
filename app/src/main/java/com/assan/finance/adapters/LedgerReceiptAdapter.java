package com.assan.finance.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import com.assan.finance.R;
import com.assan.finance.dataModels.DataModelGetLedger;

public class LedgerReceiptAdapter extends RecyclerView.Adapter<LedgerReceiptAdapter.ViewHolder> {
    private List<DataModelGetLedger> receiptList;

    public LedgerReceiptAdapter(List<DataModelGetLedger> receiptList) {
        this.receiptList = receiptList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_ledger, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataModelGetLedger receipt = receiptList.get(position);
        holder.receiptNoTextView.setText(receipt.getReceiptNo());
        holder.dueDateTextView.setText(receipt.getDueDate());
        holder.totalTextView.setText(receipt.getTotal());
        holder.txtInstallments.setText(receipt.getINSTALLMENTS());
        holder.txtPaidDate.setText(receipt.getPAID_DATE());
    }

    @Override
    public int getItemCount() {
        return receiptList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        AppCompatTextView receiptNoTextView;
        AppCompatTextView dueDateTextView;
        AppCompatTextView totalTextView;
        AppCompatTextView txtInstallments;
        AppCompatTextView txtPaidDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            receiptNoTextView = itemView.findViewById(R.id.txtRECEIPT_NO);
            dueDateTextView = itemView.findViewById(R.id.txtDueDate);
            totalTextView = itemView.findViewById(R.id.txtTotal);
            txtInstallments = itemView.findViewById(R.id.txtInstallments);
            txtPaidDate = itemView.findViewById(R.id.txtPaidDate);
        }
    }
}