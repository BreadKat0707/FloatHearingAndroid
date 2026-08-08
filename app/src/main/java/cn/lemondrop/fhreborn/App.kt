package cn.lemondrop.fhreborn

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import kotlinx.coroutines.launch
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import cn.lemondrop.fhreborn.data.repository.SettingsRepository
import cn.lemondrop.fhreborn.ui.theme.FloatHearingTheme
import cn.lemondrop.fhreborn.ui.theme.LocalAppDarkTheme
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils

import cn.lemondrop.fhreborn.ui.components.MiniPlayBar
import cn.lemondrop.fhreborn.ui.components.ScheduledPauseDialog
import cn.lemondrop.fhreborn.ui.screens.album.AlbumDetailScreen
import cn.lemondrop.fhreborn.ui.screens.artist.ArtistDetailScreen
import cn.lemondrop.fhreborn.ui.screens.crash.CrashReportScreen
import cn.lemondrop.fhreborn.ui.screens.demo.MicaDemoScreen
import cn.lemondrop.fhreborn.ui.screens.folderbrowser.FolderBrowserScreen
import cn.lemondrop.fhreborn.ui.screens.ideas.IdeasScreen
import cn.lemondrop.fhreborn.ui.screens.library.LibraryScreen
import cn.lemondrop.fhreborn.ui.screens.onboarding.OnboardingScreen
import cn.lemondrop.fhreborn.ui.screens.player.PlayerScreen
import cn.lemondrop.fhreborn.ui.screens.playlists.PlaylistDetailScreen
import cn.lemondrop.fhreborn.ui.screens.playlists.PlaylistsScreen
import cn.lemondrop.fhreborn.ui.screens.settings.SettingsScreen
import cn.lemondrop.fhreborn.ui.screens.statistics.StatisticsScreen
import cn.lemondrop.fhreborn.ui.components.ScheduledPauseDialog
import cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel
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
    data object MicaDemo : Screen("mica_demo")
    data object ArtistDetail : Screen("artist/{artistName}") {
        fun createRoute(artistName: String) = "artist/${Uri.encode(artistName)}"
    }
    data object AlbumDetail : Screen("album/{albumName}/{albumArtist}") {
        fun createRoute(albumName: String, albumArtist: String?): String {
            val artistPart = if (albumArtist.isNullOrBlank()) "_null_" else Uri.encode(albumArtist)
            return "album/${Uri.encode(albumName)}/$artistPart"
        }
    }
    data object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
}

/**
 * 全局预测返回手势开关。
 * 在 [FHRebornApp] 中根据设置项 [predictive_back] 注入，
 * 所有使用 [androidx.activity.compose.PredictiveBackHandler] 的地方都应读取此值并参与 enabled 判断。
 */
val LocalPredictiveBackEnabled = staticCompositionLocalOf { false }

/**
 * 全局 MiniPlayBar 在屏幕底部占用的总高度估算值。
 * 页面内容底部需要 spacer 时可以使用此值，避免被悬浮的 PlayBar 遮挡。
 */
val LocalGlobalPlayBarHeight = staticCompositionLocalOf { 160.dp }

/**
 * 全局抽屉展开状态（MutableState 引用）。由 App 根提供：
 * - 页面 menu 按钮通过 [LocalDrawerToggle] 切换它
 * - AppShell 读它控制侧边栏/底部弹层
 * - MiniPlayBar 读它计算避让宽度
 */
val LocalDrawerVisible = staticCompositionLocalOf { mutableStateOf(false) }

/**
 * 全局抽屉 toggle 入口（负责切换 + 大屏时持久化状态）。
 * 页面 menu 按钮直接调用：onClick = { LocalDrawerToggle.current() }
 */
val LocalDrawerToggle = staticCompositionLocalOf<() -> Unit> { {} }

/**
 * 播放器覆盖层是否打开。播放器打开时，页面自身的 BackHandler 必须让位，
 * 让返回键先关闭播放器（BackHandler 后组合者优先，否则页面会抢走返回事件）。
 */
val LocalPlayerOpen = staticCompositionLocalOf { false }

/**
 * 全局播放条显隐覆盖开关（MutableState 引用）。页面在多选等全屏底部覆盖场景下
 * 置 true 隐藏 MiniPlayBar，避免遮挡底部工具栏；退出场景后置 false 恢复。
 */
val LocalPlayBarOverride = staticCompositionLocalOf { mutableStateOf(false) }

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
    val useDynamicColor by appSettingsRepository.useDynamicColor.collectAsState(initial = false)
    val accentColorSetting by appSettingsRepository.accentColor.collectAsState(initial = "default")
    val accentColor = remember(accentColorSetting) {
        cn.lemondrop.fhreborn.ui.theme.parseAccentColor(accentColorSetting)
    }
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

    // 全局抽屉展开状态（大屏侧边栏 / 小屏 BottomSheet 共用）
    val drawerVisibleState = remember { mutableStateOf(false) }
    val isLargeScreen = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    val savedDrawerExpanded by appSettingsRepository.drawerExpanded.collectAsState(initial = true)
    val appScope = rememberCoroutineScope()

    // 打开设置时希望直达的分类（如播放器"歌词设置"→ 设置-歌词）
    val pendingSettingsCategory = remember { mutableStateOf<String?>(null) }

    // 大屏：启动时恢复上次的侧边栏展开状态（跨启动记住）
    LaunchedEffect(savedDrawerExpanded, isLargeScreen) {
        if (isLargeScreen) drawerVisibleState.value = savedDrawerExpanded
    }

    // 全局 toggle：切换展开状态；大屏时持久化（小屏 BottomSheet 的一次性行为不持久化）
    val toggleDrawer: () -> Unit = {
        drawerVisibleState.value = !drawerVisibleState.value
        if (isLargeScreen) {
            appScope.launch {
                appSettingsRepository.setDrawerExpanded(drawerVisibleState.value)
            }
        }
    }

    // 通知栏/媒体控件等外部入口要求打开播放器页面
    LaunchedEffect(playerViewModel) {
        playerViewModel.openPlayerEvent.collect {
            showPlayer = true
        }
    }

    // 计划暂停对话框（全局托管，可从播放页/任意抽屉打开）
    val showScheduledPause by playerViewModel.showScheduledPauseDialog.collectAsState()
    val timerRemaining by playerViewModel.timerRemaining.collectAsState()
    val isEndOfSongTimer by playerViewModel.isEndOfSongTimer.collectAsState()
    val pauseAfterCurrentSong by playerViewModel.pauseAfterCurrentSong.collectAsState()

    // 检测是否有上次的崩溃日志
    var showCrashReport by remember { mutableStateOf(CrashHandler.hasCrashLog(context)) }
    val crashLog by remember(showCrashReport) {
        mutableStateOf(if (showCrashReport) CrashHandler.readCrashLog(context) else "")
    }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val playBarRoutes = remember {
        setOf(
            Screen.Library.route,
            Screen.Playlists.route,
            Screen.Ideas.route,
            Screen.Statistics.route,
            Screen.Settings.route,
            Screen.AlbumDetail.route,
            Screen.ArtistDetail.route,
            Screen.PlaylistDetail.route
        )
    }
    // 多选等场景下页面通过 LocalPlayBarOverride 置 true 隐藏播放条（同一引用）
    val playBarOverrideState = remember { mutableStateOf(false) }
    val shouldShowPlayBar = currentRoute in playBarRoutes && !showPlayer && !playBarOverrideState.value

    val playBarBottomOffset = WindowInsets.navigationBars.asPaddingValues()
        .calculateBottomPadding() + 80.dp
    val globalPlayBarHeight = 160.dp

    val isAtHome = currentRoute == Screen.Library.route

    // 预测性返回手势开关（实验功能，默认关闭）
    val predictiveBack by appSettingsRepository.getBoolean("predictive_back", false)
        .collectAsState(initial = false)

    // 统一处理系统返回键：
    // 1. 播放器页打开时先关闭播放器
    // 2. 崩溃报告弹窗打开时先关闭弹窗
    // 3. 非首页时返回上一页；无法返回时结束 Activity
    val backEnabled = showPlayer || showCrashReport || !isAtHome
    val handleBack: () -> Unit = {
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
    // 开关开启时用预测性返回手势（保留上一页预览），关闭时用普通返回
    if (predictiveBack) {
        PredictiveBackHandler(enabled = backEnabled) { progress ->
            progress.collect { }
            handleBack()
        }
    } else {
        BackHandler(enabled = backEnabled) {
            handleBack()
        }
    }

    CompositionLocalProvider(
        LocalAppDarkTheme provides isDarkTheme,
        LocalPredictiveBackEnabled provides predictiveBack,
        LocalGlobalPlayBarHeight provides globalPlayBarHeight,
        LocalDrawerVisible provides drawerVisibleState,
        LocalDrawerToggle provides toggleDrawer,
        LocalPlayerOpen provides showPlayer,
        LocalPlayBarOverride provides playBarOverrideState
    ) {
        FloatHearingTheme(
            darkTheme = isDarkTheme,
            useDynamicColor = useDynamicColor,
            themeMode = themeMode,
            accentColor = accentColor
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MiuixTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
            // 二级页面（详情类）用左右滑动推入推出；根页面之间只做内容区上浮淡入
            val secondaryRoutes = remember {
                setOf(
                    Screen.AlbumDetail.route,
                    Screen.ArtistDetail.route,
                    Screen.PlaylistDetail.route,
                    Screen.MicaDemo.route
                )
            }
            NavHost(
                navController = navController,
                startDestination = if (isOnboardingCompleted == true) Screen.Library.route else Screen.Onboarding.route,
            enterTransition = {
                if (targetState.destination.route in secondaryRoutes) {
                    slideInHorizontally(initialOffsetX = { it }) + fadeIn()
                } else {
                    slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(250)) + fadeIn(tween(200))
                }
            },
            exitTransition = {
                if (initialState.destination.route in secondaryRoutes) {
                    slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut()
                } else {
                    fadeOut(tween(150))
                }
            },
            popEnterTransition = {
                if (targetState.destination.route in secondaryRoutes) {
                    slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn()
                } else {
                    slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(250)) + fadeIn(tween(200))
                }
            },
            popExitTransition = {
                if (initialState.destination.route in secondaryRoutes) {
                    slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                } else {
                    fadeOut(tween(150))
                }
            }
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
                    playerViewModel = playerViewModel
                )
            }

            composable(Screen.Playlists.route) { backStackEntry ->
                PlaylistsScreen(
                    currentRoute = backStackEntry.destination.route ?: Screen.Playlists.route,
                    onNavigate = topLevelNavigate,
                    playerViewModel = playerViewModel,
                    onOpenPlaylist = { playlistId ->
                        navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                    }
                )
            }

            composable(Screen.PlaylistDetail.route) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getString("playlistId")?.toLongOrNull() ?: 0L
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    onBack = { navController.navigateUp() },
                    playerViewModel = playerViewModel,
                    onNavigateToAlbum = { album, artist ->
                        navController.navigate(Screen.AlbumDetail.createRoute(album, artist))
                    },
                    onNavigateToArtist = { artist ->
                        navController.navigate(Screen.ArtistDetail.createRoute(artist))
                    }
                )
            }

            composable(Screen.FolderBrowser.route) { backStackEntry ->
                FolderBrowserScreen(
                    currentRoute = backStackEntry.destination.route ?: Screen.FolderBrowser.route,
                    playerViewModel = playerViewModel,
                    onBack = { navController.navigateUp() },
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Library.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Ideas.route) { backStackEntry ->
                IdeasScreen(
                    currentRoute = backStackEntry.destination.route ?: Screen.Ideas.route,
                    onNavigate = topLevelNavigate,
                    playerViewModel = playerViewModel
                )
            }

            composable(Screen.Statistics.route) { backStackEntry ->
                StatisticsScreen(
                    currentRoute = backStackEntry.destination.route ?: Screen.Statistics.route,
                    onNavigate = topLevelNavigate,
                    playerViewModel = playerViewModel
                )
            }

            composable(Screen.Settings.route) { backStackEntry ->
                SettingsScreen(
                    currentRoute = backStackEntry.destination.route ?: Screen.Settings.route,
                    onNavigate = topLevelNavigate,
                    playerViewModel = playerViewModel,
                    initialCategoryKey = pendingSettingsCategory.value
                )
            }

            composable(Screen.MicaDemo.route) {
                MicaDemoScreen(
                    onBack = { navController.navigateUp() }
                )
            }

            composable(
                route = Screen.AlbumDetail.route,
                arguments = listOf(
                    navArgument("albumName") { type = NavType.StringType },
                    navArgument("albumArtist") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = "_null_"
                    }
                )
            ) { backStackEntry ->
                val albumName = backStackEntry.arguments?.getString("albumName") ?: ""
                val rawArtist = backStackEntry.arguments?.getString("albumArtist")
                val albumArtist = rawArtist?.takeIf { it != "_null_" && it.isNotBlank() }
                AlbumDetailScreen(
                    albumName = albumName,
                    albumArtist = albumArtist,
                    onBack = { navController.navigateUp() },
                    playerViewModel = playerViewModel
                )
            }

            composable(
                route = Screen.ArtistDetail.route,
                arguments = listOf(
                    navArgument("artistName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val artistName = backStackEntry.arguments?.getString("artistName") ?: ""
                val libraryViewModel: LibraryViewModel = viewModel(
                    factory = LibraryViewModel.Factory(context.applicationContext as Application)
                )
                ArtistDetailScreen(
                    artistName = artistName,
                    onBack = { navController.navigateUp() },
                    onNavigateToAlbum = { album, albumArtist ->
                        navController.navigate(Screen.AlbumDetail.createRoute(album, albumArtist))
                    },
                    playerViewModel = playerViewModel,
                    libraryViewModel = libraryViewModel
                )
            }
        }

        // 全局迷你播放条（悬浮在主页面/详情页底部）
        if (shouldShowPlayBar) {
            // 大屏且侧边栏展开时，播放条只覆盖内容区（从侧边栏右缘开始），在内容区内水平居中
            val sidebarInset = if (isLargeScreen && drawerVisibleState.value) cn.lemondrop.fhreborn.ui.components.SidebarWidth else 0.dp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = sidebarInset)
            ) {
                MiniPlayBar(
                    playerViewModel = playerViewModel,
                    onClick = { showPlayer = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(end = 16.dp, bottom = playBarBottomOffset)
                )
            }
        }

        // 播放器页（自身管理进入/退出动画）
        if (showPlayer) {
            PlayerScreen(
                playerViewModel = playerViewModel,
                onBack = { showPlayer = false },
                onNavigateToAlbum = { album, artist ->
                    showPlayer = false
                    navController.navigate(Screen.AlbumDetail.createRoute(album, artist))
                },
                onNavigateToArtist = { artist ->
                    showPlayer = false
                    navController.navigate(Screen.ArtistDetail.createRoute(artist))
                },
                onOpenSettingsCategory = { category ->
                    // 播放器 → 设置指定分类：先关播放器，再导航到设置并直达分类
                    pendingSettingsCategory.value = category
                    showPlayer = false
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // 计划暂停对话框（全局托管）
        if (showScheduledPause) {
            ScheduledPauseDialog(
                timerRemaining = timerRemaining,
                isEndOfSongTimer = isEndOfSongTimer,
                pauseAfterCurrentSong = pauseAfterCurrentSong,
                onSetTimer = { playerViewModel.setTimer(it) },
                onSetEndOfSong = { playerViewModel.setEndOfSongTimer() },
                onSetPauseAfterCurrentSong = { playerViewModel.setPauseAfterCurrentSong(it) },
                onCancel = { playerViewModel.cancelTimer() },
                onDismiss = { playerViewModel.hideScheduledPause() }
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
