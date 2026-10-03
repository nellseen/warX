package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.GlassBottomBar
import com.example.ui.components.GlassScaffold
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.WarXTheme
import com.example.ui.viewmodel.DownloadsViewModel
import com.example.ui.viewmodel.ExtractionViewModel
import com.example.ui.viewmodel.SettingsViewModel

enum class MainNavTab(val title: String, val icon: ImageVector) {
    HOME("Ekstrak", Icons.Default.Download),
    DOWNLOADS("Unduhan", Icons.Default.Folder),
    SETTINGS("Pengaturan", Icons.Default.Settings)
}

@Composable
fun MainApp(
    sharedUrl: String? = null,
    extractionViewModel: ExtractionViewModel = viewModel(),
    downloadsViewModel: DownloadsViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(MainNavTab.HOME) }
    val colors = WarXTheme.colors

    // Realtime active downloads for badge
    val activeDownloads by downloadsViewModel.activeDownloads.collectAsStateWithLifecycle()

    // Request POST_NOTIFICATIONS on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Handle shared URL from external apps
    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            currentTab = MainNavTab.HOME
            extractionViewModel.onUrlChanged(sharedUrl)
            extractionViewModel.startExtraction()
        }
    }

    val navItems = listOf(
        Triple(MainNavTab.HOME.title, MainNavTab.HOME.icon, 0),
        Triple(MainNavTab.DOWNLOADS.title, MainNavTab.DOWNLOADS.icon, activeDownloads.size),
        Triple(MainNavTab.SETTINGS.title, MainNavTab.SETTINGS.icon, 0)
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundBrush)
    ) {
        val isLandscapeTablet = maxWidth > 800.dp && maxWidth > maxHeight

        if (isLandscapeTablet) {
            // Adaptive Side Navigation Rail for Large Landscape / Tablets
            Row(modifier = Modifier.fillMaxSize()) {
                // Side Navigation Rail
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(100.dp)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.glassCard)
                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MainNavTab.entries.forEachIndexed { index, tab ->
                            val isSelected = currentTab == tab
                            val badge = if (tab == MainNavTab.DOWNLOADS) activeDownloads.size else 0

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) colors.glassSurface else Color.Transparent)
                                    .clickable { currentTab = tab }
                                    .padding(vertical = 12.dp, horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (badge > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = colors.accentCyan,
                                                    contentColor = Color.Black
                                                ) {
                                                    Text(badge.toString(), fontSize = 10.sp)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.title,
                                                tint = if (isSelected) colors.accentCyan else colors.textMuted,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title,
                                            tint = if (isSelected) colors.accentCyan else colors.textMuted,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = tab.title,
                                        color = if (isSelected) colors.accentCyan else colors.textMuted,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // Main Content Container centered
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "tab_nav_rail"
                        ) { tab ->
                            when (tab) {
                                MainNavTab.HOME -> HomeScreen(
                                    viewModel = extractionViewModel,
                                    onNavigateToDownloads = { currentTab = MainNavTab.DOWNLOADS }
                                )
                                MainNavTab.DOWNLOADS -> DownloadsScreen(
                                    viewModel = downloadsViewModel
                                )
                                MainNavTab.SETTINGS -> SettingsScreen(
                                    viewModel = settingsViewModel
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Standard / Compact / Phone Layout with Floating Glass Bottom Bar
            GlassScaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    GlassBottomBar(
                        items = navItems,
                        selectedIndex = currentTab.ordinal,
                        onSelectIndex = { index ->
                            currentTab = MainNavTab.entries[index]
                        }
                    )
                }
            ) { _ ->
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_nav_bottom"
                ) { tab ->
                    when (tab) {
                        MainNavTab.HOME -> HomeScreen(
                            viewModel = extractionViewModel,
                            onNavigateToDownloads = { currentTab = MainNavTab.DOWNLOADS }
                        )
                        MainNavTab.DOWNLOADS -> DownloadsScreen(
                            viewModel = downloadsViewModel
                        )
                        MainNavTab.SETTINGS -> SettingsScreen(
                            viewModel = settingsViewModel
                        )
                    }
                }
            }
        }
    }
}
