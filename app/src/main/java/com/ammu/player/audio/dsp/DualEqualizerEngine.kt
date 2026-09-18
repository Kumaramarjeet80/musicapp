package com.ammu.player.audio.dsp

import android.content.Context
import android.media.audiofx.Equalizer
import kotlin.math.pow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class EqualizerMode {
    NATIVE_VLC,
    VIVO_STUDIO_DSP
}

@Serializable
data class EqPreset(
    val name: String,
    val gains: List<Float> // 10 bands from -12.0f to +12.0f dB
)

data class EqualizerState(
    val isEnabled: Boolean = false,
    val mode: EqualizerMode = EqualizerMode.NATIVE_VLC,
    val bandFrequencies: List<Int> = listOf(60, 150, 400, 1000, 2400, 4000, 7000, 10000, 13000, 16000), // Hz
    val bandGains: List<Float> = List(10) { 0f }, // dB (-12f to +12f)
    val autoGainReductionEnabled: Boolean = true,
    val autoGainFactor: Float = 0.80f, // 20% volume attenuation when EQ active
    val currentPresetName: String = "Flat"
)

class DualEqualizerEngine(
    private val context: Context,
    private val onAutoGainChanged: (Float) -> Unit
) {
    private var nativeEqualizer: Equalizer? = null
    private var audioSessionId: Int = 0

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    private val biquadFilters = List(10) { index ->
        val freq = _state.value.bandFrequencies[index]
        BiquadFilter(centerFreq = freq.toDouble())
    }

    val defaultPresets = listOf(
        EqPreset("Flat", List(10) { 0f }),
        EqPreset("Bass Boost", listOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f)),
        EqPreset("Vocal Booster", listOf(-2f, -1f, 1f, 3f, 4f, 4f, 3f, 1f, 0f, -1f)),
        EqPreset("Rock", listOf(4f, 3f, 2f, 0f, -1f, 0f, 2f, 3f, 4f, 4f)),
        EqPreset("Pop", listOf(-1f, 1f, 2f, 3f, 3f, 2f, 0f, -1f, 1f, 2f)),
        EqPreset("Jazz", listOf(3f, 2f, 1f, 2f, -1f, -1f, 0f, 1f, 2f, 3f)),
        EqPreset("Electronic", listOf(5f, 4f, 2f, 0f, -2f, 2f, 1f, 3f, 4f, 5f))
    )

    fun attachAudioSession(sessionId: Int) {
        if (sessionId == 0) return
        this.audioSessionId = sessionId
        try {
            nativeEqualizer?.release()
            nativeEqualizer = Equalizer(0, sessionId).apply {
                enabled = _state.value.isEnabled && _state.value.mode == EqualizerMode.NATIVE_VLC
            }
            applyGainsToNative()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        nativeEqualizer?.release()
        nativeEqualizer = null
    }

    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isEnabled = enabled)
        applyChanges()
    }

    fun setMode(mode: EqualizerMode) {
        _state.value = _state.value.copy(mode = mode)
        applyChanges()
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex !in 0 until 10) return
        val currentGains = _state.value.bandGains.toMutableList()
        currentGains[bandIndex] = gainDb.coerceIn(-12f, 12f)
        _state.value = _state.value.copy(
            bandGains = currentGains,
            currentPresetName = "Custom"
        )
        biquadFilters[bandIndex].gainDb = gainDb.toDouble()
        biquadFilters[bandIndex].recalculate()
        applyChanges()
    }

    fun applyPreset(preset: EqPreset) {
        val gains = preset.gains.take(10).toMutableList()
        while (gains.size < 10) gains.add(0f)
        _state.value = _state.value.copy(
            bandGains = gains,
            currentPresetName = preset.name
        )
        gains.forEachIndexed { i, g ->
            biquadFilters[i].gainDb = g.toDouble()
            biquadFilters[i].recalculate()
        }
        applyChanges()
    }

    fun toggleAutoGain(enable: Boolean) {
        _state.value = _state.value.copy(autoGainReductionEnabled = enable)
        applyChanges()
    }

    private fun applyChanges() {
        val s = _state.value
        // Apply auto-gain attenuation (20% reduction when EQ is on)
        val volumeFactor = if (s.isEnabled && s.autoGainReductionEnabled) s.autoGainFactor else 1.0f
        onAutoGainChanged(volumeFactor)

        if (s.mode == EqualizerMode.NATIVE_VLC) {
            nativeEqualizer?.enabled = s.isEnabled
            applyGainsToNative()
        } else {
            // Vivo Studio DSP mode
            nativeEqualizer?.enabled = false
        }
    }

    private fun applyGainsToNative() {
        val eq = nativeEqualizer ?: return
        if (!_state.value.isEnabled) return
        val bands = eq.numberOfBands.toInt()
        val range = eq.bandLevelRange // in millibels (e.g. -1500 to 1500)
        val minLevel = range[0]
        val maxLevel = range[1]

        for (i in 0 until bands) {
            val normalizedIdx = (i * 10 / bands).coerceIn(0, 9)
            val gainDb = _state.value.bandGains[normalizedIdx]
            val millibels = (gainDb * 100).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
            try {
                eq.setBandLevel(i.toShort(), millibels)
            } catch (e: Exception) {
                // Ignore device-specific out-of-range exceptions
            }
        }
    }

    /**
     * Calculates the composite frequency response curve in dB for rendering on a Bezier Spline Canvas.
     */
    fun calculateSplinePoints(samples: Int = 100): List<Pair<Float, Float>> {
        val points = mutableListOf<Pair<Float, Float>>()
        val minLogFreq = kotlin.math.log10(20.0)
        val maxLogFreq = kotlin.math.log10(20000.0)

        for (i in 0 until samples) {
            val fraction = i.toFloat() / (samples - 1)
            val logFreq = minLogFreq + fraction * (maxLogFreq - minLogFreq)
            val freq = 10.0.pow(logFreq)

            var totalDb = 0.0
            for (filter in biquadFilters) {
                totalDb += filter.getMagnitudeDbAt(freq)
            }
            // clamp for visual canvas display (-12dB to +12dB)
            val clampedDb = totalDb.toFloat().coerceIn(-12f, 12f)
            points.add(Pair(fraction, clampedDb))
        }
        return points
    }

    /**
     * Serializes current preset configuration to JSON.
     */
    fun exportCurrentPresetJson(): String {
        val preset = EqPreset(_state.value.currentPresetName, _state.value.bandGains)
        return Json.encodeToString(preset)
    }

    /**
     * Imports preset from JSON.
     */
    fun importPresetJson(jsonStr: String): Boolean {
        return try {
            val preset = Json.decodeFromString<EqPreset>(jsonStr)
            applyPreset(preset)
            true
        } catch (e: Exception) {
            false
        }
    }
}
