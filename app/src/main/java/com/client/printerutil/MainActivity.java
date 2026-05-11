package com.client.printerutil;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.client.printerutil.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        binding.buttonRefresh.setOnClickListener(v -> refreshStatus());
        binding.buttonPrintSettings.setOnClickListener(v -> openPrintSettings());

        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        binding.textStatus.setText(PrintingDiagnostics.buildReport(this));
    }

    private void openPrintSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            startActivity(new Intent(Settings.ACTION_PRINT_SETTINGS));
        } else {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }
}
