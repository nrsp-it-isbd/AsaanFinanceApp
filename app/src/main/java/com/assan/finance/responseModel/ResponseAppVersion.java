package com.assan.finance.responseModel;

import java.util.List;

import com.assan.finance.dataModels.AppVersionDetail;

public class ResponseAppVersion {

    private List<AppVersionDetail> AppVersionData;

    public List<AppVersionDetail> getAppVersionData() {
        return AppVersionData;
    }

    public void setAppVersionData(List<AppVersionDetail> appVersionData) {
        AppVersionData = appVersionData;
    }
}
