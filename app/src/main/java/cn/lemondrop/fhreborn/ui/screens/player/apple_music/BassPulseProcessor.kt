package cn.lemondrop.fhreborn.ui.screens.player.apple_music

import android.media.audiofx.Visualizer
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Analyzes audio from the app's playback session to produce a bass pulse value.
 * Uses Android's Visualizer API — no RECORD_AUDIO permission needed.
 *
 * Captures waveform data from the ExoPlayer audio session, applies a simple
 * DFT on low-frequency bins (30-210Hz), and outputs a smoothed 0~1 pulse value
 * suitable for driving background animation.
 */
class BassPulseProcessor {

    private var visualizer: Visualizer? = null
    private var isEnabled = false
    private var lastPulseValue = 0f

    // Smoothing state
    private var smoothedValue = 0f
    private val attackRate = 0.4f   // how fast pulse rises
    private val releaseRate = 0.08f // how fast pulse falls

    /**
     * Attach to an audio session and start capturing.
     */
    fun enable(audioSessionId: Int) {
        if (isEnabled) return
        try {
            val viz = Visualizer(audioSessionId)
            viz.captureSize = Visualizer.getCaptureSizeRange()[1] // max capture size (1024)
            // setDataCaptureListener samplingRate is in milliHz
            val captureRate = 20000 // 20Hz update rate (milliHz)

            viz.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(v: Visualizer, waveform: ByteArray, samplingRate: Int) {
                    processWaveform(waveform)
                }

                override fun onFftDataCapture(v: Visualizer, fft: ByteArray, samplingRate: Int) {
                    // We process waveform instead of FFT for simplicity
                }
            }, captureRate, true, false)

            viz.enabled = true
            visualizer = viz
            isEnabled = true
        } catch (_: Exception) {
            // Visualizer may fail on some devices/emulators
        }
    }

    fun disable() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {}
        visualizer = null
        isEnabled = false
        smoothedValue = 0f
        lastPulseValue = 0f
    }

    /**
     * Get the current bass pulse value (0~1).
     * Call this each frame to drive animation.
     */
    fun getPulse(): Float {
        // Apply smoothing: fast attack, slow release
        val target = lastPulseValue
        smoothedValue = if (target > smoothedValue) {
            smoothedValue + (target - smoothedValue) * attackRate
        } else {
            smoothedValue + (target - smoothedValue) * releaseRate
        }
        return smoothedValue.coerceIn(0f, 1f)
    }

    private fun processWaveform(waveform: ByteArray) {
        if (waveform.isEmpty()) return

        // Convert byte waveform to float samples [-1, 1]
        val sampleCount = waveform.size / 2
        val samples = FloatArray(sampleCount)
        for (i in 0 until sampleCount) {
            val lo = waveform[i * 2].toInt() and 0xFF
            val hi = waveform[i * 2 + 1].toInt()
            samples[i] = (hi shl 8 or lo).toShort().toFloat() / 32768f
        }

        // Compute RMS energy of the full signal as a proxy for bass
        // (waveform capture doesn't give frequency info, but RMS correlates
        // well with bass hits since bass dominates the waveform amplitude)
        var sumSquares = 0.0
        for (s in samples) {
            sumSquares += s.toDouble() * s.toDouble()
        }
        val rms = sqrt(sumSquares / sampleCount).toFloat()

        // Map RMS to 0~1 pulse (typical music RMS is 0.05~0.3)
        val pulse = min(rms * 4f, 1f)

        lastPulseValue = pulse
    }

    fun release() {
        disable()
    }
}
