package com.assan.finance.dataModels;

import com.google.gson.annotations.SerializedName;

public class DataModelRegistration {
    @SerializedName("NAME")
    private String name;

    @SerializedName("CNIC")
    private String cnic;

    @SerializedName("CNIC_EXPIRY_DATE")
    private String cnicExpiryDate;

    @SerializedName("PASSWORD")
    private String password;

    @SerializedName("DATE_OF_BIRTH")
    private String dateOfBirth;

    @SerializedName("PHONE")
    private String phone;

    @SerializedName("CNIC_FRONT_IMAGE_PATH")
    private String cnicFrontImagePath;

    @SerializedName("CNIC_BACK_IMAGE_PATH")
    private String cnicBackImagePath;

    @SerializedName("PROFILE_PICTURE")
    private String profilePicture;
    @SerializedName("GENDER")
    private String gender;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCnic() {
        return cnic;
    }

    public void setCnic(String cnic) {
        this.cnic = cnic;
    }

    public String getCnicExpiryDate() {
        return cnicExpiryDate;
    }

    public void setCnicExpiryDate(String cnicExpiryDate) {
        this.cnicExpiryDate = cnicExpiryDate;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCnicFrontImagePath() {
        return cnicFrontImagePath;
    }

    public void setCnicFrontImagePath(String cnicFrontImagePath) {
        this.cnicFrontImagePath = cnicFrontImagePath;
    }

    public String getCnicBackImagePath() {
        return cnicBackImagePath;
    }

    public void setCnicBackImagePath(String cnicBackImagePath) {
        this.cnicBackImagePath = cnicBackImagePath;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
