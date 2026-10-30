# EngramEdge Getting Started & User Guide

**App:** EngramEdge (`dev.engram.edge`)  
**Target Hardware:** Tiny Phone & Full Android Watch (FAW) running Android 15 (e.g. rectangular >2" wrist devices like APPLLP 9 MAX, Lemfo DM101, Rogbid, Kospet, or ultra-compact phones like Unihertz Jelly 2/Star, Soyes with 4 GB RAM)

---

## 1. Overview & Concept

In neuroscience, an **engram** represents the biophysical trace left in neural tissue by an experience or memory. **EngramEdge** serves as your on-device digital engram: a private, zero-latency multimodal memory vault that lives completely inside your wrist device or tiny palm phone.

All personal memories (notes, receipts, Wi-Fi passwords, parking locations, health vitals, voice memos) are converted into **256-dimensional Matryoshka vectors** via **EmbeddingGemma 2** and stored alongside an **Android 15 AppSearch BM25F** keyword index in a local **ObjectBox 6** database. Routine retrieval and device skills operate 100% offline using compact **LiteRT-LM** models (**Qwen3-0.6B**, **FunctionGemma 270M**, or **Gemma 3 270M**), with automatic escalation to **Firebase AI Logic** (**Gemini 3.8 Flash** Live & Maps Grounding) when online.

---

## 2. Quickstart: 5-Minute Getting Started

Follow these steps to build, deploy, and test EngramEdge on your connected device:

### Step 1: Build & Install
Connect your Android 15 device via USB and run:
```bash
cd /home/csaba/repos/AIML/OpenMMPA/EngramEdge

# Compile and install debug build
./gradlew installDebug

# Launch the app
adb shell am start -n dev.engram.edge/.MainActivity
```

### Step 2: Grant Core Permissions
Grant microphone, camera, and sensor access with one command:
```bash
SERIAL=$(adb get-serialno)
adb -s $SERIAL shell pm grant dev.engram.edge android.permission.RECORD_AUDIO
adb -s $SERIAL shell pm grant dev.engram.edge android.permission.CAMERA
adb -s $SERIAL shell pm grant dev.engram.edge android.permission.BODY_SENSORS
adb -s $SERIAL shell pm grant dev.engram.edge android.permission.ACTIVITY_RECOGNITION
adb -s $SERIAL shell pm grant dev.engram.edge android.permission.POST_NOTIFICATIONS
adb -s $SERIAL shell pm grant dev.engram.edge com.ailife.betterhealth.READ_HEALTH_DATA 2>/dev/null || true
```

### Step 3: Try Your First In-App Queries
On first launch, EngramEdge automatically seeds sample memories (Rental EV parking location, Hotel Wi-Fi password, Medication profile, Hackathon demo notes) into the hybrid vault:
1. Tap the quick chip **"Parked Car?"** on the **`ASK`** screen to test hybrid ObjectBox + AppSearch RRF recall.
2. Tap the quick chip **"Hotel Wi-Fi?"** to retrieve the exact Wi-Fi password and room code.
3. Tap **"Torch"** to toggle the rear camera LED flashlight on/off.
4. Tap **"Live HR"** to sample your optical wrist heart rate sensor safely without background battery drain.

### Step 4: Test Offline Airplane Mode
Tap the top **`TelemetryHudPill`** (top banner displaying `CO-RES • ...MB PSS • Qwen3-0.6B`). It toggles between **`HYBRID`** and **`AIRPLANE`** mode.
* In **`AIRPLANE`** mode, cloud escalation is disabled, and queries run 100% locally on-device via ObjectBox 256d HNSW + AppSearch BM25F + LiteRT-LM.
* The state is persisted via **Jetpack AppState** (`PersistToDataStore`) and will stay in `AIRPLANE` mode even if the app process is force-stopped and relaunched.

### Step 5: Configure Hugging Face Token (Optional)
Swipe right to the **`MODELS`** tab:
1. Paste your Hugging Face User Access Token (read permissions) and tap **SAVE TOKEN**. It is encrypted with AES-256-GCM via the Android Keystore (`EncryptedTokenStore`).
2. Accept the model licenses on Hugging Face for gated models if desired:
   - [FunctionGemma 270M](https://huggingface.co/litert-community/functiongemma-270m-ft-mobile-actions)
   - [Gemma 3 270M-IT](https://huggingface.co/litert-community/gemma-3-270m-it)
3. Tap **PULL FROM HUGGINGFACE** to download and hot-swap between models on-device.

---

## 3. Micro-Display UI & Gesture Navigation

EngramEdge is designed specifically for ~2.0"–2.4" micro-displays (`sw360dp`–`sw426dp`). It eliminates virtual keyboard typing in favor of an ergonomic 4-card horizontal deck:

| Card | Primary Function | Primary Touch Affordance |
| :--- | :--- | :--- |
| **`ASK`** | Memory Vault Q&A & Voice Memo Ingestion | **80dp Push-to-Talk Button** (Tap/hold to speak, or tap quick prompt chips) |
| **`SNAP`** | Visual Memory & Document OCR Ingestion | **80dp Camera Shutter** (Tap to snapshot text/scene + synthetic Prompt-Shield tests) |
| **`LIVE`** | Gemini 3.8 Flash Live Multimodal Assistant | **Bidirectional Stream Button** (1 fps camera frames + voice queries) |
| **`MODELS`** | Model Gallery, HF Token & Residency Controls | **One-Tap Model Cards** (Switch between Qwen3-0.6B, FunctionGemma, Gemma 3, or SmolVLM2) |

### Top Telemetry HUD Pill (`TelemetryHudPill` via Jetpack AppState)
The top bar persistently displays a headless `@Composable` projection (`androidx.appstate.transform`) computed off the UI thread:
* **Residency Policy & App PSS RAM:** e.g., `CO-RES • 430MB PSS`
* **Active Model & Dynamic KV Context Window:** e.g., `Qwen3-0.6B [4k]`
* **System-1 Latency & External AppFunctions Counter:** e.g., `• S1 14ms • AF#2` when OS-level Android 16+ `AppFunctions` query or store memories in the background
* **Persisted Connectivity Toggle:** One-tap `HYBRID` ↔ `AIRPLANE` switch persisted across process death via Jetpack AppState (`PersistToDataStore`), alongside preferred model selection and latest vault bookmark (`Snap #<id>?`)

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
---

## 4. Tiny Phone & Watch Hardware Skills

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

## 5. In-Depth Setup, Installation & Permissions

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

## 6. Wireless ADB for Wearable Testing

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
