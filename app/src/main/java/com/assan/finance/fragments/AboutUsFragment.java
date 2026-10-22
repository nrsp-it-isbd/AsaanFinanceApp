package com.assan.finance.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.assan.finance.R;
import com.assan.finance.utils.LocaleHelper;

import java.util.Locale;

public class AboutUsFragment extends Fragment implements TextToSpeech.OnInitListener {
    

    private ShimmerFrameLayout shimmerIntro;
    private View scrollIntroContent;
    private TextView aboutUsTextView;
    private FloatingActionButton fabPlayAudio;
    private TextView tvAudioStatus;
    private FrameLayout layoutVideoView;

    private TextToSpeech tts;
    private boolean isTtsPlaying = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about_us, container, false);

        shimmerIntro = view.findViewById(R.id.shimmerIntro);
        scrollIntroContent = view.findViewById(R.id.scrollIntroContent);
        aboutUsTextView = view.findViewById(R.id.aboutUsTextView);
        fabPlayAudio = view.findViewById(R.id.fabPlayAudio);
        tvAudioStatus = view.findViewById(R.id.tvAudioStatus);
        layoutVideoView = view.findViewById(R.id.layoutVideoView);

        // Header back
        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Setup TTS
        tts = new TextToSpeech(requireContext(), this);

        // Language specific written text
        String lang = LocaleHelper.getLanguage(requireContext());
        if ("ur".equals(lang)) {
            aboutUsTextView.setText(R.string.aboutUsUrduText);
        } else {
            String aboutUs = getString(R.string.aboutUsText);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                aboutUsTextView.setText(android.text.Html.fromHtml(aboutUs, android.text.Html.FROM_HTML_MODE_LEGACY));
            } else {
                aboutUsTextView.setText(android.text.Html.fromHtml(aboutUs));
            }
        }

        // Shimmer Loading Simulation (2 seconds)
        shimmerIntro.startShimmer();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) {
                shimmerIntro.stopShimmer();
                shimmerIntro.setVisibility(View.GONE);
                scrollIntroContent.setVisibility(View.VISIBLE);
            }
        }, 2000);

        // Audio controls
        fabPlayAudio.setOnClickListener(v -> toggleAudioIntro());

        // Video click simulation
        layoutVideoView.setOnClickListener(v -> {
            Toast.makeText(requireContext(), R.string.auto_opening_jhelum_team_progr_636, Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void toggleAudioIntro() {
        if (tts == null) return;

        if (isTtsPlaying) {
            tts.stop();
            isTtsPlaying = false;
            tvAudioStatus.setText(R.string.auto_press_play_to_listen_634);
        } else {
            String textToSpeak = aboutUsTextView.getText().toString();
            String lang = LocaleHelper.getLanguage(requireContext());
            if ("ur".equals(lang)) {
                tts.setLanguage(new Locale("ur", "PK"));
            } else {
                tts.setLanguage(Locale.US);
            }
            tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "IntroTTS");
            isTtsPlaying = true;
            tvAudioStatus.setText(R.string.auto_playing_introduction_audi_635);
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            String lang = LocaleHelper.getLanguage(requireContext());
            if ("ur".equals(lang)) {
                tts.setLanguage(new Locale("ur", "PK"));
            } else {
                tts.setLanguage(Locale.US);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}

