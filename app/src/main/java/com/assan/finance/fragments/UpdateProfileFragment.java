package com.assan.finance.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


import de.hdodenhof.circleimageview.CircleImageView;
import com.assan.finance.R;
import com.assan.finance.apis.ApiManager;
import com.assan.finance.apis.ApiService;
import com.assan.finance.apis.RetrofitClient;
import com.assan.finance.dataModels.DataModelProfile;
import com.assan.finance.responseModel.ResponseModelProfileUpdate;
import com.assan.finance.sharedPreferences.SharedManager;
import com.assan.finance.utils.CommonFunctions;
import com.assan.finance.utils.Constants;
import com.assan.finance.utils.LocaleHelper;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateProfileFragment extends Fragment {
    

    private CircleImageView ivProfilePic;
    private FloatingActionButton btnChangePhoto;
    private TextView txtName, txtAssanId, txtUsername;
    private MaterialButton updateProfileButton;
    private ImageView btnEditTop, ivDobIcon;
    private RelativeLayout layoutDob;
    private boolean isEditable = false;
    private EditText edFullName, edPhoneNumber, edCNICNumber, edDateOfBirth, edOldPassword, edNewPassword, edNewConfirmPassword;

    private SharedManager sharedManager;
    private String PROFILE_IMAGE_BASE64 = "";
    
    private String name = "", dateOfBirth = "", cnicNumber = "", phoneNumber = "", oldPassword = "", newPassword = "", newConfirmPassword = "";

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (!isAdded()) return;
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    Bundle extras = data.getExtras();
                    if (extras != null && extras.get("data") != null) {
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        ivProfilePic.setImageBitmap(imageBitmap);
                        Uri uri = CommonFunctions.getImageUri(requireContext(), imageBitmap);
                        if (uri != null) {
                            PROFILE_IMAGE_BASE64 = CommonFunctions.convertToBase64(uri, requireContext());
                        }
                        onNewPhotoSelected();
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        Uri selectedImageUri = data.getData();
                        ivProfilePic.setImageURI(selectedImageUri);
                        PROFILE_IMAGE_BASE64 = CommonFunctions.convertToBase64(selectedImageUri, requireContext());
                        onNewPhotoSelected();
                    }
                }
            }
    );

    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    showImageSourceDialog();
                } else {
                    CommonFunctions.showToast(requireContext(), getString(R.string.str_camera_permission_required));
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_update_profile, container, false);

        sharedManager = SharedManager.getInstance(requireContext());

        ivProfilePic = view.findViewById(R.id.ivProfilePic);
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto);
        txtName = view.findViewById(R.id.txtName);
        txtAssanId = view.findViewById(R.id.txtAssanId);
        txtUsername = view.findViewById(R.id.txtUsername);
        btnEditTop = view.findViewById(R.id.btnEditTop);
        updateProfileButton = view.findViewById(R.id.updateProfileButton);
        
        layoutDob = view.findViewById(R.id.layoutDob);
        ivDobIcon = view.findViewById(R.id.ivDobIcon);
        edFullName = view.findViewById(R.id.edFullName);
        edPhoneNumber = view.findViewById(R.id.edPhoneNumber);
        edCNICNumber = view.findViewById(R.id.edCNICNumber);
        edDateOfBirth = view.findViewById(R.id.edDateOfBirth);
        edOldPassword = view.findViewById(R.id.edOldPassword);
        edNewPassword = view.findViewById(R.id.edNewPassword);
        edNewConfirmPassword = view.findViewById(R.id.edNewConfirmPassword);

        View backBtn = view.findViewById(R.id.btnBack);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }
        formatData();
        // CNIC is never editable here: show it dimmed so users can see it is locked
        edCNICNumber.setEnabled(false);
        if (edCNICNumber.getParent() instanceof View) ((View) edCNICNumber.getParent()).setAlpha(0.6f);
        setupViews();
        setupClickListeners(view);
        lockFields();

        return view;
    }


    @SuppressLint("SetTextI18n")
    private void setupViews() {
        String userProfileImage = sharedManager.getString("PROFILE_PICTURE", "");
        if (!CommonFunctions.isNullOrEmpty(userProfileImage)) {
            String fullUrl = ApiManager.getFullImageUrl(userProfileImage);
            com.squareup.picasso.Picasso.get()
                    .load(fullUrl)
                    .noFade()
                    .placeholder(R.drawable.dummy_profile_image)
                    .error(R.drawable.dummy_profile_image)
                    .into(ivProfilePic);
        } else {
            ivProfilePic.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.dummy_profile_image));
        }

        if (Constants.CLIENT_ID != -1) {
            txtAssanId.setText(getString(R.string.str_Asaan_id_colon) + Constants.CLIENT_ID);
        }

        String username = sharedManager.getString("USER_NAME", "");
        if (!CommonFunctions.isNullOrEmpty(username)) {
            txtUsername.setText("@" + username);
            txtUsername.setVisibility(View.VISIBLE);
        } else {
            txtUsername.setVisibility(View.GONE);
        }

        txtName.setText(sharedManager.getString(getString(R.string.lbl_name_upper), getString(R.string.lbl_Asaan_user)));
        edFullName.setText(sharedManager.getString(getString(R.string.lbl_name_upper), ""));
        edCNICNumber.setText(sharedManager.getString(getString(R.string.lbl_cnic_upper), ""));
        edPhoneNumber.setText(sharedManager.getString(getString(R.string.lbl_phone_upper), ""));
        String rawDob = sharedManager.getString("DATE_OF_BIRTH", "");
        edDateOfBirth.setText(!CommonFunctions.isNullOrEmpty(rawDob) ? CommonFunctions.formatDateOfBirth(rawDob) : "");
    }

    private void setupClickListeners(View view) {
        if (btnEditTop != null) {
            btnEditTop.setOnClickListener(v -> {
                if (!isEditable) {
                    makeEditable();
                } else {
                    lockFields();
                }
            });
        }
        updateProfileButton.setOnClickListener(v -> {
            CommonFunctions.hideKeyboard(requireActivity());
            getDataFromFields();
        });
        
        View.OnClickListener photoClick = v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            } else {
                showImageSourceDialog();
            }
        };
        
        ivProfilePic.setOnClickListener(photoClick);
        btnChangePhoto.setOnClickListener(photoClick);
        
        View.OnClickListener dobClick = v -> {
            if (!isEditable) {
                makeEditable();
            }
            showDobDatePicker();
        };
        edDateOfBirth.setOnClickListener(dobClick);
        if (layoutDob != null) layoutDob.setOnClickListener(dobClick);
        if (ivDobIcon != null) ivDobIcon.setOnClickListener(dobClick);
    }

    /**
     * FIX: after choosing a new photo the Update button stayed disabled until the user found the
     * Edit (pencil) button, so a photo-only change could not be saved. Enable it right away.
     */
    private void onNewPhotoSelected() {
        if (!isAdded() || CommonFunctions.isNullOrEmpty(PROFILE_IMAGE_BASE64)) return;
        if (!isEditable) makeEditable();
        CommonFunctions.showSnakeBar(getView(), "New photo selected. Tap Update to save it.");
    }

    private void showDobDatePicker() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        String currentDob = edDateOfBirth.getText() != null ? edDateOfBirth.getText().toString().trim() : "";
        if (!currentDob.isEmpty()) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(com.assan.finance.utils.Constants.UI_DATE_PATTERN, java.util.Locale.ENGLISH);
                java.util.Date parsed = sdf.parse(currentDob);
                if (parsed != null) {
                    calendar.setTime(parsed);
                }
            } catch (Exception ignored) {
                calendar.add(java.util.Calendar.YEAR, -18);
            }
        } else {
            calendar.add(java.util.Calendar.YEAR, -18);
        }
        
        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                requireActivity(), 
                (datePickerView, year, month, dayOfMonth) -> {
                    java.util.Calendar selectCal = java.util.Calendar.getInstance();
                    selectCal.set(year, month, dayOfMonth);
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(com.assan.finance.utils.Constants.UI_DATE_PATTERN, java.util.Locale.ENGLISH);
                    edDateOfBirth.setText(sdf.format(selectCal.getTime()));
                    edDateOfBirth.setError(null);
                }, 
                calendar.get(java.util.Calendar.YEAR), 
                calendar.get(java.util.Calendar.MONTH), 
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
        );
        
        // Date of birth must give an age of 18 to 60
        datePickerDialog.getDatePicker().setMaxDate(com.assan.finance.utils.CommonFunctions.getDobPickerMaxMillis());
        datePickerDialog.getDatePicker().setMinDate(com.assan.finance.utils.CommonFunctions.getDobPickerMinMillis());
        
        datePickerDialog.show();
    }

    private void formatData() {
        CommonFunctions.addPhoneNumberFormatting(edPhoneNumber, requireContext());
        CommonFunctions.addCNICFormatting(edCNICNumber);
    }

    private void getDataFromFields() {
        name = edFullName.getText() != null ? edFullName.getText().toString() : "";
        dateOfBirth = edDateOfBirth.getText() != null ? edDateOfBirth.getText().toString() : "";
        cnicNumber = edCNICNumber.getText() != null ? edCNICNumber.getText().toString().replace("-", "") : "";
        phoneNumber = edPhoneNumber.getText() != null ? edPhoneNumber.getText().toString().replace("-", "") : "";
        
        oldPassword = edOldPassword.getText() != null ? edOldPassword.getText().toString() : "";
        newPassword = edNewPassword.getText() != null ? edNewPassword.getText().toString() : "";
        newConfirmPassword = edNewConfirmPassword.getText() != null ? edNewConfirmPassword.getText().toString() : "";
        
        validateAndSubmit();
    }

    private void validateAndSubmit() {
        if (name.isEmpty()) {
            edFullName.setError("Required");
            return;
        }
        if (phoneNumber.length() < 11) {
            edPhoneNumber.setError("Invalid Phone Number");
            return;
        }
        if (cnicNumber.length() < 13) {
            edCNICNumber.setError("Invalid CNIC");
            return;
        }
        if (dateOfBirth.isEmpty()) {
            edDateOfBirth.setError("Required");
            return;
        }
        
        if (!oldPassword.isEmpty() || !newPassword.isEmpty() || !newConfirmPassword.isEmpty()) {
            if (oldPassword.isEmpty() || newPassword.isEmpty() || newConfirmPassword.isEmpty()) {
                CommonFunctions.showToast(requireContext(), "Fill all password fields to update password");
                return;
            }
            if (newPassword.length() < 4) {
                CommonFunctions.showToast(requireContext(), "New password must be at least 4 characters");
                return;
            }
            if (!newPassword.equals(newConfirmPassword)) {
                CommonFunctions.showToast(requireContext(), "New passwords do not match");
                return;
            }
        }

        if (CommonFunctions.isNetworkAvailable(requireContext())) {
            updateProfile();
        } else {
            CommonFunctions.showSnakeBar(getView(), "No Internet Connection");
        }
    }

    private void updateProfile() {
        CommonFunctions.showButtonLoader(updateProfileButton, "Updating...");

        RequestBody profilePictureRequestBody = RequestBody.create(PROFILE_IMAGE_BASE64, MediaType.parse("text/plain"));
        RequestBody nameRequestBody = RequestBody.create(name, MediaType.parse("text/plain"));
        RequestBody cnicRequestBody = RequestBody.create(cnicNumber, MediaType.parse("text/plain"));
        RequestBody phoneRequestBody = RequestBody.create(CommonFunctions.removeDashes(phoneNumber), MediaType.parse("text/plain"));
        RequestBody dobRequestBody = RequestBody.create(CommonFunctions.formatDateForServer(dateOfBirth), MediaType.parse("text/plain"));
        RequestBody clientIdRequestBody = RequestBody.create(String.valueOf(Constants.CLIENT_ID), MediaType.parse("text/plain"));

        RetrofitClient<ApiService> retrofitClient = new RetrofitClient<>(ApiService.class, requireContext());
        ApiService apiService = retrofitClient.getApiService();
        
        Call<ResponseModelProfileUpdate> call = apiService.updateProfile(
                nameRequestBody, cnicRequestBody, phoneRequestBody, dobRequestBody,
                profilePictureRequestBody, clientIdRequestBody
        );

        call.enqueue(new Callback<ResponseModelProfileUpdate>() {
            @Override
            public void onResponse(@NonNull Call<ResponseModelProfileUpdate> call, @NonNull Response<ResponseModelProfileUpdate> response) {
                if (!isAdded() || getView() == null) return; // user left the screen meanwhile
                if (response.isSuccessful() && response.body() != null) {
                    ResponseModelProfileUpdate profileUpdate = response.body();
                    
                    if (profileUpdate.isSuccess()) {
                        setUpdatedRecord(profileUpdate.getUpdatedRecord());
                        PROFILE_IMAGE_BASE64 = ""; // saved; don't upload the same photo again next time
                        lockFields();
                        
                        if (!newPassword.isEmpty()) {
                            changePasswordAPI();
                        } else {
                            CommonFunctions.hideButtonLoader(updateProfileButton);
                            showProfileSuccessDialog(false);
                        }
                    } else {
                        CommonFunctions.hideButtonLoader(updateProfileButton);
                        CommonFunctions.showSnakeBar(getView(), CommonFunctions.getProfessionalErrorMessage(profileUpdate.getMessage()));
                    }
                } else {
                    CommonFunctions.hideButtonLoader(updateProfileButton);
                    CommonFunctions.showSnakeBar(getView(), getString(R.string.str_unable_to_process_request));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseModelProfileUpdate> call, @NonNull Throwable t) {
                if (!isAdded() || getView() == null) return;
                CommonFunctions.hideButtonLoader(updateProfileButton);
                CommonFunctions.handleApiFailure(requireContext(), getView(), t);
            }
        });
    }
    
    private void changePasswordAPI() {
        RetrofitClient<ApiService> retrofitClient = new RetrofitClient<>(ApiService.class, requireContext());
        ApiService apiService = retrofitClient.getApiService();
        Call<com.assan.finance.responseModel.ResponseModelChangePassword> call = apiService.changePassword(
                CommonFunctions.convertToString(Constants.CLIENT_ID), cnicNumber.replace("-", ""), oldPassword, newPassword);
        
        call.enqueue(new Callback<com.assan.finance.responseModel.ResponseModelChangePassword>() {
            @Override
            public void onResponse(@NonNull Call<com.assan.finance.responseModel.ResponseModelChangePassword> call, 
                                   @NonNull Response<com.assan.finance.responseModel.ResponseModelChangePassword> response) {
                if (!isAdded() || getView() == null) return;
                CommonFunctions.hideButtonLoader(updateProfileButton);
                com.assan.finance.responseModel.ResponseModelChangePassword responseModel = response.body();
                if (responseModel != null) {
                    String messageResponse = CommonFunctions.getProfessionalErrorMessage(responseModel.getMessage());
                    if ("Password Updated".equalsIgnoreCase(messageResponse) || responseModel.isSuccess()) {
                        edOldPassword.setText("");
                        edNewPassword.setText("");
                        edNewConfirmPassword.setText("");
                        showProfileSuccessDialog(true);
                    } else {
                        CommonFunctions.showSnakeBar(getView(), messageResponse);
                    }
                } else {
                    CommonFunctions.showSnakeBar(getView(), getString(R.string.str_unable_to_process_request));
                }
            }

            @Override
            public void onFailure(@NonNull Call<com.assan.finance.responseModel.ResponseModelChangePassword> call, @NonNull Throwable t) {
                if (!isAdded() || getView() == null) return;
                CommonFunctions.hideButtonLoader(updateProfileButton);
                CommonFunctions.handleApiFailure(requireContext(), getView(), t);
            }
        });
    }

    private void showProfileSuccessDialog(boolean isPasswordUpdated) {
        if (!isAdded() || getActivity() == null) return;
        boolean isUrdu = "ur".equals(LocaleHelper.getLanguage(requireContext()));
        String title;
        String message;
        String buttonText = isUrdu ? "ٹھیک ہے" : "Done";

        if (isPasswordUpdated) {
            title = isUrdu ? "پروفائل اور پاس ورڈ اپ ڈیٹ!" : "Profile & Password Updated!";
            message = isUrdu
                    ? "آپ کی پروفائل کی تفصیلات اور نیا پاس ورڈ کامیابی کے ساتھ محفوظ ہو چکے ہیں۔"
                    : "Your profile details and new password have been updated successfully.";
        } else {
            title = isUrdu ? "پروفائل اپ ڈیٹ ہو گئی!" : "Profile Updated Successfully!";
            message = isUrdu
                    ? "آپ کی پروفائل کی معلومات کامیابی کے ساتھ محفوظ ہو چکی ہیں۔"
                    : "Your profile information has been updated successfully.";
        }

        CommonFunctions.showPremiumFeedbackDialog(getActivity(), "success", title, message, buttonText, null);
    }

    private void setUpdatedRecord(DataModelProfile updatedRecord) {
        if (updatedRecord != null) {
            if (updatedRecord.getProfilePicture() != null && !updatedRecord.getProfilePicture().trim().isEmpty()) {
                sharedManager.saveString("PROFILE_PICTURE", updatedRecord.getProfilePicture());
            }
            if (updatedRecord.getName() != null && !updatedRecord.getName().trim().isEmpty()) {
                sharedManager.saveString(getString(R.string.lbl_name_upper), updatedRecord.getName());
                txtName.setText(updatedRecord.getName());
            }
            if (updatedRecord.getCnic() != null && !updatedRecord.getCnic().trim().isEmpty()) {
                sharedManager.saveString(getString(R.string.lbl_cnic_upper), updatedRecord.getCnic());
            }
            if (updatedRecord.getDateOfBirth() != null && !updatedRecord.getDateOfBirth().trim().isEmpty()) {
                String formattedDob = CommonFunctions.formatDateOfBirth(updatedRecord.getDateOfBirth());
                sharedManager.saveString("DATE_OF_BIRTH", !formattedDob.isEmpty() ? formattedDob : updatedRecord.getDateOfBirth());
                if (!formattedDob.isEmpty()) {
                    edDateOfBirth.setText(formattedDob);
                }
            }
            if (updatedRecord.getPhone() != null && !updatedRecord.getPhone().trim().isEmpty()) {
                sharedManager.saveString(getString(R.string.lbl_phone_upper), updatedRecord.getPhone());
            }
        }
    }

    private void makeEditable() {
        isEditable = true;
        edFullName.setEnabled(true);
        // FIX: CNIC is the login ID and the identity of the financing record; it is not editable here
        edCNICNumber.setEnabled(false);
        edPhoneNumber.setEnabled(true);
        edDateOfBirth.setEnabled(true);
        
        edOldPassword.setEnabled(true);
        edNewPassword.setEnabled(true);
        edNewConfirmPassword.setEnabled(true);
        
        updateProfileButton.setEnabled(true);
        updateProfileButton.setAlpha(1.0f);

        if (btnEditTop != null) {
            btnEditTop.setBackgroundResource(R.drawable.bg_pill_light_teal);
            btnEditTop.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary));
        }

        edFullName.requestFocus();
        CommonFunctions.showSnakeBar(getView(), "Profile editing enabled");
    }

    private void lockFields() {
        isEditable = false;
        edFullName.setEnabled(false);
        edCNICNumber.setEnabled(false);
        edPhoneNumber.setEnabled(false);
        edDateOfBirth.setEnabled(false);
        
        edOldPassword.setEnabled(false);
        edNewPassword.setEnabled(false);
        edNewConfirmPassword.setEnabled(false);
        
        updateProfileButton.setEnabled(false);
        updateProfileButton.setAlpha(0.6f);

        if (btnEditTop != null) {
            btnEditTop.setBackgroundResource(R.drawable.bg_pill_grey);
            btnEditTop.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.navy_blue));
        }
    }

    private void showImageSourceDialog() {
        Dialog dialog = new Dialog(requireContext());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_image_source, null);
        dialog.setContentView(dialogView);

        View cameraImageView = dialog.findViewById(R.id.openCamera);
        View galleryImageView = dialog.findViewById(R.id.openGallery);
        View btnCancel = dialog.findViewById(R.id.btnCancelDialog);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        cameraImageView.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(takePictureIntent);
            dialog.dismiss();
        });

        galleryImageView.setOnClickListener(v -> {
            galleryLauncher.launch(CommonFunctions.createImagePickerIntent());
            dialog.dismiss();
        });

        dialog.show();
    }
}

