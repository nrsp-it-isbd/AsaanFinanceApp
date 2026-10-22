package com.assan.finance.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import com.assan.finance.R;

public class CustomSpinnerAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final List<String> bankNames;
    // private final int[] bankImages;

    public CustomSpinnerAdapter(Context context, int resource, List<String> bankNames) {
        super(context, resource, bankNames);
        this.context = context;
        this.bankNames = bankNames;
        //  this.bankImages = bankImages;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    private View getCustomView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.custom_spinner_item, parent, false);
        }

        //ImageView imageView = convertView.findViewById(R.id.spinner_image);
        TextView textView = convertView.findViewById(R.id.spinner_text);

        textView.setText(bankNames.get(position));
        //  imageView.setImageResource(bankImages[position]);

        return convertView;
    }
}
