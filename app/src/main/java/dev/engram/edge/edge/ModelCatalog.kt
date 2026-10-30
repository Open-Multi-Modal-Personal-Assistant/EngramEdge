package dev.engram.edge.edge

import android.content.Context
import android.os.Environment
import java.io.File

enum class EdgeModelRole {
    EMBEDDING,
    GENERATIVE_TEXT,
    VISION_LANGUAGE
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
    val supportsVision: Boolean,
    val supportsToolCalling: Boolean,
    val summary: String
)

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
        supportsVision = false,
        supportsToolCalling = false,
        summary = "Ultra-light 270M text+code embedder & System-1 centroid router."
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
        supportsVision = false,
        supportsToolCalling = true,
        summary = "130+ tok/s tool-calling specialist for Android native SKILL.md intents."
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
        supportsVision = false,
        supportsToolCalling = true,
        summary = "Lowest-RAM Gemma 3 decoder for grounded ObjectBox RAG compression."
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
        supportsVision = true,
        supportsToolCalling = false,
        summary = "Higher-detail local VLM for offline camera visual Q&A."
    )

    val EMBEDDING_MODELS = listOf(
        EMBEDDING_GEMMA2_TEXT_VISION_440M,
        EMBEDDING_GEMMA2_TEXT_270M
    )

    val GENERATIVE_GALLERY_MODELS = listOf(
        QWEN3_0_6B_INT4,
        FUNCTION_GEMMA_270M,
        GEMMA3_270M_IT,
        SMOLVLM2_500M_INSTRUCT
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
            if (spec.id == FUNCTION_GEMMA_270M.id) {
                add("functiongemma-270m-ft-mobile.litertlm")
                add("functiongemma-270m-ft-mobile-actions.litertlm")
            }
            if (spec.id == GEMMA3_270M_IT.id) {
                add("gemma3-270m-it.litertlm")
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
