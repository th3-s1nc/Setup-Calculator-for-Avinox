package io.github.th3s1nc.setuprechner

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.th3s1nc.setuprechner.ui.SetupTheme
import io.github.th3s1nc.setuprechner.ui.SetupApp

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        // In der App gewählte Sprache anwenden (Info > Sprache)
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Die App ist immer dunkel, deshalb helle Symbole in Status- und Navigationsleiste.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            SetupTheme {
                SetupApp()
            }
        }
    }
}
