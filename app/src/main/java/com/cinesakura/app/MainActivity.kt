package com.cinesakura.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cinesakura.app.ui.navigation.CineSakuraNavGraph
import com.cinesakura.app.ui.theme.CineSakuraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CineSakuraTheme {
                CineSakuraNavGraph()
            }
        }
    }
}
