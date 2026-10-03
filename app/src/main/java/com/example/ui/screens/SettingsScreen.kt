package com.example.ui.screens

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppThemeMode
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassDialog
import com.example.ui.components.GlassSection
import com.example.ui.components.bounceClick
import com.example.ui.theme.WarXTheme
import com.example.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = WarXTheme.colors

    val currentTheme by viewModel.themeMode.collectAsStateWithLifecycle()
    val selectedPreset by viewModel.selectedPreset.collectAsStateWithLifecycle()

    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
    val freeSpaceGb = (storageDir.freeSpace / (1024.0 * 1024.0 * 1024.0))

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalPad = when {
            maxWidth > 840.dp -> 32.dp
            maxWidth > 600.dp -> 24.dp
            else -> 16.dp
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPad),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Page Header
        item {
            Column {
                Text(
                    text = "Pengaturan & Diagnostik",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Konfigurasi tema, engine ekstraksi, identitas browser, dan penyimpanan",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        // 1. Appearance & Theme Section
        item {
            GlassSection(
                title = "Tampilan & Tema",
                subtitle = "Pilih tema aplikasi yang tersimpan secara permanen"
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeSelectorOption(
                                title = "Sistem",
                                icon = Icons.Default.PhoneAndroid,
                                isSelected = currentTheme == AppThemeMode.SYSTEM,
                                onClick = { viewModel.setTheme(AppThemeMode.SYSTEM) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeSelectorOption(
                                title = "Terang",
                                icon = Icons.Default.LightMode,
                                isSelected = currentTheme == AppThemeMode.LIGHT,
                                onClick = { viewModel.setTheme(AppThemeMode.LIGHT) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeSelectorOption(
                                title = "Gelap",
                                icon = Icons.Default.DarkMode,
                                isSelected = currentTheme == AppThemeMode.DARK,
                                onClick = { viewModel.setTheme(AppThemeMode.DARK) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 2. Storage & Downloads Section
        item {
            GlassSection(
                title = "Penyimpanan & Unduhan",
                subtitle = "Lokasi target file video tersimpan di perangkat"
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = colors.accentCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Direktori Video",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = storageDir.absolutePath,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassChip(
                                text = "Tersedia: ${String.format("%.2f GB", freeSpaceGb)}",
                                color = colors.statusSuccess
                            )
                            GlassChip(
                                text = "Native Direct I/O",
                                color = colors.accentCyan
                            )
                        }
                    }
                }
            }
        }

        // 3. Browser User-Agent Bypass Section
        item {
            GlassSection(
                title = "Identitas Browser (User-Agent)",
                subtitle = "Gunakan profil browser berbeda untuk memintas proteksi web"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    viewModel.presets.forEach { preset ->
                        val isSelected = selectedPreset.name == preset.name
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectPreset(preset) },
                            borderColor = if (isSelected) colors.accentCyan else colors.glassBorderSubtle
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.selectPreset(preset) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = colors.accentCyan,
                                        unselectedColor = colors.textMuted
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) colors.accentCyan else colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = preset.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Extraction Engine Capabilities
        item {
            GlassSection(
                title = "Kemampuan Engine Ekstraksi",
                subtitle = "Spesifikasi arsitektur internal WarX"
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val features = listOf(
                            "Ekstraksi HLS Master Playlist & Variant otomatis",
                            "Penggabungan segmen TS native tanpa transcoding (zero CPU overhead)",
                            "Dukungan enkripsi AES-128 HLS decrypter otomatis",
                            "Pause / Resume download untuk direct MP4 dan HLS stream",
                            "Deep scanning DOM, JSON-LD, JWPlayer, VideoJS, Hls.js, unpacked eval scripts",
                            "Integrasi Media3 ExoPlayer internal untuk preview & playback",
                            "Background download notification dengan Foreground Service"
                        )

                        for (f in features) {
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colors.accentCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = f,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Diagnostics & Clean-up
        item {
            GlassSection(
                title = "Diagnostik & Riwayat",
                subtitle = "Pengelolaan basis data unduhan"
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bersihkan Riwayat Unduhan",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Hapus seluruh riwayat entri unduhan dari Room Database",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        GlassButton(
                            text = "Bersihkan",
                            isSecondary = true,
                            onClick = { showClearHistoryDialog = true }
                        )
                    }
                }
            }
        }

        // 6. About App
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "WarX Downloader Native v1.0",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Android Native • Jetpack Compose • Material 3 • HLS Parser & Downloader",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
            }
        }
    }

        if (showClearHistoryDialog) {
            GlassDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                title = "Hapus Riwayat Unduhan?",
                confirmButton = {
                    GlassButton(
                        text = "Hapus Riwayat",
                        onClick = {
                            viewModel.clearAllHistory()
                            showClearHistoryDialog = false
                        }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showClearHistoryDialog = false }) {
                        Text("Batal", color = colors.textMuted)
                    }
                }
            ) {
                Text(
                    text = "Tindakan ini akan mengosongkan seluruh riwayat unduhan pada database lokal. File fisik video yang sudah terunduh tidak akan dihapus.",
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ThemeSelectorOption(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WarXTheme.colors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) colors.glassSurface else Color.Transparent)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) colors.accentCyan else colors.glassBorderSubtle,
                shape = RoundedCornerShape(12.dp)
            )
            .bounceClick(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) colors.accentCyan else colors.textMuted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                color = if (isSelected) colors.accentCyan else colors.textPrimary,
                fontSize = 12.sp
            )
        }
    }
}
