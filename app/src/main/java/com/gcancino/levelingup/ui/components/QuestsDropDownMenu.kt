package com.gcancino.levelingup.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gcancino.levelingup.R
import com.gcancino.levelingup.domain.models.Quests
import com.gcancino.levelingup.presentation.player.dashboard.viewModels.QuestMenuViewModel
import com.gcancino.levelingup.ui.components.quests.QuestDetailBottomSheet

@ExperimentalMaterial3Api
@Composable
fun QuestDropDownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    viewModel: QuestMenuViewModel,
    navController: NavHostController
) {
    val quests by viewModel.quests.collectAsState()
    var showQuestDetailsDialog by remember { mutableStateOf<Quests?>(null) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = DpOffset((-10).dp, 8.dp),
    ) {
        if (quests.isEmpty()) {
            DropdownMenuItem(
                text = { Text("No Quest available at the moment.") },
                onClick = { onDismissRequest() },
            )
        } else {
            quests.map { quest ->
                DropdownMenuItem(
                    modifier = Modifier.padding(0.dp, 8.dp),
                    text = {
                        quest.title?.let { Text(it) }
                    },
                    onClick = {
                        showQuestDetailsDialog = quest
                        onDismissRequest()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.quest_item_icon),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )
                HorizontalDivider(
                    color = Color.LightGray,
                    modifier = Modifier.padding(8.dp, 0.dp)
                )
            }
        }
    }

    // -- Dialog for Quest Details --
    showQuestDetailsDialog?.let { questToShow ->
        /*QuestDetailDialog(
            quest = questToShow,
            onDismissAction = { showQuestDetailsDialog = null }
        )*/
        QuestDetailBottomSheet(
            modifier = Modifier,
            quest = questToShow,
            onDismiss = { showQuestDetailsDialog = null },
            onAccept = {
                navController.navigate("questStared/${questToShow.id}")
            }
        )
    }
}
