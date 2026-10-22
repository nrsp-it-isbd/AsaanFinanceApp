package com.assan.finance.responseModel;

import java.util.List;

import com.assan.finance.dataModels.DataModelBanks;

public class ResponseModelBanks {
    private List<DataModelBanks> bankChannels;

    public List<DataModelBanks> getBankChannels() {
        return bankChannels;
    }

    public void setBankChannels(List<DataModelBanks> bankChannels) {
        this.bankChannels = bankChannels;
    }
}