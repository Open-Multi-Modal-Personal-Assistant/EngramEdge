# EngramEdge User & Operator Guide

**App:** EngramEdge (`dev.engram.edge`)  
**Target Hardware:** Tiny Phone & Full Android Watch (FAW) running Android 15 (e.g. rectangular >2" wrist devices like APPLLP 9 MAX, Lemfo DM101, Rogbid, Kospet, or ultra-compact phones like Unihertz Jelly 2/Star, Soyes with 4 GB RAM)

---

## 1. Overview & Concept

In neuroscience, an **engram** represents the biophysical trace left in neural tissue by an experience or memory. **EngramEdge** serves as your on-device digital engram: a private, zero-latency multimodal memory vault that lives completely inside your wrist device or tiny palm phone.

All personal memories (notes, receipts, Wi-Fi passwords, parking locations, health vitals, voice memos) are converted into **256-dimensional Matryoshka vectors** via **EmbeddingGemma 2** and stored alongside an **AppSearch BM25F** keyword index in a local **ObjectBox 6** database. Routine retrieval and device skills operate 100% offline using **Gemma 3 270M-IT** or **Qwen3-0.6B**, with automatic escalation to **Firebase AI Logic** (Gemini Live & Maps Grounding) when online.

---

## 2. Micro-Display UI & Gesture Navigation

EngramEdge is designed specifically for ~2.0"–2.4" micro-displays (`sw360dp`–`sw426dp`). It eliminates virtual keyboard typing in favor of an ergonomic 4-card horizontal deck:

| Card | Primary Function | Primary Touch Affordance |
| :--- | :--- | :--- |
| **`ENGRAM`** | Memory Vault Q&A & Voice Memo Ingestion | **80dp Push-to-Talk Button** (Hold to speak, release to query or record) |
| **`SNAP`** | Visual Memory & Document OCR Ingestion | **80dp Camera Shutter** (Tap to snapshot text, barcodes, or scene) |
| **`LIVE`** | Gemini Live Multimodal Assistant | **Bidirectional Stream Button** (1 fps camera frames + streaming voice) |
| **`MODELS`** | Model Gallery, Telemetry & Residency Controls | **One-Tap Model Cards** (Switch between Gemma 3, Qwen3, or SmolVLM) |

### Top Telemetry HUD Pill (`TelemetryHudPill`)
The top bar persistently displays real-time hardware diagnostics:
* **Time & Battery Percentage**
* **Resident Memory (PSS / MemAvailable):** e.g., `812MB/1.4GB`
* **Active Model:** e.g., `Gemma3-270M`
* **Network Status & Residency Mode:** `HYBRID (CO-RES)` or `OFFLINE`

---

## 3. Tiny Phone & Watch Hardware Skills

EngramEdge includes 7 built-in device tools configured via the `SKILL.md` specification:

1. **Voice Alarms & Countdown Timers:**
   * *"Set a timer for 12 minutes"*
   * *"Wake me up at 6:30 AM tomorrow"*
2. **Flashlight Torch Control:**
   * *"Turn on the flashlight"* (Instantly toggles rear camera LED torch via `CameraManager`)
3. **Health Vitals Reading & Vault Logging:**
   * *"What is my heart rate?"* (Samples via 3-tier battery-safe sensor bridge)
   * *"Log my vitals"* (Indexes current BPM, oxygen, and timestamp directly into the Engram vault)
4. **Volume & Quick Settings Panels:**
   * *"Turn up the volume"*
   * *"Open Wi-Fi settings"* / *"Open Bluetooth"*
5. **Calendar Event Scheduling:**
   * *"Schedule dentist appointment tomorrow at 2 PM"*
6. **Sandboxed Quick Math & Unit Conversions:**
   * *"Calculate 18% tip on $45.60"* (Runs in isolated `run_js` sandbox)
7. **Hybrid Memory Recall:**
   * *"Where did I park?"*
   * *"What is the garage door code?"*

---

## 4. Setup, Installation & Permissions

### Build & Direct Install via ADB
```bash
# Clone and enter EngramEdge directory
cd /home/csaba/repos/AIML/OpenMMPA/EngramEdge

# Build debug APK
./gradlew assembleDebug

# Install to connected tiny phone or watch
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch EngramEdge
adb shell am start -n dev.engram.edge/.MainActivity
```

### One-Click Permissions Setup
```bash
DEVICE_SERIAL=$(adb get-serialno)

adb -s $DEVICE_SERIAL shell pm grant dev.engram.edge android.permission.RECORD_AUDIO
adb -s $DEVICE_SERIAL shell pm grant dev.engram.edge android.permission.CAMERA
adb -s $DEVICE_SERIAL shell pm grant dev.engram.edge android.permission.BODY_SENSORS
adb -s $DEVICE_SERIAL shell pm grant dev.engram.edge android.permission.ACTIVITY_RECOGNITION
adb -s $DEVICE_SERIAL shell pm grant dev.engram.edge android.permission.POST_NOTIFICATIONS

# TraX / Better Health Tracker V2 permission (if installed on FAW)
adb -s $DEVICE_SERIAL shell pm grant dev.engram.edge com.ailife.betterhealth.READ_HEALTH_DATA 2>/dev/null || true
```

---

## 5. Wireless ADB for Wearable Testing

When wearing a Full Android Watch snugly on your wrist to test optical PPG heart rate tracking, the physical USB pogo cable will break skin contact. Use Wi-Fi ADB:

```bash
# 1. While connected via cable, enable TCP/IP on port 5555
adb tcpip 5555

# 2. Disconnect the USB pogo cable and put the watch on your wrist
# 3. Find your watch IP in Settings -> Wi-Fi -> Advanced, then connect:
adb connect <WATCH_IP_ADDRESS>:5555

# 4. Stream real-time logs and verify sensor reading
adb -s <WATCH_IP_ADDRESS>:5555 logcat -s EngramEdge BetterHealthTrackerBridge
```
