package dev.engram.edge.state

import androidx.appstate.transform.TransformScope
import androidx.appstate.transform.transform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import dev.engram.edge.edge.DeviceMemoryTelemetry
import dev.engram.edge.edge.GalleryRuntimeState
import dev.engram.edge.router.SystemOneDecision
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow

/**
 * Unified, pre-computed UI projection for the always-on wrist telemetry HUD pill, derived
 * headlessly outside the UI tree via Jetpack AppState Transform (`androidx.appstate.transform`).
 */
data class HudBannerProjection(
    val primaryTelemetryLine: String = "CO-RES • 0MB PSS • Qwen3-0.6B",
    val connectivityBadge: String = "HYBRID",
    val isOnlineMode: Boolean = true,
    val activeModelLabel: String = "Qwen3-0.6B",
    val externalAgentBadge: String? = null
)

/**
 * Pure `@Composable` projection logic shared between production `transform(...)` and
 * deterministic `transformTest` unit verification.
 */
@Composable
fun computeHudBannerProjection(
    coordinator: EngramAppStateCoordinator,
    memory: DeviceMemoryTelemetry,
    gallery: GalleryRuntimeState,
    lastDecision: SystemOneDecision?
): HudBannerProjection {
    val prefs = coordinator.preferences.value
    val runtime = coordinator.runtimeStatus.value

    val activeModelLabel = if (gallery.loadedSpec != null) {
        val tokenK = if (gallery.activeMaxTokens >= 1024) {
            "${gallery.activeMaxTokens / 1024}k"
        } else {
            "${gallery.activeMaxTokens}t"
        }
        "${gallery.loadedSpec.shortBadge} [$tokenK]"
    } else {
        gallery.selectedSpec.shortBadge
    }

    val sys1Part = lastDecision?.latencyMs?.let { " • S1 ${it}ms" }.orEmpty()
    val appFnPart = if (runtime.appFunctionCallCount > 0 && !runtime.lastAppFunctionName.isNullOrBlank()) {
        " • AF#${runtime.appFunctionCallCount}"
    } else {
        ""
    }

    val primaryLine =
        "${memory.residencyMode.badge} • ${memory.appPssMb}MB PSS • $activeModelLabel$sys1Part$appFnPart"

    val externalBadge = if (runtime.appFunctionCallCount > 0 && !runtime.lastAppFunctionName.isNullOrBlank()) {
        "AppFn: ${runtime.lastAppFunctionName} (${runtime.lastAppFunctionSummary.orEmpty()})"
    } else {
        null
    }

    return HudBannerProjection(
        primaryTelemetryLine = primaryLine,
        connectivityBadge = if (prefs.isOnlineMode) "HYBRID" else "AIRPLANE",
        isOnlineMode = prefs.isOnlineMode,
        activeModelLabel = activeModelLabel,
        externalAgentBadge = externalBadge
    )
}

/**
 * Launches a headless `@Composable` state transformation using `androidx.appstate.transform`
 * that fuses `StateStore` Compose snapshot states with live hardware telemetry `StateFlow`s.
 */
fun createHeadlessHudProjection(
    coordinator: EngramAppStateCoordinator,
    memoryFlow: StateFlow<DeviceMemoryTelemetry>,
    galleryFlow: StateFlow<GalleryRuntimeState>,
    decisionFlow: StateFlow<SystemOneDecision?>,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
): State<HudBannerProjection> = transform(
    initialValue = HudBannerProjection(),
    scope = scope,
    dispatcher = dispatcher
) {
    val memory = memoryFlow.collectAsState().value
    val gallery = galleryFlow.collectAsState().value
    val decision = decisionFlow.collectAsState().value
    computeHudBannerProjection(
        coordinator = coordinator,
        memory = memory,
        gallery = gallery,
        lastDecision = decision
    )
}

/**
 * Attaches the HUD projection directly onto a [TransformScope] for deterministic `transformTest`
 * verification without needing Android UI harnesses.
 */
fun TransformScope.launchHudProjectionForTest(
    coordinator: EngramAppStateCoordinator,
    memoryState: State<DeviceMemoryTelemetry>,
    galleryState: State<GalleryRuntimeState>,
    decisionState: State<SystemOneDecision?>
): State<HudBannerProjection> = launchTransform(HudBannerProjection()) {
    computeHudBannerProjection(
        coordinator = coordinator,
        memory = memoryState.value,
        gallery = galleryState.value,
        lastDecision = decisionState.value
    )
}
