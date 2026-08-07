package cn.lemondrop.fhreborn.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 开源许可内容（纯内容组件，不带外壳）：列出本项目使用的第三方开源库及其许可证。
 */
@Composable
fun OpenSourceLicensesContent(
    paddingValues: PaddingValues,
    bottomOverlayHeight: Dp
) {
    val context = LocalContext.current
    val licenses = rememberOpenSourceLicenses()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = paddingValues.calculateTopPadding(),
                start = 16.dp,
                end = 16.dp
            ),
        contentPadding = PaddingValues(bottom = bottomOverlayHeight + 16.dp)
    ) {
        item {
            Text(
                text = "FH Reborn 使用了以下开源项目，感谢所有贡献者。",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

        items(licenses, key = { it.name }) { license ->
            LicenseItem(
                license = license,
                onClick = {
                    // 尝试打开项目主页（浏览器）
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(license.url)
                    )
                    runCatching { context.startActivity(intent) }
                }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun LicenseItem(
    license: OpenSourceLicense,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = license.name,
            style = MiuixTheme.textStyles.title3,
            color = MiuixTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${license.version} · ${license.license}",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        if (license.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = license.description,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
    }
}

private data class OpenSourceLicense(
    val name: String,
    val version: String,
    val license: String,
    val url: String,
    val description: String = ""
)

@Composable
private fun rememberOpenSourceLicenses(): List<OpenSourceLicense> {
    return remember {
        listOf(
            OpenSourceLicense(
                name = "Android Jetpack Compose",
                version = "BOM 2026.02.01",
                license = "Apache-2.0",
                url = "https://github.com/androidx/androidx/tree/androidx-main/compose",
                description = "声明式 UI 框架（含 Material3、Animation、UI、Tooling）"
            ),
            OpenSourceLicense(
                name = "AndroidX Core KTX",
                version = "1.15.0",
                license = "Apache-2.0",
                url = "https://github.com/androidx/androidx/tree/androidx-main/core",
                description = "Android 核心库 Kotlin 扩展"
            ),
            OpenSourceLicense(
                name = "AndroidX Lifecycle",
                version = "2.10.0",
                license = "Apache-2.0",
                url = "https://github.com/androidx/androidx/tree/androidx-main/lifecycle",
                description = "生命周期感知组件"
            ),
            OpenSourceLicense(
                name = "AndroidX Activity Compose",
                version = "1.13.0",
                license = "Apache-2.0",
                url = "https://github.com/androidx/androidx/tree/androidx-main/activity",
                description = "Compose 与 Activity 集成"
            ),
            OpenSourceLicense(
                name = "AndroidX Navigation Compose",
                version = "2.9.0",
                license = "Apache-2.0",
                url = "https://github.com/androidx/androidx/tree/androidx-main/navigation",
                description = "Compose 导航组件"
            ),
            OpenSourceLicense(
                name = "AndroidX Room",
                version = "2.7.1",
                license = "Apache-2.0",
                url = "https://github.com/androidx/androidx/tree/androidx-main/room",
                description = "本地 SQLite 对象映射库"
            ),
            OpenSourceLicense(
                name = "AndroidX DataStore",
                version = "1.1.6",
                license = "Apache-2.0",
                url = "https://github.com/androidx/androidx/tree/androidx-main/datastore",
                description = "类型安全偏好设置存储"
            ),
            OpenSourceLicense(
                name = "Media3",
                version = "1.7.0",
                license = "Apache-2.0",
                url = "https://github.com/androidx/media",
                description = "ExoPlayer 播放核心与 MediaSession"
            ),
            OpenSourceLicense(
                name = "Kotlin",
                version = "2.2.10",
                license = "Apache-2.0",
                url = "https://github.com/JetBrains/kotlin",
                description = "Kotlin 标准库与编译器"
            ),
            OpenSourceLicense(
                name = "Kotlin Coroutines",
                version = "1.10.2",
                license = "Apache-2.0",
                url = "https://github.com/Kotlin/kotlinx.coroutines",
                description = "异步协程支持"
            ),
            OpenSourceLicense(
                name = "Kotlin Symbol Processing (KSP)",
                version = "2.2.10-2.0.2",
                license = "Apache-2.0",
                url = "https://github.com/google/ksp",
                description = "Room 等注解处理工具"
            ),
            OpenSourceLicense(
                name = "Compose Fluent UI",
                version = "v0.1.0",
                license = "MIT",
                url = "https://github.com/Konyaco/compose-fluent-ui",
                description = "Fluent Design 风格组件"
            ),
            OpenSourceLicense(
                name = "Lucide Icons",
                version = "1.0.0",
                license = "ISC",
                url = "https://github.com/lucide-icons/lucide",
                description = "图标库（通过 com.composables:icons-lucide 在 Compose 中使用）"
            ),
            OpenSourceLicense(
                name = "Accompanist Lyrics",
                version = "UI 1.0.19 / Core 0.4.7",
                license = "Apache-2.0",
                url = "https://github.com/mocharealm/Accompanist-Lyrics",
                description = "逐字歌词显示组件"
            )
        )
    }
}