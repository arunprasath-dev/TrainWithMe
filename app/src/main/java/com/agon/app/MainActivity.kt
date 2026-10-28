package com.agon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agon.app.data.repository.ThemeMode
import com.agon.app.ui.theme.TrainWithMeTheme
import com.agon.app.viewmodel.AppViewModel
import com.agon.app.viewmodel.AppViewModelFactory

/**
 * The single activity. Compose owns the whole UI; edge-to-edge is enabled and each screen
 * handles its own insets through Scaffold.
 */
class MainActivity : ComponentActivity() {

    private val factory: AppViewModelFactory by lazy {
        AppViewModelFactory(application as TrainWithMeApp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val appViewModel: AppViewModel = viewModel(factory = factory)
            val themeMode by appViewModel.themeMode.collectAsState()
            TrainWithMeTheme(
                darkTheme = when (themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                },
            ) {
                TrainWithMeNav(
                    factory = factory,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
