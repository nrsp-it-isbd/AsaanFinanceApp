package com.assan.finance.apis;

import com.assan.finance.responseModel.LoanResponse;
import com.assan.finance.responseModel.ResponseAppVersion;
import com.assan.finance.responseModel.ResponseModelBankChannels;
import com.assan.finance.responseModel.ResponseModelBanks;
import com.assan.finance.responseModel.ResponseModelChangePassword;
import com.assan.finance.responseModel.ResponseModelDistricts;
import com.assan.finance.responseModel.ResponseModelLedger;
import com.assan.finance.responseModel.ResponseModelLedgerAmount;
import com.assan.finance.responseModel.ResponseModelLoanApplication;
import com.assan.finance.responseModel.ResponseModelLoanMinMax;
import com.assan.finance.responseModel.ResponseModelLogin;
import com.assan.finance.requestModels.LoginRequest;
import com.assan.finance.responseModel.ResponseModelProfileUpdate;
import com.assan.finance.responseModel.ResponseModelRegistration;
import com.assan.finance.responseModel.ResponseModelTehsil;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {

        @POST("Metadata/banks")
        Call<ResponseModelBanks> getAllBanks();

        @GET("Home/GetHomeScreenData")
        Call<com.assan.finance.responseModel.ResponseModelHomeScreenData> getHomeScreenData();

        // With Live APC
        @Headers({ "Content-Type: application/json" })
        @POST("Auth/login")
        Call<ResponseModelLogin> getLoginData(
                        @Body LoginRequest loginRequest);

        @Multipart
        @POST("Auth/register")
        Call<ResponseModelRegistration> registerUser(
                        @Part("NAME") RequestBody fullName,
                        @Part("USER_NAME") RequestBody userName,
                        @Part("CNIC") RequestBody cnic,
                        @Part("PHONE") RequestBody phone,
                        @Part("PASSWORD") RequestBody password,
                        @Part("DATE_OF_BIRTH") RequestBody dob,
                        @Part("CNIC_EXPIRY_DATE") RequestBody cnicExpiryDate,
                        @Part("PRESENT_ADDRESS") RequestBody presentAddress,
                        @Part("PERMANENT_ADDRESS") RequestBody permanentAddress,
                        @Part("PROFILE_PICTURE") RequestBody profilePicture,
                        @Part("GENDER") RequestBody gender);

        @Multipart
        @POST("Loan/submit")
        Call<ResponseModelLoanApplication> submitLoanForm(
                        @Part("TEHSIL_ID") RequestBody tehsilId,
                        @Part("DISTRICT_ID") RequestBody districtId,
                        @Part("REGION_ID") RequestBody regionId,
                        @Part("FIELD_UNIT_ID") RequestBody fieldUnitId,
                        @Part("PROFILE_PICTURE") RequestBody profilePicture,
                        @Part("APPLICANT_NAME") RequestBody applicantName,
                        @Part("APPLICANT_CNIC") RequestBody applicantCNIC,
                        @Part("LOAN_USED_BY_NAME") RequestBody loanUsedByName,
                        @Part("LOAN_USED_BY_CNIC") RequestBody loanUsedByCNIC,
                        @Part("DATE_OF_BIRTH") RequestBody dateOfBirth,
                        @Part("MARITAL_STATUS") RequestBody maritalStatus,
                        @Part("PRESENT_ADDRESS") RequestBody presentAddress,
                        @Part("PERMANENT_ADDRESS") RequestBody permanentAddress,
                        @Part("BANK_NAME") RequestBody bankName,
                        @Part("BANK_ACC_NUMBER") RequestBody bankAccountNumber,
                        @Part("BANK_BRANCH_CODE") RequestBody bankBranchCode,
                        @Part("BANK_BRANCH_ADDRESS") RequestBody bankBranchAddress,
                        @Part("HOW_OLD_BUSINESS") RequestBody howOldBusiness,
                        @Part("BUSINESS_ADDRESS") RequestBody businessAddress,
                        @Part("BUSINESS_CITY_NAME") RequestBody businessCityName,
                        @Part("OCCUPATION") RequestBody occupation,
                        @Part("PURPOSE_OF_LOAN") RequestBody purposeOfLoan,
                        @Part("REQUEST_AMOUNT") RequestBody requestAmount,
                        @Part("DURATION_OF_LOAN") RequestBody durationOfLoan,
                        @Part("CLIENT_ID") RequestBody clientID,
                        @Part("BORROWER_NO") RequestBody borrowerNumber,
                        @Part("PHONE") RequestBody phone,
                        @Part("SOURCE_OF_INCOME") RequestBody sourceOfIncome,
                        @Part("NATURE_OF_BUSINESS") RequestBody natureOfBusiness,
                        @Part("GENDER") RequestBody gender,
                        @Part("CNIC_EXPIRY_DATE") RequestBody cnicExpiryDate,
                        @Part("CNIC_ISSUE_DATE") RequestBody cnicIssueDate,
                        @Part("CNIC_FRONT") RequestBody cnicFront,
                        @Part("CNIC_BACK") RequestBody cnicBack);

        @Multipart
        @POST("Profile/update")
        Call<ResponseModelProfileUpdate> updateProfile(
                        @Part("NAME") RequestBody userName,
                        @Part("CNIC") RequestBody cnic,
                        @Part("PHONE") RequestBody phone,
                        @Part("DATE_OF_BIRTH") RequestBody dob,
                        @Part("PROFILE_PICTURE") RequestBody profilePicture,
                        @Part("CLIENT_ID") RequestBody clientId);

        @GET("Loan/loandata")
        Call<com.assan.finance.responseModel.ResponseModelUserLoanData> getUserLoanData(
                        @Query("clientid") String clientId);

        @FormUrlEncoded
        @POST("Profile/update-password")
        Call<ResponseModelChangePassword> changePassword(@Field("CLIENT_ID") String CLIENT_ID,
                        // FIX: the backend requires CNIC; without it every password change failed (400)
                        @Field("CNIC") String CNIC,
                        @Field("OLD_PASSWORD") String OLD_PASSWORD,
                        @Field("NEW_PASSWORD") String NEW_PASSWORD);

        @FormUrlEncoded
        @POST("Profile/update-password-otp")
        Call<ResponseModelChangePassword> updatePassword(@Field("PHONE") String PHONE,
                        @Field("NEW_PASSWORD") String NEW_PASSWORD);

        @GET("Metadata/districts")
        Call<ResponseModelDistricts> getAllDistricts();

        @GET("Metadata/tehsils")
        Call<ResponseModelTehsil> getAllTehsil(@Query("DISTRICT_ID") String DISTRICT_ID);

        @GET("Metadata/banks")
        Call<ResponseModelBankChannels> getAllBankChannels();

        @POST
        Call<com.google.gson.JsonObject> processLoanAssistant(
                        @retrofit2.http.Url String url,
                        @Body okhttp3.RequestBody body);

        @Multipart
        @POST
        Call<com.google.gson.JsonObject> processGeminiOcr(
                        @retrofit2.http.Url String url,
                        @Part okhttp3.MultipartBody.Part image,
                        @Part("side") okhttp3.RequestBody side);

        @GET("Loan/data")
        Call<LoanResponse> getLoanData(@Query("clientid") int clientId);

}
