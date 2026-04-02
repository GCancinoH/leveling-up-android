package com.gcancino.levelingup.presentation.onboarding

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.gcancino.levelingup.ui.theme.SystemColors
import com.gcancino.levelingup.ui.theme.purpleBlueGradient
import com.gcancino.levelingup.utils.rememberSystemSoundPlayer
import kotlinx.coroutines.delay

@ExperimentalMaterial3Api
@Composable
fun CounterDialog(
    ctx: Context,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var countDown by remember { mutableIntStateOf(10) }
    var infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val playSystemSound = rememberSystemSoundPlayer()

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse scale"
    )

    LaunchedEffect(Unit) {
        playSystemSound(ctx)
        while(countDown > 0) {
            delay(1000)
            countDown--
        }
        onDecline()
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
            enter = fadeIn(animationSpec = tween(500))
        ) {
            Column(
                modifier = Modifier
                    .padding(40.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚠️ SYSTEM NOTIFICATION",
                    style = TextStyle(
                        color = SystemColors.ErrorColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = SystemColors.ErrorColor.copy(alpha = 0.7f),
                            blurRadius = 15f
                        )
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "If you do not accept the terms of The System, your heart will stop in:",
                    style = TextStyle(
                        color = SystemColors.TextColor.copy(alpha = 0.85f),
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Countdown Circle
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(if (countDown <= 3) pulseScale else 1f)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            if (countDown <= 3) SystemColors.ErrorColor else SystemColors.GlowBlue,
                            CircleShape
                        )
                        .background(
                            if (countDown <= 3)
                                SystemColors.ErrorColor.copy(alpha = 0.1f)
                            else
                                SystemColors.GlowBlue.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = countDown.toString(),
                        style = TextStyle(
                            color = SystemColors.TextColor,
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    )
                }

                Spacer(modifier = Modifier.height(50.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onDecline,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            contentColor = SystemColors.TextColor
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "DECLINE",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(SystemColors.GlowBlue, SystemColors.GlowPurple)
                                    ),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ACCEPT",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}