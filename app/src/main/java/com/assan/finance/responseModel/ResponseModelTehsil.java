package com.assan.finance.responseModel;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import com.assan.finance.dataModels.DataModelGetTehsil;

public class ResponseModelTehsil {
    @SerializedName("success")
    private boolean success;
    @SerializedName("tehsils")
    private List<DataModelGetTehsil> tehsilsData;

    public boolean isSuccess() {
        return success;
    }

    public List<DataModelGetTehsil> getReceipts() {
        return tehsilsData;
    }
}
