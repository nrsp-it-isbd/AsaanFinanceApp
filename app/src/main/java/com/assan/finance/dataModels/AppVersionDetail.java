package com.assan.finance.dataModels;

public class AppVersionDetail {
    private String ID;
    private String APP_VERSION_NAME;
    private String APP_VERSION_CODE;
    private String APP_VERSION_DATE;

    // Getters and setters
    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getAPP_VERSION_NAME() {
        return APP_VERSION_NAME;
    }

    public void setAPP_VERSION_NAME(String APP_VERSION_NAME) {
        this.APP_VERSION_NAME = APP_VERSION_NAME;
    }

    public String getAPP_VERSION_CODE() {
        return APP_VERSION_CODE;
    }

    public void setAPP_VERSION_CODE(String APP_VERSION_CODE) {
        this.APP_VERSION_CODE = APP_VERSION_CODE;
    }

    public String getAPP_VERSION_DATE() {
        return APP_VERSION_DATE;
    }

    public void setAPP_VERSION_DATE(String APP_VERSION_DATE) {
        this.APP_VERSION_DATE = APP_VERSION_DATE;
    }
}