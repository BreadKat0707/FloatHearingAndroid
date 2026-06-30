package cn.lemondrop.fhreborn

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lemondrop.clover.material.CloverWallpaperMica
import java.util.function.Consumer

/**
 * 验证 clover-ui 的 [CloverWallpaperMica]：系统壁纸实时透出 + 跨窗口模糊 + tint/噪点。
 *
 * 本 Activity 使用 [R.style.Theme_FloatHearing_WallpaperProbe]
 * （android:windowShowWallpaper=true + 透明 windowBackground）提供“壁纸透出”这一窗口级前提，
 * 模糊与材质叠加全部交给 CloverWallpaperMica。
 */
class WallpaperProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                WallpaperProbeScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun WallpaperProbeScreen(onBack: () -> Unit) {
    val view = LocalView.current
    var radiusDp by remember { mutableFloatStateOf(80f) }
    var isAlt by remember { mutableStateOf(false) }
    var blurSupported by remember { mutableStateOf(false) }

    // 设备“跨窗口模糊”是否启用（GPU / 省电模式 / 系统设置）
    DisposableEffect(view) {
        val wm = view.context.getSystemService(WindowManager::class.java)
        blurSupported = wm?.isCrossWindowBlurEnabled == true
        val listener = Consumer<Boolean> { enabled -> blurSupported = enabled }
        wm?.addCrossWindowBlurEnabledListener(listener)
        onDispose { wm?.removeCrossWindowBlurEnabledListener(listener) }
    }

    // clover-ui 的系统壁纸实时 Mica 作为整页背景
    CloverWallpaperMica(
        modifier = Modifier.fillMaxSize(),
        isAlt = isAlt,
        blurRadius = radiusDp.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoCard {
                Text(
                    text = "CloverWallpaperMica 实验",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (blurSupported) {
                        "设备跨窗口模糊：已启用 ✓  这就是系统壁纸 + 模糊 + tint + 噪点的成套 Mica。"
                    } else {
                        "设备跨窗口模糊：未启用 ✗  此时只透出壁纸、不模糊（仍有 tint + 噪点）。"
                    },
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                InfoCard {
                    Text(
                        text = "背后 = 模糊后的系统壁纸 + 主题 tint + 噪点",
                        color = Color.White
                    )
                }
            }

            // 控制面板
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(16.dp)
            ) {
                Row {
                    Chip("Mica", !isAlt) { isAlt = false }
                    Spacer(Modifier.width(12.dp))
                    Chip("Mica Alt", isAlt) { isAlt = true }
                }
                Spacer(Modifier.height(12.dp))
                Text(text = "模糊半径：${radiusDp.toInt()} dp", color = Color.White)
                Slider(
                    value = radiusDp,
                    onValueChange = { radiusDp = it },
                    valueRange = 0f..150f
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable { onBack() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "返回", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color.White.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(16.dp),
        content = content
    )
}
