package com.client.printerutil;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Sends a minimal ESC/POS test receipt to the first USB device that exposes a
 * printer-class interface with a bulk OUT endpoint (common for thermal USB printers).
 */
public final class UsbStandardPrinter {

    private UsbStandardPrinter() {
    }

    /** Outcome of a print attempt (message is user-facing). */
    public static final class PrintResult {
        public final boolean success;
        public final String message;

        private PrintResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static PrintResult ok(String message) {
            return new PrintResult(true, message);
        }

        public static PrintResult fail(String message) {
            return new PrintResult(false, message);
        }
    }

    private static final class PrinterBulkOut {
        final UsbInterface usbInterface;
        final UsbEndpoint endpoint;

        PrinterBulkOut(UsbInterface usbInterface, UsbEndpoint endpoint) {
            this.usbInterface = usbInterface;
            this.endpoint = endpoint;
        }
    }

    /**
     * Returns USB devices that report a printer interface (class 7), in arbitrary order.
     */
    public static List<UsbDevice> listStandardPrinters(UsbManager usbManager) {
        List<UsbDevice> out = new ArrayList<>();
        if (usbManager == null) {
            return out;
        }
        Collection<UsbDevice> devices = usbManager.getDeviceList().values();
        for (UsbDevice d : devices) {
            if (looksLikeUsbPrinter(d)) {
                out.add(d);
            }
        }
        return out;
    }

    @Nullable
    public static UsbDevice findFirstStandardPrinter(UsbManager usbManager) {
        List<UsbDevice> list = listStandardPrinters(usbManager);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Prints a short test receipt. Caller must ensure {@link UsbManager#hasPermission(UsbDevice)}.
     */
    public static PrintResult printTestReceipt(UsbManager usbManager, UsbDevice device) {
        if (usbManager == null) {
            return PrintResult.fail("USB service not available.");
        }
        if (!usbManager.hasPermission(device)) {
            return PrintResult.fail("USB permission not granted for this device.");
        }
        UsbDeviceConnection conn = usbManager.openDevice(device);
        if (conn == null) {
            return PrintResult.fail("Could not open USB device.");
        }
        PrinterBulkOut target = findPrinterBulkOut(device);
        if (target == null) {
            conn.close();
            return PrintResult.fail("No printer bulk-OUT endpoint found on this device.");
        }
        if (!conn.claimInterface(target.usbInterface, true)) {
            conn.close();
            return PrintResult.fail("Could not claim printer USB interface.");
        }
        try {
            byte[] payload = buildEscPosTestReceipt();
            int sent = bulkWriteAll(conn, target.endpoint, payload, 8000);
            if (sent < 0) {
                return PrintResult.fail("USB bulkTransfer failed (code " + sent + ").");
            }
            if (sent != payload.length) {
                return PrintResult.ok("Sent " + sent + " of " + payload.length + " bytes (partial).");
            }
            return PrintResult.ok("Test receipt sent (" + sent + " bytes).");
        } finally {
            conn.releaseInterface(target.usbInterface);
            conn.close();
        }
    }

    private static int bulkWriteAll(
            UsbDeviceConnection conn, UsbEndpoint endpoint, byte[] data, int timeoutMs) {
        int maxPkt = endpoint.getMaxPacketSize();
        if (maxPkt <= 0) {
            maxPkt = 64;
        }
        int offset = 0;
        while (offset < data.length) {
            int len = Math.min(maxPkt, data.length - offset);
            byte[] chunk = new byte[len];
            System.arraycopy(data, offset, chunk, 0, len);
            int r = conn.bulkTransfer(endpoint, chunk, len, timeoutMs);
            if (r < 0) {
                return r;
            }
            offset += r;
            if (r == 0) {
                return -1;
            }
        }
        return offset;
    }

    @Nullable
    private static PrinterBulkOut findPrinterBulkOut(UsbDevice device) {
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface intf = device.getInterface(i);
            if (intf.getInterfaceClass() != UsbConstants.USB_CLASS_PRINTER) {
                continue;
            }
            for (int e = 0; e < intf.getEndpointCount(); e++) {
                UsbEndpoint ep = intf.getEndpoint(e);
                if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
                        && ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                    return new PrinterBulkOut(intf, ep);
                }
            }
        }
        return null;
    }

    private static boolean looksLikeUsbPrinter(UsbDevice device) {
        if (device.getDeviceClass() == UsbConstants.USB_CLASS_PRINTER) {
            return true;
        }
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            if (device.getInterface(i).getInterfaceClass() == UsbConstants.USB_CLASS_PRINTER) {
                return true;
            }
        }
        return false;
    }

    private static byte[] buildEscPosTestReceipt() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Initialize printer
        out.write(0x1B);
        out.write(0x40);
        // Center, emphasized title
        out.write(0x1B);
        out.write(0x61);
        out.write(0x01);
        out.write(0x1B);
        out.write(0x45);
        out.write(0x01);
        writeAscii(out, "PRINTER UTIL\n");
        writeAscii(out, "USB TEST RECEIPT\n");
        out.write(0x1B);
        out.write(0x45);
        out.write(0x00);
        out.write(0x1B);
        out.write(0x61);
        out.write(0x00);
        writeAscii(out, "------------------------------\n");
        writeAscii(out, "Standard USB printer (class 7)\n");
        String when = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        writeAscii(out, when + "\n");
        writeAscii(out, "------------------------------\n");
        writeAscii(out, "\n");
        writeAscii(out, "If you can read this, bulk\n");
        writeAscii(out, "USB printing from this app\n");
        writeAscii(out, "worked.\n");
        writeAscii(out, "\n");
        // Feed a few lines (ESC d n)
        out.write(0x1B);
        out.write(0x64);
        out.write(0x04);
        // Partial cut (common on ESC/POS thermal); ignored on printers without cutter
        out.write(0x1D);
        out.write(0x56);
        out.write(0x01);
        return out.toByteArray();
    }

    private static void writeAscii(ByteArrayOutputStream out, String s) {
        byte[] b = s.getBytes(StandardCharsets.US_ASCII);
        out.write(b, 0, b.length);
    }
}
