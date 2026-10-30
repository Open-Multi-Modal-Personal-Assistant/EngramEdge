package dev.engram.edge.ui

import dev.engram.edge.EngramEdgeApplication
import dev.engram.edge.agent.PendingConfirmation
import dev.engram.edge.audio.SpeechUiState
import dev.engram.edge.cloud.LiveBurstStatus
import dev.engram.edge.data.memory.HybridMemoryResult
import dev.engram.edge.data.memory.IngestionOutcome
import dev.engram.edge.data.memory.MemoryChunkEntity
import dev.engram.edge.edge.DeviceMemoryTelemetry
import dev.engram.edge.edge.EdgeModelSpec
import dev.engram.edge.edge.GalleryRuntimeState
import dev.engram.edge.edge.ResidencyMode
import dev.engram.edge.router.SystemOneDecision
import dev.engram.edge.security.EncryptedTokenStore
import dev.engram.edge.security.ShieldVerdict
import dev.engram.edge.skills.AndroidSkillExecutor
import dev.engram.edge.skills.RunIntentArgs
import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EngramConversationUiState(
    val queryInput: String = "",
    val streamingAnswer: String = "Tap a quick prompt or hold Push-to-Talk. Works 100% offline via ObjectBox 256d HNSW + Android 15 AppSearch BM25F!",
    val isRunningTurn: Boolean = false,
    val isOnlineMode: Boolean = true,
    val autoSpeakReplies: Boolean = true,
    val executedTierBadge: String = "LOCAL READY",
    val citations: List<HybridMemoryResult> = emptyList(),
    val lastOcrStatus: String = "Point camera at a sign/receipt or tap a synthetic OCR test below.",
    val lastExtractedOcrText: String? = null,
    val lastStoredMemoryId: Long? = null,
    val lastStoredMemoryTitle: String? = null,
    val lastRetrievalDemo: List<HybridMemoryResult> = emptyList(),
    val isTorchActive: Boolean = false,
    val lastCapturedJpeg: ByteArray? = null,
    val hfTokenInput: String = ""
)

class EngramEdgeViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as EngramEdgeApplication).container
    private val tokenStore = EncryptedTokenStore(app)

    private val _uiState = MutableStateFlow(EngramConversationUiState())
    val uiState: StateFlow<EngramConversationUiState> = _uiState.asStateFlow()

    init {
        val savedToken = tokenStore.loadHfToken().orEmpty()
        if (savedToken.isNotBlank()) {
            _uiState.update { it.copy(hfTokenInput = savedToken) }
        }
    }

    val memoryTelemetry: StateFlow<DeviceMemoryTelemetry> = container.residencyMonitor.telemetry
    val galleryState: StateFlow<GalleryRuntimeState> = container.galleryManager.state
    val recentMemories: StateFlow<List<MemoryChunkEntity>> = container.memoryRepository.recentMemories
    val reindexProgress: StateFlow<dev.engram.edge.data.memory.ReindexProgress> = container.memoryRepository.reindexProgress
    val lastDecision: StateFlow<SystemOneDecision?> = container.koogOrchestrator.lastDecision
    val pendingConfirmation: StateFlow<PendingConfirmation?> = container.koogOrchestrator.pendingConfirmation
    val lastShieldVerdict: StateFlow<ShieldVerdict?> = container.promptShield.lastVerdict
    val liveBurstStatus: StateFlow<LiveBurstStatus> = container.cloudBridge.liveBurstStatus
    val speechState: StateFlow<SpeechUiState> = container.speechController.state

    fun updateQueryInput(text: String) {
        _uiState.update { it.copy(queryInput = text) }
    }

    fun updateHfToken(token: String) {
        _uiState.update { it.copy(hfTokenInput = token) }
        tokenStore.saveHfToken(token)
    }

    fun toggleOnlineMode() {
        _uiState.update {
            val next = !it.isOnlineMode
            it.copy(
                isOnlineMode = next,
                executedTierBadge = if (next) "HYBRID ARMED" else "AIRPLANE EDGE"
            )
        }
    }

    fun toggleAutoSpeak() {
        _uiState.update { it.copy(autoSpeakReplies = !it.autoSpeakReplies) }
    }

    private var turnJob: kotlinx.coroutines.Job? = null

    private fun sanitizeAnswer(raw: String): String {
        if (!raw.contains("<think>")) return raw
        return if (raw.contains("</think>")) {
            raw.substringAfter("</think>").trim()
        } else {
            "Reasoning on wrist..."
        }
    }

    fun submitQuery(explicitQuery: String? = null, attachLastSnapshot: Boolean = false) {
        val q = (explicitQuery ?: _uiState.value.queryInput).trim()
        if (q.isEmpty()) return

        turnJob?.cancel()

        _uiState.update {
            it.copy(
                queryInput = q,
                isRunningTurn = true,
                streamingAnswer = "Routing via System-1..."
            )
        }

        turnJob = viewModelScope.launch {
            try {
                val imgBytes = if (attachLastSnapshot) _uiState.value.lastCapturedJpeg else null
                val response = container.koogOrchestrator.runTurn(
                    utterance = q,
                    isOnline = _uiState.value.isOnlineMode,
                    attachedImageBytes = imgBytes,
                    onStreamToken = { partial ->
                        _uiState.update { st -> st.copy(streamingAnswer = sanitizeAnswer(partial)) }
                    }
                )

                val cleanFinal = sanitizeAnswer(response.answerText)
                _uiState.update {
                    it.copy(
                        isRunningTurn = false,
                        streamingAnswer = cleanFinal,
                        executedTierBadge = response.executedTierLabel,
                        citations = response.citations
                    )
                }

                if (_uiState.value.autoSpeakReplies && response.pendingConfirmation == null) {
                    container.speechController.speakConciseReply(cleanFinal)
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Expected when user issues a new turn before old finishes
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isRunningTurn = false,
                        streamingAnswer = "Error processing turn: ${t.message ?: t::class.simpleName}",
                        executedTierBadge = "ERROR"
                    )
                }
            }
        }
    }

    fun confirmPendingSkill() {
        viewModelScope.launch {
            val res = container.koogOrchestrator.confirmPendingWristSkill() ?: return@launch
            _uiState.update {
                it.copy(
                    streamingAnswer = res.answerText,
                    executedTierBadge = res.executedTierLabel
                )
            }
            if (_uiState.value.autoSpeakReplies) {
                container.speechController.speakConciseReply(res.answerText)
            }
        }
    }

    fun dismissPendingSkill() {
        container.koogOrchestrator.dismissPendingConfirmation()
        _uiState.update {
            it.copy(
                streamingAnswer = "Action cancelled.",
                executedTierBadge = "CANCELLED"
            )
        }
    }

    fun togglePushToTalk() {
        if (speechState.value.isListening) {
            container.speechController.stopListening()
        } else {
            _uiState.update {
                it.copy(
                    streamingAnswer = "Listening for voice input... Speak now.",
                    executedTierBadge = "MICROPHONE ACTIVE"
                )
            }
            container.speechController.startPushToTalk(
                preferOffline = !_uiState.value.isOnlineMode,
                onError = { err ->
                    _uiState.update { st ->
                        st.copy(
                            streamingAnswer = "Speech recognition: $err",
                            executedTierBadge = "VOICE RETRY"
                        )
                    }
                }
            ) { recognized ->
                _uiState.update { it.copy(queryInput = recognized) }
                submitQuery(recognized)
            }
        }
    }

    fun captureAndIngestBitmap(bitmap: Bitmap, fallbackLabel: String = "Snapshot") {
        viewModelScope.launch {
            _uiState.update { it.copy(lastOcrStatus = "Running on-device ML Kit OCR + PromptInjectionShield...") }
            val processed = container.cameraOcrManager.processBitmap(bitmap)
            val hasOcrText = processed.extractedOcrText.isNotBlank()
            val isSyntheticCustom = fallbackLabel.isNotBlank() && fallbackLabel != "Snapshot"
            val textBody = when {
                hasOcrText -> processed.extractedOcrText
                isSyntheticCustom -> fallbackLabel
                else -> "[Camera Frame: No printed text detected by ML Kit OCR]"
            }
            val title = when {
                hasOcrText -> textBody.lines().firstOrNull { it.isNotBlank() }?.take(36)?.trim() ?: "Snapshot"
                isSyntheticCustom -> fallbackLabel.lines().firstOrNull { it.isNotBlank() }?.take(36)?.trim() ?: "Snapshot"
                else -> "Visual Frame #${(System.currentTimeMillis() / 1000) % 10000}"
            }
            val modality = if (hasOcrText || isSyntheticCustom) {
                MemoryChunkEntity.Modality.OCR
            } else {
                MemoryChunkEntity.Modality.IMAGE
            }

            val outcome = container.memoryRepository.ingestTextOrOcrMemory(
                title = title,
                contentText = textBody,
                modality = modality,
                isTrustedSource = false, // Forces Chokepoint A PromptInjectionShield inspection!
                rawImageBytes = processed.jpegBytes
            )

            when (outcome) {
                is IngestionOutcome.Stored -> {
                    val demoHits = container.memoryRepository.hybridSearch(
                        query = outcome.entity.title,
                        limit = 2
                    ).results

                    val statusMessage = if (hasOcrText || isSyntheticCustom) {
                        "Indexed #${outcome.entity.id} '${outcome.entity.title}' into ObjectBox 256d HNSW + AppSearch BM25F!"
                    } else {
                        "No text found by ML Kit OCR. Stored 256d visual embedding in ObjectBox! (Use 'TEST BENIGN SIGN' for OCR demo, or ASK with photo for VLM description)."
                    }

                    _uiState.update {
                        it.copy(
                            lastCapturedJpeg = processed.jpegBytes,
                            executedTierBadge = if (hasOcrText) "VAULT INDEXED" else "VISUAL STORED",
                            lastOcrStatus = statusMessage,
                            lastExtractedOcrText = if (hasOcrText || isSyntheticCustom) textBody else "(No printed text found in camera frame)",
                            lastStoredMemoryId = outcome.entity.id,
                            lastStoredMemoryTitle = outcome.entity.title,
                            lastRetrievalDemo = demoHits
                        )
                    }
                }
                is IngestionOutcome.BlockedByPromptShield -> {
                    _uiState.update {
                        it.copy(
                            executedTierBadge = "SHIELD BLOCKED",
                            lastOcrStatus = "BLOCKED (${outcome.verdict.triggeredCategory}, p=${"%.2f".format(outcome.verdict.injectionProbability)}): ${outcome.verdict.reason}",
                            lastExtractedOcrText = textBody,
                            lastRetrievalDemo = emptyList()
                        )
                    }
                }
            }
        }
    }

    fun queryVaultDirectly(query: String) {
        viewModelScope.launch {
            val res = container.memoryRepository.hybridSearch(query = query, limit = 2)
            _uiState.update { it.copy(lastRetrievalDemo = res.results) }
        }
    }

    fun toggleTorchDirectly() {
        viewModelScope.launch {
            val nextState = !_uiState.value.isTorchActive
            val res = container.skillExecutor.executeRunIntent(
                RunIntentArgs(
                    action = AndroidSkillExecutor.ACTION_TOGGLE_TORCH,
                    mode = if (nextState) "ON" else "OFF"
                )
            )
            _uiState.update {
                it.copy(
                    isTorchActive = nextState,
                    streamingAnswer = res.summary,
                    executedTierBadge = "TORCH ${if (nextState) "ON" else "OFF"}"
                )
            }
        }
    }

    fun runSyntheticOcrDemo(adversarialAttack: Boolean) {
        val (bmp, syntheticText) = container.cameraOcrManager.createSyntheticSignBitmap(adversarialAttack)
        captureAndIngestBitmap(bmp, fallbackLabel = syntheticText)
    }

    fun toggleLiveBurst() {
        if (liveBurstStatus.value.isStreaming) {
            container.cloudBridge.stopLiveBurst()
        } else {
            container.cloudBridge.startOneFpsLiveBurst(_uiState.value.queryInput)
        }
    }

    fun sendOneFpsSyntheticOrCameraFrame(cameraBitmap: Bitmap?) {
        val bmp = cameraBitmap ?: container.cameraOcrManager.createSyntheticSignBitmap(adversarialAttack = false).first
        container.cloudBridge.submitOneFpsCameraFrame(
            frameBitmap = bmp,
            voicePrompt = _uiState.value.queryInput
        )
    }

    fun selectGalleryModel(spec: EdgeModelSpec) {
        viewModelScope.launch {
            container.galleryManager.selectAndLoadModel(spec)
        }
    }

    fun downloadGalleryModel(spec: EdgeModelSpec) {
        viewModelScope.launch {
            val token = _uiState.value.hfTokenInput.ifBlank { tokenStore.loadHfToken().orEmpty() }.takeIf { it.isNotBlank() }
            val res = container.galleryManager.downloadModelFromHuggingFace(spec, token)
            if (res.isSuccess) {
                container.galleryManager.selectAndLoadModel(spec)
            }
        }
    }

    fun cycleResidencyPolicy() {
        val current = memoryTelemetry.value.residencyMode
        val next = if (current == ResidencyMode.CO_RESIDENT) {
            ResidencyMode.AUTO_SWAP_ON_DEMAND
        } else {
            ResidencyMode.CO_RESIDENT
        }
        container.residencyMonitor.setPinnedResidencyMode(next)
    }

    override fun onCleared() {
        super.onCleared()
        container.speechController.close()
    }
}
