package com.assan.finance.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.assan.finance.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.widget.ImageView;

public class DownloadsAdapter extends RecyclerView.Adapter<DownloadsAdapter.ViewHolder> {

    private final Context context;
    private List<File> files;

    public DownloadsAdapter(Context context, List<File> files) {
        this.context = context;
        this.files = new java.util.ArrayList<>(files);
    }
    
    public void updateList(List<File> newFiles) {
        this.files.clear();
        this.files.addAll(newFiles);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_download_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = files.get(position);
        holder.tvFileName.setText(file.getName());

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy, hh:mm a", Locale.getDefault());
        holder.tvFileDate.setText(sdf.format(new Date(file.lastModified())));

        long sizeInBytes = file.length();
        long sizeInKb = sizeInBytes / 1024;
        if (sizeInKb > 1024) {
            holder.tvFileSize.setText((sizeInKb / 1024) + " MB");
        } else {
            holder.tvFileSize.setText(sizeInKb + " KB");
        }

        holder.itemView.setOnClickListener(v -> openPdf(file));
        holder.btnShareFile.setOnClickListener(v -> sharePdf(file));
        holder.btnDeleteFile.setOnClickListener(v -> confirmDelete(file, position));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    private void sharePdf(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(shareIntent, "Share Ledger PDF"));
        } catch (Exception e) {
            Toast.makeText(context, R.string.auto_could_not_share_pdf_630, Toast.LENGTH_SHORT).show();
        }
    }

    private void openPdf(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            Intent chooser = Intent.createChooser(intent, "Open PDF");
            context.startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(context, R.string.auto_no_pdf_viewer_found_631, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void confirmDelete(File file, int position) {
        new android.app.AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.action_delete_ledger))
            .setMessage(context.getString(R.string.msg_confirm_delete_ledger))
            .setPositiveButton(context.getString(R.string.action_delete), (dialog, which) -> {
                if (file.exists() && file.delete()) {
                    files.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, files.size());
                    Toast.makeText(context, R.string.auto_ledger_deleted_632, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, R.string.auto_failed_to_delete_file_633, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(context.getString(R.string.action_cancel), null)
            .show();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName, tvFileDate, tvFileSize;
        ImageView btnShareFile, btnDeleteFile;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileDate = itemView.findViewById(R.id.tvFileDate);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            btnShareFile = itemView.findViewById(R.id.btnShareFile);
            btnDeleteFile = itemView.findViewById(R.id.btnDeleteFile);
        }
    }
}
