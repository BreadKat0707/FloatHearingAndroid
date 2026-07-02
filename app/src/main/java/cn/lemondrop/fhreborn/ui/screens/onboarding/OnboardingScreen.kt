package cn.lemondrop.fhreborn.ui.screens.onboarding

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.material3.MaterialTheme
import io.github.composefluent.component.Icon
import io.github.composefluent.component.ProgressRing
import io.github.composefluent.component.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lemondrop.fhreborn.scanner.ScanProgress
import cn.lemondrop.clover.CloverAdaptivePageScaffold
import cn.lemondrop.fhreborn.ui.theme.FluentButton
import cn.lemondrop.fhreborn.ui.theme.FluentOutlinedButton
import cn.lemondrop.fhreborn.ui.viewmodel.OnboardingViewModel
import cn.lemondrop.fhreborn.util.PermissionUtils
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.HardDrive
import com.composables.icons.lucide.Lucide

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.Factory(context.applicationContext as Application)
    )

    var currentStep by remember { mutableStateOf(0) }
    val scanProgress by viewModel.scanProgress.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    var storageGranted by remember {
        mutableStateOf(PermissionUtils.hasStoragePermission(context))
    }
    var notificationGranted by remember {
        mutableStateOf(PermissionUtils.hasNotificationPermission(context))
    }

    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        storageGranted = result.values.any { it }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGranted = granted
    }

    LaunchedEffect(currentStep) {
        if (currentStep == 2 && !isScanning) {
            viewModel.startFirstScan()
        }
    }

    CloverAdaptivePageScaffold(
        title = {
            Text(
                text = "首次设置",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    ) { paddingValues ->
        val cutoutPadding = WindowInsets.displayCutout.asPaddingValues()
        val cutoutLeft = cutoutPadding.calculateLeftPadding(LayoutDirection.Ltr)
        val cutoutRight = cutoutPadding.calculateRightPadding(LayoutDirection.Ltr)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp + cutoutLeft.coerceAtLeast(0.dp) + cutoutRight.coerceAtLeast(0.dp))
                .padding(
                    top = paddingValues.calculateTopPadding() + 32.dp,
                    bottom = paddingValues.calculateBottomPadding()
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "欢迎使用 FH Reborn",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "让我们完成几个简单设置，开始你的音乐之旅",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            when (currentStep) {
                0 -> StoragePermissionStep(
                    granted = storageGranted,
                    onRequestPermission = {
                        storageLauncher.launch(PermissionUtils.getRequiredPermissions())
                    },
                    onNext = { currentStep = 1 }
                )
                1 -> NotificationPermissionStep(
                    granted = notificationGranted,
                    onRequestPermission = {
                        PermissionUtils.getNotificationPermission()?.let {
                            notificationLauncher.launch(it)
                        }
                    },
                    onNext = { currentStep = 2 }
                )
                2 -> ScanningStep(
                    progress = scanProgress,
                    isScanning = isScanning,
                    onFinish = {
                        viewModel.completeOnboarding()
                        onFinished()
                    }
                )
            }
        }
    }
}

@Composable
private fun StoragePermissionStep(
    granted: Boolean,
    onRequestPermission: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Lucide.HardDrive,
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("存储权限", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "FH Reborn 需要访问您的本地音乐文件，以扫描和播放媒体库中的歌曲。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (granted) {
            Text(
                "✓ 权限已授权",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            FluentButton(onClick = onNext) {
                Text("下一步")
            }
        } else {
            FluentButton(onClick = onRequestPermission) {
                Text("授权存储权限")
            }
        }
    }
}

@Composable
private fun NotificationPermissionStep(
    granted: Boolean,
    onRequestPermission: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Lucide.Bell,
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("通知权限", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "后台播放时需要在通知栏显示播放控制，建议您授予通知权限。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (granted) {
            Text(
                "✓ 权限已授权",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!granted) {
                FluentOutlinedButton(onClick = onRequestPermission) {
                    Text("授权通知权限")
                }
            }
            FluentButton(onClick = onNext) {
                Text(if (granted) "下一步" else "跳过")
            }
        }
    }
}

@Composable
private fun ScanningStep(
    progress: ScanProgress,
    isScanning: Boolean,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        when (progress) {
            is ScanProgress.Idle -> {
                ProgressRing(modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("准备扫描...")
            }
            is ScanProgress.Scanning -> {
                ProgressRing(modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("正在扫描媒体库...")
            }
            is ScanProgress.Progress -> {
                ProgressRing(modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("正在扫描: ${progress.current}/${progress.total}")
                Text(
                    progress.path,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is ScanProgress.Completed -> {
                Text(
                    "扫描完成！",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("共发现 ${progress.songsFound} 首歌曲")
                Spacer(modifier = Modifier.height(24.dp))
                FluentButton(onClick = onFinish) {
                    Text("进入应用")
                }
            }
            is ScanProgress.Error -> {
                Text(
                    "扫描出错",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    progress.message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                FluentButton(onClick = onFinish) {
                    Text("继续")
                }
            }
        }
    }
}
