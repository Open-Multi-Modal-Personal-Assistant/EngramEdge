package dev.engram.edge

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.snapshotFlow
import dev.engram.edge.agent.EngramKoogOrchestrator
import dev.engram.edge.audio.EngramSpeechController
import dev.engram.edge.cloud.FirebaseAiCloudBridge
import dev.engram.edge.data.memory.AppSearchLexicalIndex
import dev.engram.edge.data.memory.HybridMemoryRepository
import dev.engram.edge.data.memory.MyObjectBox
import dev.engram.edge.edge.AdaptiveResidencyMonitor
import dev.engram.edge.edge.EmbeddingGemmaEngine
import dev.engram.edge.edge.LiteRtModelGalleryManager
import dev.engram.edge.router.SystemOneDecisionEngine
import dev.engram.edge.security.PromptInjectionShield
import dev.engram.edge.skills.AndroidSkillExecutor
import dev.engram.edge.skills.BetterHealthTrackerBridge
import dev.engram.edge.state.EngramAppStateCoordinator
import dev.engram.edge.vision.WatchCameraOcrManager
import io.objectbox.BoxStore
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EngramEdgeContainer(context: Context, val boxStore: BoxStore) {
    private val appContext = context.applicationContext

    val appState = EngramAppStateCoordinator()
    val residencyMonitor = AdaptiveResidencyMonitor()
    val promptShield = PromptInjectionShield(appContext)
    val embeddingEngine = EmbeddingGemmaEngine(appContext, residencyMonitor)
    val galleryManager = LiteRtModelGalleryManager(
        context = appContext,
        residencyMonitor = residencyMonitor,
        embeddingEngine = embeddingEngine,
        appState = appState
    )
    val appSearchIndex = AppSearchLexicalIndex(appContext)
    val betterHealthBridge = BetterHealthTrackerBridge(
        context = appContext,
        appState = appState
    )

    val memoryRepository = HybridMemoryRepository(
        boxStore = boxStore,
        appSearchIndex = appSearchIndex,
        embeddingEngine = embeddingEngine,
        promptShield = promptShield,
        appState = appState
    )

    val systemOneRouter = SystemOneDecisionEngine(
        embeddingEngine = embeddingEngine,
        promptShield = promptShield
    )

    val skillExecutor = AndroidSkillExecutor(
        context = appContext,
        residencyMonitor = residencyMonitor,
        promptShield = promptShield,
        betterHealthBridge = betterHealthBridge,
        memoryRepositoryProvider = { memoryRepository },
        appState = appState
    )

    val cloudBridge = FirebaseAiCloudBridge(appContext)

    val koogOrchestrator = EngramKoogOrchestrator(
        memoryRepository = memoryRepository,
        systemOneRouter = systemOneRouter,
        galleryManager = galleryManager,
        cloudBridge = cloudBridge,
        skillExecutor = skillExecutor,
        promptShield = promptShield
    )

    val speechController = EngramSpeechController(appContext)
    val cameraOcrManager = WatchCameraOcrManager()
}

class EngramEdgeApplication : Application() {

    lateinit var container: EngramEdgeContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val boxStore = MyObjectBox.builder()
            .androidContext(this)
            .build()

        if (BuildConfig.DEBUG) {
            runCatching {
                val adminClass = Class.forName("io.objectbox.android.Admin")
                val adminInstance = adminClass.getConstructor(BoxStore::class.java).newInstance(boxStore)
                val started = adminClass.getMethod("start", Context::class.java).invoke(adminInstance, this)
                Log.i("EngramEdge", "ObjectBox Admin browser started=$started")
            }
        }

        container = EngramEdgeContainer(this, boxStore)
        registerComponentCallbacks(container.residencyMonitor)
        container.residencyMonitor.startMonitoring()
        container.galleryManager.refreshCatalogStatuses()

        // Continuously synchronize Jetpack AppState (`PersistToDataStore` keys) with JSON DataStore
        val appStateFile = File(filesDir, "datastore/engram_appstate.json")
        appScope.launch {
            container.appState.syncWithDataStoreFile(
                storageFile = appStateFile,
                scope = appScope
            )
        }
        appScope.launch {
            snapshotFlow { container.appState.preferences.value }
                .collect { prefs ->
                    container.galleryManager.syncFromPersistedPreferences(
                        preferGpuVision = prefs.preferGpuVision,
                        preferredModelId = prefs.preferredModelId
                    )
                }
        }

        appScope.launch {
            container.skillExecutor.stageAndDiscoverSkills()
            container.embeddingEngine.tryLoadBestAvailableModel()
            container.galleryManager.refreshCatalogStatuses()
            container.memoryRepository.initializeAndSeedDemoVault()
            container.memoryRepository.reindexAllWithActiveEmbeddingModel()
            container.cloudBridge.initializeIfConfigured()
        }
    }
}

