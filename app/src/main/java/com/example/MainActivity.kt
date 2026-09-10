package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.MainAppLayout
import com.example.ui.MainViewModel
import com.example.ui.theme.HealthBridgeTheme

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val userSession by mainViewModel.userSession.collectAsState()
            val darkTheme = userSession?.darkThemeEnabled ?: true

            HealthBridgeTheme(darkTheme = darkTheme) {
                MainAppLayout(viewModel = mainViewModel)
            }
        }
    }
}

