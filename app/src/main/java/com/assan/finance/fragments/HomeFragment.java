package com.assan.finance.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.assan.finance.R;
import com.assan.finance.activities.loan.LoanApplicationActivity;
import com.assan.finance.sharedPreferences.SharedManager;
import com.assan.finance.utils.CommonFunctions;

public class HomeFragment extends Fragment {
    private TextView tvHomeUserName;
    private TextView tvBorrowerNumber;
    private SharedManager sharedManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        sharedManager = SharedManager.getInstance(requireContext());

        tvHomeUserName = view.findViewById(R.id.tvHomeUserName);
        tvBorrowerNumber = view.findViewById(R.id.tvBorrowerNumber);

        // Promotion banner — both card and apply button launch loan form
        View cardPromo = view.findViewById(R.id.cardPromotionBanner);
        View btnPromoApply = view.findViewById(R.id.btnPromoApply);

        if (cardPromo != null) {
            cardPromo.setOnClickListener(v -> launchLoanForm());
        }
        if (btnPromoApply != null) {
            btnPromoApply.setOnClickListener(v -> launchLoanForm());
        }

        setupData();

        return view;
    }

    private void setupData() {
        String name = sharedManager.getString("NAME", "Asaan User");
        if (tvHomeUserName != null)
            tvHomeUserName.setText(name);
            
        String borrowerNo = sharedManager.getString("BORROWER_NO", "");
        if (tvBorrowerNumber != null) {
            if (!CommonFunctions.isNullOrEmpty(borrowerNo)) {
                tvBorrowerNumber.setText(getString(R.string.str_borrower_no) + borrowerNo);
                tvBorrowerNumber.setVisibility(View.VISIBLE);
            } else {
                tvBorrowerNumber.setVisibility(View.GONE);
            }
        }
    }

    private void launchLoanForm() {
        Intent intent = new Intent(requireContext(), LoanApplicationActivity.class);
        startActivity(intent);
    }

    public void updateLoanData(String status, String amount) {
        // Reserved for live loan status updates
    }
}
