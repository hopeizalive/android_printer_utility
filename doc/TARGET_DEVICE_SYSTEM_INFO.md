# Target device — system information

This document records what was visible on the **CPU-Z** app on the client unit (captured via AnyDesk). Use it when setting `minSdk`, ABIs, and printer integration expectations (USB receipt / Bluetooth thermal).

## SOC (processor and graphics)

| Field | Value |
|--------|--------|
| Processor (as shown) | Rockchip RK3066 |
| Cores | 4 |
| CPU | 4× ARM Cortex-A17 @ up to 1.61 GHz |
| CPU revision | r0p1 |
| Process (as reported) | 40 nm |
| Clock range (as reported) | 696 MHz – 1.61 GHz |
| GPU vendor | ARM |
| GPU renderer | Mali-T760 |
| OpenGL ES (System tab) | 3.2 |

## System (Android and kernel)

| Field | Value |
|--------|--------|
| Android version | 7.1.2 |
| API level | 25 (Nougat) |
| Security patch level | 2017-04-05 |
| Bootloader | unknown |
| Build ID | 1.0.6 |
| Java VM | ART 2.1.0 |
| Kernel architecture | armv7l (32-bit ARM) |
| Kernel version | 4.4.112 (eng build dated 20220621 in the reported string) |
| Root access | Yes |
| Google Play services (as shown) | 26.16.61 (040300-906635036) |

## Implications for the printer utility app

- **Ship an APK that supports at least API 25** (`minSdk` 25 or lower, e.g. 21–22), or the app **cannot install** on this device.
- **ABI**: expect **armeabi-v7a** (not 64-bit arm64 on this image).
- **USB receipt printers**: use **USB host** APIs; optional `android.hardware.usb.host` with `required="false"` is appropriate.
- **Bluetooth thermal**: classic Bluetooth (SPP) is typical; declare legacy Bluetooth permissions appropriate for API 25; many vendors still ship a small SDK or fixed UUID for the ESC/POS channel.
- **Print framework**: Android printing exists on Nougat, but **listing print jobs the way newer APIs do** differs by version; treat API 25 as “capabilities + USB/BT visibility,” not as a full desktop print queue.
- **Root**: present on this sample; do not rely on it for a normal product path unless the deployment contract explicitly requires root-only hooks.

## Source

Observations from CPU-Z screenshots: **SOC** tab and **SYSTEM** tab on the device, with host session context (AnyDesk) noted only for traceability, not as a runtime dependency.
