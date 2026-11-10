package com.assan.finance.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bank apps copy extra text with the IBAN, e.g. HBL copies "IBAN PK24MEZN0082010102520857". Pasted into the
 * 24-character IBAN field this became "IBANPK24MEZN008201010252" (label kept, last digits cut).
 * Plain Java (no Android), so it is unit tested.
 */
public final class IbanText {

    private static final Pattern PAKISTAN_IBAN = Pattern.compile("PK\\d{2}[A-Z]{4}\\d{16}");

    private IbanText() {
    }

    /**
     * Pasted text (already upper case, letters and digits only) -> the IBAN inside it. If no full Pakistani IBAN
     * is found, a leading "IBAN" label is removed and the rest is kept for the user to correct.
     */
    public static String fromPaste(String cleaned) {
        if (cleaned == null) return "";
        Matcher m = PAKISTAN_IBAN.matcher(cleaned);
        if (m.find()) return m.group();
        String s = cleaned;
        while (s.startsWith("IBAN")) s = s.substring(4);
        return s;
    }
}
