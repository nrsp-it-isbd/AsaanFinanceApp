package com.assan.finance.dataModels;

import com.google.gson.annotations.SerializedName;

public class DataModelGetLedgerAmount {
    @SerializedName("DUE_DATE")
    private String dueDate;

    @SerializedName("DUE_AMOUNT")
    private String dueAmount;

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getDueAmount() {
        return dueAmount;
    }

    public void setDueAmount(String dueAmount) {
        this.dueAmount = dueAmount;
    }
}