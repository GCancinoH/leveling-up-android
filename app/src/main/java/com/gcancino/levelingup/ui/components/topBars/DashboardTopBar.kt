package com.gcancino.levelingup.ui.components.topBars

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gcancino.levelingup.R
import com.gcancino.levelingup.ui.components.QuestDropDownMenu

@ExperimentalMaterial3Api
@Composable
fun DashboardTopBar(
    expandDropDown: Boolean,
    navController: NavHostController
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Leveling Up",
                color = Color.White
            )
        },
        actions = {
            IconButton(
                onClick = { expandDropDown }
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.quest_icon),
                    contentDescription = null
                )
            }
            QuestDropDownMenu(
                expanded = expandDropDown,
                onDismissRequest = { expandDropDown },
                viewModel = hiltViewModel(),
                navController = navController
            )
        }
    )

}