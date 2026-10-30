# EngramEdge

In neuroscience, an **engram** is the physical trace of a memory in the brain. We built **Engram Edge** to give Android devices a private, zero-latency multimodal memory that fits entirely inside a strict 4GB RAM budget.

Instead of sending every voice note, photo, or question to the cloud, Engram Edge uses **EmbeddingGemma 2** on **LiteRT-LM** to compress memories into **256-dimensional Matryoshka** vectors stored locally in **ObjectBox**. When you ask a question or trigger an action, a fast System One classifier evaluates intent and safety in under 30 milliseconds. Orchestrated by **JetBrains Koog**, routine memory Q&A and native Android skills run 100% offline on **Gemma 3 270M**, while complex real-time video assistance and Google Maps grounding seamlessly escalate to Firebase AI Logic when online.

package ID **`dev.engram.edge`**

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
| **Cloud Escalation** | Firebase AI Logic (`firebase-ai`) + Firebase App Check | Gemini 3.1 Live 1 fps streaming & Gemini 3 Flash Google Maps grounding with zero Cloud Functions |


