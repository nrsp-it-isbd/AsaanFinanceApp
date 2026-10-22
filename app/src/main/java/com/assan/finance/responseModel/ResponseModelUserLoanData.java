package com.assan.finance.responseModel;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ResponseModelUserLoanData {
    @SerializedName("loanData")
    private List<UserLoanData> loanData;

    public List<UserLoanData> getLoanData() {
        return loanData;
    }

    public static class UserLoanData {
        @SerializedName("APPLICANT_NAME")
        private String applicantName;

        @SerializedName("LOAN_AMOUNT")
        private String loanAmount;

        @SerializedName("STATUS")
        private String status;

        public String getApplicantName() { return applicantName; }
        public String getLoanAmount() { return loanAmount; }
        public String getStatus() { return status; }
    }
}
