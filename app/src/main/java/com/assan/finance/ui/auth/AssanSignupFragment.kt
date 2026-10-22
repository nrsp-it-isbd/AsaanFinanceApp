package com.assan.finance.ui.auth

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.assan.finance.R
import com.assan.finance.databinding.FragmentAssanSignupBinding
import com.assan.finance.model.assan.ResponseModelRegistration
import com.assan.finance.network.assan.AssanRetrofitClient
import androidx.navigation.fragment.findNavController
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import android.content.Intent
import android.widget.TextView
import android.widget.ImageView
import android.widget.FrameLayout
import android.graphics.Color
import com.assan.finance.utils.CommonFunctions

@android.annotation.SuppressLint("InflateParams")
class AssanSignupFragment : Fragment() {

    private val cnicScannerLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val data = result.data
            val cnic = data?.getStringExtra("CNIC_NUMBER") ?: ""
            val dob = data?.getStringExtra("DATE_OF_BIRTH")?.replace(".", "-") ?: ""
            val name = data?.getStringExtra("NAME_EN") ?: ""
            val expiry = data?.getStringExtra("EXPIRY_DATE")?.replace(".", "-") ?: ""
            val gender = data?.getStringExtra("GENDER") ?: ""
            val presentAddress = data?.getStringExtra("PRESENT_ADDRESS") ?: ""
            val permanentAddress = data?.getStringExtra("PERMANENT_ADDRESS") ?: ""
            
            if (cnic.isNotEmpty()) binding.etCnic.setText(cnic)
            if (dob.isNotEmpty()) binding.etDob.setText(dob)
            if (dob.isNotEmpty() && !CommonFunctions.isAgeAllowed(dob)) CommonFunctions.showAgeRestrictionDialog(requireActivity(), true, dob)
            if (name.isNotEmpty()) binding.etFullName.setText(name)
            if (expiry.isNotEmpty()) binding.etCnicExpiry.setText(expiry)
            if (presentAddress.isNotEmpty()) binding.etPresentAddress.setText(presentAddress)
            if (permanentAddress.isNotEmpty()) binding.etPermanentAddress.setText(permanentAddress)
            
            if (gender.equals("M", ignoreCase = true)) binding.rbMale.isChecked = true
            else if (gender.equals("F", ignoreCase = true)) binding.rbFemale.isChecked = true
            else if (gender.isNotEmpty()) binding.rbOther.isChecked = true
            
            Toast.makeText(requireContext(), R.string.auto_cnic_successfully_scanned_650, Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        val selectedImageUri = CommonFunctions.handleImageActivityResultProfileImage(
            REQUEST_IMAGE_CODE_CAMERA,
            result.resultCode,
            result.data,
            requireActivity(),
            REQUEST_IMAGE_CODE_CAMERA,
            REQUEST_IMAGE_CODE_GALLERY,
            binding.ivProfilePicture
        )
        if (selectedImageUri != null) {
            binding.ivProfilePicture.setImageURI(selectedImageUri)
            base64Image = CommonFunctions.convertToBase64(selectedImageUri, requireContext())
        }
    }

    private val galleryLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        val selectedImageUri = CommonFunctions.handleImageActivityResultProfileImage(
            REQUEST_IMAGE_CODE_GALLERY,
            result.resultCode,
            result.data,
            requireActivity(),
            REQUEST_IMAGE_CODE_CAMERA,
            REQUEST_IMAGE_CODE_GALLERY,
            binding.ivProfilePicture
        )
        if (selectedImageUri != null) {
            binding.ivProfilePicture.setImageURI(selectedImageUri)
            base64Image = CommonFunctions.convertToBase64(selectedImageUri, requireContext())
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            showImageSourceDialog()
        } else {
            Toast.makeText(requireContext(), R.string.auto_camera_permission_denied_651, Toast.LENGTH_SHORT).show()
        }
    }


    private var _binding: FragmentAssanSignupBinding? = null
    private val binding get() = _binding!!

    private val REQUEST_IMAGE_CODE_CAMERA = 100
    private val REQUEST_IMAGE_CODE_GALLERY = 200
    private var base64Image: String = ""
    private val REQUEST_CNIC_SCAN = 300

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssanSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val llSignupForm = binding.llSignupForm
        llSignupForm.translationY = 200f
        llSignupForm.alpha = 0f
        llSignupForm.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(600)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
        
        CommonFunctions.addCNICFormatting(binding.etCnic)
        CommonFunctions.addPhoneNumberFormatting(binding.etPhoneNumber, requireContext())
        
        val redAsterisk = "<font color='#FF0000'>*</font>"
        binding.tvFullNameLabel.text = android.text.Html.fromHtml("${getString(R.string.auto_full_name_795)} $redAsterisk", android.text.Html.FROM_HTML_MODE_LEGACY)
        binding.tvUsernameLabel.text = android.text.Html.fromHtml("${getString(R.string.auto_username_796)} $redAsterisk", android.text.Html.FROM_HTML_MODE_LEGACY)
        binding.tvCnicLabel.text = android.text.Html.fromHtml("${getString(R.string.auto_cnic_number_797)} $redAsterisk", android.text.Html.FROM_HTML_MODE_LEGACY)
        binding.tvDobLabel.text = android.text.Html.fromHtml("${getString(R.string.auto_date_of_birth_798)} $redAsterisk", android.text.Html.FROM_HTML_MODE_LEGACY)
        binding.tvExpiryLabel.text = android.text.Html.fromHtml("${getString(R.string.auto_cnic_expiry_799)} $redAsterisk", android.text.Html.FROM_HTML_MODE_LEGACY)
        binding.tvGenderLabel.text = android.text.Html.fromHtml("${getString(R.string.auto_gender_800)} $redAsterisk", android.text.Html.FROM_HTML_MODE_LEGACY)
        binding.tvPhoneLabel.text = android.text.Html.fromHtml("${getString(R.string.auto_phone_number_801)} $redAsterisk", android.text.Html.FROM_HTML_MODE_LEGACY)
        binding.tvPasswordLabel.text = android.text.Html.fromHtml("${getString(R.string.auto_password_802)} $redAsterisk", android.text.Html.FROM_HTML_MODE_LEGACY)
        
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.tvLogin.setOnClickListener {
            findNavController().navigateUp()
        }

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
        
        binding.etDob.setOnClickListener { showDobDatePicker { binding.etDob.setText(it) } }
        binding.etCnicExpiry.setOnClickListener { showCnicExpiryDatePicker { binding.etCnicExpiry.setText(it) } }



        binding.ivProfilePicture.setOnClickListener {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            } else {
                showImageSourceDialog()
            }
        }

        binding.etUserName.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && binding.etUserName.text.toString().isEmpty()) {
                val fullName = binding.etFullName.text.toString().trim()
                if (fullName.isNotEmpty()) {
                    val base = fullName.lowercase().replace("\\s+".toRegex(), "")
                    val randomNum = (100..999).random()
                    binding.etUserName.setText("$base$randomNum")
                }
            }
        }

        binding.etUserName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tvUsernameError.visibility = View.GONE
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        
        binding.etCnic.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                CommonFunctions.removeInlineError(binding.etCnic)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        
        binding.etPhoneNumber.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                CommonFunctions.removeInlineError(binding.etPhoneNumber)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.btnSignup.setOnClickListener {
            val name = binding.etFullName.text.toString().trim()
            val userName = binding.etUserName.text.toString().trim()
            val phone = binding.etPhoneNumber.text.toString().trim().replace("-", "")
            val cnic = binding.etCnic.text.toString().trim().replace("-", "")
            val dob = binding.etDob.text.toString().trim()
            val cnicExpiry = binding.etCnicExpiry.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()
            val presentAddress = binding.etPresentAddress.text.toString().trim()
            val permanentAddress = binding.etPermanentAddress.text.toString().trim()
            val gender = if (binding.rbMale.isChecked) "M" else if (binding.rbFemale.isChecked) "F" else "O"
            val deviceId = android.provider.Settings.Secure.getString(requireContext().contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: ""

            if (name.isEmpty() || userName.isEmpty() || cnic.isEmpty() || phone.isEmpty() || dob.isEmpty() || cnicExpiry.isEmpty() || pass.isEmpty() || presentAddress.isEmpty() || permanentAddress.isEmpty()) {
                Toast.makeText(requireContext(), R.string.auto_please_fill_all_required_652, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Must be 18 to 60 years old today (the date picker limits this, but a scanned CNIC date does not)
            if (!CommonFunctions.isAgeAllowed(dob)) {
                CommonFunctions.showAgeRestrictionDialog(requireActivity(), true, dob)
                return@setOnClickListener
            }

            if (cnic.length != 13) {
                CommonFunctions.showInlineError(requireContext(), binding.etCnic, getString(R.string.auto_please_enter_a_valid_15_d_642))
                return@setOnClickListener
            } else {
                CommonFunctions.removeInlineError(binding.etCnic)
            }

            if (phone.length != 11) {
                CommonFunctions.showInlineError(requireContext(), binding.etPhoneNumber, getString(R.string.auto_please_enter_a_valid_11_d_653))
                return@setOnClickListener
            } else {
                CommonFunctions.removeInlineError(binding.etPhoneNumber)
            }

            performSignup(name, userName, phone, cnic, dob, cnicExpiry, pass, presentAddress, permanentAddress, gender, deviceId)
        }
    }
    
    private fun showDobDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -18)
        val datePickerDialog = DatePickerDialog(requireContext(), { _, year, month, day ->
            val selectCal = Calendar.getInstance()
            selectCal.set(year, month, day)
            val sdf = java.text.SimpleDateFormat(com.assan.finance.utils.Constants.UI_DATE_PATTERN, java.util.Locale.ENGLISH)
            onDateSelected(sdf.format(selectCal.time))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        
        datePickerDialog.datePicker.maxDate = CommonFunctions.getDobPickerMaxMillis()
        datePickerDialog.datePicker.minDate = CommonFunctions.getDobPickerMinMillis()
        datePickerDialog.show()
    }

    private fun showCnicExpiryDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(requireContext(), { _, year, month, day ->
            val selectCal = Calendar.getInstance()
            selectCal.set(year, month, day)
            val sdf = java.text.SimpleDateFormat(com.assan.finance.utils.Constants.UI_DATE_PATTERN, java.util.Locale.ENGLISH)
            onDateSelected(sdf.format(selectCal.time))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    private fun performSignup(name: String, userName: String, phone: String, cnic: String, dob: String, cnicExpiry: String, pass: String, presentAddress: String, permanentAddress: String, gender: String, deviceId: String) {
        CommonFunctions.showButtonLoader(binding.btnSignup, "Please wait...")

        try {
            val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val userNameBody = userName.toRequestBody("text/plain".toMediaTypeOrNull())
            val cnicBody = cnic.toRequestBody("text/plain".toMediaTypeOrNull())
            val phoneBody = phone.toRequestBody("text/plain".toMediaTypeOrNull())
            val passBody = pass.toRequestBody("text/plain".toMediaTypeOrNull())
            val dobBody = com.assan.finance.utils.CommonFunctions.formatDateForServer(dob).toRequestBody("text/plain".toMediaTypeOrNull())
            val cnicExpiryBody = com.assan.finance.utils.CommonFunctions.formatDateForServer(cnicExpiry).toRequestBody("text/plain".toMediaTypeOrNull())
            val genderBody = gender.toRequestBody("text/plain".toMediaTypeOrNull())
            val deviceIdBody = deviceId.toRequestBody("text/plain".toMediaTypeOrNull())
            val presentAddressBody = presentAddress.toRequestBody("text/plain".toMediaTypeOrNull())
            val permanentAddressBody = permanentAddress.toRequestBody("text/plain".toMediaTypeOrNull())
            val picBody = base64Image.toRequestBody("text/plain".toMediaTypeOrNull())

            AssanRetrofitClient.apiService.registerUser(nameBody, userNameBody, cnicBody, phoneBody, passBody, dobBody, cnicExpiryBody, genderBody, deviceIdBody, presentAddressBody, permanentAddressBody, picBody).enqueue(object : Callback<ResponseModelRegistration> {
                override fun onResponse(call: Call<ResponseModelRegistration>, response: Response<ResponseModelRegistration>) {
                    if (!isAdded || _binding == null) return // user left the screen meanwhile
                    CommonFunctions.hideButtonLoader(binding.btnSignup)

                    if (response.isSuccessful && response.body() != null) {
                        val regResponse = response.body()!!
                        if (regResponse.success) {
                            showSignupResultDialog("Success", "Registration Successful", true)
                        } else {
                            val msg = com.assan.finance.utils.CommonFunctions.getProfessionalErrorMessage(regResponse.message) ?: "Signup failed"
                            val isAlreadyRegistered = msg.contains("already", ignoreCase = true) || msg.contains("exists", ignoreCase = true)
                            val isActuallySuccess = msg.contains("success", ignoreCase = true) || msg.contains("User registered successfully", ignoreCase = true)
                            
                            if (isAlreadyRegistered) {
                                if (msg.contains("phone", ignoreCase = true) || msg.contains("number", ignoreCase = true)) {
                                    CommonFunctions.showInlineError(requireContext(), binding.etPhoneNumber, msg)
                                    CommonFunctions.scrollViewToVisible(binding.etPhoneNumber)
                                } else if (msg.contains("cnic", ignoreCase = true)) {
                                    CommonFunctions.showInlineError(requireContext(), binding.etCnic, msg)
                                    CommonFunctions.scrollViewToVisible(binding.etCnic)
                                } else {
                                    binding.tvUsernameError.visibility = View.VISIBLE
                                    binding.tvUsernameError.text = msg
                                    CommonFunctions.scrollViewToVisible(binding.tvUsernameError)
                                }
                            } else {
                                val title = when {
                                    isActuallySuccess -> "Success"
                                    else -> "Failed"
                                }
                                showSignupResultDialog(title, msg, isActuallySuccess)
                            }
                        }
                    } else {
                        showSignupResultDialog("Failed", getString(R.string.str_unable_to_process_request), false)
                    }
                }

                override fun onFailure(call: Call<ResponseModelRegistration>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    CommonFunctions.hideButtonLoader(binding.btnSignup)
                    CommonFunctions.handleApiFailure(requireContext(), binding.root, t)
                }
            })
        } catch (e: Exception) {
            CommonFunctions.hideButtonLoader(binding.btnSignup)
            CommonFunctions.showSnakeBar(binding.root, "Error creating request.")
        }
    }


    private fun showSignupResultDialog(title: String, message: String, isSuccess: Boolean) {
        val context = context ?: return
        val dialog = android.app.Dialog(context)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        
        if (isSuccess) {
            dialog.setContentView(R.layout.dialog_success)
        } else {
            dialog.setContentView(R.layout.dialog_error)
        }
        
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        if (isSuccess) {
            val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
            val tvMessage = dialog.findViewById<TextView>(R.id.txtMessage)
            val btnOkay = dialog.findViewById<View>(R.id.okButton)

            tvTitle?.text = title
            tvMessage?.text = message

            btnOkay?.setOnClickListener {
                dialog.dismiss()
                binding.etCnic.text?.clear()
                binding.etPhoneNumber.text?.clear()
                binding.etFullName.text?.clear()
                binding.etUserName.text?.clear()
                binding.etPassword.text?.clear()
                binding.etDob.text?.clear()
                binding.etCnicExpiry.text?.clear()
                binding.etPresentAddress.text?.clear()
                binding.etPermanentAddress.text?.clear()
                
                binding.rbMale.isChecked = true
                binding.rbFemale.isChecked = false
                binding.rbOther.isChecked = false
                binding.ivProfilePicture.setImageResource(R.drawable.dummy_profile_image)
                base64Image = ""
                
                findNavController().navigateUp()
            }
        } else {
            val tvTitle = dialog.findViewById<TextView>(R.id.txtErrorTitle)
            val tvMessage = dialog.findViewById<TextView>(R.id.txtErrorMessage)
            val btnDismiss = dialog.findViewById<View>(R.id.btnDismiss)
            
            tvTitle?.text = title
            tvMessage?.text = message
            
            btnDismiss?.setOnClickListener {
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }
    
    private fun showImageSourceDialog() {
        val context = context ?: return
        val dialog = android.app.Dialog(context)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        
        val view = layoutInflater.inflate(R.layout.dialog_image_source, null)
        dialog.setContentView(view)

        view.findViewById<View>(R.id.openCamera).setOnClickListener {
            val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(intent)
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.openGallery).setOnClickListener {
            galleryLauncher.launch(CommonFunctions.createImagePickerIntent())
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.btnCancelDialog).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
