package com.assan.finance.activities.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.assan.finance.R;

public class OnboardingFragment extends Fragment {

    private static final String ARG_IMAGE = "image";
    private static final String ARG_BADGE_ICON = "badge_icon";
    private static final String ARG_BADGE_TEXT = "badge_text";
    private static final String ARG_TITLE = "title";
    private static final String ARG_DESC = "desc";

    private View phone, logo, badge, title, desc;

    /** @param imageRes app screen to show, or 0 to show the logo (welcome page) */
    public static OnboardingFragment newInstance(int imageRes, int badgeIcon, int badgeText, int title, int desc) {
        OnboardingFragment fragment = new OnboardingFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_IMAGE, imageRes);
        args.putInt(ARG_BADGE_ICON, badgeIcon);
        args.putInt(ARG_BADGE_TEXT, badgeText);
        args.putInt(ARG_TITLE, title);
        args.putInt(ARG_DESC, desc);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding, container, false);

        View hero = view.findViewById(R.id.heroPanel);
        hero.setClipToOutline(true); // keep the screen inside the rounded panel

        ImageView phoneView = view.findViewById(R.id.imgOnboardingPhone);
        ImageView badgeIcon = view.findViewById(R.id.imgOnboardingBadge);
        TextView badgeText = view.findViewById(R.id.txtOnboardingBadge);
        TextView titleView = view.findViewById(R.id.txtTitleOnboarding);
        TextView descView = view.findViewById(R.id.txtDescriptionOnboarding);
        phone = phoneView;
        logo = view.findViewById(R.id.layoutOnboardingLogo);
        badge = view.findViewById(R.id.cardOnboardingBadge);
        title = titleView;
        desc = descView;

        Bundle a = requireArguments();
        int image = a.getInt(ARG_IMAGE);
        if (image == 0) {
            phoneView.setVisibility(View.GONE);
            logo.setVisibility(View.VISIBLE);
            badge.setVisibility(View.GONE); // the logo page needs no badge (it would cover the logo)
        } else {
            phoneView.setImageResource(image);
        }
        badgeIcon.setImageResource(a.getInt(ARG_BADGE_ICON));
        badgeText.setText(a.getInt(ARG_BADGE_TEXT));
        titleView.setText(a.getInt(ARG_TITLE));
        descView.setText(a.getInt(ARG_DESC));
        return view;
    }

    /** ViewPager2 resumes only the visible page, so each page animates in when it is shown. */
    @Override
    public void onResume() {
        super.onResume();
        float dp = getResources().getDisplayMetrics().density;
        PathInterpolator ease = new PathInterpolator(0.2f, 0f, 0f, 1f);

        View hero = phone.getVisibility() == View.VISIBLE ? phone : logo;
        hero.setAlpha(0f);
        hero.setTranslationY(48f * dp);
        hero.animate().alpha(1f).translationY(0f).setDuration(550).setInterpolator(ease).start();

        if (badge.getVisibility() == View.VISIBLE) {
            badge.setAlpha(0f);
            badge.setScaleX(0.6f);
            badge.setScaleY(0.6f);
            badge.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(300).setDuration(400)
                    .setInterpolator(new OvershootInterpolator(2f)).start();
        }

        View[] texts = {title, desc};
        for (int i = 0; i < texts.length; i++) {
            texts[i].setAlpha(0f);
            texts[i].setTranslationY(14f * dp);
            texts[i].animate().alpha(1f).translationY(0f).setStartDelay(120 + i * 90L)
                    .setDuration(450).setInterpolator(ease).start();
        }
    }
}
