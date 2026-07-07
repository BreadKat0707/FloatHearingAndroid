package cn.lemondrop.fhreborn.ui.screens.settings

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lemondrop.clover.CloverBottomSheet
import cn.lemondrop.clover.CloverButton
import cn.lemondrop.clover.CloverDialog
import cn.lemondrop.clover.CloverIconButton
import cn.lemondrop.clover.CloverSizes
import cn.lemondrop.clover.ui.layout.CloverAdaptiveShellScaffold
import cn.lemondrop.clover.ui.layout.CloverShellStrategy
import cn.lemondrop.fhreborn.data.model.SettingCategory
import cn.lemondrop.fhreborn.data.model.SettingItem
import cn.lemondrop.fhreborn.data.model.SettingType
import cn.lemondrop.fhreborn.data.repository.SettingsRepository
import cn.lemondrop.fhreborn.ui.components.AppBackgroundLayer
import cn.lemondrop.fhreborn.ui.components.AppDrawer
import cn.lemondrop.fhreborn.ui.components.MiniPlayBar
import cn.lemondrop.fhreborn.ui.theme.FluentLargeCorner
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.SettingsViewModel
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.MonitorSpeaker
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Puzzle
import com.composables.icons.lucide.Volume2
import com.composables.icons.lucide.Wrench
import com.composables.icons.lucide.X
import dev.chrisbanes.haze.HazeState
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Slider
import io.github.composefluent.component.Switcher
import io.github.composefluent.component.Text
import kotlinx.coroutines.launch

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
    val settingsRepository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    val artistSeparators by settingsRepository.artistSeparators.collectAsState(initial = setOf(" / "))

    var currentPage by remember { mutableStateOf<SettingsPage>(SettingsPage.Home) }
    var showArtistSeparatorSheet by remember { mutableStateOf(false) }
    var showDrawer by remember { mutableStateOf(false) }

    // 系统返回键：子页返回设置主页，分隔符弹窗优先关闭
    BackHandler(enabled = currentPage != SettingsPage.Home) {
        currentPage = SettingsPage.Home
        viewModel.navigateBack()
    }
    BackHandler(enabled = showArtistSeparatorSheet) {
        showArtistSeparatorSheet = false
    }

    val isHome = currentPage == SettingsPage.Home

    val titleText: @Composable () -> Unit = {
        val page = currentPage
        Text(
            text = when (page) {
                SettingsPage.Home -> "设置"
                is SettingsPage.Category -> when (page.key) {
                    "language" -> "语言"
                    "personalize" -> "个性化"
                    "features" -> "功能"
                    "output" -> "输出"
                    "lyrics" -> "歌词"
                    "library" -> "媒体库"
                    "about" -> "关于"
                    else -> "设置"
                }
                SettingsPage.Background -> "背景"
                SettingsPage.CodecCapabilities -> "本机编解码器"
                SettingsPage.AccompanistLyric -> "Accompanist Lyric 设置"
                SettingsPage.OpenSourceLicenses -> "开源许可"
                SettingsPage.PlayerBackground -> "播放器页面背景"
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    val menuButton: @Composable () -> Unit = {
        CloverIconButton(
            icon = Lucide.Menu,
            contentDescription = "菜单",
            onClick = { showDrawer = true }
        )
    }

    val backButton: @Composable () -> Unit = {
        CloverIconButton(
            icon = Lucide.ArrowLeft,
            contentDescription = "返回",
            onClick = {
                currentPage = SettingsPage.Home
                viewModel.navigateBack()
            }
        )
    }

    val onNavigateItem: (SettingItem) -> Unit = { item ->
        when (item.key) {
            "main_bg" -> currentPage = SettingsPage.Background
            "artist_separators" -> showArtistSeparatorSheet = true
            "codec_capability" -> currentPage = SettingsPage.CodecCapabilities
            "accompanist_lyric" -> currentPage = SettingsPage.AccompanistLyric
            "open_source" -> currentPage = SettingsPage.OpenSourceLicenses
            "player_bg" -> currentPage = SettingsPage.PlayerBackground
        }
    }

    CloverAdaptiveShellScaffold(
        strategy = CloverShellStrategy.BottomCombined,
        title = titleText,
        navigationIcon = if (isHome) menuButton else backButton,
        background = { AppBackgroundLayer() },
        overlay = { state ->
            AppDrawer(
                visible = showDrawer,
                onDismiss = { showDrawer = false },
                currentRoute = currentRoute,
                onNavigate = { route ->
                    showDrawer = false
                    onNavigate(route)
                },
                hazeState = state.hazeState,
                onScheduledPauseClick = { playerViewModel.showScheduledPause() }
            )

            if (showArtistSeparatorSheet) {
                ArtistSeparatorSheet(
                    separators = artistSeparators,
                    onDismiss = { showArtistSeparatorSheet = false },
                    hazeState = state.hazeState,
                    onSave = { newSeparators ->
                        scope.launch {
                            settingsRepository.setArtistSeparators(newSeparators)
                        }
                    }
                )
            }

            MiniPlayBar(
                playerViewModel = playerViewModel,
                onClick = onPlayerClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = state.contentPadding.calculateBottomPadding() + 8.dp
                    )
            )
        },
        content = { state ->
            val bottomOverlayHeight = state.contentPadding.calculateBottomPadding() + 64.dp + 16.dp
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentPage) {
                    SettingsPage.Home,
                    is SettingsPage.Category -> SettingsListContent(
                        viewModel = viewModel,
                        currentPage = currentPage,
                        onCategoryClick = { key ->
                            currentPage = SettingsPage.Category(key)
                            viewModel.selectCategory(key)
                        },
                        onNavigationClick = onNavigateItem,
                        paddingValues = state.contentPadding,
                        bottomOverlayHeight = bottomOverlayHeight
                    )

                    SettingsPage.Background -> BackgroundSettingsContent(viewModel)
                    SettingsPage.CodecCapabilities -> CodecCapabilitiesContent(
                        paddingValues = state.contentPadding,
                        bottomOverlayHeight = bottomOverlayHeight,
                        hazeState = state.hazeState
                    )
                    SettingsPage.AccompanistLyric -> AccompanistLyricSettingsContent(
                        paddingValues = state.contentPadding,
                        bottomOverlayHeight = bottomOverlayHeight,
                        hazeState = state.hazeState
                    )
                    SettingsPage.OpenSourceLicenses -> OpenSourceLicensesContent(
                        paddingValues = state.contentPadding,
                        bottomOverlayHeight = bottomOverlayHeight,
                        hazeState = state.hazeState
                    )
                    SettingsPage.PlayerBackground -> PlayerBackgroundPickerContent(
                        paddingValues = state.contentPadding,
                        bottomOverlayHeight = bottomOverlayHeight,
                        hazeState = state.hazeState
                    )
                }
            }
        }
    )
}

private sealed class SettingsPage {
    data object Home : SettingsPage()
    data class Category(val key: String) : SettingsPage()
    data object Background : SettingsPage()
    data object CodecCapabilities : SettingsPage()
    data object AccompanistLyric : SettingsPage()
    data object OpenSourceLicenses : SettingsPage()
    data object PlayerBackground : SettingsPage()
}

@Composable
private fun SettingsListContent(
    viewModel: SettingsViewModel,
    currentPage: SettingsPage,
    onCategoryClick: (String) -> Unit,
    onNavigationClick: (SettingItem) -> Unit,
    paddingValues: PaddingValues,
    bottomOverlayHeight: Dp
) {
    val selectedCategory = (currentPage as? SettingsPage.Category)?.key
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding()),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (selectedCategory == null) {
            items(buildCategories(), key = { it.key }) { category ->
                CategoryItem(
                    category = category,
                    onClick = { onCategoryClick(category.key) }
                )
            }
        } else {
            val category = buildCategories().find { it.key == selectedCategory }
            if (category != null) {
                items(category.items) { item ->
                    SettingItemRow(
                        item = item,
                        viewModel = viewModel,
                        onNavigationClick = onNavigationClick
                    )
                }
            }
        }

        // 底部占位，让最后一项可以滚动到迷你播放条上方
        item {
            Spacer(modifier = Modifier.height(bottomOverlayHeight + 16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArtistSeparatorSheet(
    separators: Set<String>,
    onDismiss: () -> Unit,
    hazeState: HazeState,
    onSave: (Set<String>) -> Unit
) {
    var current by remember { mutableStateOf(separators.toSortedSet()) }
    var input by remember { mutableStateOf("") }

    CloverBottomSheet(
        onDismiss = onDismiss,
        title = "艺术家分隔符",
        hazeState = hazeState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CloverSizes.listOuterHorizontalPadding)
        ) {
            Text(
                text = "用于拆分歌曲艺术家字段。例如添加 \" / \" 后，\"A / B\" 会被识别为两个艺术家 A 和 B。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                current.forEach { sep ->
                    InputChip(
                        selected = false,
                        onClick = { },
                        label = { Text("\"$sep\"") },
                        trailingIcon = {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "删除",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        current = current.toMutableSet().apply { remove(sep) }.toSortedSet()
                                        onSave(current)
                                    }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("添加分隔符") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                CloverButton(
                    text = "添加",
                    onClick = {
                        if (input.isNotBlank()) {
                            current = current.toMutableSet().apply { add(input) }.toSortedSet()
                            input = ""
                            onSave(current)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            CloverButton(
                text = "恢复默认",
                onClick = {
                    current = setOf(" / ").toSortedSet()
                    onSave(current)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CategoryItem(
    category: SettingCategory,
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
            visible = true,
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

private fun buildCategories(): List<SettingCategory> {
    return listOf(
        SettingCategory(
            key = "language",
            title = "语言",
            icon = Lucide.Globe,
            items = listOf(
                SettingItem("lang_app", "应用语言", "当前: 简体中文", Lucide.Globe, SettingType.Selection(listOf(cn.lemondrop.fhreborn.data.model.Option("简体中文", "简体中文"), cn.lemondrop.fhreborn.data.model.Option("繁體中文", "繁體中文"), cn.lemondrop.fhreborn.data.model.Option("English", "English"), cn.lemondrop.fhreborn.data.model.Option("日本語", "日本語"), cn.lemondrop.fhreborn.data.model.Option("한국어", "한국어"))))
            )
        ),
        SettingCategory(
            key = "personalize",
            title = "个性化",
            icon = Lucide.Palette,
            items = listOf(
                // 主题与颜色
                SettingItem("", "主题与颜色", null, null, SettingType.Info),
                SettingItem("theme_mode", "颜色模式", "深色 / 浅色 / 跟随系统", null, SettingType.Selection(listOf(cn.lemondrop.fhreborn.data.model.Option("system", "跟随系统"), cn.lemondrop.fhreborn.data.model.Option("light", "浅色"), cn.lemondrop.fhreborn.data.model.Option("dark", "深色"))), "system"),
                SettingItem("accent_color", "主题颜色", "紫色", Lucide.Palette, SettingType.Selection(listOf(cn.lemondrop.fhreborn.data.model.Option("默认", "默认"), cn.lemondrop.fhreborn.data.model.Option("蓝", "蓝"), cn.lemondrop.fhreborn.data.model.Option("绿", "绿"), cn.lemondrop.fhreborn.data.model.Option("紫", "紫"), cn.lemondrop.fhreborn.data.model.Option("橙", "橙"), cn.lemondrop.fhreborn.data.model.Option("粉", "粉"), cn.lemondrop.fhreborn.data.model.Option("红", "红"), cn.lemondrop.fhreborn.data.model.Option("青", "青"))), "紫"),
                SettingItem("dynamic_color", "Material You 动态取色", "跟随系统的壁纸取色使用monet取色", Lucide.Palette, SettingType.Toggle, false),

                // 主界面
                SettingItem("", "主界面", null, null, SettingType.Info),
                SettingItem("hide_system_ui", "隐藏状态栏和导航栏", "滑动状态栏/导航栏以显示", null, SettingType.Toggle, false),
                SettingItem("main_bg", "主页面背景", "纯色 / 自选图片 / 云母", null, SettingType.Navigation),
                SettingItem("player_bg", "播放器页面背景", "旋转流体 / AGSL 流体 / 封面模糊", null, SettingType.Navigation),
                SettingItem("predictive_back", "预测性返回手势", "返回时预览上一页（实验，可能有异常）", null, SettingType.Toggle, false)
            )
        ),
        SettingCategory(
            key = "features",
            title = "功能",
            icon = Lucide.Wrench,
            items = listOf(
                SettingItem("wake_lock", "唤醒锁", "播放器页面保持屏幕常亮", null, SettingType.Toggle, true)
            )
        ),
        SettingCategory(
            key = "output",
            title = "输出",
            icon = Lucide.Volume2,
            items = listOf(
                SettingItem("codec_capability", "查看本机支持的编解码器", null, null, SettingType.Navigation)
            )
        ),
        SettingCategory(
            key = "lyrics",
            title = "歌词",
            icon = Lucide.BookOpen,
            items = listOf(
                SettingItem("accompanist_lyric", "Accompanist Lyric设置", null, null, SettingType.Navigation)
            )
        ),
        SettingCategory(
            key = "library",
            title = "媒体库",
            icon = Lucide.Music,
            items = listOf(
                SettingItem("auto_scan", "启动时自动扫描", "每次打开检测媒体库变更", null, SettingType.Toggle, true),
                SettingItem("scan_directories", "扫描目录", "管理音乐文件夹", Lucide.FolderOpen, SettingType.Navigation),
                SettingItem("hidden_folders", "隐藏文件夹", "管理黑名单目录", null, SettingType.Navigation),
                SettingItem("cover_cache", "封面缓存策略", "懒加载 / 磁盘缓存 / 混合", null, SettingType.Selection(listOf(cn.lemondrop.fhreborn.data.model.Option("懒加载", "懒加载"), cn.lemondrop.fhreborn.data.model.Option("磁盘缓存", "磁盘缓存"), cn.lemondrop.fhreborn.data.model.Option("混合策略", "混合策略"))), "磁盘缓存"),
                SettingItem("ignore_short", "忽略短音频", "过滤时长过短的文件", null, SettingType.Toggle, true),
                SettingItem("artist_separators", "艺术家分隔符", "配置多艺术家拆分规则", null, SettingType.Navigation)
            )
        ),
        SettingCategory(
            key = "about",
            title = "关于",
            icon = Lucide.Heart,
            items = listOf(
                SettingItem("app_version", "版本", "FH Reborn v1.0.0", null, SettingType.Info, "v1.0.0"),
                SettingItem("open_source", "开源许可", "查看第三方库许可证", null, SettingType.Navigation),
                SettingItem("privacy_policy", "隐私政策", null, null, SettingType.Navigation),
                SettingItem("check_update", "检查更新", null, null, SettingType.Navigation),
                SettingItem("feedback", "反馈", "发送意见或建议", null, SettingType.Navigation)
            )
        )
    )
}
