package com.gcancino.levelingup.ui.components.topBars

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcancino.levelingup.R
import com.gcancino.levelingup.domain.models.notification.NotificationItem
import com.gcancino.levelingup.ui.components.notifications.NotificationsDropDown

@ExperimentalMaterial3Api
@Composable
fun DashboardTopBar(
    onProfileClick: () -> Unit,
    notificationCount: Int,
    notificationsExpanded: Boolean,
    onNotificationsBellClick: () -> Unit,
    onNotificationsDismiss: () -> Unit,
    notifications: List<NotificationItem>,
    onNotificationTapped: (NotificationItem) -> Unit,
) {
    CenterAlignedTopAppBar(
        navigationIcon = {
            IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.app_name),
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            )
        },
        actions = {
            Box(
                modifier = Modifier.wrapContentSize(Alignment.TopStart)
            ) {
                IconButton(onClick = onNotificationsBellClick) {
                    Icon(
                        modifier           = Modifier.size(26.dp),
                        imageVector        = Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        tint               = Color.White
                    )
                }
                // Red dot badge — only visible when there are notifications
                if (notificationCount > 0) {
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp),
                        containerColor = Color(0xFFE53935)
                    ) {
                        Text(
                            text  = if (notificationCount > 9) "9+" else notificationCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
                // Notifications dropdown anchored to the bell icon
                NotificationsDropDown(
                    expanded             = notificationsExpanded,
                    notifications        = notifications,
                    onDismissRequest     = onNotificationsDismiss,
                    onNotificationTapped = onNotificationTapped
                )
            }
        }
    )
}
