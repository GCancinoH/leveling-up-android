package com.gcancino.levelingup

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.WindowCompat
import com.gcancino.levelingup.core.Navigation
import com.gcancino.levelingup.ui.theme.LevelingUpTheme
import com.onesignal.OneSignal
import dagger.hilt.android.AndroidEntryPoint

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @ExperimentalMaterial3Api
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setting app orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Init OneSignal
        //OneSignal.initWithContext(this, "f55d7e4d-c67d-49d3-b4c7-d6718aa8a504")

        enableEdgeToEdge()
        setContent {
            LevelingUpTheme {
                Navigation()
            }
        }
    }
}
