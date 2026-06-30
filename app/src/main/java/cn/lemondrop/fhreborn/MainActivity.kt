package cn.lemondrop.fhreborn

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import cn.lemondrop.fhreborn.util.CrashHandler
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by lazy {
        ViewModelProvider(
            this,
            PlayerViewModel.Factory(application)
        )[PlayerViewModel::class.java]
    }

    private lateinit var appSettingsRepository: AppSettingsRepository
    private var currentThemeMode: String = "system"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashHandler.init(applicationContext)
        enableEdgeToEdge()

        appSettingsRepository = AppSettingsRepository(this)

        // 根据 App 设置的颜色模式同步状态栏/导航栏图标反色
        lifecycleScope.launch {
            appSettingsRepository.themeMode.collect { mode ->
                currentThemeMode = mode
                applyEdgeToEdge(mode)
            }
        }

        lifecycleScope.launch {
            appSettingsRepository.hideSystemUi.collect { hide ->
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    if (hide) {
                        hide(WindowInsetsCompat.Type.systemBars())
                    } else {
                        show(WindowInsetsCompat.Type.systemBars())
                    }
                }
            }
        }

        // 主页面背景为「云母」时按需开启窗口壁纸透出，切走立即关闭
        lifecycleScope.launch {
            appSettingsRepository.bgType.collect { type ->
                applyWallpaperMode(type == "mica")
            }
        }

        setContent {
            FHRebornApp()
        }
    }

    override fun onStart() {
        super.onStart()
        applyEdgeToEdgeWithDelay(currentThemeMode)
    }

    /**
     * 主页面背景为「云母」时把窗口背景置为透明，让主题常开的 `windowShowWallpaper` 透出系统壁纸；
     * 其余背景类型恢复不透明的主题背景色。
     */
    private fun applyWallpaperMode(enable: Boolean) {
        if (enable) {
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        } else {
            val tv = android.util.TypedValue()
            theme.resolveAttribute(android.R.attr.colorBackground, tv, true)
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(tv.data))
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyEdgeToEdgeWithDelay(currentThemeMode)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyEdgeToEdgeWithDelay(currentThemeMode)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 如果 App 已经在运行，从通知栏/媒体控件回到 App 时直接弹出播放器
        if (intent.action == Intent.ACTION_MAIN && playerViewModel.currentSong.value != null) {
            playerViewModel.requestOpenPlayer()
        }
    }

    /**
     * 立即 + 延迟重新应用 Edge-to-Edge。
     * 小窗/分屏/resize 后系统可能会在我们设置完之后再重置导航栏属性，
     * 所以多 post 几次，确保最终状态正确。
     */
    private fun applyEdgeToEdgeWithDelay(themeMode: String?) {
        applyEdgeToEdge(themeMode)
        window.decorView.post { applyEdgeToEdge(themeMode) }
        window.decorView.postDelayed({ applyEdgeToEdge(themeMode) }, 100)
        window.decorView.postDelayed({ applyEdgeToEdge(themeMode) }, 300)
    }

    /**
     * 重新应用 Edge-to-Edge 和状态栏/导航栏反色。
     * 在 onCreate、onStart、onConfigurationChanged、onWindowFocusChanged 都会调用，
     * 避免进入小窗/分屏/恢复时系统重置这些属性导致沉浸失效。
     */
    private fun applyEdgeToEdge(themeMode: String?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        @Suppress("DEPRECATION")
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        @Suppress("DEPRECATION")
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            window.isStatusBarContrastEnforced = false
            @Suppress("DEPRECATION")
            window.isNavigationBarContrastEnforced = false
        }

        val isSystemDark =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
        val isDark = when (themeMode) {
            "light" -> false
            "dark" -> true
            else -> isSystemDark
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }
}
