package io.github.th3s1nc.setuprechner

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.th3s1nc.setuprechner.ui.DARK_BACKGROUND
import io.github.th3s1nc.setuprechner.ui.LIGHT_BACKGROUND
import io.github.th3s1nc.setuprechner.ui.SetupTheme
import io.github.th3s1nc.setuprechner.ui.SetupApp

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        // In der App gewählte Sprache anwenden (Info > Sprache)
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Helles oder dunkles Design (Info > Design): Fensterhintergrund und Symbole der Systemleisten passend dazu
        val light = AppDesign.isLight(this)
        window.decorView.setBackgroundColor(if (light) LIGHT_BACKGROUND else DARK_BACKGROUND)
        val bars = if (light) SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT) else SystemBarStyle.dark(Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        setContent {
            SetupTheme(light) {
                SetupApp()
            }
        }
    }
}
