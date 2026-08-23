package cn.lemondrop.fhreborn.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import cn.lemondrop.fhreborn.util.BackgroundImageUtils

/**
 * 主页面共享背景层：纯色 / 自选图片（亮度 + 模糊）。
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
        "image" -> {
            val bgBitmap = remember(bgImagePath) { BackgroundImageUtils.loadBitmapFromPath(bgImagePath) }
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
