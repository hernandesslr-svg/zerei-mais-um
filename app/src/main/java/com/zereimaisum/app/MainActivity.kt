package com.zereimaisum.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.zereimaisum.app.data.session.ThemeMode
import com.zereimaisum.app.ui.navigation.AppRoot
import com.zereimaisum.app.ui.theme.ZmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ZmApplication).container
        setContent {
            val theme by container.session.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            ZmTheme(theme) { AppRoot() }
        }
    }
}
