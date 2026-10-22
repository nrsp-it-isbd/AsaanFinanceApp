package com.assan.finance.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import de.hdodenhof.circleimageview.CircleImageView;
import com.assan.finance.R;

import com.assan.finance.apis.ApiManager;
import com.assan.finance.sharedPreferences.SharedManager;
import com.assan.finance.utils.CommonFunctions;
import com.assan.finance.utils.Constants;
import android.app.Dialog;
import android.view.Window;
import com.assan.finance.fragments.UpdateProfileFragment;
import android.content.Intent;
import android.graphics.Color;

public class SettingsFragment extends Fragment {

    private CircleImageView ivSettingsProfile;
    private TextView tvSettingsName, tvSettingsId;
    private LinearLayout btnSettingsProfile, btnSettingsAbout, btnSettingsTerms, btnSettingsShare, btnSettingsLogout, btnSettingsMyOrders, btnSettingsFeedback, btnSettingsPrivacy, btnSettingsWhatsapp, btnSettingsContactUs;
    private SharedManager sharedManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        sharedManager = SharedManager.getInstance(requireContext());

        ivSettingsProfile = view.findViewById(R.id.ivSettingsProfile);
        tvSettingsName = view.findViewById(R.id.tvSettingsName);
        tvSettingsId = view.findViewById(R.id.tvSettingsId);

        btnSettingsProfile = view.findViewById(R.id.btnSettingsProfile);
        btnSettingsAbout = view.findViewById(R.id.btnSettingsAbout);
        btnSettingsTerms = view.findViewById(R.id.btnSettingsTerms);
        btnSettingsShare = view.findViewById(R.id.btnSettingsShare);
        btnSettingsLogout = view.findViewById(R.id.btnSettingsLogout);
        btnSettingsFeedback = view.findViewById(R.id.btnSettingsFeedback);
        btnSettingsPrivacy = view.findViewById(R.id.btnSettingsPrivacy);
        btnSettingsWhatsapp = view.findViewById(R.id.btnSettingsWhatsapp);
        btnSettingsContactUs = view.findViewById(R.id.btnSettingsContactUs);
        
        setupProfile();
        setupClickListeners();

        // Delete account (Google Play account-deletion requirement)
        View btnDeleteAccount = view.findViewById(R.id.btnSettingsDeleteAccount);
        if (btnDeleteAccount != null) {
            btnDeleteAccount.setOnClickListener(v ->
                    com.assan.finance.utils.AccountDeletionHelper.showDeleteAccountDialog(requireActivity()));
        }

        // Notification Popups Setting
        androidx.appcompat.widget.SwitchCompat switchPopups = view.findViewById(R.id.switchNotificationPopups);
        if (switchPopups != null) {
            boolean isPopupsEnabled = sharedManager.getBoolean("NOTIFICATION_POPUPS", true);
            switchPopups.setChecked(isPopupsEnabled);
            switchPopups.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sharedManager.saveBoolean("NOTIFICATION_POPUPS", isChecked);
            });
        }

        // Always Screen ON Setting
        androidx.appcompat.widget.SwitchCompat switchScreenOn = view.findViewById(R.id.switchScreenOn);
        if (switchScreenOn != null) {
            boolean isScreenOn = sharedManager.getBoolean("KEEP_SCREEN_ON", true);
            switchScreenOn.setChecked(isScreenOn);
            switchScreenOn.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sharedManager.saveBoolean("KEEP_SCREEN_ON", isChecked);
                if (getActivity() != null) {
                    if (isChecked) {
                        getActivity().getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } else {
                        getActivity().getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                }
            });
        }

        // Auto Logout Session Setting
        com.google.android.material.switchmaterial.SwitchMaterial switchAutoLogout = view.findViewById(R.id.switchAutoLogout);
        if (switchAutoLogout != null) {
            boolean isAutoLogout = sharedManager.getBoolean("AUTO_LOGOUT_ENABLED", false);
            switchAutoLogout.setChecked(isAutoLogout);
            switchAutoLogout.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sharedManager.saveBoolean("AUTO_LOGOUT_ENABLED", isChecked);
                if (!isChecked) {
                    com.assan.finance.AssanFinanceApp.Companion.cancelInactivityTimer();
                } else {
                    if (getActivity() != null) {
                        com.assan.finance.AssanFinanceApp.Companion.resetInactivityTimer(getActivity());
                    }
                }
            });
        }

        // Language Switch Setting
        com.google.android.material.switchmaterial.SwitchMaterial switchLanguage = view.findViewById(R.id.switchLanguage);
        if (switchLanguage != null) {
            String currentLang = com.assan.finance.utils.LocaleHelper.getLanguage(requireContext());
            switchLanguage.setChecked("ur".equals(currentLang));
            switchLanguage.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String newLang = isChecked ? "ur" : "en";
                com.assan.finance.utils.LocaleHelper.setLocale(requireContext(), newLang);
                if (getActivity() != null) {
                    getActivity().recreate();
                }
            });
        }

        // Dark Mode Setting
        androidx.appcompat.widget.SwitchCompat switchDarkMode = view.findViewById(R.id.switchDarkMode);
        if (switchDarkMode != null) {
            boolean isDarkMode = sharedManager.getBoolean("DARK_MODE", false);
            switchDarkMode.setChecked(isDarkMode);
            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sharedManager.saveBoolean("DARK_MODE", isChecked);
                // Delay so the switch animation completes smoothly before recreation
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (isChecked) {
                        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                    } else {
                        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                    }
                }, 300);
            });
        }

        // Header back button
        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) {
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            });
        }

        // Edit profile quick icon
        View btnEditProfile = view.findViewById(R.id.btnEditProfile);
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> navigate(new UpdateProfileFragment(), R.id.updateProfileFragment));
        }

        // App Version
        TextView tvAppVersion = view.findViewById(R.id.tvAppVersion);
        if (tvAppVersion != null) {
            tvAppVersion.setText(String.format(getString(R.string.app_version_label), com.assan.finance.BuildConfig.VERSION_NAME));
        }

        return view;
    }

    private void setupProfile() {
        String name = sharedManager.getString("NAME", "Asaan User");
        tvSettingsName.setText(name);
        
        if (Constants.CLIENT_ID != -1) {
            tvSettingsId.setText(getString(R.string.str_Asaan_id_colon) + Constants.CLIENT_ID);
        }

        String userProfileImage = sharedManager.getString("PROFILE_PICTURE", "");
        if (!CommonFunctions.isNullOrEmpty(userProfileImage)) {
            CommonFunctions.loadPicassoImage(userProfileImage, ivSettingsProfile);
        } else {
            ivSettingsProfile.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.dummy_profile_image));
        }
    }

    private void setupClickListeners() {
        if (btnSettingsProfile != null) btnSettingsProfile.setOnClickListener(v -> navigate(new UpdateProfileFragment(), R.id.updateProfileFragment));
        if (btnSettingsAbout != null) btnSettingsAbout.setOnClickListener(v -> showAboutUsDialog());
        if (btnSettingsTerms != null) btnSettingsTerms.setOnClickListener(v -> showTermsDialog());
        if (btnSettingsLogout != null) btnSettingsLogout.setOnClickListener(v -> CommonFunctions.showLogoutDialog(requireActivity()));
        
        if (btnSettingsFeedback != null) btnSettingsFeedback.setOnClickListener(v -> navigate(new com.assan.finance.fragments.FeedbackFragment(), R.id.feedbackFragment));
        if (btnSettingsPrivacy != null) btnSettingsPrivacy.setOnClickListener(v -> showPrivacyPolicyDialog());
        if (btnSettingsWhatsapp != null) btnSettingsWhatsapp.setOnClickListener(v -> openWhatsApp());
        if (btnSettingsContactUs != null) btnSettingsContactUs.setOnClickListener(v -> showContactUsDialog());        
        if (btnSettingsShare != null) {
            btnSettingsShare.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_app_subject));
                String shareMessage = getString(R.string.share_app_message);
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));
            });
        }
    }

    private void openWhatsApp() {
        String phoneNumber = Constants.SUPPORT_PHONE_INTL;
        String message = getString(R.string.whatsapp_support_msg);
        
        android.content.pm.PackageManager pm = requireContext().getPackageManager();
        boolean hasNormal = false;
        boolean hasBusiness = false;
        
        try {
            pm.getPackageInfo("com.whatsapp", android.content.pm.PackageManager.GET_ACTIVITIES);
            hasNormal = true;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {}
        
        try {
            pm.getPackageInfo("com.whatsapp.w4b", android.content.pm.PackageManager.GET_ACTIVITIES);
            hasBusiness = true;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {}
        
        if (hasNormal && hasBusiness) {
            com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
            View view = getLayoutInflater().inflate(R.layout.dialog_whatsapp_selector, null);
            dialog.setContentView(view);
            
            view.findViewById(R.id.btnNormal).setOnClickListener(v -> {
                dialog.dismiss();
                launchWhatsApp(phoneNumber, message, "com.whatsapp");
            });
            view.findViewById(R.id.btnBusiness).setOnClickListener(v -> {
                dialog.dismiss();
                launchWhatsApp(phoneNumber, message, "com.whatsapp.w4b");
            });
            dialog.show();
        } else if (hasNormal) {
            launchWhatsApp(phoneNumber, message, "com.whatsapp");
        } else if (hasBusiness) {
            launchWhatsApp(phoneNumber, message, "com.whatsapp.w4b");
        } else {
            CommonFunctions.showErrorDialog(requireActivity(), "WhatsApp is not installed on your device.");
        }
    }
    
    private void launchWhatsApp(String phoneNumber, String message, String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse("https://api.whatsapp.com/send?phone=" + phoneNumber + "&text=" + android.net.Uri.encode(message)));
            intent.setPackage(packageName);
            startActivity(intent);
        } catch (Exception e) {
            CommonFunctions.showErrorDialog(requireActivity(), "Failed to open WhatsApp.");
        }
    }

    private void showAboutUsDialog() {
        Dialog dialog = new Dialog(requireActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_about_us, null);
        dialog.setContentView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.horizontalMargin = 0.04f;
            dialog.getWindow().setAttributes(params);
        }
        
        dialog.findViewById(R.id.btnBack).setOnClickListener(v -> dialog.dismiss());
        TextView aboutUsTextView = dialog.findViewById(R.id.aboutUsTextView);
        
        androidx.appcompat.widget.SwitchCompat switchLanguage = dialog.findViewById(R.id.switchLanguage);
        
        TextView tvDialogTitle = dialog.findViewById(R.id.tvDialogTitle);
        
        Runnable updateText = () -> {
            if (switchLanguage.isChecked()) {
                String aboutUsUrdu = getString(R.string.aboutUsUrduText);
                aboutUsTextView.setText(androidx.core.text.HtmlCompat.fromHtml(aboutUsUrdu, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY));
                tvDialogTitle.setText(R.string.aboutUsUrduHeadingText);
            } else {
                String aboutUs = getString(R.string.aboutUsText);
                aboutUsTextView.setText(androidx.core.text.HtmlCompat.fromHtml(aboutUs, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY));
                tvDialogTitle.setText(getString(R.string.lbl_Asaan_finance_intro));
            }
        };
        
        String lang = com.assan.finance.utils.LocaleHelper.getLanguage(requireContext());
        switchLanguage.setChecked("ur".equals(lang));
        
        updateText.run();
        
        switchLanguage.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateText.run();
        });
        
        dialog.show();
    }

    private void showTermsDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_terms_conditions);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.horizontalMargin = 0.04f;
            dialog.getWindow().setAttributes(params);
        }
        
        dialog.findViewById(R.id.btnBack).setOnClickListener(v -> dialog.dismiss());
        TextView termsTextView = dialog.findViewById(R.id.termsTextView);
        termsTextView.setText(R.string.termsAndConditionsUrdu);
        
        dialog.show();
    }

    private void showPrivacyPolicyDialog() {
        Dialog dialog = new Dialog(requireActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_terms_conditions, null);
        dialog.setContentView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.horizontalMargin = 0.04f;
            dialog.getWindow().setAttributes(params);
        }
        
        dialog.findViewById(R.id.btnBack).setOnClickListener(v -> dialog.dismiss());
        
        TextView title = dialog.findViewById(R.id.tvDialogTitle);
        if (title != null) title.setText(R.string.auto_privacy_policy_640);
        
        TextView termsTextView = dialog.findViewById(R.id.termsTextView);
        String privacyText = getString(R.string.privacy_policy_text);
        
        if (termsTextView != null) {
            termsTextView.setText(androidx.core.text.HtmlCompat.fromHtml(privacyText, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY));
        }
        
        dialog.show();
    }

    private void showContactUsDialog() {
        Dialog dialog = new Dialog(requireActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_contact_us, null);
        dialog.setContentView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.horizontalMargin = 0.04f;
            dialog.getWindow().setAttributes(params);
        }
        
        TextView tvSupportPhone = dialog.findViewById(R.id.tvSupportPhone);
        TextView tvSupportEmail = dialog.findViewById(R.id.tvSupportEmail);
        if (tvSupportPhone != null) tvSupportPhone.setText(Constants.SUPPORT_PHONE);
        if (tvSupportEmail != null) tvSupportEmail.setText(Constants.SUPPORT_EMAIL);
        dialog.findViewById(R.id.btnBack).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void navigate(Fragment fragment, int destinationId) {
        if (getActivity() instanceof com.assan.finance.MainActivity) {
            try {
                androidx.navigation.fragment.NavHostFragment.findNavController(this)
                    .navigate(destinationId);
            } catch (Exception e) {
                requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .addToBackStack(null)
                    .commit();
            }
        }
    }

    private String readRawText(int resId) {
        java.io.InputStream inputStream = getResources().openRawResource(resId);
        java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
        int i;
        try {
            i = inputStream.read();
            while (i != -1) {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return byteArrayOutputStream.toString();
    }
}
