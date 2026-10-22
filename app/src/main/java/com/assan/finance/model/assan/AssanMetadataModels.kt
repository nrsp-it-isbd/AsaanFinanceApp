package com.assan.finance.model.assan

import com.google.gson.annotations.SerializedName
import com.google.gson.JsonElement

data class ResponseModelAssanDistricts(
    @SerializedName("districts") val districts: List<AssanDistrict>? = null
)

data class AssanDistrict(
    @SerializedName("DISTRICT_ID") val districtId: String? = null,
    @SerializedName(value = "DISTRICT", alternate = ["DISTRICT_NAME"]) val districtName: String? = null
)

data class ResponseModelAssanTehsils(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("tehsils") val tehsils: List<AssanTehsil>? = null
)

data class AssanTehsil(
    @SerializedName("TEHSIL_ID") val tehsilId: String? = null,
    @SerializedName("TEHSIL") val tehsilName: String? = null,
    @SerializedName("FIELD_UNIT_ID") val fieldUnitId: String? = null,
    @SerializedName("REGION_ID") val regionId: String? = null
)

data class ResponseModelAssanBanks(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName(value = "records", alternate = ["banks"]) val records: List<AssanBank>? = null
)

data class AssanBank(
    @SerializedName("BANK_ID") val bankIdEle: JsonElement? = null,
    @SerializedName("BANK") val bankNameEle: JsonElement? = null,
    @SerializedName("SHRT_NME") val shortNameEle: JsonElement? = null,
    @SerializedName("AC_NO_FORMAT") val acNoFormatEle: JsonElement? = null,
    @SerializedName("ACC_DIGITS") val accDigitsEle: JsonElement? = null,
    @SerializedName("BANK_CODE") val bankCodeEle: JsonElement? = null,
    @SerializedName("MCB_BANK_CODE") val mcbBankCodeEle: JsonElement? = null,
    @SerializedName("AC_DIGIT_BKP") val acDigitBkpEle: JsonElement? = null,
    @SerializedName("MEDP_STATUS") val medpStatusEle: JsonElement? = null
) {
    fun getBankId(): Int? = if (bankIdEle != null && bankIdEle.isJsonPrimitive) bankIdEle.asInt else null
    fun getBankName(): String? = if (bankNameEle != null && bankNameEle.isJsonPrimitive) bankNameEle.asString else null
    fun getShortName(): String? = if (shortNameEle != null && shortNameEle.isJsonPrimitive) shortNameEle.asString else null
    fun getAcNoFormat(): String? = if (acNoFormatEle != null && acNoFormatEle.isJsonPrimitive) acNoFormatEle.asString else null
    fun getAccDigits(): String? = if (accDigitsEle != null && accDigitsEle.isJsonPrimitive) accDigitsEle.asString else null
    fun getBankCode(): String? = if (bankCodeEle != null && bankCodeEle.isJsonPrimitive) bankCodeEle.asString else null
    fun getMcbBankCode(): String? = if (mcbBankCodeEle != null && mcbBankCodeEle.isJsonPrimitive) mcbBankCodeEle.asString else null
    fun getAcDigitBkp(): Int? = if (acDigitBkpEle != null && acDigitBkpEle.isJsonPrimitive) acDigitBkpEle.asInt else null
    /** Returns true if this bank is active (MEDP_STATUS == 1) */
    fun isActive(): Boolean = medpStatusEle != null && medpStatusEle.isJsonPrimitive && medpStatusEle.asInt == 1
}


data class ResponseModelAssanProducts(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("records") val records: List<com.assan.finance.dataModels.DataModelLoanData>? = null
)
