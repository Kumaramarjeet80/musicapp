package com.ammu.player.ui.dialogs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ammu.player.audio.dsp.DualEqualizerEngine
import com.ammu.player.audio.dsp.EqualizerMode
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

@Composable
fun DualEqualizerView(
    equalizerEngine: DualEqualizerEngine,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eqState by equalizerEngine.state.collectAsState()
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var jsonInputText by remember { mutableStateOf("") }
    var exportJsonText by remember { mutableStateOf("") }

    val splinePoints = remember(eqState.bandGains, eqState.isEnabled) {
        equalizerEngine.calculateSplinePoints(80)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Studio DSP Equalizer",
                        style = Typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = if (eqState.mode == EqualizerMode.NATIVE_VLC) "10-Band Native VLC Mode" else "10-Band Vivo Studio DSP Mode",
                        style = Typography.bodyMedium,
                        color = AccentCyan
                    )
                }

                Switch(
                    checked = eqState.isEnabled,
                    onCheckedChange = { equalizerEngine.setEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentCyan,
                        checkedTrackColor = AccentCyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = SurfaceElevated
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Selector (Native VLC vs Vivo Studio DSP)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureBlack, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (eqState.mode == EqualizerMode.NATIVE_VLC) SurfaceElevated else Color.Transparent)
                        .clickable { equalizerEngine.setMode(EqualizerMode.NATIVE_VLC) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Native VLC Mode",
                        style = Typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = if (eqState.mode == EqualizerMode.NATIVE_VLC) AccentCyan else TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (eqState.mode == EqualizerMode.VIVO_STUDIO_DSP) SurfaceElevated else Color.Transparent)
                        .clickable { equalizerEngine.setMode(EqualizerMode.VIVO_STUDIO_DSP) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Vivo Studio DSP",
                        style = Typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = if (eqState.mode == EqualizerMode.VIVO_STUDIO_DSP) AccentPink else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Auto-Gain Headroom Protection Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceElevated, RoundedCornerShape(10.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Auto Headroom: -20% (-2dB) Headroom Guard",
                        style = Typography.bodyMedium.copy(fontSize = 11.sp),
                        color = TextPrimary
                    )
                }

                Switch(
                    checked = eqState.autoGainReductionEnabled,
                    onCheckedChange = { equalizerEngine.toggleAutoGain(it) },
                    modifier = Modifier.size(36.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentGold,
                        checkedTrackColor = AccentGold.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real-Time Bezier Spline Frequency Response Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PureBlack)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                    val w = size.width
                    val h = size.height
                    val midY = h / 2f

                    // Grid reference lines: 0dB, +6dB, -6dB
                    drawLine(
                        color = SurfaceBorder,
                        start = Offset(0f, midY),
                        end = Offset(w, midY),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = SurfaceBorder.copy(alpha = 0.5f),
                        start = Offset(0f, midY - (h / 4f)),
                        end = Offset(w, midY - (h / 4f)),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = SurfaceBorder.copy(alpha = 0.5f),
                        start = Offset(0f, midY + (h / 4f)),
                        end = Offset(w, midY + (h / 4f)),
                        strokeWidth = 1f
                    )

                    if (splinePoints.isNotEmpty()) {
                        val path = Path()
                        val fillPath = Path()

                        // Map dB [-12, +12] to Y [h, 0]
                        fun mapDbToY(db: Float): Float {
                            val norm = (db + 12f) / 24f
                            return h - (norm * h)
                        }

                        val firstX = splinePoints.first().first * w
                        val firstY = mapDbToY(splinePoints.first().second)
                        path.moveTo(firstX, firstY)
                        fillPath.moveTo(firstX, h)
                        fillPath.lineTo(firstX, firstY)

                        for (i in 1 until splinePoints.size) {
                            val prev = splinePoints[i - 1]
                            val curr = splinePoints[i]

                            val pX = prev.first * w
                            val pY = mapDbToY(prev.second)
                            val cX = curr.first * w
                            val cY = mapDbToY(curr.second)

                            val midX = (pX + cX) / 2f
                            path.quadraticBezierTo(pX, pY, midX, (pY + cY) / 2f)
                            fillPath.quadraticBezierTo(pX, pY, midX, (pY + cY) / 2f)
                        }

                        val last = splinePoints.last()
                        val lastX = last.first * w
                        val lastY = mapDbToY(last.second)
                        path.lineTo(lastX, lastY)
                        fillPath.lineTo(lastX, lastY)
                        fillPath.lineTo(lastX, h)
                        fillPath.close()

                        // Gradient fill under curve
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    AccentCyan.copy(alpha = if (eqState.isEnabled) 0.35f else 0.08f),
                                    Color.Transparent
                                )
                            )
                        )

                        // Spline curve stroke
                        drawPath(
                            path = path,
                            brush = Brush.horizontalGradient(
                                colors = listOf(AccentCyan, AccentPurple, AccentPink)
                            ),
                            style = Stroke(width = if (eqState.isEnabled) 3.5f else 1.5f)
                        )
                    }
                }

                // Grid dB label stamps
                Text(
                    text = "+12 dB",
                    style = Typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(start = 6.dp, top = 4.dp)
                )
                Text(
                    text = "0 dB",
                    style = Typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp)
                )
                Text(
                    text = "-12 dB",
                    style = Typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 6.dp, bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Presets Horizontal Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                equalizerEngine.defaultPresets.forEach { preset ->
                    val isSelected = eqState.currentPresetName == preset.name
                    FilterChip(
                        selected = isSelected,
                        onClick = { equalizerEngine.applyPreset(preset) },
                        label = { Text(preset.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentCyan.copy(alpha = 0.25f),
                            selectedLabelColor = AccentCyan,
                            containerColor = SurfaceElevated,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 10-Band Sliders
            Text(
                text = "Frequency Bands",
                style = Typography.titleMedium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            val freqLabels = listOf("60Hz", "150Hz", "400Hz", "1kHz", "2.4k", "4kHz", "7kHz", "10k", "13k", "16k")

            eqState.bandGains.forEachIndexed { index, gain ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = freqLabels.getOrElse(index) { "B$index" },
                        style = Typography.bodyMedium.copy(fontSize = 11.sp),
                        color = TextSecondary,
                        modifier = Modifier.width(48.dp)
                    )

                    Slider(
                        value = gain,
                        onValueChange = { equalizerEngine.setBandGain(index, it) },
                        valueRange = -12f..12f,
                        enabled = eqState.isEnabled,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = AccentCyan,
                            activeTrackColor = AccentCyan,
                            inactiveTrackColor = SurfaceBorder
                        )
                    )

                    Text(
                        text = "%+.1f dB".format(gain),
                        style = Typography.bodyMedium.copy(fontSize = 11.sp),
                        color = if (gain != 0f) AccentCyan else TextTertiary,
                        modifier = Modifier
                            .width(56.dp)
                            .padding(start = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Export / Import Preset JSON Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        exportJsonText = equalizerEngine.exportCurrentPresetJson()
                        showExportDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan)
                ) {
                    Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export JSON")
                }

                OutlinedButton(
                    onClick = {
                        jsonInputText = ""
                        showImportDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPink)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import JSON")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated)
            ) {
                Text("Done", color = TextPrimary)
            }
        }
    }

    // Export Preset Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export EQ Preset JSON", color = TextPrimary) },
            text = {
                Column {
                    Text("Copy this JSON preset payload:", style = Typography.bodyMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportJsonText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close", color = AccentCyan)
                }
            },
            containerColor = SurfaceCard
        )
    }

    // Import Preset Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import EQ Preset JSON", color = TextPrimary) },
            text = {
                Column {
                    Text("Paste your JSON preset here:", style = Typography.bodyMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jsonInputText,
                        onValueChange = { jsonInputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("{\"name\":\"Custom\",\"gains\":[...]}", color = TextTertiary) },
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (equalizerEngine.importPresetJson(jsonInputText)) {
                            showImportDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPink)
                ) {
                    Text("Import & Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard
        )
    }
}
