package com.assan.finance.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.assan.finance.R;
import com.assan.finance.adapters.DownloadsAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.widget.ImageView;
import android.widget.EditText;

public class DownloadsFragment extends Fragment {

    private RecyclerView rvDownloads;
    private TextView tvNoDownloads;
    private DownloadsAdapter adapter;
    private List<File> fileList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_downloads, container, false);
        
        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        }
        
        rvDownloads = view.findViewById(R.id.rvDownloads);
        tvNoDownloads = view.findViewById(R.id.tvNoDownloads);
        EditText etSearchDownloads = view.findViewById(R.id.etSearchDownloads);
        
        rvDownloads.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        loadFiles();

        if (etSearchDownloads != null) {
            etSearchDownloads.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterFiles(s.toString());
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }
        
        return view;
    }

    private void filterFiles(String query) {
        if (adapter == null) return;
        List<File> filteredList = new ArrayList<>();
        if (query.isEmpty()) {
            filteredList.addAll(fileList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (File file : fileList) {
                if (file.getName().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(file);
                }
            }
        }
        adapter.updateList(filteredList);
        
        if (filteredList.isEmpty()) {
            tvNoDownloads.setVisibility(View.VISIBLE);
            rvDownloads.setVisibility(View.GONE);
        } else {
            tvNoDownloads.setVisibility(View.GONE);
            rvDownloads.setVisibility(View.VISIBLE);
        }
    }
    
    private void loadFiles() {
        File docsDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (docsDir != null && docsDir.exists()) {
            File[] files = docsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
            if (files != null && files.length > 0) {
                // Sort by last modified (newest first)
                Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                fileList = Arrays.asList(files);
            }
        }
        
        if (fileList.isEmpty()) {
            tvNoDownloads.setVisibility(View.VISIBLE);
            rvDownloads.setVisibility(View.GONE);
        } else {
            tvNoDownloads.setVisibility(View.GONE);
            rvDownloads.setVisibility(View.VISIBLE);
            adapter = new DownloadsAdapter(requireContext(), fileList);
            rvDownloads.setAdapter(adapter);
        }
    }
}
