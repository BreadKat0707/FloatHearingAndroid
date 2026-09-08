package cn.lemondrop.fhreborn.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
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

    /** 媒体库-专辑视图样式：list / grid / card / square */
    val albumViewStyle: Flow<String> = dataStore.data.map { it[stringPreferencesKey("album_view_style")] ?: "grid" }
    suspend fun setAlbumViewStyle(value: String) = dataStore.edit { it[stringPreferencesKey("album_view_style")] = value }

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

    /** 逐字歌词（卡拉OK逐字高亮）；关闭时降级为整行高亮 */
    val acclLyricWordLevel: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("accl_lyric_word_level")] ?: true }
    suspend fun setAcclLyricWordLevel(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("accl_lyric_word_level")] = value }

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

    /** 封面投影 Y 轴偏移（dp） */
    val playerCoverShadowY: Flow<Int> = dataStore.data.map { it[intPreferencesKey("player_cover_shadow_y")] ?: 16 }
    suspend fun setPlayerCoverShadowY(value: Int) = dataStore.edit { it[intPreferencesKey("player_cover_shadow_y")] = value }

    /** 封面投影颜色浓度（0-100%，控制阴影 alpha） */
    val playerCoverShadowAlpha: Flow<Int> = dataStore.data.map { it[intPreferencesKey("player_cover_shadow_alpha")] ?: 40 }
    suspend fun setPlayerCoverShadowAlpha(value: Int) = dataStore.edit { it[intPreferencesKey("player_cover_shadow_alpha")] = value }

    /** 封面投影模糊程度（dp） */
    val playerCoverShadowBlur: Flow<Int> = dataStore.data.map { it[intPreferencesKey("player_cover_shadow_blur")] ?: 20 }
    suspend fun setPlayerCoverShadowBlur(value: Int) = dataStore.edit { it[intPreferencesKey("player_cover_shadow_blur")] = value }

    /** 暂停时封面缩小百分比（50-100%，默认92%） */
    val playerCoverPauseScale: Flow<Int> = dataStore.data.map { it[intPreferencesKey("player_cover_pause_scale")] ?: 92 }
    suspend fun setPlayerCoverPauseScale(value: Int) = dataStore.edit { it[intPreferencesKey("player_cover_pause_scale")] = value }

    // ========== 播放器元素外观 — 浅色模式（10个元素各自独立） ==========
    // 1.拖拽手柄
    val playerEl01HandleAlpha: Flow<Int> = dataStore.data.map { it[intPreferencesKey("pe01_a")] ?: 100 }
    suspend fun setPlayerEl01HandleAlpha(v: Int) = dataStore.edit { it[intPreferencesKey("pe01_a")] = v }
    val playerEl01HandleBlend: Flow<String> = dataStore.data.map { it[stringPreferencesKey("pe01_b")] ?: "SrcOver" }
    suspend fun setPlayerEl01HandleBlend(v: String) = dataStore.edit { it[stringPreferencesKey("pe01_b")] = v }
    // 2.歌名
    val playerEl02TitleAlpha: Flow<Int> = dataStore.data.map { it[intPreferencesKey("pe02_a")] ?: 100 }
    suspend fun setPlayerEl02TitleAlpha(v: Int) = dataStore.edit { it[intPreferencesKey("pe02_a")] = v }
    val playerEl02TitleBlend: Flow<String> = dataStore.data.map { it[stringPreferencesKey("pe02_b")] ?: "SrcOver" }
    suspend fun setPlayerEl02TitleBlend(v: String) = dataStore.edit { it[stringPreferencesKey("pe02_b")] = v }
    // 3.专辑-艺术家
    val playerEl03ArtistAlpha: Flow<Int> = dataStore.data.map { it[intPreferencesKey("pe03_a")] ?: 100 }
    suspend fun setPlayerEl03ArtistAlpha(v: Int) = dataStore.edit { it[intPreferencesKey("pe03_a")] = v }
    val playerEl03ArtistBlend: Flow<String> = dataStore.data.map { it[stringPreferencesKey("pe03_b")] ?: "SrcOver" }
    suspend fun setPlayerEl03ArtistBlend(v: String) = dataStore.edit { it[stringPreferencesKey("pe03_b")] = v }
    // 4.小歌词
    val playerEl04MiniLyricAlpha: Flow<Int> = dataStore.data.map { it[intPreferencesKey("pe04_a")] ?: 100 }
    suspend fun setPlayerEl04MiniLyricAlpha(v: Int) = dataStore.edit { it[intPreferencesKey("pe04_a")] = v }
    val playerEl04MiniLyricBlend: Flow<String> = dataStore.data.map { it[stringPreferencesKey("pe04_b")] ?: "SrcOver" }
    suspend fun setPlayerEl04MiniLyricBlend(v: String) = dataStore.edit { it[stringPreferencesKey("pe04_b")] = v }
    // 5.进度条底轨
    val playerEl05TrackAlpha: Flow<Int> = dataStore.data.map { it[intPreferencesKey("pe05_a")] ?: 50 }
    suspend fun setPlayerEl05TrackAlpha(v: Int) = dataStore.edit { it[intPreferencesKey("pe05_a")] = v }
    val playerEl05TrackBlend: Flow<String> = dataStore.data.map { it[stringPreferencesKey("pe05_b")] ?: "SrcOver" }
    suspend fun setPlayerEl05TrackBlend(v: String) = dataStore.edit { it[stringPreferencesKey("pe05_b")] = v }
    // 6.进度条进度
    val playerEl06FillAlpha: Flow<Int> = dataStore.data.map { it[intPreferencesKey("pe06_a")] ?: 100 }
    suspend fun setPlayerEl06FillAlpha(v: Int) = dataStore.edit { it[intPreferencesKey("pe06_a")] = v }
    val playerEl06FillBlend: Flow<String> = dataStore.data.map { it[stringPreferencesKey("pe06_b")] ?: "SrcOver" }
    suspend fun setPlayerEl06FillBlend(v: String) = dataStore.edit { it[stringPreferencesKey("pe06_b")] = v }
    // 7.进度条时间文字
    val playerEl07TimeAlpha: Flow<Int> = dataStore.data.map { it[intPreferencesKey("pe07_a")] ?: 100 }
    suspend fun setPlayerEl07TimeAlpha(v: Int) = dataStore.edit { it[intPreferencesKey("pe07_a")] = v }
    val playerEl07TimeBlend: Flow<String> = dataStore.data.map { it[stringPreferencesKey("pe07_b")] ?: "SrcOver" }
    suspend fun setPlayerEl07TimeBlend(v: String) = dataStore.edit { it[stringPreferencesKey("pe07_b")] = v }
    // 8.播控图标
    val playerEl08ControlsAlpha: Flow<Int> = dataStore.data.map { it[intPreferencesKey("pe08_a")] ?: 100 }
    suspend fun setPlayerEl08ControlsAlpha(v: Int) = dataStore.edit { it[intPreferencesKey("pe08_a")] = v }
    val playerEl08ControlsBlend: Flow<String> = dataStore.data.map { it[stringPreferencesKey("pe08_b")] ?: "SrcOver" }
    suspend fun setPlayerEl08ControlsBlend(v: String) = dataStore.edit { it[stringPreferencesKey("pe08_b")] = v }
    // 9.播放模式（随机/循环）
    val playerEl09ModeAlpha: Flow<Int> = dataStore.data.map { it[intPreferencesKey("pe09_a")] ?: 100 }
    suspend fun setPlayerEl09ModeAlpha(v: Int) = dataStore.edit { it[intPreferencesKey("pe09_a")] = v }
    val playerEl09ModeBlend: Flow<String> = dataStore.data.map { it[stringPreferencesKey("pe09_b")] ?: "SrcOver" }
    suspend fun setPlayerEl09ModeBlend(v: String) = dataStore.edit { it[stringPreferencesKey("pe09_b")] = v }
    // 10.底部图标
    val playerEl10BottomAlpha: Flow<Int> = dataStore.data.map { it[intPreferencesKey("pe10_a")] ?: 100 }
    suspend fun setPlayerEl10BottomAlpha(v: Int) = dataStore.edit { it[intPreferencesKey("pe10_a")] = v }
    val playerEl10BottomBlend: Flow<String> = dataStore.data.map { it[stringPreferencesKey("pe10_b")] ?: "SrcOver" }
    suspend fun setPlayerEl10BottomBlend(v: String) = dataStore.edit { it[stringPreferencesKey("pe10_b")] = v }

    // ========== 播放器元素外观 — 深色模式（10个元素各自独立） ==========
    val playerEl01HandleAlphaDark: Flow<Int> = dataStore.data.map { it[intPreferencesKey("pe01_a_d")] ?: 100 }
    suspend fun setPlayerEl01HandleAlphaDark(v: Int) = dataStore.edit { it[intPreferencesKey("pe01_a_d")] = v }
    val playerEl01HandleBlendDark: Flow<String> = dataStore.data.map { it[stringPreferencesKey("pe01_b_d")] ?: "SrcOver" }
    suspend fun setPlayerEl01HandleBlendDark(v: String) = dataStore.edit { it[stringPreferencesKey("pe01_b_d")] = v }
    val playerEl02TitleAlphaDark: Flow<Int> = dataStore.data.map { it[intPreferencesKey("pe02_a_d")] ?: 100 }
    suspend fun setPlayerEl02TitleAlphaDark(v: Int) = dataStore.edit { it[intPreferencesKey("pe02_a_d")] = v }
    val playerEl02TitleBlendDark: Flow<String> = dataStore.data.map { it[stringPreferencesKey("pe02_b_d")] ?: "SrcOver" }
    suspend fun setPlayerEl02TitleBlendDark(v: String) = dataStore.edit { it[stringPreferencesKey("pe02_b_d")] = v }
    val playerEl03ArtistAlphaDark: Flow<Int> = dataStore.data.map { it[intPreferencesKey("pe03_a_d")] ?: 100 }
    suspend fun setPlayerEl03ArtistAlphaDark(v: Int) = dataStore.edit { it[intPreferencesKey("pe03_a_d")] = v }
    val playerEl03ArtistBlendDark: Flow<String> = dataStore.data.map { it[stringPreferencesKey("pe03_b_d")] ?: "SrcOver" }
    suspend fun setPlayerEl03ArtistBlendDark(v: String) = dataStore.edit { it[stringPreferencesKey("pe03_b_d")] = v }
    val playerEl04MiniLyricAlphaDark: Flow<Int> = dataStore.data.map { it[intPreferencesKey("pe04_a_d")] ?: 100 }
    suspend fun setPlayerEl04MiniLyricAlphaDark(v: Int) = dataStore.edit { it[intPreferencesKey("pe04_a_d")] = v }
    val playerEl04MiniLyricBlendDark: Flow<String> = dataStore.data.map { it[stringPreferencesKey("pe04_b_d")] ?: "SrcOver" }
    suspend fun setPlayerEl04MiniLyricBlendDark(v: String) = dataStore.edit { it[stringPreferencesKey("pe04_b_d")] = v }
    val playerEl05TrackAlphaDark: Flow<Int> = dataStore.data.map { it[intPreferencesKey("pe05_a_d")] ?: 50 }
    suspend fun setPlayerEl05TrackAlphaDark(v: Int) = dataStore.edit { it[intPreferencesKey("pe05_a_d")] = v }
    val playerEl05TrackBlendDark: Flow<String> = dataStore.data.map { it[stringPreferencesKey("pe05_b_d")] ?: "SrcOver" }
    suspend fun setPlayerEl05TrackBlendDark(v: String) = dataStore.edit { it[stringPreferencesKey("pe05_b_d")] = v }
    val playerEl06FillAlphaDark: Flow<Int> = dataStore.data.map { it[intPreferencesKey("pe06_a_d")] ?: 100 }
    suspend fun setPlayerEl06FillAlphaDark(v: Int) = dataStore.edit { it[intPreferencesKey("pe06_a_d")] = v }
    val playerEl06FillBlendDark: Flow<String> = dataStore.data.map { it[stringPreferencesKey("pe06_b_d")] ?: "SrcOver" }
    suspend fun setPlayerEl06FillBlendDark(v: String) = dataStore.edit { it[stringPreferencesKey("pe06_b_d")] = v }
    val playerEl07TimeAlphaDark: Flow<Int> = dataStore.data.map { it[intPreferencesKey("pe07_a_d")] ?: 100 }
    suspend fun setPlayerEl07TimeAlphaDark(v: Int) = dataStore.edit { it[intPreferencesKey("pe07_a_d")] = v }
    val playerEl07TimeBlendDark: Flow<String> = dataStore.data.map { it[stringPreferencesKey("pe07_b_d")] ?: "SrcOver" }
    suspend fun setPlayerEl07TimeBlendDark(v: String) = dataStore.edit { it[stringPreferencesKey("pe07_b_d")] = v }
    val playerEl08ControlsAlphaDark: Flow<Int> = dataStore.data.map { it[intPreferencesKey("pe08_a_d")] ?: 100 }
    suspend fun setPlayerEl08ControlsAlphaDark(v: Int) = dataStore.edit { it[intPreferencesKey("pe08_a_d")] = v }
    val playerEl08ControlsBlendDark: Flow<String> = dataStore.data.map { it[stringPreferencesKey("pe08_b_d")] ?: "SrcOver" }
    suspend fun setPlayerEl08ControlsBlendDark(v: String) = dataStore.edit { it[stringPreferencesKey("pe08_b_d")] = v }
    val playerEl09ModeAlphaDark: Flow<Int> = dataStore.data.map { it[intPreferencesKey("pe09_a_d")] ?: 100 }
    suspend fun setPlayerEl09ModeAlphaDark(v: Int) = dataStore.edit { it[intPreferencesKey("pe09_a_d")] = v }
    val playerEl09ModeBlendDark: Flow<String> = dataStore.data.map { it[stringPreferencesKey("pe09_b_d")] ?: "SrcOver" }
    suspend fun setPlayerEl09ModeBlendDark(v: String) = dataStore.edit { it[stringPreferencesKey("pe09_b_d")] = v }
    val playerEl10BottomAlphaDark: Flow<Int> = dataStore.data.map { it[intPreferencesKey("pe10_a_d")] ?: 100 }
    suspend fun setPlayerEl10BottomAlphaDark(v: Int) = dataStore.edit { it[intPreferencesKey("pe10_a_d")] = v }
    val playerEl10BottomBlendDark: Flow<String> = dataStore.data.map { it[stringPreferencesKey("pe10_b_d")] ?: "SrcOver" }
    suspend fun setPlayerEl10BottomBlendDark(v: String) = dataStore.edit { it[stringPreferencesKey("pe10_b_d")] = v }

    // ========== 媒体库 ==========
    val scanSourceMode: Flow<String> = dataStore.data.map {
        it[stringPreferencesKey("source_mode")] ?: "media_store"
    }
    suspend fun setScanSourceMode(value: String) =
        dataStore.edit { it[stringPreferencesKey("source_mode")] = value }

    val minDurationEnabled: Flow<Boolean> = dataStore.data.map {
        it[booleanPreferencesKey("min_duration_enabled")] ?: false
    }
    suspend fun setMinDurationEnabled(value: Boolean) =
        dataStore.edit { it[booleanPreferencesKey("min_duration_enabled")] = value }

    val minDurationSeconds: Flow<Int> = dataStore.data.map {
        it[intPreferencesKey("min_duration_seconds")]?.coerceIn(10, 60) ?: 30
    }
    suspend fun setMinDurationSeconds(value: Int) {
        val clamped = value.coerceIn(10, 60)
        dataStore.edit { it[intPreferencesKey("min_duration_seconds")] = clamped }
    }

    val autoScanOnLaunch: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("auto_scan")] ?: true }
    suspend fun setAutoScanOnLaunch(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("auto_scan")] = value }

    val coverCacheStrategy: Flow<String> = dataStore.data.map { it[stringPreferencesKey("cover_cache")] ?: "disk" }
    suspend fun setCoverCacheStrategy(value: String) = dataStore.edit { it[stringPreferencesKey("cover_cache")] = value }

    // ========== 主界面 ==========
    val titleBarStyle: Flow<String> = dataStore.data.map {
        it[stringPreferencesKey("title_bar_style")] ?: "gaussian"
    }
    suspend fun setTitleBarStyle(value: String) =
        dataStore.edit { it[stringPreferencesKey("title_bar_style")] = value }

    val titleBarBlurRadius: Flow<Int> = dataStore.data.map {
        it[intPreferencesKey("title_bar_blur_radius")]?.coerceIn(0, 150) ?: 10
    }
    suspend fun setTitleBarBlurRadius(value: Int) {
        dataStore.edit {
            it[intPreferencesKey("title_bar_blur_radius")] = value.coerceIn(0, 150)
        }
    }

    val titleBarProgressiveDirection: Flow<String> = dataStore.data.map {
        it[stringPreferencesKey("title_bar_progressive_direction")] ?: "top"
    }
    suspend fun setTitleBarProgressiveDirection(value: String) =
        dataStore.edit {
            it[stringPreferencesKey("title_bar_progressive_direction")] = value
        }

    val titleBarProgressiveStart: Flow<Int> = dataStore.data.map {
        it[intPreferencesKey("title_bar_progressive_start")]?.coerceIn(0, 100) ?: 0
    }
    suspend fun setTitleBarProgressiveStart(value: Int) =
        dataStore.edit {
            it[intPreferencesKey("title_bar_progressive_start")] = value.coerceIn(0, 100)
        }

    val titleBarProgressiveEnd: Flow<Int> = dataStore.data.map {
        it[intPreferencesKey("title_bar_progressive_end")]?.coerceIn(0, 100) ?: 100
    }
    suspend fun setTitleBarProgressiveEnd(value: Int) =
        dataStore.edit {
            it[intPreferencesKey("title_bar_progressive_end")] = value.coerceIn(0, 100)
        }

    val titleBarProgressiveCurve: Flow<Int> = dataStore.data.map {
        it[intPreferencesKey("title_bar_progressive_curve")]?.coerceIn(20, 300) ?: 220
    }
    suspend fun setTitleBarProgressiveCurve(value: Int) =
        dataStore.edit {
            it[intPreferencesKey("title_bar_progressive_curve")] = value.coerceIn(20, 300)
        }

    val hideSystemUi: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("hide_system_ui")] ?: false }
    suspend fun setHideSystemUi(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("hide_system_ui")] = value }

    // ========== 主页面背景 ==========
    val bgType: Flow<String> = dataStore.data.map { it[stringPreferencesKey("bg_type")] ?: "color" }
    suspend fun setBgType(value: String) = dataStore.edit { it[stringPreferencesKey("bg_type")] = value }

    /** 背景前景色：auto=跟随颜色模式，light=浅色前景，dark=深色前景 */
    val bgForeground: Flow<String> = dataStore.data.map { it[stringPreferencesKey("bg_foreground")] ?: "auto" }
    suspend fun setBgForeground(value: String) = dataStore.edit { it[stringPreferencesKey("bg_foreground")] = value }

    val bgColor: Flow<String> = dataStore.data.map { it[stringPreferencesKey("bg_color")] ?: "" }
    suspend fun setBgColor(value: String) = dataStore.edit { it[stringPreferencesKey("bg_color")] = value }

    val bgImagePath: Flow<String> = dataStore.data.map { it[stringPreferencesKey("bg_image_path")] ?: "" }
    suspend fun setBgImagePath(value: String) = dataStore.edit { it[stringPreferencesKey("bg_image_path")] = value }

    val bgImageBrightness: Flow<Int> = dataStore.data.map { it[intPreferencesKey("bg_image_brightness")] ?: 100 }
    suspend fun setBgImageBrightness(value: Int) = dataStore.edit { it[intPreferencesKey("bg_image_brightness")] = value }

    val bgImageBlur: Flow<Int> = dataStore.data.map { it[intPreferencesKey("bg_image_blur")] ?: 0 }
    suspend fun setBgImageBlur(value: Int) = dataStore.edit { it[intPreferencesKey("bg_image_blur")] = value }

    // ========== 数据管理 ==========
    /** 听歌统计与数据分析总开关：关闭后不再记录播放统计 */
    val statsEnabled: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("stats_enabled")] ?: true }
    suspend fun setStatsEnabled(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("stats_enabled")] = value }

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

    fun getDouble(key: String, default: Double = 0.0): Flow<Double> =
        dataStore.data.map { it[doublePreferencesKey(key)] ?: default }

    suspend fun setDouble(key: String, value: Double) =
        dataStore.edit { it[doublePreferencesKey(key)] = value }

    // ========== Apple Music 背景 ==========
    /** 模糊强度 (dp) */
    val appleMusicBlurDp: Flow<Int> = dataStore.data.map { it[intPreferencesKey("apple_music_blur_dp")] ?: 40 }
    suspend fun setAppleMusicBlurDp(value: Int) = dataStore.edit { it[intPreferencesKey("apple_music_blur_dp")] = value }

    /** 暗色遮罩强度 (%) */
    val appleMusicScrimPct: Flow<Int> = dataStore.data.map { it[intPreferencesKey("apple_music_scrim_pct")] ?: 30 }
    suspend fun setAppleMusicScrimPct(value: Int) = dataStore.edit { it[intPreferencesKey("apple_music_scrim_pct")] = value }

    /** 旋转速度倍率 */
    val appleMusicSpeed: Flow<Double> = dataStore.data.map { it[doublePreferencesKey("apple_music_speed")] ?: 1.0 }
    suspend fun setAppleMusicSpeed(value: Double) = dataStore.edit { it[doublePreferencesKey("apple_music_speed")] = value }

    /** 交叉淡入淡出时间 (ms) */
    val appleMusicCrossfadeMs: Flow<Int> = dataStore.data.map { it[intPreferencesKey("apple_music_crossfade_ms")] ?: 600 }
    suspend fun setAppleMusicCrossfadeMs(value: Int) = dataStore.edit { it[intPreferencesKey("apple_music_crossfade_ms")] = value }

    /** 色彩饱和度倍率 */
    val appleMusicSaturation: Flow<Double> = dataStore.data.map { it[doublePreferencesKey("apple_music_saturation")] ?: 1.0 }
    suspend fun setAppleMusicSaturation(value: Double) = dataStore.edit { it[doublePreferencesKey("apple_music_saturation")] = value }

    /** 渲染分辨率缩放 */
    val appleMusicRenderScale: Flow<Double> = dataStore.data.map { it[doublePreferencesKey("apple_music_render_scale")] ?: 0.5 }
    suspend fun setAppleMusicRenderScale(value: Double) = dataStore.edit { it[doublePreferencesKey("apple_music_render_scale")] = value }

    /** 低音脉冲开关 */
    val appleMusicBassPulse: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey("apple_music_bass_pulse")] ?: false }
    suspend fun setAppleMusicBassPulse(value: Boolean) = dataStore.edit { it[booleanPreferencesKey("apple_music_bass_pulse")] = value }
}
