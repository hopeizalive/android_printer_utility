package com.client.printerutil;

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
        binding.buttonCopy.setOnClickListener(v -> copyToClipboard());

        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        String diagnosticReport = PrintingDiagnostics.buildReport(this);
        String usbReport = new UsbDiagnostics(this).buildUsbReport();
        binding.textStatus.setText(diagnosticReport + usbReport);
    }

    private void copyToClipboard() {
        String text = binding.textStatus.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(this, "No content to copy", Toast.LENGTH_SHORT).show();
            return;
        }

        android.content.ClipboardManager clipboard = 
            (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Printer Diagnostics", text);
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
