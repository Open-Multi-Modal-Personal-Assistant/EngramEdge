package dev.engram.edge.state

import androidx.appstate.datastore.PersistToDataStore
import androidx.appstate.datastore.syncToDataStore
import androidx.appstate.statestore.StateStore
import androidx.appstate.statestore.StateStoreKey
import androidx.compose.runtime.State
import dev.engram.edge.edge.ModelCatalog
import dev.engram.edge.skills.FawHealthSnapshot
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable

/**
 * User & Edge runtime preferences persisted across process death via Jetpack AppState
 * (`androidx.appstate.statestore:statestore-datastore`).
 *
 * Security boundary: Sensitive credentials (e.g., HuggingFace Bearer token) remain strictly inside
 * Android Keystore (`EncryptedTokenStore`) and are never stored here.
 */
@Serializable
data class EdgePreferencesState(
    val isOnlineMode: Boolean = true,
    val autoSpeakReplies: Boolean = true,
    val preferGpuVision: Boolean = false,
    val preferredModelId: String = ModelCatalog.QWEN3_0_6B_INT4.id
)

/**
 * Persisted bookmark of the most recently stored personal memory or OCR snapshot, synchronized
 * between foreground UI (`MainActivity`) and headless OS `AppFunctions` (`saveMemoryNote`).
 */
@Serializable
data class VaultBookmarkState(
    val lastStoredMemoryId: Long? = null,
    val lastStoredMemoryTitle: String? = null,
    val totalIndexedMemories: Int = 0
)

/**
 * Persisted wrist vitals snapshot shared between UI hardware skills and `readWristVitals`
 * Android 16+ `AppFunctionService` invocations.
 */
@Serializable
data class CachedVitalsState(
    val providerSource: String = "",
    val heartRateBpm: Int? = null,
    val spo2Percent: Int? = null,
    val stepsToday: Int? = null,
    val caloriesKcal: Int? = null,
    val summary: String = "",
    val recordedAtMillis: Long = 0L
)

/**
 * Ephemeral (in-memory) cross-component state tracking external OS agent calls
 * (`BaseEngramAppFunctionService`) and hardware torch state within the app process.
 */
@Serializable
data class ProcessSharedRuntimeState(
    val isTorchActive: Boolean = false,
    val lastAppFunctionName: String? = null,
    val lastAppFunctionSummary: String? = null,
    val lastAppFunctionTimestampMillis: Long = 0L,
    val appFunctionCallCount: Int = 0
)

@Serializable
private object EdgePreferencesKey : StateStoreKey<EdgePreferencesState>(
    defaultValue = EdgePreferencesState()
), PersistToDataStore

@Serializable
private object VaultBookmarkKey : StateStoreKey<VaultBookmarkState>(
    defaultValue = VaultBookmarkState()
), PersistToDataStore

@Serializable
private object CachedVitalsKey : StateStoreKey<CachedVitalsState>(
    defaultValue = CachedVitalsState()
), PersistToDataStore

@Serializable
private object ProcessSharedRuntimeKey : StateStoreKey<ProcessSharedRuntimeState>(
    defaultValue = ProcessSharedRuntimeState()
)

/**
 * Application-scoped Jetpack AppState (`androidx.appstate`) coordinator encapsulating strongly
 * typed `StateStoreKey`s for cross-component Compose `State<T>` synchronization (`MainActivity`,
 * `BaseEngramAppFunctionService`, `BetterHealthTrackerBridge`, `LiteRtModelGalleryManager`) and
 * automatic JSON DataStore persistence across process recreation.
 */
class EngramAppStateCoordinator(
    val stateStore: StateStore = StateStore()
) {
    // Pre-register all keys on initialization so syncToDataStore immediately restores persisted keys
    val preferences: State<EdgePreferencesState> = stateStore.getState(EdgePreferencesKey)
    val vaultBookmark: State<VaultBookmarkState> = stateStore.getState(VaultBookmarkKey)
    val cachedVitals: State<CachedVitalsState> = stateStore.getState(CachedVitalsKey)
    val runtimeStatus: State<ProcessSharedRuntimeState> = stateStore.getState(ProcessSharedRuntimeKey)

    /**
     * Binds [stateStore] to a JSON-backed Jetpack DataStore file. Suspends indefinitely while
     * restoring saved `PersistToDataStore` keys and persisting subsequent snapshot updates.
     */
    suspend fun syncWithDataStoreFile(storageFile: File, scope: CoroutineScope) {
        storageFile.parentFile?.mkdirs()
        stateStore.syncToDataStore(
            path = storageFile.absolutePath,
            coroutineScope = scope
        )
    }

    fun setOnlineMode(enabled: Boolean) {
        stateStore.updateState(EdgePreferencesKey) { it.copy(isOnlineMode = enabled) }
    }

    fun toggleOnlineMode(): Boolean {
        var next = true
        stateStore.updateState(EdgePreferencesKey) { current ->
            next = !current.isOnlineMode
            current.copy(isOnlineMode = next)
        }
        return next
    }

    fun toggleAutoSpeak(): Boolean {
        var next = true
        stateStore.updateState(EdgePreferencesKey) { current ->
            next = !current.autoSpeakReplies
            current.copy(autoSpeakReplies = next)
        }
        return next
    }

    fun setPreferGpuVision(enabled: Boolean) {
        stateStore.updateState(EdgePreferencesKey) { it.copy(preferGpuVision = enabled) }
    }

    fun setPreferredModelId(modelId: String) {
        stateStore.updateState(EdgePreferencesKey) { it.copy(preferredModelId = modelId) }
    }

    fun recordVaultBookmark(
        memoryId: Long?,
        memoryTitle: String?,
        totalCount: Int? = null
    ) {
        stateStore.updateState(VaultBookmarkKey) { current ->
            current.copy(
                lastStoredMemoryId = memoryId ?: current.lastStoredMemoryId,
                lastStoredMemoryTitle = memoryTitle ?: current.lastStoredMemoryTitle,
                totalIndexedMemories = totalCount ?: current.totalIndexedMemories
            )
        }
    }

    fun updateVaultTotalCount(totalCount: Int) {
        stateStore.updateState(VaultBookmarkKey) { it.copy(totalIndexedMemories = totalCount) }
    }

    fun recordVitalsSnapshot(snapshot: FawHealthSnapshot, nowMillis: Long = System.currentTimeMillis()) {
        stateStore.setState(
            CachedVitalsKey,
            CachedVitalsState(
                providerSource = snapshot.providerSource,
                heartRateBpm = snapshot.heartRateBpm,
                spo2Percent = snapshot.spo2Percent,
                stepsToday = snapshot.stepsToday,
                caloriesKcal = snapshot.caloriesKcal,
                summary = snapshot.formatConciseSummary(),
                recordedAtMillis = nowMillis
            )
        )
    }

    fun setTorchActive(active: Boolean) {
        stateStore.updateState(ProcessSharedRuntimeKey) { it.copy(isTorchActive = active) }
    }

    fun recordExternalAppFunctionCall(
        functionName: String,
        summary: String,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        stateStore.updateState(ProcessSharedRuntimeKey) { current ->
            current.copy(
                lastAppFunctionName = functionName,
                lastAppFunctionSummary = summary,
                lastAppFunctionTimestampMillis = nowMillis,
                appFunctionCallCount = current.appFunctionCallCount + 1
            )
        }
    }
}
