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

    // ========== 媒体库 ==========
    val autoScanOnLaunch: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("auto_scan")] ?: true }
    suspend fun setAutoScanOnLaunch(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("auto_scan")] = value }

    val coverCacheStrategy: Flow<String> = dataStore.data.map { it[stringPreferencesKey("cover_cache")] ?: "disk" }
    suspend fun setCoverCacheStrategy(value: String) = dataStore.edit { it[stringPreferencesKey("cover_cache")] = value }

    // ========== 主界面 ==========
    val hideSystemUi: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("hide_system_ui")] ?: false }
    suspend fun setHideSystemUi(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("hide_system_ui")] = value }

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
