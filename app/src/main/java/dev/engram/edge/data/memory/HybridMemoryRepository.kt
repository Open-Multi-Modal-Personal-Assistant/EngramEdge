package dev.engram.edge.data.memory

import dev.engram.edge.edge.EmbeddingGemmaEngine
import dev.engram.edge.security.PromptInjectionShield
import dev.engram.edge.security.ShieldVerdict
import dev.engram.edge.state.EngramAppStateCoordinator
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.query.QueryCondition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class HybridMemoryResult(
    val chunk: MemoryChunkEntity,
    val rrfScore: Double,
    val cosineDistance: Double?,
    val bm25fScore: Double?,
    val vectorRank: Int?,
    val lexicalRank: Int?
)

data class HybridSearchResponse(
    val query: String,
    val results: List<HybridMemoryResult>,
    val passedGroundingGate: Boolean,
    val latencyMs: Long,
    val embedderBackend: String,
    val lexicalBackend: String
)

sealed interface IngestionOutcome {
    data class Stored(val entity: MemoryChunkEntity) : IngestionOutcome
    data class BlockedByPromptShield(val verdict: ShieldVerdict) : IngestionOutcome
}

data class ReindexProgress(
    val current: Int = 0,
    val total: Int = 0,
    val isRunning: Boolean = false
) {
    val progressFraction: Float
        get() = if (total > 0) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
}

/**
 * Coordinates ObjectBox 6 HNSW (256d MRL EmbeddingGemma 2 vectors) with Android 15
 * AppSearch PlatformStorage (BM25F) via Reciprocal Rank Fusion (RRF), gated on ingress
 * by the on-device [PromptInjectionShield].
 */
class HybridMemoryRepository(
    boxStore: BoxStore,
    private val appSearchIndex: AppSearchLexicalIndex,
    private val embeddingEngine: EmbeddingGemmaEngine,
    private val promptShield: PromptInjectionShield,
    private val appState: EngramAppStateCoordinator? = null
) {
    private val box: Box<MemoryChunkEntity> = boxStore.boxFor(MemoryChunkEntity::class.java)

    private val _recentMemories = MutableStateFlow<List<MemoryChunkEntity>>(emptyList())
    val recentMemories: StateFlow<List<MemoryChunkEntity>> = _recentMemories.asStateFlow()

    private val _reindexProgress = MutableStateFlow(ReindexProgress())
    val reindexProgress: StateFlow<ReindexProgress> = _reindexProgress.asStateFlow()

    suspend fun initializeAndSeedDemoVault() = withContext(Dispatchers.IO) {
        appSearchIndex.ensureInitialized()
        if (box.isEmpty) {
            seedHackathonDemoMemories()
        }
        refreshRecentMemories()
    }

    suspend fun ingestTextOrOcrMemory(
        title: String,
        contentText: String,
        modality: MemoryChunkEntity.Modality = MemoryChunkEntity.Modality.TEXT,
        sourceUri: String = "",
        tagsCsv: String = "",
        isTrustedSource: Boolean = modality != MemoryChunkEntity.Modality.OCR,
        rawImageBytes: ByteArray? = null
    ): IngestionOutcome = withContext(Dispatchers.IO) {
        if (!isTrustedSource) {
            val verdict = promptShield.inspectUntrustedText("$title\n$contentText")
            if (!verdict.isSafe) {
                return@withContext IngestionOutcome.BlockedByPromptShield(verdict)
            }
        }

        val vector256 = if (rawImageBytes != null && rawImageBytes.isNotEmpty() && contentText.isBlank()) {
            embeddingEngine.embedImage256(rawImageBytes)
        } else {
            val formattedDoc = HybridSearchMath.formatDocumentText(title, contentText)
            val textVec = embeddingEngine.embedRawText256(formattedDoc)
            if (rawImageBytes != null && rawImageBytes.isNotEmpty()) {
                val imgVec = embeddingEngine.embedImage256(rawImageBytes)
                blendAndNormalize(textVec, imgVec)
            } else {
                textVec
            }
        }

        val entity = MemoryChunkEntity(
            modalityType = modality.name,
            title = title.ifBlank { "none" },
            contentText = contentText.take(4_000),
            sourceUri = sourceUri,
            tagsCsv = tagsCsv,
            isTrustedSource = isTrustedSource,
            timestampMillis = System.currentTimeMillis(),
            embedding = vector256,
            embeddingModelId = embeddingEngine.activeModelId
        )

        box.put(entity)
        appSearchIndex.putMemory(entity)
        refreshRecentMemories()
        appState?.recordVaultBookmark(
            memoryId = entity.id,
            memoryTitle = entity.title,
            totalCount = box.count().toInt()
        )
        IngestionOutcome.Stored(entity)
    }

    suspend fun hybridSearch(
        query: String,
        modalityFilter: MemoryChunkEntity.Modality? = null,
        sinceTimestampMillis: Long? = null,
        limit: Int = 4
    ): HybridSearchResponse = withContext(Dispatchers.IO) {
        val startNs = System.nanoTime()

        val queryVec = embeddingEngine.embedRawText256(HybridSearchMath.formatSearchQuery(query))
        val candidatePool = (limit * 4).coerceAtLeast(16)

        var condition: QueryCondition<MemoryChunkEntity> =
            MemoryChunkEntity_.embedding.nearestNeighbors(queryVec, candidatePool)
        if (modalityFilter != null) {
            condition = condition.and(MemoryChunkEntity_.modalityType.equal(modalityFilter.name))
        }
        if (sinceTimestampMillis != null) {
            condition = condition.and(MemoryChunkEntity_.timestampMillis.greater(sinceTimestampMillis))
        }

        val obxResults = box.query(condition).build().use { it.findWithScores() }
        val vectorHits = obxResults.map { scored ->
            HybridSearchMath.VectorHit(
                objectBoxId = scored.get().id,
                cosineDistance = scored.score
            )
        }

        val lexicalHits = appSearchIndex.searchBm25f(
            rawQuery = query,
            modalityFilter = modalityFilter,
            maxResults = candidatePool
        )

        val fused = HybridSearchMath.fuseReciprocalRank(
            vectorHits = vectorHits,
            lexicalHits = lexicalHits,
            limit = limit
        )
        val passedGate = HybridSearchMath.passesGroundingGate(fused)

        val entitiesById = box.getMap(fused.map { it.objectBoxId })
        val hydrated = fused.mapNotNull { hit ->
            val chunk = entitiesById[hit.objectBoxId] ?: return@mapNotNull null
            HybridMemoryResult(
                chunk = chunk,
                rrfScore = hit.rrfScore,
                cosineDistance = hit.cosineDistance,
                bm25fScore = hit.bm25fScore,
                vectorRank = hit.vectorRank,
                lexicalRank = hit.lexicalRank
            )
        }

        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
        HybridSearchResponse(
            query = query,
            results = hydrated,
            passedGroundingGate = passedGate,
            latencyMs = elapsedMs,
            embedderBackend = embeddingEngine.activeModeLabel,
            lexicalBackend = appSearchIndex.activeBackendName
        )
    }

    suspend fun deleteMemory(id: Long) = withContext(Dispatchers.IO) {
        box.remove(id)
        appSearchIndex.removeMemory(id)
        refreshRecentMemories()
    }

    fun count(): Long = box.count()

    /**
     * Counts how many stored chunks have an [MemoryChunkEntity.embeddingModelId] that does not match
     * the currently active embedder ([EmbeddingGemmaEngine.activeModelId]).
     */
    fun countMismatchedEmbeddings(): Int {
        val currentModel = embeddingEngine.activeModelId
        val chunks = box.all
        return chunks.count { it.embeddingModelId != currentModel }
    }

    /**
     * Recomputes 256-dimensional MRL embeddings for all stored memory chunks using the
     * currently active embedder, updating [MemoryChunkEntity.embedding] and [MemoryChunkEntity.embeddingModelId].
     *
     * Runs in batches on Dispatchers.IO, updating [_reindexProgress] for UI visibility.
     * Returns the count of re-embedded chunks.
     */
    suspend fun reindexAllWithActiveEmbeddingModel(): Int = withContext(Dispatchers.IO) {
        val currentModel = embeddingEngine.activeModelId
        val chunks = box.all
        val mismatched = chunks.filter { it.embeddingModelId != currentModel }
        if (mismatched.isEmpty()) {
            _reindexProgress.value = ReindexProgress(0, 0, false)
            return@withContext 0
        }

        val total = mismatched.size
        _reindexProgress.value = ReindexProgress(0, total, true)

        try {
            var processed = 0
            val batchSize = 10
            for (batch in mismatched.chunked(batchSize)) {
                for (chunk in batch) {
                    val formatted = HybridSearchMath.formatDocumentText(chunk.title, chunk.contentText)
                    chunk.embedding = embeddingEngine.embedRawText256(formatted)
                    chunk.embeddingModelId = currentModel
                    processed++
                    _reindexProgress.value = ReindexProgress(processed, total, true)
                }
                box.put(batch)
            }
            refreshRecentMemories()
            processed
        } finally {
            _reindexProgress.value = ReindexProgress(total, total, false)
        }
    }

    private fun refreshRecentMemories() {
        val latest = box.query()
            .orderDesc(MemoryChunkEntity_.timestampMillis)
            .build()
            .use { it.find(0, 25) }
        _recentMemories.value = latest
        appState?.updateVaultTotalCount(box.count().toInt())
    }

    private fun blendAndNormalize(a: FloatArray, b: FloatArray): FloatArray {
        val out = FloatArray(HybridSearchMath.MRL_DIMENSIONS)
        for (i in out.indices) {
            out[i] = (a[i] * 0.55f) + (b[i] * 0.45f)
        }
        return HybridSearchMath.truncateAndNormalizeMrl(out, HybridSearchMath.MRL_DIMENSIONS)
    }

    private suspend fun seedHackathonDemoMemories() {
        val samples = listOf(
            Triple(
                "Garage Parking Spot",
                "Parked rental EV (plate 8KZM419) on Level P3, Pillar Green-14 near the North elevator.",
                MemoryChunkEntity.Modality.IMAGE to "parking,car,garage,plate"
            ),
            Triple(
                "Hotel Wi-Fi & Room Door Code",
                "Grand Hyatt Room 1408. Guest Wi-Fi passcode: EdgeEngram2026! Late checkout confirmed for 2:00 PM.",
                MemoryChunkEntity.Modality.OCR to "hotel,wifi,room,code"
            ),
            Triple(
                "Medication & Allergy Profile",
                "Allergic to penicillin. Taking Vitamin D3 2000 IU each morning after breakfast.",
                MemoryChunkEntity.Modality.VOICE to "health,allergy,medication"
            ),
            Triple(
                "EngramEdge Launch Checklist",
                "Demo airplane-mode instant hybrid recall on sw426dp/sw360dp display, live PSS RAM pill, Prompt-Guard OCR defense, and Gemini Live 1fps camera burst.",
                MemoryChunkEntity.Modality.TEXT to "hackathon,demo,pitch,engram"
            )
        )
        for ((title, body, meta) in samples) {
            ingestTextOrOcrMemory(
                title = title,
                contentText = body,
                modality = meta.first,
                tagsCsv = meta.second,
                isTrustedSource = true
            )
        }
    }
}
