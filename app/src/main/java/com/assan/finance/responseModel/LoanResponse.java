package com.assan.finance.responseModel;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LoanResponse {
    @SerializedName("loanData")
    private List<LoanData> loanData;

    public List<LoanData> getLoanData() {
        return loanData;
    }

    public void setLoanData(List<LoanData> loanData) {
        this.loanData = loanData;
    }

    public static class LoanData {
        @SerializedName("APPLICANT_NAME")
        private String applicantName;

        @SerializedName("LOAN_AMOUNT")
        private String loanAmount;

        @SerializedName("STATUS")
        private String status;

        public String getApplicantName() {
            return applicantName;
        }

        public void setApplicantName(String applicantName) {
            this.applicantName = applicantName;
        }

        public String getLoanAmount() {
            return loanAmount;
        }

        public void setLoanAmount(String loanAmount) {
            this.loanAmount = loanAmount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}


