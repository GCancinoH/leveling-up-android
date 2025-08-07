package com.gcancino.levelingup.ui.components

import android.widget.Space
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gcancino.levelingup.domain.models.Quests
import com.gcancino.levelingup.R
import com.gcancino.levelingup.ui.theme.purpleBlueGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestDetailDialog(
    quest: Quests,
    onDismissAction: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismissAction,
    ) {
        Surface(
            modifier = Modifier.wrapContentWidth()
                .wrapContentHeight()
                .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp)),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = quest.title ?: "",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Description
                Text(
                    text = quest.description ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                /**
                 * Rewards
                 */
                Column(
                    modifier = Modifier.background(Color(0x80222222))
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,

                ) {
                    Text(
                        text = "REWARDS",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(0.dp, 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                            .border(0.dp, Color.Transparent, RoundedCornerShape(10.dp))
                            .padding(0.dp, 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.trophy_icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "XP",
                                    style = MaterialTheme.typography.bodyMedium,
                                )

                            }
                            Text(
                                text = quest.rewards?.xp.toString() + " points",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(4.dp, 0.dp, 0.dp, 0.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.coins_icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Coins",
                                    style = MaterialTheme.typography.bodyMedium,
                                )

                            }
                            Text(
                                text = quest.rewards?.xp.toString() + " coins",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                QuestTimerCircularProgressBar()
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { /* TODO() */ },
                        modifier = Modifier
                            .weight(1f)
                            .background(color = Color.LightGray, shape = RoundedCornerShape(10.dp))
                    ) {
                        Text(
                            text = "Dismiss",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onDismissAction,
                        modifier = Modifier
                            .weight(1f)
                            .background(brush = purpleBlueGradient, shape = RoundedCornerShape(10.dp))
                    ) { Text(
                        text = "Start",
                        style = MaterialTheme.typography.titleMedium
                    ) }
                }
            }
        }
    }
}