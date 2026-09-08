package cn.lemondrop.fhreborn.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import cn.lemondrop.fhreborn.ui.theme.LocalAppDarkTheme
import cn.lemondrop.fhreborn.util.BackgroundImageUtils
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.blendColors
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.noiseDither
/**
 * 主页面共享背景层：纯色 / 自选图片（亮度 + 模糊）/ Mica。
 *
 * 只渲染背景，不含上层内容。各页面通过 scaffold 的 background 参数引入，
 * 统一背景以透到所有页面。
 */
@Composable
fun AppBackgroundLayer(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val bgRepo = remember { AppSettingsRepository(context) }
    val bgType by bgRepo.bgType.collectAsState(initial = "color")
    val bgColor by bgRepo.bgColor.collectAsState(initial = "")
    val bgImagePath by bgRepo.bgImagePath.collectAsState(initial = "")
    val bgImageBrightness by bgRepo.bgImageBrightness.collectAsState(initial = 100)
    val bgImageBlur by bgRepo.bgImageBlur.collectAsState(initial = 0)

    when (bgType) {
        "image" -> ImageBackground(
            bgImagePath = bgImagePath,
            bgImageBrightness = bgImageBrightness,
            bgImageBlur = bgImageBlur,
            modifier = modifier
        )
        "mica" -> MicaBackground(
            bgImagePath = bgImagePath,
            modifier = modifier
        )
        else -> {
            val solidColor = if (bgColor.isBlank()) {
                MiuixTheme.colorScheme.background
            } else {
                Color(android.graphics.Color.parseColor(bgColor))
            }
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(solidColor)
            )
        }
    }
}

@Composable
private fun ImageBackground(
    bgImagePath: String,
    bgImageBrightness: Int,
    bgImageBlur: Int,
    modifier: Modifier = Modifier
) {
    val bgBitmap = rememberBackgroundBitmap(bgImagePath)
    if (bgBitmap != null) {
        Image(
            bitmap = bgBitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .fillMaxSize()
                .blur(bgImageBlur.dp)
        )
        // 亮度压暗遮罩：100=不压暗，0=最暗（0.85）
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (100 - bgImageBrightness) / 100f * 0.85f))
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
        )
    }
}

/**
 * Mica 风格静态背景：底层图片只解码一次，再通过 Miuix textureBlur 做一次高模糊
 * 采样，并叠 85% 的浅色/深色表面。Android 13 以下回退到普通静态模糊。
 */
@Composable
private fun MicaBackground(
    bgImagePath: String,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppDarkTheme.current
    val surfaceColor = if (isDark) Color(0xFF0A0A0A) else Color.White
    val bgBitmap = rememberBackgroundBitmap(bgImagePath)

    if (bgBitmap == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(surfaceColor)
        )
        return
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val backdrop = rememberLayerBackdrop()
        val colors = BlurDefaults.blurColors(
            blendColors = listOf(
                BlendColorEntry(color = surfaceColor.copy(alpha = 0.85f))
            )
        )
        Box(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
            ) {
                Image(
                    bitmap = bgBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(0.dp) },
                        effects = {
                            noiseDither(BlurDefaults.NoiseCoefficient)
                            blur(160f * density, 160f * density)
                            blendColors(colors)
                        }
                    )
            )
        }
    } else {
        Image(
            bitmap = bgBitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .fillMaxSize()
                .blur(160.dp)
        )
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(surfaceColor.copy(alpha = 0.85f))
        )
    }
}

@Composable
private fun rememberBackgroundBitmap(path: String): ImageBitmap? {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bitmap = BackgroundImageUtils.loadBitmapFromPath(path)
    }
    return bitmap
}
