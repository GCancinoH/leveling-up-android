package com.gcancino.levelingup.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.gcancino.levelingup.presentation.player.dashboard.components.BodyCompositionBottomSheet
import com.gcancino.levelingup.presentation.player.dashboard.viewModels.BodyCompositionViewModel
import com.gcancino.levelingup.ui.theme.PurpleGrey40
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
@Composable
fun ExpandableFloatingButton(
    snackbarState: SnackbarHostState,
    bodyCompositionViewModel: BodyCompositionViewModel,
) {
    var expanded by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<MiniFabItem?>(null) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()

    val bodyCompositionHasEntryForToday by bodyCompositionViewModel.hasEntryToday.collectAsState()

    val items = listOf(
        MiniFabItem(Icons.Outlined.Bed, "Sleep") {
            showBottomSheet = true
            selectedItem = it
            expanded = false
        },
        MiniFabItem(Icons.Outlined.Bloodtype, "Blood Pressure") {
            showBottomSheet = true
            selectedItem = it
            expanded = false
        },
        MiniFabItem(Icons.Outlined.MonitorWeight, "Body Composition") {
            if (!bodyCompositionHasEntryForToday) {
                showBottomSheet = true
                selectedItem = it
                expanded = false
            } else {
                expanded = false
                selectedItem = null
                showBottomSheet = false
                scope.launch {
                    snackbarState.showSnackbar("You already have an entry for today!", "x")
                }
            }
        },
    )

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
                selectedItem = null
            },
            sheetState = sheetState,
            modifier = Modifier.wrapContentHeight(),
        ) {
            when (selectedItem?.title) {
                "Body Composition" -> {
                    BodyCompositionBottomSheet(
                        snackbarState = snackbarState,
                        viewModel = bodyCompositionViewModel,
                        onDismiss = {
                            selectedItem = null
                            showBottomSheet = false
                        }
                    )
                }
                else -> null
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.End
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = {it}) + expandVertically(),
            exit = fadeOut() + slideOutVertically(targetOffsetY = {it}) + shrinkVertically()
        ) {
            LazyColumn(
                modifier = Modifier.padding(8.dp)
            ) {
                items(items.size) {
                    ItemUI(
                        icon = items[it].icon,
                        title = items[it].title,
                        onClick = {
                            scope.launch {
                                sheetState.expand()
                            }
                            items[it].onClick(items[it])
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        val transition = updateTransition(targetState = expanded, label = "transition")
        val rotation by transition.animateFloat(label = "rotation") {
            if (it) 315f else 0f
        }

        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = PurpleGrey40
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

@Composable
fun ItemUI(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(text = title)
        Spacer(modifier = Modifier.width(10.dp))
        FloatingActionButton(
            onClick = {
                onClick()
            },
            modifier = Modifier.size(45.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        }
    }
}

data class MiniFabItem(
    val icon: ImageVector,
    val title: String,
    val onClick: (MiniFabItem) -> Unit
)