package com.assan.finance.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Server dates come in UTC, e.g. "2026-07-10T19:00:00.000Z", which is 11-Jul-2026 00:00 in Pakistan (the date
 * the portal shows). Cutting the text at "T" showed every ledger date one day early. Plain Java (no Android),
 * so it is unit tested.
 */
public final class ServerDates {

    private ServerDates() {
    }

    /** The date in Pakistan time as "yyyy-MM-dd"; a value without a time zone is returned as is. */
    public static String toLocalDate(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.isEmpty() || !s.contains("T")) return s;
        String datePart = s.substring(0, s.indexOf('T'));
        boolean hasZone = s.endsWith("Z") || s.matches(".*T.*[+-]\\d{2}:?\\d{2}$");
        if (!hasZone) return datePart;
        String[] patterns = {"yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mmX"};
        for (String p : patterns) {
            try {
                SimpleDateFormat in = new SimpleDateFormat(p, Locale.US);
                in.setLenient(false);
                Date d = in.parse(s);
                if (d == null) continue;
                SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                out.setTimeZone(TimeZone.getTimeZone("Asia/Karachi"));
                return out.format(d);
            } catch (ParseException ignored) {
            }
        }
        return datePart;
    }

    /** Date for the screen like the portal ledger: "11-Jul-26" (Pakistan date). Unreadable values are returned as is. */
    public static String toDisplayDate(String raw) {
        String local = toLocalDate(raw);
        if (local.isEmpty()) return local;
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            in.setLenient(false);
            Date d = in.parse(local);
            return d == null ? local : new SimpleDateFormat("dd-MMM-yy", Locale.US).format(d);
        } catch (ParseException e) {
            return local;
        }
    }
}
