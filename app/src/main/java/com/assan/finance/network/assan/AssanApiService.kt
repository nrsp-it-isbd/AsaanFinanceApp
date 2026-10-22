package com.assan.finance.network.assan

import com.assan.finance.model.assan.ForgotPasswordRequest
import com.assan.finance.model.assan.LoginRequest
import com.assan.finance.model.assan.ResponseModelAssanBanks
import com.assan.finance.model.assan.ResponseModelAssanDistricts
import com.assan.finance.model.assan.ResponseModelAssanTehsils
import com.assan.finance.model.assan.ResponseModelForgotPassword
import com.assan.finance.model.assan.ResponseModelLogin
import com.assan.finance.model.assan.ResponseModelRegistration
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface AssanApiService {

    @Headers("Content-Type: application/json")
    @POST("Auth/login")
    fun getLoginData(@Body loginRequest: LoginRequest): Call<ResponseModelLogin>

    @Headers("Content-Type: application/json")
    @POST("Auth/forgot-password")
    fun forgotPassword(@Body request: ForgotPasswordRequest): Call<ResponseModelForgotPassword>

    @Multipart
    @POST("Auth/register")
    fun registerUser(
        @Part("NAME") fullName: RequestBody,
        @Part("USER_NAME") userName: RequestBody,
        @Part("CNIC") cnic: RequestBody,
        @Part("PHONE") phone: RequestBody,
        @Part("PASSWORD") password: RequestBody,
        @Part("DATE_OF_BIRTH") dob: RequestBody,
        @Part("CNIC_EXPIRY_DATE") cnicExpiry: RequestBody,
        @Part("GENDER") gender: RequestBody,
        @Part("DEVICE_ID") deviceId: RequestBody,
        @Part("PRESENT_ADDRESS") presentAddress: RequestBody,
        @Part("PERMANENT_ADDRESS") permanentAddress: RequestBody,
        @Part("PROFILE_PICTURE") profilePicture: RequestBody
    ): Call<ResponseModelRegistration>

    @GET("Metadata/districts")
    fun getDistricts(): Call<ResponseModelAssanDistricts>

    @GET("Metadata/tehsils")
    fun getTehsils(@Query("DISTRICT_ID") districtId: String): Call<ResponseModelAssanTehsils>

    @GET("Metadata/banks")
    fun getBanks(): Call<ResponseModelAssanBanks>

    @GET("PaymentChannels")
    fun getPaymentChannels(): Call<com.assan.finance.model.assan.ResponseModelPaymentChannels>

    @Headers("Content-Type: application/json")
    @POST("Payment/SubmitManualPayment")
    fun submitManualPayment(@Body req: com.assan.finance.model.assan.RequestModelManualPayment): Call<com.assan.finance.model.assan.ResponseModelManualPayment>

    @GET("Metadata/products")
    fun getProducts(): Call<com.assan.finance.model.assan.ResponseModelAssanProducts>

    @GET("Ledger") // Replace with actual endpoint if different
    fun getLedgerData(
        @Query("officeId") officeId: String,
        @Query("borrowerNo") borrowerNo: String
    ): Call<com.assan.finance.model.assan.ResponseModelAssanLedger>

    @GET("Ledger/borrower-summary")
    fun getBorrowerSummary(
        @Query("borrowerNo") borrowerNo: String
    ): Call<com.assan.finance.model.assan.ResponseModelBorrowerSummary>

    @GET("Ledger/recovery-schedule")
    fun getRecoverySchedule(
        @Query("borrowerNo") borrowerNo: String,
        @Query("installmentNo") installmentNo: Int
    ): Call<com.assan.finance.model.assan.ResponseModelRecoverySchedule>

    @GET("Ledger/recovery-schedule")
    fun getFullRecoverySchedule(
        @Query("borrowerNo") borrowerNo: String
    ): Call<com.assan.finance.model.assan.ResponseModelRecoverySchedule>

    @GET("Ledger/by-cnic")
    fun getLedgerByCnic(
        @Query("cnic") cnic: String
    ): Call<com.assan.finance.model.assan.ResponseModelAssanLedger>

    @Headers("Content-Type: application/json")
    @POST("User/SaveDeviceToken")
    fun saveDeviceToken(
        @Body request: com.assan.finance.model.assan.TokenRequest
    ): Call<com.assan.finance.model.assan.ResponseModelLogin> // Reusing generic response pattern

    @Headers("Content-Type: application/json")
    @POST("User/MarkNotificationRead")
    fun markNotificationRead(
        @Body logId: Int
    ): Call<com.assan.finance.model.assan.ResponseModelLogin> // Reusing generic response pattern

    @Headers("Content-Type: application/json")
    @POST("User/DeleteAccount")
    fun deleteAccount(
        @Body request: com.assan.finance.model.assan.DeleteAccountRequest
    ): Call<com.assan.finance.model.assan.ResponseModelDeleteAccount>

    @GET("User/GetNewNotifications")
    fun getNotifications(
        @Query("borrowerId") borrowerId: String?,
        @Query("cnic") cnic: String?
    ): Call<com.assan.finance.model.assan.ResponseModelNotifications>

    @Multipart
    @POST("User/SubmitFeedback")
    fun submitFeedback(
        @Part borrowerId: okhttp3.MultipartBody.Part,
        @Part cnic: okhttp3.MultipartBody.Part,
        @Part title: okhttp3.MultipartBody.Part,
        @Part comments: okhttp3.MultipartBody.Part,
        @Part image: okhttp3.MultipartBody.Part?
    ): Call<com.assan.finance.model.assan.ResponseModelRegistration>
    @Multipart
    @POST("Ai/voice-chat")
    suspend fun processVoiceChat(@Part audio: okhttp3.MultipartBody.Part): retrofit2.Response<com.assan.finance.model.assan.VoiceChatResult>

    @POST("Ai/voice-chat")
    suspend fun processVoiceChatText(@Body body: okhttp3.RequestBody): retrofit2.Response<com.assan.finance.model.assan.VoiceChatResult>

    @Multipart
    @POST("Ai/gemini-ocr")
    suspend fun processGeminiOcr(
        @Part image: okhttp3.MultipartBody.Part,
        @Part("side") side: okhttp3.RequestBody
    ): retrofit2.Response<com.google.gson.JsonObject>

}

