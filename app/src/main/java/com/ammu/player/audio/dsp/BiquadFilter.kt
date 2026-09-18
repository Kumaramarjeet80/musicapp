package com.ammu.player.audio.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Direct Form 1 / 2 Biquad Filter implementation based on Robert Bristow-Johnson's Audio EQ Cookbook.
 * Used for Vivo Studio Mode software DSP filtering and response curve calculation.
 */
class BiquadFilter(
    var sampleRate: Double = 44100.0,
    var centerFreq: Double = 1000.0,
    var qFactor: Double = 1.0,
    var gainDb: Double = 0.0
) {
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a0 = 1.0
    private var a1 = 0.0
    private var a2 = 0.0

    init {
        recalculate()
    }

    fun recalculate() {
        val a = 10.0.pow(gainDb / 40.0)
        val omega = 2.0 * PI * centerFreq / sampleRate
        val alpha = sin(omega) / (2.0 * qFactor)

        b0 = 1.0 + alpha * a
        b1 = -2.0 * cos(omega)
        b2 = 1.0 - alpha * a
        a0 = 1.0 + alpha / a
        a1 = -2.0 * cos(omega)
        a2 = 1.0 - alpha / a

        // Normalize
        b0 /= a0
        b1 /= a0
        b2 /= a0
        a1 /= a0
        a2 /= a0
        a0 = 1.0
    }

    /**
     * Calculates the magnitude response (in dB) of this filter at a given frequency.
     */
    fun getMagnitudeDbAt(freq: Double): Double {
        val omega = 2.0 * PI * freq / sampleRate
        val cosOmega = cos(omega)
        val sinOmega = sin(omega)
        val cos2Omega = cos(2.0 * omega)
        val sin2Omega = sin(2.0 * omega)

        val numReal = b0 + b1 * cosOmega + b2 * cos2Omega
        val numImag = -(b1 * sinOmega + b2 * sin2Omega)
        val denReal = 1.0 + a1 * cosOmega + a2 * cos2Omega
        val denImag = -(a1 * sinOmega + a2 * sin2Omega)

        val numMagSq = numReal * numReal + numImag * numImag
        val denMagSq = denReal * denReal + denImag * denImag

        val response = sqrt(numMagSq / denMagSq)
        return 20.0 * kotlin.math.log10(response.coerceAtLeast(1e-6))
    }
}
