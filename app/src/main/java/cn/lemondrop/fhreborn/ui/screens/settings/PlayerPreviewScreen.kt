package cn.lemondrop.fhreborn.ui.screens.settings

import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import cn.lemondrop.fhreborn.ui.components.FhBottomSheet
import cn.lemondrop.fhreborn.ui.screens.player.PlayerBackground
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Repeat
import com.composables.icons.lucide.Shuffle
import com.composables.icons.lucide.SkipBack
import com.composables.icons.lucide.SkipForward
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

// ==================== 混合模式 ====================
private val BlendModes = listOf(
    "SrcOver" to "正常", "Multiply" to "正片叠底", "Screen" to "滤色",
    "Overlay" to "叠加", "SoftLight" to "柔光", "HardLight" to "强光",
    "Lighter" to "变亮", "PlusLighter" to "浅色加", "Darken" to "变暗", "PlusDarker" to "深色加"
)

private fun parseBlendMode(mode: String): BlendMode = when (mode) {
    "Multiply" -> BlendMode.Multiply; "Screen" -> BlendMode.Screen
    "Overlay" -> BlendMode.Overlay; "SoftLight" -> BlendMode.Softlight
    "HardLight" -> BlendMode.Hardlight; "Lighter" -> BlendMode.Lighten
    "PlusLighter" -> BlendMode.Plus; "Darken" -> BlendMode.Darken
    "PlusDarker" -> BlendMode.Darken; else -> BlendMode.SrcOver
}

// ==================== 元素数据 ====================
private data class ElementData(
    val index: Int, val name: String,
    val getAlpha: AppSettingsRepository.(Boolean) -> kotlinx.coroutines.flow.Flow<Int>,
    val getBlend: AppSettingsRepository.(Boolean) -> kotlinx.coroutines.flow.Flow<String>,
    val setAlpha: suspend AppSettingsRepository.(Boolean, Int) -> Unit,
    val setBlend: suspend AppSettingsRepository.(Boolean, String) -> Unit
)

private val Elements = listOf(
    ElementData(1, "拖拽手柄",
        { dark -> if (dark) this.playerEl01HandleAlphaDark else this.playerEl01HandleAlpha },
        { dark -> if (dark) this.playerEl01HandleBlendDark else this.playerEl01HandleBlend },
        { dark, v -> if (dark) this.setPlayerEl01HandleAlphaDark(v) else this.setPlayerEl01HandleAlpha(v) },
        { dark, v -> if (dark) this.setPlayerEl01HandleBlendDark(v) else this.setPlayerEl01HandleBlend(v) }
    ),
    ElementData(2, "歌名",
        { dark -> if (dark) this.playerEl02TitleAlphaDark else this.playerEl02TitleAlpha },
        { dark -> if (dark) this.playerEl02TitleBlendDark else this.playerEl02TitleBlend },
        { dark, v -> if (dark) this.setPlayerEl02TitleAlphaDark(v) else this.setPlayerEl02TitleAlpha(v) },
        { dark, v -> if (dark) this.setPlayerEl02TitleBlendDark(v) else this.setPlayerEl02TitleBlend(v) }
    ),
    ElementData(3, "专辑 - 艺术家",
        { dark -> if (dark) this.playerEl03ArtistAlphaDark else this.playerEl03ArtistAlpha },
        { dark -> if (dark) this.playerEl03ArtistBlendDark else this.playerEl03ArtistBlend },
        { dark, v -> if (dark) this.setPlayerEl03ArtistAlphaDark(v) else this.setPlayerEl03ArtistAlpha(v) },
        { dark, v -> if (dark) this.setPlayerEl03ArtistBlendDark(v) else this.setPlayerEl03ArtistBlend(v) }
    ),
    ElementData(4, "小歌词",
        { dark -> if (dark) this.playerEl04MiniLyricAlphaDark else this.playerEl04MiniLyricAlpha },
        { dark -> if (dark) this.playerEl04MiniLyricBlendDark else this.playerEl04MiniLyricBlend },
        { dark, v -> if (dark) this.setPlayerEl04MiniLyricAlphaDark(v) else this.setPlayerEl04MiniLyricAlpha(v) },
        { dark, v -> if (dark) this.setPlayerEl04MiniLyricBlendDark(v) else this.setPlayerEl04MiniLyricBlend(v) }
    ),
    ElementData(5, "进度条底轨",
        { dark -> if (dark) this.playerEl05TrackAlphaDark else this.playerEl05TrackAlpha },
        { dark -> if (dark) this.playerEl05TrackBlendDark else this.playerEl05TrackBlend },
        { dark, v -> if (dark) this.setPlayerEl05TrackAlphaDark(v) else this.setPlayerEl05TrackAlpha(v) },
        { dark, v -> if (dark) this.setPlayerEl05TrackBlendDark(v) else this.setPlayerEl05TrackBlend(v) }
    ),
    ElementData(6, "进度条进度",
        { dark -> if (dark) this.playerEl06FillAlphaDark else this.playerEl06FillAlpha },
        { dark -> if (dark) this.playerEl06FillBlendDark else this.playerEl06FillBlend },
        { dark, v -> if (dark) this.setPlayerEl06FillAlphaDark(v) else this.setPlayerEl06FillAlpha(v) },
        { dark, v -> if (dark) this.setPlayerEl06FillBlendDark(v) else this.setPlayerEl06FillBlend(v) }
    ),
    ElementData(7, "进度条时间",
        { dark -> if (dark) this.playerEl07TimeAlphaDark else this.playerEl07TimeAlpha },
        { dark -> if (dark) this.playerEl07TimeBlendDark else this.playerEl07TimeBlend },
        { dark, v -> if (dark) this.setPlayerEl07TimeAlphaDark(v) else this.setPlayerEl07TimeAlpha(v) },
        { dark, v -> if (dark) this.setPlayerEl07TimeBlendDark(v) else this.setPlayerEl07TimeBlend(v) }
    ),
    ElementData(8, "播控图标",
        { dark -> if (dark) this.playerEl08ControlsAlphaDark else this.playerEl08ControlsAlpha },
        { dark -> if (dark) this.playerEl08ControlsBlendDark else this.playerEl08ControlsBlend },
        { dark, v -> if (dark) this.setPlayerEl08ControlsAlphaDark(v) else this.setPlayerEl08ControlsAlpha(v) },
        { dark, v -> if (dark) this.setPlayerEl08ControlsBlendDark(v) else this.setPlayerEl08ControlsBlend(v) }
    ),
    ElementData(9, "播放模式",
        { dark -> if (dark) this.playerEl09ModeAlphaDark else this.playerEl09ModeAlpha },
        { dark -> if (dark) this.playerEl09ModeBlendDark else this.playerEl09ModeBlend },
        { dark, v -> if (dark) this.setPlayerEl09ModeAlphaDark(v) else this.setPlayerEl09ModeAlpha(v) },
        { dark, v -> if (dark) this.setPlayerEl09ModeBlendDark(v) else this.setPlayerEl09ModeBlend(v) }
    ),
    ElementData(10, "底部图标",
        { dark -> if (dark) this.playerEl10BottomAlphaDark else this.playerEl10BottomAlpha },
        { dark -> if (dark) this.playerEl10BottomBlendDark else this.playerEl10BottomBlend },
        { dark, v -> if (dark) this.setPlayerEl10BottomAlphaDark(v) else this.setPlayerEl10BottomAlpha(v) },
        { dark, v -> if (dark) this.setPlayerEl10BottomBlendDark(v) else this.setPlayerEl10BottomBlend(v) }
    )
)

private fun highlightMod(mod: Modifier, editing: Boolean, index: Int, editingElement: Int): Modifier {
    if (!editing) return mod
    return if (index == editingElement) mod.border(2.dp, Color(0xFF667EEA), RoundedCornerShape(4.dp))
    else mod.graphicsLayer { alpha = 0.3f }
}

// ==================== 主入口 ====================
@Composable
fun PlayerPreviewContent(paddingValues: PaddingValues, bottomOverlayHeight: Dp) {
    var showModeSheet by remember { mutableStateOf(true) }
    var selectedMode by remember { mutableIntStateOf(-1) }

    if (showModeSheet && selectedMode == -1) {
        FhBottomSheet(show = true, onDismissRequest = { }, title = "选择预览模式") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeOption("浅色模式", "在浅色背景下预览播放器外观") { selectedMode = 0; showModeSheet = false }
                ModeOption("深色模式", "在深色背景下预览播放器外观") { selectedMode = 1; showModeSheet = false }
            }
        }
    }

    if (selectedMode >= 0) {
        var editingElement by remember { mutableIntStateOf(-1) }
        PlayerPreviewPage(
            isDark = selectedMode == 1, editingElement = editingElement,
            onElementClick = { editingElement = if (editingElement == it) -1 else it },
            onCoverClick = { editingElement = if (editingElement == 11) -1 else 11 },
            onBack = { selectedMode = -1; showModeSheet = true; editingElement = -1 },
            paddingValues = paddingValues
        )
    }
}

@Composable
private fun ModeOption(title: String, desc: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(MiuixTheme.colorScheme.surfaceContainerHighest)
        .clickable(onClick = onClick).padding(16.dp)) {
        Text(text = title, style = MiuixTheme.textStyles.body1, color = MiuixTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(text = desc, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

// ==================== 播放器预览页 ====================
@Composable
private fun PlayerPreviewPage(
    isDark: Boolean, editingElement: Int,
    onElementClick: (Int) -> Unit, onCoverClick: () -> Unit,
    onBack: () -> Unit, paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val repo = remember { AppSettingsRepository(context) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 读取元素设置
    val elAlpha = remember { mutableMapOf<Int, androidx.compose.runtime.State<Int>>() }
    val elBlend = remember { mutableMapOf<Int, androidx.compose.runtime.State<String>>() }
    for (el in Elements) {
        elAlpha[el.index] = el.getAlpha(repo, isDark).collectAsState(initial = 100)
        elBlend[el.index] = el.getBlend(repo, isDark).collectAsState(initial = "SrcOver")
    }

    // 封面设置
    val coverCornerRadius by repo.playerCoverCornerRadius.collectAsState(initial = 12)
    val coverShadowY by repo.playerCoverShadowY.collectAsState(initial = 16)
    val coverShadowAlpha by repo.playerCoverShadowAlpha.collectAsState(initial = 40)
    val coverShadowBlur by repo.playerCoverShadowBlur.collectAsState(initial = 20)
    var isPaused by remember { mutableStateOf(false) }
    val coverScale by animateFloatAsState(if (isPaused) 0.92f else 1f, tween(300, easing = FastOutSlowInEasing), label = "scale")

    val fluidOnColor = if (isDark) Color.White else Color.Black
    val fluidOnColorHint = fluidOnColor.copy(alpha = 0.5f)
    val editing = editingElement >= 0

    // 系统栏 padding
    val statusBarPadding = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues()
    val statusBarHeight = statusBarPadding.calculateTopPadding()
    val navBarHeight = navBarPadding.calculateBottomPadding()

    // 播放器布局（与真实播放器完全相同的结构）
    Box(Modifier.fillMaxSize()) {
        // 背景：使用 PlayerBackground（Apple Music 流体背景）
        PlayerBackground(
            songId = null, // 使用默认封面/流体效果
            isPlaying = !isPaused,
            modifier = Modifier.fillMaxSize()
        )

        // 返回按钮（左上角）
        Text(text = "← 返回", color = fluidOnColor, modifier = Modifier.padding(start = 16.dp, top = statusBarHeight + 8.dp).clickable { onBack() })

        // 完成按钮（右上角）
        if (editing) {
            Text(text = "完成", color = Color(0xFF667EEA), modifier = Modifier.padding(end = 16.dp, top = statusBarHeight + 8.dp)
                .align(Alignment.TopEnd).clickable { onElementClick(-1) })
        }

        // 主内容区
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(top = statusBarHeight, bottom = navBarHeight)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp)) // 返回按钮空间

            // 拖拽手柄 (元素1)
            val pe01a = elAlpha[1]?.value ?: 100; val pe01b = elBlend[1]?.value ?: "SrcOver"
            if (!editing || editingElement == 1) {
                Box(highlightMod(Modifier.clickable { onElementClick(1) }, editing, 1, editingElement)) {
                    Box(Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp))
                        .graphicsLayer { alpha = pe01a / 100f; blendMode = parseBlendMode(pe01b) }
                        .background(fluidOnColor))
                }
            }
            Spacer(Modifier.height(20.dp))

            // 封面 (元素11)
            if (!editing || editingElement == 11) {
                Box(highlightMod(Modifier.clickable { onCoverClick() }, editing, 11, editingElement)
                    .graphicsLayer { scaleX = coverScale; scaleY = coverScale }
                    .shadow(coverShadowBlur.dp, RoundedCornerShape(coverCornerRadius.dp),
                        ambientColor = Color.Black.copy(alpha = coverShadowAlpha / 100f),
                        spotColor = Color.Black.copy(alpha = coverShadowAlpha / 100f))
                    .clip(RoundedCornerShape(coverCornerRadius.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF667EEA), Color(0xFF764BA2), Color(0xFF6B73FF))))) {
                    Icon(Lucide.Music, null, Modifier.fillMaxSize().padding(78.dp), Color.White.copy(alpha = 0.7f))
                }
            }
            Spacer(Modifier.height(18.dp))

            // 歌名 (元素2)
            val pe02a = elAlpha[2]?.value ?: 100; val pe02b = elBlend[2]?.value ?: "SrcOver"
            if (!editing || editingElement == 2) {
                Text(text = "示例歌曲名称", color = fluidOnColor, style = MiuixTheme.textStyles.headline2,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    modifier = highlightMod(Modifier.fillMaxWidth().padding(horizontal = 24.dp).clickable { onElementClick(2) }, editing, 2, editingElement)
                        .graphicsLayer { alpha = pe02a / 100f; blendMode = parseBlendMode(pe02b) })
            }
            Spacer(Modifier.height(4.dp))

            // 艺术家 (元素3)
            val pe03a = elAlpha[3]?.value ?: 100; val pe03b = elBlend[3]?.value ?: "SrcOver"
            if (!editing || editingElement == 3) {
                Text(text = "示例艺术家 / 示例专辑", color = fluidOnColorHint, style = MiuixTheme.textStyles.body1,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    modifier = highlightMod(Modifier.fillMaxWidth().padding(horizontal = 24.dp).clickable { onElementClick(3) }, editing, 3, editingElement)
                        .graphicsLayer { alpha = pe03a / 100f; blendMode = parseBlendMode(pe03b) })
            }
            Spacer(Modifier.height(10.dp))

            // 歌词 (元素4)
            val pe04a = elAlpha[4]?.value ?: 100; val pe04b = elBlend[4]?.value ?: "SrcOver"
            if (!editing || editingElement == 4) {
                Column(highlightMod(Modifier.fillMaxWidth().padding(horizontal = 24.dp).clickable { onElementClick(4) }, editing, 4, editingElement)
                    .graphicsLayer { alpha = pe04a / 100f; blendMode = parseBlendMode(pe04b) },
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "这是歌词预览效果", color = fluidOnColor, style = MiuixTheme.textStyles.body1, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(2.dp))
                    Text(text = "第二行歌词内容", color = fluidOnColorHint, style = MiuixTheme.textStyles.body1, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(10.dp))

            // 进度条 (元素5/6/7)
            val pe05a = elAlpha[5]?.value ?: 50; val pe05b = elBlend[5]?.value ?: "SrcOver"
            val pe06a = elAlpha[6]?.value ?: 100; val pe06b = elBlend[6]?.value ?: "SrcOver"
            val pe07a = elAlpha[7]?.value ?: 100; val pe07b = elBlend[7]?.value ?: "SrcOver"
            Column(highlightMod(Modifier.fillMaxWidth().padding(horizontal = 24.dp).clickable { onElementClick(5) }, editing, 5, editingElement).padding(vertical = 4.dp)) {
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                    .graphicsLayer { alpha = pe05a / 100f; blendMode = parseBlendMode(pe05b) }
                    .background(fluidOnColor.copy(alpha = 0.2f))) {
                    Box(Modifier.fillMaxWidth(0.37f).fillMaxSize().clickable { onElementClick(6) }
                        .graphicsLayer { alpha = pe06a / 100f; blendMode = parseBlendMode(pe06b) }
                        .background(fluidOnColor))
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp).clickable { onElementClick(7) }
                    .graphicsLayer { alpha = pe07a / 100f; blendMode = parseBlendMode(pe07b) },
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "1:23", color = fluidOnColorHint, style = MiuixTheme.textStyles.footnote1)
                    Text(text = "3:45", color = fluidOnColorHint, style = MiuixTheme.textStyles.footnote1)
                }
            }
            Spacer(Modifier.height(14.dp))

            // 播控 (元素8/9)
            val pe08a = elAlpha[8]?.value ?: 100; val pe08b = elBlend[8]?.value ?: "SrcOver"
            val pe09a = elAlpha[9]?.value ?: 100; val pe09b = elBlend[9]?.value ?: "SrcOver"
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                Icon(Lucide.Shuffle, null, highlightMod(Modifier.size(28.dp).clickable { onElementClick(9) }, editing, 9, editingElement)
                    .graphicsLayer { alpha = pe09a / 100f; blendMode = parseBlendMode(pe09b) }, fluidOnColorHint)
                Row(highlightMod(Modifier.clickable { onElementClick(8) }, editing, 8, editingElement)
                    .graphicsLayer { alpha = pe08a / 100f; blendMode = parseBlendMode(pe08b) },
                    horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.SkipBack, null, Modifier.size(32.dp), fluidOnColor)
                    Icon(if (isPaused) Lucide.Play else Lucide.Pause, null,
                        Modifier.size(48.dp).clip(CircleShape).background(fluidOnColor.copy(alpha = 0.1f))
                            .clickable { isPaused = !isPaused }.padding(8.dp), fluidOnColor)
                    Icon(Lucide.SkipForward, null, Modifier.size(32.dp), fluidOnColor)
                }
                Icon(Lucide.Repeat, null, highlightMod(Modifier.size(28.dp).clickable { onElementClick(9) }, editing, 9, editingElement)
                    .graphicsLayer { alpha = pe09a / 100f; blendMode = parseBlendMode(pe09b) }, fluidOnColorHint)
            }
            Spacer(Modifier.height(10.dp))

            // 底部图标 (元素10)
            val pe10a = elAlpha[10]?.value ?: 100; val pe10b = elBlend[10]?.value ?: "SrcOver"
            Row(highlightMod(Modifier.fillMaxWidth().padding(horizontal = 32.dp).clickable { onElementClick(10) }, editing, 10, editingElement)
                .graphicsLayer { alpha = pe10a / 100f; blendMode = parseBlendMode(pe10b) },
                horizontalArrangement = Arrangement.SpaceEvenly) {
                Icon(Lucide.ListMusic, null, Modifier.size(24.dp), fluidOnColorHint)
                Icon(Lucide.Music, null, Modifier.size(24.dp), fluidOnColorHint)
                Icon(Lucide.ListMusic, null, Modifier.size(24.dp), fluidOnColorHint)
            }
            Spacer(Modifier.height(navBarHeight + 24.dp))
        }

        // 编辑面板（底部弹出，覆盖在播放器上方）
        AnimatedVisibility(visible = editing, enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)) {
            Column(Modifier.fillMaxWidth().heightIn(max = 350.dp)
                .background(Color(0xFF2A2A2A).copy(alpha = 0.95f))
                .verticalScroll(rememberScrollState()).padding(16.dp)) {
                if (editingElement in 1..10) {
                    val el = Elements.find { it.index == editingElement }!!
                    ElementEditor(el, isDark, repo, scope)
                } else if (editingElement == 11) {
                    CoverEditor(repo, scope)
                }
            }
        }
    }
}

// ==================== 编辑器 ====================
@Composable
private fun ElementEditor(el: ElementData, isDark: Boolean, repo: AppSettingsRepository, scope: CoroutineScope) {
    val alpha by el.getAlpha(repo, isDark).collectAsState(initial = 100)
    val blend by el.getBlend(repo, isDark).collectAsState(initial = "SrcOver")
    val blendLabel = BlendModes.find { it.first == blend }?.second ?: blend

    Text(text = el.name, style = MiuixTheme.textStyles.headline2, color = Color.White)
    Spacer(Modifier.height(16.dp))
    Text(text = "透明度", style = MiuixTheme.textStyles.body2, color = Color.Gray)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Slider(value = alpha.toFloat(), onValueChange = { scope.launch { el.setAlpha(repo, isDark, it.toInt()) } },
            valueRange = 0f..100f, steps = 99, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(text = "${alpha}%", color = Color.White)
    }
    Spacer(Modifier.height(16.dp))
    Text(text = "混合模式", style = MiuixTheme.textStyles.body2, color = Color.Gray)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BlendModes.forEach { (key, label) ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .clickable { scope.launch { el.setBlend(repo, isDark, key) } }
                .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(16.dp).clip(CircleShape)
                    .border(2.dp, if (blend == key) Color(0xFF667EEA) else Color.Gray, CircleShape)
                    .then(if (blend == key) Modifier.background(Color(0xFF667EEA)) else Modifier))
                Spacer(Modifier.width(12.dp))
                Text(text = label, color = Color.White)
            }
        }
    }
}

@Composable
private fun CoverEditor(repo: AppSettingsRepository, scope: CoroutineScope) {
    val cornerRadius by repo.playerCoverCornerRadius.collectAsState(initial = 12)
    val shadowY by repo.playerCoverShadowY.collectAsState(initial = 16)
    val shadowAlpha by repo.playerCoverShadowAlpha.collectAsState(initial = 40)
    val shadowBlur by repo.playerCoverShadowBlur.collectAsState(initial = 20)
    val pauseScale by repo.playerCoverPauseScale.collectAsState(initial = 92)

    Text(text = "封面设置", style = MiuixTheme.textStyles.headline2, color = Color.White)
    Spacer(Modifier.height(16.dp))
    InlineSlider("圆角", cornerRadius.toFloat(), 0f..32f, 31, "${cornerRadius}dp") { scope.launch { repo.setPlayerCoverCornerRadius(it.toInt()) } }
    InlineSlider("投影 Y 偏移", shadowY.toFloat(), -30f..60f, 89, "${shadowY}dp") { scope.launch { repo.setPlayerCoverShadowY(it.toInt()) } }
    InlineSlider("投影浓度", shadowAlpha.toFloat(), 0f..100f, 99, "${shadowAlpha}%") { scope.launch { repo.setPlayerCoverShadowAlpha(it.toInt()) } }
    InlineSlider("投影模糊", shadowBlur.toFloat(), 0f..60f, 59, "${shadowBlur}dp") { scope.launch { repo.setPlayerCoverShadowBlur(it.toInt()) } }
    InlineSlider("暂停缩小", pauseScale.toFloat(), 50f..100f, 49, "${pauseScale}%") { scope.launch { repo.setPlayerCoverPauseScale(it.toInt()) } }
}

@Composable
private fun InlineSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, text: String, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, style = MiuixTheme.textStyles.body1, color = Color.White, modifier = Modifier.weight(1f))
            Text(text = text, style = MiuixTheme.textStyles.body2, color = Color.Gray)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}
