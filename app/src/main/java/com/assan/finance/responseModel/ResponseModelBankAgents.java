package com.assan.finance.responseModel;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ResponseModelBankAgents {
    @SerializedName("BANK_NAME")
    private String bankName;

    @SerializedName("bankChannels")
    private List<ResponseModelBankAgents> bankChannels;

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public List<ResponseModelBankAgents> getBankChannels() {
        return bankChannels;
    }

    public void setBankChannels(List<ResponseModelBankAgents> bankChannels) {
        this.bankChannels = bankChannels;
    }
}
