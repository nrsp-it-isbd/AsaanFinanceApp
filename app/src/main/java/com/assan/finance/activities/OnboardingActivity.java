package com.assan.finance.activities;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

import com.assan.finance.R;
import com.assan.finance.activities.adapters.OnboardingAdapter;
import com.assan.finance.base.BaseActivity;
import com.assan.finance.sharedPreferences.SharedManager;
import com.assan.finance.utils.Constants;
import com.assan.finance.utils.LocaleHelper;

public class OnboardingActivity extends BaseActivity {

    private static final String STATE_PAGE = "page";

    private ViewPager2 viewPager;
    private OnboardingAdapter adapter;
    private LinearLayout layoutDots;
    private MaterialButton btnAction, btnSkip;
    private SharedManager sharedManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        // White status bar with dark icons, to match the white page
        getWindow().setStatusBarColor(android.graphics.Color.WHITE);
        new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);

        initialization();
        setupViewPager();
        buildDots();
        listeners();

        // keep the page when the language is switched (the screen is restarted)
        int page = savedInstanceState != null ? savedInstanceState.getInt(STATE_PAGE, 0)
                : getIntent().getIntExtra(STATE_PAGE, 0);
        viewPager.setCurrentItem(page, false);
        selectDot(page, false);
        updateButton(page);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PAGE, viewPager.getCurrentItem());
    }

    private void initialization() {
        viewPager = findViewById(R.id.onboardingViewPager);
        layoutDots = findViewById(R.id.layoutDots);
        btnAction = findViewById(R.id.btnOnboardingAction);
        btnSkip = findViewById(R.id.btnSkip);
        sharedManager = SharedManager.getInstance(this);
        adapter = new OnboardingAdapter(this);
    }

    private void setupViewPager() {
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(1);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                selectDot(position, true);
                updateButton(position);
            }
        });
    }

    /** Pill indicator: the active dot stretches to 24dp, the others are 8dp. */
    private void buildDots() {
        layoutDots.removeAllViews();
        float dp = getResources().getDisplayMetrics().density;
        for (int i = 0; i < adapter.getItemCount(); i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Math.round(8 * dp), Math.round(8 * dp));
            lp.setMarginStart(Math.round(4 * dp));
            lp.setMarginEnd(Math.round(4 * dp));
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(R.drawable.dot_inactive);
            layoutDots.addView(dot);
        }
    }

    private void selectDot(int current, boolean animate) {
        float dp = getResources().getDisplayMetrics().density;
        for (int i = 0; i < layoutDots.getChildCount(); i++) {
            View dot = layoutDots.getChildAt(i);
            boolean active = i == current;
            dot.setBackgroundResource(active ? R.drawable.dot_active : R.drawable.dot_inactive);
            int target = Math.round((active ? 24 : 8) * dp);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dot.getLayoutParams();
            if (!animate || lp.width == target) {
                lp.width = target;
                dot.setLayoutParams(lp);
                continue;
            }
            ValueAnimator anim = ValueAnimator.ofInt(lp.width, target);
            anim.setDuration(250);
            anim.addUpdateListener(a -> {
                lp.width = (int) a.getAnimatedValue();
                dot.setLayoutParams(lp);
            });
            anim.start();
        }
    }

    private void updateButton(int position) {
        boolean last = position == adapter.getItemCount() - 1;
        btnAction.setText(last ? R.string.auto_get_started_623 : R.string.str_next);
        btnSkip.setVisibility(last ? View.INVISIBLE : View.VISIBLE);
    }

    private void listeners() {
        btnAction.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() + 1 < adapter.getItemCount()) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                finishOnboarding();
            }
        });

        btnSkip.setOnClickListener(v -> finishOnboarding());

        // English / Urdu switch: the label shows the language you can switch to
        TextView tvLanguage = findViewById(R.id.tvOnboardingLanguage);
        tvLanguage.setText(R.string.onb_language_switch);
        tvLanguage.setOnClickListener(v -> {
            String next = "ur".equals(LocaleHelper.getLanguage(this)) ? "en" : "ur";
            LocaleHelper.setLocale(this, next);
            // recreate() keeps the old layout direction, so restart the screen to get Urdu right-to-left
            Intent restart = new Intent(this, OnboardingActivity.class).putExtra(STATE_PAGE, viewPager.getCurrentItem());
            startActivity(restart);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    private void finishOnboarding() {
        sharedManager.saveBoolean(Constants.isFirstTimeLaunch, false);
        startActivity(new Intent(OnboardingActivity.this, com.assan.finance.MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
