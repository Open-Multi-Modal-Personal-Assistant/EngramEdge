# EngramEdge

In neuroscience, an **engram** is the physical substrate of a memory trace in neural tissue. **EngramEdge** brings this concept to Android edge devices—delivering a private, zero-latency multimodal memory vault and hybrid agentic assistant tailored for strict 4GB RAM budgets on tiny phones and Full Android Watches.

Instead of sending every voice memo, camera snapshot, or personal inquiry to the cloud, EngramEdge fuses **EmbeddingGemma 2** (256-dimensional Matryoshka vectors) in **ObjectBox 6 HNSW** with **Android 15 AppSearch BM25F** for instant, offline hybrid recall. A zero-marginal-RAM **System-1 router** evaluates intent and adversarial safety in under 30 milliseconds. Orchestrated by **JetBrains Koog** and powered locally by compact **LiteRT-LM** models (including **Qwen3-0.6B**, **FunctionGemma 270M**, and **Gemma 3 270M**), routine memory Q&A, OS-level **Jetpack AppFunctions**, and native hardware skills run 100% offline. When complex real-time camera assistance or spatial Google Maps grounding is requested, the agent seamlessly escalates to **Firebase AI Logic** via **Gemini 3.8 Flash**.

<!-- Package ID: dev.engram.edge -->

---

## Architecture & Documentation

- [**System Architecture Specification**](file:///home/csaba/repos/AIML/OpenMMPA/EngramEdge/docs/ARCHITECTURE.md): Complete engineering specification covering the 4GB memory topology, Matryoshka 256d vector embeddings, ObjectBox + AppSearch hybrid vault, Jev-style System-1 router, JetBrains Koog 1.3.0 orchestration, Jetpack AppState cross-component synchronization, and 3-tier health hardware bridge.
- [**User & Operator Guide**](file:///home/csaba/repos/AIML/OpenMMPA/EngramEdge/docs/USER_GUIDE.md): Practical guide covering micro-display ergonomics (~2.0"–2.4" screen, 80dp touch targets), built-in device skills, wireless ADB setup, and sensor tracking.

---

## Core Pillars

| Pillar | Implementation | Key Hardware Benefit |
| :--- | :--- | :--- |
| **Form Factor & Display** | Micro-Deck Compose UI (`sw360dp`–`sw426dp`, AMOLED `#000000`) | 80dp touch targets & 20–26sp text designed for small Full Android Watches and ~2" tiny phones |
| **4GB Memory Topology** | `AdaptiveResidencyMonitor` (`CO-RES` default, `AUTO-SWAP` fallback) | Stays safely under the ~1.1–1.4 GB LMKD ceiling with dynamic 4096/2048 KV-token scaling |
| **Multimodal Vault** | `EmbeddingGemma 2` (256d MRL) + `ObjectBox 6` HNSW + Android 15 `AppSearch` BM25F | Hybrid vector-lexical recall with Reciprocal Rank Fusion & Pre-LLM Grounding Gate |
| **System-1 Router** | 256d Centroid Classifier + Temperature Softmax ($T=0.08$) | Sub-30ms intent routing and confidence gating with 0 MB extra RAM |
| **Adversarial Safety** | LiteRT 2.3.0 `CompiledModel` BERT q8 Prompt Injection Shield | Dual chokepoints at ingress (OCR/Voice) and egress (`run_intent`/`run_js`) |
| **Orchestrator** | JetBrains Koog 1.3.0 + KV History Compression | Progressive `SKILL.md` tool disclosure and seamless edge-to-cloud hot-swapping |
| **Cross-Component AppState** | Jetpack AppState (`StateStore` + `PersistToDataStore` + `Transform`) | Syncs foreground UI with headless `AppFunctionService` & derives HUD projections off the UI thread |
| **Sensor & Health Bridge** | 3-Tier `BetterHealthTrackerBridge` (TraX V2 `ContentProvider` + native Sensor 21) | Zero-PPG LED battery drain with immediate sensor release |
| **Cloud Escalation** | Firebase AI Logic (`firebase-ai`) + Firebase App Check | Gemini 3.8 Flash Live 1 fps streaming & Google Maps grounding with zero Cloud Functions |


