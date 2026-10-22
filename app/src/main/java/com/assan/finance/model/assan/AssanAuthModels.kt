package com.assan.finance.model.assan

import com.google.gson.annotations.SerializedName

// --- Requests ---

data class LoginRequest(
    @SerializedName(value = "username", alternate = ["USERNAME"]) val username: String,
    @SerializedName(value = "password", alternate = ["PASSWORD"]) val password: String
)

// --- Responses ---

data class ResponseModelLogin(
    @SerializedName("success_bool") val successBool: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: UserData?,
    @SerializedName("success") val successString: String?,
    @SerializedName("error") val error: String?
)

data class UserData(
    @SerializedName(value = "id", alternate = ["CLIENT_ID"]) val clientId: String?,
    @SerializedName(value = "fulL_NAME", alternate = ["NAME", "name"]) val name: String?,
    @SerializedName(value = "username", alternate = ["USER_NAME"]) val userName: String?,
    @SerializedName(value = "emaiL_ADDRESS", alternate = ["EMAIL", "email"]) val emailAddress: String?,
    @SerializedName(value = "rolE_ID", alternate = ["ROLE_ID"]) val roleId: String?,
    @SerializedName(value = "regioN_ID", alternate = ["REGION_ID"]) val regionId: String?,
    @SerializedName(value = "regioN_NAME", alternate = ["REGION_NAME"]) val regionName: String?,
    @SerializedName(value = "fielD_UNIT_ID", alternate = ["FIELD_UNIT_ID"]) val fieldUnitId: String?,
    @SerializedName(value = "fielD_UNIT_NAME", alternate = ["FIELD_UNIT_NAME"]) val fieldUnitName: String?,
    @SerializedName(value = "status", alternate = ["STATUS"]) val status: String?,
    @SerializedName(value = "cniC_NUMBER", alternate = ["CNIC", "cnic"]) val cnic: String?,
    @SerializedName(value = "datE_OF_BIRTH", alternate = ["DATE_OF_BIRTH"]) val dateOfBirth: String?,
    @SerializedName(value = "cniC_EXPIRY_DATE", alternate = ["CNIC_EXPIRY"]) val cnicExpiry: String?,
    @SerializedName(value = "cniC_ISSUE_DATE", alternate = ["CNIC_ISSUE_DATE"]) val cnicIssueDate: String?,
    @SerializedName(value = "phonE_NUMBER", alternate = ["PHONE", "phone"]) val phone: String?,
    @SerializedName(value = "gender", alternate = ["GENDER"]) val gender: String?,
    @SerializedName(value = "profilE_PICTURE", alternate = ["PROFILE_PICTURE", "profile_picture", "profilePicture", "PROFILE_IMAGE", "profile_image", "ProfilePicture"]) val profilePicture: String?,
    @SerializedName(value = "borrowerNo", alternate = ["BORROWER_NO"]) val borrowerNo: String?,
    @SerializedName(value = "devicE_ID", alternate = ["DEVICE_ID"]) val deviceId: String?
)

data class ResponseModelRegistration(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?
)

// User/DeleteAccount (backend_fix/AccountDeletionController.cs)
data class DeleteAccountRequest(
    @SerializedName("cnic") val cnic: String,
    @SerializedName("password") val password: String,
    @SerializedName("reason") val reason: String?
)

data class ResponseModelDeleteAccount(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String? = null,
    // Machine-readable failure: INVALID_CREDENTIALS, NOT_CONFIGURED, ...
    @SerializedName("error") val error: String? = null,
    @SerializedName("data") val data: DeleteAccountResult? = null
)

data class DeleteAccountResult(
    // "deleted" = everything removed, "records_kept" = financing records kept by law
    @SerializedName("mode") val mode: String? = null
)

data class TokenRequest(
    @SerializedName(value = "BorrowerId", alternate = ["borrowerId", "borrowerNo"]) val borrowerId: String,
    @SerializedName(value = "FcmToken", alternate = ["fcmToken", "deviceToken"]) val fcmToken: String,
    @SerializedName(value = "CNIC", alternate = ["cnic"]) val cnic: String? = null
)

data class NotificationDataModel(
    @SerializedName("logId") val logId: Int,
    @SerializedName(value = "title", alternate = ["Title", "NOTIFICATION_TITLE", "notification_title"]) val title: String? = "Notification",
    @SerializedName(value = "message", alternate = ["Message"]) val message: String,
    @SerializedName(value = "date", alternate = ["createdAt", "CreatedAt"]) val date: String,
    @SerializedName(value = "category", alternate = ["Category"]) val category: String? = "General",
    @SerializedName(value = "isRead", alternate = ["IsRead"]) var isRead: Boolean
)

data class ResponseModelNotifications(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<NotificationDataModel>?
)

data class NotificationPayload(
    @SerializedName("logId") val logId: Long,
    @SerializedName("borrowerId") val borrowerId: String?,
    @SerializedName("cnic") val cnic: String?,
    @SerializedName("message") val message: String,
    @SerializedName("createdAt") val createdAt: String
)

data class ForgotPasswordRequest(
    @SerializedName(value = "cniC_OR_PHONE", alternate = ["CNIC_OR_PHONE", "cnic_or_phone", "cnicOrPhone"]) val cnicOrPhone: String,
    @SerializedName(value = "neW_PASSWORD", alternate = ["NEW_PASSWORD", "new_password", "newPassword"]) val newPassword: String
)

data class ForgotPasswordData(
    @SerializedName(value = "username", alternate = ["USER_NAME", "userName"]) val username: String? = null,
    @SerializedName(value = "name", alternate = ["NAME", "fulL_NAME"]) val name: String? = null,
    @SerializedName(value = "phone_number", alternate = ["PHONE", "phone", "phonE_NUMBER"]) val phoneNumber: String? = null,
    @SerializedName(value = "cnic_number", alternate = ["CNIC", "cnic", "cniC_NUMBER"]) val cnicNumber: String? = null
)

data class ResponseModelForgotPassword(
    @SerializedName("success_bool") val successBool: Boolean? = null,
    @SerializedName("success") val successString: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: ForgotPasswordData? = null,
    @SerializedName("error") val error: String? = null
)



