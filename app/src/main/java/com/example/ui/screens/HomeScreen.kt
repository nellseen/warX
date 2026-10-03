package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.model.MediaSource
import com.example.model.VideoMetadata
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassDialog
import com.example.ui.components.GlassSection
import com.example.ui.components.GlassTextField
import com.example.ui.components.SkeletonShimmer
import com.example.ui.components.VideoPlayerModal
import com.example.ui.theme.WarXTheme
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
    val colors = WarXTheme.colors

    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedSource by viewModel.selectedSource.collectAsStateWithLifecycle()
    val previewVideo by viewModel.previewVideoUrl.collectAsStateWithLifecycle()

    var showDownloadDialog by remember { mutableStateOf(false) }
    var customFileName by remember { mutableStateOf("") }
    var activeMetadataForDownload by remember { mutableStateOf<VideoMetadata?>(null) }
    var showExtendedSpecs by remember { mutableStateOf(false) }

    val testPresets = listOf(
        Pair("Big Buck Bunny HLS", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
        Pair("Tears of Steel HLS", "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"),
        Pair("Sample Direct MP4", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4")
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.accentGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = colors.buttonTextOnAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "WarX",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = colors.accentCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Downloader",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ekstraksi mendalam video web & downloader HLS master/variant native tanpa transcoding.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        // 2. URL Input Section
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (urlInput.isNotBlank()) colors.glassBorder else null
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Tautan Halaman Video",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    GlassTextField(
                        value = urlInput,
                        onValueChange = { viewModel.onUrlChanged(it) },
                        placeholder = "Tempelkan tautan web (HTML/HLS/MP4)...",
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (urlInput.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.clearUrl() }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Hapus",
                                            tint = colors.textMuted
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
                                        tint = colors.accentCyan
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                viewModel.startExtraction()
                            }
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    GlassButton(
                        text = "Analisis & Ekstrak Video",
                        onClick = {
                            keyboardController?.hide()
                            viewModel.startExtraction()
                        },
                        icon = Icons.Default.Search,
                        enabled = uiState !is ExtractionUiState.Extracting && urlInput.isNotBlank(),
                        isLoading = uiState is ExtractionUiState.Extracting,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preset Quick Stream Chips
                    Text(
                        text = "Uji Coba Cepat (Sample Streams):",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for ((name, streamUrl) in testPresets) {
                            GlassChip(
                                text = name,
                                color = colors.accentViolet,
                                onClick = {
                                    viewModel.onUrlChanged(streamUrl)
                                    viewModel.startExtraction()
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. Extraction State (Loading / Error / Success)
        when (val state = uiState) {
            is ExtractionUiState.Extracting -> {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = colors.accentCyan.copy(alpha = 0.6f)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = colors.accentCyan,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = state.stage.message,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colors.accentCyan
                                    )
                                    Text(
                                        text = state.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            SkeletonShimmer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            )
                        }
                    }
                }
            }

            is ExtractionUiState.Error -> {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = colors.statusError.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = colors.statusError,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Gagal Mengekstrak Video",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colors.statusError
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            is ExtractionUiState.Success -> {
                val metadata = state.metadata

                // 4. Metadata Preview Card
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = colors.glassBorder
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Thumbnail Preview with Play Overlay
                            if (!metadata.thumbnailUrl.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.Black.copy(alpha = 0.5f))
                                ) {
                                    AsyncImage(
                                        model = metadata.thumbnailUrl,
                                        contentDescription = "Thumbnail",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    val firstPlayableUrl = selectedSource?.url ?: metadata.sources.firstOrNull()?.url
                                    if (firstPlayableUrl != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .align(Alignment.Center)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.65f))
                                                .border(2.dp, colors.accentCyan, CircleShape)
                                                .clickable {
                                                    viewModel.openPreview(firstPlayableUrl, metadata.title)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Putar",
                                                tint = colors.accentCyan,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Badges: Domain & Duration
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GlassChip(
                                    text = metadata.domain,
                                    color = colors.accentCyan
                                )
                                if (metadata.durationFormatted != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    GlassChip(
                                        text = metadata.durationFormatted,
                                        color = colors.accentViolet
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = metadata.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!metadata.author.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Uploader: ${metadata.author}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                            }

                            if (!metadata.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = metadata.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Expandable Technical Specs
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showExtendedSpecs = !showExtendedSpecs }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = colors.accentCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Spesifikasi Teknis Stream",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.accentCyan
                                    )
                                }
                                Icon(
                                    imageVector = if (showExtendedSpecs) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = colors.accentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = showExtendedSpecs,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    val highestVariant = metadata.sources.firstOrNull { !it.isMasterPlaylist }
                                        ?: metadata.sources.firstOrNull()

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (highestVariant?.resolution != null) {
                                            GlassChip(text = "Resolusi: ${highestVariant.resolution}", color = colors.accentCyan)
                                        }
                                        if (highestVariant?.videoCodec != null) {
                                            GlassChip(text = "Video: ${highestVariant.videoCodec}", color = colors.accentViolet)
                                        }
                                        if (highestVariant?.audioCodec != null) {
                                            GlassChip(text = "Audio: ${highestVariant.audioCodec}", color = colors.accentViolet)
                                        }
                                        if (highestVariant != null && highestVariant.fps > 0) {
                                            GlassChip(text = "${highestVariant.fps.toInt()} FPS", color = colors.accentCyan)
                                        }
                                        if (highestVariant != null && highestVariant.bitrate > 0) {
                                            GlassChip(text = "${highestVariant.bitrate / 1000} kbps", color = colors.accentCyan)
                                        }
                                        if (metadata.subtitles.isNotEmpty()) {
                                            GlassChip(text = "${metadata.subtitles.size} Subtitle", color = colors.statusInfo)
                                        }
                                        if (metadata.audioTracks.isNotEmpty()) {
                                            GlassChip(text = "${metadata.audioTracks.size} Audio Tracks", color = colors.statusInfo)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Sources & Quality Header
                item {
                    Text(
                        text = "Pilih Kualitas / Variant Stream (${metadata.sources.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                }

                // List of Streams
                items(metadata.sources) { source ->
                    val isSelected = selectedSource?.id == source.id
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectSource(source) },
                        borderColor = if (isSelected) colors.accentCyan else colors.glassBorderSubtle,
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
                                    selectedColor = colors.accentCyan,
                                    unselectedColor = colors.textMuted
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = source.quality,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) colors.accentCyan else colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    GlassChip(
                                        text = source.format,
                                        color = if (source.isHls) colors.accentViolet else colors.accentCyan
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
                                            color = colors.textMuted
                                        )
                                    }
                                    if (source.fps > 0) {
                                        Text(
                                            text = "${source.fps.toInt()} FPS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textMuted
                                        )
                                    }
                                    if (source.bitrate > 0) {
                                        Text(
                                            text = "${source.bitrate / 1000} kbps",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textMuted
                                        )
                                    }
                                    if (source.sizeBytes > 0) {
                                        Text(
                                            text = formatBytes(source.sizeBytes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.statusSuccess
                                        )
                                    }
                                }
                            }

                            // Preview Button
                            IconButton(onClick = {
                                viewModel.openPreview(source.url, "${metadata.title} [${source.quality}]")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Preview Stream",
                                    tint = colors.accentCyan
                                )
                            }
                        }
                    }
                }

                // 6. Download Action Button
                item {
                    val currentSource = selectedSource ?: metadata.sources.firstOrNull()
                    if (currentSource != null) {
                        GlassButton(
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
                // Empty state card
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = colors.accentCyan.copy(alpha = 0.85f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Ekstraktor Video Siap Digunakan",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Salin URL halaman video dari browser atau gunakan tombol Uji Coba Cepat di atas untuk mencoba pengunduhan HLS master atau MP4 langsung.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // Video Player Modal
    if (previewVideo != null) {
        val (url, title) = previewVideo!!
        VideoPlayerModal(
            videoUrl = url,
            title = title,
            onDismiss = { viewModel.closePreview() }
        )
    }

    // Download Confirmation Dialog
    if (showDownloadDialog && activeMetadataForDownload != null && selectedSource != null) {
        val meta = activeMetadataForDownload!!
        val src = selectedSource!!

        GlassDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = "Mulai Unduhan",
            confirmButton = {
                GlassButton(
                    text = "Unduh Sekarang",
                    onClick = {
                        showDownloadDialog = false
                        viewModel.startDownload(
                            context = context,
                            metadata = meta,
                            source = src,
                            customTitle = customFileName,
                            onStarted = { onNavigateToDownloads() }
                        )
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showDownloadDialog = false }) {
                    Text("Batal", color = colors.textMuted)
                }
            }
        ) {
            Column {
                Text(
                    text = "Kualitas: ${src.quality} (${src.format})",
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Nama File Video:",
                    color = colors.textMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                GlassTextField(
                    value = customFileName,
                    onValueChange = { customFileName = it },
                    placeholder = "Nama file..."
                )
            }
        }
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
