package com.client.printerutil;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.client.printerutil.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private enum Tab {
        DIAGNOSTICS,
        USB_TEST
    }

    private ActivityMainBinding binding;
    private Tab currentTab = Tab.DIAGNOSTICS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        binding.buttonDiagnosticsTab.setOnClickListener(v -> setTab(Tab.DIAGNOSTICS));
        binding.buttonUsbTestTab.setOnClickListener(v -> setTab(Tab.USB_TEST));
        binding.buttonRefresh.setOnClickListener(v -> refreshStatus());
        binding.buttonPrintSettings.setOnClickListener(v -> openPrintSettings());
        binding.buttonCopy.setOnClickListener(v -> copyToClipboard());

        setTab(Tab.DIAGNOSTICS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void setTab(Tab tab) {
        currentTab = tab;
        binding.buttonDiagnosticsTab.setEnabled(tab != Tab.DIAGNOSTICS);
        binding.buttonUsbTestTab.setEnabled(tab != Tab.USB_TEST);
        refreshStatus();
    }

    private void refreshStatus() {
        String text;
        if (currentTab == Tab.USB_TEST) {
            text = new UsbDiagnostics(this).buildUsbTestReport();
        } else {
            text = PrintingDiagnostics.buildReport(this);
        }
        binding.textStatus.setText(text);
    }

    private void copyToClipboard() {
        String text = binding.textStatus.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(this, "No content to copy", Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Printer Diagnostics", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Diagnostics copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void openPrintSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            startActivity(new Intent(Settings.ACTION_PRINT_SETTINGS));
        } else {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }
}
