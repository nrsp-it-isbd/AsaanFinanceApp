package com.assan.finance.fragments;

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
import com.assan.finance.utils.Constants;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordFragment extends Fragment {
    

    private EditText edOldPassword, edNewPassword, edNewConfirmPassword;
    private MaterialButton updatePasswordButton;

    private String oldPassword = "";
    private String newPassword = "";
    private String newConfirmPassword = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_password, container, false);

        edOldPassword = view.findViewById(R.id.edOldPassword);
        edNewPassword = view.findViewById(R.id.edNewPassword);
        edNewConfirmPassword = view.findViewById(R.id.edNewConfirmPassword);
        updatePasswordButton = view.findViewById(R.id.updatePasswordButton);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        setupClickListeners();

        return view;
    }

    private void setupClickListeners() {
        updatePasswordButton.setOnClickListener(v -> validateFields());
    }

    private void validateFields() {
        oldPassword = Objects.requireNonNull(edOldPassword.getText()).toString();
        newPassword = Objects.requireNonNull(edNewPassword.getText()).toString();
        newConfirmPassword = Objects.requireNonNull(edNewConfirmPassword.getText()).toString();

        if (oldPassword.isEmpty() || newPassword.isEmpty() || newConfirmPassword.isEmpty()) {
            CommonFunctions.showSnakeBar(getView(), "Fill all the fields");
        } else if (!newPassword.equals(newConfirmPassword)) {
            CommonFunctions.showSnakeBar(getView(), "Password not match");
        } else {
            changePassword();
        }
    }

    private void changePassword() {
        CommonFunctions.showButtonLoader(updatePasswordButton, "Updating...");
        
        RetrofitClient<ApiService> retrofitClient = new RetrofitClient<>(ApiService.class, requireContext());
        ApiService apiService = retrofitClient.getApiService();
        Call<ResponseModelChangePassword> call = apiService.changePassword(
                CommonFunctions.convertToString(Constants.CLIENT_ID),
                com.assan.finance.sharedPreferences.SharedManager.getInstance(requireContext()).getString("CNIC", "").replace("-", ""),
                oldPassword, newPassword);
                
        call.enqueue(new Callback<ResponseModelChangePassword>() {
            @Override
            public void onResponse(@NonNull Call<ResponseModelChangePassword> call, @NonNull Response<ResponseModelChangePassword> response) {
                if (!isAdded() || getView() == null) return;
                CommonFunctions.hideButtonLoader(updatePasswordButton);
                
                ResponseModelChangePassword changePassword = response.body();
                if (changePassword != null) {
                    String messageResponse = CommonFunctions.getProfessionalErrorMessage(changePassword.getMessage());
                    if (messageResponse.equalsIgnoreCase("Password Updated") || changePassword.isSuccess()) {
                        CommonFunctions.showToast(requireContext(), "Password Updated");
                        if (getActivity() != null) {
                            getActivity().onBackPressed();
                        }
                    } else {
                        CommonFunctions.showToast(requireContext(), messageResponse);
                    }
                } else {
                    CommonFunctions.showSnakeBar(getView(), getString(R.string.str_unable_to_process_request));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseModelChangePassword> call, @NonNull Throwable t) {
                if (!isAdded() || getView() == null) return;
                CommonFunctions.hideButtonLoader(updatePasswordButton);
                CommonFunctions.handleApiFailure(requireContext(), getView(), t);
            }
        });
    }
}
