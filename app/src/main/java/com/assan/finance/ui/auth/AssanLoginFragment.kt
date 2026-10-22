package com.assan.finance.ui.auth

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.assan.finance.R
import com.assan.finance.databinding.FragmentAssanLoginBinding
import com.assan.finance.model.assan.ForgotPasswordRequest
import com.assan.finance.model.assan.LoginRequest
import com.assan.finance.model.assan.ResponseModelForgotPassword
import com.assan.finance.model.assan.ResponseModelLogin
import com.assan.finance.network.assan.AssanRetrofitClient
import com.assan.finance.sharedPreferences.SharedManager
import com.assan.finance.utils.CommonFunctions
import com.assan.finance.utils.Constants
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@android.annotation.SuppressLint("InflateParams")
class AssanLoginFragment : Fragment() {

    private var _binding: FragmentAssanLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssanLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Entrance animation for form content only
        binding.llLoginForm.alpha = 0f
        binding.llLoginForm.translationY = 200f
        binding.llLoginForm.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(100)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
        
        CommonFunctions.addCNICFormatting(binding.etUsername)
        
        var isPasswordVisible = false
        binding.btnToggleVisibility.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnToggleVisibility.setImageResource(R.drawable.ic_eye_show)
            } else {
                binding.etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.btnToggleVisibility.setImageResource(R.drawable.ic_eye_hide)
            }
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }

        CommonFunctions.addCNICFormatting(binding.etUsername)

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), R.string.auto_please_enter_username_and_641, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (username.length != 15) {
                Toast.makeText(requireContext(), R.string.auto_please_enter_a_valid_15_d_642, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Remove dashes for API if backend expects 13 digits, or send as is based on backend
            // Let's remove dashes so it's a 13-digit string for the backend username
            val cleanUsername = username.replace("-", "")
            performLogin(cleanUsername, password)
        }

        binding.tvSignup.setOnClickListener {
            findNavController().navigate(R.id.action_assanLogin_to_assanSignup)
        }
        
        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun showForgotPasswordDialog() {
        val context = context ?: return
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(context, R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        dialog.setContentView(view)
        
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        
        dialog.setOnShowListener {
            val bottomSheetDialog = it as com.google.android.material.bottomsheet.BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? android.widget.FrameLayout
            bottomSheet?.let { sheet ->
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet)
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        val etFpCnicOrPhone = view.findViewById<EditText>(R.id.etFpCnicOrPhone)
        val etFpNewPassword = view.findViewById<EditText>(R.id.etFpNewPassword)
        val etFpConfirmPassword = view.findViewById<EditText>(R.id.etFpConfirmPassword)
        val btnToggleNewPassword = view.findViewById<ImageView>(R.id.btnToggleNewPassword)
        val btnToggleConfirmPassword = view.findViewById<ImageView>(R.id.btnToggleConfirmPassword)
        val btnResetPassword = view.findViewById<MaterialButton>(R.id.btnResetPassword)
        val btnClose = view.findViewById<View>(R.id.btnClose)

        var isNewPassVisible = false
        btnToggleNewPassword.setOnClickListener {
            isNewPassVisible = !isNewPassVisible
            if (isNewPassVisible) {
                etFpNewPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnToggleNewPassword.setImageResource(R.drawable.ic_eye_show)
            } else {
                etFpNewPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnToggleNewPassword.setImageResource(R.drawable.ic_eye_hide)
            }
            etFpNewPassword.setSelection(etFpNewPassword.text?.length ?: 0)
        }

        var isConfirmPassVisible = false
        btnToggleConfirmPassword.setOnClickListener {
            isConfirmPassVisible = !isConfirmPassVisible
            if (isConfirmPassVisible) {
                etFpConfirmPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnToggleConfirmPassword.setImageResource(R.drawable.ic_eye_show)
            } else {
                etFpConfirmPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnToggleConfirmPassword.setImageResource(R.drawable.ic_eye_hide)
            }
            etFpConfirmPassword.setSelection(etFpConfirmPassword.text?.length ?: 0)
        }

        btnResetPassword.setOnClickListener {
            val cnicOrPhoneRaw = etFpCnicOrPhone.text.toString().trim()
            val cleanCnicOrPhone = cnicOrPhoneRaw.replace("-", "").replace(" ", "")
            val newPassword = etFpNewPassword.text.toString().trim()
            val confirmPassword = etFpConfirmPassword.text.toString().trim()

            if (cleanCnicOrPhone.isEmpty()) {
                etFpCnicOrPhone.error = "Enter CNIC or Phone Number"
                etFpCnicOrPhone.requestFocus()
                Toast.makeText(context, "Please enter your CNIC or Phone Number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (cleanCnicOrPhone.length < 11) {
                etFpCnicOrPhone.error = "Enter valid CNIC or Phone Number"
                etFpCnicOrPhone.requestFocus()
                Toast.makeText(context, "Please enter a valid CNIC or Phone Number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.isEmpty()) {
                etFpNewPassword.error = "Enter new password"
                etFpNewPassword.requestFocus()
                Toast.makeText(context, "Please enter a new password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.length < 4) {
                etFpNewPassword.error = "Password must be at least 4 characters"
                etFpNewPassword.requestFocus()
                Toast.makeText(context, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (confirmPassword.isEmpty()) {
                etFpConfirmPassword.error = "Confirm your password"
                etFpConfirmPassword.requestFocus()
                Toast.makeText(context, "Please confirm your new password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                etFpConfirmPassword.error = "Passwords do not match"
                etFpConfirmPassword.requestFocus()
                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CommonFunctions.showButtonLoader(btnResetPassword, "Resetting...")

            val request = ForgotPasswordRequest(
                cnicOrPhone = cleanCnicOrPhone,
                newPassword = newPassword
            )

            AssanRetrofitClient.apiService.forgotPassword(request).enqueue(object : Callback<ResponseModelForgotPassword> {
                override fun onResponse(call: Call<ResponseModelForgotPassword>, response: Response<ResponseModelForgotPassword>) {
                    if (!isAdded) return
                    CommonFunctions.hideButtonLoader(btnResetPassword)

                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        val isSuccess = body.successBool == true || body.successString == "1" || body.successString?.lowercase() == "true"

                        if (isSuccess) {
                            dialog.dismiss()
                            val msg = body.message ?: "Password reset successfully."
                            CommonFunctions.showSuccessDialog(requireActivity(), msg) {
                                val userIdentifier = body.data?.cnicNumber ?: body.data?.username ?: cleanCnicOrPhone
                                if (userIdentifier.length == 13) {
                                    val formatted = "${userIdentifier.substring(0, 5)}-${userIdentifier.substring(5, 12)}-${userIdentifier.substring(12, 13)}"
                                    binding.etUsername.setText(formatted)
                                } else {
                                    binding.etUsername.setText(userIdentifier)
                                }
                                binding.etPassword.setText(newPassword)
                            }
                        } else {
                            val errorMsg = body.message ?: "Failed to reset password. Please check your credentials."
                            showLoginErrorDialog(errorMsg)
                        }
                    } else {
                        showLoginErrorDialog(getString(R.string.str_unable_to_process_request))
                    }
                }

                override fun onFailure(call: Call<ResponseModelForgotPassword>, t: Throwable) {
                    if (!isAdded) return
                    CommonFunctions.hideButtonLoader(btnResetPassword)
                    CommonFunctions.handleApiFailure(requireContext(), binding.root, t)
                }
            })
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun performLogin(username: String, pass: String) {
        CommonFunctions.showButtonLoader(binding.btnLogin, "Please wait...")

        val request = LoginRequest(username, pass)
        
        AssanRetrofitClient.apiService.getLoginData(request).enqueue(object : Callback<ResponseModelLogin> {
            override fun onResponse(
                call: Call<ResponseModelLogin>,
                response: Response<ResponseModelLogin>
            ) {
                if (!isAdded || _binding == null) return // user left the screen while logging in
                CommonFunctions.hideButtonLoader(binding.btnLogin)

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    // Some APIs return "Success" as string instead of boolean success
                    val isSuccess = loginResponse.successBool == true || loginResponse.successString == "1" || loginResponse.successString?.lowercase() == "true"
                    
                    if (isSuccess) {
                      //  Toast.makeText(requireContext(), R.string.auto_login_successful_645, Toast.LENGTH_SHORT).show()
                        
                        // Save user data to SharedPreferences
                        val sharedManager = SharedManager.getInstance(requireContext())
                        sharedManager.saveBoolean(Constants.isLogin, true)
                        
                        val userData = loginResponse.data
                        if (userData != null) {
                            sharedManager.saveString("NAME", userData.name ?: "")
                            sharedManager.saveString("USER_NAME", userData.userName ?: "")
                            sharedManager.saveString("CNIC", userData.cnic ?: "")
                            sharedManager.saveString("DATE_OF_BIRTH", userData.dateOfBirth ?: "")
                            sharedManager.saveString("CNIC_EXPIRY", userData.cnicExpiry ?: "")
                            sharedManager.saveString("CNIC_ISSUE_DATE", userData.cnicIssueDate ?: "")
                            sharedManager.saveString("PHONE", userData.phone ?: "")
                            sharedManager.saveString("GENDER", userData.gender ?: "")
                            sharedManager.saveString("PROFILE_PICTURE", userData.profilePicture ?: "")
                            sharedManager.saveString("BORROWER_NO", userData.borrowerNo ?: "")
                            sharedManager.saveString("EMAIL", userData.emailAddress ?: "")
                            sharedManager.saveString("ROLE_ID", userData.roleId ?: "")
                            sharedManager.saveString("REGION_ID", userData.regionId ?: "")
                            sharedManager.saveString("REGION_NAME", userData.regionName ?: "")
                            sharedManager.saveString("FIELD_UNIT_ID", userData.fieldUnitId ?: "")
                            sharedManager.saveString("FIELD_UNIT_NAME", userData.fieldUnitName ?: "")
                            sharedManager.saveString("STATUS", userData.status ?: "")
                            
                            val clientIdStr = userData.clientId ?: "0"
                            val clientId = try { clientIdStr.toInt() } catch (e: Exception) { 0 }
                            sharedManager.saveInt("CLIENT_ID", clientId)
                            Constants.CLIENT_ID = clientId
                        } else {
                            sharedManager.saveString("USER_NAME", username)
                        }
                        
                        // Start SignalR
                        context?.let { ctx ->
                            com.assan.finance.managers.SignalRManager.connect(ctx)
                        }
                        
                        // Get FCM Token
                        // FIX: this callback can run after we navigate to Home (fragment detached), where
                        // requireContext() throws. Use the application context captured now instead.
                        val appContext = requireContext().applicationContext
                        // If the token delete at the last logout failed (offline), delete it first so the
                        // previous user's token is not reused for this user.
                        val messaging = com.google.firebase.messaging.FirebaseMessaging.getInstance()
                        val tokenPrefs = SharedManager.getInstance(appContext)
                        val tokenTask = if (tokenPrefs.getBoolean(CommonFunctions.KEY_FCM_TOKEN_DELETE_PENDING, false)) {
                            messaging.deleteToken().continueWithTask { deleteTask ->
                                if (deleteTask.isSuccessful) {
                                    tokenPrefs.saveBoolean(CommonFunctions.KEY_FCM_TOKEN_DELETE_PENDING, false)
                                }
                                messaging.token
                            }
                        } else {
                            messaging.token
                        }
                        tokenTask.addOnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                Log.w("USMAN_TOKEN_ERROR", "Fetching FCM registration token failed", task.exception)
                                return@addOnCompleteListener
                            }
                            val token = task.result
                            Log.d("USMAN_TOKEN_ERROR", "Token generated on login: $token")
                            
                            val sharedManagerForToken = SharedManager.getInstance(appContext)
                            val savedToken = sharedManagerForToken.getString("FCM_TOKEN", "")
                            val isTokenRegistered = sharedManagerForToken.getBoolean("TOKEN_REGISTERED", false)
                            
                            if (token == savedToken && isTokenRegistered) {
                                Log.d("USMAN_TOKEN_ERROR", "Token already registered. Skipping API call.")
                                return@addOnCompleteListener
                            }

                            // Call .NET API to save Borrower ID, CNIC and Token
                            val borrowerIdForToken = userData?.borrowerNo ?: ""
                            val userCnic = userData?.cnic ?: username
                            val tokenRequest = com.assan.finance.model.assan.TokenRequest(borrowerIdForToken, token, userCnic)
                            Log.d("USMAN_TOKEN_ERROR", "Sending TokenRequest: BorrowerId=$borrowerIdForToken, CNIC=$userCnic, Token=$token")

                            AssanRetrofitClient.apiService.saveDeviceToken(tokenRequest).enqueue(object : Callback<ResponseModelLogin> {
                                override fun onResponse(call: Call<ResponseModelLogin>, res: Response<ResponseModelLogin>) {
                                    Log.d("USMAN_TOKEN_ERROR", "Token API Response Code: ${res.code()}")
                                    if (res.isSuccessful) {
                                        sharedManagerForToken.saveString("FCM_TOKEN", token)
                                        sharedManagerForToken.saveBoolean("TOKEN_REGISTERED", true)
                                        Log.d("USMAN_TOKEN_ERROR", "Token registered successfully")
                                    } else {
                                        Log.e("USMAN_TOKEN_ERROR", "Failed to register token: ${res.errorBody()?.string()}")
                                    }
                                }

                                override fun onFailure(call: Call<ResponseModelLogin>, t: Throwable) {
                                    Log.e("USMAN_TOKEN_ERROR", "API Failure Exception: ${t.message}")
                                }
                            })
                        }
                        
                        findNavController().navigate(R.id.action_assanLogin_to_homeFragment)
                    } else {
                        val msg = com.assan.finance.utils.CommonFunctions.getProfessionalErrorMessage(loginResponse.message) ?: "Invalid credentials"
                        showLoginErrorDialog(msg)
                    }
                } else {
                    showLoginErrorDialog(getString(R.string.str_unable_to_process_request))
                }
            }

            override fun onFailure(call: Call<ResponseModelLogin>, t: Throwable) {
                if (!isAdded || _binding == null) return
                CommonFunctions.hideButtonLoader(binding.btnLogin)
                CommonFunctions.handleApiFailure(requireContext(), binding.root, t)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun showLoginErrorDialog(message: String) {
        val dialog = android.app.Dialog(requireContext())
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_login_error, null)
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)

        val tvDialogMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
        val btnOk = dialogView.findViewById<TextView>(R.id.btnOk)

        tvDialogMessage.text = message

        btnOk.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
