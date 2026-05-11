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
import android.print.PrinterId;

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
        // Human-readable name is getPrinterName() (API 29+). PrinterId is API 21+ and always on the classpath.
        PrinterId printerId = info.getPrinterId();
        String printerRef =
                printerId != null ? printerId.getLocalId() : "(no PrinterId)";
        lines.add("  • " + info.getLabel());
        lines.add(
                "    state="
                        + stateName
                        + "  printerId="
                        + printerRef
                        + "  jobId="
                        + String.valueOf(job.getId()));
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
        int vendorSpecificCount = 0;
        for (UsbDevice d : devices) {
            if (looksLikeUsbPrinter(d)) {
                printerCount++;
            }
            if (hasVendorSpecificInterface(d)) {
                vendorSpecificCount++;
            }
        }
        sb.append('\n');
        sb.append("Likely USB printers (class 7 / interface printer): ").append(printerCount).append("\n");
        sb.append("Vendor-specific USB devices: ").append(vendorSpecificCount).append("\n");
        if (vendorSpecificCount > 0 && printerCount == 0) {
            sb.append("Note: One or more connected USB devices use vendor-specific interfaces and may require a proprietary driver or SDK.\n");
        }
        sb.append("Note: Standard USB printer commands are only likely to work if the device reports printer class or has printer interfaces.\n");
        return sb.toString().trim();
    }

    private static boolean hasVendorSpecificInterface(UsbDevice device) {
        for (int ci = 0; ci < device.getConfigurationCount(); ci++) {
            UsbConfiguration cfg = device.getConfiguration(ci);
            if (cfg == null) {
                continue;
            }
            for (int ii = 0; ii < cfg.getInterfaceCount(); ii++) {
                UsbInterface intf = cfg.getInterface(ii);
                if (intf.getInterfaceClass() == UsbConstants.USB_CLASS_VENDOR_SPEC) {
                    return true;
                }
            }
        }
        return false;
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
        StringBuilder builder = new StringBuilder();
        builder.append("  • ").append(name).append(printerHint).append("  VID=").append(vid).append(" PID=").append(pid).append("  ").append(d.getDeviceName()).append("\n");
        builder.append("    DeviceClass: 0x").append(String.format(Locale.US, "%02X", d.getDeviceClass())).append(" (" + usbClassName(d.getDeviceClass()) + ")\n");
        for (int ci = 0; ci < d.getConfigurationCount(); ci++) {
            UsbConfiguration cfg = d.getConfiguration(ci);
            if (cfg == null) {
                continue;
            }
            for (int ii = 0; ii < cfg.getInterfaceCount(); ii++) {
                UsbInterface intf = cfg.getInterface(ii);
                builder.append("    Interface ").append(ii).append(": class=0x").append(String.format(Locale.US, "%02X", intf.getInterfaceClass())).append(" (" + usbClassName(intf.getInterfaceClass()) + ")");
                builder.append(" subclass=0x").append(String.format(Locale.US, "%02X", intf.getInterfaceSubclass())).append(" protocol=0x").append(String.format(Locale.US, "%02X", intf.getInterfaceProtocol())).append(" endpoints=").append(intf.getEndpointCount()).append("\n");
            }
        }
        return builder.toString().trim();
    }

    private static String usbClassName(int usbClass) {
        switch (usbClass) {
            case UsbConstants.USB_CLASS_PRINTER:
                return "Printer";
            case UsbConstants.USB_CLASS_VENDOR_SPEC:
                return "Vendor Specific";
            case UsbConstants.USB_CLASS_COMM:
                return "Communications";
            case UsbConstants.USB_CLASS_HID:
                return "Human Interface Device";
            case UsbConstants.USB_CLASS_MASS_STORAGE:
                return "Mass Storage";
            case UsbConstants.USB_CLASS_HUB:
                return "Hub";
            case UsbConstants.USB_CLASS_AUDIO:
                return "Audio";
            case UsbConstants.USB_CLASS_CDC_DATA:
                return "CDC Data";
            case UsbConstants.USB_CLASS_VIDEO:
                return "Video";
            case UsbConstants.USB_CLASS_WIRELESS_CONTROLLER:
                return "Wireless";
            default:
                return usbClass == 0 ? "Device Defined" : "Unknown";
        }
    }
}
