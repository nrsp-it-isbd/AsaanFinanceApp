package com.assan.finance.responseModel;

import com.google.gson.annotations.SerializedName;

import com.assan.finance.dataModels.DataModelProfile;

public class ResponseModelProfileUpdate {
    // FIX: the backend may send success as a boolean (true) or as text ("1" / "true"), and the real
    // boolean sometimes as "success_bool" (like Loan/submit). Read it as Object and interpret it.
    @SerializedName("success")
    private Object success;

    @SerializedName("success_bool")
    private Boolean successBool;

    @SerializedName("message")
    private String message;

    // FIX: validation errors come back as {"error": "CNIC is required"}; this text was never shown
    @SerializedName("error")
    private String error;

    @SerializedName(value = "updated_record", alternate = {"updatedRecord", "data", "UpdatedRecord", "RECORD", "record"})
    private DataModelProfile updatedRecord;

    public DataModelProfile getUpdatedRecord() {
        return updatedRecord;
    }

    public void setUpdatedRecord(DataModelProfile updatedRecord) {
        this.updatedRecord = updatedRecord;
    }

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

    /** Message to show the user: the server's message, or its error text when there is no message. */
    public String getMessage() {
        if (message != null && !message.trim().isEmpty()) return message;
        return error;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
