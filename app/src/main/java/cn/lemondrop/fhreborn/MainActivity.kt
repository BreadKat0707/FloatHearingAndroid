package cn.lemondrop.fhreborn

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashHandler.init(applicationContext)
        enableEdgeToEdge()

        val appSettingsRepository = AppSettingsRepository(this)
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

        setContent {
            FHRebornApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 如果 App 已经在运行，从通知栏/媒体控件回到 App 时直接弹出播放器
        if (intent.action == Intent.ACTION_MAIN && playerViewModel.currentSong.value != null) {
            playerViewModel.requestOpenPlayer()
        }
    }
}
