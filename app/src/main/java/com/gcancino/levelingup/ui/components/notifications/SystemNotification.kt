package com.gcancino.levelingup.ui.components.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

@Composable
fun SystemNotification(message: String, subMessage: String) {
    Surface(
        color = Color.Black.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, Color(0xFF7986CB)), // Your System Blue
        shape = RoundedCornerShape(2.dp), // Sharp edges
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "[ SYSTEM MESSAGE ]",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF7986CB),
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontFamily = FontFamily.Monospace // Gives that System feel
            )
            Text(
                text = subMessage,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}


@Composable
fun SystemNotificationOverlay(
    message: String,
    subMessage: String,
    isVisible: Boolean,
    onTimeout: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 20.dp, end = 20.dp)
                .zIndex(10f) // Ensure it's above everything
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, Color(0xFF7986CB)), // System Blue
                shape = RoundedCornerShape(2.dp), // Sharp edges for the "System" look
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "[ SYSTEM MESSAGE ]",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF7986CB),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = subMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }

    // Auto-hide after 3 seconds
    if (isVisible) {
        LaunchedEffect(Unit) {
            delay(4000)
            onTimeout()
        }
    }
}