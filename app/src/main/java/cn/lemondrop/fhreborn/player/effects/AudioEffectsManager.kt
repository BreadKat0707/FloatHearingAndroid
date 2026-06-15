package cn.lemondrop.fhreborn.player.effects

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log

/**
 * 音频效果器管理器
 *
 * 管理 Equalizer（均衡器）、BassBoost（低音增强）、Virtualizer（声场扩展）
 * 需要在 ExoPlayer 的 audioSessionId 可用后初始化
 */
class AudioEffectsManager(private val audioSessionId: Int) {

    companion object {
        private const val TAG = "AudioEffectsManager"

        // 均衡器预设名称映射
        val EQ_PRESETS = listOf(
            "关闭" to -1,
            "流行" to 0,
            "摇滚" to 1,
            "古典" to 2,
            "爵士" to 3,
            "电子" to 4,
            "人声" to 5,
            "舞曲" to 6,
            "轻柔" to 7,
            "重金属" to 8,
            "嘻哈" to 9,
            "自定义" to -2
        )
    }

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    // 均衡器频段中心频率（Hz），用于UI展示
    val bandFrequencies: List<Int>
    val bandLevelRange: Pair<Short, Short>

    init {
        try {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
            bassBoost = BassBoost(0, audioSessionId).apply { enabled = true }
            virtualizer = Virtualizer(0, audioSessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize audio effects", e)
        }

        // 获取均衡器信息
        val eq = equalizer
        bandFrequencies = if (eq != null) {
            (0 until eq.numberOfBands).map { eq.getCenterFreq(it.toShort()) / 1000 }
        } else emptyList()

        bandLevelRange = if (eq != null) {
            val range = eq.bandLevelRange
            Pair(range[0], range[1])
        } else Pair(0, 0)
    }

    // ===== Equalizer =====

    fun setEqualizerEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
    }

    fun isEqualizerEnabled(): Boolean = equalizer?.enabled == true

    fun getNumberOfBands(): Short = equalizer?.numberOfBands ?: 0

    fun getNumberOfPresets(): Short = equalizer?.numberOfPresets ?: 0

    fun getPresetName(preset: Short): String =
        equalizer?.getPresetName(preset) ?: "Preset $preset"

    fun usePreset(preset: Short) {
        equalizer?.usePreset(preset)
    }

    fun getCurrentPreset(): Short = equalizer?.currentPreset ?: 0

    fun setBandLevel(band: Short, level: Short) {
        equalizer?.setBandLevel(band, level)
    }

    fun getBandLevel(band: Short): Short =
        equalizer?.getBandLevel(band) ?: 0

    // ===== BassBoost =====

    fun setBassBoostEnabled(enabled: Boolean) {
        bassBoost?.enabled = enabled
    }

    fun isBassBoostEnabled(): Boolean = bassBoost?.enabled == true

    fun setBassBoostStrength(strength: Short) {
        bassBoost?.setStrength(strength.coerceIn(0, 1000).toShort())
    }

    fun getBassBoostStrength(): Short = bassBoost?.roundedStrength ?: 0

    // ===== Virtualizer =====

    fun setVirtualizerEnabled(enabled: Boolean) {
        virtualizer?.enabled = enabled
    }

    fun isVirtualizerEnabled(): Boolean = virtualizer?.enabled == true

    fun setVirtualizerStrength(strength: Short) {
        virtualizer?.setStrength(strength.coerceIn(0, 1000).toShort())
    }

    fun getVirtualizerStrength(): Short = virtualizer?.roundedStrength ?: 0

    // ===== Release =====

    fun release() {
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        equalizer = null
        bassBoost = null
        virtualizer = null
    }
}
