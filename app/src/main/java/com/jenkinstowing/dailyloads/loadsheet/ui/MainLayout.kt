package com.jenkinstowing.dailyloads.loadsheet.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

enum class AppTab(val title: String) {
    DASHBOARD("Dashboard"),
    VERIFICATION("Verify"),
    LOGS("History Logs"),
    METRICS("Admin Metrics"),
    SCRIPT_SETUP("Script Setup")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
    viewModel: MainViewModel,
    onProcessingComplete: (List<com.jenkinstowing.dailyloads.loadsheet.data.ExtractedRecord>) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    val currentBatch by viewModel.currentBatch.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                NavigationBarItem(
                    selected = activeTab == AppTab.DASHBOARD,
                    onClick = { activeTab = AppTab.DASHBOARD },
                    label = { Text("Dashboard") },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = activeTab == AppTab.VERIFICATION,
                    onClick = { activeTab = AppTab.VERIFICATION },
                    label = { Text("Verify (${currentBatch.size})") },
                    icon = { 
                        BadgedBox(
                            badge = {
                                if (currentBatch.isNotEmpty()) {
                                    Badge { Text("${currentBatch.size}") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.FactCheck, contentDescription = "Verification")
                        }
                    },
                    modifier = Modifier.testTag("nav_verification")
                )
                NavigationBarItem(
                    selected = activeTab == AppTab.LOGS,
                    onClick = { activeTab = AppTab.LOGS },
                    label = { Text("Logs") },
                    icon = { Icon(Icons.Default.History, contentDescription = "Logs") },
                    modifier = Modifier.testTag("nav_logs")
                )
                NavigationBarItem(
                    selected = activeTab == AppTab.METRICS,
                    onClick = { activeTab = AppTab.METRICS },
                    label = { Text("Metrics") },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Metrics") },
                    modifier = Modifier.testTag("nav_metrics")
                )
                NavigationBarItem(
                    selected = activeTab == AppTab.SCRIPT_SETUP,
                    onClick = { activeTab = AppTab.SCRIPT_SETUP },
                    label = { Text("Script") },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Script Setup") },
                    modifier = Modifier.testTag("nav_setup")
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Crossfade(
            targetState = activeTab,
            modifier = Modifier.padding(innerPadding),
            label = "ScreenTransition"
        ) { tab ->
            when (tab) {
                AppTab.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToVerification = { activeTab = AppTab.VERIFICATION }
                )
                AppTab.VERIFICATION -> VerificationScreen(
                    viewModel = viewModel,
                    onNavigateToDashboard = { activeTab = AppTab.DASHBOARD },
                    onProcessingComplete = onProcessingComplete
                )
                AppTab.LOGS -> HistoryScreen(viewModel = viewModel)
                AppTab.METRICS -> MetricsScreen(viewModel = viewModel)
                AppTab.SCRIPT_SETUP -> IntegrationGuide()
            }
        }
    }
}
