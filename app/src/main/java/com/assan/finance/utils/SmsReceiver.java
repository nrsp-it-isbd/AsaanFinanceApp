package com.assan.finance.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsReceiver extends BroadcastReceiver {

    private final SmsListener smsListener;

    public SmsReceiver(SmsListener smsListener) {
        this.smsListener = smsListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus != null) {
                for (Object pdu : pdus) {
                    SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                    String messageBody = smsMessage.getMessageBody();
                    String otp = extractOtp(messageBody);
                    if (otp != null && otp.length() == 4 && smsListener != null) {
                        smsListener.onOTPReceived(otp);
                    }
                }
            }
        }
    }

    private String extractOtp(String messageBody) {
        Pattern pattern = Pattern.compile("\\b(\\d{4})\\b");
        Matcher matcher = pattern.matcher(messageBody);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public interface SmsListener {
        void onOTPReceived(String otp);
    }
}
