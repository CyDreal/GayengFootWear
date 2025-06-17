package com.example.gayeng.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.gayeng.R;
import com.example.gayeng.databinding.FragmentAboutUsBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AboutUsFragment extends Fragment {
    private WebView mapWebView;
    private FragmentAboutUsBinding binding;
    private BottomNavigationView bottomNav;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAboutUsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupMapWebView();
        setupToolbar();
        handleBottomNavVisibility(false);
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> {
            Navigation.findNavController(requireView()).navigateUp();
        });
    }

    private void handleBottomNavVisibility(boolean show) {
        if (getActivity() != null) {
            bottomNav = getActivity().findViewById(R.id.nav_view);
            if (bottomNav != null) {
                bottomNav.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void setupMapWebView() {
        mapWebView = binding.mapWebView;
        mapWebView.getSettings().setJavaScriptEnabled(true);

        // HTML content with iframe
        String htmlContent = "<iframe src=\"https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3960.104278818543!2d110.41351907446715!3d-6.996999368527376!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x2e708b6794166eb3%3A0x8f8964e5898d521e!2sJl.%20Lempongsari%20Timur%20III%20Jl.%20Veteran%20No.35%20B%2C%20Lempongsari%2C%20Kec.%20Gajahmungkur%2C%20Kota%20Semarang%2C%20Jawa%20Tengah%2050231!5e0!3m2!1sid!2sid!4v1750152040520!5m2!1sid!2sid\" width=\"600\" height=\"450\" style=\"border:0;\" allowfullscreen=\"\" loading=\"lazy\" referrerpolicy=\"no-referrer-when-downgrade\"></iframe>";

        // Load HTML content
        mapWebView.loadData(htmlContent, "text/html", "utf-8");
    }

    @Override
    public void onDestroyView() {
        handleBottomNavVisibility(true);
        super.onDestroyView();
        binding = null;
    }
}