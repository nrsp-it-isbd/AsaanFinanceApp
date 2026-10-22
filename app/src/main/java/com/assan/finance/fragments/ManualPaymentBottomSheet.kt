package com.assan.finance.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.assan.finance.R
import com.assan.finance.apis.ApiManager
import com.assan.finance.model.assan.PaymentChannel
import com.assan.finance.model.assan.RequestModelManualPayment
import com.assan.finance.model.assan.ResponseModelManualPayment
import com.assan.finance.apis.RetrofitClient
import com.assan.finance.network.assan.AssanApiService
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ManualPaymentBottomSheet(
    private val channel: PaymentChannel,
    private val borrowerNo: String,
    private val cnic: String
) : BottomSheetDialogFragment() {

    private lateinit var ivChannelLogo: ImageView
    private lateinit var tvChannelName: TextView
    private lateinit var btnClose: ImageView
    private lateinit var tvAccountNumber: TextView
    private lateinit var btnCopyAccount: ImageView
    private lateinit var btnOpenApp: MaterialButton
    private lateinit var etTransactionId: TextInputEditText
    private lateinit var etAmount: TextInputEditText
    private lateinit var btnSubmitPayment: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_manual_payment_bottom_sheet, container, false)
        
        ivChannelLogo = view.findViewById(R.id.ivChannelLogo)
        tvChannelName = view.findViewById(R.id.tvChannelName)
        btnClose = view.findViewById(R.id.btnClose)
        tvAccountNumber = view.findViewById(R.id.tvAccountNumber)
        btnCopyAccount = view.findViewById(R.id.btnCopyAccount)
        btnOpenApp = view.findViewById(R.id.btnOpenApp)
        etTransactionId = view.findViewById(R.id.etTransactionId)
        etAmount = view.findViewById(R.id.etAmount)
        btnSubmitPayment = view.findViewById(R.id.btnSubmitPayment)

        setupUI()
        setupListeners()
        
        return view
    }

    private fun setupUI() {
        tvChannelName.text = channel.name

        val logoUrl = if (channel.logoUrl != null) {
            var path = channel.logoUrl
            
            if (path.contains("/api/images/uploads/", ignoreCase = true)) {
                val type = channel.channelType?.lowercase() ?: ""
                val correctFolder = when {
                    type.contains("bank") -> "banks"
                    type.contains("agent") -> "agents"
                    type.contains("network") -> "payment_networks"
                    else -> "uploads" // fallback
                }
                path = path.replace("/api/images/uploads/", "/api/images/$correctFolder/", ignoreCase = true)
            }
            
            if (!path.startsWith("http", ignoreCase = true)) {
                if (path.startsWith("uploads/", ignoreCase = true)) {
                    path = path.replaceFirst("uploads/", "images/", ignoreCase = true)
                    path = path.replace("/channels/", "/", ignoreCase = true)
                }
                ApiManager.IMAGE_BASE_URL + path
            } else {
                path
            }
        } else ""

        val shimmer = com.facebook.shimmer.Shimmer.AlphaHighlightBuilder()
            .setDuration(1200)
            .setBaseAlpha(0.7f)
            .setHighlightAlpha(0.9f)
            .setDirection(com.facebook.shimmer.Shimmer.Direction.LEFT_TO_RIGHT)
            .setAutoStart(true)
            .build()
        val shimmerDrawable = com.facebook.shimmer.ShimmerDrawable().apply {
            setShimmer(shimmer)
        }

        Glide.with(requireContext())
            .load(logoUrl)
            .placeholder(shimmerDrawable)
            .error(android.R.drawable.ic_menu_report_image)
            .into(ivChannelLogo)

        val channelName = channel.name?.lowercase() ?: ""
        
        if (channelName.contains("easypaisa")) {
            btnOpenApp.text = getString(R.string.str_open_easypaisa_app)
        } else if (channelName.contains("jazzcash")) {
            btnOpenApp.text = getString(R.string.str_open_jazzcash_app)
        } else {
            btnOpenApp.text = getString(R.string.str_open_app_to_pay)
        }
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { dismiss() }

        btnCopyAccount.setOnClickListener {
            val accountNum = tvAccountNumber.text.toString()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("NRSP Account", accountNum)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), getString(R.string.str_account_number_copied), Toast.LENGTH_SHORT).show()
        }

        btnOpenApp.setOnClickListener {
            val channelName = channel.name?.lowercase() ?: ""
            val intent = if (channelName.contains("easypaisa")) {
                requireContext().packageManager.getLaunchIntentForPackage("pk.com.telenor.phoenix")
            } else if (channelName.contains("jazzcash")) {
                requireContext().packageManager.getLaunchIntentForPackage("com.mobilink.jazzcash.retailer")
            } else {
                null
            }

            if (intent != null) {
                startActivity(intent)
            } else {
                // Open play store if app not found or not mapped
                val fallbackIntent = Intent(Intent.ACTION_VIEW)
                if (channelName.contains("easypaisa")) {
                    fallbackIntent.data = Uri.parse("market://details?id=pk.com.telenor.phoenix")
                    try {
                        startActivity(fallbackIntent)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), getString(R.string.str_app_not_found), Toast.LENGTH_SHORT).show()
                    }
                } else if (channelName.contains("jazzcash")) {
                    fallbackIntent.data = Uri.parse("market://details?id=com.mobilink.jazzcash.retailer")
                    try {
                        startActivity(fallbackIntent)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), getString(R.string.str_app_not_found), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), getString(R.string.str_no_specific_app_mapped), Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnSubmitPayment.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.str_coming_soon), Toast.LENGTH_SHORT).show()
        }
    }

    private fun submitPaymentProof(tid: String, amount: Double) {
        btnSubmitPayment.isEnabled = false
        btnSubmitPayment.text = getString(R.string.str_submitting_feedback)

        val request = RequestModelManualPayment(
            borrowerNo = borrowerNo,
            cnic = cnic,
            channelId = channel.id,
            transactionId = tid,
            amount = amount
        )

        val retrofitClient = RetrofitClient(AssanApiService::class.java, requireContext())
        retrofitClient.apiService.submitManualPayment(request).enqueue(object : Callback<ResponseModelManualPayment> {
            override fun onResponse(
                call: Call<ResponseModelManualPayment>,
                response: Response<ResponseModelManualPayment>
            ) {
                btnSubmitPayment.isEnabled = true
                btnSubmitPayment.text = getString(R.string.str_submit_proof)

                if (response.isSuccessful) {
                    val msg = com.assan.finance.utils.CommonFunctions.getProfessionalErrorMessage(response.body()?.message) ?: "Failed to submit. Please try again."
                    if (response.body()?.success == true) {
                        Toast.makeText(requireContext(), getString(R.string.str_payment_proof_submitted), Toast.LENGTH_LONG).show()
                        dismiss()
                    } else {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseModelManualPayment>, t: Throwable) {
                btnSubmitPayment.isEnabled = true
                btnSubmitPayment.text = getString(R.string.str_submit_proof)
                com.assan.finance.utils.CommonFunctions.handleApiFailure(requireContext(), null, t)
            }
        })
    }
}
