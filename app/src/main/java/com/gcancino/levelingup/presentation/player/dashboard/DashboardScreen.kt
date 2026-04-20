package com.gcancino.levelingup.presentation.player.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.core.SyncState
import com.gcancino.levelingup.domain.models.notification.NotificationType
import com.gcancino.levelingup.presentation.player.dashboard.components.BodyCompositionBottomSheet
import com.gcancino.levelingup.presentation.player.dashboard.components.TaskCreationBottomSheet
import com.gcancino.levelingup.presentation.player.dashboard.components.TasksCard
import com.gcancino.levelingup.presentation.player.dashboard.components.TodaySessionCard
import com.gcancino.levelingup.presentation.player.dashboard.viewModels.BodyCompositionViewModel
import com.gcancino.levelingup.presentation.player.dashboard.viewModels.CalendarViewmodel
import com.gcancino.levelingup.presentation.player.dashboard.viewModels.TasksViewModel
import com.gcancino.levelingup.presentation.player.identity.ActiveQuestCard
import com.gcancino.levelingup.presentation.player.identity.ActiveQuestViewModel
import com.gcancino.levelingup.presentation.player.identity.IdentityCard
import com.gcancino.levelingup.presentation.player.identity.IdentityViewModel
import com.gcancino.levelingup.ui.components.DailyFlowCTACard
import com.gcancino.levelingup.ui.components.calendar.WeeklyCalendar
import com.gcancino.levelingup.ui.theme.BackgroundColor
import java.time.LocalDate
import com.gcancino.levelingup.ui.components.ExpandableFloatingButton
import com.gcancino.levelingup.ui.components.notifications.NotificationsViewModel
import com.gcancino.levelingup.ui.components.topBars.DashboardTopBar
import timber.log.Timber

@ExperimentalMaterial3Api
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    bodyCompositionBottomSheetViewModel: BodyCompositionViewModel = hiltViewModel(),
    notificationViewModel: NotificationsViewModel = hiltViewModel(),
    tasksViewModel: TasksViewModel = hiltViewModel(),
    activeQuestViewModel: ActiveQuestViewModel = hiltViewModel(),
    identityViewModel: IdentityViewModel = hiltViewModel(),
    onStartSession: (LocalDate) -> Unit,
    onSetupIdentity: () -> Unit,
    onViewStandards: () -> Unit,
    onProfileClick: () -> Unit,
    onStartMorningFlow: () -> Unit,
    onStartEveningFlow: () -> Unit,
) {
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val calendarViewModel: CalendarViewmodel = viewModel()
    val selectedDate by calendarViewModel.selectedDate.collectAsStateWithLifecycle()
    val todaySessionState by viewModel.todaySession.collectAsStateWithLifecycle()
    val notifications by notificationViewModel.notifications.collectAsStateWithLifecycle()
    val notificationCount by notificationViewModel.notificationCount.collectAsStateWithLifecycle()

    /* Snackbar and dropdown */
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    var isNotificationExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    /* Bottom sheet state for initial data notifications */
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBodyCompositionSheet by remember { mutableStateOf(false) }
    val showTaskCreationSheet by viewModel.showTaskCreationSheet.collectAsStateWithLifecycle()

    /* Tasks variables */
    val tasks by tasksViewModel.tasks.collectAsStateWithLifecycle()
    val canAddMore by tasksViewModel.canAddMore.collectAsStateWithLifecycle()

    // Observe Task Save Events
    LaunchedEffect(Unit) {
        tasksViewModel.saveSuccess.collect {
            viewModel.closeTaskCreation()
            snackbarHostState.showSnackbar("Daily tasks updated!")
        }
    }

    LaunchedEffect(Unit) {
        tasksViewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (showBodyCompositionSheet) {
        BodyCompositionBottomSheet (
            viewModel = bodyCompositionBottomSheetViewModel,
            onDismiss = {
                showBodyCompositionSheet = false
                notificationViewModel.refresh()
            },
            snackbarState = snackbarHostState
        )
    }

    if (showTaskCreationSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeTaskCreation() },
            sheetState = sheetState,
            containerColor = Color(0xFF0A0A0A), // Dark System theme
            scrimColor = Color.Black.copy(alpha = 0.7f)
        ) {
            Box(modifier = Modifier.padding(bottom = 32.dp, start = 16.dp, end = 16.dp)) {
                TaskCreationBottomSheet (
                    tasks = tasks,
                    canAddMore = canAddMore,
                    onAddTask = { title, priority ->
                        tasksViewModel.addTask(title, priority)
                    },
                    onRemoveTask = { id -> tasksViewModel.removeTask(id) },
                    onSave = { tasksViewModel.saveTasks() }
                )
            }
        }
    }

    /* Daily Tasks */
    val showMorningCTA by viewModel.showMorningCTA.collectAsStateWithLifecycle()
    val showEveningCTA by viewModel.showEveningCTA.collectAsStateWithLifecycle()

    // Log sync state changes for debugging
    LaunchedEffect(syncState) {
        when (syncState) {
            is SyncState.Syncing -> Timber.tag("DashboardScreen").d("Syncing week data...")
            is SyncState.Success -> Timber.tag("DashboardScreen").d("Sync complete")
            is SyncState.Error   -> Timber.tag("DashboardScreen").e("Sync error: ${(syncState as SyncState.Error).message}")
            is SyncState.Idle    -> Unit
        }
    }

    // Reload session whenever the user picks a different date in the calendar
    LaunchedEffect(selectedDate) {
        viewModel.getSessionForDate(selectedDate)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundColor,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar         = {
            DashboardTopBar(
                onProfileClick = onProfileClick,
                notificationCount = notificationCount,
                notificationsExpanded = isNotificationExpanded,
                onNotificationsBellClick = { isNotificationExpanded = true },
                onNotificationsDismiss = { isNotificationExpanded = false },
                notifications            = notifications,
                onNotificationTapped     = { notification ->
                    when (notification.type) {
                        NotificationType.MISSING_INITIAL_MEASUREMENTS,
                        NotificationType.MISSING_INITIAL_COMPOSITION -> {
                            showBodyCompositionSheet = true
                        }
                        else -> notification.action?.invoke()
                    }
                }
            )
        },
        floatingActionButton = {
            ExpandableFloatingButton(
                snackbarState = snackbarHostState,
                bodyCompositionViewModel = bodyCompositionBottomSheetViewModel,
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 16.dp)
        ) {
            WeeklyCalendar(viewModel = calendarViewModel)
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .weight(1f)
            ) {
                if (syncState is SyncState.Syncing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color    = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (showMorningCTA) {
                    DailyFlowCTACard(
                        title    = "Morning Reflection 🌅",
                        subtitle = "Start your day with intention",
                        color    = Color(0xFFFFB300),
                        onClick  = onStartMorningFlow
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (showEveningCTA) {
                    DailyFlowCTACard(
                        title    = "Evening Reflection 🌙",
                        subtitle = "Review your day and set tomorrow's tasks",
                        color    = Color(0xFF7986CB),
                        onClick  = onStartEveningFlow
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                when (val resource = todaySessionState) {
                    is Resource.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    is Resource.Success -> {
                        TodaySessionCard(
                            session = resource.data,
                            isToday = selectedDate == LocalDate.now(),
                            onStartSession = { onStartSession(selectedDate) }
                        )
                    }

                    is Resource.Error -> {
                        Text(
                            text = "Error: ${resource.message}",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                /*TasksCard(
                    viewModel    = tasksViewModel,
                    onViewAllTasks = { onViewTasks() },
                    onEmptyTasks = { viewModel.openTaskCreation() }
                )*/
                IdentityCard(
                    viewModel = identityViewModel,
                    onViewStandards = onViewStandards,
                    onSetupIdentity = onSetupIdentity
                )
                Spacer(modifier = Modifier.height(4.dp))
                ActiveQuestCard(
                    viewModel = activeQuestViewModel
                )
            }
        }
    }
}
