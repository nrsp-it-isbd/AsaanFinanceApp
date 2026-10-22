package com.assan.finance.dataModels;

import com.google.gson.annotations.SerializedName;

public class DataModelLogin {

    @SerializedName("PHONE")
    private String PHONE;

    @SerializedName("PASSWORD")
    private Boolean password;


    public String getPHONE() {
        return PHONE;
    }

    public void setPHONE(String PHONE) {
        this.PHONE = PHONE;
    }

    public Boolean getPassword() {
        return password;
    }

    public void setPassword(Boolean password) {
        this.password = password;
    }
}
