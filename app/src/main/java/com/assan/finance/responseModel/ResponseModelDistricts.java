package com.assan.finance.responseModel;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ResponseModelDistricts {
    @SerializedName("DISTRICT_ID")
    private String districtId;

    @SerializedName("DISTRICT")
    private String districtName;

    @SerializedName("PROVINCE_ID")
    private String provinceId;
    // Getter and Setter for List of ResponseModelDistricts (if needed)
    @SerializedName("districts")
    private List<ResponseModelDistricts> districts;

    public String getDistrictId() {
        return districtId;
    }

    public String getDistrictName() {
        return districtName;
    }

    public String getProvinceId() {
        return provinceId;
    }

    public List<ResponseModelDistricts> getDistricts() {
        return districts;
    }

    public void setDistricts(List<ResponseModelDistricts> districts) {
        this.districts = districts;
    }
}
