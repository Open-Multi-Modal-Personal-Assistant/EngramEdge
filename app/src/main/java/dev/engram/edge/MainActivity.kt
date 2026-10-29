package dev.engram.edge

import dev.engram.edge.ui.EngramMicroTheme
import dev.engram.edge.ui.EngramEdgeRootScreen
import dev.engram.edge.ui.EngramEdgeViewModel
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import android.graphics.Color as AndroidColor

class MainActivity : ComponentActivity() {

    private val viewModel: EngramEdgeViewModel by viewModels()

    private val healthPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Results picked up dynamically by BetterHealthTrackerBridge on next query */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.BLACK),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.BLACK)
        )
        super.onCreate(savedInstanceState)

        // Request BODY_SENSORS / ACTIVITY_RECOGNITION and TraX V2 com.ailife.betterhealth.READ_HEALTH_DATA if dangerous
        val missingHealthPermissions = (application as EngramEdgeApplication)
            .container
            .betterHealthBridge
            .getRequestableRuntimePermissions()
        if (missingHealthPermissions.isNotEmpty()) {
            runCatching {
                healthPermissionLauncher.launch(missingHealthPermissions.toTypedArray())
            }
        }

        setContent {
            EngramMicroTheme {
                EngramEdgeRootScreen(viewModel = viewModel)
            }
        }
    }
}
