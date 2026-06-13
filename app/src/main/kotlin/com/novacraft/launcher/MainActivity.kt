package com.novacraft.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.novacraft.launcher.ui.navigation.NovaCraftNavHost
import com.novacraft.launcher.ui.theme.NovaCraftTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity
 *
 * Single-activity architecture entry point. Hosts the Compose navigation graph.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NovaCraftTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NovaCraftNavHost()
                }
            }
        }
    }
}
