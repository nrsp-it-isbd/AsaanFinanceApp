package com.assan.finance.activities.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.assan.finance.R;
import com.assan.finance.activities.fragments.OnboardingFragment;

/**
 * Onboarding pages. Each page shows a real screen of the app (or the logo on the welcome page),
 * a short badge, a title and one or two lines of text, all from string resources (English + Urdu).
 */
public class OnboardingAdapter extends FragmentStateAdapter {

    // image (0 = show the logo), badge icon, badge text, title, description
    private static final int[][] PAGES = {
            {0, R.drawable.ic_shield_check, R.string.onb1_badge, R.string.onb1_title, R.string.onb1_desc},
            {R.drawable.onb_shot_scan, R.drawable.ic_camera, R.string.onb2_badge, R.string.onb2_title, R.string.onb2_desc},
            {R.drawable.onb_shot_assistant, R.drawable.ic_mic, R.string.onb3_badge, R.string.onb3_title, R.string.onb3_desc},
            {R.drawable.onb_shot_ledger, R.drawable.ic_bell, R.string.onb4_badge, R.string.onb4_title, R.string.onb4_desc},
    };

    public OnboardingAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        int[] p = PAGES[position];
        return OnboardingFragment.newInstance(p[0], p[1], p[2], p[3], p[4]);
    }

    @Override
    public int getItemCount() {
        return PAGES.length;
    }
}
