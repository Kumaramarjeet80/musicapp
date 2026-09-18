package com.ammu.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.ammu.player.ui.components.HeartBurstOverlay
import com.ammu.player.ui.components.QueueTwoZoneList
import com.ammu.player.ui.components.VinylDiscView
import com.ammu.player.ui.components.WaveformScrubber
import com.ammu.player.ui.dialogs.DualEqualizerView
import com.ammu.player.ui.theme.AccentCyan
import com.ammu.player.ui.theme.AccentGold
import com.ammu.player.ui.theme.AccentPink
import com.ammu.player.ui.theme.AccentPurple
import com.ammu.player.ui.theme.PureBlack
import com.ammu.player.ui.theme.SurfaceBorder
import com.ammu.player.ui.theme.SurfaceCard
import com.ammu.player.ui.theme.SurfaceElevated
import com.ammu.player.ui.theme.TextPrimary
import com.ammu.player.ui.theme.TextSecondary
import com.ammu.player.ui.theme.TextTertiary
import com.ammu.player.ui.theme.Typography
import com.ammu.player.ui.viewmodel.AmmuViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PlayerDrawer {
    NONE,
    VOLUME,
    EQUALIZER,
    TIMESTAMPS,
    AB_LOOP,
    LYRICS,
    QUEUE,
    TRIMMER
}

@OptIn(UnstableApi::class)
@Composable
fun FullscreenPlayerSheet(
    viewModel: AmmuViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val track by viewModel.audioController.currentTrack.collectAsState()
    val isPlaying by viewModel.audioController.isPlaying.collectAsState()
    val positionMs by viewModel.audioController.positionMs.collectAsState()
    val durationMs by viewModel.audioController.durationMs.collectAsState()
    val isShuffle by viewModel.audioController.isShuffleEnabled.collectAsState()
    val repeatMode by viewModel.audioController.repeatMode.collectAsState()
    val isVinylMode by viewModel.isVinylMode.collectAsState()

    val favTrigger by viewModel.favoriteAnimTrigger.collectAsState()
    val lastFavState by viewModel.lastFavoriteState.collectAsState()

    val timestamps by viewModel.currentTimestamps.collectAsState()
    val lyricsEntity by viewModel.currentLyrics.collectAsState()
    val clips by viewModel.currentClips.collectAsState()
    val queue by viewModel.audioController.queue.collectAsState()
    val currentQueueIdx by viewModel.audioController.currentQueueIndex.collectAsState()
    val volume by viewModel.audioController.volume.collectAsState()
    val abLoop by viewModel.audioController.abLoopState.collectAsState()

    var activeDrawer by remember { mutableStateOf(PlayerDrawer.NONE) }
    var seekFeedbackText by remember { mutableStateOf<String?>(null) }

    // Dialog state for adding timestamp
    var showAddTimestampDialog by remember { mutableStateOf(false) }
    var newTimestampLabel by remember { mutableStateOf("") }

    // Dialog state for Audio Trimmer
    var trimmerStartMs by remember { mutableStateOf(0L) }
    var trimmerEndMs by remember { mutableStateOf(30000L) }
    var trimmerClipTitle by remember { mutableStateOf("") }

    // Editable lyrics state
    var editLyricsText by remember { mutableStateOf("") }
    var editNotesText by remember { mutableStateOf("") }
    var isEditingLyrics by remember { mutableStateOf(false) }

    if (track == null) return

    Surface(
        modifier = modifier.fillMaxSize(),
        color = PureBlack
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag-down pull-to-dismiss handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SurfaceBorder)
                    )
                }

                // Top Sheet Bar: Dismiss arrow & Vinyl Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dismiss",
                            tint = TextPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = if (isVinylMode) "Vinyl Disc Mode" else "Cover Mode",
                        style = Typography.bodyMedium,
                        color = AccentCyan
                    )

                    IconButton(onClick = { viewModel.isVinylMode.value = !isVinylMode }) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = "Toggle Vinyl",
                            tint = if (isVinylMode) AccentCyan else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Artwork / Vinyl Disc with Double-Tap Gesture Zones (-10s / +10s)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(durationMs) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    val isLeftHalf = offset.x < (size.width / 2f)
                                    if (isLeftHalf) {
                                        viewModel.seekRelative(-10000L)
                                        seekFeedbackText = "⏪ -10s"
                                    } else {
                                        viewModel.seekRelative(10000L)
                                        seekFeedbackText = "⏩ +10s"
                                    }
                                    coroutineScope.launch {
                                        delay(900)
                                        seekFeedbackText = null
                                    }
                                },
                                onTap = {
                                    viewModel.isVinylMode.value = !isVinylMode
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    VinylDiscView(
                        albumArtUri = track?.albumArtUri,
                        isPlaying = isPlaying,
                        isVinylMode = isVinylMode,
                        onToggleVinylMode = { viewModel.isVinylMode.value = !isVinylMode }
                    )

                    // Floating animated feedback pill for double-tap seek
                    seekFeedbackText?.let { text ->
                        Box(
                            modifier = Modifier
                                .shadow(16.dp, RoundedCornerShape(24.dp))
                                .background(PureBlack.copy(alpha = 0.85f), RoundedCornerShape(24.dp))
                                .border(1.dp, AccentCyan, RoundedCornerShape(24.dp))
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = text,
                                style = Typography.titleLarge,
                                color = AccentCyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Track Title & Favorite Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track?.displayTitle() ?: "",
                            style = Typography.headlineMedium.copy(fontSize = 20.sp),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${track?.artist} • ${track?.album}",
                            style = Typography.bodyLarge,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { track?.let { viewModel.toggleFavorite(it) } },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (track?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (track?.isFavorite == true) AccentPink else TextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Waveform / Scrubber with Timestamp Markers
                WaveformScrubber(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    timestamps = timestamps,
                    onSeek = { viewModel.seekTo(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sticky Playback Controls Anchor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(onClick = { viewModel.toggleShuffle() }) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) AccentCyan else TextTertiary
                        )
                    }

                    // Previous
                    IconButton(onClick = { viewModel.skipPrevious() }) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = TextPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Large Play / Pause Glowing Button
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .shadow(20.dp, CircleShape, ambientColor = AccentCyan, spotColor = AccentCyan)
                            .clip(CircleShape)
                            .background(AccentCyan)
                            .clickable { viewModel.playPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = PureBlack,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Next
                    IconButton(onClick = { viewModel.skipNext() }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = TextPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Repeat
                    IconButton(onClick = { viewModel.toggleRepeat() }) {
                        Icon(
                            imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (repeatMode != Player.REPEAT_MODE_OFF) AccentCyan else TextTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Sub-Drawers Navigation Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard, RoundedCornerShape(20.dp))
                        .padding(vertical = 4.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        activeDrawer = if (activeDrawer == PlayerDrawer.VOLUME) PlayerDrawer.NONE else PlayerDrawer.VOLUME
                    }) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = if (activeDrawer == PlayerDrawer.VOLUME) AccentCyan else TextSecondary)
                    }

                    IconButton(onClick = {
                        activeDrawer = if (activeDrawer == PlayerDrawer.EQUALIZER) PlayerDrawer.NONE else PlayerDrawer.EQUALIZER
                    }) {
                        Icon(Icons.Default.Equalizer, contentDescription = "EQ", tint = if (activeDrawer == PlayerDrawer.EQUALIZER) AccentCyan else TextSecondary)
                    }

                    IconButton(onClick = {
                        activeDrawer = if (activeDrawer == PlayerDrawer.TIMESTAMPS) PlayerDrawer.NONE else PlayerDrawer.TIMESTAMPS
                    }) {
                        Icon(Icons.Default.Bookmark, contentDescription = "Markers", tint = if (activeDrawer == PlayerDrawer.TIMESTAMPS) AccentCyan else TextSecondary)
                    }

                    IconButton(onClick = {
                        activeDrawer = if (activeDrawer == PlayerDrawer.AB_LOOP) PlayerDrawer.NONE else PlayerDrawer.AB_LOOP
                    }) {
                        Text("A-B", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (activeDrawer == PlayerDrawer.AB_LOOP || abLoop.isEnabled) AccentPink else TextSecondary)
                    }

                    IconButton(onClick = {
                        activeDrawer = if (activeDrawer == PlayerDrawer.LYRICS) PlayerDrawer.NONE else PlayerDrawer.LYRICS
                    }) {
                        Icon(Icons.Default.Notes, contentDescription = "Lyrics", tint = if (activeDrawer == PlayerDrawer.LYRICS) AccentCyan else TextSecondary)
                    }

                    IconButton(onClick = {
                        activeDrawer = if (activeDrawer == PlayerDrawer.QUEUE) PlayerDrawer.NONE else PlayerDrawer.QUEUE
                    }) {
                        Icon(Icons.Default.QueueMusic, contentDescription = "Queue", tint = if (activeDrawer == PlayerDrawer.QUEUE) AccentCyan else TextSecondary)
                    }

                    IconButton(onClick = {
                        activeDrawer = if (activeDrawer == PlayerDrawer.TRIMMER) PlayerDrawer.NONE else PlayerDrawer.TRIMMER
                    }) {
                        Icon(Icons.Default.ContentCut, contentDescription = "Trimmer", tint = if (activeDrawer == PlayerDrawer.TRIMMER) AccentPink else TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Sub-Drawers Overlay Container
            AnimatedVisibility(
                visible = activeDrawer != PlayerDrawer.NONE,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.65f)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(SurfaceCard)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    when (activeDrawer) {
                        PlayerDrawer.VOLUME -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Volume Control", style = Typography.titleMedium, color = TextPrimary)
                                    IconButton(onClick = { activeDrawer = PlayerDrawer.NONE }) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextSecondary)
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Slider(
                                    value = volume,
                                    onValueChange = { viewModel.setVolume(it) },
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AccentCyan,
                                        activeTrackColor = AccentCyan,
                                        inactiveTrackColor = SurfaceBorder
                                    )
                                )
                                Text(
                                    text = "${(volume * 100).toInt()}%",
                                    style = Typography.titleLarge,
                                    color = AccentCyan
                                )
                            }
                        }

                        PlayerDrawer.EQUALIZER -> {
                            DualEqualizerView(
                                equalizerEngine = viewModel.equalizerEngine,
                                onDismiss = { activeDrawer = PlayerDrawer.NONE }
                            )
                        }

                        PlayerDrawer.TIMESTAMPS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Timestamp Markers", style = Typography.titleMedium, color = TextPrimary)
                                    IconButton(onClick = {
                                        newTimestampLabel = ""
                                        showAddTimestampDialog = true
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = "Add", tint = AccentCyan)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(timestamps) { marker ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(SurfaceElevated, RoundedCornerShape(8.dp))
                                                .clickable { viewModel.seekTo(marker.timestampMs) }
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(marker.label, style = Typography.bodyLarge, color = TextPrimary)
                                                Text(
                                                    "%d:%02d".format((marker.timestampMs / 1000) / 60, (marker.timestampMs / 1000) % 60),
                                                    style = Typography.bodyMedium,
                                                    color = AccentCyan
                                                )
                                            }
                                            IconButton(onClick = { viewModel.deleteTimestamp(marker.id) }) {
                                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Delete", tint = TextTertiary)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        PlayerDrawer.AB_LOOP -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("A-B Loop Station", style = Typography.titleMedium, color = TextPrimary)
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Button(
                                        onClick = { viewModel.audioController.setAbLoopA(positionMs) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated)
                                    ) {
                                        Text("Set A: %d:%02d".format((abLoop.pointAMs / 1000) / 60, (abLoop.pointAMs / 1000) % 60), color = AccentCyan)
                                    }

                                    Button(
                                        onClick = { viewModel.audioController.setAbLoopB(positionMs) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated)
                                    ) {
                                        Text("Set B: %d:%02d".format((abLoop.pointBMs / 1000) / 60, (abLoop.pointBMs / 1000) % 60), color = AccentPink)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Loop Active", style = Typography.bodyLarge, color = TextPrimary)
                                    Switch(
                                        checked = abLoop.isEnabled,
                                        onCheckedChange = { viewModel.audioController.toggleAbLoop(it) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = AccentPink)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                TextButton(onClick = { viewModel.audioController.clearAbLoop() }) {
                                    Text("Clear A-B Markers", color = TextSecondary)
                                }
                            }
                        }

                        PlayerDrawer.LYRICS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Offline Lyrics & Notes", style = Typography.titleMedium, color = TextPrimary)
                                    TextButton(onClick = {
                                        if (isEditingLyrics) {
                                            viewModel.saveLyricsAndNotes(editLyricsText, editNotesText)
                                        } else {
                                            editLyricsText = lyricsEntity?.plainLyrics ?: ""
                                            editNotesText = lyricsEntity?.notes ?: ""
                                        }
                                        isEditingLyrics = !isEditingLyrics
                                    }) {
                                        Text(if (isEditingLyrics) "Save" else "Edit", color = AccentCyan)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (isEditingLyrics) {
                                    OutlinedTextField(
                                        value = editLyricsText,
                                        onValueChange = { editLyricsText = it },
                                        label = { Text("Song Lyrics") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = editNotesText,
                                        onValueChange = { editNotesText = it },
                                        label = { Text("Personal Notes") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(0.5f)
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        Text(
                                            text = lyricsEntity?.plainLyrics?.ifBlank { "No offline lyrics available yet. Tap Edit to paste." } ?: "No offline lyrics available yet. Tap Edit to paste.",
                                            style = Typography.bodyLarge.copy(lineHeight = 26.sp),
                                            color = TextPrimary
                                        )

                                        if (!lyricsEntity?.notes.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text("Notes:", style = Typography.titleSmall, color = AccentCyan)
                                            Text(lyricsEntity?.notes ?: "", style = Typography.bodyMedium, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }

                        PlayerDrawer.QUEUE -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Active Queue (${queue.size})", style = Typography.titleMedium, color = TextPrimary)
                                    IconButton(onClick = { activeDrawer = PlayerDrawer.NONE }) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextSecondary)
                                    }
                                }

                                Text(
                                    "Zone A: Long-press & drag to reorder. Zone B: Shift Up/Down buttons.",
                                    style = Typography.bodyMedium.copy(fontSize = 11.sp),
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                QueueTwoZoneList(
                                    queue = queue,
                                    currentIndex = currentQueueIdx,
                                    onTrackSelected = { viewModel.playTrack(it) },
                                    onReorder = { from, to -> viewModel.audioController.reorderQueue(from, to) },
                                    onRemove = { viewModel.audioController.removeFromQueue(it) }
                                )
                            }
                        }

                        PlayerDrawer.TRIMMER -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text("Real MP3 Audio Slicer", style = Typography.titleMedium, color = AccentPink)
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = trimmerClipTitle,
                                    onValueChange = { trimmerClipTitle = it },
                                    label = { Text("Clip Title") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    "Range: %d:%02d - %d:%02d".format(
                                        (trimmerStartMs / 1000) / 60, (trimmerStartMs / 1000) % 60,
                                        (trimmerEndMs / 1000) / 60, (trimmerEndMs / 1000) % 60
                                    ),
                                    style = Typography.bodyMedium,
                                    color = AccentCyan
                                )

                                RangeSlider(
                                    value = trimmerStartMs.toFloat()..trimmerEndMs.toFloat(),
                                    onValueChange = { range ->
                                        trimmerStartMs = range.start.toLong()
                                        trimmerEndMs = range.endInclusive.toLong()
                                    },
                                    valueRange = 0f..durationMs.toFloat().coerceAtLeast(1000f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = AccentPink,
                                        activeTrackColor = AccentPink
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        viewModel.sliceAudioClip(
                                            title = trimmerClipTitle.ifBlank { "Clip_${System.currentTimeMillis()}" },
                                            startMs = trimmerStartMs,
                                            endMs = trimmerEndMs
                                        ) {
                                            activeDrawer = PlayerDrawer.NONE
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPink)
                                ) {
                                    Text("Slice & Save MP3 Clip", color = TextPrimary)
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text("Trimmed Clips in Library:", style = Typography.titleSmall, color = TextPrimary)
                                clips.forEach { clip ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(clip.clipTitle, style = Typography.bodyMedium, color = TextSecondary)
                                        Text(
                                            "%d:%02d - %d:%02d".format(
                                                (clip.startMs / 1000) / 60, (clip.startMs / 1000) % 60,
                                                (clip.endMs / 1000) / 60, (clip.endMs / 1000) % 60
                                            ),
                                            style = Typography.bodyMedium,
                                            color = AccentPink
                                        )
                                    }
                                }
                            }
                        }
                        PlayerDrawer.NONE -> {}
                    }
                }
            }

            // Heart burst particle overlay & emotional toast
            HeartBurstOverlay(
                triggerKey = favTrigger,
                isFavorite = lastFavState,
                onAnimationEnd = {}
            )
        }
    }

    // Add Timestamp Dialog
    if (showAddTimestampDialog) {
        AlertDialog(
            onDismissRequest = { showAddTimestampDialog = false },
            title = { Text("Add Timestamp Marker", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newTimestampLabel,
                    onValueChange = { newTimestampLabel = it },
                    label = { Text("Marker Label (e.g. Solo, Drop)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addTimestamp(newTimestampLabel)
                    showAddTimestampDialog = false
                }) {
                    Text("Add Marker")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTimestampDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard
        )
    }
}
