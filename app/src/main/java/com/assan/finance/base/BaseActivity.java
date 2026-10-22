package com.assan.finance.base;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;

import com.assan.finance.R;
import com.assan.finance.utils.CertificateUpdater;
import com.assan.finance.utils.CommonFunctions;
import com.assan.finance.utils.LocaleHelper;
import android.content.Context;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CertificateUpdater.checkAndDownloadCertificate(this);
        Window window = getWindow();
// window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false);
        // FIX: drawing behind the system bars hid the bottom buttons (Next, Submit, Capture) under the
        // 3-button navigation bar on many phones, and the keyboard covered form fields. Keep the
        // content clear of the status bar, navigation bar, display cutout and keyboard.
        android.view.View content = window.getDecorView().findViewById(android.R.id.content);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
                            | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            androidx.core.graphics.Insets ime = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return androidx.core.view.WindowInsetsCompat.CONSUMED;
        });
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        // FLAG_TRANSLUCENT_STATUS is deprecated, clearFlags still works but we can just skip it or keep it with SuppressWarnings
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorPrimaryDark));
        window.setNavigationBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
        
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @SuppressWarnings("deprecation")
            @Override
            public void handleOnBackPressed() {
                finish();
                if (android.os.Build.VERSION.SDK_INT >= 34) {
                    overrideActivityTransition(android.app.Activity.OVERRIDE_TRANSITION_CLOSE, R.anim.slide_in_left, R.anim.slide_out_right);
                } else {
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void setToolBar(Toolbar toolbar, AppCompatTextView heading) {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(view -> CommonFunctions.onBackFinish(this));
        heading.setText(getResources().getString(R.string.loan_application_status));
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        com.assan.finance.AssanFinanceApp.Companion.resetInactivityTimer(this);
        return super.dispatchTouchEvent(ev);
    }
}
