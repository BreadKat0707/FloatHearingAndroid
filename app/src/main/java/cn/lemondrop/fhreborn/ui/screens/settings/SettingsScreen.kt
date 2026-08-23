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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import cn.lemondrop.fhreborn.LocalDrawerToggle
import cn.lemondrop.fhreborn.LocalDrawerVisible
import cn.lemondrop.fhreborn.LocalPlayerOpen
import cn.lemondrop.fhreborn.LocalGlobalPlayBarHeight
import cn.lemondrop.fhreborn.data.model.SettingCategory
import cn.lemondrop.fhreborn.data.model.SettingItem
import cn.lemondrop.fhreborn.data.model.SettingType
import cn.lemondrop.fhreborn.data.repository.SettingsRepository
import cn.lemondrop.fhreborn.ui.components.FhColorPalette
import cn.lemondrop.fhreborn.ui.components.LazyListScrollBar
import cn.lemondrop.fhreborn.ui.components.FhListItem
import cn.lemondrop.fhreborn.ui.theme.BlurTopBar
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.SettingsViewModel
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.BookOpen
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
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.OkHsvHueSlider
import top.yukonga.miuix.kmp.basic.OkHsvSaturationSlider
import top.yukonga.miuix.kmp.basic.OkHsvValueSlider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.color.core.Transforms
import cn.lemondrop.fhreborn.ui.components.FhBottomSheet
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

@Composable
fun SettingsScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    playerViewModel: PlayerViewModel,
    initialCategoryKey: String? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(context.applicationContext as Application)
    )
    val settingsRepository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    val artistSeparators by settingsRepository.artistSeparators.collectAsState(initial = setOf(" / "))
    val drawerVisible = LocalDrawerVisible.current
    val drawerToggle = LocalDrawerToggle.current

    // 设置页导航：页面栈（Home 为栈底，Category/子页入栈），返回逐级弹出。
    // 外部可指定直达分类（如播放器"歌词设置"→ 设置-歌词）；
    // remember 的 key 变化会重新初始化，导航复用实例时也能生效。
    // 直达时栈底始终保留 Home，保证返回链路完整（分类 → Home）
    val pageStack: androidx.compose.runtime.snapshots.SnapshotStateList<SettingsPage> = remember(initialCategoryKey) {
        if (initialCategoryKey != null) {
            mutableStateListOf(SettingsPage.Home, SettingsPage.Category(initialCategoryKey))
        } else {
            mutableStateListOf(SettingsPage.Home)
        }
    }
    fun currentPage(): SettingsPage = pageStack.last()
    var currentSelectionItem by remember { mutableStateOf<SettingItem?>(null) }
    var showArtistSeparatorSheet by remember { mutableStateOf(false) }
    // 顶栏滚动感知：主页/分类页列表滚离顶部时显示背景/模糊，回顶隐藏
    var topBarScrolled by remember { mutableStateOf(false) }

    val onNavigateItem: (SettingItem) -> Unit = { item ->
        when (item.key) {
            "main_bg" -> pageStack.add(SettingsPage.Background)
            "artist_separators" -> showArtistSeparatorSheet = true
            "codec_capability" -> pageStack.add(SettingsPage.CodecCapabilities)
            "accompanist_lyric" -> pageStack.add(SettingsPage.AccompanistLyric)
            "open_source" -> pageStack.add(SettingsPage.OpenSourceLicenses)
            "player_bg" -> pageStack.add(SettingsPage.PlayerBackground)
        }
    }

    val onSettingItemClick: (SettingItem) -> Unit = { item ->
        when (item.type) {
            is SettingType.Navigation -> onNavigateItem(item)
            is SettingType.Selection -> currentSelectionItem = item
            is SettingType.Toggle -> viewModel.toggleSetting(item)
            else -> { }
        }
    }

    // 播放器覆盖层打开时让位：返回键优先关闭播放器（由 App 层处理）
    val playerOpen = LocalPlayerOpen.current

    // 系统返回键：逐级返回（子页 → 进入页 → 主页），分隔符弹窗优先关闭
    BackHandler(enabled = currentPage() != SettingsPage.Home && !playerOpen) {
        if (pageStack.size > 1) pageStack.removeAt(pageStack.lastIndex)
        viewModel.navigateBack()
    }
    BackHandler(enabled = showArtistSeparatorSheet) {
        showArtistSeparatorSheet = false
    }

    val isHome = currentPage() == SettingsPage.Home

    val pageTitle = {
        val page = currentPage()
        when (page) {
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
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 层背景：顶栏对其做真实模糊（页面内容捕获进 GraphicsLayer）
        val surfaceColor = MiuixTheme.colorScheme.surface
        val backdrop = rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                BlurTopBar(
                    // 主页/分类页滚动感知；子页面常显背景
                    scrolled = if (currentPage() is SettingsPage.Home || currentPage() is SettingsPage.Category) topBarScrolled else true,
                    title = pageTitle(),
                    navigationIcon = {
                        if (isHome) {
                            IconButton(onClick = { drawerToggle() }) {
                                Icon(Lucide.Menu, "菜单", tint = MiuixTheme.colorScheme.onSurface)
                            }
                        } else {
                            IconButton(onClick = {
                                if (pageStack.size > 1) pageStack.removeAt(pageStack.lastIndex)
                                viewModel.navigateBack()
                            }) {
                                Icon(Lucide.ArrowLeft, "返回", tint = MiuixTheme.colorScheme.onSurface)
                            }
                        }
                    }
                )
            }
        ) { padding ->
            val bottomOverlayHeight = LocalGlobalPlayBarHeight.current

            if (showArtistSeparatorSheet) {
                ArtistSeparatorSheet(
                    separators = artistSeparators,
                    onDismiss = { showArtistSeparatorSheet = false },
                    onSave = { newSeparators ->
                        scope.launch {
                            settingsRepository.setArtistSeparators(newSeparators)
                        }
                    }
                )
            }

            currentSelectionItem?.let { item ->
                SelectionDialog(
                    item = item,
                    viewModel = viewModel,
                    onDismiss = { currentSelectionItem = null }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
            ) {
                when (currentPage()) {
                    SettingsPage.Home,
                    is SettingsPage.Category -> SettingsListContent(
                        viewModel = viewModel,
                        currentPage = currentPage(),
                        onCategoryClick = { key ->
                            pageStack.add(SettingsPage.Category(key))
                            viewModel.selectCategory(key)
                        },
                        onSettingItemClick = onSettingItemClick,
                        bottomOverlayHeight = bottomOverlayHeight,
                        topInset = padding.calculateTopPadding(),
                        onScrolledChange = { topBarScrolled = it }
                    )

                    SettingsPage.Background -> BackgroundSettingsContent(
                        viewModel = viewModel,
                        paddingValues = padding
                    )
                    SettingsPage.CodecCapabilities -> CodecCapabilitiesContent(
                        paddingValues = padding,
                        bottomOverlayHeight = bottomOverlayHeight
                    )
                    SettingsPage.AccompanistLyric -> AccompanistLyricSettingsContent(
                        paddingValues = padding,
                        bottomOverlayHeight = bottomOverlayHeight
                    )
                    SettingsPage.OpenSourceLicenses -> OpenSourceLicensesContent(
                        paddingValues = padding,
                        bottomOverlayHeight = bottomOverlayHeight
                    )
                    SettingsPage.PlayerBackground -> PlayerBackgroundPickerContent(
                        paddingValues = padding,
                        bottomOverlayHeight = bottomOverlayHeight
                    )
                }
            }
        }
    }
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
    onSettingItemClick: (SettingItem) -> Unit,
    bottomOverlayHeight: Dp,
    topInset: Dp = 0.dp,
    onScrolledChange: (Boolean) -> Unit = {}
) {
    val selectedCategory = (currentPage as? SettingsPage.Category)?.key
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    // 滚动感知：滚离顶部时通知父层（顶栏显示背景/模糊）
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }.collect(onScrolledChange)
    }
    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = topInset + 4.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (selectedCategory == null) {            items(buildCategories(), key = { it.key }) { category ->
                CategoryItem(
                    category = category,
                    onClick = { onCategoryClick(category.key) }
                )
            }
        } else {
            val category = buildCategories().find { it.key == selectedCategory }
            if (category != null) {
                category.items.forEach { item ->
                    item {
                        SettingItemRow(
                            item = item,
                            viewModel = viewModel,
                            onClick = onSettingItemClick
                        )
                    }
                    // 个性化页：主题色选择器紧跟"主题与颜色"分组标题
                    if (category.key == "personalize" && item.key.isEmpty() && item.title == "主题与颜色") {
                        item {
                            AccentColorPickerItem(viewModel = viewModel)
                        }
                    }
                }
            }
        }

        // 底部占位，让最后一项可以滚动到迷你播放条上方
        item {
            Spacer(modifier = Modifier.height(bottomOverlayHeight + 16.dp))
        }
    }
    // 滚动条：自动淡入淡出，可拖动定位
    LazyListScrollBar(
        listState = listState,
        modifier = Modifier.align(Alignment.CenterEnd)
    )
    }
}

@Composable
private fun AccentColorPickerItem(viewModel: SettingsViewModel) {
    val accentColorSetting by viewModel.getStringValue("accent_color", "default")
        .collectAsState(initial = "default")
    val currentColor = remember(accentColorSetting) {
        cn.lemondrop.fhreborn.ui.theme.parseAccentColor(accentColorSetting)
    }

    // OKHSV 滑杆状态（无 alpha 通道）
    val initialOkhsv = remember(currentColor) { Transforms.colorToOkhsv(currentColor) }
    var currentH by remember { mutableFloatStateOf(initialOkhsv[0]) }
    var currentS by remember { mutableFloatStateOf(initialOkhsv[1]) }
    var currentV by remember { mutableFloatStateOf(initialOkhsv[2]) }
    var hexInput by remember { mutableStateOf(formatHex(currentColor)) }

    // 外部颜色变化（HEX 输入生效/重置）时同步滑杆
    SideEffect {
        val external = currentColor.toArgb()
        val internal = Transforms.okhsvToColor(currentH, currentS, currentV).toArgb()
        if (external != internal) {
            val okhsv = Transforms.colorToOkhsv(currentColor)
            currentH = okhsv[0]
            currentS = okhsv[1]
            currentV = okhsv[2]
            hexInput = formatHex(currentColor)
        }
    }

    fun applyColor(color: Color) {
        viewModel.setStringSetting("accent_color", formatHex(color))
        hexInput = formatHex(color)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "主题色",
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        // 无 alpha 通道的 HSV 网格调色板（预览条移至 HEX 输入框左侧）
        FhColorPalette(
            color = currentColor,
            onColorChanged = { color ->
                applyColor(color)
            },
            showPreview = false
        )
        Spacer(modifier = Modifier.height(12.dp))
        OkHsvHueSlider(
            currentH = currentH,
            onHueChanged = {
                currentH = it
                applyColor(Transforms.okhsvToColor(currentH, currentS, currentV))
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OkHsvSaturationSlider(
            currentH = currentH,
            currentS = currentS,
            onSaturationChanged = {
                currentS = it
                applyColor(Transforms.okhsvToColor(currentH, currentS, currentV))
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OkHsvValueSlider(
            currentH = currentH,
            currentS = currentS,
            currentV = currentV,
            onValueChanged = {
                currentV = it
                applyColor(Transforms.okhsvToColor(currentH, currentS, currentV))
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        // HEX 输入（RRGGBB，可带 # 前缀），左侧为当前色预览块
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .background(currentColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            TextField(
                value = hexInput,
                onValueChange = { input ->
                    hexInput = input
                    val cleaned = input.trim().removePrefix("#")
                    val isValidHex = cleaned.length == 6 &&
                        cleaned.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
                    if (isValidHex) {
                        val parsed = cn.lemondrop.fhreborn.ui.theme.parseAccentColor("#$cleaned")
                        if (parsed.toArgb() != currentColor.toArgb()) {
                            viewModel.setStringSetting("accent_color", formatHex(parsed))
                        }
                    }
                },
                label = "HEX (RRGGBB)",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun formatHex(color: Color): String = String.format("#%06X", color.toArgb() and 0xFFFFFF)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArtistSeparatorSheet(
    separators: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    var current by remember { mutableStateOf(separators.toSortedSet()) }
    var input by remember { mutableStateOf("") }

    FhBottomSheet(
        show = true,
        onDismissRequest = onDismiss,
        title = "艺术家分隔符",
        backgroundColor = MiuixTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "用于拆分歌曲艺术家字段。例如添加 \" / \" 后，\"A / B\" 会被识别为两个艺术家 A 和 B。",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                current.forEach { sep ->
                    // 使用 Card 替代 InputChip
                    Card(
                        modifier = Modifier.clickable { }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = "\"$sep\"")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "删除",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        current = current.toMutableSet().apply { remove(sep) }.toSortedSet()
                                        onSave(current)
                                    },
                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    label = "添加分隔符"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (input.isNotBlank()) {
                            current = current.toMutableSet().apply { add(input) }.toSortedSet()
                            input = ""
                            onSave(current)
                        }
                    }
                ) {
                    Text("添加")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    current = setOf(" / ").toSortedSet()
                    onSave(current)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("恢复默认")
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: SettingCategory,
    onClick: () -> Unit
) {
    ArrowPreference(
        title = category.title,
        onClick = onClick,
        startAction = category.icon?.let {
            { Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(22.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary) }
        }
    )
}

@Composable
private fun SettingItemRow(
    item: SettingItem,
    viewModel: SettingsViewModel,
    onClick: (SettingItem) -> Unit
) {
    when (item.type) {
        is SettingType.Toggle -> {
            val toggleValue by viewModel.getToggleValue(item.key, item.defaultValue as? Boolean ?: false)
                .collectAsState(initial = item.defaultValue as? Boolean ?: false)
            SwitchPreference(
                checked = toggleValue,
                onCheckedChange = { viewModel.toggleSetting(item) },
                title = item.title,
                summary = item.description,
                startAction = item.icon?.let {
                    { Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(20.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary) }
                }
            )
        }
        is SettingType.Selection -> {
            val stringValue by viewModel.getStringValue(item.key, item.defaultValue as? String ?: "")
                .collectAsState(initial = item.defaultValue as? String ?: "")
            val options = (item.type as SettingType.Selection).options
            val selectedLabel = options.find { it.key == stringValue }?.label ?: stringValue
            ArrowPreference(
                title = item.title,
                summary = selectedLabel,
                onClick = { onClick(item) },
                startAction = item.icon?.let {
                    { Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(20.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary) }
                }
            )
        }
        is SettingType.Slider -> {
            val sliderValue by viewModel.getIntValue(item.key, (item.defaultValue as? Number)?.toInt() ?: 0)
                .collectAsState(initial = (item.defaultValue as? Number)?.toInt() ?: 0)
            val sliderType = item.type as SettingType.Slider
            SliderPreference(
                value = sliderValue.toFloat(),
                onValueChange = { viewModel.setIntSetting(item.key, it.toInt()) },
                title = item.title,
                summary = item.description,
                valueText = sliderValue.toString(),
                valueRange = sliderType.min..sliderType.max,
                steps = sliderType.steps,
                startAction = item.icon?.let {
                    { Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(20.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary) }
                }
            )
        }
        is SettingType.Navigation -> {
            ArrowPreference(
                title = item.title,
                summary = item.description,
                onClick = { onClick(item) },
                startAction = item.icon?.let {
                    { Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(20.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary) }
                }
            )
        }
        else -> {
            if (item.type is SettingType.Info && item.key.isEmpty()) {
                // 分组标题：使用 miuix 默认 SmallTitle（不自定义颜色）
                SmallTitle(text = item.title)
            } else {
                FhListItem(
                    title = item.title,
                    summary = item.defaultValue?.toString(),
                )
            }
        }
    }
}

@Composable
private fun SelectionDialog(
    item: SettingItem,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val stringValue by viewModel.getStringValue(item.key, item.defaultValue as? String ?: "")
        .collectAsState(initial = item.defaultValue as? String ?: "")
    val selectionType = item.type as? SettingType.Selection ?: return

    FhBottomSheet(
        show = true,
        onDismissRequest = onDismiss,
        title = item.title,
        backgroundColor = MiuixTheme.colorScheme.surfaceContainer,
        content = {
            selectionType.options.forEach { option ->
                RadioButtonPreference(
                    title = option.label,
                    selected = option.key == stringValue,
                    onClick = {
                        viewModel.setStringSetting(item.key, option.key)
                        onDismiss()
                    }
                )
            }
        }
    )
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
                SettingItem("dynamic_color", "Material You 动态取色", "跟随系统的壁纸取色使用monet取色", Lucide.Palette, SettingType.Toggle, false),

                // 主界面
                SettingItem("", "主界面", null, null, SettingType.Info),
                SettingItem("hide_system_ui", "隐藏状态栏和导航栏", "滑动状态栏/导航栏以显示", null, SettingType.Toggle, false),
                SettingItem("main_bg", "主页面背景", "纯色 / 自选图片 / 云母", null, SettingType.Navigation),
                SettingItem("player_bg", "播放器页面背景", "旋转流体 / AGSL 流体 / 封面模糊", null, SettingType.Navigation),

                // 播放器
                SettingItem("", "播放器", null, null, SettingType.Info),
                SettingItem("player_cover_corner_radius", "封面圆角", "播放器封面圆角大小", null, SettingType.Slider(0f, 32f, 16), 12),
                SettingItem("player_cover_rotating", "圆形旋转封面", "非正方形封面将裁切为方形显示", null, SettingType.Toggle, false),

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