package com.assan.finance.responseModel;

import com.google.gson.annotations.SerializedName;

public class ResponseModelChangePassword {
    // The backend may send success as a boolean or as text ("1" / "true"); interpret both
    @SerializedName("success")
    private Object success;

    @SerializedName("success_bool")
    private Boolean successBool;

    @SerializedName("message")
    private String message;

    @SerializedName("error")
    private String error;

    public boolean isSuccess() {
        if (Boolean.TRUE.equals(successBool)) return true;
        if (success instanceof Boolean) return (Boolean) success;
        if (success instanceof Number) return ((Number) success).intValue() == 1;
        if (success instanceof String) {
            String s = ((String) success).trim();
            return s.equals("1") || s.equalsIgnoreCase("true");
        }
        return false;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    /** The server's message, or its error text when there is no message. */
    public String getMessage() {
        if (message != null && !message.trim().isEmpty()) return message;
        return error;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
