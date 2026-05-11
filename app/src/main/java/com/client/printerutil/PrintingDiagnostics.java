package com.client.printerutil;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.print.PrintJob;
import android.print.PrintJobInfo;
import android.print.PrintManager;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Collects Android printing capability and connection hints for the main app.
 * USB class-7 devices are reported as likely printers; network printers usually
 * appear only after a vendor print service or driver is installed.
 */
public final class PrintingDiagnostics {

    private PrintingDiagnostics() {
    }

    public static String buildReport(Context context) {
        PackageManager pm = context.getPackageManager();
        List<String> lines = new ArrayList<>();

        lines.add("Device: " + Build.MANUFACTURER + " " + Build.MODEL);
        lines.add("Android API: " + Build.VERSION.SDK_INT + " (" + Build.VERSION.RELEASE + ")");
        lines.add("");

        boolean hasPrintingFeature = pm.hasSystemFeature(PackageManager.FEATURE_PRINTING);
        lines.add("System printing feature (FEATURE_PRINTING): " + hasPrintingFeature);

        PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);
        lines.add("PrintManager available: " + (printManager != null));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lines.add("");
            lines.add("Recent / active print jobs (app scope, API 28+):");
            List<PrintJob> jobs = printManager.getPrintJobs();
            if (jobs.isEmpty()) {
                lines.add("  (none reported — normal if nothing printed from this device session)");
            } else {
                for (PrintJob job : jobs) {
                    appendPrintJob(lines, job);
                }
            }
        } else {
            lines.add("");
            lines.add("Print job listing requires API 28+ (current device is lower).");
        }

        lines.add("");
        lines.add(usbSection(context));

        lines.add("");
        lines.add("Notes:");
        lines.add("- Wi‑Fi / IPP printers often need the manufacturer print service app.");
        lines.add("- Thermal Bluetooth printers need Bluetooth permissions and vendor SDKs.");
        lines.add("- Use Refresh after plugging USB; grant USB permission if the system prompts.");

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                out.append('\n');
            }
            out.append(lines.get(i));
        }
        return out.toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private static void appendPrintJob(List<String> lines, PrintJob job) {
        PrintJobInfo info = job.getInfo();
        String stateName = printJobStateName(info.getState());
        String printer;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String pn = info.getPrinterName();
            printer = pn != null ? pn : "(printer name N/A)";
        } else {
            printer = "(printer name requires API 29+)";
        }
        lines.add("  • " + info.getLabel());
        lines.add("    state=" + stateName + "  printer=" + printer + "  id=" + String.valueOf(job.getId()));
    }

    private static String printJobStateName(int state) {
        switch (state) {
            case PrintJobInfo.STATE_QUEUED:
                return "QUEUED";
            case PrintJobInfo.STATE_STARTED:
                return "STARTED";
            case PrintJobInfo.STATE_BLOCKED:
                return "BLOCKED";
            case PrintJobInfo.STATE_COMPLETED:
                return "COMPLETED";
            case PrintJobInfo.STATE_FAILED:
                return "FAILED";
            case PrintJobInfo.STATE_CANCELED:
                return "CANCELED";
            default:
                return "UNKNOWN(" + state + ")";
        }
    }

    private static String usbSection(Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        Collection<UsbDevice> devices = usbManager.getDeviceList().values();
        if (devices.isEmpty()) {
            return "USB: no USB devices detected by UsbManager.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("USB devices (").append(devices.size()).append("):\n");
        for (UsbDevice d : devices) {
            sb.append(describeUsbDevice(d)).append('\n');
        }
        int printerCount = 0;
        for (UsbDevice d : devices) {
            if (looksLikeUsbPrinter(d)) {
                printerCount++;
            }
        }
        sb.append('\n');
        sb.append("Likely USB printers (class 7 / interface printer): ").append(printerCount);
        return sb.toString().trim();
    }

    private static boolean looksLikeUsbPrinter(UsbDevice device) {
        if (device.getDeviceClass() == UsbConstants.USB_CLASS_PRINTER) {
            return true;
        }
        for (int ci = 0; ci < device.getConfigurationCount(); ci++) {
            UsbConfiguration cfg = device.getConfiguration(ci);
            if (cfg == null) {
                continue;
            }
            for (int ii = 0; ii < cfg.getInterfaceCount(); ii++) {
                UsbInterface intf = cfg.getInterface(ii);
                if (intf.getInterfaceClass() == UsbConstants.USB_CLASS_PRINTER) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String describeUsbDevice(UsbDevice d) {
        String vid = String.format(Locale.US, "0x%04X", d.getVendorId());
        String pid = String.format(Locale.US, "0x%04X", d.getProductId());
        String printerHint = looksLikeUsbPrinter(d) ? " [printer-class]" : "";
        String name;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CharSequence pn = d.getProductName();
            name = pn != null ? pn.toString() : d.getDeviceName();
        } else {
            name = d.getDeviceName();
        }
        return "  • " + name + printerHint + "  VID=" + vid + " PID=" + pid + "  " + d.getDeviceName();
    }
}
