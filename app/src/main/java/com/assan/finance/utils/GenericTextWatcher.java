package com.assan.finance.utils;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;

public class GenericTextWatcher implements TextWatcher, View.OnKeyListener {
    private final AppCompatEditText currentView;
    private final AppCompatEditText nextView;
    private final AppCompatEditText previousView;
    private final AppCompatButton submitButton;
    private final Context context;

    public GenericTextWatcher(Context context, AppCompatEditText currentView, AppCompatEditText nextView, AppCompatButton submitButton) {
        this(context, currentView, nextView, null, submitButton);
    }

    public GenericTextWatcher(Context context, AppCompatEditText currentView, AppCompatEditText nextView, AppCompatEditText previousView, AppCompatButton submitButton) {
        this.currentView = currentView;
        this.nextView = nextView;
        this.previousView = previousView;
        this.submitButton = submitButton;
        this.context = context;
        this.currentView.setOnKeyListener(this);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // No implementation needed
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // No implementation needed
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (s.length() == 1) {
            if (nextView != null) {
                nextView.requestFocus();
            } else {
                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(currentView.getWindowToken(), 0);
                }

                // Trigger submit button click
                submitButton.performClick();
            }
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
            if (currentView.getText().length() == 0 && previousView != null) {
                previousView.requestFocus();
                previousView.setText("");
            }
        }
        return false;
    }
}
