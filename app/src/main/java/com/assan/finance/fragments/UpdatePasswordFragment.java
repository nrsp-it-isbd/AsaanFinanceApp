package com.assan.finance.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import android.widget.EditText;

import java.util.Objects;

import com.assan.finance.R;
import com.assan.finance.apis.ApiService;
import com.assan.finance.apis.RetrofitClient;
import com.assan.finance.responseModel.ResponseModelChangePassword;
import com.assan.finance.utils.CommonFunctions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.assan.finance.sharedPreferences.SharedManager;

public class UpdatePasswordFragment extends Fragment {

    private EditText editPhoneNumber, edNewPassword, edNewConfirmPassword;
    private MaterialButton updatePasswordButton;


    private String phoneNumber = "";
    private String newPassword = "";
    private String newConfirmPassword = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_update_password, container, false);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        editPhoneNumber = view.findViewById(R.id.editPhoneNumber);
        edNewPassword = view.findViewById(R.id.edNewPassword);
        edNewConfirmPassword = view.findViewById(R.id.edNewConfirmPassword);
        updatePasswordButton = view.findViewById(R.id.updatePasswordButton);

        setupClickListeners();
        formatData();

        if (getArguments() != null) {
            phoneNumber = getArguments().getString("phoneNumber", "");
            editPhoneNumber.setText(phoneNumber);
        }

        return view;
    }


    private void setupClickListeners() {
        updatePasswordButton.setOnClickListener(v -> validateFields());
    }

    private void formatData() {
        CommonFunctions.addPhoneNumberFormatting(editPhoneNumber, requireContext());
    }

    private void validateFields() {
        phoneNumber = Objects.requireNonNull(editPhoneNumber.getText()).toString().replace("-", "");
        newPassword = Objects.requireNonNull(edNewPassword.getText()).toString();
        newConfirmPassword = Objects.requireNonNull(edNewConfirmPassword.getText()).toString();

        if (phoneNumber.isEmpty() || newPassword.isEmpty() || newConfirmPassword.isEmpty()) {
            CommonFunctions.showSnakeBar(getView(), "Fill all the fields");
        } else if (!newPassword.equals(newConfirmPassword)) {
            CommonFunctions.showSnakeBar(getView(), "Password not match");
        } else {
            updatePassword();
        }
    }

    private void updatePassword() {
        CommonFunctions.showButtonLoader(updatePasswordButton, "Updating...");

        RetrofitClient<ApiService> retrofitClient = new RetrofitClient<>(ApiService.class, requireContext());
        ApiService apiService = retrofitClient.getApiService();
        Call<ResponseModelChangePassword> call = apiService.updatePassword(
                CommonFunctions.removeDashes(phoneNumber), newPassword);
        
        call.enqueue(new Callback<ResponseModelChangePassword>() {
            @Override
            public void onResponse(@NonNull Call<ResponseModelChangePassword> call, @NonNull Response<ResponseModelChangePassword> response) {
                CommonFunctions.hideButtonLoader(updatePasswordButton);

                ResponseModelChangePassword responseModel = response.body();
                if (responseModel != null) {
                    String messageResponse = CommonFunctions.getProfessionalErrorMessage(responseModel.getMessage());
                    if (messageResponse.equals("Password Updated") || responseModel.isSuccess()) {
                        CommonFunctions.showToast(requireContext(), "Password Updated successfully");
                        SharedManager.getInstance(requireContext()).clearPreferences();
                        Intent intent = new Intent(getActivity(), com.assan.finance.MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        if (getActivity() != null) getActivity().finish();
                    } else {
                        CommonFunctions.showToast(requireContext(), messageResponse);
                    }
                } else {
                    CommonFunctions.showSnakeBar(getView(), getString(R.string.str_unable_to_process_request));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseModelChangePassword> call, @NonNull Throwable t) {
                CommonFunctions.hideButtonLoader(updatePasswordButton);
                CommonFunctions.handleApiFailure(requireContext(), getView(), t);
            }
        });
    }
}
