package cn.lemondrop.fhreborn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cn.lemondrop.fhreborn.ui.theme.FloatHearingTheme

import cn.lemondrop.fhreborn.util.CrashHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashHandler.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            FloatHearingTheme {
                FHRebornApp()
            }
        }
    }
}
