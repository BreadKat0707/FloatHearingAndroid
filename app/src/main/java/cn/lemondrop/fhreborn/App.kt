package cn.lemondrop.fhreborn

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import cn.lemondrop.fhreborn.data.repository.SettingsRepository
import cn.lemondrop.fhreborn.ui.components.MiniPlayBar
import cn.lemondrop.fhreborn.ui.components.ScheduledPauseDialog
import cn.lemondrop.fhreborn.ui.screens.album.AlbumDetailScreen
import cn.lemondrop.fhreborn.ui.screens.artist.ArtistDetailScreen
import cn.lemondrop.fhreborn.ui.screens.crash.CrashReportScreen
import cn.lemondrop.fhreborn.ui.screens.folderbrowser.FolderBrowserScreen
import cn.lemondrop.fhreborn.ui.screens.folderbrowser.FolderDetailScreen
import cn.lemondrop.fhreborn.ui.screens.hidden.HiddenFoldersScreen
import cn.lemondrop.fhreborn.ui.screens.ideas.IdeasScreen
import cn.lemondrop.fhreborn.ui.screens.library.LibraryScreen
import cn.lemondrop.fhreborn.ui.screens.onboarding.OnboardingScreen
import cn.lemondrop.fhreborn.ui.screens.player.PlayerScreen
import cn.lemondrop.fhreborn.ui.screens.playlists.PlaylistDetailScreen
import cn.lemondrop.fhreborn.ui.screens.playlists.PlaylistsScreen
import cn.lemondrop.fhreborn.ui.screens.settings.SettingsScreen
import cn.lemondrop.fhreborn.ui.screens.statistics.StatisticsScreen
import cn.lemondrop.fhreborn.ui.theme.FloatHearingTheme
import cn.lemondrop.fhreborn.ui.theme.LocalAppDarkTheme
import cn.lemondrop.fhreborn.ui.theme.LocalTitleBarProgressiveConfig
import cn.lemondrop.fhreborn.ui.theme.LocalTitleBarStyle
import cn.lemondrop.fhreborn.ui.theme.TitleBarProgressiveConfig
import cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import cn.lemondrop.fhreborn.util.CrashHandler
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Global predictive-back switch. Page-level predictive handlers read this value before choosing
 * between predictive and plain back handling.
 */
val LocalPredictiveBackEnabled = staticCompositionLocalOf { false }

/**
 * Estimated bottom space occupied by the global MiniPlayBar. Screens use it to add spacer space
 * without importing the playbar implementation.
 */
val LocalGlobalPlayBarHeight = staticCompositionLocalOf { 160.dp }

/**
 * Shared drawer visibility. AppShell reads this for the sidebar / bottom sheet, screens use it
 * for menu icons, and MiniPlayBar uses it to avoid drawing over an open large-screen sidebar.
 */
val LocalDrawerVisible = staticCompositionLocalOf { mutableStateOf(false) }

val LocalDrawerToggle = staticCompositionLocalOf<() -> Unit> { {} }

/**
 * Whether the full player overlay is open. Screens should give way to the player's back handling
 * when this is true.
 */
val LocalPlayerOpen = staticCompositionLocalOf { false }

val LocalPlayBarOverride = staticCompositionLocalOf { mutableStateOf(false) }

@Composable
fun FHRebornApp() {
    val context = LocalContext.current
    val settingsRepository = SettingsRepository(context)
    val appSettingsRepository = AppSettingsRepository(context)
    val isOnboardingCompleted by settingsRepository.isOnboardingCompleted.collectAsState(initial = null)

    if (isOnboardingCompleted == null) return

    val themeMode by appSettingsRepository.themeMode.collectAsState(initial = "system")
    val useDynamicColor by appSettingsRepository.useDynamicColor.collectAsState(initial = false)
    val accentColorSetting by appSettingsRepository.accentColor.collectAsState(initial = "default")
    val titleBarStyle by appSettingsRepository.titleBarStyle.collectAsState(initial = "gaussian")
    val titleBarBlurRadius by appSettingsRepository.titleBarBlurRadius.collectAsState(initial = 10)
    val titleBarProgressiveStart by appSettingsRepository.titleBarProgressiveStart
        .collectAsState(initial = 0)
    val titleBarProgressiveEnd by appSettingsRepository.titleBarProgressiveEnd
        .collectAsState(initial = 100)
    val titleBarProgressiveCurve by appSettingsRepository.titleBarProgressiveCurve
        .collectAsState(initial = 220)
    val titleBarProgressiveConfig = remember(
        titleBarBlurRadius,
        titleBarProgressiveStart,
        titleBarProgressiveEnd,
        titleBarProgressiveCurve
    ) {
        TitleBarProgressiveConfig(
            blurRadius = titleBarBlurRadius.toFloat(),
            startFraction = titleBarProgressiveStart / 100f,
            endFraction = titleBarProgressiveEnd / 100f,
            curve = titleBarProgressiveCurve / 100f
        )
    }
    val accentColor = remember(accentColorSetting) {
        cn.lemondrop.fhreborn.ui.theme.parseAccentColor(accentColorSetting)
    }
    val bgForeground by appSettingsRepository.bgForeground.collectAsState(initial = "auto")
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemDark
    }

    val onboardingCompleted = isOnboardingCompleted == true
    val navBackStack = rememberNavBackStack<AppRoute>(
        if (onboardingCompleted) AppRoute.Shell else AppRoute.Onboarding
    )
    var topLevelRoute by remember { mutableStateOf<AppRoute>(AppRoute.Library) }
    LaunchedEffect(onboardingCompleted) {
        val rootIsShell = navBackStack.firstOrNull() == AppRoute.Shell
        if (onboardingCompleted != rootIsShell) {
            while (navBackStack.size > 1) {
                navBackStack.removeAt(navBackStack.lastIndex)
            }
            if (navBackStack.isEmpty()) {
                navBackStack.add(if (onboardingCompleted) AppRoute.Shell else AppRoute.Onboarding)
            } else {
                navBackStack[0] = if (onboardingCompleted) AppRoute.Shell else AppRoute.Onboarding
            }
            if (onboardingCompleted) topLevelRoute = AppRoute.Library
        }
    }

    val playerViewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(context.applicationContext as Application)
    )

    var showPlayer by remember { mutableStateOf(false) }
    val drawerVisibleState = remember { mutableStateOf(false) }
    val isLargeScreen = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    val savedDrawerExpanded by appSettingsRepository.drawerExpanded.collectAsState(initial = true)
    val appScope = rememberCoroutineScope()
    val pendingSettingsCategory = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(savedDrawerExpanded, isLargeScreen) {
        if (isLargeScreen) drawerVisibleState.value = savedDrawerExpanded
    }

    val toggleDrawer: () -> Unit = {
        drawerVisibleState.value = !drawerVisibleState.value
        if (isLargeScreen) {
            appScope.launch {
                appSettingsRepository.setDrawerExpanded(drawerVisibleState.value)
            }
        }
    }

    LaunchedEffect(playerViewModel) {
        playerViewModel.openPlayerEvent.collect {
            showPlayer = true
        }
    }

    val showScheduledPause by playerViewModel.showScheduledPauseDialog.collectAsState()
    val timerRemaining by playerViewModel.timerRemaining.collectAsState()
    val isEndOfSongTimer by playerViewModel.isEndOfSongTimer.collectAsState()
    val pauseAfterCurrentSong by playerViewModel.pauseAfterCurrentSong.collectAsState()

    var showCrashReport by remember { mutableStateOf(CrashHandler.hasCrashLog(context)) }
    val crashLog by remember(showCrashReport) {
        mutableStateOf(if (showCrashReport) CrashHandler.readCrashLog(context) else "")
    }

    val currentAppRoute = when (val top = navBackStack.lastOrNull() as? AppRoute) {
        AppRoute.Shell, null -> topLevelRoute
        else -> top
    }
    val currentRoute = currentAppRoute.toScreenRoute()
    val playBarOverrideState = remember { mutableStateOf(false) }
    val shouldShowPlayBar =
        currentAppRoute != AppRoute.Onboarding && !showPlayer && !playBarOverrideState.value
    val hasBottomNavBar =
        currentAppRoute == AppRoute.Library || currentAppRoute == AppRoute.Statistics
    val bottomNavBarHeight = if (hasBottomNavBar) 64.dp else 0.dp
    val playBarBottomOffset = WindowInsets.navigationBars.asPaddingValues()
        .calculateBottomPadding() + bottomNavBarHeight + 8.dp
    val globalPlayBarHeight = 160.dp

    val predictiveBack by appSettingsRepository.getBoolean("predictive_back", false)
        .collectAsState(initial = false)

    val popNav: () -> Unit = {
        if (navBackStack.size > 1) {
            navBackStack.removeAt(navBackStack.lastIndex)
        }
    }

    val pushRoute: (AppRoute) -> Unit = { key ->
        if (navBackStack.lastOrNull() != key) {
            val existing = navBackStack.indexOf(key)
            if (existing >= 0) {
                while (navBackStack.lastIndex > existing) {
                    navBackStack.removeAt(navBackStack.lastIndex)
                }
            } else {
                navBackStack.add(key)
            }
        }
    }

    val topLevelRoutes = setOf(
        AppRoute.Library,
        AppRoute.Playlists,
        AppRoute.FolderBrowser,
        AppRoute.Ideas,
        AppRoute.Statistics,
        AppRoute.Settings
    )

    val navigateRoute: (String) -> Unit = { route ->
        route.toAppRouteOrNull()?.let { key ->
            if (key in topLevelRoutes) {
                topLevelRoute = key
                while (navBackStack.size > 1) {
                    navBackStack.removeAt(navBackStack.lastIndex)
                }
            } else {
                pushRoute(key)
            }
        }
    }

    val topLevelNavigate: (String) -> Unit = { route ->
        val target = route.toAppRouteOrNull()?.takeIf { it in topLevelRoutes } ?: AppRoute.Library
        topLevelRoute = target
        while (navBackStack.size > 1) {
            navBackStack.removeAt(navBackStack.lastIndex)
        }
        if (navBackStack.isEmpty()) {
            navBackStack.add(AppRoute.Shell)
        } else if (navBackStack.firstOrNull() != AppRoute.Shell) {
            navBackStack[0] = AppRoute.Shell
        }
    }

    val finishOnboarding: () -> Unit = {
        topLevelRoute = AppRoute.Library
        while (navBackStack.size > 1) {
            navBackStack.removeAt(navBackStack.lastIndex)
        }
        if (navBackStack.isEmpty()) {
            navBackStack.add(AppRoute.Shell)
        } else {
            navBackStack[0] = AppRoute.Shell
        }
    }

    val dismissCrashReport = {
        CrashHandler.clearCrashLog(context)
        showCrashReport = false
    }

    CompositionLocalProvider(
        LocalAppDarkTheme provides isDarkTheme,
        LocalTitleBarStyle provides titleBarStyle,
        LocalTitleBarProgressiveConfig provides titleBarProgressiveConfig,
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
            accentColor = accentColor,
            foregroundMode = bgForeground
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MiuixTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { _ ->
                Box(modifier = Modifier.fillMaxSize()) {
                    val navCornerRadius = rememberNavSystemCornerRadius()
                    val backdropColor = MiuixTheme.colorScheme.background
                    val navEffects = remember(navCornerRadius, isLargeScreen, backdropColor) {
                        NavDisplayEffects(
                            enableCornerClip = true,
                            cornerClipRadius = if (isLargeScreen) 0.dp else navCornerRadius,
                            cornerClipMode = NavCornerClipMode.Leading,
                            dimAmount = 0.5f,
                            blockInputDuringTransition = false,
                            backdropColor = backdropColor
                        )
                    }

                    cn.lemondrop.fhreborn.ui.components.AppShell(
                        drawerVisible = drawerVisibleState.value,
                        onDismissDrawer = { drawerVisibleState.value = false },
                        currentRoute = currentRoute,
                        onNavigate = topLevelNavigate,
                        onScheduledPauseClick = { playerViewModel.showScheduledPause() }
                    ) {
                        NavDisplay(
                            backStack = navBackStack,
                            modifier = Modifier.fillMaxSize(),
                            onBack = popNav,
                            transition = NavTransitions.MiuixDefault,
                            effects = navEffects
                        ) {
                            entry<AppRoute.Onboarding> {
                                OnboardingScreen(onFinished = finishOnboarding)
                            }

                            entry<AppRoute.Shell> {
                                AnimatedContent(
                                    targetState = topLevelRoute,
                                    transitionSpec = {
                                        slideInHorizontally(
                                            animationSpec = tween(240),
                                            initialOffsetX = { it / 2 }
                                        ) + fadeIn(tween(180)) togetherWith
                                            slideOutHorizontally(
                                                animationSpec = tween(240),
                                                targetOffsetX = { -it / 4 }
                                            ) + fadeOut(tween(120))
                                    },
                                    label = "topLevel"
                                ) { route ->
                                    when (route) {
                                        AppRoute.Library -> LibraryScreen(
                                            currentRoute = route.toScreenRoute(),
                                            onNavigate = navigateRoute,
                                            playerViewModel = playerViewModel
                                        )
                                        AppRoute.Playlists -> PlaylistsScreen(
                                            currentRoute = route.toScreenRoute(),
                                            onNavigate = navigateRoute,
                                            playerViewModel = playerViewModel,
                                            onOpenPlaylist = { playlistId ->
                                                pushRoute(AppRoute.PlaylistDetail(playlistId))
                                            }
                                        )
                                        AppRoute.FolderBrowser -> FolderBrowserScreen(
                                            currentRoute = route.toScreenRoute(),
                                            playerViewModel = playerViewModel,
                                            onBack = { topLevelRoute = AppRoute.Library },
                                            onNavigate = navigateRoute
                                        )
                                        AppRoute.Ideas -> IdeasScreen(
                                            currentRoute = route.toScreenRoute(),
                                            onNavigate = navigateRoute,
                                            playerViewModel = playerViewModel
                                        )
                                        AppRoute.Statistics -> StatisticsScreen(
                                            currentRoute = route.toScreenRoute(),
                                            onNavigate = navigateRoute,
                                            playerViewModel = playerViewModel,
                                            onNavigateToAlbum = { album, artist ->
                                                pushRoute(AppRoute.AlbumDetail(album, artist))
                                            },
                                            onNavigateToArtist = { artist ->
                                                pushRoute(AppRoute.ArtistDetail(artist))
                                            }
                                        )
                                        AppRoute.Settings -> SettingsScreen(
                                            currentRoute = route.toScreenRoute(),
                                            onNavigate = navigateRoute,
                                            playerViewModel = playerViewModel,
                                            initialCategoryKey = pendingSettingsCategory.value
                                        )
                                        else -> LibraryScreen(
                                            currentRoute = AppRoute.Library.toScreenRoute(),
                                            onNavigate = navigateRoute,
                                            playerViewModel = playerViewModel
                                        )
                                    }
                                }
                            }

                            entry<AppRoute.PlaylistDetail> { route ->
                                PlaylistDetailScreen(
                                    playlistId = route.playlistId,
                                    onBack = popNav,
                                    playerViewModel = playerViewModel,
                                    onNavigateToAlbum = { album, artist ->
                                        pushRoute(AppRoute.AlbumDetail(album, artist))
                                    },
                                    onNavigateToArtist = { artist ->
                                        pushRoute(AppRoute.ArtistDetail(artist))
                                    }
                                )
                            }

                            entry<AppRoute.FolderDetail> { route ->
                                val libraryViewModel: LibraryViewModel = viewModel(
                                    factory = LibraryViewModel.Factory(context.applicationContext as Application)
                                )
                                FolderDetailScreen(
                                    folderPath = route.folderPath,
                                    onBack = popNav,
                                    playerViewModel = playerViewModel,
                                    libraryViewModel = libraryViewModel
                                )
                            }

                            entry<AppRoute.HiddenFolders> {
                                val libraryViewModel: LibraryViewModel = viewModel(
                                    factory = LibraryViewModel.Factory(context.applicationContext as Application)
                                )
                                HiddenFoldersScreen(
                                    libraryViewModel = libraryViewModel,
                                    playerViewModel = playerViewModel,
                                    onBack = popNav
                                )
                            }

                            entry<AppRoute.AlbumDetail> { route ->
                                AlbumDetailScreen(
                                    albumName = route.albumName,
                                    albumArtist = route.albumArtist,
                                    onBack = popNav,
                                    playerViewModel = playerViewModel
                                )
                            }

                            entry<AppRoute.ArtistDetail> { route ->
                                val libraryViewModel: LibraryViewModel = viewModel(
                                    factory = LibraryViewModel.Factory(context.applicationContext as Application)
                                )
                                ArtistDetailScreen(
                                    artistName = route.artistName,
                                    onBack = popNav,
                                    onNavigateToAlbum = { album, albumArtist ->
                                        pushRoute(AppRoute.AlbumDetail(album, albumArtist))
                                    },
                                    playerViewModel = playerViewModel,
                                    libraryViewModel = libraryViewModel
                                )
                            }
                        }
                    }

                    if (shouldShowPlayBar) {
                        val sidebarInset =
                            if (isLargeScreen && drawerVisibleState.value) {
                                cn.lemondrop.fhreborn.ui.components.SidebarWidth
                            } else {
                                0.dp
                            }
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
                                    .padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        bottom = playBarBottomOffset
                                    )
                            )
                        }
                    }

                    if (showPlayer) {
                        PlayerScreen(
                            playerViewModel = playerViewModel,
                            onBack = { showPlayer = false },
                            onNavigateToAlbum = { album, artist ->
                                showPlayer = false
                                pushRoute(AppRoute.AlbumDetail(album, artist))
                            },
                            onNavigateToArtist = { artist ->
                                showPlayer = false
                                pushRoute(AppRoute.ArtistDetail(artist))
                            },
                            onOpenSettingsCategory = { category ->
                                pendingSettingsCategory.value = category
                                showPlayer = false
                                topLevelRoute = AppRoute.Settings
                                while (navBackStack.size > 1) {
                                    navBackStack.removeAt(navBackStack.lastIndex)
                                }
                            }
                        )
                    }

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

                    AnimatedVisibility(
                        visible = showCrashReport && crashLog.isNotBlank(),
                        enter = fadeIn() + scaleIn(initialScale = 0.9f),
                        exit = fadeOut() + scaleOut(targetScale = 0.9f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CrashReportScreen(
                            crashLog = crashLog,
                            onDismiss = dismissCrashReport
                        )
                    }

                    BackHandler(
                        enabled = !showPlayer &&
                            !showCrashReport &&
                            navBackStack.lastOrNull() == AppRoute.Shell &&
                            topLevelRoute != AppRoute.Library &&
                            (!drawerVisibleState.value || isLargeScreen)
                    ) {
                        topLevelRoute = AppRoute.Library
                    }
                }
            }
        }

        BackHandler(enabled = showCrashReport && crashLog.isNotBlank()) {
            dismissCrashReport()
        }
    }
}
