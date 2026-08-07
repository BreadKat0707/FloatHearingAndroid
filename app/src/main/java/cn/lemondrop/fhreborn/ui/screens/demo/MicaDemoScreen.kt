package cn.lemondrop.fhreborn.ui.screens.demo

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import cn.lemondrop.fhreborn.WallpaperProbeActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MicaDemoScreen(
    onBack: () -> Unit
) {
    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues()
    val cutoutLeft = cutoutPadding.calculateLeftPadding(LayoutDirection.Ltr)
    val cutoutRight = cutoutPadding.calculateRightPadding(LayoutDirection.Ltr)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp + cutoutLeft,
                    end = 16.dp + cutoutRight,
                    top = 16.dp
                )
        ) {
            // 顶部返回
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Lucide.ArrowLeft,
                    contentDescription = "返回",
                    tint = MiuixTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Mica / Mica Alt",
                style = MiuixTheme.textStyles.title1,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // 实验入口：windowShowWallpaper 直透壁纸（独立透明 Activity）
            val context = LocalContext.current
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable {
                        context.startActivity(Intent(context, WallpaperProbeActivity::class.java))
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "▶ 打开「windowShowWallpaper 直透壁纸」实验",
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = "壁纸获取方式对比",
                style = MiuixTheme.textStyles.title2,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val drawableWallpaper = androidx.compose.runtime.remember { null as androidx.compose.ui.graphics.ImageBitmap? }
            MicaSampleCard(
                title = "getDrawable()",
                description = "绑定式获取，返回当前壁纸 Drawable。"
            ) {
                WallpaperPreview(drawableWallpaper)
            }

            Spacer(modifier = Modifier.height(16.dp))

            val peekWallpaper = androidx.compose.runtime.remember { null as androidx.compose.ui.graphics.ImageBitmap? }
            MicaSampleCard(
                title = "peekDrawable()",
                description = "非绑定式获取，不会触发动态壁纸重新绑定。"
            ) {
                WallpaperPreview(peekWallpaper)
            }

            Spacer(modifier = Modifier.height(16.dp))

            val fastWallpaper = androidx.compose.runtime.remember { null as androidx.compose.ui.graphics.ImageBitmap? }
            MicaSampleCard(
                title = "getFastDrawable()",
                description = "API 24+，更轻量的壁纸获取方式。"
            ) {
                WallpaperPreview(fastWallpaper)
            }

            Spacer(modifier = Modifier.height(24.dp))

            MicaSampleCard(
                title = "Mica",
                description = "以桌面壁纸为基底，重度模糊后叠加上主题色 tint 与噪点。"
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MiuixTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                ) {
                    DemoContent("Mica 背景区域")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            MicaSampleCard(
                title = "Mica Alt",
                description = "同样基于壁纸模糊，但主题色 tint 与噪点更强，视觉层次更重。"
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MiuixTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                ) {
                    DemoContent("Mica Alt 背景区域")
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun MicaSampleCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        style = MiuixTheme.textStyles.title2,
        color = MiuixTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    Text(
        text = description,
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        content()
    }
}

@Composable
private fun WallpaperPreview(wallpaper: androidx.compose.ui.graphics.ImageBitmap?) {
    if (wallpaper != null) {
        Image(
            bitmap = wallpaper,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "未获取到壁纸",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.title3
            )
        }
    }
}

@Composable
private fun DemoContent(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(20.dp),
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurface
            )
        }
    }
}