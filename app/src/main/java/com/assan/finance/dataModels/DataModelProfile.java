package com.assan.finance.dataModels;

import com.google.gson.annotations.SerializedName;

public class DataModelProfile {
    @SerializedName(value = "NAME", alternate = {"fulL_NAME", "name", "Name"})
    private String name;

    @SerializedName(value = "CNIC", alternate = {"cniC_NUMBER", "cnic", "Cnic"})
    private String cnic;

    @SerializedName(value = "DATE_OF_BIRTH", alternate = {"datE_OF_BIRTH", "date_of_birth", "dateOfBirth"})
    private String dateOfBirth;

    @SerializedName(value = "PHONE", alternate = {"phonE_NUMBER", "phone", "Phone"})
    private String phone;

    @SerializedName(value = "PROFILE_PICTURE", alternate = {"profilE_PICTURE", "profile_picture", "profilePicture", "PROFILE_IMAGE", "profile_image", "ProfilePicture"})
    private String profilePicture;


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

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
}
