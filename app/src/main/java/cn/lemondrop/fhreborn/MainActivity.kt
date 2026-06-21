package cn.lemondrop.fhreborn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import cn.lemondrop.fhreborn.util.CrashHandler
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
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
}
