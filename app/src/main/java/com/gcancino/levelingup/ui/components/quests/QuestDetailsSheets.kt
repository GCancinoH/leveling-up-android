package com.gcancino.levelingup.ui.components.quests

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gcancino.levelingup.domain.models.Quests
import com.gcancino.levelingup.R
import com.gcancino.levelingup.ui.components.QuestTimerCircularProgressBar
import com.gcancino.levelingup.ui.theme.Blue40
import com.gcancino.levelingup.ui.theme.purpleBlueGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestDetailBottomSheet(
    modifier: Modifier = Modifier,
    quest: Quests,
    viewModel: QuestStartedViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
) {
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    var expandedDropDown by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        containerColor = Color(0xFF0A0A0A),
        contentColor = Color.White,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp),
                color = Color(0xFF1E88E5),
                shape = RoundedCornerShape(2.dp)
            ) {}
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Quest Title with glow effect
            Text(
                text = quest.title ?: "Unknown Quest",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color(0xFF9B59B6),
                        offset = Offset(0f, 2f),
                        blurRadius = 4f
                    )
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = quest.description ?: "No description available",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Rewards Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF111111),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Blue40)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "QUEST REWARDS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Blue40
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // XP Reward
                        RewardItem(
                            icon = R.drawable.trophy_icon,
                            label = "EXP",
                            value = "${quest.rewards?.xp ?: 0}",
                            color = Color(0xFFFFD700)
                        )

                        // Coins Reward
                        RewardItem(
                            icon = R.drawable.coins_icon,
                            label = "COINS",
                            value = "${quest.rewards?.coins ?: quest.rewards?.xp ?: 0}", // Fixed coins display
                            color = Color(0xFF00D4AA)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Timer Section
            QuestTimerCircularProgressBar()

            Spacer(modifier = Modifier.height(20.dp))

            // Warning Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF2D1B1B),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFE74C3C))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color(0xFFE74C3C),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WARNING: Failure to complete this quest may result in stat penalties.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFAAAA),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Decline Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFB0B0B0)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF404040)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "DECLINE",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // Accept Button
                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(brush = purpleBlueGradient, shape = RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ACCEPT QUEST",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    when(expandedDropDown) {
        true -> {
            QuestStartedScreen(
                questID = quest.id,
                onNavigateBack = onDismiss
            )
        }
        false -> {}
    }
}

@Composable
private fun RewardItem(
    icon: Int,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            color = color.copy(alpha = 0.2f),
            shape = CircleShape,
            border = BorderStroke(1.dp, color)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color(0xFFB0B0B0)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
    }
}