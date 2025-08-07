package com.gcancino.levelingup.presentation.auth.signUp.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.player.Improvement
import com.gcancino.levelingup.ui.components.TappableCard

@Composable
fun ImprovementStep(
    viewModel: ImprovementsViewModel
) {
    val buttonOnClick: () -> Unit
    val buttonContent: @Composable RowScope.() -> Unit
    val improvements = Improvement.entries
    val state by viewModel.savedState.collectAsState()

    when (state) {
        is Resource.Success -> {
            buttonOnClick = { viewModel.goToNextStep() }
            buttonContent = {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Next"
                )
            }
        }
        is Resource.Loading -> {
            buttonOnClick = { /* Button is typically disabled when loading, or action does nothing */ }
            buttonContent = {
                CircularProgressIndicator(
                    // Use MaterialTheme colors if available for consistency
                    color = Color.Black, // Example if button container is primary
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        else -> {
            buttonOnClick = { viewModel.saveImprovementsData() }
            buttonContent = {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = "Save"
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "Improvements",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Choose the areas you'd lie to focus on improving. Your selections will help " +
                        "us create a personalized plan tailored to your specific goals. Select all that apply " +
                        "and start your journey with Leveling Up!",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(
                    count = improvements.size,
                    itemContent = { index ->
                        val improvement = improvements[index]
                        TappableCard(
                            text = when (improvement) {
                                Improvement.STRENGTH -> "Strength"
                                Improvement.MOBILITY -> "Mobility"
                                Improvement.SELF_DEVELOPMENT -> "Self-Development"
                                Improvement.RECOVERY -> "Recovery"
                                Improvement.MENTAL_TOUGHNESS -> "Mental Toughness"
                                Improvement.FINANCE -> "Finance"
                                else -> null
                            }.toString(),
                            isSelected = viewModel.selectedImprovements.contains(improvement),
                            onClick = { viewModel.toggleImprovement(improvement) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (viewModel.selectedImprovements.isNotEmpty()) {
            Button(
                onClick = buttonOnClick,
                modifier = Modifier.align(Alignment.BottomEnd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                buttonContent()
            }
        }

    }
}