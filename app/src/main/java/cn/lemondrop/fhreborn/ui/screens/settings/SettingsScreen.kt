package cn.lemondrop.fhreborn.ui.screens.settings

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import io.github.composefluent.component.Icon
import io.github.composefluent.component.ListItem
import io.github.composefluent.component.Slider
import io.github.composefluent.component.Switcher
import io.github.composefluent.component.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lemondrop.fhreborn.data.model.SettingItem
import cn.lemondrop.fhreborn.data.model.SettingType
import cn.lemondrop.fhreborn.data.model.Option
import cn.lemondrop.clover.CloverDialog
import cn.lemondrop.fhreborn.ui.components.MainScaffold
import cn.lemondrop.fhreborn.ui.theme.FluentLargeCorner
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.SettingsViewModel
import com.composables.icons.lucide.Activity
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.BrainCircuit
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Database
import com.composables.icons.lucide.Ear
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Headphones
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MonitorSpeaker
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Puzzle
import com.composables.icons.lucide.Type
import com.composables.icons.lucide.Volume2
import com.composables.icons.lucide.Wrench
import com.composables.icons.lucide.Zap

@Composable
fun SettingsScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onPlayerClick: () -> Unit,
    playerViewModel: PlayerViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(context.applicationContext as Application)
    )

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    var showBackground by remember { mutableStateOf(false) }

    // 拦截系统返回键：背景子页优先返回，其次分类详情页回到设置主页，避免直接退出
    BackHandler(enabled = showBackground) {
        showBackground = false
    }
    BackHandler(enabled = selectedCategory != null && !showBackground) {
        viewModel.navigateBack()
    }

    val titleText: @Composable () -> Unit = {
        Text(
            text = when {
                showBackground -> "背景"
                else -> when (selectedCategory) {
                    null -> "设置"
                    "language" -> "语言"
                    "personalize" -> "个性化"
                    "features" -> "功能"
                    "output" -> "输出"
                    "lyrics" -> "歌词"
                    "library" -> "媒体库"
                    "accessibility" -> "无障碍"
                    "plugins" -> "扩展与插件"
                    "data" -> "数据管理"
                    "experimental" -> "实验性选项"
                    "developer" -> "开发者选项"
                    "about" -> "关于"
                    else -> "设置"
                }
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    MainScaffold(
        playerViewModel = playerViewModel,
        currentRoute = currentRoute,
        onNavigate = onNavigate,
        title = titleText,
        onPlayerClick = onPlayerClick
    ) { paddingValues, bottomOverlayHeight ->
        if (showBackground) {
            BackgroundSettingsContent(viewModel)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding()),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (selectedCategory == null) {
                    // 设置主页：分类列表
                    items(buildCategories(), key = { it.key }) { category ->
                        CategoryItem(
                            category = category,
                            onClick = { viewModel.selectCategory(category.key) }
                        )
                    }
                } else {
                    // 分类详情页
                    val category = buildCategories().find { it.key == selectedCategory }
                    if (category != null) {
                        items(category.items) { item ->
                            SettingItemRow(
                                item = item,
                                viewModel = viewModel,
                                onNavigationClick = { if (it.key == "main_bg") showBackground = true }
                            )
                        }
                    }
                }

                // 底部占位，让最后一项可以滚动到亚克力底栏上方
                item {
                    Spacer(modifier = Modifier.height(bottomOverlayHeight + 16.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: cn.lemondrop.fhreborn.data.model.SettingCategory,
    onClick: () -> Unit
) {
    cn.lemondrop.fhreborn.ui.components.FhListItem(
        title = category.title,
        onClick = onClick,
        leading = category.icon?.let {
            { Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        trailing = {
            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

@Composable
private fun SettingItemRow(
    item: SettingItem,
    viewModel: SettingsViewModel,
    onNavigationClick: ((SettingItem) -> Unit)? = null
) {
    // 根据类型只读取对应的值，避免类型转换崩溃
    val toggleValue by when (item.type) {
        is SettingType.Toggle -> viewModel.getToggleValue(item.key, item.defaultValue as? Boolean ?: false)
            .collectAsState(initial = item.defaultValue as? Boolean ?: false)
        else -> remember { mutableStateOf(item.defaultValue as? Boolean ?: false) }
    }

    val stringValue by when (item.type) {
        is SettingType.Selection -> viewModel.getStringValue(item.key, item.defaultValue as? String ?: "")
            .collectAsState(initial = item.defaultValue as? String ?: "")
        else -> remember { mutableStateOf(item.defaultValue as? String ?: "") }
    }

    val sliderValue by when (item.type) {
        is SettingType.Slider -> viewModel.getIntValue(item.key, (item.defaultValue as? Number)?.toInt() ?: 0)
            .collectAsState(initial = (item.defaultValue as? Number)?.toInt() ?: 0)
        else -> remember { mutableStateOf((item.defaultValue as? Number)?.toInt() ?: 0) }
    }

    val selectionType = item.type as? SettingType.Selection
    var showSelectionDialog by remember { mutableStateOf(false) }

    // 选择弹窗
    if (showSelectionDialog && selectionType != null) {
        CloverDialog(
            onDismissRequest = { showSelectionDialog = false },
            title = item.title,
            buttons = {
                TextButton(onClick = { showSelectionDialog = false }) {
                    Text("取消")
                }
            }
        ) {
            selectionType.options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.setStringSetting(item.key, option.key)
                            showSelectionDialog = false
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = option.key == stringValue,
                        onClick = {
                            viewModel.setStringSetting(item.key, option.key)
                            showSelectionDialog = false
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = option.label)
                }
            }
        }
    }

    // Slider 类型使用 Column 布局，其他使用 Row
    if (item.type is SettingType.Slider) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 3.dp)
                .clip(RoundedCornerShape(FluentLargeCorner))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.icon != null) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (item.description != null) {
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = sliderValue.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val sliderType = item.type as SettingType.Slider
            Slider(
                value = sliderValue.toFloat(),
                onValueChange = { viewModel.setIntSetting(item.key, it.toInt()) },
                valueRange = sliderType.min..sliderType.max,
                steps = sliderType.steps,
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        cn.lemondrop.fhreborn.ui.components.FhListItem(
            title = item.title,
            subtitle = item.description,
            onClick = {
                when (item.type) {
                    is SettingType.Toggle -> viewModel.toggleSetting(item)
                    is SettingType.Selection -> showSelectionDialog = true
                    else -> onNavigationClick?.invoke(item)
                }
            },
            leading = item.icon?.let {
                { Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            trailing = {
                when (item.type) {
                    is SettingType.Toggle -> {
                        Switcher(
                            checked = toggleValue,
                            onCheckStateChange = { viewModel.toggleSetting(item) }
                        )
                    }
                    is SettingType.Selection -> {
                        val selectedLabel = selectionType?.options?.find { it.key == stringValue }?.label ?: stringValue
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = selectedLabel)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Lucide.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    is SettingType.Info -> {
                        val infoText = item.defaultValue?.toString() ?: ""
                        Text(text = infoText)
                    }
                    else -> {
                        Icon(
                            imageVector = Lucide.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        )
    }
}

// ========== 设置分类定义 ==========

private fun buildCategories(): List<cn.lemondrop.fhreborn.data.model.SettingCategory> {
    return listOf(
        cn.lemondrop.fhreborn.data.model.SettingCategory(
            key = "language",
            title = "语言",
            icon = Lucide.Globe,
            items = listOf(
                SettingItem("lang_app", "应用语言", "当前: 简体中文", Lucide.Globe, SettingType.Selection(listOf(Option("简体中文", "简体中文"), Option("繁體中文", "繁體中文"), Option("English", "English"), Option("日本語", "日本語"), Option("한국어", "한국어")))),
                SettingItem("lang_lyric", "歌词语言", "歌词显示与搜索语言", Lucide.BookOpen, SettingType.Selection(listOf(Option("自动", "自动"), Option("简体中文", "简体中文"), Option("繁體中文", "繁體中文"), Option("English", "English"), Option("日本語", "日本語"), Option("한국어", "한국어")))),
                SettingItem("lang_sort", "排序规则", "按语言习惯排序", Lucide.Type, SettingType.Selection(listOf(Option("默认", "默认"), Option("拼音", "拼音"), Option("五十音图", "五十音图"), Option("罗马音", "罗马音"))))
            )
        ),
        cn.lemondrop.fhreborn.data.model.SettingCategory(
            key = "personalize",
            title = "个性化",
            icon = Lucide.Palette,
            items = listOf(
                // 主题与颜色
                SettingItem("", "主题与颜色", null, null, SettingType.Info),
                SettingItem("theme_mode", "颜色模式", "深色 / 浅色 / 跟随系统", null, SettingType.Selection(listOf(Option("system", "跟随系统"), Option("light", "浅色"), Option("dark", "深色"))), "system"),
                SettingItem("accent_color", "主题颜色", "紫色", Lucide.Palette, SettingType.Selection(listOf(Option("默认", "默认"), Option("蓝", "蓝"), Option("绿", "绿"), Option("紫", "紫"), Option("橙", "橙"), Option("粉", "粉"), Option("红", "红"), Option("青", "青"))), "紫"),
                SettingItem("dynamic_color", "Material You 动态取色", "跟随系统的壁纸取色使用monet取色", Lucide.Palette, SettingType.Toggle, false),

                // 主界面
                SettingItem("", "主界面", null, null, SettingType.Info),
                SettingItem("hide_system_ui", "隐藏状态栏和导航栏", "滑动状态栏/导航栏以显示", null, SettingType.Toggle, false),
                SettingItem("main_bg", "主页面背景", "纯色 / 自选图片 / 云母", null, SettingType.Navigation),
                SettingItem("predictive_back", "预测性返回手势", "返回时预览上一页（实验，可能有异常）", null, SettingType.Toggle, false),

                // 播放器
                SettingItem("", "播放器", null, null, SettingType.Info),
                SettingItem("player_bg", "播放器页面背景", "专辑封面模糊（跟随app颜色模式）", null, SettingType.Selection(listOf(
                    Option("rotating_fluid", "旋转流体背景（跟随app颜色模式）"),
                    Option("agsl_fluid", "AGSL流体背景（跟随app颜色模式）"),
                    Option("cover_blur", "专辑封面模糊（跟随app颜色模式）"),
                    Option("default_color", "默认背景色（跟随app颜色模式）")
                )), "cover_blur"),
                SettingItem("cover_radius", "封面圆角大小调整", "调整播放器专辑封面的圆角大小", null, SettingType.Slider(0f, 24f, 23), 12),
                SettingItem("cover_crop", "封面裁剪", "将不规则尺寸的封面以正方形显示", null, SettingType.Toggle, true),
                SettingItem("show_playmode_queue", "显示播放模式和播放列表", "显示在播放控制两侧", null, SettingType.Toggle, true),
                SettingItem("show_mini_lyric", "显示小歌词", "显示在封面下方", null, SettingType.Toggle, true),
                SettingItem("player_button_custom", "播放器按钮定制", "为播放器按钮排序", null, SettingType.Navigation),
                SettingItem("show_audio_info", "显示音频信息", "位于屏幕底部", null, SettingType.Toggle, true)
            )
        ),
        cn.lemondrop.fhreborn.data.model.SettingCategory(
            key = "features",
            title = "功能",
            icon = Lucide.Wrench,
            items = listOf(
                // 搜索
                SettingItem("", "搜索", null, null, SettingType.Info),
                SettingItem("search_mode", "搜索模式", "模糊匹配 / 精确匹配 / 拼音", null, SettingType.Selection(listOf(Option("模糊匹配", "模糊匹配"), Option("精确匹配", "精确匹配"), Option("拼音搜索", "拼音搜索"))), "模糊匹配"),
                SettingItem("search_scope", "搜索范围", "歌曲 / 专辑 / 艺术家 / 全部", null, SettingType.Selection(listOf(Option("全部", "全部"), Option("歌曲", "歌曲"), Option("专辑", "专辑"), Option("艺术家", "艺术家"), Option("歌词", "歌词"))), "全部"),
                SettingItem("search_history", "搜索历史", null, null, SettingType.Toggle, true),
                SettingItem("search_suggestions", "搜索建议", "输入时显示推荐结果", null, SettingType.Toggle, true),

                // 节电与后台
                SettingItem("", "节电与后台", null, null, SettingType.Info),
                SettingItem("power_save_mode", "节电模式", "降低后台刷新和动画以延长续航", Lucide.Zap, SettingType.Toggle, false),
                SettingItem("keep_in_background", "保持后台播放", "允许应用在后台持续播放", null, SettingType.Toggle, true),
                SettingItem("stop_on_task_removed", "移除任务时停止", "从最近任务划掉后停止播放", null, SettingType.Toggle, false),
                SettingItem("wake_lock", "唤醒锁", "播放器页面保持屏幕常亮", null, SettingType.Toggle, true),

                // 自动行为
                SettingItem("", "自动行为", null, null, SettingType.Info),
                SettingItem("auto_play_launch", "启动自动播放", "打开应用时继续上次播放", null, SettingType.Toggle, false),
                SettingItem("auto_play_headset", "插入耳机自动播放", "检测到耳机连接时开始播放", null, SettingType.Toggle, false),
                SettingItem("auto_play_bluetooth", "蓝牙连接自动播放", "连接到蓝牙音频设备时开始播放", null, SettingType.Toggle, false),
                SettingItem("auto_resume", "中断后自动恢复", "来电或通知结束后自动恢复播放", null, SettingType.Toggle, true),
                SettingItem("gapless_playback", "无缝播放", "消除歌曲间的间隙", null, SettingType.Toggle, true),

                // 常用功能
                SettingItem("skip_silence", "跳过静音", "自动跳过歌曲开头的静音部分", null, SettingType.Toggle, false),
                SettingItem("gesture_control", "手势控制", "播放页左滑下首 / 右滑上首", null, SettingType.Toggle, true),
                SettingItem("mini_lyric", "迷你歌词", "播放页显示当前歌词行", null, SettingType.Toggle, true)
            )
        ),
        cn.lemondrop.fhreborn.data.model.SettingCategory(
            key = "output",
            title = "输出",
            icon = Lucide.Volume2,
            items = listOf(
                // 播放设备
                SettingItem("", "播放设备", null, null, SettingType.Info),
                SettingItem("audio_output", "输出设备", "自动 / 扬声器 / 耳机 / 蓝牙", Lucide.Headphones, SettingType.Selection(listOf(Option("自动", "自动"), Option("内置扬声器", "内置扬声器"), Option("有线耳机", "有线耳机"), Option("蓝牙耳机", "蓝牙耳机"), Option("USB DAC", "USB DAC"))), "自动"),
                SettingItem("audio_device_priority", "设备优先级", "多设备连接时的首选输出", null, SettingType.Selection(listOf(Option("上次使用", "上次使用"), Option("有线优先", "有线优先"), Option("蓝牙优先", "蓝牙优先"), Option("USB优先", "USB优先"))), "上次使用"),
                SettingItem("auto_switch_device", "自动切换设备", "断开当前设备时自动切换", null, SettingType.Toggle, true),

                // 音量
                SettingItem("", "音量", null, null, SettingType.Info),
                SettingItem("media_volume", "媒体音量", "系统媒体音量控制", Lucide.MonitorSpeaker, SettingType.Slider(0f, 100f, 99), 70),
                SettingItem("volume_normalize", "音量标准化", "统一不同歌曲的音量差异 (ReplayGain)", null, SettingType.Toggle, false),
                SettingItem("volume_normalize_mode", "标准化模式", "音轨增益 / 专辑增益", null, SettingType.Selection(listOf(Option("音轨增益", "音轨增益"), Option("专辑增益", "专辑增益"))), "音轨增益"),
                SettingItem("volume_limit", "音量限制", "设置最大输出音量", null, SettingType.Slider(0f, 100f, 99), 100),
                SettingItem("fade_in_out", "淡入淡出", "播放开始和结束时平滑过渡音量", null, SettingType.Toggle, true),
                SettingItem("fade_duration", "淡入淡出时长", "毫秒", null, SettingType.Selection(listOf(Option("500ms", "500ms"), Option("1000ms", "1000ms"), Option("2000ms", "2000ms"), Option("3000ms", "3000ms"))), "1000ms"),

                // 输出选项
                SettingItem("", "输出选项", null, null, SettingType.Info),
                SettingItem("sample_rate", "输出采样率", "跟随系统或固定采样率", Lucide.MonitorSpeaker, SettingType.Selection(listOf(Option("跟随系统", "跟随系统"), Option("44.1kHz", "44.1kHz"), Option("48kHz", "48kHz"), Option("88.2kHz", "88.2kHz"), Option("96kHz", "96kHz"), Option("176.4kHz", "176.4kHz"), Option("192kHz", "192kHz"), Option("384kHz", "384kHz"))), "跟随系统"),
                SettingItem("bit_depth", "输出位深", "16bit / 24bit / 32bit 浮点", null, SettingType.Selection(listOf(Option("跟随源文件", "跟随源文件"), Option("16bit", "16bit"), Option("24bit", "24bit"), Option("32bit 浮点", "32bit 浮点"))), "跟随源文件"),
                SettingItem("channel_mode", "通道模式", "立体声 / 单声道 / 自动", null, SettingType.Selection(listOf(Option("自动", "自动"), Option("立体声", "立体声"), Option("单声道", "单声道"), Option("反相立体声", "反相立体声"))), "自动"),
                SettingItem("audio_latency", "音频延迟补偿", "调整音视频同步偏移", null, SettingType.Slider(-500f, 500f, 1000), 0),
                SettingItem("buffer_size", "缓冲区大小", "较大的缓冲区可减少卡顿", null, SettingType.Selection(listOf(Option("自动", "自动"), Option("小", "小"), Option("中", "中"), Option("大", "大"), Option("极大", "极大"))), "自动"),

                // 解码器
                SettingItem("", "解码器", null, null, SettingType.Info),
                SettingItem("decoder_priority", "解码器优先级", "系统内置 / Media3 / FFmpeg", Lucide.Activity, SettingType.Selection(listOf(Option("自动选择", "自动选择"), Option("系统内置优先", "系统内置优先"), Option("Media3优先", "Media3优先"), Option("FFmpeg优先", "FFmpeg优先"))), "自动选择"),
                SettingItem("ffmpeg_enabled", "FFmpeg 解码器", "使用 FFmpeg 处理更多格式", null, SettingType.Toggle, true),
                SettingItem("dsd_direct", "DSD 直通", "原生 DSD 输出到支持设备", null, SettingType.Toggle, false),
                SettingItem("dsd_to_pcm", "DSD 转 PCM", "将 DSD 转为高采样率 PCM", null, SettingType.Selection(listOf(Option("自动", "自动"), Option("DoP (DSD over PCM)", "DoP (DSD over PCM)"), Option("Native PCM 转换", "Native PCM 转换"))), "自动"),
                SettingItem("gapless_decoder", "无缝解码", "精确处理歌曲边界", null, SettingType.Toggle, true),

                // 均衡器与音效
                SettingItem("", "均衡器与音效", null, null, SettingType.Info),
                SettingItem("eq_enabled", "均衡器", "自定义频段增益", Lucide.Activity, SettingType.Toggle, false),
                SettingItem("eq_preset", "均衡器预设", "预设音效", null, SettingType.Selection(listOf(Option("关闭", "关闭"), Option("流行", "流行"), Option("摇滚", "摇滚"), Option("古典", "古典"), Option("爵士", "爵士"), Option("电子", "电子"), Option("人声", "人声"), Option("舞曲", "舞曲"), Option("轻柔", "轻柔"), Option("重金属", "重金属"), Option("嘻哈", "嘻哈"))), "关闭"),
                SettingItem("bass_boost_enabled", "低音增强", "提升低频响应", null, SettingType.Toggle, false),
                SettingItem("bass_boost_strength", "低音增强强度", "0 ~ 100%", null, SettingType.Slider(0f, 100f, 99), 50),
                SettingItem("virtualizer_enabled", "声场扩展", "拓宽立体声声场", null, SettingType.Toggle, false),
                SettingItem("virtualizer_strength", "声场扩展强度", "0 ~ 100%", null, SettingType.Slider(0f, 100f, 99), 50),
                SettingItem("reverb_enabled", "混响效果", "模拟不同空间的混响", null, SettingType.Toggle, false),
                SettingItem("reverb_preset", "混响预设", null, null, SettingType.Selection(listOf(Option("关闭", "关闭"), Option("小房间", "小房间"), Option("中房间", "中房间"), Option("大房间", "大房间"), Option("大厅", "大厅"), Option("教堂", "教堂"), Option("浴室", "浴室"))), "关闭"),
                SettingItem("loudness_enabled", "响度增强", "在低音量时增强高低频", null, SettingType.Toggle, false),
                SettingItem("loudness_strength", "响度增强强度", "0 ~ 100%", null, SettingType.Slider(0f, 100f, 99), 50),

                // 高级输出
                SettingItem("", "高级输出", null, null, SettingType.Info),
                SettingItem("exclusive_mode", "独占音频输出", "绕过系统混音器独占音频设备", null, SettingType.Toggle, false),
                SettingItem("float_output", "浮点输出", "使用 32-bit float 输出", null, SettingType.Toggle, false),
                SettingItem("resampler_quality", "重采样质量", "音频重采样算法质量", null, SettingType.Selection(listOf(Option("快速", "快速"), Option("标准", "标准"), Option("高质量", "高质量"), Option("极致", "极致"))), "标准"),
                SettingItem("mixer_mode", "混音模式", "音频混合处理模式", null, SettingType.Selection(listOf(Option("标准", "标准"), Option("高保真", "高保真"), Option("低延迟", "低延迟"))), "标准")
            )
        ),
        cn.lemondrop.fhreborn.data.model.SettingCategory(
            key = "lyrics",
            title = "歌词",
            icon = Lucide.BookOpen,
            items = listOf(
                SettingItem("lyric_priority", "歌词来源优先级", "内嵌 / 文件 / 导入", null, SettingType.Selection(listOf(Option("内嵌标签优先", "内嵌标签优先"), Option("同目录文件优先", "同目录文件优先"), Option("应用内置目录优先", "应用内置目录优先"))), "内嵌标签优先"),
                SettingItem("lyric_translation", "显示翻译", "双行显示原文+翻译", null, SettingType.Toggle, true),
                SettingItem("lyric_romaji", "显示罗马音", "日语/韩语罗马音注音", null, SettingType.Toggle, false),
                SettingItem("lyric_font_size", "歌词字体大小", "8sp ~ 48sp", null, SettingType.Selection(listOf(Option("小", "小"), Option("中", "中"), Option("大", "大"), Option("特大", "特大"))), "中"),
                SettingItem("lyric_align_center", "歌词居中对齐", "歌词预览和歌词页居中对齐", null, SettingType.Toggle, false),
                SettingItem("desktop_lyric", "桌面歌词", "悬浮窗形式显示歌词", null, SettingType.Toggle, false),
                SettingItem("statusbar_lyric", "状态栏歌词", "通知栏显示当前歌词", null, SettingType.Toggle, false)
            )
        ),
        cn.lemondrop.fhreborn.data.model.SettingCategory(
            key = "library",
            title = "媒体库",
            icon = Lucide.Music,
            items = listOf(
                SettingItem("auto_scan", "启动时自动扫描", "每次打开检测媒体库变更", null, SettingType.Toggle, true),
                SettingItem("scan_directories", "扫描目录", "管理音乐文件夹", Lucide.FolderOpen, SettingType.Navigation),
                SettingItem("hidden_folders", "隐藏文件夹", "管理黑名单目录", null, SettingType.Navigation),
                SettingItem("cover_cache", "封面缓存策略", "懒加载 / 磁盘缓存 / 混合", null, SettingType.Selection(listOf(Option("懒加载", "懒加载"), Option("磁盘缓存", "磁盘缓存"), Option("混合策略", "混合策略"))), "磁盘缓存"),
                SettingItem("ignore_short", "忽略短音频", "过滤时长过短的文件", null, SettingType.Toggle, true)
            )
        ),
        cn.lemondrop.fhreborn.data.model.SettingCategory(
            key = "accessibility",
            title = "无障碍",
            icon = Lucide.Ear,
            items = listOf(
                SettingItem("large_text", "大字体模式", "全局字体放大", null, SettingType.Toggle, false),
                SettingItem("high_contrast", "高对比度", "增强文字与背景对比", null, SettingType.Toggle, false),
                SettingItem("reduce_motion", "减少动画", "关闭或简化界面动画", null, SettingType.Toggle, false),
                SettingItem("screen_reader", "屏幕阅读器优化", "优化 TalkBack 体验", null, SettingType.Toggle, false)
            )
        ),
        cn.lemondrop.fhreborn.data.model.SettingCategory(
            key = "plugins",
            title = "扩展与插件",
            icon = Lucide.Puzzle,
            items = listOf(
                SettingItem("plugin_list", "已安装插件", "查看和管理插件", Lucide.Puzzle, SettingType.Navigation),
                SettingItem("plugin_store", "插件市场", "浏览和下载插件", null, SettingType.Navigation)
            )
        ),
        cn.lemondrop.fhreborn.data.model.SettingCategory(
            key = "data",
            title = "数据管理",
            icon = Lucide.Database,
            items = listOf(
                SettingItem("export_data", "导出数据", "备份数据库和设置", Lucide.Database, SettingType.Navigation),
                SettingItem("import_data", "导入数据", "恢复备份", null, SettingType.Navigation),
                SettingItem("auto_backup", "自动备份", "定期自动导出数据", null, SettingType.Toggle, false),
                SettingItem("clear_cache", "清除缓存", "删除封面缩略图缓存", null, SettingType.Navigation),
                SettingItem("reset_settings", "恢复默认设置", "重置所有选项到默认", null, SettingType.Navigation)
            )
        ),
        cn.lemondrop.fhreborn.data.model.SettingCategory(
            key = "experimental",
            title = "实验性选项",
            icon = Lucide.Zap,
            items = listOf(
                SettingItem("exp_fluid_bg", "流体律动背景", "音频驱动的动态 Shader 背景", Lucide.Zap, SettingType.Toggle, false),
                SettingItem("exp_reveal", "Reveal 按压光效", "Fluent Design 按压高亮", null, SettingType.Toggle, false),
                SettingItem("exp_blendmode", "BlendMode 发光", "文字/图标混合模式发光", null, SettingType.Toggle, false),
                SettingItem("exp_exclusive", "独占音频输出", "绕过系统混音器（需 DAC）", null, SettingType.Toggle, false)
            )
        ),
        cn.lemondrop.fhreborn.data.model.SettingCategory(
            key = "developer",
            title = "开发者选项",
            icon = Lucide.BrainCircuit,
            items = listOf(
                SettingItem("crash_report", "崩溃报告", "捕获并显示崩溃日志", null, SettingType.Toggle, true),
                SettingItem("debug_mode", "调试模式", "显示额外调试信息", null, SettingType.Toggle, false),
                SettingItem("log_level", "日志级别", "控制日志输出详细程度", null, SettingType.Selection(listOf(Option("错误", "错误"), Option("警告", "警告"), Option("信息", "信息"), Option("调试", "调试"), Option("详细", "详细"))), "信息"),
                SettingItem("clear_database", "清空数据库", "删除所有扫描数据（谨慎）", null, SettingType.Navigation)
            )
        ),
        cn.lemondrop.fhreborn.data.model.SettingCategory(
            key = "about",
            title = "关于",
            icon = Lucide.Heart,
            items = listOf(
                SettingItem("app_version", "版本", "FH Reborn v1.0", null, SettingType.Info, "v1.0"),
                SettingItem("open_source", "开源许可", "查看第三方库许可证", null, SettingType.Navigation),
                SettingItem("privacy_policy", "隐私政策", null, null, SettingType.Navigation),
                SettingItem("check_update", "检查更新", null, null, SettingType.Navigation),
                SettingItem("feedback", "反馈", "发送意见或建议", null, SettingType.Navigation)
            )
        )
    )
}


