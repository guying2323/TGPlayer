package com.tgplayer.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.tgplayer.android.ui.AppRoot
import com.tgplayer.android.data.Settings
import com.tgplayer.android.ui.theme.ThemeController
import com.tgplayer.android.ui.theme.TmPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
        ThemeController.install(Settings.get())
        setContent {
            val mode by ThemeController.mode.collectAsState()
            val dynamic by ThemeController.dynamic.collectAsState()
            val dark = when (mode) {
                1 -> true
                2 -> false
                else -> isSystemInDarkTheme()
            }
            TmPlayerTheme(darkTheme = dark, dynamicColor = dynamic) {
                AppRoot()
            }
        }
    }
}
