package cn.lemondrop.fhreborn

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import cn.lemondrop.fhreborn.data.repository.SettingsRepository
import cn.lemondrop.fhreborn.ui.theme.FloatHearingTheme
import cn.lemondrop.fhreborn.ui.theme.LocalAppDarkTheme
import cn.lemondrop.clover.CloverTheme
import cn.lemondrop.fhreborn.ui.screens.crash.CrashReportScreen
import cn.lemondrop.fhreborn.ui.screens.demo.MicaDemoScreen
import cn.lemondrop.fhreborn.ui.screens.folderbrowser.FolderBrowserScreen
import cn.lemondrop.fhreborn.ui.screens.ideas.IdeasScreen
import cn.lemondrop.fhreborn.ui.screens.library.LibraryScreen
import cn.lemondrop.fhreborn.ui.screens.onboarding.OnboardingScreen
import cn.lemondrop.fhreborn.ui.screens.player.PlayerScreen
import cn.lemondrop.fhreborn.ui.screens.playlists.PlaylistsScreen
import cn.lemondrop.fhreborn.ui.screens.settings.SettingsScreen
import cn.lemondrop.fhreborn.ui.screens.statistics.StatisticsScreen
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import cn.lemondrop.fhreborn.util.CrashHandler

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Library : Screen("library")
    data object Playlists : Screen("playlists")
    data object FolderBrowser : Screen("folder_browser")
    data object Ideas : Screen("ideas")
    data object Settings : Screen("settings")
    data object Statistics : Screen("statistics")
    data object Player : Screen("player")
    data object HazeDemo : Screen("haze_demo")
    data object CloverDemo : Screen("clover_demo")
    data object MicaDemo : Screen("mica_demo")
}

@Composable
fun FHRebornApp() {
    val context = LocalContext.current
    val settingsRepository = SettingsRepository(context)
    val appSettingsRepository = AppSettingsRepository(context)
    // initial = null 避免 startDestination 从 Onboarding 动态变成 Library 导致 NavHost 重建
    val isOnboardingCompleted by settingsRepository.isOnboardingCompleted.collectAsState(initial = null)

    // 等待 DataStore 读取完成，确定 onboarding 状态后再创建 NavHost
    if (isOnboardingCompleted == null) return

    val themeMode by appSettingsRepository.themeMode.collectAsState(initial = "system")
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemDark
    }

    val navController = rememberNavController()

    // 在 App 层级创建 PlayerViewModel，确保所有页面共享同一个实例
    val playerViewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(context.applicationContext as Application)
    )

    var showPlayer by remember { mutableStateOf(false) }

    // 通知栏/媒体控件等外部入口要求打开播放器页面
    LaunchedEffect(playerViewModel) {
        playerViewModel.openPlayerEvent.collect {
            showPlayer = true
        }
    }

    // 检测是否有上次的崩溃日志
    var showCrashReport by remember { mutableStateOf(CrashHandler.hasCrashLog(context)) }
    val crashLog by remember(showCrashReport) {
        mutableStateOf(if (showCrashReport) CrashHandler.readCrashLog(context) else "")
    }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val isAtHome = currentRoute == Screen.Library.route

    // 统一处理系统返回键（支持预测返回手势）：
    // 1. 播放器页打开时先关闭播放器
    // 2. 崩溃报告弹窗打开时先关闭弹窗
    // 3. 非首页时返回上一页；无法返回时结束 Activity
    PredictiveBackHandler(enabled = showPlayer || showCrashReport || !isAtHome) { progress ->
        progress.collect { }
        when {
            showPlayer -> showPlayer = false
            showCrashReport -> {
                CrashHandler.clearCrashLog(context)
                showCrashReport = false
            }
            !isAtHome -> {
                if (!navController.navigateUp()) {
                    (context as? android.app.Activity)?.finish()
                }
            }
        }
    }

    CompositionLocalProvider(LocalAppDarkTheme provides isDarkTheme) {
        CloverTheme(darkTheme = isDarkTheme) {
        FloatHearingTheme(darkTheme = isDarkTheme) {
            Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = if (isOnboardingCompleted == true) Screen.Library.route else Screen.Onboarding.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
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

            // Drawer 切换主页面时清理堆栈：回退直接回到媒体库，不会一层层返回
            val topLevelNavigate: (String) -> Unit = { route ->
                if (route != navController.currentBackStackEntry?.destination?.route) {
                    navController.navigate(route) {
                        popUpTo(Screen.Library.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }

            composable(Screen.Library.route) { backStackEntry ->
                LibraryScreen(
                    currentRoute = backStackEntry.destination.route ?: Screen.Library.route,
                    onNavigate = topLevelNavigate,
                    onPlayerClick = { showPlayer = true },
                    playerViewModel = playerViewModel
                )
            }

            composable(Screen.Playlists.route) { backStackEntry ->
                PlaylistsScreen(
                    currentRoute = backStackEntry.destination.route ?: Screen.Playlists.route,
                    onNavigate = topLevelNavigate,
                    onPlayerClick = { showPlayer = true },
                    playerViewModel = playerViewModel
                )
            }

            composable(Screen.FolderBrowser.route) { backStackEntry ->
                FolderBrowserScreen(
                    currentRoute = backStackEntry.destination.route ?: Screen.FolderBrowser.route,
                    onNavigate = topLevelNavigate,
                    onPlayerClick = { showPlayer = true },
                    playerViewModel = playerViewModel
                )
            }

            composable(Screen.Ideas.route) { backStackEntry ->
                IdeasScreen(
                    currentRoute = backStackEntry.destination.route ?: Screen.Ideas.route,
                    onNavigate = topLevelNavigate,
                    onPlayerClick = { showPlayer = true },
                    playerViewModel = playerViewModel
                )
            }

            composable(Screen.Statistics.route) { backStackEntry ->
                StatisticsScreen(
                    currentRoute = backStackEntry.destination.route ?: Screen.Statistics.route,
                    onNavigate = topLevelNavigate,
                    onPlayerClick = { showPlayer = true },
                    playerViewModel = playerViewModel
                )
            }

            composable(Screen.Settings.route) { backStackEntry ->
                SettingsScreen(
                    currentRoute = backStackEntry.destination.route ?: Screen.Settings.route,
                    onNavigate = topLevelNavigate,
                    onPlayerClick = { showPlayer = true },
                    playerViewModel = playerViewModel
                )
            }

            composable(Screen.HazeDemo.route) {
                cn.lemondrop.fhreborn.ui.screens.demo.HazeDemoScreen(
                    onBack = { navController.navigateUp() }
                )
            }

            composable(Screen.CloverDemo.route) {
                cn.lemondrop.fhreborn.ui.screens.demo.CloverDemoScreen(
                    onBack = { navController.navigateUp() }
                )
            }

            composable(Screen.MicaDemo.route) {
                MicaDemoScreen(
                    onBack = { navController.navigateUp() }
                )
            }
        }

        // 播放器页（自身管理进入/退出动画）
        if (showPlayer) {
            PlayerScreen(
                playerViewModel = playerViewModel,
                onBack = { showPlayer = false }
            )
        }

        // 崩溃报告覆盖层（最上层）
        AnimatedVisibility(
            visible = showCrashReport && crashLog.isNotBlank(),
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier.fillMaxSize()
        ) {
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
    }
    }
}
