package com.assan.finance.activities;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.assan.finance.R;
import com.assan.finance.sharedPreferences.SharedManager;
import com.assan.finance.utils.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * One launch screen. The system splash shows only the white background (no icon, see
 * Theme.AssanFinanceApp.Splash), then this screen builds the real Asaan Finance logo piece by piece:
 * the crescent swings in, the three bars grow up one after another, "Asaan Finance" is revealed
 * smoothly from left to right, and "Powered by NRSP" fades in at the bottom.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_MS = 2700;
    private static final long REVEAL_START_MS = 850;
    private static final long REVEAL_MS = 650;
    private static final long WORD_GAP_MS = 220;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Animator> running = new ArrayList<>();
    private boolean opened = false;
    private volatile Boolean rooted = null; // null = root check still running

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(com.assan.finance.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splash = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        // Hand over from the (empty) system splash at once: same white background
        splash.setOnExitAnimationListener(view -> view.remove());

        // Root check takes ~0.2 s: run it while the logo animates instead of before the first frame
        new Thread(() -> {
            boolean isRooted = new com.scottyab.rootbeer.RootBeer(getApplicationContext()).isRooted();
            rooted = isRooted;
            if (isRooted) runOnUiThread(this::showRootedDialog);
        }, "root-check").start();

        View stage = findViewById(R.id.logoStage);
        stage.post(this::playIntro);
        handler.postDelayed(this::openNext, SPLASH_MS);
    }

    private void showRootedDialog() {
        if (isFinishing()) return;
        handler.removeCallbacksAndMessages(null);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.msg_security_alert))
                .setMessage("This app cannot be run on a rooted device or emulator due to security policies.")
                .setCancelable(false)
                .setPositiveButton(getString(R.string.action_exit), (dialog, which) -> finishAffinity())
                .show();
    }

    private void playIntro() {
        float dp = getResources().getDisplayMetrics().density;
        PathInterpolator ease = new PathInterpolator(0.2f, 0f, 0f, 1f);

        // 1) Crescent swings in and settles
        View crescent = findViewById(R.id.ivSplashCrescent);
        crescent.setPivotX(crescent.getWidth() / 2f);
        crescent.setPivotY(crescent.getHeight() / 2f);
        AnimatorSet crescentIn = new AnimatorSet();
        crescentIn.playTogether(
                ObjectAnimator.ofFloat(crescent, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(crescent, View.ROTATION, -110f, 0f),
                ObjectAnimator.ofFloat(crescent, View.SCALE_X, 0.55f, 1f),
                ObjectAnimator.ofFloat(crescent, View.SCALE_Y, 0.55f, 1f));
        crescentIn.setDuration(700);
        crescentIn.setInterpolator(new OvershootInterpolator(1.1f));
        start(crescentIn);

        // 2) Bars grow up from the bottom, one after another (like a rising chart)
        int[] bars = {R.id.ivSplashBar1, R.id.ivSplashBar2, R.id.ivSplashBar3};
        for (int i = 0; i < bars.length; i++) {
            View bar = findViewById(bars[i]);
            bar.setPivotX(bar.getWidth() / 2f);
            bar.setPivotY(bar.getHeight());
            bar.setScaleY(0f);
            AnimatorSet grow = new AnimatorSet();
            grow.playTogether(
                    ObjectAnimator.ofFloat(bar, View.ALPHA, 0f, 1f),
                    ObjectAnimator.ofFloat(bar, View.SCALE_Y, 0f, 1f));
            grow.setStartDelay(380 + i * 130L);
            grow.setDuration(480);
            grow.setInterpolator(new OvershootInterpolator(2.2f));
            start(grow);
        }

        // 3) "Asaan Finance" is revealed smoothly from left to right, one word after the other
        View asaan = findViewById(R.id.ivSplashWordAsaan);
        View finance = findViewById(R.id.ivSplashWordFinance);
        reveal(asaan, REVEAL_START_MS);
        reveal(finance, REVEAL_START_MS + WORD_GAP_MS);
        long revealEnd = REVEAL_START_MS + WORD_GAP_MS + REVEAL_MS;

        // 4) Whole logo gives one soft "heartbeat" when the name is complete
        View stage = findViewById(R.id.logoStage);
        AnimatorSet beat = new AnimatorSet();
        beat.playTogether(
                ObjectAnimator.ofFloat(stage, View.SCALE_X, 1f, 1.05f, 1f),
                ObjectAnimator.ofFloat(stage, View.SCALE_Y, 1f, 1.05f, 1f));
        beat.setStartDelay(revealEnd);
        beat.setDuration(450);
        beat.setInterpolator(new DecelerateInterpolator());
        start(beat);

        // 5) Powered by NRSP
        View footer = findViewById(R.id.llFooterBranding);
        footer.setTranslationY(16f * dp);
        footer.animate().alpha(1f).translationY(0f)
                .setStartDelay(1000).setDuration(450).setInterpolator(ease).start();
    }

    /** Wipes the word in from left to right (clip grows smoothly) with a light fade and slide. */
    private void reveal(View word, long delayMs) {
        float dp = getResources().getDisplayMetrics().density;
        word.setAlpha(1f);
        word.setClipBounds(new Rect(0, 0, 0, word.getHeight()));
        word.setTranslationX(-6f * dp);
        android.animation.ValueAnimator wipe = android.animation.ValueAnimator.ofFloat(0f, 1f);
        wipe.setStartDelay(delayMs);
        wipe.setDuration(REVEAL_MS);
        wipe.setInterpolator(new PathInterpolator(0.25f, 0.1f, 0.25f, 1f));
        wipe.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            word.setClipBounds(new Rect(0, 0, Math.round(word.getWidth() * t), word.getHeight()));
            word.setTranslationX(-6f * dp * (1f - t));
        });
        wipe.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                word.setClipBounds(null);
            }
        });
        start(wipe);
    }

    private void start(Animator animator) {
        running.add(animator);
        animator.start();
    }

    private void openNext() {
        if (opened || isFinishing()) return;
        if (rooted == null) {                // root check not finished yet: wait a moment
            handler.postDelayed(this::openNext, 100);
            return;
        }
        if (rooted) return;                  // the rooted-device dialog is showing
        opened = true;
        boolean isFirstTime = SharedManager.getInstance(this).getBoolean(Constants.isFirstTimeLaunch, true);
        Intent intent = new Intent(this, isFirstTime ? OnboardingActivity.class : com.assan.finance.MainActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        for (Animator a : running) a.cancel();
        running.clear();
        super.onDestroy();
    }
}
