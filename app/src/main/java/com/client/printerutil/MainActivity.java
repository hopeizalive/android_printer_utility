package com.client.printerutil;



import android.app.PendingIntent;

import android.content.BroadcastReceiver;

import android.content.ClipData;

import android.content.ClipboardManager;

import android.content.Context;

import android.content.Intent;

import android.content.IntentFilter;

import android.hardware.usb.UsbDevice;

import android.hardware.usb.UsbManager;

import android.os.Build;

import android.os.Bundle;

import android.provider.Settings;

import android.widget.Toast;



import androidx.appcompat.app.AppCompatActivity;

import androidx.core.view.WindowCompat;



import com.client.printerutil.databinding.ActivityMainBinding;



public class MainActivity extends AppCompatActivity {



    private static final String ACTION_USB_PERMISSION = "com.client.printerutil.action.USB_PERMISSION";



    private enum Tab {

        DIAGNOSTICS,

        USB_TEST

    }



    private ActivityMainBinding binding;

    private Tab currentTab = Tab.DIAGNOSTICS;



    private final BroadcastReceiver usbPermissionReceiver =

            new BroadcastReceiver() {

                @Override

                public void onReceive(Context context, Intent intent) {

                    if (!ACTION_USB_PERMISSION.equals(intent.getAction())) {

                        return;

                    }

                    UsbDevice device = readUsbDeviceExtra(intent);

                    boolean granted =

                            intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);

                    if (granted && device != null) {

                        runPrintInBackground(device);

                    } else {

                        Toast.makeText(MainActivity.this, "USB permission denied", Toast.LENGTH_SHORT)

                                .show();

                        setPrintStatusIcon(false);

                    }

                }

            };



    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        binding = ActivityMainBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);



        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            registerReceiver(usbPermissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        } else {

            registerReceiver(usbPermissionReceiver, filter);

        }



        binding.buttonDiagnosticsTab.setOnClickListener(v -> setTab(Tab.DIAGNOSTICS));

        binding.buttonUsbTestTab.setOnClickListener(v -> setTab(Tab.USB_TEST));

        binding.buttonRefresh.setOnClickListener(v -> refreshStatus());

        binding.buttonPrintSettings.setOnClickListener(v -> openPrintSettings());

        binding.buttonCopy.setOnClickListener(v -> copyToClipboard());

        binding.buttonPrintTestReceipt.setOnClickListener(v -> onPrintTestReceiptClicked());



        setTab(Tab.DIAGNOSTICS);

    }



    @Override

    protected void onDestroy() {

        super.onDestroy();

        unregisterReceiver(usbPermissionReceiver);

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



    private void onPrintTestReceiptClicked() {

        UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);

        UsbDevice printer = UsbStandardPrinter.findFirstStandardPrinter(usbManager);

        if (printer == null) {

            setPrintStatusIcon(false);

            Toast.makeText(

                            this,

                            "No USB class-7 printer found. Connect a standard USB printer and tap Refresh.",

                            Toast.LENGTH_LONG)

                    .show();

            return;

        }

        if (!usbManager.hasPermission(printer)) {

            PendingIntent permissionIntent = buildUsbPermissionPendingIntent();

            usbManager.requestPermission(printer, permissionIntent);

            Toast.makeText(this, "Allow USB access, then printing runs automatically.", Toast.LENGTH_LONG)

                    .show();

            return;

        }

        runPrintInBackground(printer);

    }



    private PendingIntent buildUsbPermissionPendingIntent() {

        Intent intent = new Intent(ACTION_USB_PERMISSION);

        intent.setPackage(getPackageName());

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            flags |= PendingIntent.FLAG_IMMUTABLE;

        }

        return PendingIntent.getBroadcast(this, 0, intent, flags);

    }



    private void runPrintInBackground(UsbDevice device) {

        setPrintStatusIcon(false);

        UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);

        new Thread(

                        () -> {

                            UsbStandardPrinter.PrintResult result =

                                    UsbStandardPrinter.printTestReceipt(usbManager, device);

                            runOnUiThread(

                                    () -> {

                                        Toast.makeText(MainActivity.this, result.message, Toast.LENGTH_LONG)

                                                .show();

                                        setPrintStatusIcon(result.success);

                                        refreshStatus();

                                    });

                        })

                .start();

    }



    /** Small status badge next to Print test receipt: failure until a print completes successfully. */

    private void setPrintStatusIcon(boolean success) {

        binding.imagePrintStatus.setImageResource(

                success ? R.drawable.ic_print_status_ok : R.drawable.ic_print_status_failed);

        binding.imagePrintStatus.setContentDescription(

                getString(

                        success ? R.string.print_status_ok_cd : R.string.print_status_failed_cd));

    }



    @SuppressWarnings("deprecation")

    private UsbDevice readUsbDeviceExtra(Intent intent) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            return intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice.class);

        }

        return intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

    }

}

