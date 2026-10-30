# EngramEdge Architecture Specification

**Project:** EngramEdge (`dev.engram.edge`)  
**Target Hardware:** Tiny Phone & Full Android Watch (FAW) — Android 15 (API 35–37), ~2.0"–2.4" Rectangular Micro-Display (`sw360dp`–`sw426dp`), 4 GB Physical LPDDR RAM, Google Play Services  
**System Mission:** Private, zero-latency on-device multimodal memory vault ("engram") with hybrid edge-cloud agentic intelligence, zero Cloud Functions, and strict 4 GB RAM adherence.

---

## 1. Executive Architectural Blueprint

In neuroscience, an **engram** is the physical neural substrate of a memory trace. **EngramEdge** implements this concept for Android devices with severe physical and thermal constraints (such as small Full Android Watches and tiny 2-inch phones) by providing an offline-first, private multimodal memory engine.

```
                    ┌────────────────────────────────────────────────────────┐
                    │       TINY PHONE & FAW MULTIMODAL INTERACTION          │
                    │   • Push-to-Talk Voice Memo (Android STT)              │
                    │   • CameraX Snapshot & ML Kit Text Recognition         │
                    │   • AMOLED Micro-Deck (`sw360dp`–`sw426dp`, 80dp targets)│
                    │   • Android 16+ Jetpack AppFunctions System Triggers   │
                    └───────────────────────────┬────────────────────────────┘
                                                │
                                                ▼
                    ┌────────────────────────────────────────────────────────┐
                    │               CHOKEPOINT A: INGRESS SHIELD             │
                    │        LiteRT 2.3.0 PromptInjectionShield (BERT q8)   │
                    │  Sanitizes OCR text, voice inputs, and external clips │
                    └───────────────────────────┬────────────────────────────┘
                                                │ (Clean Input / Utterance)
                                                ▼
                    ┌────────────────────────────────────────────────────────┐
                    │           SYSTEM-1 DISCRIMINATIVE ROUTER (<30ms)       │
                    │     256d MRL EmbeddingGemma 2 Centroids (0 MB Extra)   │
                    │  Choice: [AgentRoute] | Score: [Complexity] | Noul: [Safe]
                    │      Confidence Gating: P_max >= 0.80 / 0.50 / <0.50   │
                    └───────┬───────────────────┬────────────────────┬───────┘
                            │                   │                    │
              [P >= 0.80 RAG]│    [0.50 <= P < 0.80]│     [Live/Maps │ Cloud]
                            ▼                   ▼                    ▼
     ┌────────────────────────────┐ ┌─────────────────────┐ ┌────────────────────────┐
     │   HYBRID MULTIMODAL VAULT  │ │  WRIST CONFIRMATION │ │ FIREBASE AI LOGIC TIER │
     │  ObjectBox 6 (256d HNSW)   │ │  One-Tap Action Card│ │  Gemini 2.5 Flash / Live│
     │  AppSearch (BM25F Service) │ └──────────┬──────────┘ │  Maps Grounding / TTS  │
     │  Reciprocal Rank Fusion    │            │            │  Firebase App Check    │
     │  Pre-LLM Grounding Gate    │            │            └────────────────────────┘
     └──────────────┬─────────────┘            │
                    │                          │
                    ▼                          ▼
     ┌────────────────────────────────────────────────────┐
     │        JETBRAINS KOOG 1.3.0 AGENT HARNESS          │
     │   DynamicPromptExecutor + KV History Compression   │
     │    LiteRT-LM 0.18.0 (Gemma 3 270M / Qwen3-0.6B)    │
     └─────────────────────────┬──────────────────────────┘
                               │ Tool Calls
                               ▼
     ┌────────────────────────────────────────────────────┐
     │            CHOKEPOINT B: EGRESS SHIELD             │
     │   Deep Payload Inspection of run_intent / run_js   │
     └─────────────────────────┬──────────────────────────┘
                               │ Verified Action
                               ▼
     ┌────────────────────────────────────────────────────┐
     │        TINY PHONE & FAW DEVICE SKILLS EXECUTOR     │
     │  • TraX V2 ContentProvider (Zero-PPG LED Drain)    │
     │  • Native Android Sensor.TYPE_HEART_RATE (21)      │
     │  • Alarms & Countdown Timers (AlarmClock Intents)  │
     │  • Log Vitals to Vault (ObjectBox + AppSearch)     │
     │  • Camera LED Flashlight Torch (CameraManager)     │
     │  • Quick Settings & Volume Control (Settings)      │
     │  • Calendar Scheduler (CalendarContract.Events)    │
     │  • Isolated JS Math Sandbox (WebView run_js)       │
     └────────────────────────────────────────────────────┘
```

---

## 2. Micro-Display Form Factor & Ergonomics (~2.0"–2.4" Screen)

### Physical & UI Reality
Target devices include standalone Full Android Watches (e.g. Lemfo, Rogbid, Lokmat running full AOSP Android 12–15) and ultra-compact palm phones (e.g. Unihertz Jelly/Atom, Soyes).
* **Viewport Dimensions:** Typical resolution yields `sw360dp` to `sw426dp` (`426x490` dp @ 180 dpi or `360x360` dp @ 240 dpi).
* **Touch Dynamics (Fat-Finger Prevention):** On a 2-inch screen, standard 48dp Material buttons result in high mis-tap rates. EngramEdge enforces minimum **64dp to 80dp touch targets** for primary interactions.
* **Typography:** Minimum text scale of **18sp to 24sp** for critical readouts (timestamps, vitals, summary answers) to guarantee readability without reading glasses while walking or running.
* **AMOLED True-Black Power Conservation:** Backgrounds are hardcoded to `#000000` (AMOLED off-state) to conserve battery on small 800 mAh–1500 mAh battery cells.
* **No On-Screen Typing:** Software keyboards consume 65% of a 2" display and frustrate users. EngramEdge adopts a **voice-first and camera-first ingress**:
  * Tap-and-hold Push-to-Talk (Android `SpeechRecognizer` / Firebase Gemini Live).
  * One-tap CameraX snapshot with ML Kit OCR for instant receipt, document, or card ingestion.
  * One-tap quick response chips.

### AMOLED Micro-Deck Layout
```
┌────────────────────────────────────────────────────────┐
│  [HUD] 12:45  RAM: 812MB/1.4GB  Gemma3-270M  HYBRID    │  <- TelemetryHudPill
├────────────────────────────────────────────────────────┤
│                                                        │
│                    ENGRAM VAULT CARD                   │
│                                                        │
│           "Where did I park my rental EV?"             │
│                                                        │
│   ┌────────────────────────────────────────────────┐   │
│   │ [ENGRAM #42] Parked on Level 3, Spot B-14      │   │
│   │ Matched via Hybrid RRF (Vector 0.91 + BM25F)   │   │
│   └────────────────────────────────────────────────┘   │
│                                                        │
│   ┌────────────────────────────────────────────────┐   │
│   │              [ HOLD TO SPEAK (80dp) ]          │   │  <- Primary Touch Target
│   └────────────────────────────────────────────────┘   │
├────────────────────────────────────────────────────────┤
│  [ENGRAM]        [SNAP]        [LIVE]        [MODELS]  │  <- HorizontalPager Tabs
└────────────────────────────────────────────────────────┘
```

---

## 3. Hardware Constraints & 4 GB Memory Topology

### Resident Memory Breakdown
On modern Android 15, the Linux kernel, ART runtime, SystemUI, SurfaceFlinger, and Play Services consume ~2.0 GB to 2.4 GB of physical RAM. The Android Low Memory Killer Daemon (`lmkd`) enforces an application PSS ceiling of **1,100 MB to 1,400 MB**.

```
Total Physical LPDDR RAM: 4,096 MB
┌────────────────────────────────────────────────────────┬────────────────────────┐
│ Android OS, Kernel, Display Server, System Services   │ Application Headroom   │
│ (~2,300 MB - 2,500 MB)                                 │ (~1,200 MB - 1,400 MB) │
└────────────────────────────────────────────────────────┴───────────┬────────────┘
                                                                     │
       ┌─────────────────────────────────────────────────────────────┴──────────┐
       │ EngramEdge Co-Resident Memory Allocation                               │
       ├────────────────────────────────────────┬───────────────────────────────┤
       │ Component                              │ Resident Native / Heap Budget │
       ├────────────────────────────────────────┼───────────────────────────────┤
       │ Android Runtime Heap (App UI & Logic)  │ 65 MB – 90 MB                 │
       │ ObjectBox 6 Engine & HNSW Graph Index  │ 25 MB – 40 MB                 │
       │ AppSearch Lexical Index (System Svc)   │ 0 MB (PlatformStorage)        │
       │ EmbeddingGemma 2 (256d Text Head)      │ ~290 MB – 320 MB              │
       │ Active LLM: Gemma 3 270M-IT (or Qwen3) │ ~310 MB – 410 MB              │
       │ LiteRT PromptShield (8-bit BERT)       │ 18 MB – 24 MB                 │
       ├────────────────────────────────────────┼───────────────────────────────┤
       │ Total App Resident Footprint (CO-RES)  │ ~708 MB – 884 MB              │
       │ Safe Headroom to LMKD Threshold        │ ~516 MB – 692 MB              │
       └────────────────────────────────────────┴───────────────────────────────┘
```

### Adaptive Residency Lifecycle (`AdaptiveResidencyMonitor`)
The application continuously monitors memory pressure via:
1. Parsing `/proc/meminfo` (`MemAvailable` and `MemTotal`).
2. Querying `Debug.getPss()`.
3. Registering `ComponentCallbacks2.onTrimMemory(level)`.

* **`CO-RESIDENT` Mode (Default):** Both `EmbeddingGemma 2` and the compact synthesis LLM (`Gemma 3 270M-IT` or `Qwen3-0.6B`) reside simultaneously in RAM for instant, sub-second query and synthesis.
* **`AUTO-SWAP` Mode (Emergency Fallback):** If `MemAvailable` drops below **340 MB**, the monitor unloads the generative LLM while computing embeddings or running camera ingestion, and unloads embeddings when streaming generative answers, completely preventing LMKD crash terminations.
* **Dynamic Hardware Tier & Suitability Detection:** Evaluates `/proc/meminfo` physical total RAM at runtime. Automatically classifies model suitability (`OPTIMAL FOR HARDWARE`, `HIGH RAM • AUTO-SWAP`, or `REQUIRES 6GB+ RAM`), guards against loading 2B+ models (`Gemma 4 E2B`) on memory-constrained devices, and expands KV-cache to 4096 tokens on 6GB+ hardware.

---

## 4. Hybrid Multimodal Vault Architecture (ObjectBox + AppSearch)

Pure vector embeddings fail on exact alphanumeric queries (e.g. Wi-Fi passwords, gate codes, flight numbers, medication names). EngramEdge deploys a dual-engine **Hybrid Multimodal Vault**:

```
                       User Query: "What is my garage door pin code?"
                                       │
                    ┌──────────────────┴──────────────────┐
                    ▼                                     ▼
        [Semantic Vector Branch]              [Lexical BM25F Branch]
      EmbeddingGemma 2 Text Engine         Android 15 AppSearch Service
    Prepend: "task: search query | text: "     Sanitize & Tokenize: "garage door pin code"
        Truncate to 256d MRL                     PlatformStorage System Engine
     ObjectBox 6 HNSW Cosine Index             BM25F Field Frequency Scoring
                    │                                     │
                    ▼                                     ▼
     List<VectorHit> (Top-8 by Cosine)     List<LexicalHit> (Top-8 by BM25F)
                    │                                     │
                    └──────────────────┬──────────────────┘
                                       ▼
                       Reciprocal Rank Fusion (RRF)
                  RRF(d) = w_v / (k + rank_v) + w_l / (k + rank_l)
                                (k = 60, w_v = 1.0, w_l = 0.85)
                                       │
                                       ▼
                         Pre-LLM Grounding Gate
                   Passes if Cosine Distance <= 0.42 OR Lexical Hit
                                       │
                   ┌───────────────────┴───────────────────┐
                   │ Passes                                │ Fails
                   ▼                                       ▼
      Top-3 Chunks Injected into             Deterministic Fallback
      Koog Context via UserPromptAugmenter    "No relevant engram saved in memory."
```

### Critical EmbeddingGemma 2 Guardrails
1. **Never Use FP16:** EmbeddingGemma 2's internal activation range exceeds standard IEEE 754 float16 dynamic range. Executing in FP16 causes silent NaN overflows without crashing. Inference must be configured in `bfloat16`, `float32`, or `int8`.
2. **Matryoshka Representation Learning (MRL):** Truncate the raw 768-dimensional output vector to **256 dimensions** and re-normalize using double-precision $L_2$ norm:
   $$\mathbf{v}_{256} = \frac{\mathbf{v}_{1..256}}{\|\mathbf{v}_{1..256}\|_2}$$
   This reduces ObjectBox HNSW index memory consumption by **66%** with negligible impact on retrieval recall.
3. **Asymmetric Task-Steered Prefixes:**
   * Document Indexing: `title: {title} | text: {content}` (or `title: none | text: {content}`)
   * Query Retrieval: `task: search query | text: {query}`
   * Intent Classification: `task: classification | text: {query}`
4. **Pre-LLM Grounding Gate:** If the nearest neighbor cosine distance exceeds `0.42` and no lexical hit exists, the system bypasses the generative LLM entirely, saving battery, thermal headroom, and preventing hallucinations.

---

## 5. System-1 Discriminative Decision Router & Adversarial Safety

Generative LLMs are too slow and non-deterministic for routine routing and tool dispatch on an edge device. EngramEdge implements a **Jev-style discriminative decision engine** operating in under 30 milliseconds with **0 MB extra RAM**.

### Decision Primitives
* **`Choice` (Categorical Route):** Evaluated over 5 routes:
  1. `LOCAL_MEMORY_RAG`: Offline query over ObjectBox/AppSearch memory vault.
  2. `DEVICE_HARDWARE_SKILL`: Triggering watch/phone alarms, torch, volume, health vitals.
  3. `VISUAL_SCENE_QA`: Ingesting CameraX snapshot for local OCR analysis.
  4. `LIVE_CLOUD_STREAM`: Bidirectional Gemini Live audio/video session.
  5. `CLOUD_GROUNDED_SEARCH`: Firebase AI Logic with Google Maps grounding.
  * *Implementation:* Computed via cosine similarities against pre-compiled 256d `EmbeddingGemma 2` route centroids, passed through temperature-scaled softmax ($T = 0.08$) and lexical anchor boosts. Sums strictly to 1.0.
* **`Score` (Complexity Rating):** Estimates cognitive complexity (`LOW`, `MEDIUM`, `HIGH`) based on linguistic syntactic structure.
* **`Noul` (Truth/Safety Probability):** Emitted by the `PromptInjectionShield`.

### Confidence Gating
* **$P_{\max} \ge 0.80$ (or Device Offline):** Auto-execute locally on-device.
* **$0.50 \le P_{\max} < 0.80$:** Render a one-tap confirmation card on the micro-deck before executing sensitive OS actions.
* **$P_{\max} < 0.50$:** Escalate to Firebase AI Logic cloud tier when online.

### Two-Chokepoint Security Architecture
* **Chokepoint A (Ingress):** Sanitizes untrusted camera OCR, speech transcripts, and incoming messages against prompt injection, Unicode smuggling (`\u200B`), and system prompt override attempts using a single-thread LiteRT 2.3.0 BERT q8 classifier.
* **Chokepoint B (Egress):** Deep argument validation before passing tool calls to `run_intent` or `run_js`, eliminating confused deputy attacks.

---

## 6. JetBrains Koog 1.3.0 Orchestration & Watch/Phone Skills

EngramEdge uses **JetBrains Koog** (`ai.koog:koog-agents:1.3.0` + `ai.koog:skills:1.3.0-beta`):
* **`DynamicPromptExecutor`:** Seamlessly hot-swaps between on-device `LiteRTLLMClient` (Gemma 3 270M / Qwen3-0.6B) and cloud `FirebaseAiCloudBridge` without losing conversational state.
* **Intelligent KV History Compression:** Prunes older context turns to maintain a strict 1,024-token context limit, keeping KV-cache memory constant.
* **Progressive `SKILL.md` Tool Disclosure:** Discloses tool parameter schemas to the compact LLM only when a matching skill category is activated.

### Bundled Hardware Skills (`SKILL.md`)
1. **`set-watch-alarm`** (`run_intent`): Sets alarms and timers via standard Android `AlarmClock` intents.
2. **`read-wrist-sensors`** (`run_intent`): Reads battery status, memory telemetry, and 3-tier health vitals.
3. **`log-vitals-to-vault`** (`run_intent`): Samples current vitals and indexes an engram note directly into ObjectBox and AppSearch.
4. **`toggle-watch-torch`** (`run_intent`): Toggles the camera LED flashlight via `CameraManager.setTorchMode` (essential on wrist and micro-phones).
5. **`control-watch-system`** (`run_intent`): Controls media volume and opens Android 15 Quick Settings panels (`WIFI`, `BLUETOOTH`, `DISPLAY`, `BATTERY`, `SOUND`).
6. **`schedule-watch-event`** (`run_intent`): Inserts calendar entries via `CalendarContract.Events`.
7. **`quick-js-calc`** (`run_js`): Executes sandboxed mathematical and unit calculations inside an isolated, network-blocked `WebView`.

### Android 16+ Jetpack AppFunctions (`BaseEngramAppFunctionService`)
Exposes EngramEdge directly to Android system intelligence:
* `searchEngrams(query, limit)`
* `saveEngramNote(title, content, tags)`
* `readEngramVitals(metric, forceLive)`

---

## 7. Dual-Form-Factor Sensor & Health Bridge

Full Android Watches (FAWs) feature optical Photoplethysmography (PPG) sensors that frequently cause battery drain or sensor lockup if polled improperly. EngramEdge implements a **3-tier battery-safe bridge**:

```
                             readHealthSnapshot()
                                       │
                                       ▼
                ┌──────────────────────────────────────────────┐
                │ TIER 1: TraX V2 ContentProvider Query        │
                │ content://com.ailife.betterhealth.healthprovider
                │ Permission: com.ailife.betterhealth.READ_HEALTH_DATA
                │ Reads: cached heartrate, oxygen, steps       │
                │ Battery Impact: ZERO (0) LED activation      │
                └──────────────────────┬───────────────────────┘
                                       │
                       Cached data available & live not requested?
                                      ├──► YES ──► Return Snapshot
                                       ▼ NO
                ┌──────────────────────────────────────────────┐
                │ TIER 2: Native Android Sensor.TYPE_HEART_RATE│
                │ Sensor ID: 21 (Available on modern FAW ROMs) │
                │ Action: One-shot sample via SensorManager    │
                │ Guard: Immediately unregister on valid BPM   │
                └──────────────────────┬───────────────────────┘
                                       │
                       Native sensor 21 missing or zero readings?
                                      ├──► NO ──► Return Live BPM
                                       ▼ YES
                ┌──────────────────────────────────────────────┐
                │ TIER 3: Legacy Fallback Broadcasts / Vendor  │
                │ Proprietary HSC/CD vendor IDs (31, 43, 65596)│
                │ Legacy GET_DATA_API broadcast for SpO2       │
                └──────────────────────────────────────────────┘
```

* **Zero-LED Cache Ingestion:** Prioritizing TraX V2 `ContentProvider` eliminates unneeded green optical PPG LED firings.
* **Immediate Sensor Release:** When sampling native `Sensor.TYPE_HEART_RATE` (21), the listener is unregistered the instant the first valid reading in `35..230` BPM is received, guaranteeing no background sensor lockup.

---

## 8. Cloud Escalation with Firebase AI Logic (Zero Cloud Functions)

When connectivity is available and tasks require open-world reasoning, live camera assistance, or spatial maps grounding, EngramEdge escalates to **Firebase AI Logic** (`com.google.firebase:firebase-ai`):
* **No Cloud Functions:** Replaces legacy Cloud Run / Cloud Functions proxies with direct client-side Firebase AI Logic SDK calls.
* **Firebase App Check (Play Integrity):** Secures all API calls against unauthorized access and API key scraping.
* **Gemini 3.8 Flash Live Multimodal Session (`gemini-3.8-flash`):** Streams throttled 1 fps CameraX frames and voice prompts for live visual assistant queries.
* **Gemini 3.8 Flash + Google Maps Grounding (`gemini-3.8-flash`):** Enriches spatial search queries with grounded local places and open-world knowledge.
* **Speech Pipeline:** Uses device TTS/STT seamlessly across online and airplane-offline modes.

---

## 9. Cross-Component State & Headless Projection (Jetpack AppState)

Because EngramEdge operates simultaneously through a foreground Jetpack Compose UI (`MainActivity` / `EngramEdgeViewModel`) and headless Android 16+ OS entry points (`BaseEngramAppFunctionService`), relying solely on UI-scoped `ViewModel` flows leaves background agent mutations invisible to the UI and loses user preferences under `lmkd` process reclamation on 4 GB hardware.

EngramEdge integrates **Jetpack AppState (`androidx.appstate` 1.0.0-alpha01)** via `EngramAppStateCoordinator`:

```
  ┌──────────────────────────┐        ┌──────────────────────────────────┐
  │ BaseEngramAppFunctionSvc │        │  Domain & Hardware Bridges       │
  │ • searchMemories         │        │  • HybridMemoryRepository        │
  │ • saveMemoryNote         │        │  • BetterHealthTrackerBridge     │
  │ • readWristVitals        │        │  • LiteRtModelGalleryManager     │
  └────────────┬─────────────┘        └─────────────────┬────────────────┘
               │  updateState / setState                │
               ▼                                        ▼
  ┌──────────────────────────────────────────────────────────────────────┐
  │             EngramAppStateCoordinator (Jetpack StateStore)           │
  │  • EdgePreferencesKey   (@Serializable, PersistToDataStore)          │
  │  • VaultBookmarkKey     (@Serializable, PersistToDataStore)          │
  │  • CachedVitalsKey      (@Serializable, PersistToDataStore)          │
  │  • ProcessSharedRuntimeKey (Ephemeral: torch & AppFunction counters) │
  └───────────────────┬───────────────────────────────┬──────────────────┘
                      │ syncToDataStore               │ Snapshot State<T>
                      ▼                               ▼
     ┌────────────────────────────────┐  ┌───────────────────────────────┐
     │ JSON DataStore Persistence     │  │ Headless AppState Transform   │
     │ files/datastore/engram_appstate│  │ @Composable off UI thread     │
     │ Survives LMKD process death    │  │ -> State<HudBannerProjection> │
     └────────────────────────────────┘  └───────────────┬───────────────┘
                                                         │
                                                         ▼
                                         ┌───────────────────────────────┐
                                         │   Compose TelemetryHudPill    │
                                         │ CO-RES • 685MB • Qwen3 [4k]   │
                                         └───────────────────────────────┘
```

### Key Architectural Guarantees
1. **Encapsulated `StateStoreKey<T>` Ownership:** All `StateStoreKey` objects (`EdgePreferencesKey`, `VaultBookmarkKey`, `CachedVitalsKey`, `ProcessSharedRuntimeKey`) are `private` inside `EngramAppStateCoordinator`, enforcing unidirectional mutations through typed domain methods while exposing Compose `State<T>` snapshots to any reader in the process.
2. **Automatic Process-Death Persistence (`PersistToDataStore`):** Implementing `PersistToDataStore` on `@Serializable` keys and binding `stateStore.syncToDataStore(...)` in `EngramEdgeApplication.onCreate()` persists `HYBRID`/`AIRPLANE` mode, auto-speak, preferred LiteRT-LM model ID, GPU vision preference, latest stored vault bookmark, and cached wrist vitals across OS process death without manual `SharedPreferences` boilerplate.
3. **Strict Credential & Binary Boundaries:** High-entropy secrets (the HuggingFace Bearer token) remain strictly inside Android Keystore AES-256-GCM (`EncryptedTokenStore`) and are never serialized into `StateStore` JSON; raw CameraX JPEG byte arrays remain ephemeral in memory.
4. **Headless `@Composable` HUD Derivation (`androidx.appstate.transform`):** `HudStateTransformer` runs `transform(...)` on `Dispatchers.Default`, fusing `StateStore` Compose snapshot states with high-frequency `StateFlow`s (`DeviceMemoryTelemetry`, `GalleryRuntimeState`, `SystemOneDecision`) outside the UI composition tree and enabling fast JVM unit testing via `transformTest`.

---

## 10. Dependency & Tooling Matrix

| Component | Maven Artifact / Plugin | Target Version |
| :--- | :--- | :--- |
| **Android Gradle Plugin** | `com.android.application` | `9.4.1` |
| **Kotlin & KSP** | `org.jetbrains.kotlin.*` / `ksp` | `2.3.10` / `2.3.12` |
| **Jetpack Compose** | `androidx.compose:compose-bom` | `2026.09.00` |
| **Jetpack AppState** | `androidx.appstate:statestore*` / `transform*` | `1.0.0-alpha01` |
| **LiteRT-LM** | `com.google.ai.edge.litertlm:litertlm-android` | `0.18.0` |
| **LiteRT V2 (`CompiledModel`)** | `com.google.ai.edge.litert:litert` (+ `gpu`) | `2.3.0` |
| **JetBrains Koog** | `ai.koog:koog-agents` + `ai.koog:skills` | `1.3.0` + `1.3.0-beta` |
| **ObjectBox** | `io.objectbox:objectbox-kotlin` / `android` | `6.0.0-beta3` |
| **Jetpack AppSearch & AppFunctions** | `androidx.appsearch:*` / `androidx.appfunctions:*` | `1.1.0` / `1.0.0-beta01` |
| **Firebase BoM** | `com.google.firebase:firebase-bom` | `35.1.0` |

