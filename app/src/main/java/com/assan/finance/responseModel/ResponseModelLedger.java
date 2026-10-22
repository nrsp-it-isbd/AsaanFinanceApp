package com.assan.finance.responseModel;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import com.assan.finance.dataModels.DataModelGetLedger;

public class ResponseModelLedger {
    @SerializedName("success")
    private boolean success;
    @SerializedName("data")
    private List<DataModelGetLedger> receiptsData;

    public boolean isSuccess() {
        return success;
    }

    public List<DataModelGetLedger> getReceipts() {
        return receiptsData;
    }
}

