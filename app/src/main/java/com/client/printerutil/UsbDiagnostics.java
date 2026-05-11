package com.client.printerutil;

import android.content.Context;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;

import java.util.HashMap;

/**
 * USB diagnostics helper for API 25+ devices.
 * Enumerates USB devices, checks permissions, and provides device information.
 */
public class UsbDiagnostics {

    private final Context context;
    private final UsbManager usbManager;

    public UsbDiagnostics(Context context) {
        this.context = context;
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

    /**
     * Build a detailed USB diagnostics report.
     */
    public String buildUsbReport() {
        StringBuilder report = new StringBuilder();

        report.append("\n=== USB HOST API INFORMATION ===\n");
        report.append("API Level: ").append(Build.VERSION.SDK_INT).append("\n");
        report.append("USB Host Feature: ");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            boolean hasUsbHost = context.getPackageManager()
                    .hasSystemFeature("android.hardware.usb.host");
            report.append(hasUsbHost ? "SUPPORTED\n" : "NOT SUPPORTED\n");
        } else {
            report.append("API < 21, feature check unavailable\n");
        }

        report.append("\n--- USB Devices Enumeration ---\n");
        report.append(getUsbDevicesInfo());

        report.append("\n--- USB Accessories ---\n");
        report.append(getUsbAccessoriesInfo());

        report.append("\n--- Permission Status ---\n");
        report.append(getPermissionStatus());

        return report.toString();
    }

    /**
     * Get detailed information about all connected USB devices.
     */
    private String getUsbDevicesInfo() {
        if (usbManager == null) {
            return "USB Manager not available.\n";
        }

        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        if (deviceList.isEmpty()) {
            return "No USB devices found.\n";
        }

        StringBuilder info = new StringBuilder();
        info.append("Found ").append(deviceList.size()).append(" USB device(s):\n\n");

        for (UsbDevice device : deviceList.values()) {
            info.append("Device: ").append(device.getDeviceName()).append("\n");
            info.append("  Vendor ID: 0x").append(String.format("%04X", device.getVendorId())).append("\n");
            info.append("  Product ID: 0x").append(String.format("%04X", device.getProductId())).append("\n");
            info.append("  Device Class: 0x").append(String.format("%02X", device.getDeviceClass()));
            info.append(" (").append(getDeviceClassName(device.getDeviceClass())).append(")\n");
            info.append("  Device Subclass: 0x").append(String.format("%02X", device.getDeviceSubclass())).append("\n");
            info.append("  Device Protocol: 0x").append(String.format("%02X", device.getDeviceProtocol())).append("\n");
            info.append("  Serial Number: ").append(device.getSerialNumber() != null ? device.getSerialNumber() : "N/A").append("\n");
            info.append("  Manufacturer: ").append(device.getManufacturerName()).append("\n");
            info.append("  Product Name: ").append(device.getProductName()).append("\n");
            info.append("  Interfaces: ").append(device.getInterfaceCount()).append("\n");

            // Interface details
            for (int i = 0; i < device.getInterfaceCount(); i++) {
                info.append("    Interface ").append(i).append(":\n");
                info.append("      Class: 0x").append(String.format("%02X", device.getInterface(i).getInterfaceClass()));
                info.append(" (").append(getInterfaceClassName(device.getInterface(i).getInterfaceClass())).append(")\n");
                info.append("      Subclass: 0x").append(String.format("%02X", device.getInterface(i).getInterfaceSubclass())).append("\n");
                info.append("      Protocol: 0x").append(String.format("%02X", device.getInterface(i).getInterfaceProtocol())).append("\n");
                info.append("      Endpoint Count: ").append(device.getInterface(i).getEndpointCount()).append("\n");
            }

            // Check if app has permission
            boolean hasPermission = usbManager.hasPermission(device);
            info.append("  Has Permission: ").append(hasPermission ? "YES" : "NO").append("\n");

            info.append("\n");
        }

        return info.toString();
    }

    /**
     * Get information about USB accessories.
     */
    private String getUsbAccessoriesInfo() {
        if (usbManager == null) {
            return "USB Manager not available.\n";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsbAccessory[] accessories = usbManager.getAccessoryList();
            if (accessories == null || accessories.length == 0) {
                return "No USB accessories detected.\n";
            }

            StringBuilder info = new StringBuilder();
            info.append("Found ").append(accessories.length).append(" accessory(ies):\n");
            for (UsbAccessory acc : accessories) {
                info.append("  Accessory: ").append(acc.getDescription()).append("\n");
                info.append("    Manufacturer: ").append(acc.getManufacturer()).append("\n");
                info.append("    Model: ").append(acc.getModel()).append("\n");
            }
            return info.toString();
        }

        return "Accessory info available on API 21+\n";
    }

    /**
     * Get USB permission status for the app.
     */
    private String getPermissionStatus() {
        StringBuilder status = new StringBuilder();
        status.append("App Package: ").append(context.getPackageName()).append("\n");
        status.append("USB Backend: ");

        if (usbManager == null) {
            status.append("NOT AVAILABLE\n");
        } else {
            status.append("AVAILABLE\n");
            status.append("Connected Devices with Permission:\n");

            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            if (deviceList.isEmpty()) {
                status.append("  None\n");
            } else {
                for (UsbDevice device : deviceList.values()) {
                    if (usbManager.hasPermission(device)) {
                        status.append("  - ").append(device.getProductName()).append("\n");
                    }
                }
            }

            status.append("\nTo request permission for a device:\n");
            status.append("  1. Tap a device in the main report\n");
            status.append("  2. System will prompt for USB permission\n");
            status.append("  3. Tap 'Allow' to grant access\n");
        }

        return status.toString();
    }

    public String buildUsbTestReport() {
        if (usbManager == null) {
            return "USB diagnostics unavailable.\n";
        }

        StringBuilder report = new StringBuilder();
        report.append("\n=== USB PRINTER TEST ===\n");
        report.append("API Level: ").append(Build.VERSION.SDK_INT).append("\n");
        report.append("USB Host Feature: ");
        boolean hasUsbHost = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && context.getPackageManager().hasSystemFeature("android.hardware.usb.host");
        report.append(hasUsbHost ? "SUPPORTED\n" : "NOT SUPPORTED\n");

        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        if (deviceList.isEmpty()) {
            report.append("No USB devices detected.\n");
            return report.toString();
        }

        report.append("Found ").append(deviceList.size()).append(" USB device(s):\n\n");
        boolean hasStandardPrinter = false;
        boolean hasVendorSpecific = false;

        for (UsbDevice device : deviceList.values()) {
            report.append("Device: ").append(device.getDeviceName()).append("\n");
            report.append("  Vendor ID: 0x").append(String.format("%04X", device.getVendorId())).append("\n");
            report.append("  Product ID: 0x").append(String.format("%04X", device.getProductId())).append("\n");
            report.append("  Device Class: 0x").append(String.format("%02X", device.getDeviceClass()))
                    .append(" (" + getDeviceClassName(device.getDeviceClass()) + ")\n");
            report.append("  Interface count: ").append(device.getInterfaceCount()).append("\n");

            boolean standardPrinter = looksLikeUsbPrinter(device);
            boolean vendorSpecific = hasVendorSpecificInterface(device);
            boolean bulkEndpoints = hasBulkEndpoints(device);

            report.append("  Standard printer interface: ").append(standardPrinter ? "YES" : "NO").append("\n");
            report.append("  Vendor-specific interface: ").append(vendorSpecific ? "YES" : "NO").append("\n");
            report.append("  Bulk endpoints present: ").append(bulkEndpoints ? "YES" : "NO").append("\n");

            if (standardPrinter) {
                hasStandardPrinter = true;
            }
            if (vendorSpecific) {
                hasVendorSpecific = true;
            }

            for (int i = 0; i < device.getInterfaceCount(); i++) {
                UsbInterface intf = device.getInterface(i);
                report.append("    Interface ").append(i).append(": class=0x")
                        .append(String.format("%02X", intf.getInterfaceClass()))
                        .append(" (" + getInterfaceClassName(intf.getInterfaceClass()) + ")")
                        .append(" subclass=0x").append(String.format("%02X", intf.getInterfaceSubclass()))
                        .append(" protocol=0x").append(String.format("%02X", intf.getInterfaceProtocol()))
                        .append(" endpoints=").append(intf.getEndpointCount()).append("\n");
            }
            report.append("\n");
        }

        report.append("Summary:\n");
        report.append("  - Standard printer support detected: ").append(hasStandardPrinter ? "YES" : "NO").append("\n");
        report.append("  - Vendor-specific device surfaces: ").append(hasVendorSpecific ? "YES" : "NO").append("\n");
        report.append("\n");
        if (hasStandardPrinter) {
            report.append("This device exposes a standard printer interface. Generic USB printer APIs may work.\n");
        } else {
            report.append("This device does not expose a standard printer interface. Generic printer APIs are unlikely to work.\n");
        }
        if (hasVendorSpecific) {
            report.append("The connected device appears vendor-specific and likely requires a proprietary driver or SDK.\n");
        }
        report.append("If printing is required, the next useful checks are:\n");
        report.append("  1) Verify vendor SDK or driver documentation for the device.\n");
        report.append("  2) Test whether the device supports bulk IN/OUT transfers for custom protocol data.\n");
        report.append("  3) Compare with the existing SmartPosDemo app behavior to identify printer-specific commands.\n");

        return report.toString();
    }

    private boolean hasBulkEndpoints(UsbDevice device) {
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface intf = device.getInterface(i);
            for (int e = 0; e < intf.getEndpointCount(); e++) {
                if (intf.getEndpoint(e).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasVendorSpecificInterface(UsbDevice device) {
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface intf = device.getInterface(i);
            if (intf.getInterfaceClass() == UsbConstants.USB_CLASS_VENDOR_SPEC) {
                return true;
            }
        }
        return false;
    }

    private boolean looksLikeUsbPrinter(UsbDevice device) {
        if (device.getDeviceClass() == UsbConstants.USB_CLASS_PRINTER) {
            return true;
        }
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface intf = device.getInterface(i);
            if (intf.getInterfaceClass() == UsbConstants.USB_CLASS_PRINTER) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convert USB device class to human-readable name.
     */
    private String getDeviceClassName(int deviceClass) {
        switch (deviceClass) {
            case 0x00: return "Device Defined Class";
            case 0x01: return "Audio";
            case 0x02: return "Communications";
            case 0x03: return "Human Interface Device (HID)";
            case 0x04: return "Physical";
            case 0x05: return "Image";
            case 0x06: return "Printer";
            case 0x07: return "Mass Storage";
            case 0x08: return "Hub";
            case 0x09: return "CDC Data";
            case 0x0A: return "Smart Card";
            case 0x0B: return "Content Security";
            case 0x0D: return "Content";
            case 0x0E: return "Video";
            case 0x0F: return "Personal Healthcare";
            case 0xFF: return "Vendor Specific";
            default: return "Unknown (0x" + String.format("%02X", deviceClass) + ")";
        }
    }

    /**
     * Convert USB interface class to human-readable name.
     */
    private String getInterfaceClassName(int interfaceClass) {
        switch (interfaceClass) {
            case 0x00: return "Interface Defined Class";
            case 0x01: return "Audio";
            case 0x02: return "Communications Control";
            case 0x03: return "HID";
            case 0x04: return "Physical";
            case 0x05: return "Image";
            case 0x06: return "Printer";
            case 0x07: return "Mass Storage";
            case 0x08: return "Hub";
            case 0x09: return "CDC Data";
            case 0x0A: return "Smart Card";
            case 0x0B: return "Content Security";
            case 0x0E: return "Video";
            case 0x0F: return "Personal Healthcare";
            case 0xFF: return "Vendor Specific";
            default: return "Unknown (0x" + String.format("%02X", interfaceClass) + ")";
        }
    }
}
