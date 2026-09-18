package com.ammu.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.ammu.player.R
import com.ammu.player.data.model.PlaylistEntity
import com.ammu.player.data.model.TrackEntity
import com.ammu.player.ui.components.HeartBurstOverlay
import com.ammu.player.ui.components.MiniPlayerBar
import com.ammu.player.ui.dialogs.BackupExportImportDialog
import com.ammu.player.ui.dialogs.StorageAuditorDialog
import com.ammu.player.ui.theme.AccentCyan
import com.ammu.player.ui.theme.AccentGold
import com.ammu.player.ui.theme.AccentPink
import com.ammu.player.ui.theme.AccentRed
import com.ammu.player.ui.theme.PureBlack
import com.ammu.player.ui.theme.SurfaceBorder
import com.ammu.player.ui.theme.SurfaceCard
import com.ammu.player.ui.theme.SurfaceElevated
import com.ammu.player.ui.theme.TextPrimary
import com.ammu.player.ui.theme.TextSecondary
import com.ammu.player.ui.theme.TextTertiary
import com.ammu.player.ui.theme.Typography
import com.ammu.player.ui.viewmodel.AmmuViewModel

@OptIn(UnstableApi::class)
@Composable
fun AmmuMainScreen(
    viewModel: AmmuViewModel,
    modifier: Modifier = Modifier
) {
    val tracks by viewModel.displayedTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val selectedPlaylistId by viewModel.selectedPlaylistId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isCleanClutter by viewModel.isCleanClutterEnabled.collectAsState()

    val currentTrack by viewModel.audioController.currentTrack.collectAsState()
    val isPlaying by viewModel.audioController.isPlaying.collectAsState()
    val positionMs by viewModel.audioController.positionMs.collectAsState()
    val durationMs by viewModel.audioController.durationMs.collectAsState()

    val isMultiSelect by viewModel.isMultiSelectMode.collectAsState()
    val selectedTrackIds by viewModel.selectedTrackIds.collectAsState()
    val undoMsg by viewModel.undoSnackbarMessage.collectAsState()

    val favTrigger by viewModel.favoriteAnimTrigger.collectAsState()
    val lastFavState by viewModel.lastFavoriteState.collectAsState()

    var isFullscreenVisible by remember { mutableStateOf(false) }
    var showStudioMenu by remember { mutableStateOf(false) }
    var showStorageAuditor by remember { mutableStateOf(false) }
    var showBackupVault by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var playlistToAddTo by remember { mutableStateOf<PlaylistEntity?>(null) }
    var showPlaylistPickerForBatch by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(undoMsg) {
        undoMsg?.let { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoLastDelete()
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = PureBlack
    ) {
        Scaffold(
            containerColor = PureBlack,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (currentTrack != null) {
                    MiniPlayerBar(
                        track = currentTrack,
                        isPlaying = isPlaying,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        isFavorite = currentTrack?.isFavorite == true,
                        onPlayPause = { viewModel.playPause() },
                        onNext = { viewModel.skipNext() },
                        onPrevious = { viewModel.skipPrevious() },
                        onToggleFavorite = { currentTrack?.let { viewModel.toggleFavorite(it) } },
                        onExpandToFullscreen = { isFullscreenVisible = true }
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Top Bar (Logo, Ammu title, Studio Hub button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ammu_logo),
                            contentDescription = "Ammu Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Ammu",
                            style = Typography.headlineLarge.copy(fontSize = 26.sp),
                            color = TextPrimary
                        )
                    }

                    Box {
                        IconButton(onClick = { showStudioMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Studio Hub",
                                tint = AccentCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showStudioMenu,
                            onDismissRequest = { showStudioMenu = false },
                            modifier = Modifier.background(SurfaceCard)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Storage Auditor & Duplicates", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null, tint = AccentCyan) },
                                onClick = {
                                    showStudioMenu = false
                                    showStorageAuditor = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("4-Key Vault & Backups", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = AccentPink) },
                                onClick = {
                                    showStudioMenu = false
                                    showBackupVault = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rescan Audio Storage", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = AccentGold) },
                                onClick = {
                                    showStudioMenu = false
                                    viewModel.scanDeviceAudioFiles()
                                }
                            )
                        }
                    }
                }

                // Search Bar with Clean Clutter Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search songs, artists, albums…", color = TextTertiary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // ✨ Clean Clutter Button
                    Box(
                        modifier = Modifier
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCleanClutter) AccentCyan.copy(alpha = 0.2f) else SurfaceCard)
                            .border(1.dp, if (isCleanClutter) AccentCyan else SurfaceBorder, RoundedCornerShape(14.dp))
                            .clickable { viewModel.toggleCleanClutter() }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (isCleanClutter) AccentCyan else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Clean",
                                style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isCleanClutter) AccentCyan else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Playlists Horizontal Scrollable Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    playlists.forEach { pl ->
                        val isSelected = selectedPlaylistId == pl.id || (selectedPlaylistId == null && pl.id == 1L)
                        var showPlMenu by remember { mutableStateOf(false) }

                        Box {
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectedPlaylistId.value = pl.id },
                                label = { Text("${pl.iconEmoji} ${pl.name}") },
                                trailingIcon = {
                                    if (!pl.isSmartPlaylist) {
                                        IconButton(
                                            onClick = { showPlMenu = true },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = AccentCyan,
                                    containerColor = SurfaceCard,
                                    labelColor = TextSecondary
                                )
                            )

                            if (!pl.isSmartPlaylist) {
                                DropdownMenu(
                                    expanded = showPlMenu,
                                    onDismissRequest = { showPlMenu = false },
                                    modifier = Modifier.background(SurfaceCard)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Delete Playlist", color = AccentRed) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AccentRed) },
                                        onClick = {
                                            showPlMenu = false
                                            viewModel.deletePlaylist(pl.id)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Add Custom Playlist Button
                    IconButton(
                        onClick = {
                            newPlaylistName = ""
                            showCreatePlaylistDialog = true
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard)
                            .border(1.dp, SurfaceBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Playlist", tint = AccentCyan, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Multi-Select Batch Action Bar
                AnimatedVisibility(visible = isMultiSelect) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .background(SurfaceElevated, RoundedCornerShape(12.dp))
                            .border(1.dp, AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedTrackIds.size} Selected",
                            style = Typography.titleMedium,
                            color = AccentCyan
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { viewModel.selectAll() }) {
                                Text("Select All", color = TextPrimary)
                            }

                            IconButton(onClick = { showPlaylistPickerForBatch = true }) {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to playlist", tint = AccentCyan)
                            }

                            IconButton(onClick = { viewModel.batchDeleteTracks() }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed)
                            }

                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Default.Clear, contentDescription = "Cancel", tint = TextSecondary)
                            }
                        }
                    }
                }

                // Track Count & Info Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${tracks.size} Songs",
                        style = Typography.labelSmall,
                        color = TextSecondary
                    )
                    if (isCleanClutter) {
                        Text(
                            text = "✨ Clean titles active",
                            style = Typography.labelSmall,
                            color = AccentCyan
                        )
                    }
                }

                // Tracks LazyColumn
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(tracks, key = { it.id }) { track ->
                        val isCurrentPlaying = currentTrack?.id == track.id
                        val isSelected = selectedTrackIds.contains(track.id)

                        TrackRowItem(
                            track = track,
                            isCurrent = isCurrentPlaying,
                            isPlaying = isPlaying && isCurrentPlaying,
                            isCleanClutter = isCleanClutter,
                            isMultiSelect = isMultiSelect,
                            isSelected = isSelected,
                            onClick = {
                                if (isMultiSelect) {
                                    viewModel.toggleTrackSelection(track.id)
                                } else {
                                    viewModel.playTrack(track)
                                }
                            },
                            onLongClick = {
                                viewModel.toggleTrackSelection(track.id)
                            },
                            onFavoriteToggle = {
                                viewModel.toggleFavorite(track)
                            }
                        )
                    }
                }
            }
        }

        // Fullscreen Player Sheet Modal
        AnimatedVisibility(
            visible = isFullscreenVisible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            FullscreenPlayerSheet(
                viewModel = viewModel,
                onDismiss = { isFullscreenVisible = false }
            )
        }

        // Dialogs
        if (showStorageAuditor) {
            StorageAuditorDialog(
                viewModel = viewModel,
                onDismiss = { showStorageAuditor = false }
            )
        }

        if (showBackupVault) {
            BackupExportImportDialog(
                viewModel = viewModel,
                onDismiss = { showBackupVault = false }
            )
        }

        // Create Playlist Dialog
        if (showCreatePlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showCreatePlaylistDialog = false },
                title = { Text("Create New Playlist", color = TextPrimary) },
                text = {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                viewModel.createPlaylist(newPlaylistName)
                                showCreatePlaylistDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                    ) {
                        Text("Create", color = PureBlack)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePlaylistDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = SurfaceCard
            )
        }

        // Batch Add to Playlist Picker Dialog
        if (showPlaylistPickerForBatch) {
            val customPlaylists = playlists.filter { !it.isSmartPlaylist }
            AlertDialog(
                onDismissRequest = { showPlaylistPickerForBatch = false },
                title = { Text("Add to Playlist", color = TextPrimary) },
                text = {
                    Column {
                        if (customPlaylists.isEmpty()) {
                            Text("No custom playlists created yet.", color = TextSecondary)
                        } else {
                            customPlaylists.forEach { pl ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.batchAddToPlaylist(pl.id)
                                            showPlaylistPickerForBatch = false
                                        }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${pl.iconEmoji} ${pl.name}", style = Typography.bodyLarge, color = TextPrimary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPlaylistPickerForBatch = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = SurfaceCard
            )
        }

        // Heart burst particle overlay
        HeartBurstOverlay(
            triggerKey = favTrigger,
            isFavorite = lastFavState,
            onAnimationEnd = {}
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TrackRowItem(
    track: TrackEntity,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isCleanClutter: Boolean,
    isMultiSelect: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> AccentCyan.copy(alpha = 0.18f)
                    isCurrent -> AccentCyan.copy(alpha = 0.08f)
                    else -> SurfaceCard
                }
            )
            .border(
                1.dp,
                if (isSelected) AccentCyan else if (isCurrent) AccentCyan.copy(alpha = 0.3f) else SurfaceBorder,
                RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMultiSelect) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(checkedColor = AccentCyan)
            )
        } else {
            // Album art thumbnail
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PureBlack),
                contentAlignment = Alignment.Center
            ) {
                if (!track.albumArtUri.isNullOrBlank()) {
                    AsyncImage(
                        model = track.albumArtUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (isCurrent) AccentCyan else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.displayTitle(useCleanClutter = isCleanClutter),
                style = Typography.bodyLarge.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                ),
                color = if (isCurrent) AccentCyan else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${track.artist} • %d:%02d".format((track.durationMs / 1000) / 60, (track.durationMs / 1000) % 60),
                style = Typography.bodyMedium.copy(fontSize = 12.sp),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Favorite heart button
        IconButton(
            onClick = onFavoriteToggle,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (track.isFavorite) AccentPink else TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
