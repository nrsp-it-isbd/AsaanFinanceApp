package com.assan.finance.responseModel;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ResponseModelBankChannels {
    @SerializedName("BANK_NAME")
    private String bankName;

    @SerializedName("bankChannels")
    private List<ResponseModelBankChannels> bankChannels;

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public List<ResponseModelBankChannels> getBankChannels() {
        return bankChannels;
    }

    public void setBankChannels(List<ResponseModelBankChannels> bankChannels) {
        this.bankChannels = bankChannels;
    }
}
