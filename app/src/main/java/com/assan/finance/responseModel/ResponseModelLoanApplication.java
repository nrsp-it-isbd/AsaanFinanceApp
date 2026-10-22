package com.assan.finance.responseModel;

import com.google.gson.annotations.SerializedName;

public class ResponseModelLoanApplication {
    // The backend sends "success":"1" (string) and the real boolean as "success_bool"
    @SerializedName("success")
    private boolean success;

    @SerializedName("success_bool")
    private Boolean successBool;
    @SerializedName("message")
    private String message;

    @SerializedName("status")
    private String status;

    @SerializedName("TOKEN_NUMBER")
    private String tokenNumber;

    // Loan/submit returns { data: { applicationId, tokenNumber, profilePicture } }
    @SerializedName("data")
    private Data data;

    public static class Data {
        @SerializedName("applicationId")
        private Integer applicationId;

        @SerializedName("tokenNumber")
        private String tokenNumber;

        @SerializedName("profilePicture")
        private String profilePicture;

        public Integer getApplicationId() {
            return applicationId;
        }

        public String getTokenNumber() {
            return tokenNumber;
        }

        public String getProfilePicture() {
            return profilePicture;
        }
    }

    public Data getData() {
        return data;
    }

    /** Server path of the saved profile picture, or null if the backend did not save one. */
    public String getProfilePicture() {
        return data != null ? data.getProfilePicture() : null;
    }

    public boolean isSuccess() {
        return success || Boolean.TRUE.equals(successBool);
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTokenNumber() {
        // FIX: the backend sends the token inside data.tokenNumber, not TOKEN_NUMBER
        if ((tokenNumber == null || tokenNumber.isEmpty()) && data != null) {
            return data.getTokenNumber();
        }
        return tokenNumber;
    }

    public void setTokenNumber(String tokenNumber) {
        this.tokenNumber = tokenNumber;
    }
}

