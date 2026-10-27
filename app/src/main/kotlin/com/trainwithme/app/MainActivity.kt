package com.trainwithme.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.trainwithme.app.ui.AppViewModel
import com.trainwithme.app.ui.TrainWithMeApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single activity. Compose owns the whole UI; edge-to-edge is enabled and each screen handles
 * its own insets through Scaffold.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TrainWithMeApp(
                appViewModel = hiltViewModel<AppViewModel>(),
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
            )
        }
    }
}
