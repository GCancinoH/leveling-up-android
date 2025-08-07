package com.gcancino.levelingup.presentation.player.dashboard

import android.util.Log
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.presentation.player.dashboard.viewModels.BodyCompositionBottomSheetViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    bodyCompositionBottomSheetViewModel: BodyCompositionBottomSheetViewModel
) {
    val state = viewModel.state.collectAsState()

    LaunchedEffect(state.value) {
        if (state.value is Resource.Success) {
            Log.d("DashboardScreen", "Quests saved locally successfully")
        }
    }

    Button(
        onClick = {
            viewModel.saveQuestsLocally()
        }
    ) {
        Text("Save Quests Locally")
    }
}

