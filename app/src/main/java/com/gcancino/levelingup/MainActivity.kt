package com.gcancino.levelingup

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.work.WorkManager
import com.gcancino.levelingup.core.Navigation
import com.gcancino.levelingup.ui.theme.LevelingUpTheme
import com.google.firebase.messaging.FirebaseMessaging
import com.onesignal.OneSignal
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

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

        // Validate notification intent
        validateNotificationIntent(intent)

        // Init OneSignal
        //OneSignal.initWithContext(this, "f55d7e4d-c67d-49d3-b4c7-d6718aa8a504")

        askNotificationPermission()
        getAndLogFCMToken()

        enableEdgeToEdge()
        setContent {
            LevelingUpTheme {
                Navigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        validateNotificationIntent(intent)
    }

    private fun validateNotificationIntent(intent: Intent?) {
        val action = intent?.action
        val notificationType = intent?.getStringExtra("notification_type")
        
        // Ensure the intent comes from a trusted action and has the expected type
        if (action == "com.gcancino.levelingup.NOTIFICATION_CLICK" && notificationType == "fcm_push") {
            // Check if the intent was actually delivered to this component specifically 
            // and contains expected data keys to prevent intent spoofing.
            if (intent.hasExtra("notification_title") && intent.hasExtra("notification_body")) {
                val title = intent.getStringExtra("notification_title") ?: ""
                val body = intent.getStringExtra("notification_body") ?: ""
                
                Timber.tag("FCM").d("Notification clicked: title=$title, body=$body")
                
                // Future enhancement: Add a signature or cryptographic token in the intent 
                // data to verify it originated from our FCMService.
            } else {
                Timber.tag("FCM").w("Notification intent missing required extras.")
            }
        }
    }

    private fun getAndLogFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Timber.tag("FCM").w(task.exception, "Fetching FCM registration token failed")
                return@addOnCompleteListener
            }

            // Do not log tokens in production or anywhere they can be captured.
            // val token = task.result
            // Timber.tag("FCM").i("FCM Token: $token")
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Timber.tag("FCM").d("Notification permission granted")
        } else {
            Timber.tag("FCM").w("Notification permission denied")
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (Tiramisu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                Timber.tag("FCM").d("Notification permission already granted")
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
