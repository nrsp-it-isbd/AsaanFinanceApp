package com.assan.finance.model.assan

import com.google.gson.annotations.SerializedName

data class ResponseModelAssanLedger(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("borrowerNo") val borrowerNo: String? = null,
    @SerializedName("officeId") val officeId: String? = null,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("records") val records: List<AssanLedgerRecord>? = null
)

data class AssanLedgerRecord(
    @SerializedName("CO_NAME") val coName: String? = null,
    @SerializedName("COTYPE") val coType: String? = null,
    @SerializedName("REFUND_CHEQUE") val refundCheque: String? = null,
    @SerializedName("REFUND_DATE") val refundDate: String? = null,
    @SerializedName("REFUND_AMOUNT") val refundAmount: String? = null,
    @SerializedName("NAME") val name: String? = null,
    @SerializedName("F_H_NAME") val fhName: String? = null,
    @SerializedName("SEX") val sex: String? = null,
    @SerializedName("CNIC") val cnic: String? = null,
    @SerializedName("CELL") val cell: String? = null,
    @SerializedName("BORROWER_NO") val borrowerNo: String? = null,
    @SerializedName("TOTAL_INST") val totalInst: Int = 0,
    @SerializedName("AMOUNT") val amount: Double = 0.0,
    @SerializedName("RATE") val rate: Double = 0.0,
    @SerializedName("DISBRDATE") val disbrDate: String? = null,
    @SerializedName("SERVICE_CHARGE") val serviceCharge: Double = 0.0,
    @SerializedName("CHEQUENO") val chequeNo: String? = null,
    @SerializedName("SANCTIONNO") val sanctionNo: String? = null,
    @SerializedName("OLD_SAN_NO") val oldSanNo: String? = null,
    @SerializedName("CHEQUE_DATE") val chequeDate: String? = null,
    @SerializedName("WRITEOFF_AMOUNT") val writeoffAmount: Double = 0.0,
    @SerializedName("DESCRIPTION") val description: String? = null,
    @SerializedName("PRODUCT_NAME") val productName: String? = null,
    @SerializedName("DUEDATE") val dueDate: String? = null,
    @SerializedName("DUEPA") val duePa: Double = 0.0,
    @SerializedName("DUESC") val dueSc: Double = 0.0,
    @SerializedName("DUETOTAL") val dueTotal: Double = 0.0,
    @SerializedName("INSTALLMENT_NO") val installmentNo: Int = 0,
    @SerializedName("RECEIPTNO") val receiptNo: String? = null,
    @SerializedName("RECEIPT_DATE") val receiptDate: String? = null,
    @SerializedName("REC_AMOUNT") val recAmount: Double = 0.0,
    @SerializedName("REC_SC") val recSc: Double = 0.0,
    @SerializedName("RECPA") val recPa: Double = 0.0,
    @SerializedName("DISPLAY_NAME") val displayName: String? = null,
    @SerializedName("DURATION") val duration: Int = 0,
    @SerializedName("SC_AMOUNT") val scAmount: Double = 0.0,
    @SerializedName("DUEPEN") val duePen: Double = 0.0,
    @SerializedName("ADJ") val adj: Double = 0.0,
    @SerializedName("ODPA") val odPa: Double = 0.0,
    @SerializedName("ODSC") val odSc: Double = 0.0,
    @SerializedName("CLIENT_ACCOUNT") val clientAccount: String? = null,
    @SerializedName("LOAN_STATUS") val loanStatus: String? = null
)

data class BorrowerSummaryData(
    @SerializedName("BORROWER_NO") val borrowerNo: String? = null,
    @SerializedName("BORROWER_NAME") val borrowerName: String? = null,
    @SerializedName("FATHER_NAME") val fatherName: String? = null,
    @SerializedName("CNIC") val cnic: String? = null,
    @SerializedName("CELL") val cell: String? = null,
    @SerializedName("SEX") val sex: String? = null,
    @SerializedName("CO_NAME") val coName: String? = null,
    @SerializedName("COTYPE") val coType: String? = null,
    @SerializedName("LOAN_AMOUNT") val loanAmount: Double = 0.0,
    @SerializedName("TOTAL_INSTALLMENTS") val totalInstallments: Int = 0,
    @SerializedName("INTEREST_RATE") val interestRate: Double = 0.0,
    @SerializedName("SERVICE_CHARGE") val serviceCharge: Double = 0.0,
    @SerializedName("DISBURSEMENT_DATE") val disbursementDate: String? = null,
    @SerializedName("LOAN_DURATION") val loanDuration: Int = 0,
    @SerializedName("SANCTION_NUMBER") val sanctionNumber: String? = null,
    @SerializedName("CHEQUE_NUMBER") val chequeNumber: String? = null,
    @SerializedName("CHEQUE_DATE") val chequeDate: String? = null,
    @SerializedName("CLIENT_ACCOUNT") val clientAccount: String? = null,
    @SerializedName("PRODUCT_NAME") val productName: String? = null,
    @SerializedName("FREQUENCY") val frequency: String? = null,
    @SerializedName("WRITEOFF_AMOUNT") val writeoffAmount: Double = 0.0,
    @SerializedName("PROJECT_NAME") val projectName: String? = null,
    @SerializedName("TOTAL_RECOVERED") val totalRecovered: Double? = 0.0,
    @SerializedName("TOTAL_SC_RECOVERED") val totalScRecovered: Double? = 0.0,
    @SerializedName("TOTAL_RECOVERED_AMOUNT") val totalRecoveredAmount: Double? = 0.0
)

data class ResponseModelBorrowerSummary(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("borrowerNo") val borrowerNo: String? = null,
    @SerializedName("data") val data: BorrowerSummaryData? = null
)

data class RecoveryScheduleRecord(
    @SerializedName("INSTALLMENT_NO") val installmentNo: Int = 0,
    @SerializedName("DUEDATE") val dueDate: String? = null,
    @SerializedName("PRINCIPAL_DUE") val principalDue: Double = 0.0,
    @SerializedName("SERVICE_CHARGE_DUE") val serviceChargeDue: Double = 0.0,
    @SerializedName("TOTAL_DUE") val totalDue: Double = 0.0,
    @SerializedName("PENALTY_DUE") val penaltyDue: Double = 0.0,
    @SerializedName("OUTSTANDING_PRINCIPAL") val outstandingPrincipal: Double = 0.0,
    @SerializedName("OUTSTANDING_SERVICE_CHARGE") val outstandingServiceCharge: Double = 0.0,
    @SerializedName("RECOVERED_PRINCIPAL") val recoveredPrincipal: Double = 0.0,
    @SerializedName("RECOVERED_SERVICE_CHARGE") val recoveredServiceCharge: Double = 0.0,
    @SerializedName("EXCESS_RECOVERY") val excessRecovery: Double = 0.0,
    @SerializedName("RECEIPTNO") val receiptNo: String? = null,
    @SerializedName("RECEIPT_DATE") val receiptDate: String? = null,
    @SerializedName("STATUS") val status: String? = null
)

data class ResponseModelRecoverySchedule(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("borrowerNo") val borrowerNo: String? = null,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("records") val records: List<RecoveryScheduleRecord>? = null
)

data class PaymentChannel(
    @SerializedName("ID") val id: Int = 0,
    @SerializedName("NAME") val name: String? = null,
    @SerializedName("CHANNEL_TYPE") val channelType: String? = null,
    @SerializedName("LOGO_URL") val logoUrl: String? = null,
    @SerializedName("IS_ACTIVE") val isActive: Int = 1,
    @SerializedName("CREATED_AT") val createdAt: String? = null,
    @SerializedName("UPDATED_AT") val updatedAt: String? = null
)

data class ResponseModelPaymentChannels(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("records") val records: List<PaymentChannel>? = null
)


data class RequestModelManualPayment(
    @SerializedName("borrowerNo") val borrowerNo: String,
    @SerializedName("cnic") val cnic: String,
    @SerializedName("channelId") val channelId: Int,
    @SerializedName("transactionId") val transactionId: String,
    @SerializedName("amount") val amount: Double
)

data class ResponseModelManualPayment(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String? = null
)
