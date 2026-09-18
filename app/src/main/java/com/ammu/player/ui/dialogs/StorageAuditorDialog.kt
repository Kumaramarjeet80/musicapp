package com.ammu.player.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import com.ammu.player.ui.theme.AccentCyan
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
fun StorageAuditorDialog(
    viewModel: AmmuViewModel,
    onDismiss: () -> Unit
) {
    val auditSummary by viewModel.storageAuditResult.collectAsState()
    val isAuditing by viewModel.isAuditingStorage.collectAsState()
    var showConfirmPurge by remember { mutableStateOf(false) }
    var purgedCountMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.runStorageAudit()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceCard)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Storage Auditor",
                            style = Typography.titleLarge,
                            color = TextPrimary
                        )
                    }

                    if (isAuditing) {
                        CircularProgressIndicator(
                            color = AccentCyan,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.runStorageAudit() }) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = "Rescan",
                                tint = AccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats Dashboard
                auditSummary?.let { summary ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PureBlack, RoundedCornerShape(14.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${summary.totalTracksChecked}",
                                style = Typography.titleLarge.copy(fontSize = 18.sp),
                                color = AccentCyan
                            )
                            Text(
                                text = "Indexed Tracks",
                                style = Typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formatFileSize(summary.totalFilesSize),
                                style = Typography.titleLarge.copy(fontSize = 18.sp),
                                color = TextPrimary
                            )
                            Text(
                                text = "Total Size",
                                style = Typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formatFileSize(summary.totalWastedBytes),
                                style = Typography.titleLarge.copy(fontSize = 18.sp),
                                color = if (summary.totalWastedBytes > 0) AccentRed else TextSecondary
                            )
                            Text(
                                text = "Duplicate Waste",
                                style = Typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    purgedCountMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AccentCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(text = msg, style = Typography.bodyMedium, color = AccentCyan)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Duplicates List
                    Text(
                        text = if (summary.duplicateGroups.isEmpty()) "✓ No duplicate audio blobs found!" else "Detected Duplicate Blobs (${summary.duplicateGroups.size} Groups)",
                        style = Typography.titleMedium.copy(fontSize = 13.sp),
                        color = if (summary.duplicateGroups.isEmpty()) AccentCyan else TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(summary.duplicateGroups) { group ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceElevated, RoundedCornerShape(10.dp))
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = group.originalTrack.displayTitle(),
                                        style = Typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "+${group.duplicateTracks.size} copies (${formatFileSize(group.potentialSavedBytes)})",
                                        style = Typography.bodyMedium.copy(fontSize = 11.sp),
                                        color = AccentRed
                                    )
                                }

                                Text(
                                    text = "Keep: ${group.originalTrack.path}",
                                    style = Typography.bodyMedium.copy(fontSize = 10.sp),
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                group.duplicateTracks.forEach { dup ->
                                    Text(
                                        text = "Purge: ${dup.path}",
                                        style = Typography.bodyMedium.copy(fontSize = 10.sp),
                                        color = AccentPink.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close", color = TextSecondary)
                        }

                        if (summary.duplicateGroups.isNotEmpty()) {
                            Button(
                                onClick = { showConfirmPurge = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Purge Duplicates")
                            }
                        }
                    }
                }
            }
        }
    }

    // Purge confirmation dialog
    if (showConfirmPurge) {
        AlertDialog(
            onDismissRequest = { showConfirmPurge = false },
            title = { Text("Purge Duplicate Blobs?", color = TextPrimary) },
            text = {
                Text(
                    "This will delete redundant physical audio files from disk and remove them from your library. The primary original track in each group will be preserved.",
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmPurge = false
                        auditSummary?.let { summary ->
                            viewModel.purgeDuplicates(summary.duplicateGroups) { count ->
                                purgedCountMessage = "Successfully purged $count duplicate files!"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Confirm Purge")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmPurge = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.2f GB".format(gb)
        mb >= 1.0 -> "%.1f MB".format(mb)
        kb >= 1.0 -> "%.1f KB".format(kb)
        else -> "$bytes B"
    }
}
