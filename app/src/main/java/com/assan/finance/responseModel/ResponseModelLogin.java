package com.assan.finance.responseModel;

import com.google.gson.annotations.SerializedName;

public class ResponseModelLogin {

    @SerializedName("success_bool")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private UserData data;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UserData getData() {
        return data;
    }

    public void setData(UserData data) {
        this.data = data;
    }

    @SerializedName("success")
    private String successString;

    public String getSuccess() {
        return successString;
    }

    public static class UserData {
        @SerializedName(value = "id", alternate = {"CLIENT_ID"})
        private String clientId;

        @SerializedName(value = "name", alternate = {"NAME"})
        private String name;

        @SerializedName(value = "username", alternate = {"USER_NAME"})
        private String userName;

        @SerializedName(value = "emaiL_ADDRESS", alternate = {"EMAIL"})
        private String emailAddress;

        @SerializedName(value = "rolE_ID", alternate = {"ROLE_ID"})
        private String roleId;
        
        @SerializedName("regioN_ID")
        private String regionId;

        @SerializedName("regioN_NAME")
        private String regionName;

        @SerializedName("fielD_UNIT_ID")
        private String fieldUnitId;

        @SerializedName("fielD_UNIT_NAME")
        private String fieldUnitName;

        @SerializedName("status")
        private String status;

        @SerializedName("CNIC")
        private String cnic;

        @SerializedName("DATE_OF_BIRTH")
        private String dateOfBirth;

        @SerializedName("CNIC_EXPIRY")
        private String cnicExpiry;

        @SerializedName("CNIC_ISSUE_DATE")
        private String cnicIssueDate;

        @SerializedName("PHONE")
        private String phone;

        @SerializedName("GENDER")
        private String gender;

        @SerializedName(value = "PROFILE_PICTURE", alternate = {"profilE_PICTURE", "profile_picture", "profilePicture", "PROFILE_IMAGE", "profile_image", "ProfilePicture"})
        private String profilePicture;

        @SerializedName("BORROWER_NO")
        private String borrowerNo;

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }

        public String getEmailAddress() { return emailAddress; }
        public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

        public String getRoleId() { return roleId; }
        public void setRoleId(String roleId) { this.roleId = roleId; }

        public String getRegionId() { return regionId; }
        public void setRegionId(String regionId) { this.regionId = regionId; }

        public String getRegionName() { return regionName; }
        public void setRegionName(String regionName) { this.regionName = regionName; }

        public String getFieldUnitId() { return fieldUnitId; }
        public void setFieldUnitId(String fieldUnitId) { this.fieldUnitId = fieldUnitId; }

        public String getFieldUnitName() { return fieldUnitName; }
        public void setFieldUnitName(String fieldUnitName) { this.fieldUnitName = fieldUnitName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getCnic() { return cnic; }
        public void setCnic(String cnic) { this.cnic = cnic; }

        public String getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

        public String getCnicExpiry() { return cnicExpiry; }
        public void setCnicExpiry(String cnicExpiry) { this.cnicExpiry = cnicExpiry; }

        public String getCnicIssueDate() { return cnicIssueDate; }
        public void setCnicIssueDate(String cnicIssueDate) { this.cnicIssueDate = cnicIssueDate; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }

        public String getProfilePicture() { return profilePicture; }
        public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

        public String getBorrowerNo() { return borrowerNo; }
        public void setBorrowerNo(String borrowerNo) { this.borrowerNo = borrowerNo; }
    }
}
