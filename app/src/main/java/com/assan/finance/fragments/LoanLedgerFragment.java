package com.assan.finance.fragments;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.assan.finance.R;
import com.assan.finance.adapters.AssanLedgerAdapter;
import com.assan.finance.apis.RetrofitClient;
import com.assan.finance.fragments.ManualPaymentBottomSheet;
import com.assan.finance.responseModel.ResponseModelBankChannels;
import com.assan.finance.responseModel.ResponseModelLedger;
import com.assan.finance.responseModel.ResponseModelLedgerAmount;
import com.assan.finance.sharedPreferences.SharedManager;
import com.assan.finance.utils.CommonFunctions;
import com.assan.finance.adapters.PaymentChannelsAdapter;
import com.assan.finance.adapters.ChannelCategoryAdapter;
import com.assan.finance.adapters.PaymentCategory;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import com.assan.finance.model.assan.PaymentChannel;
import com.assan.finance.model.assan.ResponseModelPaymentChannels;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoanLedgerFragment extends androidx.fragment.app.Fragment {

    private final androidx.activity.result.ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    downloadDepositSlip();
                } else {
                    if (isFragmentAlive()) {
                        CommonFunctions.showToast(requireActivity(), "Permission denied, cannot download.");
                    }
                }
            });

    private View view;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String BASE_URL = "http://103.172.140.103:2020/PF_Card.aspx?";
    private static final String PARAM_AC_YEAR = "acyear";
    private static final String PARAM_OFFICE = "office";
    private static final String PARAM_EMP_CODE = "empcode";
    private static final String TAG = LoanLedgerFragment.class.getSimpleName();
    SharedManager sharedManager;
    AssanLedgerAdapter assanLedgerAdapter;
    TextView numberOfReceipts;
    TextView TotalAmount;
    TextView txtNoReceipts;
    LinearLayout layoutParent;
    LinearLayout layoutLedgerData;
    String totalAmount = "0";
    TextView tvBorrowerName;
    TextView tvBorrowerNo;
    TextView tvProduct;
    TextView tvLoanAmount;
    TextView tvCnic;
    TextView tvTotalRecovered;
    String[] bankAgents;
    String[] bankChannels;
    NestedScrollView svParent;
    String phoneNumber = "";
    private RecyclerView ledgerRecyclerView;
    private SwipeRefreshLayout swipeRefreshLedger;
    private RecyclerView recyclerViewPaymentChannelCategories;
    private LinearLayout layoutPaymentChannelsHeader;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerLedger;
    private boolean isChannelsLoaded = false;
    private boolean isLedgerLoaded = false;
    private boolean isSummaryLoaded = false;
    private com.assan.finance.model.assan.BorrowerSummaryData borrowerSummaryData;
    private List<com.assan.finance.model.assan.AssanLedgerRecord> ledgerRecords;
    private List<com.assan.finance.model.assan.RecoveryScheduleRecord> recoverySchedules;
    private ImageView btnDownloadPdf;

    // Track active calls so we can cancel them on destroy
    private Call<ResponseModelLedger> ledgerCall;
    private Call<ResponseModelLedgerAmount> ledgerAmountCall;
    private Call<ResponseModelBankChannels> channelsCall;

    @Override
    public View onCreateView(@androidx.annotation.NonNull LayoutInflater inflater,
                             @androidx.annotation.Nullable ViewGroup container,
                             @androidx.annotation.Nullable Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_loan_ledger, container, false);
        initialization();
        showLoadingState();

        phoneNumber = sharedManager.getString("PHONE", "");
        String cnic = sharedManager.getString("CNIC", "");

        // Delay data fetching slightly so enter animations run smoothly at 60fps
        view.postDelayed(() -> {
            if (isFragmentAlive()) {
                getAllPaymentChannels();
                getLedgerDataByCnic(cnic);
            }
        }, 250);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel any in-flight network calls to prevent callbacks hitting destroyed views
        if (ledgerCall != null) ledgerCall.cancel();
        if (ledgerAmountCall != null) ledgerAmountCall.cancel();
        if (channelsCall != null) channelsCall.cancel();

        // Stop shimmer to release animation resources
        if (shimmerLedger != null) {
            shimmerLedger.stopShimmer();
            shimmerLedger.setVisibility(View.GONE);
        }
    }

    /**
     * Safe check: returns true only if Fragment is still alive and attached.
     * Use before any UI update inside a network callback.
     */
    private boolean isFragmentAlive() {
        return isAdded() && getContext() != null && !isDetached() && view != null;
    }

    private void downloadDepositSlipPdfFile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            downloadDepositSlip();
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else {
                downloadDepositSlip();
            }
        }
    }


    private void initialization() {
        svParent = view.findViewById(R.id.svParent);
        shimmerLedger = view.findViewById(R.id.shimmerLedger);
        tvBorrowerName = view.findViewById(R.id.tvBorrowerName);
        tvBorrowerNo = view.findViewById(R.id.tvBorrowerNo);
        tvProduct = view.findViewById(R.id.tvProduct);
        tvLoanAmount = view.findViewById(R.id.tvLoanAmount);
        tvCnic = view.findViewById(R.id.tvCnic);
        tvTotalRecovered = view.findViewById(R.id.tvTotalRecovered);
        btnDownloadPdf = view.findViewById(R.id.btnDownloadPdf);
        if (btnDownloadPdf != null) {
            btnDownloadPdf.setOnClickListener(v -> generateLedgerPdf(false));
        }

        ImageView btnViewDownloads = view.findViewById(R.id.btnViewDownloads);
        if (btnViewDownloads != null) {
            btnViewDownloads.setOnClickListener(v -> {
                try {
                    androidx.navigation.Navigation.findNavController(view).navigate(R.id.action_loanLedgerFragment_to_downloadsFragment);
                } catch (Exception e) {
                    CommonFunctions.showToast(requireActivity(), "Navigation error.");
                }
            });
        }

        // removed initProgressBar
        txtNoReceipts = view.findViewById(R.id.txtNoReceipts);
        layoutParent = view.findViewById(R.id.layoutParent);
        layoutLedgerData = view.findViewById(R.id.layoutLedgerData);
        TotalAmount = view.findViewById(R.id.TotalAmount);
        numberOfReceipts = view.findViewById(R.id.numberOfReceipts);
        sharedManager = SharedManager.getInstance(requireContext());
        ledgerRecyclerView = view.findViewById(R.id.ledgerRecyclerView);
        swipeRefreshLedger = view.findViewById(R.id.swipeRefreshLedger);
        recyclerViewPaymentChannelCategories = view.findViewById(R.id.recyclerViewPaymentChannelCategories);
        
        layoutPaymentChannelsHeader = view.findViewById(R.id.layoutPaymentChannelsHeader);

        recyclerViewPaymentChannelCategories.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        ledgerRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 1));

        if (swipeRefreshLedger != null) {
            swipeRefreshLedger.setOnRefreshListener(() -> {
                String cnic = sharedManager.getString("CNIC", "");
                isChannelsLoaded = false;
                isLedgerLoaded = false;
                isSummaryLoaded = false;
                getAllPaymentChannels();
                getLedgerDataByCnic(cnic);
            });
        }

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (getActivity() != null) {
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });
        bankAgents = new String[]{
                "EasyPaisa",
                "HBL-Konnect",
                "JazzCash",
                "Omni"
        };

        bankChannels = new String[]{
                "AKBL",
                "BAF",
                "BOP",
                "HBL",
                "HMPB_COC",
                "MANUAL",
                "MCB",
                "NBP_COC",
                "NRSP",
                "UBL_COC"
        };
    }

    private void showLoadingState() {
        isChannelsLoaded = false;
        isLedgerLoaded = false;
        isSummaryLoaded = false;
        if (svParent != null) {
            svParent.setVisibility(View.GONE);
        }
        if (shimmerLedger != null) {
            shimmerLedger.setVisibility(View.VISIBLE);
            shimmerLedger.startShimmer();
        }
    }

    private void checkAllDataLoaded() {
        // Guard: if fragment is gone, do nothing
        if (!isFragmentAlive()) return;

        if (isChannelsLoaded && isLedgerLoaded && isSummaryLoaded) {
            if (shimmerLedger != null) {
                shimmerLedger.stopShimmer();
                shimmerLedger.setVisibility(View.GONE);
            }
            if (svParent != null) {
                svParent.setVisibility(View.VISIBLE); // ALWAYS visible now
                if (txtNoReceipts != null && txtNoReceipts.getVisibility() == View.VISIBLE) {
                    if (btnDownloadPdf != null) btnDownloadPdf.setVisibility(View.GONE);
                    if (recyclerViewPaymentChannelCategories != null) recyclerViewPaymentChannelCategories.setVisibility(View.GONE);
                    if (layoutPaymentChannelsHeader != null) layoutPaymentChannelsHeader.setVisibility(View.GONE);
                } else {
                    // if (btnDownloadPdf != null) btnDownloadPdf.setVisibility(View.VISIBLE);
                    if (recyclerViewPaymentChannelCategories != null) recyclerViewPaymentChannelCategories.setVisibility(View.VISIBLE);
                    if (layoutPaymentChannelsHeader != null) layoutPaymentChannelsHeader.setVisibility(View.VISIBLE);
                }
            }

            if (swipeRefreshLedger != null && swipeRefreshLedger.isRefreshing()) {
                swipeRefreshLedger.setRefreshing(false);
            }
        }
    }

    private void getLedgerDataByCnic(String cnic) {
        if (cnic == null || cnic.isEmpty()) {
            CommonFunctions.showToast(requireActivity(), "CNIC not found.");
            txtNoReceipts.setVisibility(View.VISIBLE);
            layoutParent.setVisibility(View.GONE);
            isLedgerLoaded = true;
            isSummaryLoaded = true;
            checkAllDataLoaded();
            return;
        }

        RetrofitClient<com.assan.finance.network.assan.AssanApiService> retrofitClient = new RetrofitClient<>(com.assan.finance.network.assan.AssanApiService.class, requireContext());
        com.assan.finance.network.assan.AssanApiService apiService = retrofitClient.getApiService();
        Call<com.assan.finance.model.assan.ResponseModelAssanLedger> call = apiService.getLedgerByCnic(cnic);

        call.enqueue(new Callback<com.assan.finance.model.assan.ResponseModelAssanLedger>() {
            @Override
            public void onResponse(@NonNull Call<com.assan.finance.model.assan.ResponseModelAssanLedger> call, @NonNull Response<com.assan.finance.model.assan.ResponseModelAssanLedger> response) {
                if (!isFragmentAlive()) return;

                if (response.isSuccessful() && response.body() != null) {
                    com.assan.finance.model.assan.ResponseModelAssanLedger res = response.body();

                    if (res.getSuccess()) {
                        List<com.assan.finance.model.assan.AssanLedgerRecord> records = res.getRecords();
                        ledgerRecords = records;

                        if (records != null && !records.isEmpty() && records.get(0).getBorrowerNo() != null) {
                            String bNo = records.get(0).getBorrowerNo();
                            isLedgerLoaded = true;
                            getBorrowerSummary(bNo);
                            getFullRecoverySchedule(bNo);
                        } else {
                            txtNoReceipts.setVisibility(View.VISIBLE);
                            layoutLedgerData.setVisibility(View.GONE);
                            isLedgerLoaded = true;
                            isSummaryLoaded = true;
                            checkAllDataLoaded();
                        }
                    } else {
                        txtNoReceipts.setVisibility(View.VISIBLE);
                        layoutLedgerData.setVisibility(View.GONE);
                        isLedgerLoaded = true;
                        isSummaryLoaded = true;
                        checkAllDataLoaded();
                    }
                } else {
                    txtNoReceipts.setVisibility(View.VISIBLE);
                    layoutLedgerData.setVisibility(View.GONE);
                    isLedgerLoaded = true;
                    isSummaryLoaded = true;
                    checkAllDataLoaded();
                }
            }

            @Override
            public void onFailure(@NonNull Call<com.assan.finance.model.assan.ResponseModelAssanLedger> call, @NonNull Throwable t) {
                if (!isFragmentAlive()) return;
                CommonFunctions.handleApiFailure(requireActivity(), view, t);
                txtNoReceipts.setVisibility(View.VISIBLE);
                layoutLedgerData.setVisibility(View.GONE);
                isLedgerLoaded = true;
                isSummaryLoaded = true;
                checkAllDataLoaded();
            }
        });
    }

    private void getBorrowerSummary(String borrowerNo) {
        RetrofitClient<com.assan.finance.network.assan.AssanApiService> retrofitClient = new RetrofitClient<>(com.assan.finance.network.assan.AssanApiService.class, requireContext());
        Call<com.assan.finance.model.assan.ResponseModelBorrowerSummary> call = retrofitClient.getApiService().getBorrowerSummary(borrowerNo);
        call.enqueue(new Callback<com.assan.finance.model.assan.ResponseModelBorrowerSummary>() {
            @Override
            public void onResponse(@NonNull Call<com.assan.finance.model.assan.ResponseModelBorrowerSummary> call, @NonNull Response<com.assan.finance.model.assan.ResponseModelBorrowerSummary> response) {
                if (!isFragmentAlive()) return;
                if (response.isSuccessful() && response.body() != null && response.body().getSuccess()) {
                    borrowerSummaryData = response.body().getData();
                    if (borrowerSummaryData != null) {
                        String naText = getString(R.string.auto_n_a_723);
                        if (tvBorrowerName != null) tvBorrowerName.setText(borrowerSummaryData.getBorrowerName() != null ? borrowerSummaryData.getBorrowerName() : naText);
                        if (tvBorrowerNo != null) tvBorrowerNo.setText(borrowerSummaryData.getBorrowerNo() != null ? borrowerSummaryData.getBorrowerNo() : naText);
                        if (tvProduct != null) tvProduct.setText(borrowerSummaryData.getProductName() != null ? borrowerSummaryData.getProductName() : naText);
                        if (tvLoanAmount != null) tvLoanAmount.setText(String.format(java.util.Locale.US, "%,.0f %s", borrowerSummaryData.getLoanAmount(), getString(R.string.lbl_pkr)));
                        if (tvCnic != null) tvCnic.setText(borrowerSummaryData.getCnic() != null ? borrowerSummaryData.getCnic() : naText);
                        
                        double totalRec = borrowerSummaryData.getTotalRecoveredAmount() != null ? borrowerSummaryData.getTotalRecoveredAmount() : 0.0;
                        if (tvTotalRecovered != null) tvTotalRecovered.setText(String.format(java.util.Locale.US, "%,.0f %s", totalRec, getString(R.string.lbl_pkr)));
                        // FIX: this headline was never filled, so it showed the placeholder "Financing Details"
                        TextView tvRemaining = getView() != null ? getView().findViewById(R.id.TotalDueAmount) : null;
                        if (tvRemaining != null) {
                            double remaining = Math.max(0, borrowerSummaryData.getLoanAmount() - totalRec);
                            tvRemaining.setText(String.format(java.util.Locale.US, "%,.0f %s", remaining, getString(R.string.lbl_pkr)));
                        }
                        
                        
                        layoutLedgerData.setVisibility(View.VISIBLE);
                        txtNoReceipts.setVisibility(View.GONE);
                    }
                }
                isSummaryLoaded = true;
                checkAllDataLoaded();
            }

            @Override
            public void onFailure(@NonNull Call<com.assan.finance.model.assan.ResponseModelBorrowerSummary> call, @NonNull Throwable t) {
                if (!isFragmentAlive()) return;
                CommonFunctions.handleApiFailure(requireActivity(), view, t);
                isSummaryLoaded = true;
                checkAllDataLoaded();
            }
        });
    }

    private void getFullRecoverySchedule(String borrowerNo) {
        RetrofitClient<com.assan.finance.network.assan.AssanApiService> retrofitClient = new RetrofitClient<>(com.assan.finance.network.assan.AssanApiService.class, requireContext());
        Call<com.assan.finance.model.assan.ResponseModelRecoverySchedule> call = retrofitClient.getApiService().getFullRecoverySchedule(borrowerNo);
        call.enqueue(new Callback<com.assan.finance.model.assan.ResponseModelRecoverySchedule>() {
            @Override
            public void onResponse(@NonNull Call<com.assan.finance.model.assan.ResponseModelRecoverySchedule> call, @NonNull Response<com.assan.finance.model.assan.ResponseModelRecoverySchedule> response) {
                if (!isFragmentAlive()) return;
                TextView txtNoData = view.findViewById(R.id.txtNoDataFound);
                if (response.isSuccessful() && response.body() != null && response.body().getSuccess()) {
                    List<com.assan.finance.model.assan.RecoveryScheduleRecord> schedules = response.body().getRecords();
                    recoverySchedules = schedules;
                    if (schedules != null && !schedules.isEmpty()) {
                        assanLedgerAdapter = new AssanLedgerAdapter(schedules, item -> {
                            showRecoveryDetailsDialog(item);
                        });
                        ledgerRecyclerView.setAdapter(assanLedgerAdapter);
                        if (txtNoData != null) txtNoData.setVisibility(View.GONE);
                    } else {
                        if (txtNoData != null) txtNoData.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (txtNoData != null) txtNoData.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<com.assan.finance.model.assan.ResponseModelRecoverySchedule> call, @NonNull Throwable t) {
                if (!isFragmentAlive()) return;
                CommonFunctions.handleApiFailure(requireActivity(), view, t);
                TextView txtNoData = view.findViewById(R.id.txtNoDataFound);
                if (txtNoData != null) txtNoData.setVisibility(View.VISIBLE);
            }
        });
    }

    private void fetchRecoverySchedule(String borrowerNo, int installmentNo) {
        RetrofitClient<com.assan.finance.network.assan.AssanApiService> retrofitClient = new RetrofitClient<>(com.assan.finance.network.assan.AssanApiService.class, requireContext());
        Call<com.assan.finance.model.assan.ResponseModelRecoverySchedule> call = retrofitClient.getApiService().getRecoverySchedule(borrowerNo, installmentNo);
        call.enqueue(new Callback<com.assan.finance.model.assan.ResponseModelRecoverySchedule>() {
            @Override
            public void onResponse(@NonNull Call<com.assan.finance.model.assan.ResponseModelRecoverySchedule> call, @NonNull Response<com.assan.finance.model.assan.ResponseModelRecoverySchedule> response) {
                if (!isFragmentAlive()) return;
                if (response.isSuccessful() && response.body() != null && response.body().getSuccess() && response.body().getRecords() != null && !response.body().getRecords().isEmpty()) {
                    showRecoveryDetailsDialog(response.body().getRecords().get(0));
                } else {
                    CommonFunctions.showToast(requireActivity(), "Details not found for this installment");
                }
            }

            @Override
            public void onFailure(@NonNull Call<com.assan.finance.model.assan.ResponseModelRecoverySchedule> call, @NonNull Throwable t) {
                if (!isFragmentAlive()) return;
                CommonFunctions.handleApiFailure(requireActivity(), view, t);
            }
        });
    }

    private void showRecoveryDetailsDialog(com.assan.finance.model.assan.RecoveryScheduleRecord record) {
        android.app.Dialog dialog = new android.app.Dialog(requireActivity());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_ledger_details, null);
        dialog.setContentView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvDialogTitle = dialog.findViewById(R.id.tvDialogTitle);
        if (tvDialogTitle != null)
            tvDialogTitle.setText(getString(R.string.str_recovery_details_installment) + record.getInstallmentNo());

        setTextOrNa(dialog, R.id.tvReceiptNo, record.getReceiptNo());
        setTextOrNa(dialog, R.id.tvDueDateDialog, record.getDueDate() != null ? record.getDueDate().split("T")[0] : null);
        setTextOrNa(dialog, R.id.tvRecDateDialog, record.getReceiptDate() != null ? record.getReceiptDate().split("T")[0] : null);
        setTextOrNa(dialog, R.id.tvDescription, record.getStatus());

        setTextAmount(dialog, R.id.tvDuePA, record.getPrincipalDue());
        setTextAmount(dialog, R.id.tvRecPA, record.getRecoveredPrincipal());
        setTextAmount(dialog, R.id.tvDueSC, record.getServiceChargeDue());
        setTextAmount(dialog, R.id.tvRecSC, record.getRecoveredServiceCharge());
        setTextAmount(dialog, R.id.tvOdPa, record.getOutstandingPrincipal());
        setTextAmount(dialog, R.id.tvOdSc, record.getOutstandingServiceCharge());
        setTextAmount(dialog, R.id.tvDuePen, record.getPenaltyDue());
        setTextAmount(dialog, R.id.tvAdj, record.getExcessRecovery()); // Assuming ADJ is excess
        setTextAmount(dialog, R.id.tvWriteoff, 0.0); // Not in schedule API
        String naText = getString(R.string.auto_n_a_723);
        setTextOrNa(dialog, R.id.tvRefundCheque, naText);
        setTextOrNa(dialog, R.id.tvRefundDate, naText);
        setTextOrNa(dialog, R.id.tvRefundAmount, naText);
        setTextAmount(dialog, R.id.tvDueTotalD, record.getTotalDue());
        setTextAmount(dialog, R.id.tvRecTotalD, record.getRecoveredPrincipal() + record.getRecoveredServiceCharge());

        Button btnClose = dialog.findViewById(R.id.btnClose);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setTextOrNa(android.app.Dialog dialog, int viewId, String text) {
        TextView tv = dialog.findViewById(viewId);
        if (tv != null) {
            tv.setText(text != null && !text.isEmpty() ? text : getString(R.string.auto_n_a_723));
        }
    }

    private void setTextAmount(android.app.Dialog dialog, int viewId, Double amount) {
        TextView tv = dialog.findViewById(viewId);
        if (tv != null) {
            tv.setText(String.format(java.util.Locale.US, "%,.0f", amount != null ? amount : 0.0));
        }
    }

    private void getAllPaymentChannels() {
        RetrofitClient<com.assan.finance.network.assan.AssanApiService> retrofitClient = new RetrofitClient<>(com.assan.finance.network.assan.AssanApiService.class, requireContext());
        Call<ResponseModelPaymentChannels> call = retrofitClient.getApiService().getPaymentChannels();

        call.enqueue(new Callback<ResponseModelPaymentChannels>() {
            @Override
            public void onResponse(@NonNull Call<ResponseModelPaymentChannels> call, @NonNull Response<ResponseModelPaymentChannels> response) {
                if (!isFragmentAlive()) return;
                
                if (response.isSuccessful() && response.body() != null && response.body().getSuccess()) {
                    List<PaymentChannel> records = response.body().getRecords();
                    if (records != null && !records.isEmpty()) {
                        Map<String, List<PaymentChannel>> groupedChannels = new LinkedHashMap<>();
                        for (PaymentChannel channel : records) {
                            String type = channel.getChannelType() != null ? channel.getChannelType() : "Other";
                            if (!groupedChannels.containsKey(type)) {
                                groupedChannels.put(type, new ArrayList<>());
                            }
                            groupedChannels.get(type).add(channel);
                        }

                        List<PaymentCategory> categories = new ArrayList<>();
                        for (Map.Entry<String, List<PaymentChannel>> entry : groupedChannels.entrySet()) {
                            categories.add(new PaymentCategory(entry.getKey(), entry.getValue()));
                        }

                        if (!categories.isEmpty()) {
                            // Channels are display-only for now: the manual payment sheet still shows a placeholder
                            // master account (PK1234567890). Set ENABLE_MANUAL_PAYMENT = true once the real account is set.
                            final boolean ENABLE_MANUAL_PAYMENT = false;
                            ChannelCategoryAdapter categoryAdapter = new ChannelCategoryAdapter(categories, !ENABLE_MANUAL_PAYMENT ? null : new kotlin.jvm.functions.Function1<PaymentChannel, kotlin.Unit>() {
                                @Override
                                public kotlin.Unit invoke(PaymentChannel channel) {
                                    String bNo = tvBorrowerNo != null ? tvBorrowerNo.getText().toString() : "";
                                    String userCnic = tvCnic != null ? tvCnic.getText().toString() : "";
                                    ManualPaymentBottomSheet sheet = new ManualPaymentBottomSheet(channel, bNo, userCnic);
                                    sheet.show(getChildFragmentManager(), "ManualPaymentBottomSheet");
                                    return kotlin.Unit.INSTANCE;
                                }
                            });
                            recyclerViewPaymentChannelCategories.setAdapter(categoryAdapter);
                            layoutPaymentChannelsHeader.setVisibility(View.VISIBLE);
                        }
                    }
                }
                
                isChannelsLoaded = true;
                checkAllDataLoaded();
            }

            @Override
            public void onFailure(@NonNull Call<ResponseModelPaymentChannels> call, @NonNull Throwable t) {
                if (!isFragmentAlive()) return;
                isChannelsLoaded = true;
                checkAllDataLoaded();
                // Optionally show error, but we don't want to break the whole screen if just channels fail
                Log.e(TAG, "Failed to load payment channels: " + t.getMessage());
            }
        });
    }

    private void downloadDepositSlip() {
        // Keeping original logic intact just in case
        try {
            String acYear = "2022/23";
            String office = "0001";
            String empCode = "0001-0047";

            String url = BASE_URL + PARAM_AC_YEAR + "=" + Uri.encode(acYear) + "&" +
                    PARAM_OFFICE + "=" + Uri.encode(office) + "&" +
                    PARAM_EMP_CODE + "=" + Uri.encode(empCode);

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(getString(R.string.lbl_deposit_slip));
            request.setDescription("Downloading");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "DepositSlip.pdf");

            if (!isFragmentAlive()) return;
            CommonFunctions.showToast(requireActivity(), getString(R.string.downloading_deposit_slip));

            DownloadManager manager = (DownloadManager) requireActivity().getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
            } else {
                CommonFunctions.showToast(requireActivity(), "Failed to initialize DownloadManager.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Download error: " + e.getMessage());
        }
    }

    private void generateLedgerPdf(boolean shareInstead) {
        if (ledgerRecords == null || ledgerRecords.isEmpty()) {
            CommonFunctions.showToast(requireActivity(), "Please wait for data to load completely.");
            return;
        }

        // Request POST_NOTIFICATIONS permission on Android 13+ if not granted
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }

        final int NOTIFICATION_ID = 1001;
        final String CHANNEL_ID = "pdf_download_channel";
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(CHANNEL_ID, "Downloads", android.app.NotificationManager.IMPORTANCE_HIGH);
            if (notificationManager != null) notificationManager.createNotificationChannel(channel);
        }

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.download_file)
                .setContentTitle(shareInstead ? "Generating PDF for Sharing" : "Downloading Ledger")
                .setContentText("Generating PDF...")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setProgress(0, 0, true);

        if (notificationManager != null)
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        CommonFunctions.showToast(requireActivity(), "Generating PDF...");

        Context context = requireContext();
        androidx.fragment.app.FragmentActivity activity = getActivity();

        new Thread(() -> {
            java.io.File pdfFile = com.assan.finance.utils.LedgerPdfGenerator.generatePdf(context, borrowerSummaryData, recoverySchedules);

            if (activity != null && isAdded()) {
                activity.runOnUiThread(() -> {
                    if (pdfFile != null) {
                        if (shareInstead) {
                            try {
                                Uri uri = androidx.core.content.FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", pdfFile);
                                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                shareIntent.setType("application/pdf");
                                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                                shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(Intent.createChooser(shareIntent, "Share Ledger PDF"));
                            } catch (Exception e) {
                                CommonFunctions.showToast(activity, getString(R.string.msg_could_not_share_pdf));
                            }
                            if (notificationManager != null)
                                notificationManager.cancel(NOTIFICATION_ID);
                        } else {
                            Intent openIntent = new Intent(Intent.ACTION_VIEW);
                            Uri uri = androidx.core.content.FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", pdfFile);
                            openIntent.setDataAndType(uri, "application/pdf");
                            openIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

                            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(context, 0, openIntent, android.app.PendingIntent.FLAG_IMMUTABLE | android.app.PendingIntent.FLAG_UPDATE_CURRENT);

                            builder.setContentText("Download Complete: " + pdfFile.getName())
                                    .setProgress(0, 0, false)
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true);

                            if (notificationManager != null)
                                notificationManager.notify(NOTIFICATION_ID, builder.build());
                            CommonFunctions.showToast(activity, "Saved to Documents: " + pdfFile.getName());
                        }
                    } else {
                        if (!shareInstead) {
                            builder.setContentText("Failed to generate PDF")
                                    .setProgress(0, 0, false);
                            if (notificationManager != null)
                                notificationManager.notify(NOTIFICATION_ID, builder.build());
                        }
                        CommonFunctions.showToast(activity, getString(R.string.msg_failed_generate_pdf));
                    }
                });
            } else if (pdfFile != null && !shareInstead) {
                // If activity is dead but PDF generated, just update notification
                Intent openIntent = new Intent(Intent.ACTION_VIEW);
                Uri uri = androidx.core.content.FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", pdfFile);
                openIntent.setDataAndType(uri, "application/pdf");
                openIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(context, 0, openIntent, android.app.PendingIntent.FLAG_IMMUTABLE | android.app.PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentText("Download Complete: " + pdfFile.getName())
                        .setProgress(0, 0, false)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);
                if (notificationManager != null)
                    notificationManager.notify(NOTIFICATION_ID, builder.build());
            }
        }).start();
    }
}