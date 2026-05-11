package com.client.printerutil;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;

import java.util.HashMap;
import java.util.List;

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

            for (int i = 0; i < device.getInterfaceCount(); i++) {
                UsbInterface intf = device.getInterface(i);
                info.append("    Interface ").append(i).append(":\n");
                info.append("      Class: 0x").append(String.format("%02X", intf.getInterfaceClass()));
                info.append(" (").append(getInterfaceClassName(intf.getInterfaceClass())).append(")\n");
                info.append("      Subclass: 0x").append(String.format("%02X", intf.getInterfaceSubclass())).append("\n");
                info.append("      Protocol: 0x").append(String.format("%02X", intf.getInterfaceProtocol())).append("\n");
                info.append("      Endpoint Count: ").append(intf.getEndpointCount()).append("\n");
                for (int e = 0; e < intf.getEndpointCount(); e++) {
                    UsbEndpoint endpoint = intf.getEndpoint(e);
                    info.append("        Endpoint ").append(e).append(": type=")
                            .append(endpointTypeName(endpoint.getType()))
                            .append(" direction=")
                            .append(endpointDirectionName(endpoint.getDirection()))
                            .append(" packetSize=")
                            .append(endpoint.getMaxPacketSize())
                            .append("\n");
                }
            }

            boolean hasPermission = usbManager.hasPermission(device);
            info.append("  Has Permission: ").append(hasPermission ? "YES" : "NO").append("\n\n");
        }

        return info.toString();
    }

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
                    .append(" (").append(getDeviceClassName(device.getDeviceClass())).append(")\n");
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

            report.append("  Usb Probe: ").append(runUsbProbe(device)).append("\n");
            report.append("\n");
        }

        report.append("Summary:\n");
        report.append("  - Standard printer support detected: ").append(hasStandardPrinter ? "YES" : "NO").append("\n");
        report.append("  - Vendor-specific device surfaces: ").append(hasVendorSpecific ? "YES" : "NO").append("\n\n");
        if (hasStandardPrinter) {
            report.append("This device exposes a standard printer interface. Generic USB printer APIs may work.\n");
        } else {
            report.append("This device does not expose a standard printer interface. Generic printer APIs are unlikely to work.\n");
        }
        if (hasVendorSpecific) {
            report.append("The connected device appears vendor-specific and likely requires a proprietary driver or SDK.\n");
        }
        report.append("\n");
        report.append(getPackageInspectionInfo());
        report.append("\nIf printing is required, the next useful checks are:\n");
        report.append("  1) Verify vendor SDK or driver documentation for the device.\n");
        report.append("  2) Test whether the device supports bulk IN/OUT transfers for custom protocol data.\n");
        report.append("  3) Compare with the existing SmartPosDemo app behavior to identify printer-specific commands.\n");

        return report.toString();
    }

    private String runUsbProbe(UsbDevice device) {
        if (!usbManager.hasPermission(device)) {
            return "NO PERMISSION";
        }

        UsbDeviceConnection connection = usbManager.openDevice(device);
        if (connection == null) {
            return "FAILED TO OPEN DEVICE";
        }

        StringBuilder probeResult = new StringBuilder();
        boolean anyClaimed = false;

        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface intf = device.getInterface(i);
            if (!connection.claimInterface(intf, true)) {
                probeResult.append("    Interface ").append(i).append(" claim failed\n");
                continue;
            }
            anyClaimed = true;
            probeResult.append("    Interface ").append(i).append(" claimed\n");

            for (int e = 0; e < intf.getEndpointCount(); e++) {
                UsbEndpoint endpoint = intf.getEndpoint(e);
                if (endpoint.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    continue;
                }
                if (endpoint.getDirection() != UsbConstants.USB_DIR_OUT) {
                    continue;
                }
                byte[] testData = new byte[0];
                int result = connection.bulkTransfer(endpoint, testData, 0, 200);
                probeResult.append("      OUT endpoint ").append(e).append(" test result=").append(result).append("\n");
                if (result >= 0) {
                    probeResult.append("      Bulk OUT probe appears accepted.\n");
                    connection.releaseInterface(intf);
                    connection.close();
                    return probeResult.toString().trim();
                }
            }
            connection.releaseInterface(intf);
        }

        connection.close();
        if (!anyClaimed) {
            return "NO CLAIMABLE INTERFACES";
        }
        return probeResult.append("    No bulk OUT probe succeeded\n").toString().trim();
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

    private String getPackageInspectionInfo() {
        StringBuilder info = new StringBuilder();
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);

        info.append("\n--- Vendor/Driver Package Inspection ---\n");
        int found = 0;
        for (PackageInfo pkg : packages) {
            String name = pkg.packageName.toLowerCase();
            if (name.contains("dukkentek") || name.contains("smartpos") || name.contains("paydevice") || name.contains("printer") || name.contains("pos")) {
                CharSequence label = pkg.applicationInfo.loadLabel(pm);
                info.append("  - ").append(pkg.packageName).append(" (" ).append(label).append(")\n");
                found++;
            }
        }
        if (found == 0) {
            info.append("  No likely vendor or printer driver packages found by name.\n");
        }

        return info.toString();
    }

    private String endpointTypeName(int type) {
        switch (type) {
            case UsbConstants.USB_ENDPOINT_XFER_CONTROL:
                return "CONTROL";
            case UsbConstants.USB_ENDPOINT_XFER_ISOC:
                return "ISOCHRONOUS";
            case UsbConstants.USB_ENDPOINT_XFER_BULK:
                return "BULK";
            case UsbConstants.USB_ENDPOINT_XFER_INT:
                return "INTERRUPT";
            default:
                return "UNKNOWN";
        }
    }

    private String endpointDirectionName(int direction) {
        switch (direction) {
            case UsbConstants.USB_DIR_IN:
                return "IN";
            case UsbConstants.USB_DIR_OUT:
                return "OUT";
            default:
                return "UNKNOWN";
        }
    }

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
