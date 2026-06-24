package cn.lemondrop.fhreborn.ui.screens.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cn.lemondrop.clover.CloverMica
import cn.lemondrop.clover.CloverMicaAlt
import cn.lemondrop.clover.WallpaperLoadStrategy
import cn.lemondrop.clover.rememberWallpaperBitmap
import cn.lemondrop.fhreborn.ui.theme.FluentIconButton
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text

@Composable
fun MicaDemoScreen(
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
        ) {
            // 顶部返回
            FluentIconButton(onClick = onBack) {
                Icon(
                    imageVector = Lucide.ArrowLeft,
                    contentDescription = "返回",
                    modifier = Modifier.padding(4.dp)
                )
            }

            Text(
                text = "Mica / Mica Alt",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Text(
                text = "壁纸获取方式对比",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val drawableWallpaper = rememberWallpaperBitmap(WallpaperLoadStrategy.Drawable)
            MicaSampleCard(
                title = "getDrawable()",
                description = "绑定式获取，返回当前壁纸 Drawable。"
            ) {
                WallpaperPreview(drawableWallpaper)
            }

            Spacer(modifier = Modifier.height(16.dp))

            val peekWallpaper = rememberWallpaperBitmap(WallpaperLoadStrategy.Peek)
            MicaSampleCard(
                title = "peekDrawable()",
                description = "非绑定式获取，不会触发动态壁纸重新绑定。"
            ) {
                WallpaperPreview(peekWallpaper)
            }

            Spacer(modifier = Modifier.height(16.dp))

            val fastWallpaper = rememberWallpaperBitmap(WallpaperLoadStrategy.Fast)
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
                CloverMica(modifier = Modifier.fillMaxSize()) {
                    DemoContent("Mica 背景区域")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            MicaSampleCard(
                title = "Mica Alt",
                description = "同样基于壁纸模糊，但主题色 tint 与噪点更强，视觉层次更重。"
            ) {
                CloverMicaAlt(modifier = Modifier.fillMaxSize()) {
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
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium
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
            modifier = Modifier.padding(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
            )
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
