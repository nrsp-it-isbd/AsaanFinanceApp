package com.assan.finance.fragments
import com.assan.finance.R

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.assan.finance.databinding.FragmentFeedbackBinding
import com.assan.finance.model.assan.ResponseModelRegistration
import com.assan.finance.network.assan.AssanRetrofitClient
import com.assan.finance.sharedPreferences.SharedManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import android.util.Log
import com.assan.finance.utils.CommonFunctions

class FeedbackFragment : Fragment() {

    private var _binding: FragmentFeedbackBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedManager: SharedManager

    private val categories = arrayOf("App Issue", "Complaint", "Suggestion", "Other")
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            selectedImageUri = result.data?.data
            binding.ivAttachmentPreview.visibility = View.VISIBLE
            binding.ivAttachmentPreview.setImageURI(selectedImageUri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedbackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedManager = SharedManager.getInstance(requireContext())

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupSpinner()

        binding.btnAttachImage.setOnClickListener {
            imagePickerLauncher.launch(com.assan.finance.utils.CommonFunctions.createImagePickerIntent())
        }

        binding.btnSubmit.setOnClickListener {
            submitFeedback()
        }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spinnerTitle.adapter = adapter

        binding.spinnerTitle.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (categories[position] == "Other") {
                    binding.tilCustomTitle.visibility = View.VISIBLE
                } else {
                    binding.tilCustomTitle.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val file = File(requireContext().cacheDir, "feedback_attachment_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun submitFeedback() {
        val category = binding.spinnerTitle.selectedItem.toString()
        val title = if (category == "Other") {
            val customTitle = binding.etCustomTitle.text.toString().trim()
            if (customTitle.isEmpty()) {
                Toast.makeText(requireContext(), R.string.auto_please_enter_a_custom_tit_637, Toast.LENGTH_SHORT).show()
                return
            }
            customTitle
        } else {
            category
        }

        val comments = binding.etComments.text.toString().trim()
        val cnic = sharedManager.getString("CNIC", "") ?: ""
        var borrowerId = sharedManager.getString("BORROWER_NO", "") ?: ""
        if (borrowerId.isEmpty()) {
            borrowerId = "0000-000000"
        }

        val borrowerIdPart = MultipartBody.Part.createFormData("BorrowerId", borrowerId)
        val cnicPart = MultipartBody.Part.createFormData("Cnic", cnic)
        val titlePart = MultipartBody.Part.createFormData("Title", title)
        val commentsPart = MultipartBody.Part.createFormData("Comments", comments)

        var imagePart: MultipartBody.Part? = null
        if (selectedImageUri != null) {
            val file = getFileFromUri(selectedImageUri!!)
            if (file != null) {
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                imagePart = MultipartBody.Part.createFormData("Attachment", file.name, requestFile)
            }
        }

        binding.btnSubmit.isEnabled = false
        binding.btnSubmit.text = getString(R.string.str_submitting_feedback)
        
        Log.d("USMAN_FEEDBACK", "Making Request -> BorrowerId: $borrowerId, Cnic: $cnic, Title: $title, Comments: $comments, HasImage: ${imagePart != null}")

        try {
            AssanRetrofitClient.apiService.submitFeedback(
                borrowerIdPart, cnicPart, titlePart, commentsPart, imagePart
            ).enqueue(object : Callback<ResponseModelRegistration> {
            override fun onResponse(
                call: Call<ResponseModelRegistration>,
                response: Response<ResponseModelRegistration>
            ) {
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = getString(R.string.str_submit_feedback_btn)
                
                Log.d("USMAN_FEEDBACK", "Response Code: ${response.code()}")
                Log.d("USMAN_FEEDBACK", "Response Body: ${response.body()}")
                try {
                    Log.d("USMAN_FEEDBACK", "Response Error: ${response.errorBody()?.string()}")
                } catch (e: Exception) { }
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val msg = com.assan.finance.utils.CommonFunctions.getProfessionalErrorMessage(response.body()?.message) ?: "Feedback submitted successfully."
                    CommonFunctions.showSuccessDialog(requireActivity(), msg) {
                        findNavController().navigateUp()
                    }
                } else {
                    val msg = com.assan.finance.utils.CommonFunctions.getProfessionalErrorMessage(response.body()?.message) ?: "Failed to submit feedback"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseModelRegistration>, t: Throwable) {
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = getString(R.string.str_submit_feedback_btn)
                Log.e("USMAN_FEEDBACK", "OnFailure Network error: ${t.message}", t)
                com.assan.finance.utils.CommonFunctions.handleApiFailure(requireContext(), binding.root, t)
            }
        })
        } catch (e: Exception) {
            binding.btnSubmit.isEnabled = true
            binding.btnSubmit.text = getString(R.string.str_submit_feedback_btn)
            Log.e("USMAN_FEEDBACK", "Crash before API call: ${e.message}", e)
            Toast.makeText(requireContext(), getString(R.string.str_unable_to_process_request), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
