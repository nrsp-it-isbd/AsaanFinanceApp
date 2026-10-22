package com.assan.finance.dataModels;

import com.google.gson.annotations.SerializedName;

public class DataModelGetLedger {
    @SerializedName("RECEIPT_NO")
    private String receiptNo;

    @SerializedName("DUE_DATE")
    private String dueDate;

    @SerializedName("TOTAL")
    private String total;

    @SerializedName("INSTALLMENTS")
    private String INSTALLMENTS;

    @SerializedName("PAID_DATE")
    private String PAID_DATE;


    public String getINSTALLMENTS() {
        return INSTALLMENTS;
    }

    public void setINSTALLMENTS(String INSTALLMENTS) {
        this.INSTALLMENTS = INSTALLMENTS;
    }

    public String getPAID_DATE() {
        return PAID_DATE;
    }

    public void setPAID_DATE(String PAID_DATE) {
        this.PAID_DATE = PAID_DATE;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }
}