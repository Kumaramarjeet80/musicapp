package com.ammu.player.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import com.ammu.player.data.backup.BackupPayload
import com.ammu.player.data.backup.ExportPermissions
import com.ammu.player.data.backup.Phase1Result
import com.ammu.player.data.backup.SecurityKeySuite
import com.ammu.player.ui.theme.AccentCyan
import com.ammu.player.ui.theme.AccentGreen
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
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun BackupExportImportDialog(
    viewModel: AmmuViewModel,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Export, 1: Import
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceCard)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Dialog Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "4-Key Vault & Backup",
                            style = Typography.titleLarge,
                            color = TextPrimary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = PureBlack,
                    contentColor = AccentCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = if (selectedTab == 0) AccentCyan else AccentPink
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("4-Key Export", color = if (selectedTab == 0) AccentCyan else TextSecondary) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Dual-Phase Import", color = if (selectedTab == 1) AccentPink else TextSecondary) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    ExportSuiteView(viewModel = viewModel, context = context)
                } else {
                    ImportSuiteView(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ExportSuiteView(viewModel: AmmuViewModel, context: Context) {
    val coroutineScope = rememberCoroutineScope()
    val playlists by viewModel.playlists.collectAsState()

    // 4 Keys
    var masterKey by remember { mutableStateOf(viewModel.securityManager.getSavedMasterKey() ?: "") }
    var aesKey by remember { mutableStateOf(viewModel.securityManager.getSavedAesKey() ?: "") }
    var creatorPasskey by remember { mutableStateOf(viewModel.securityManager.getSavedCreatorPasskey() ?: "") }
    var downloadKey by remember { mutableStateOf(viewModel.securityManager.getSavedDownloadKey() ?: "") }

    // Permissions
    var includePlaylists by remember { mutableStateOf(true) }
    var includeAudioBlobs by remember { mutableStateOf(false) }
    var includeTrimmedClips by remember { mutableStateOf(true) }
    var includeTimestamps by remember { mutableStateOf(true) }
    var includeLyricsAndNotes by remember { mutableStateOf(true) }
    var allowDownloads by remember { mutableStateOf(true) }

    var exportedPayloadString by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "4-Key Security Suite (Optional / Granular)",
            style = Typography.titleMedium,
            color = AccentCyan
        )
        Text(
            text = "Configure cryptographic keys to protect your export archive with AES-256-GCM.",
            style = Typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 4-Key Inputs
        OutlinedTextField(
            value = masterKey,
            onValueChange = { masterKey = it; viewModel.securityManager.setSavedMasterKey(it) },
            label = { Text("1. Master Key (Root)") },
            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = AccentCyan) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = SurfaceBorder
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = aesKey,
            onValueChange = { aesKey = it; viewModel.securityManager.setSavedAesKey(it) },
            label = { Text("2. AES-256-GCM Cipher Key") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AccentCyan) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = SurfaceBorder
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = creatorPasskey,
                onValueChange = { creatorPasskey = it; viewModel.securityManager.setSavedCreatorPasskey(it) },
                label = { Text("3. Creator Key") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = SurfaceBorder
                )
            )

            OutlinedTextField(
                value = downloadKey,
                onValueChange = { downloadKey = it; viewModel.securityManager.setSavedDownloadKey(it) },
                label = { Text("4. Download Key") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = SurfaceBorder
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Granular Permission Matrix
        Text(
            text = "Granular Permission Matrix",
            style = Typography.titleMedium,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Include Custom Playlists", style = Typography.bodyMedium, color = TextPrimary)
            Switch(
                checked = includePlaylists,
                onCheckedChange = { includePlaylists = it },
                colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Include Offline Lyrics & Notes", style = Typography.bodyMedium, color = TextPrimary)
            Switch(
                checked = includeLyricsAndNotes,
                onCheckedChange = { includeLyricsAndNotes = it },
                colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Include Timestamp Markers", style = Typography.bodyMedium, color = TextPrimary)
            Switch(
                checked = includeTimestamps,
                onCheckedChange = { includeTimestamps = it },
                colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Include Trimmed MP3 Clips", style = Typography.bodyMedium, color = TextPrimary)
            Switch(
                checked = includeTrimmedClips,
                onCheckedChange = { includeTrimmedClips = it },
                colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Embed Audio Blobs (Base64)", style = Typography.bodyMedium, color = TextPrimary)
            Switch(
                checked = includeAudioBlobs,
                onCheckedChange = { includeAudioBlobs = it },
                colors = SwitchDefaults.colors(checkedThumbColor = AccentPink)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Export Action
        Button(
            onClick = {
                isExporting = true
                coroutineScope.launch {
                    val permissions = ExportPermissions(
                        allowTrackDownloads = allowDownloads,
                        includeAudioBlobs = includeAudioBlobs,
                        includeTrimmedClips = includeTrimmedClips,
                        includeTimestamps = includeTimestamps,
                        includeLyricsAndNotes = includeLyricsAndNotes,
                        includePlaylists = includePlaylists
                    )
                    val keys = SecurityKeySuite(
                        masterKey = masterKey,
                        aesGcmKey = aesKey,
                        creatorPasskey = creatorPasskey,
                        downloadKey = downloadKey
                    )
                    val result = viewModel.exportBackup(
                        selectedPlaylists = playlists.map { it.id }.toSet(),
                        permissions = permissions,
                        securityKeys = keys
                    )
                    isExporting = false
                    exportedPayloadString = result.getOrNull()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
        ) {
            if (isExporting) {
                CircularProgressIndicator(color = PureBlack, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Generate Vault Export", color = PureBlack, fontWeight = FontWeight.Bold)
            }
        }

        // Export Result Display
        exportedPayloadString?.let { payload ->
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceElevated, RoundedCornerShape(12.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (aesKey.isNotBlank()) "🔒 Encrypted AES-256-GCM Payload" else "📄 Plain JSON Vault Payload",
                            style = Typography.titleMedium.copy(fontSize = 12.sp),
                            color = AccentCyan
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Ammu Backup", payload)
                                clipboard.setPrimaryClip(clip)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = AccentCyan, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = payload.take(240) + if (payload.length > 240) "…" else "",
                        style = Typography.bodyMedium.copy(fontSize = 11.sp),
                        color = TextSecondary,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ImportSuiteView(viewModel: AmmuViewModel) {
    val coroutineScope = rememberCoroutineScope()
    var rawInputText by remember { mutableStateOf("") }
    var keyAttemptText by remember { mutableStateOf("") }

    val phase1Result by viewModel.phase1Result.collectAsState()
    val storageMatches by viewModel.storageMatchResults.collectAsState()

    var importPlaylists by remember { mutableStateOf(true) }
    var importTimestamps by remember { mutableStateOf(true) }
    var importLyrics by remember { mutableStateOf(true) }
    var importClips by remember { mutableStateOf(true) }

    var importSuccessMessage by remember { mutableStateOf<String?>(null) }
    var isExecutingPhase1 by remember { mutableStateOf(false) }
    var isExecutingPhase2 by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Phase 1: Verification & Decryption Guard",
            style = Typography.titleMedium,
            color = AccentPink
        )
        Text(
            text = "Paste backup data and provide the decryption key to verify vault authenticity.",
            style = Typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = rawInputText,
            onValueChange = { rawInputText = it },
            label = { Text("Backup Payload (JSON or Base64 Envelope)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentPink,
                unfocusedBorderColor = SurfaceBorder
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = keyAttemptText,
            onValueChange = { keyAttemptText = it },
            label = { Text("Decryption Key / Super-Key Guard") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentPink,
                unfocusedBorderColor = SurfaceBorder
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                isExecutingPhase1 = true
                coroutineScope.launch {
                    viewModel.runPhase1Import(rawInputText, keyAttemptText)
                    isExecutingPhase1 = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPink)
        ) {
            if (isExecutingPhase1) {
                CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Verify & Decrypt Phase 1", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Phase 1 Feedback
        when (val result = phase1Result) {
            is Phase1Result.SuperKeyRejected -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AccentRed.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .border(1.dp, AccentRed, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(text = "🛡️ ${result.message}", style = Typography.bodyMedium, color = AccentRed)
                }
            }
            is Phase1Result.DecryptionFailed -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AccentRed.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(text = "✕ ${result.error}", style = Typography.bodyMedium, color = AccentRed)
                }
            }
            is Phase1Result.CorruptedPayload -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AccentRed.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(text = "✕ ${result.error}", style = Typography.bodyMedium, color = AccentRed)
                }
            }
            is Phase1Result.Success -> {
                // Phase 2 View
                val payload = result.payload
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceElevated, RoundedCornerShape(14.dp))
                        .border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Phase 1 Verified (${payload.tracks.size} tracks, ${payload.playlists.size} playlists)",
                            style = Typography.titleMedium.copy(fontSize = 13.sp),
                            color = AccentGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Phase 2: Storage Matcher & Verification Audit",
                        style = Typography.titleMedium,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Storage Matcher Badges List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(storageMatches) { match ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PureBlack, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = match.title,
                                    style = Typography.bodyMedium.copy(fontSize = 11.sp),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                if (match.isAvailableLocally) {
                                    Text(
                                        text = "✓ Available",
                                        style = Typography.labelSmall,
                                        color = AccentGreen,
                                        modifier = Modifier
                                            .background(AccentGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                } else {
                                    Text(
                                        text = "✕ Missing",
                                        style = Typography.labelSmall,
                                        color = AccentRed,
                                        modifier = Modifier
                                            .background(AccentRed.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Import permissions checkboxes
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = importPlaylists,
                            onCheckedChange = { importPlaylists = it },
                            colors = CheckboxDefaults.colors(checkedColor = AccentCyan)
                        )
                        Text("Import Playlists", style = Typography.bodyMedium, color = TextPrimary)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = importLyrics,
                            onCheckedChange = { importLyrics = it },
                            colors = CheckboxDefaults.colors(checkedColor = AccentCyan)
                        )
                        Text("Import Offline Lyrics & Notes", style = Typography.bodyMedium, color = TextPrimary)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = importTimestamps,
                            onCheckedChange = { importTimestamps = it },
                            colors = CheckboxDefaults.colors(checkedColor = AccentCyan)
                        )
                        Text("Import Timestamp Markers", style = Typography.bodyMedium, color = TextPrimary)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = importClips,
                            onCheckedChange = { importClips = it },
                            colors = CheckboxDefaults.colors(checkedColor = AccentCyan)
                        )
                        Text("Import Trimmed Clips & Audio Blobs", style = Typography.bodyMedium, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            isExecutingPhase2 = true
                            coroutineScope.launch {
                                val res = viewModel.runPhase2Import(
                                    payload = payload,
                                    importPlaylists = importPlaylists,
                                    importTimestamps = importTimestamps,
                                    importLyrics = importLyrics,
                                    importClips = importClips
                                )
                                isExecutingPhase2 = false
                                importSuccessMessage = "Successfully imported ${res.getOrDefault(0)} items into your library!"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) {
                        if (isExecutingPhase2) {
                            CircularProgressIndicator(color = PureBlack, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Execute Phase 2 Restore", color = PureBlack, fontWeight = FontWeight.Bold)
                        }
                    }

                    importSuccessMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = msg, style = Typography.bodyMedium, color = AccentGreen)
                    }
                }
            }
            null -> {}
        }
    }
}
