package dev.engram.edge.edge

import android.content.Context
import android.os.Environment
import java.io.File

enum class EdgeModelRole {
    EMBEDDING,
    GENERATIVE_TEXT,
    VISION_LANGUAGE
}

enum class ModelMemorySuitability(val badge: String, val isWarning: Boolean) {
    COMPATIBLE("OPTIMAL FOR HARDWARE", false),
    MARGINAL_AUTO_SWAP("HIGH RAM • AUTO-SWAP", true),
    INSUFFICIENT_HARDWARE_RAM("REQUIRES 6GB+ RAM", true)
}

data class EdgeModelSpec(
    val id: String,
    val displayName: String,
    val shortBadge: String,
    val role: EdgeModelRole,
    val fileName: String,
    val huggingFaceRepo: String,
    val downloadUrl: String,
    val approxDiskMb: Int,
    val approxPeakRssMb: Int,
    val minPhysicalRamMb: Int = 3000,
    val supportsVision: Boolean,
    val supportsToolCalling: Boolean,
    val summary: String,
    val isGated: Boolean = false
) {
    val gateUrl: String
        get() = "https://huggingface.co/$huggingFaceRepo"

    fun evaluateSuitability(telemetry: DeviceMemoryTelemetry): ModelMemorySuitability {
        return when {
            telemetry.physicalTotalMb < (minPhysicalRamMb - 400L) ->
                ModelMemorySuitability.INSUFFICIENT_HARDWARE_RAM
            telemetry.memAvailableMb < approxPeakRssMb ->
                ModelMemorySuitability.MARGINAL_AUTO_SWAP
            else ->
                ModelMemorySuitability.COMPATIBLE
        }
    }
}

/**
 * Switchable AI Edge Gallery model catalog tailored to a 4 GB physical RAM Android 15 device.
 *
 * Resolves `.litertlm` files from app internal `filesDir/models`, app external `files/models`,
 * or `/sdcard/Download` (convenient for `adb push` during testing).
 */
object ModelCatalog {

    val EMBEDDING_GEMMA2_TEXT_VISION_440M = EdgeModelSpec(
        id = "embeddinggemma-2-text-vision-440m",
        displayName = "EmbeddingGemma 2 Text+Vision (440M)",
        shortBadge = "EG2-440M",
        role = EdgeModelRole.EMBEDDING,
        fileName = "embeddinggemma-2-text-vision-440m.litertlm",
        huggingFaceRepo = "litert-community/embeddinggemma-2-text-vision-440m-litert-lm",
        downloadUrl = "https://huggingface.co/litert-community/embeddinggemma-2-text-vision-440m-litert-lm/resolve/main/embeddinggemma-2-text-vision-440m.litertlm",
        approxDiskMb = 370,
        approxPeakRssMb = 450,
        minPhysicalRamMb = 3000,
        supportsVision = true,
        supportsToolCalling = false,
        summary = "Unified 270M Text + 170M Vision encoder sliced via MRL to 256d (no FP16)."
    )

    val EMBEDDING_GEMMA2_TEXT_270M = EdgeModelSpec(
        id = "embeddinggemma-2-text-270m",
        displayName = "EmbeddingGemma 2 Text (270M)",
        shortBadge = "EG2-270M",
        role = EdgeModelRole.EMBEDDING,
        fileName = "embeddinggemma-2-text-270m.litertlm",
        huggingFaceRepo = "litert-community/embeddinggemma-2-text-270m-litert-lm",
        downloadUrl = "https://huggingface.co/litert-community/embeddinggemma-2-text-270m-litert-lm/resolve/main/embeddinggemma-2-text-270m.litertlm",
        approxDiskMb = 175,
        approxPeakRssMb = 310,
        minPhysicalRamMb = 2000,
        supportsVision = false,
        supportsToolCalling = false,
        summary = "Ultra-light 270M text+code embedder & System-1 centroid router."
    )

    val EMBEDDING_GEMMA_300M = EdgeModelSpec(
        id = "embeddinggemma-300m",
        displayName = "EmbeddingGemma 300M",
        shortBadge = "EG-300M",
        role = EdgeModelRole.EMBEDDING,
        fileName = "embeddinggemma-300m.litertlm",
        huggingFaceRepo = "litert-community/embeddinggemma-300m",
        downloadUrl = "https://huggingface.co/litert-community/embeddinggemma-300m/resolve/main/embeddinggemma-300m.litertlm",
        approxDiskMb = 145,
        approxPeakRssMb = 190,
        minPhysicalRamMb = 2000,
        supportsVision = false,
        supportsToolCalling = false,
        summary = "Ultra-compact 300M MRL text embedder with 190MB RAM footprint."
    )

    val QWEN3_0_6B_INT4 = EdgeModelSpec(
        id = "qwen3-0.6b-int4",
        displayName = "Qwen3-0.6B (INT4)",
        shortBadge = "Qwen3-0.6B",
        role = EdgeModelRole.GENERATIVE_TEXT,
        fileName = "Qwen3-0.6B.litertlm",
        huggingFaceRepo = "litert-community/Qwen3-0.6B",
        downloadUrl = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B.litertlm",
        approxDiskMb = 329,
        approxPeakRssMb = 560,
        minPhysicalRamMb = 3000,
        supportsVision = false,
        supportsToolCalling = true,
        summary = "All-around 0.6B local chat, RAG synthesis, and multi-tool agent model."
    )

    val FUNCTION_GEMMA_270M = EdgeModelSpec(
        id = "functiongemma-270m",
        displayName = "FunctionGemma 270M",
        shortBadge = "FnGemma-270M",
        role = EdgeModelRole.GENERATIVE_TEXT,
        fileName = "mobile_actions_q8_ekv1024.litertlm",
        huggingFaceRepo = "litert-community/functiongemma-270m-ft-mobile-actions",
        downloadUrl = "https://huggingface.co/litert-community/functiongemma-270m-ft-mobile-actions/resolve/main/mobile_actions_q8_ekv1024.litertlm",
        approxDiskMb = 285,
        approxPeakRssMb = 360,
        minPhysicalRamMb = 2000,
        supportsVision = false,
        supportsToolCalling = true,
        summary = "130+ tok/s tool-calling specialist for Android native SKILL.md intents.",
        isGated = true
    )

    val GEMMA3_270M_IT = EdgeModelSpec(
        id = "gemma3-270m-it",
        displayName = "Gemma 3 270M-IT",
        shortBadge = "Gemma3-270M",
        role = EdgeModelRole.GENERATIVE_TEXT,
        fileName = "gemma3-270m-it-q8.litertlm",
        huggingFaceRepo = "litert-community/gemma-3-270m-it",
        downloadUrl = "https://huggingface.co/litert-community/gemma-3-270m-it/resolve/main/gemma3-270m-it-q8.litertlm",
        approxDiskMb = 290,
        approxPeakRssMb = 380,
        minPhysicalRamMb = 2000,
        supportsVision = false,
        supportsToolCalling = true,
        summary = "Lowest-RAM Gemma 3 decoder for grounded ObjectBox RAG compression.",
        isGated = true
    )

    val SMOLVLM2_500M_INSTRUCT = EdgeModelSpec(
        id = "smolvlm2-500m-instruct",
        displayName = "SmolVLM2-500M-Instruct",
        shortBadge = "SmolVLM2-500M",
        role = EdgeModelRole.VISION_LANGUAGE,
        fileName = "SmolVLM2-500M.litertlm",
        huggingFaceRepo = "litert-community/SmolVLM2-500M",
        downloadUrl = "https://huggingface.co/litert-community/SmolVLM2-500M/resolve/main/SmolVLM2-500M.litertlm",
        approxDiskMb = 510,
        approxPeakRssMb = 720,
        minPhysicalRamMb = 3500,
        supportsVision = true,
        supportsToolCalling = false,
        summary = "Higher-detail local VLM for offline camera visual Q&A."
    )

    val QWEN35_0_8B = EdgeModelSpec(
        id = "qwen3.5-0.8b",
        displayName = "Qwen3.5-0.8B",
        shortBadge = "Qwen3.5-0.8B",
        role = EdgeModelRole.GENERATIVE_TEXT,
        fileName = "Qwen3.5-0.8B.litertlm",
        huggingFaceRepo = "litert-community/Qwen3.5-0.8B",
        downloadUrl = "https://huggingface.co/litert-community/Qwen3.5-0.8B/resolve/main/Qwen3.5-0.8B.litertlm",
        approxDiskMb = 540,
        approxPeakRssMb = 640,
        minPhysicalRamMb = 3500,
        supportsVision = false,
        supportsToolCalling = true,
        summary = "Next-gen 0.8B hybrid GatedDeltaNet linear attention model with superior reasoning."
    )

    val SMOLLM2_360M_INSTRUCT = EdgeModelSpec(
        id = "smollm2-360m-instruct",
        displayName = "SmolLM2-360M-Instruct",
        shortBadge = "SmolLM2-360M",
        role = EdgeModelRole.GENERATIVE_TEXT,
        fileName = "SmolLM2-360M-Instruct.litertlm",
        huggingFaceRepo = "litert-community/SmolLM2-360M-Instruct",
        downloadUrl = "https://huggingface.co/litert-community/SmolLM2-360M-Instruct/resolve/main/SmolLM2-360M-Instruct.litertlm",
        approxDiskMb = 260,
        approxPeakRssMb = 320,
        minPhysicalRamMb = 2000,
        supportsVision = false,
        supportsToolCalling = true,
        summary = "Ultra-compact 360M model trained on FineWeb-Edu for fast text synthesis."
    )

    val FALCON_H1_0_5B = EdgeModelSpec(
        id = "falcon-h1-0.5b-instruct",
        displayName = "Falcon-H1-0.5B-Instruct",
        shortBadge = "Falcon-0.5B",
        role = EdgeModelRole.GENERATIVE_TEXT,
        fileName = "Falcon-H1-0.5B-Instruct.litertlm",
        huggingFaceRepo = "litert-community/Falcon-H1-0.5B-Instruct",
        downloadUrl = "https://huggingface.co/litert-community/Falcon-H1-0.5B-Instruct/resolve/main/Falcon-H1-0.5B-Instruct.litertlm",
        approxDiskMb = 380,
        approxPeakRssMb = 440,
        minPhysicalRamMb = 3000,
        supportsVision = false,
        supportsToolCalling = true,
        summary = "Parallel Attention + Mamba-2 SSM hybrid architecture for linear memory scaling."
    )

    val GEMMA3_1B_IT = EdgeModelSpec(
        id = "gemma3-1b-it",
        displayName = "Gemma 3 1B-IT",
        shortBadge = "Gemma3-1B",
        role = EdgeModelRole.GENERATIVE_TEXT,
        fileName = "gemma3-1b-it-q8.litertlm",
        huggingFaceRepo = "litert-community/Gemma3-1B-IT",
        downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-q8.litertlm",
        approxDiskMb = 980,
        approxPeakRssMb = 950,
        minPhysicalRamMb = 3500,
        supportsVision = false,
        supportsToolCalling = true,
        summary = "Full 1B Gemma 3 instruct model. Runs in Auto-Swap on 4GB, optimal on 6GB.",
        isGated = true
    )

    val DEEPSEEK_R1_DISTILL_QWEN_1_5B = EdgeModelSpec(
        id = "deepseek-r1-distill-qwen-1.5b",
        displayName = "DeepSeek-R1-Distill-Qwen-1.5B",
        shortBadge = "DeepSeek-1.5B",
        role = EdgeModelRole.GENERATIVE_TEXT,
        fileName = "DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv4096.litertlm",
        huggingFaceRepo = "litert-community/DeepSeek-R1-Distill-Qwen-1.5B",
        downloadUrl = "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv4096.litertlm",
        approxDiskMb = 1830,
        approxPeakRssMb = 1400,
        minPhysicalRamMb = 5500,
        supportsVision = false,
        supportsToolCalling = true,
        summary = "Chain-of-thought edge reasoning model. Recommended for 6GB+ smartwatches."
    )

    val GEMMA4_E2B_IT = EdgeModelSpec(
        id = "gemma4-e2b-it",
        displayName = "Gemma 4 E2B-IT (Multimodal)",
        shortBadge = "Gemma4-2B",
        role = EdgeModelRole.GENERATIVE_TEXT,
        fileName = "gemma-4-E2B-it-litert-lm.litertlm",
        huggingFaceRepo = "litert-community/gemma-4-E2B-it-litert-lm",
        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it-litert-lm.litertlm",
        approxDiskMb = 2580,
        approxPeakRssMb = 2100,
        minPhysicalRamMb = 5500,
        supportsVision = true,
        supportsToolCalling = true,
        summary = "Flagship 2B multimodal edge agent. Recommended for 6GB+ smartwatches.",
        isGated = true
    )

    val EMBEDDING_MODELS = listOf(
        EMBEDDING_GEMMA2_TEXT_VISION_440M,
        EMBEDDING_GEMMA2_TEXT_270M,
        EMBEDDING_GEMMA_300M
    )

    val GENERATIVE_GALLERY_MODELS = listOf(
        SMOLLM2_360M_INSTRUCT,
        QWEN3_0_6B_INT4,
        FALCON_H1_0_5B,
        QWEN35_0_8B,
        GEMMA3_1B_IT,
        FUNCTION_GEMMA_270M,
        GEMMA3_270M_IT,
        SMOLVLM2_500M_INSTRUCT,
        DEEPSEEK_R1_DISTILL_QWEN_1_5B,
        GEMMA4_E2B_IT
    )

    val ALL_MODELS = EMBEDDING_MODELS + GENERATIVE_GALLERY_MODELS

    fun modelsDir(context: Context): File =
        File(context.filesDir, "models").apply { mkdirs() }

    /**
     * Resolves an installed `.litertlm` file across app internal storage, external app storage,
     * and `/sdcard/Download` so developers can either download in-app or `adb push`.
     */
    fun resolveInstalledFile(context: Context, spec: EdgeModelSpec): File? {
        val fileNames = buildList {
            add(spec.fileName)
            if (spec.id == EMBEDDING_GEMMA2_TEXT_VISION_440M.id) {
                add("eg2-gallery-compact.litertlm")
                add("embeddinggemma-2-740m.litertlm")
            }
            if (spec.id == EMBEDDING_GEMMA_300M.id) {
                add("embeddinggemma-300m-it.litertlm")
            }
            if (spec.id == SMOLLM2_360M_INSTRUCT.id) {
                add("smollm2-360m.litertlm")
                add("SmolLM2-360M.litertlm")
            }
            if (spec.id == FALCON_H1_0_5B.id) {
                add("falcon-h1-0.5b.litertlm")
                add("Falcon-H1-0.5B.litertlm")
            }
            if (spec.id == QWEN35_0_8B.id) {
                add("qwen3.5-0.8b.litertlm")
                add("qwen35-0.8b.litertlm")
            }
            if (spec.id == GEMMA3_1B_IT.id) {
                add("gemma3-1b-it.litertlm")
                add("Gemma3-1B-IT.litertlm")
            }
            if (spec.id == FUNCTION_GEMMA_270M.id) {
                add("functiongemma-270m-ft-mobile.litertlm")
                add("functiongemma-270m-ft-mobile-actions.litertlm")
            }
            if (spec.id == GEMMA3_270M_IT.id) {
                add("gemma3-270m-it.litertlm")
            }
            if (spec.id == DEEPSEEK_R1_DISTILL_QWEN_1_5B.id) {
                add("deepseek-r1-distill-qwen-1.5b.litertlm")
                add("DeepSeek-R1-Distill-Qwen-1.5B.litertlm")
            }
            if (spec.id == GEMMA4_E2B_IT.id) {
                add("gemma-4-E2B-it.litertlm")
                add("gemma4-e2b-it.litertlm")
            }
        }

        val candidates = buildList {
            for (fn in fileNames) {
                add(File(modelsDir(context), fn))
                add(File(context.filesDir, fn))
                context.getExternalFilesDir("models")?.let { add(File(it, fn)) }
                context.getExternalFilesDir(null)?.let { add(File(it, fn)) }
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                add(File(downloadsDir, fn))
                add(File(downloadsDir, "models/$fn"))
            }
        }
        return candidates.firstOrNull { it.exists() && it.isFile && it.length() > 1_048_576L }
    }
}
