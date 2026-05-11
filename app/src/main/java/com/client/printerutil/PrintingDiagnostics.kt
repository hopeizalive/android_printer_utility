package com.client.printerutil

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.print.PrintJob
import android.print.PrintJobInfo
import android.print.PrintManager
import androidx.annotation.RequiresApi
import java.util.Locale

/**
 * Collects Android printing capability and connection hints for the main app.
 * USB class-7 devices are reported as likely printers; network printers usually
 * appear only after a vendor print service or driver is installed.
 */
object PrintingDiagnostics {

    fun buildReport(context: Context): String {
        val pm = context.packageManager
        val lines = ArrayList<String>()

        lines += "Device: ${Build.MANUFACTURER} ${Build.MODEL}"
        lines += "Android API: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})"
        lines += ""

        val hasPrintingFeature =
            pm.hasSystemFeature(PackageManager.FEATURE_PRINTING)
        lines += "System printing feature (FEATURE_PRINTING): $hasPrintingFeature"

        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        lines += "PrintManager available: ${printManager != null}"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lines += ""
            lines += "Recent / active print jobs (app scope, API 28+):"
            val jobs = printManager.printJobs
            if (jobs.isEmpty()) {
                lines += "  (none reported — normal if nothing printed from this device session)"
            } else {
                jobs.forEach { appendPrintJob(lines, it) }
            }
        } else {
            lines += ""
            lines += "Print job listing requires API 28+ (current device is lower)."
        }

        lines += ""
        lines += usbSection(context)

        lines += ""
        lines += "Notes:"
        lines += "- Wi‑Fi / IPP printers often need the manufacturer print service app."
        lines += "- Thermal Bluetooth printers need Bluetooth permissions and vendor SDKs."
        lines += "- Use Refresh after plugging USB; grant USB permission if the system prompts."

        return lines.joinToString("\n")
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun appendPrintJob(lines: MutableList<String>, job: PrintJob) {
        val info: PrintJobInfo = job.info
        val stateName = printJobStateName(info.state)
        val printer =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                info.printerName ?: "(printer name N/A)"
            } else {
                "(printer name requires API 29+)"
            }
        lines += "  • ${info.label}"
        lines += "    state=$stateName  printer=$printer  id=${job.id}"
    }

    private fun printJobStateName(state: Int): String = when (state) {
        PrintJobInfo.STATE_QUEUED -> "QUEUED"
        PrintJobInfo.STATE_STARTED -> "STARTED"
        PrintJobInfo.STATE_BLOCKED -> "BLOCKED"
        PrintJobInfo.STATE_COMPLETED -> "COMPLETED"
        PrintJobInfo.STATE_FAILED -> "FAILED"
        PrintJobInfo.STATE_CANCELED -> "CANCELED"
        else -> "UNKNOWN($state)"
    }

    private fun usbSection(context: Context): String {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val devices = usbManager.deviceList.values
        if (devices.isEmpty()) {
            return "USB: no USB devices detected by UsbManager."
        }

        val sb = StringBuilder()
        sb.appendLine("USB devices (${devices.size}):")
        devices.forEach { sb.appendLine(describeUsbDevice(it)) }
        val printers = devices.filter { looksLikeUsbPrinter(it) }
        sb.appendLine()
        sb.appendLine(
            "Likely USB printers (class 7 / interface printer): ${printers.size}"
        )
        return sb.toString().trimEnd()
    }

    private fun looksLikeUsbPrinter(device: UsbDevice): Boolean {
        if (device.deviceClass == UsbConstants.USB_CLASS_PRINTER) return true
        for (ci in 0 until device.configurationCount) {
            val cfg = device.getConfiguration(ci) ?: continue
            for (ii in 0 until cfg.interfaceCount) {
                val intf = cfg.getInterface(ii)
                if (intf.interfaceClass == UsbConstants.USB_CLASS_PRINTER) return true
            }
        }
        return false
    }

    private fun describeUsbDevice(d: UsbDevice): String {
        val vid = String.format(Locale.US, "0x%04X", d.vendorId)
        val pid = String.format(Locale.US, "0x%04X", d.productId)
        val printerHint = if (looksLikeUsbPrinter(d)) " [printer-class]" else ""
        val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            d.productName ?: d.deviceName
        } else {
            d.deviceName
        }
        return "  • $name$printerHint  VID=$vid PID=$pid  ${d.deviceName}"
    }
}
