package cn.lemondrop.fhreborn.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "fh_app_settings")

class AppSettingsRepository(private val context: Context) {

    private val dataStore = context.appSettingsDataStore

    // ========== 语言 ==========
    val language: Flow<String> = dataStore.data.map { it[stringPreferencesKey("language")] ?: "system" }
    suspend fun setLanguage(value: String) = dataStore.edit { it[stringPreferencesKey("language")] = value }

    // ========== 个性化 ==========
    val themeMode: Flow<String> = dataStore.data.map { it[stringPreferencesKey("theme_mode")] ?: "system" }
    suspend fun setThemeMode(value: String) = dataStore.edit { it[stringPreferencesKey("theme_mode")] = value }

    val useDynamicColor: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("dynamic_color")] ?: false }
    suspend fun setUseDynamicColor(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("dynamic_color")] = value }

    // 大屏侧边栏展开状态（跨启动记住）
    val drawerExpanded: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("drawer_expanded")] ?: true }
    suspend fun setDrawerExpanded(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("drawer_expanded")] = value }

    /** 歌单页视图样式：list / grid / card / square */
    val playlistViewStyle: Flow<String> = dataStore.data.map { it[stringPreferencesKey("playlist_view_style")] ?: "list" }
    suspend fun setPlaylistViewStyle(value: String) = dataStore.edit { it[stringPreferencesKey("playlist_view_style")] = value }

    val accentColor: Flow<String> = dataStore.data.map { it[stringPreferencesKey("accent_color")] ?: "default" }
    suspend fun setAccentColor(value: String) = dataStore.edit { it[stringPreferencesKey("accent_color")] = value }

    // ========== 功能 ==========
    val sleepTimer: Flow<Int> = dataStore.data.map { it[intPreferencesKey("sleep_timer")] ?: 0 }
    suspend fun setSleepTimer(value: Int) = dataStore.edit { it[intPreferencesKey("sleep_timer")] = value }

    val autoPlayOnLaunch: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("auto_play_launch")] ?: false }
    suspend fun setAutoPlayOnLaunch(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("auto_play_launch")] = value }

    val skipSilence: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("skip_silence")] ?: false }
    suspend fun setSkipSilence(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("skip_silence")] = value }

    val wakeLock: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("wake_lock")] ?: true }
    suspend fun setWakeLock(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("wake_lock")] = value }

    // ========== 输出 ==========
    val audioOutputDevice: Flow<String> = dataStore.data.map { it[stringPreferencesKey("audio_output")] ?: "auto" }
    suspend fun setAudioOutputDevice(value: String) = dataStore.edit { it[stringPreferencesKey("audio_output")] = value }

    val outputSampleRate: Flow<String> = dataStore.data.map { it[stringPreferencesKey("sample_rate")] ?: "auto" }
    suspend fun setOutputSampleRate(value: String) = dataStore.edit { it[stringPreferencesKey("sample_rate")] = value }

    val eqPreset: Flow<String> = dataStore.data.map { it[stringPreferencesKey("eq_preset")] ?: "off" }
    suspend fun setEqPreset(value: String) = dataStore.edit { it[stringPreferencesKey("eq_preset")] = value }

    // ========== 歌词 ==========
    val lyricSourcePriority: Flow<String> = dataStore.data.map { it[stringPreferencesKey("lyric_priority")] ?: "embedded" }
    suspend fun setLyricSourcePriority(value: String) = dataStore.edit { it[stringPreferencesKey("lyric_priority")] = value }

    val showTranslation: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("lyric_translation")] ?: true }
    suspend fun setShowTranslation(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("lyric_translation")] = value }

    val showRomaji: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("lyric_romaji")] ?: false }
    suspend fun setShowRomaji(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("lyric_romaji")] = value }

    val desktopLyric: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("desktop_lyric")] ?: false }
    suspend fun setDesktopLyric(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("desktop_lyric")] = value }

    val statusBarLyric: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("statusbar_lyric")] ?: false }
    suspend fun setStatusBarLyric(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("statusbar_lyric")] = value }

    // Accompanist Lyric 设置
    val acclLyricMainTextSizeSp: Flow<Int> = dataStore.data.map { it[intPreferencesKey("accl_lyric_main_text_size")] ?: 34 }
    suspend fun setAcclLyricMainTextSizeSp(value: Int) = dataStore.edit { it[intPreferencesKey("accl_lyric_main_text_size")] = value }

    val acclLyricAccompanimentTextSizeSp: Flow<Int> = dataStore.data.map { it[intPreferencesKey("accl_lyric_accompaniment_text_size")] ?: 20 }
    suspend fun setAcclLyricAccompanimentTextSizeSp(value: Int) = dataStore.edit { it[intPreferencesKey("accl_lyric_accompaniment_text_size")] = value }

    val acclLyricPhoneticTextSizeSp: Flow<Int> = dataStore.data.map { it[intPreferencesKey("accl_lyric_phonetic_text_size")] ?: 13 }
    suspend fun setAcclLyricPhoneticTextSizeSp(value: Int) = dataStore.edit { it[intPreferencesKey("accl_lyric_phonetic_text_size")] = value }

    val acclLyricMainFontWeight: Flow<Int> = dataStore.data.map { it[intPreferencesKey("accl_lyric_main_font_weight")] ?: 700 }
    suspend fun setAcclLyricMainFontWeight(value: Int) = dataStore.edit { it[intPreferencesKey("accl_lyric_main_font_weight")] = value }

    val acclLyricAccompanimentFontWeight: Flow<Int> = dataStore.data.map { it[intPreferencesKey("accl_lyric_accompaniment_font_weight")] ?: 700 }
    suspend fun setAcclLyricAccompanimentFontWeight(value: Int) = dataStore.edit { it[intPreferencesKey("accl_lyric_accompaniment_font_weight")] = value }

    val acclLyricPhoneticFontWeight: Flow<Int> = dataStore.data.map { it[intPreferencesKey("accl_lyric_phonetic_font_weight")] ?: 400 }
    suspend fun setAcclLyricPhoneticFontWeight(value: Int) = dataStore.edit { it[intPreferencesKey("accl_lyric_phonetic_font_weight")] = value }

    val acclLyricShowTranslation: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("accl_lyric_show_translation")] ?: true }
    suspend fun setAcclLyricShowTranslation(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("accl_lyric_show_translation")] = value }

    val acclLyricShowPhonetic: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("accl_lyric_show_phonetic")] ?: true }
    suspend fun setAcclLyricShowPhonetic(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("accl_lyric_show_phonetic")] = value }

    val acclLyricUseBlurEffect: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("accl_lyric_use_blur")] ?: true }
    suspend fun setAcclLyricUseBlurEffect(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("accl_lyric_use_blur")] = value }

    val acclLyricBlurDelta: Flow<Int> = dataStore.data.map { it[intPreferencesKey("accl_lyric_blur_delta")] ?: 3 }
    suspend fun setAcclLyricBlurDelta(value: Int) = dataStore.edit { it[intPreferencesKey("accl_lyric_blur_delta")] = value }

    val acclLyricTextAlign: Flow<String> = dataStore.data.map { it[stringPreferencesKey("accl_lyric_text_align")] ?: "center" }
    suspend fun setAcclLyricTextAlign(value: String) = dataStore.edit { it[stringPreferencesKey("accl_lyric_text_align")] = value }

    val acclLyricGlowEffect: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("accl_lyric_glow")] ?: true }
    suspend fun setAcclLyricGlowEffect(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("accl_lyric_glow")] = value }

    val acclLyricBreathingDotsSize: Flow<Int> = dataStore.data.map { it[intPreferencesKey("accl_lyric_dots_size")] ?: 16 }
    suspend fun setAcclLyricBreathingDotsSize(value: Int) = dataStore.edit { it[intPreferencesKey("accl_lyric_dots_size")] = value }

    val acclLyricTranslationTextSizeSp: Flow<Int> = dataStore.data.map { it[intPreferencesKey("accl_lyric_translation_size")] ?: 14 }
    suspend fun setAcclLyricTranslationTextSizeSp(value: Int) = dataStore.edit { it[intPreferencesKey("accl_lyric_translation_size")] = value }

    val acclLyricTranslationFontWeight: Flow<Int> = dataStore.data.map { it[intPreferencesKey("accl_lyric_translation_font_weight")] ?: 400 }
    suspend fun setAcclLyricTranslationFontWeight(value: Int) = dataStore.edit { it[intPreferencesKey("accl_lyric_translation_font_weight")] = value }

    /** 当前行歌词在视口中的竖向位置（百分比，行中心对齐） */
    val acclLyricLinePositionPercent: Flow<Int> = dataStore.data.map { it[intPreferencesKey("accl_lyric_line_position_percent")] ?: 35 }
    suspend fun setAcclLyricLinePositionPercent(value: Int) = dataStore.edit { it[intPreferencesKey("accl_lyric_line_position_percent")] = value }

    // ========== 播放器封面 ==========
    /** 播放器封面圆角（dp） */
    val playerCoverCornerRadius: Flow<Int> = dataStore.data.map { it[intPreferencesKey("player_cover_corner_radius")] ?: 12 }
    suspend fun setPlayerCoverCornerRadius(value: Int) = dataStore.edit { it[intPreferencesKey("player_cover_corner_radius")] = value }

    /** 圆形旋转封面（启用后非正方形封面裁切为方形显示） */
    val playerCoverRotating: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("player_cover_rotating")] ?: false }
    suspend fun setPlayerCoverRotating(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("player_cover_rotating")] = value }

    // ========== 媒体库 ==========
    val autoScanOnLaunch: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("auto_scan")] ?: true }
    suspend fun setAutoScanOnLaunch(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("auto_scan")] = value }

    val coverCacheStrategy: Flow<String> = dataStore.data.map { it[stringPreferencesKey("cover_cache")] ?: "disk" }
    suspend fun setCoverCacheStrategy(value: String) = dataStore.edit { it[stringPreferencesKey("cover_cache")] = value }

    // ========== 主界面 ==========
    val hideSystemUi: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("hide_system_ui")] ?: false }
    suspend fun setHideSystemUi(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("hide_system_ui")] = value }

    // ========== 主页面背景 ==========
    val bgType: Flow<String> = dataStore.data.map { it[stringPreferencesKey("bg_type")] ?: "color" }
    suspend fun setBgType(value: String) = dataStore.edit { it[stringPreferencesKey("bg_type")] = value }

    val bgColor: Flow<String> = dataStore.data.map { it[stringPreferencesKey("bg_color")] ?: "" }
    suspend fun setBgColor(value: String) = dataStore.edit { it[stringPreferencesKey("bg_color")] = value }

    val bgImagePath: Flow<String> = dataStore.data.map { it[stringPreferencesKey("bg_image_path")] ?: "" }
    suspend fun setBgImagePath(value: String) = dataStore.edit { it[stringPreferencesKey("bg_image_path")] = value }

    val bgImageBrightness: Flow<Int> = dataStore.data.map { it[intPreferencesKey("bg_image_brightness")] ?: 100 }
    suspend fun setBgImageBrightness(value: Int) = dataStore.edit { it[intPreferencesKey("bg_image_brightness")] = value }

    val bgImageBlur: Flow<Int> = dataStore.data.map { it[intPreferencesKey("bg_image_blur")] ?: 0 }
    suspend fun setBgImageBlur(value: Int) = dataStore.edit { it[intPreferencesKey("bg_image_blur")] = value }

    // ========== 无障碍 ==========
    val largeText: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("large_text")] ?: false }
    suspend fun setLargeText(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("large_text")] = value }

    val highContrast: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("high_contrast")] ?: false }
    suspend fun setHighContrast(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("high_contrast")] = value }

    val reduceMotion: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("reduce_motion")] ?: false }
    suspend fun setReduceMotion(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("reduce_motion")] = value }

    // ========== 数据管理 ==========
    val autoBackup: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("auto_backup")] ?: false }
    suspend fun setAutoBackup(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("auto_backup")] = value }

    // ========== 实验性选项 ==========
    val experimentalFluidBg: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("exp_fluid_bg")] ?: false }
    suspend fun setExperimentalFluidBg(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("exp_fluid_bg")] = value }

    val experimentalReveal: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("exp_reveal")] ?: false }
    suspend fun setExperimentalReveal(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("exp_reveal")] = value }

    // ========== 开发者选项 ==========
    val crashReporting: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("crash_report")] ?: true }
    suspend fun setCrashReporting(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("crash_report")] = value }

    val debugMode: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("debug_mode")] ?: false }
    suspend fun setDebugMode(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("debug_mode")] = value }

    // ========== 通用布尔读取/写入 ==========
    fun getBoolean(key: String, default: Boolean = false): Flow<Boolean> =
        dataStore.data.map { it[booleanPreferencesKey(key)] ?: default }

    suspend fun setBoolean(key: String, value: Boolean) =
        dataStore.edit { it[booleanPreferencesKey(key)] = value }

    fun getString(key: String, default: String = ""): Flow<String> =
        dataStore.data.map { it[stringPreferencesKey(key)] ?: default }

    suspend fun setString(key: String, value: String) =
        dataStore.edit { it[stringPreferencesKey(key)] = value }

    fun getInt(key: String, default: Int = 0): Flow<Int> =
        dataStore.data.map { it[intPreferencesKey(key)] ?: default }

    suspend fun setInt(key: String, value: Int) =
        dataStore.edit { it[intPreferencesKey(key)] = value }
}
