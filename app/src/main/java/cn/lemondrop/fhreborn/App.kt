package cn.lemondrop.fhreborn

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cn.lemondrop.fhreborn.data.repository.SettingsRepository
import cn.lemondrop.fhreborn.ui.screens.crash.CrashReportScreen
import cn.lemondrop.fhreborn.ui.screens.library.LibraryScreen
import cn.lemondrop.fhreborn.ui.screens.onboarding.OnboardingScreen
import cn.lemondrop.fhreborn.ui.screens.player.PlayerScreen
import cn.lemondrop.fhreborn.ui.screens.settings.SettingsScreen
import cn.lemondrop.fhreborn.ui.screens.statistics.StatisticsScreen
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import cn.lemondrop.fhreborn.util.CrashHandler

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Library : Screen("library")
    data object Settings : Screen("settings")
    data object Statistics : Screen("statistics")
    data object Player : Screen("player")
}

@Composable
fun FHRebornApp() {
    val context = LocalContext.current
    val settingsRepository = SettingsRepository(context)
    // initial = null 避免 startDestination 从 Onboarding 动态变成 Library 导致 NavHost 重建
    val isOnboardingCompleted by settingsRepository.isOnboardingCompleted.collectAsState(initial = null)

    // 等待 DataStore 读取完成，确定 onboarding 状态后再创建 NavHost
    if (isOnboardingCompleted == null) return

    val navController = rememberNavController()

    // 在 App 层级创建 PlayerViewModel，确保所有页面共享同一个实例
    val playerViewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(context.applicationContext as Application)
    )

    var showPlayer by remember { mutableStateOf(false) }

    // 检测是否有上次的崩溃日志
    var showCrashReport by remember { mutableStateOf(CrashHandler.hasCrashLog(context)) }
    val crashLog by remember(showCrashReport) {
        mutableStateOf(if (showCrashReport) CrashHandler.readCrashLog(context) else "")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = if (isOnboardingCompleted == true) Screen.Library.route else Screen.Onboarding.route
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Screen.Library.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Library.route) {
                LibraryScreen(
                    playerViewModel = playerViewModel,
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToStatistics = {
                        navController.navigate(Screen.Statistics.route)
                    },
                    onNavigateToPlayer = {
                        showPlayer = true
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = {
                        navController.navigateUp()
                    }
                )
            }

            composable(Screen.Statistics.route) {
                StatisticsScreen(
                    onBack = {
                        navController.navigateUp()
                    }
                )
            }
        }

        if (showPlayer) {
            PlayerScreen(
                playerViewModel = playerViewModel,
                onBack = { showPlayer = false }
            )
        }

        // 崩溃报告覆盖层（最上层）
        if (showCrashReport && crashLog.isNotBlank()) {
            CrashReportScreen(
                crashLog = crashLog,
                onDismiss = {
                    CrashHandler.clearCrashLog(context)
                    showCrashReport = false
                }
            )
        }
    }
}
