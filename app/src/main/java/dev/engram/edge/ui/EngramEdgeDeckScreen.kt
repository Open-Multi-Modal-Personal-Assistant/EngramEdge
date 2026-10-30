package dev.engram.edge.ui

import dev.engram.edge.agent.PendingConfirmation
import dev.engram.edge.cloud.LiveBurstStatus
import dev.engram.edge.data.memory.HybridMemoryResult
import dev.engram.edge.edge.DeviceMemoryTelemetry
import dev.engram.edge.edge.EdgeModelRole
import dev.engram.edge.edge.EdgeModelSpec
import dev.engram.edge.edge.GalleryRuntimeState
import dev.engram.edge.edge.ModelCatalog
import dev.engram.edge.edge.ModelInstallStatus
import dev.engram.edge.router.SystemOneDecision
import dev.engram.edge.security.ShieldVerdict
import dev.engram.edge.skills.AndroidSkillExecutor
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreviewUseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.engram.edge.audio.SpeechUiState
import kotlinx.coroutines.launch

@Composable
fun EngramEdgeRootScreen(viewModel: EngramEdgeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val memory by viewModel.memoryTelemetry.collectAsStateWithLifecycle()
    val gallery by viewModel.galleryState.collectAsStateWithLifecycle()
    val decision by viewModel.lastDecision.collectAsStateWithLifecycle()
    val pending by viewModel.pendingConfirmation.collectAsStateWithLifecycle()
    val shieldVerdict by viewModel.lastShieldVerdict.collectAsStateWithLifecycle()
    val liveBurst by viewModel.liveBurstStatus.collectAsStateWithLifecycle()
    val speech by viewModel.speechState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) viewModel.togglePushToTalk()
    }

    val camPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val systemSpeechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val text = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            .orEmpty()
            .trim()
        if (text.isNotBlank()) {
            viewModel.updateQueryInput(text)
            viewModel.submitQuery(text)
        }
    }

    EngramEdgeDeckContent(
        uiState = uiState,
        memory = memory,
        gallery = gallery,
        lastDecision = decision,
        pendingConfirmation = pending,
        lastShieldVerdict = shieldVerdict,
        liveBurst = liveBurst,
        speech = speech,
        hasCameraPermission = hasCameraPermission,
        onToggleOnline = viewModel::toggleOnlineMode,
        onQueryChange = viewModel::updateQueryInput,
        onSubmitQuery = { q, withSnap -> viewModel.submitQuery(q, withSnap) },
        onPushToTalk = {
            if (hasMicPermission) viewModel.togglePushToTalk()
            else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        onLaunchSystemVoiceDialog = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to EngramEdge...")
            }
            systemSpeechLauncher.launch(intent)
        },
        onConfirmSkill = viewModel::confirmPendingSkill,
        onDismissSkill = viewModel::dismissPendingSkill,
        onRequestCameraPermission = { camPermLauncher.launch(Manifest.permission.CAMERA) },
        onCaptureBitmap = { bmp -> viewModel.captureAndIngestBitmap(bmp) },
        onRunSyntheticOcr = viewModel::runSyntheticOcrDemo,
        onToggleLiveBurst = viewModel::toggleLiveBurst,
        onSendLiveFrame = viewModel::sendOneFpsSyntheticOrCameraFrame,
        onSelectModel = viewModel::selectGalleryModel,
        onDownloadModel = viewModel::downloadGalleryModel,
        onUpdateHfToken = viewModel::updateHfToken,
        onCycleResidency = viewModel::cycleResidencyPolicy,
        onQueryVaultDirectly = viewModel::queryVaultDirectly,
        onToggleTorch = viewModel::toggleTorchDirectly
    )
}

@Composable
fun EngramEdgeDeckContent(
    uiState: EngramConversationUiState,
    memory: DeviceMemoryTelemetry,
    gallery: GalleryRuntimeState,
    lastDecision: SystemOneDecision?,
    pendingConfirmation: PendingConfirmation?,
    lastShieldVerdict: ShieldVerdict?,
    liveBurst: LiveBurstStatus,
    speech: SpeechUiState,
    hasCameraPermission: Boolean,
    onToggleOnline: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSubmitQuery: (String?, Boolean) -> Unit,
    onPushToTalk: () -> Unit,
    onLaunchSystemVoiceDialog: () -> Unit,
    onConfirmSkill: () -> Unit,
    onDismissSkill: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onCaptureBitmap: (Bitmap) -> Unit,
    onRunSyntheticOcr: (Boolean) -> Unit,
    onToggleLiveBurst: () -> Unit,
    onSendLiveFrame: (Bitmap?) -> Unit,
    onSelectModel: (EdgeModelSpec) -> Unit,
    onDownloadModel: (EdgeModelSpec) -> Unit,
    onUpdateHfToken: (String) -> Unit,
    onCycleResidency: () -> Unit,
    onQueryVaultDirectly: (String) -> Unit,
    onToggleTorch: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val tabLabels = listOf("ASK", "SNAP", "LIVE", "MODELS")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = EngramColors.AmoledBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(EngramDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TelemetryHudPill(
                memory = memory,
                activeModelShort = if (gallery.loadedSpec != null) {
                    val tokenK = if (gallery.activeMaxTokens >= 1024) "${gallery.activeMaxTokens / 1024}k" else "${gallery.activeMaxTokens}t"
                    "${gallery.loadedSpec.shortBadge} [$tokenK]"
                } else {
                    gallery.selectedSpec.shortBadge
                },
                tierBadge = uiState.executedTierBadge,
                sysOneLatencyMs = lastDecision?.latencyMs,
                isOnlineSimulated = uiState.isOnlineMode,
                onToggleOnlineMode = onToggleOnline
            )

            // Large swipeable/tappable 4-card watch navigation strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabLabels.forEachIndexed { index, title ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selected) EngramColors.EmeraldEdge else EngramColors.CardSurface
                            )
                            .clickable { scope.launch { pagerState.animateScrollToPage(index) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) Color(0xFF002110) else EngramColors.TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> AskAndVoiceCard(
                        uiState = uiState,
                        lastDecision = lastDecision,
                        pendingConfirmation = pendingConfirmation,
                        speech = speech,
                        onQueryChange = onQueryChange,
                        onSubmitQuery = onSubmitQuery,
                        onPushToTalk = onPushToTalk,
                        onLaunchSystemVoiceDialog = onLaunchSystemVoiceDialog,
                        onConfirmSkill = onConfirmSkill,
                        onDismissSkill = onDismissSkill
                    )
                    1 -> SnapOcrShieldCard(
                        uiState = uiState,
                        lastShieldVerdict = lastShieldVerdict,
                        hasCameraPermission = hasCameraPermission,
                        isActive = (pagerState.currentPage == 1),
                        onRequestCameraPermission = onRequestCameraPermission,
                        onCaptureBitmap = onCaptureBitmap,
                        onRunSyntheticOcr = onRunSyntheticOcr,
                        onAskAboutSnapshot = { prompt ->
                            scope.launch { pagerState.animateScrollToPage(0) }
                            onSubmitQuery(prompt, true)
                        },
                        onTestRetrieval = onQueryVaultDirectly,
                        onToggleTorch = onToggleTorch
                    )
                    2 -> GeminiLiveBurstCard(
                        liveBurst = liveBurst,
                        onToggleLiveBurst = onToggleLiveBurst,
                        onSendLiveFrame = { onSendLiveFrame(null) }
                    )
                    3 -> EdgeModelGalleryCard(
                        memory = memory,
                        gallery = gallery,
                        hfToken = uiState.hfTokenInput,
                        onUpdateHfToken = onUpdateHfToken,
                        onSelectModel = onSelectModel,
                        onDownloadModel = onDownloadModel,
                        onCycleResidency = onCycleResidency
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AskAndVoiceCard(
    uiState: EngramConversationUiState,
    lastDecision: SystemOneDecision?,
    pendingConfirmation: PendingConfirmation?,
    speech: SpeechUiState,
    onQueryChange: (String) -> Unit,
    onSubmitQuery: (String?, Boolean) -> Unit,
    onPushToTalk: () -> Unit,
    onLaunchSystemVoiceDialog: () -> Unit,
    onConfirmSkill: () -> Unit,
    onDismissSkill: () -> Unit
) {
    val quickPrompts = buildList {
        if (!uiState.lastStoredMemoryTitle.isNullOrBlank()) {
            add("Snap #${uiState.lastStoredMemoryId ?: "1"}?" to "What was the text and pin code in my ${uiState.lastStoredMemoryTitle}?")
        }
        if (uiState.lastCapturedJpeg != null) {
            add("Describe Snap" to "Describe what you see in this photo snapshot")
        }
        add("Parked Car?" to "Where did I park my rental EV and what is the plate?")
        add("Hotel Wi-Fi?" to "What is my Grand Hyatt room number and Wi-Fi passcode?")
        add("Torch" to "Toggle watch flashlight torch")
        add("Live HR" to "Measure my heart rate right now")
        add("Log Vitals" to "Log my watch vitals and heart rate to memory")
        add("5m Timer" to "Start a countdown timer for 5 minutes")
        add("BT Panel" to "Open Bluetooth settings on watch")
        add("Nearby Maps" to "Find nearby coffee shops open right now around me")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Huge 80dp Push-to-Talk button tailored to >2" wrist tap ergonomics
        Button(
            onClick = onPushToTalk,
            modifier = Modifier
                .fillMaxWidth()
                .height(EngramDimens.PrimaryTouchHeight),
            shape = RoundedCornerShape(EngramDimens.CardCornerRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (speech.isListening) EngramColors.CoralDanger else EngramColors.EmeraldEdge,
                contentColor = Color(0xFF001F0E)
            )
        ) {
            Text(
                text = if (speech.isListening) "LISTENING... TAP TO STOP" else "PUSH TO TALK (OFFLINE STT)",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Live voice recognition transcription or error feedback
        if (speech.isListening) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF002914))
                    .border(1.dp, EngramColors.EmeraldEdge, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🎙️", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = speech.partialTranscript.ifBlank { "Listening... speak near wrist mic" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = EngramColors.EmeraldEdge
                )
            }
        } else if (!speech.lastError.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF331414))
                    .border(1.dp, EngramColors.CoralDanger, RoundedCornerShape(14.dp))
                    .clickable { onLaunchSystemVoiceDialog() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⚠️", style = MaterialTheme.typography.titleMedium)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = speech.lastError,
                        style = MaterialTheme.typography.bodySmall,
                        color = EngramColors.CoralDanger
                    )
                    Text(
                        text = "Tap to speak with Google Voice Dialog",
                        style = MaterialTheme.typography.labelSmall,
                        color = EngramColors.EmeraldEdge
                    )
                }
            }
        }

        // One-tap wrist presets so hackathon demos take zero typing on a 2-inch screen
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for ((shortLabel, fullPrompt) in quickPrompts) {
                OutlinedButton(
                    onClick = { onSubmitQuery(fullPrompt, shortLabel == "Describe Snap") },
                    modifier = Modifier.heightIn(min = 52.dp),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(shortLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Mid-confidence System-1 confirmation card (0.50 <= P_max < 0.80)
        if (pendingConfirmation != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background( Color(0xFF2B2105) )
                    .border(2.dp, EngramColors.AmberWarn, RoundedCornerShape(18.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Confirm Watch Skill: ${pendingConfirmation.summaryLabel}",
                    style = MaterialTheme.typography.titleMedium,
                    color = EngramColors.AmberWarn
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onConfirmSkill,
                        modifier = Modifier
                            .weight(1f)
                            .height(EngramDimens.SecondaryTouchHeight),
                        colors = ButtonDefaults.buttonColors(containerColor = EngramColors.EmeraldEdge)
                    ) {
                        Text("CONFIRM", color = Color.Black, style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = onDismissSkill,
                        modifier = Modifier
                            .weight(1f)
                            .height(EngramDimens.SecondaryTouchHeight)
                    ) {
                        Text("CANCEL", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // Answer & System-1 telemetry card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(EngramDimens.CardCornerRadius))
                .background(EngramColors.CardSurface)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.executedTierBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = EngramColors.CyanCloud
                )
                if (lastDecision != null) {
                    Text(
                        text = "${lastDecision.routeChoice.selected.displayLabel} P=${"%.2f".format(lastDecision.routeChoice.maxProbability)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EngramColors.EmeraldEdge
                    )
                }
            }

            Text(
                text = uiState.streamingAnswer,
                style = MaterialTheme.typography.bodyLarge,
                color = EngramColors.TextPrimary
            )

            if (uiState.citations.isNotEmpty()) {
                CitationBadgesSection(uiState.citations)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.queryInput,
                onValueChange = onQueryChange,
                placeholder = { Text("Ask EngramEdge...", style = MaterialTheme.typography.bodyMedium) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = { onSubmitQuery(null, false) },
                modifier = Modifier.height(64.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("GO", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun CitationBadgesSection(citations: List<HybridMemoryResult>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "HYBRID RRF CITATIONS (ObjectBox 256d + AppSearch BM25F):",
            style = MaterialTheme.typography.labelSmall,
            color = EngramColors.TextMuted
        )
        for (hit in citations) {
            val cosStr = hit.cosineDistance?.let { "cos=${"%.2f".format(it)}" } ?: "cos=-"
            val lexStr = hit.lexicalRank?.let { "bm25=#$it" } ?: "bm25=-"
            Text(
                text = "• ${hit.chunk.title} [RRF=${"%.3f".format(hit.rrfScore)} | $cosStr | $lexStr]",
                style = MaterialTheme.typography.labelSmall,
                color = EngramColors.EmeraldEdge
            )
        }
    }
}

@Composable
private fun SnapOcrShieldCard(
    uiState: EngramConversationUiState,
    lastShieldVerdict: ShieldVerdict?,
    hasCameraPermission: Boolean,
    isActive: Boolean,
    onRequestCameraPermission: () -> Unit,
    onCaptureBitmap: (Bitmap) -> Unit,
    onRunSyntheticOcr: (Boolean) -> Unit,
    onAskAboutSnapshot: (String) -> Unit,
    onTestRetrieval: (String) -> Unit,
    onToggleTorch: () -> Unit
) {
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (hasCameraPermission) {
            WatchCameraPreviewBox(
                isActive = isActive,
                onPreviewReady = { previewViewRef = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { previewViewRef?.bitmap?.let(onCaptureBitmap) },
                    modifier = Modifier
                        .weight(1f)
                        .height(EngramDimens.PrimaryTouchHeight),
                    shape = RoundedCornerShape(EngramDimens.CardCornerRadius)
                ) {
                    Text("SNAP CAMERA -> OCR", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(
                    onClick = onToggleTorch,
                    modifier = Modifier
                        .width(76.dp)
                        .height(EngramDimens.PrimaryTouchHeight),
                    shape = RoundedCornerShape(EngramDimens.CardCornerRadius),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (uiState.isTorchActive) EngramColors.AmberWarn else Color.Transparent
                    )
                ) {
                    Text(
                        text = if (uiState.isTorchActive) "🔦 ON" else "🔦",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (uiState.isTorchActive) Color.Black else Color.White
                    )
                }
            }
        } else {
            OutlinedButton(
                onClick = onRequestCameraPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(EngramDimens.SecondaryTouchHeight),
                shape = RoundedCornerShape(EngramDimens.CardCornerRadius)
            ) {
                Text("ENABLE WATCH CAMERAX", style = MaterialTheme.typography.titleMedium)
            }
        }

        // Stage-proof synthetic OCR + Prompt-Injection Shield tests
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onRunSyntheticOcr(false) },
                modifier = Modifier
                    .weight(1f)
                    .height(EngramDimens.SecondaryTouchHeight),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EngramColors.EmeraldEdge)
            ) {
                Text("TEST BENIGN SIGN", color = Color.Black, style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = { onRunSyntheticOcr(true) },
                modifier = Modifier
                    .weight(1f)
                    .height(EngramDimens.SecondaryTouchHeight),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EngramColors.CoralDanger)
            ) {
                Text("TEST INJECTION", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }

        // Extracted OCR text display box
        if (!uiState.lastExtractedOcrText.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF002213))
                    .border(1.dp, EngramColors.EmeraldEdge, RoundedCornerShape(18.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📸 EXTRACTED OCR TEXT",
                        style = MaterialTheme.typography.labelMedium,
                        color = EngramColors.EmeraldEdge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ML Kit 19.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = EngramColors.TextMuted
                    )
                }
                Text(
                    text = uiState.lastExtractedOcrText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }

        // OCR status & prompt injection shield verdict
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(EngramColors.CardSurface)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "OCR & PROMPT SHIELD VERDICT",
                style = MaterialTheme.typography.labelSmall,
                color = EngramColors.AmberWarn
            )
            Text(
                text = uiState.lastOcrStatus,
                style = MaterialTheme.typography.bodyLarge,
                color = EngramColors.TextPrimary
            )
            if (lastShieldVerdict != null) {
                Text(
                    text = "Backend: ${lastShieldVerdict.backend} • p_inj=${"%.2f".format(lastShieldVerdict.injectionProbability)} • ${lastShieldVerdict.latencyMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (lastShieldVerdict.isSafe) EngramColors.EmeraldEdge else EngramColors.CoralDanger
                )
            }
        }

        // Vault Storage & Retrieval Demo Card
        if (uiState.lastStoredMemoryId != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(EngramColors.CardSurface)
                    .border(1.dp, Color(0xFF1B3224), RoundedCornerShape(18.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "VAULT STORAGE & RETRIEVAL DEMO",
                    style = MaterialTheme.typography.labelSmall,
                    color = EngramColors.CyanCloud,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Memory #${uiState.lastStoredMemoryId} indexed into ObjectBox (256d HNSW) and Android 15 AppSearch (BM25F).",
                    style = MaterialTheme.typography.bodySmall,
                    color = EngramColors.TextPrimary
                )

                if (uiState.lastRetrievalDemo.isNotEmpty()) {
                    Text(
                        text = "Instant Hybrid RRF Verification:",
                        style = MaterialTheme.typography.labelSmall,
                        color = EngramColors.AmberWarn
                    )
                    for (hit in uiState.lastRetrievalDemo.take(1)) {
                        val cosStr = hit.cosineDistance?.let { "cos=${"%.2f".format(it)}" } ?: "cos=-"
                        val lexStr = hit.lexicalRank?.let { "bm25=#$it" } ?: "bm25=-"
                        Text(
                            text = "• ${hit.chunk.title} [RRF=${"%.3f".format(hit.rrfScore)} | $cosStr | $lexStr]",
                            style = MaterialTheme.typography.bodySmall,
                            color = EngramColors.EmeraldEdge
                        )
                        Text(
                            text = "\"${hit.chunk.contentText.take(80)}...\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = EngramColors.TextMuted
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val title = uiState.lastStoredMemoryTitle ?: "snapshot"
                            onAskAboutSnapshot("What was the text and pin code in my $title?")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(EngramDimens.SecondaryTouchHeight),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EngramColors.EmeraldEdge)
                    ) {
                        Text("ASK IN CHAT ->", color = Color.Black, style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = {
                            uiState.lastStoredMemoryTitle?.let(onTestRetrieval)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(EngramDimens.SecondaryTouchHeight),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("RE-TEST RRF", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchCameraPreviewBox(
    isActive: Boolean,
    onPreviewReady: (PreviewView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    DisposableEffect(isActive, lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)

        if (!isActive) {
            runCatching {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                    AndroidSkillExecutor.activeCameraControl = null
                }
            }
            return@DisposableEffect onDispose { }
        }

        cameraProviderFuture.addListener({
            runCatching {
                val provider = cameraProviderFuture.get()
                val preview = CameraPreviewUseCase.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                provider.unbindAll()
                val selector = if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
                val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview)
                AndroidSkillExecutor.activeCameraControl = camera.cameraControl
                onPreviewReady(previewView)
            }
        }, executor)

        onDispose {
            runCatching {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                    AndroidSkillExecutor.activeCameraControl = null
                }
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

@Composable
private fun GeminiLiveBurstCard(
    liveBurst: LiveBurstStatus,
    onToggleLiveBurst: () -> Unit,
    onSendLiveFrame: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onToggleLiveBurst,
            modifier = Modifier
                .fillMaxWidth()
                .height(EngramDimens.PrimaryTouchHeight),
            shape = RoundedCornerShape(EngramDimens.CardCornerRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (liveBurst.isStreaming) EngramColors.CoralDanger else EngramColors.CyanCloud,
                contentColor = Color.Black
            )
        ) {
            Text(
                text = if (liveBurst.isStreaming) "STOP 1FPS GEMINI LIVE" else "START 1FPS GEMINI LIVE BURST",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (liveBurst.isStreaming) {
            OutlinedButton(
                onClick = onSendLiveFrame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(EngramDimens.SecondaryTouchHeight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "SEND 1FPS WATCH FRAME (#${liveBurst.framesSent + 1})",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(EngramDimens.CardCornerRadius))
                .background(EngramColors.CardSurface)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (liveBurst.cloudConfigured) EngramColors.EmeraldEdge else EngramColors.AmberWarn)
                )
                Text(
                    text = if (liveBurst.cloudConfigured) {
                        "Firebase AI Logic + App Check Active"
                    } else {
                        "Offline Demo Mode (awaiting google-services.json)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = EngramColors.CyanCloud
                )
            }

            Text(
                text = liveBurst.transcriptLog,
                style = MaterialTheme.typography.bodyLarge,
                color = EngramColors.TextPrimary
            )
        }
    }
}

@Composable
private fun EdgeModelGalleryCard(
    memory: DeviceMemoryTelemetry,
    gallery: GalleryRuntimeState,
    hfToken: String,
    onUpdateHfToken: (String) -> Unit,
    onSelectModel: (EdgeModelSpec) -> Unit,
    onDownloadModel: (EdgeModelSpec) -> Unit,
    onCycleResidency: () -> Unit
) {
    var isTokenExpanded by remember { mutableStateOf(hfToken.isBlank()) }
    var isTokenVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 4GB RAM Residency Controller card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(EngramColors.CardSurfaceElevated)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "4GB LPDDR RESIDENCY: ${memory.residencyMode.badge}",
                style = MaterialTheme.typography.titleMedium,
                color = EngramColors.EmeraldEdge
            )
            Text(
                text = "Physical: ${memory.physicalTotalMb}MB • Free: ${memory.memAvailableMb}MB • App PSS: ${memory.appPssMb}MB",
                style = MaterialTheme.typography.labelSmall,
                color = EngramColors.TextMuted
            )
            OutlinedButton(
                onClick = onCycleResidency,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("TOGGLE CO-RESIDENT / AUTO-SWAP", style = MaterialTheme.typography.labelLarge)
            }
        }

        // Collapsible HuggingFace Token Accordion
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EngramColors.CardSurface)
                .border(1.dp, EngramColors.CardSurfaceElevated, RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isTokenExpanded = !isTokenExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = "HF Token",
                        tint = if (hfToken.isNotBlank()) EngramColors.EmeraldEdge else EngramColors.TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "HuggingFace Token",
                        style = MaterialTheme.typography.titleSmall,
                        color = EngramColors.TextPrimary
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (hfToken.isNotBlank()) "CONFIGURED (••••)" else "NOT SET",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hfToken.isNotBlank()) EngramColors.EmeraldEdge else EngramColors.TextMuted
                    )
                    Icon(
                        imageVector = if (isTokenExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isTokenExpanded) "Collapse" else "Expand",
                        tint = EngramColors.TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (isTokenExpanded) {
                OutlinedTextField(
                    value = hfToken,
                    onValueChange = onUpdateHfToken,
                    placeholder = {
                        Text(
                            "hf_... (for gated Gemma models)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                            Icon(
                                imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isTokenVisible) "Hide token" else "Show token",
                                tint = EngramColors.TextMuted
                            )
                        }
                    }
                )
                Text(
                    text = "Hardware-encrypted via AndroidKeyStore AES-256 GCM. Saved automatically.",
                    style = MaterialTheme.typography.labelSmall,
                    color = EngramColors.TextMuted
                )
            }
        }

        if (!gallery.lastError.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF3B1212))
                    .border(1.dp, EngramColors.CoralDanger, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "DOWNLOAD ERROR",
                    style = MaterialTheme.typography.labelSmall,
                    color = EngramColors.CoralDanger
                )
                Text(
                    text = gallery.lastError,
                    style = MaterialTheme.typography.bodySmall,
                    color = EngramColors.TextPrimary
                )
            }
        }

        val statuses = gallery.statuses.ifEmpty {
            ModelCatalog.ALL_MODELS.map { ModelInstallStatus(it, null, false) }
        }

        for (item in statuses) {
            val isSelected = gallery.selectedSpec.id == item.spec.id
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(EngramColors.CardSurface)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) EngramColors.EmeraldEdge else EngramColors.CardSurfaceElevated,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${item.spec.displayName} (${item.spec.approxDiskMb} MB)",
                    style = MaterialTheme.typography.titleMedium,
                    color = EngramColors.TextPrimary
                )
                Text(
                    text = item.spec.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EngramColors.TextMuted
                )

                if (item.spec.role != EdgeModelRole.EMBEDDING) {
                    Text(
                        text = if (item.isLoaded) {
                            "Context: ${gallery.activeMaxTokens} tokens (dynamically allocated)"
                        } else {
                            "Adaptive Context: up to 4096 tokens (auto-scaled)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.isLoaded) EngramColors.EmeraldEdge else EngramColors.CyanCloud
                    )
                }

                if (item.downloadProgress != null) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { item.downloadProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = EngramColors.EmeraldEdge,
                        trackColor = EngramColors.CardSurfaceElevated
                    )
                }

                if (item.installedFile != null) {
                    Button(
                        onClick = { onSelectModel(item.spec) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (item.isLoaded) EngramColors.EmeraldEdge else EngramColors.CardSurfaceElevated,
                            contentColor = if (item.isLoaded) Color(0xFF001F0E) else EngramColors.TextPrimary
                        )
                    ) {
                        Text(
                            text = if (item.isLoaded) {
                                "ACTIVE IN RAM • ${gallery.activeMaxTokens} TOKENS"
                            } else {
                                "ACTIVATE ON WRIST"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    if (item.downloadProgress != null) {
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "DOWNLOADING ${(item.downloadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    } else {
                        Button(
                            onClick = { onDownloadModel(item.spec) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EngramColors.EmeraldEdge,
                                contentColor = Color(0xFF001F0E)
                            )
                        ) {
                            Text(
                                text = "PULL FROM HUGGINGFACE",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calibrated preview matching the user's rectangular >2" Full Android 15 watch (`Smallest Width = 426dp`).
 */
@Preview(
    name = "EngramEdge sw426dp Rectangular Watch",
    device = "spec:width=426dp,height=490dp,dpi=180",
    showBackground = true
)
@Composable
fun EngramEdgeSw426WatchPreview() {
    EngramMicroTheme {
        EngramEdgeDeckContent(
            uiState = EngramConversationUiState(
                executedTierBadge = "LOCAL (Qwen3-0.6B)",
                streamingAnswer = "Parked rental EV (plate 8KZM419) on Level P3, Pillar Green-14 near the North elevator."
            ),
            memory = DeviceMemoryTelemetry(
                physicalTotalMb = 4096L,
                memAvailableMb = 1320L,
                appPssMb = 685L
            ),
            gallery = GalleryRuntimeState(),
            lastDecision = null,
            pendingConfirmation = null,
            lastShieldVerdict = null,
            liveBurst = LiveBurstStatus(),
            speech = SpeechUiState(),
            hasCameraPermission = false,
            onToggleOnline = {},
            onQueryChange = {},
            onSubmitQuery = { _, _ -> },
            onPushToTalk = {},
            onLaunchSystemVoiceDialog = {},
            onConfirmSkill = {},
            onDismissSkill = {},
            onRequestCameraPermission = {},
            onCaptureBitmap = {},
            onRunSyntheticOcr = {},
            onToggleLiveBurst = {},
            onSendLiveFrame = {},
            onSelectModel = {},
            onDownloadModel = {},
            onUpdateHfToken = {},
            onCycleResidency = {},
            onQueryVaultDirectly = {},
            onToggleTorch = {}
        )
    }
}
