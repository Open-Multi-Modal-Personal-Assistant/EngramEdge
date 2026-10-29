# EngramEdge

In neuroscience, an **engram** is the physical trace of a memory in the brain. We built **Engram Edge** to give Android devices a private, zero-latency multimodal memory that fits entirely inside a strict 4GB RAM budget.

Instead of sending every voice note, photo, or question to the cloud, Engram Edge uses **EmbeddingGemma 2** on **LiteRT-LM** to compress memories into **256-dimensional Matryoshka** vectors stored locally in **ObjectBox**. When you ask a question or trigger an action, a fast System One classifier evaluates intent and safety in under 30 milliseconds. Orchestrated by **JetBrains Koog**, routine memory Q&A and native Android skills run 100% offline on **Gemma 3 270M**, while complex real-time video assistance and Google Maps grounding seamlessly escalate to Firebase AI Logic when online.

package ID **dev.engram.edge**
