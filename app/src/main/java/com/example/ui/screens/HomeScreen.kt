package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.model.MediaSource
import com.example.model.VideoMetadata
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowingGradientButton
import com.example.ui.components.NeonBadge
import com.example.ui.components.SkeletonShimmer
import com.example.ui.components.VideoPlayerModal
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.WarXCyan
import com.example.ui.theme.WarXPurple
import com.example.ui.theme.WarXViolet
import com.example.ui.viewmodel.ExtractionUiState
import com.example.ui.viewmodel.ExtractionViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: ExtractionViewModel,
    onNavigateToDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedSource by viewModel.selectedSource.collectAsStateWithLifecycle()
    val previewVideo by viewModel.previewVideoUrl.collectAsStateWithLifecycle()

    var showDownloadDialog by remember { mutableStateOf(false) }
    var customFileName by remember { mutableStateOf("") }
    var activeMetadataForDownload by remember { mutableStateOf<VideoMetadata?>(null) }

    // Preset test stream URLs
    val testPresets = listOf(
        Pair("Big Buck Bunny HLS", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
        Pair("Tears of Steel HLS", "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"),
        Pair("Sample Direct MP4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4")
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(WarXCyan, WarXViolet))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "WarX",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = WarXCyan
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Downloader",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ekstraksi mendalam video web & downloader HLS master/variant native tanpa transcoding.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // URL Input Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (urlInput.isNotBlank()) GlassBorder else null
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "URL Halaman Video / Stream",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { viewModel.onUrlChanged(it) },
                        placeholder = {
                            Text(
                                "Tempelkan tautan web (HTML/HLS/MP4)...",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 13.sp
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (urlInput.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.clearUrl() }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Hapus",
                                            tint = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val text = clip.getItemAt(0).text?.toString().orEmpty()
                                        if (text.isNotBlank()) {
                                            viewModel.onUrlChanged(text)
                                        }
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Tempel",
                                        tint = WarXCyan
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                viewModel.startExtraction()
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarXCyan,
                            unfocusedBorderColor = GlassBorderSubtle,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = WarXCyan
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    GlowingGradientButton(
                        text = "Analisis & Ekstrak Video",
                        onClick = {
                            keyboardController?.hide()
                            viewModel.startExtraction()
                        },
                        icon = Icons.Default.Search,
                        enabled = uiState !is ExtractionUiState.Extracting && urlInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Quick Stream Chips
                    Text(
                        text = "Uji Coba Cepat (Sample Streams):",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for ((name, streamUrl) in testPresets) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(WarXViolet.copy(alpha = 0.15f))
                                    .border(1.dp, WarXViolet.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.onUrlChanged(streamUrl)
                                        viewModel.startExtraction()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = name,
                                    color = WarXCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Extraction State Loading / Skeleton
        when (val state = uiState) {
            is ExtractionUiState.Extracting -> {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = WarXCyan.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = WarXCyan,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = state.stage.message,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = WarXCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = state.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            SkeletonShimmer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            )
                        }
                    }
                }
            }

            is ExtractionUiState.Error -> {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = StatusError.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = StatusError,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Gagal Mengekstrak Video",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = StatusError,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            is ExtractionUiState.Success -> {
                val metadata = state.metadata

                // Metadata Card
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = GlassBorder
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Thumbnail Preview with Play Overlay
                            if (!metadata.thumbnailUrl.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.Black.copy(alpha = 0.6f))
                                ) {
                                    AsyncImage(
                                        model = metadata.thumbnailUrl,
                                        contentDescription = "Thumbnail",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Play Preview Button
                                    val firstPlayableUrl = selectedSource?.url ?: metadata.sources.firstOrNull()?.url
                                    if (firstPlayableUrl != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .align(Alignment.Center)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.65f))
                                                .border(2.dp, WarXCyan, CircleShape)
                                                .clickable {
                                                    viewModel.openPreview(firstPlayableUrl, metadata.title)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Putar",
                                                tint = WarXCyan,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Title & Domain
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NeonBadge(
                                    text = metadata.domain,
                                    color = WarXCyan
                                )
                                if (metadata.durationFormatted != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    NeonBadge(
                                        text = metadata.durationFormatted,
                                        color = WarXViolet
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = metadata.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!metadata.author.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Uploader: ${metadata.author}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }

                            if (!metadata.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = metadata.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Sources & Quality Selection Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pilih Kualitas / Variant Stream (${metadata.sources.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                // List of Discovered Streams
                items(metadata.sources) { source ->
                    val isSelected = selectedSource?.id == source.id
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectSource(source) },
                        borderColor = if (isSelected) WarXCyan else GlassBorderSubtle,
                        borderWidth = if (isSelected) 1.5.dp else 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.selectSource(source) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = WarXCyan,
                                    unselectedColor = Color.White.copy(alpha = 0.5f)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = source.quality,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) WarXCyan else Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    NeonBadge(
                                        text = source.format,
                                        color = if (source.isHls) WarXViolet else WarXCyan
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!source.resolution.isNullOrBlank()) {
                                        Text(
                                            text = source.resolution,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                    if (source.fps > 0) {
                                        Text(
                                            text = "${source.fps.toInt()} FPS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                    if (source.bitrate > 0) {
                                        Text(
                                            text = "${source.bitrate / 1000} kbps",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                    if (source.videoCodec != null) {
                                        Text(
                                            text = source.videoCodec,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                    if (source.sizeBytes > 0) {
                                        Text(
                                            text = formatBytes(source.sizeBytes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatusSuccess
                                        )
                                    }
                                }
                            }

                            // Preview Stream button
                            IconButton(onClick = {
                                viewModel.openPreview(source.url, "${metadata.title} [${source.quality}]")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Preview Stream",
                                    tint = WarXCyan
                                )
                            }
                        }
                    }
                }

                // Download Action Button
                item {
                    val currentSource = selectedSource ?: metadata.sources.firstOrNull()
                    if (currentSource != null) {
                        GlowingGradientButton(
                            text = "Unduh ${currentSource.quality} (${currentSource.format})",
                            onClick = {
                                activeMetadataForDownload = metadata
                                customFileName = metadata.title
                                showDownloadDialog = true
                            },
                            icon = Icons.Default.Download,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            ExtractionUiState.Idle -> {
                // Empty state guidance
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = WarXCyan.copy(alpha = 0.8f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Ekstraktor Video Siap Digunakan",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Salin URL halaman video dari browser atau gunakan tombol Uji Coba Cepat di atas untuk mencoba pengunduhan HLS master atau MP4 langsung.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.65f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // Video Player Modal (ExoPlayer preview)
    if (previewVideo != null) {
        val (url, title) = previewVideo!!
        VideoPlayerModal(
            videoUrl = url,
            title = title,
            onDismiss = { viewModel.closePreview() }
        )
    }

    // Download Confirmation & File Naming Dialog
    if (showDownloadDialog && activeMetadataForDownload != null && selectedSource != null) {
        val meta = activeMetadataForDownload!!
        val src = selectedSource!!

        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            containerColor = Color(0xFF0F172A),
            title = {
                Text("Mulai Unduhan", color = WarXCyan, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Kualitas: ${src.quality} (${src.format})",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Nama File:",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customFileName,
                        onValueChange = { customFileName = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarXCyan,
                            unfocusedBorderColor = GlassBorderSubtle,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDownloadDialog = false
                        viewModel.startDownload(
                            context = context,
                            metadata = meta,
                            source = src,
                            customTitle = customFileName,
                            onStarted = {
                                onNavigateToDownloads()
                            }
                        )
                    }
                ) {
                    Text("Unduh Sekarang", color = WarXCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadDialog = false }) {
                    Text("Batal", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
