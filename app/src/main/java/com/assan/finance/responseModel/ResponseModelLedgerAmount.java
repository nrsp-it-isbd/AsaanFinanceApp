package com.assan.finance.responseModel;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import com.assan.finance.dataModels.DataModelGetLedgerAmount;

public class ResponseModelLedgerAmount {
    @SerializedName("success")
    private boolean success;
    @SerializedName("data")
    private List<DataModelGetLedgerAmount> ledgerAmounts;

    public boolean isSuccess() {
        return success;
    }

    public List<DataModelGetLedgerAmount> getLedgerAmounts() {
        return ledgerAmounts;
    }
}

