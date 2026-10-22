package com.assan.finance.dataModels;

import com.google.gson.annotations.SerializedName;

public class DataModelLoanData {
    @SerializedName("CID")
    private String cid;

    @SerializedName("PROJECT_ID")
    private String projectId;

    @SerializedName("PRODUCT_ID")
    private String productId;

    @SerializedName("PRODUCT_NAME")
    private String productName;

    @SerializedName("LOAN_MIN_LIMIT")
    private String loanMinLimit;

    @SerializedName("LOAN_MAX_LIMIT")
    private String loanMaxLimit;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getLoanMinLimit() {
        return loanMinLimit;
    }

    public void setLoanMinLimit(String loanMinLimit) {
        this.loanMinLimit = loanMinLimit;
    }

    public String getLoanMaxLimit() {
        return loanMaxLimit;
    }

    public void setLoanMaxLimit(String loanMaxLimit) {
        this.loanMaxLimit = loanMaxLimit;
    }
}
