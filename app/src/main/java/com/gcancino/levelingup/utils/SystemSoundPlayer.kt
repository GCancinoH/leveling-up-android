package com.gcancino.levelingup.utils

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.gcancino.levelingup.R

@Composable
fun rememberSystemSoundPlayer() : (Context) -> Unit {
    return remember {
        { ctx ->
            try {
                val mediaPlayer = MediaPlayer.create(ctx, R.raw.notification_sound)
                mediaPlayer?.apply {
                    setOnCompletionListener { release() }
                    start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}