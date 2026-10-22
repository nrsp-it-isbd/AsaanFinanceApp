package com.assan.finance.utils;

import android.text.InputFilter;
import android.text.Spanned;

import android.widget.EditText;

public class RegexInputFilter implements InputFilter {
    private final EditText editText;
    private final String regex;

    public RegexInputFilter(EditText editText, String regex) {
        this.editText = editText;
        this.regex = regex;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        String input = source.toString();
        if (input.isEmpty()) return null;

        String newValue = dest.toString().substring(0, dstart) + input + dest.toString().substring(dend);
        if (newValue.matches(regex)) {
            return null;
        } else {
            return "";
        }
    }
}
