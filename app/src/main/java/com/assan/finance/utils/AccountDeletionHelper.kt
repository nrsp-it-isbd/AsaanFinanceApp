package com.assan.finance.utils

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.widget.doAfterTextChanged
import com.assan.finance.R
import com.assan.finance.model.assan.DeleteAccountRequest
import com.assan.finance.model.assan.ResponseModelDeleteAccount
import com.assan.finance.model.assan.ResponseModelRegistration
import com.assan.finance.network.assan.AssanRetrofitClient
import com.assan.finance.sharedPreferences.SharedManager
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * In-app account deletion (Google Play requires apps with sign-up to offer it).
 *
 * Uses POST User/DeleteAccount (CNIC + password; backend_fix/AccountDeletionController.cs).
 * Until that API is live on the server (HTTP 404, or error NOT_CONFIGURED), the request is sent
 * to NRSP through User/SubmitFeedback with the title "Account Deletion Request" instead, and
 * NRSP staff delete the account by hand.
 */
object AccountDeletionHelper {

    const val REQUEST_TITLE = "Account Deletion Request"

    private const val MODE_RECORDS_KEPT = "records_kept"
    private const val ERROR_INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
    private const val ERROR_NOT_CONFIGURED = "NOT_CONFIGURED"

    private sealed class ApiOutcome {
        class Deleted(val mode: String?) : ApiOutcome()
        object WrongPassword : ApiOutcome()
        object NotAvailable : ApiOutcome()   // API not deployed / not configured yet -> old way
        object Failed : ApiOutcome()
    }

    @JvmStatic
    fun showDeleteAccountDialog(activity: Activity) {
        if (activity.isFinishing) return
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_delete_account, null)
        val dialog = Dialog(activity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(view)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        val layoutForm = view.findViewById<View>(R.id.layoutDelAccForm)
        val layoutDone = view.findViewById<View>(R.id.layoutDelAccDone)
        val etPassword = view.findViewById<EditText>(R.id.etDelAccPassword)
        val btnToggle = view.findViewById<ImageView>(R.id.btnDelAccTogglePassword)
        val tvPasswordError = view.findViewById<TextView>(R.id.tvDelAccPasswordError)
        val etReason = view.findViewById<EditText>(R.id.etDelAccReason)
        val cbConfirm = view.findViewById<CheckBox>(R.id.cbDelAccConfirm)
        val tvError = view.findViewById<TextView>(R.id.tvDelAccError)
        val btnCancel = view.findViewById<TextView>(R.id.btnDelAccCancel)
        val btnDelete = view.findViewById<TextView>(R.id.btnDelAccConfirm)

        fun setDeleteEnabled(enabled: Boolean) {
            btnDelete.isEnabled = enabled
            btnDelete.alpha = if (enabled) 1f else 0.5f
        }

        fun setBusy(busy: Boolean) {
            dialog.setCancelable(!busy)
            btnCancel.isEnabled = !busy
            btnDelete.setText(if (busy) R.string.del_acc_deleting else R.string.del_acc_confirm)
            setDeleteEnabled(!busy && cbConfirm.isChecked)
        }

        fun showError(field: TextView, @StringRes message: Int) {
            field.setText(message)
            field.visibility = View.VISIBLE
        }

        fun showDone(@StringRes title: Int, @StringRes message: Int) {
            dialog.setCancelable(false)
            layoutForm.visibility = View.GONE
            layoutDone.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.tvDelAccDoneTitle).setText(title)
            view.findViewById<TextView>(R.id.tvDelAccDoneMessage).setText(message)
            view.findViewById<View>(R.id.btnDelAccDoneOk).setOnClickListener {
                dialog.dismiss()
                CommonFunctions.performLogout(activity)
            }
        }

        // Show / hide password, same as the login screen
        var passwordVisible = false
        btnToggle.setOnClickListener {
            passwordVisible = !passwordVisible
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or
                if (passwordVisible) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_TEXT_VARIATION_PASSWORD
            btnToggle.setImageResource(if (passwordVisible) R.drawable.ic_eye_show else R.drawable.ic_eye_hide)
            etPassword.setSelection(etPassword.text.length)
        }
        etPassword.doAfterTextChanged { tvPasswordError.visibility = View.GONE }

        setDeleteEnabled(false)
        cbConfirm.setOnCheckedChangeListener { _, checked -> setDeleteEnabled(checked) }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnDelete.setOnClickListener {
            tvError.visibility = View.GONE
            val password = etPassword.text.toString()
            if (password.isEmpty()) {
                showError(tvPasswordError, R.string.del_acc_password_required)
                etPassword.requestFocus()
                return@setOnClickListener
            }
            val reason = etReason.text.toString().trim()
            setBusy(true)

            deleteViaApi(activity, password, reason) { outcome ->
                if (activity.isFinishing || !dialog.isShowing) return@deleteViaApi
                when (outcome) {
                    is ApiOutcome.Deleted -> showDone(
                        R.string.del_acc_deleted_title,
                        if (outcome.mode == MODE_RECORDS_KEPT) R.string.del_acc_kept_message else R.string.del_acc_deleted_message
                    )
                    ApiOutcome.WrongPassword -> {
                        setBusy(false)
                        showError(tvPasswordError, R.string.del_acc_wrong_password)
                        etPassword.requestFocus()
                    }
                    ApiOutcome.NotAvailable -> sendRequestViaFeedback(activity, reason) { ok ->
                        if (activity.isFinishing || !dialog.isShowing) return@sendRequestViaFeedback
                        if (ok) {
                            showDone(R.string.del_acc_done_title, R.string.del_acc_done_message)
                        } else {
                            setBusy(false)
                            showError(tvError, R.string.del_acc_failed)
                        }
                    }
                    ApiOutcome.Failed -> {
                        setBusy(false)
                        showError(tvError, R.string.del_acc_failed)
                    }
                }
            }
        }

        dialog.show()
    }

    /** POST User/DeleteAccount: the server checks the password and deletes the account itself. */
    private fun deleteViaApi(activity: Activity, password: String, reason: String, onResult: (ApiOutcome) -> Unit) {
        val cnic = SharedManager.getInstance(activity).getString("CNIC", "") ?: ""
        try {
            AssanRetrofitClient.apiService.deleteAccount(
                DeleteAccountRequest(cnic = cnic, password = password, reason = reason.ifEmpty { null })
            ).enqueue(object : Callback<ResponseModelDeleteAccount> {
                override fun onResponse(call: Call<ResponseModelDeleteAccount>, response: Response<ResponseModelDeleteAccount>) {
                    val body = response.body()
                    onResult(
                        when {
                            // Older server without the endpoint
                            response.code() == 404 -> ApiOutcome.NotAvailable
                            !response.isSuccessful || body == null -> ApiOutcome.Failed
                            body.success -> ApiOutcome.Deleted(body.data?.mode)
                            body.error == ERROR_INVALID_CREDENTIALS -> ApiOutcome.WrongPassword
                            body.error == ERROR_NOT_CONFIGURED -> ApiOutcome.NotAvailable
                            else -> ApiOutcome.Failed
                        }
                    )
                }

                override fun onFailure(call: Call<ResponseModelDeleteAccount>, t: Throwable) {
                    onResult(ApiOutcome.Failed)
                }
            })
        } catch (e: Exception) {
            onResult(ApiOutcome.Failed)
        }
    }

    /** Fallback: deletion request through User/SubmitFeedback, handled by NRSP staff. */
    private fun sendRequestViaFeedback(activity: Activity, reason: String, onResult: (Boolean) -> Unit) {
        val prefs = SharedManager.getInstance(activity)
        val cnic = prefs.getString("CNIC", "") ?: ""
        val borrowerId = (prefs.getString("BORROWER_NO", "") ?: "").ifEmpty { "0000-000000" }
        val phone = prefs.getString("PHONE", "") ?: ""
        val comments = buildString {
            append("The user asked to delete their Asaan Finance account from the app.\n")
            append("CNIC: ").append(cnic).append('\n')
            append("Phone: ").append(phone)
            if (reason.isNotEmpty()) append('\n').append("Reason: ").append(reason)
        }
        try {
            AssanRetrofitClient.apiService.submitFeedback(
                MultipartBody.Part.createFormData("BorrowerId", borrowerId),
                MultipartBody.Part.createFormData("Cnic", cnic),
                MultipartBody.Part.createFormData("Title", REQUEST_TITLE),
                MultipartBody.Part.createFormData("Comments", comments),
                null
            ).enqueue(object : Callback<ResponseModelRegistration> {
                override fun onResponse(call: Call<ResponseModelRegistration>, response: Response<ResponseModelRegistration>) {
                    onResult(response.isSuccessful && response.body()?.success == true)
                }

                override fun onFailure(call: Call<ResponseModelRegistration>, t: Throwable) {
                    onResult(false)
                }
            })
        } catch (e: Exception) {
            onResult(false)
        }
    }
}
