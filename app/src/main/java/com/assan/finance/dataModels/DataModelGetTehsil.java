package com.assan.finance.dataModels;

import com.google.gson.annotations.SerializedName;

public class DataModelGetTehsil {
    @SerializedName("TEHSIL_ID")
    private String tehsilId;

    @SerializedName("TEHSIL")
    private String tehsilName;

    public String getTehsilId() {
        return tehsilId;
    }

    public void setTehsilId(String tehsilId) {
        this.tehsilId = tehsilId;
    }

    public String getTehsilName() {
        return tehsilName;
    }

    public void setTehsilName(String tehsilName) {
        this.tehsilName = tehsilName;
    }
}