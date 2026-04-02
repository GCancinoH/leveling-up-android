package com.gcancino.levelingup.ui.components.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.gcancino.levelingup.domain.models.notification.NotificationItem
import com.gcancino.levelingup.domain.models.notification.NotificationType

@Composable
fun NotificationsDropDown(
    expanded: Boolean,
    notifications: List<NotificationItem>,
    onDismissRequest: () -> Unit,
    onNotificationTapped: (NotificationItem) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = DpOffset((-10).dp, 8.dp),
        properties = PopupProperties(focusable = true)
    ) {
        // Header
        Text(
            text = "Notifications",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        HorizontalDivider(color = Color.DarkGray)

        if (notifications.isEmpty()) {
            // Empty State
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .widthIn(min = 240.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF00D4AA),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "You're all caught up!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            Column(
                modifier = Modifier.widthIn(min = 240.dp, max = 320.dp)
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                notifications.forEach { notification ->
                    NotificationRow(
                        notification = notification,
                        onTap = {
                            onDismissRequest()
                            onNotificationTapped(notification)
                        }
                    )
                    HorizontalDivider(
                        color = Color.DarkGray.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: NotificationItem,
    onTap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = notification.action != null) { onTap() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Type icon
        Icon(
            imageVector        = notification.type.icon(),
            contentDescription = null,
            tint               = notification.type.color(),
            modifier           = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = notification.title,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text  = notification.message,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            // Tap hint — only shown when there's an action
            if (notification.action != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text  = "Tap to fix →",
                    style = MaterialTheme.typography.labelSmall,
                    color = notification.type.color()
                )
            }
        }
    }
}

private fun NotificationType.icon(): ImageVector = when (this) {
    NotificationType.MISSING_INITIAL_MEASUREMENTS -> Icons.Default.Person
    NotificationType.MISSING_INITIAL_COMPOSITION  -> Icons.Default.MonitorWeight
    NotificationType.LOW_ONE_REP_MAX              -> Icons.Default.FitnessCenter
    NotificationType.INCOMPLETE_SESSION           -> Icons.Default.Timer
}

private fun NotificationType.color(): Color = when (this) {
    NotificationType.MISSING_INITIAL_MEASUREMENTS -> Color(0xFFFFB300) // amber
    NotificationType.MISSING_INITIAL_COMPOSITION  -> Color(0xFFFFB300) // amber
    NotificationType.LOW_ONE_REP_MAX              -> Color(0xFFE53935) // red
    NotificationType.INCOMPLETE_SESSION           -> Color(0xFF1E88E5) // blue
}