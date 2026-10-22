package com.assan.finance.responseModel;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import com.assan.finance.dataModels.DataModelLoanData;

public class ResponseModelLoanMinMax {
    @SerializedName("loanData")
    private List<DataModelLoanData> loanData;

    public List<DataModelLoanData> getLoanData() {
        return loanData;
    }

    public void setLoanData(List<DataModelLoanData> loanData) {
        this.loanData = loanData;
    }

}
