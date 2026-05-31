package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.NoiseSentryViewModel

class MainActivity : FragmentActivity() {
    private val viewModel: NoiseSentryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SentryApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentryApp(viewModel: NoiseSentryViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    
    val isBiometricLocked by viewModel.isBiometricLocked.collectAsState()
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()

    if (currentUser == null) {
        WelcomeScreen(viewModel = viewModel, onLoginSuccess = {
            viewModel.setScreen("dashboard")
            if (viewModel.biometricEnabled.value) {
                viewModel.setBiometricLocked(true)
            } else {
                viewModel.setBiometricLocked(false)
            }
        })
    } else if (biometricEnabled && isBiometricLocked) {
        BiometricLockScreen(viewModel = viewModel)
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "MONITOR ACTIVE GRID",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = NeonCyan,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "Noise Sentry",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )
                        }
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberBlack)
                                .border(1.dp, CyberGray, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = if (isRecording) NeonRed else CyberGreenSafe,
                                modifier = Modifier.size(8.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentUser?.fullName?.take(11) ?: "",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.testTag("logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Log Out",
                                tint = NeonRed
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = CyberDarkCard,
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = CyberDarkCard,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.testTag("sentry_bottom_nav_bar")
                ) {
                    val tabs = listOf(
                        NavigationBarItemInfo("dashboard", "HQ", Icons.Default.Terminal, "Home Dashboard"),
                        NavigationBarItemInfo("meter", "Radar", Icons.Default.Mic, "Real-time Tracker"),
                        NavigationBarItemInfo("map", "Map", Icons.Default.Map, "Sentry Coverage Map"),
                        NavigationBarItemInfo("reports", "Ledger", Icons.Default.Description, "Log reports"),
                        NavigationBarItemInfo("gamify", "Trophy", Icons.Default.Stars, "Gamification Board"),
                        NavigationBarItemInfo("export", "Export", Icons.Default.Output, "Authorities Data Export")
                    )

                    tabs.forEach { tab ->
                        val isSelected = currentScreen == tab.id
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.setScreen(tab.id) },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.desc,
                                    tint = if (isSelected) NeonCyan else CyberLightGray,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonCyan else CyberLightGray
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = NeonCyan.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_${tab.id}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    "dashboard" -> DashboardScreen(viewModel = viewModel)
                    "meter" -> RealTimeMeterScreen(viewModel = viewModel)
                    "map" -> SentryMapScreen(viewModel = viewModel)
                    "reports" -> ReportingScreen(viewModel = viewModel)
                    "gamify" -> GamificationScreen(viewModel = viewModel)
                    "export" -> ExportScreen(viewModel = viewModel)
                    else -> DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}

data class NavigationBarItemInfo(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val desc: String
)
