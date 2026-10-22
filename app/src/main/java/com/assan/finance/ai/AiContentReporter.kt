package com.assan.finance.ai

import android.app.Activity
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.assan.finance.R
import com.assan.finance.model.assan.ResponseModelRegistration
import com.assan.finance.network.assan.AssanRetrofitClient
import com.assan.finance.sharedPreferences.SharedManager
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Lets users report an AI assistant answer (Google Play AI-generated content policy requires
 * in-app reporting). Reports go to the existing User/SubmitFeedback API with an "AI Report"
 * title, so NRSP sees them with the other feedback.
 */
object AiContentReporter {

    const val SOURCE_HOME = "Asaan Assistant (Home)"
    const val SOURCE_FORM = "Financing Form Assistant"

    private const val MAX_ANSWER_CHARS = 1500

    @JvmStatic
    fun showReportDialog(activity: Activity, source: String, answer: String?) {
        if (activity.isFinishing) return
        val text = answer?.trim().orEmpty()
        if (text.isEmpty()) {
            Toast.makeText(activity, R.string.ai_report_nothing, Toast.LENGTH_SHORT).show()
            return
        }

        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_ai_report, null)
        val rgReason = view.findViewById<RadioGroup>(R.id.rgAiReportReason)
        val etComment = view.findViewById<EditText>(R.id.etAiReportComment)

        AlertDialog.Builder(activity)
            .setTitle(R.string.ai_report_title)
            .setView(view)
            .setNegativeButton(R.string.ai_report_cancel, null)
            .setPositiveButton(R.string.ai_report_send) { _, _ ->
                val reason = when (rgReason.checkedRadioButtonId) {
                    R.id.rbAiReportOffensive -> "Offensive or harmful"
                    R.id.rbAiReportOther -> "Other problem"
                    else -> "Wrong or misleading information"
                }
                submit(activity, source, reason, text, etComment.text.toString().trim())
            }
            .show()
    }

    private fun submit(activity: Activity, source: String, reason: String, answer: String, comment: String) {
        val prefs = SharedManager.getInstance(activity)
        val cnic = prefs.getString("CNIC", "") ?: ""
        val borrowerId = (prefs.getString("BORROWER_NO", "") ?: "").ifEmpty { "0000-000000" }
        val comments = buildString {
            append("Assistant: ").append(source).append('\n')
            append("Reason: ").append(reason).append('\n')
            append("Answer: ").append(answer.take(MAX_ANSWER_CHARS))
            if (comment.isNotEmpty()) append('\n').append("User comment: ").append(comment)
        }
        val appContext = activity.applicationContext
        try {
            AssanRetrofitClient.apiService.submitFeedback(
                MultipartBody.Part.createFormData("BorrowerId", borrowerId),
                MultipartBody.Part.createFormData("Cnic", cnic),
                MultipartBody.Part.createFormData("Title", "AI Report - $reason"),
                MultipartBody.Part.createFormData("Comments", comments),
                null
            ).enqueue(object : Callback<ResponseModelRegistration> {
                override fun onResponse(call: Call<ResponseModelRegistration>, response: Response<ResponseModelRegistration>) {
                    val ok = response.isSuccessful && response.body()?.success == true
                    Toast.makeText(appContext, if (ok) R.string.ai_report_sent else R.string.ai_report_failed, Toast.LENGTH_LONG).show()
                }

                override fun onFailure(call: Call<ResponseModelRegistration>, t: Throwable) {
                    Toast.makeText(appContext, R.string.ai_report_failed, Toast.LENGTH_LONG).show()
                }
            })
        } catch (e: Exception) {
            Toast.makeText(appContext, R.string.ai_report_failed, Toast.LENGTH_LONG).show()
        }
    }
}
