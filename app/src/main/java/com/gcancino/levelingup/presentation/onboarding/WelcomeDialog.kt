package com.gcancino.levelingup.presentation.onboarding

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.gcancino.levelingup.ui.theme.SystemColors
import com.gcancino.levelingup.ui.theme.purpleBlueGradient
import com.gcancino.levelingup.utils.rememberSystemSoundPlayer
import kotlinx.coroutines.delay

@ExperimentalMaterial3Api
@Composable
fun WelcomeDialog(
    ctx: Context,
    onDismiss: () -> Unit
) {
    var countdown by remember { mutableIntStateOf(5) }
    val playSystemSound = rememberSystemSoundPlayer()

    LaunchedEffect(Unit) {
        playSystemSound(ctx)
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        onDismiss()
    }

    BasicAlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        modifier = Modifier.border(1.dp, purpleBlueGradient, RoundedCornerShape(8.dp))
    ) {
        AnimatedVisibility(
            visible = true,
            enter =
                expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    expandFrom = Alignment.CenterVertically
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 300, delayMillis = 150)
                ),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                shrinkTowards = Alignment.CenterVertically
            ) + fadeOut(animationSpec = tween(durationMillis = 300))
        ) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome, Player.",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(SystemColors.GlowBlue, SystemColors.GlowPurple)
                        ),
                        shadow = Shadow(
                            color = SystemColors.GlowBlue.copy(alpha = 0.5f),
                            blurRadius = 10f
                        )
                    )
                )
                Spacer(modifier = Modifier.padding(10.dp))
                Text(
                    text = "You have been chosen by The System to evolve. Your growth will be exponential.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 18.sp,
                        color = Color.White
                    )

                )
            }
        }
    }
}