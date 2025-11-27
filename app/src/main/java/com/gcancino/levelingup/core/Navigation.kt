package com.gcancino.levelingup.core

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gcancino.levelingup.presentation.auth.signIn.SignInScreen
import com.gcancino.levelingup.presentation.auth.signUp.SignUpScreen
import com.gcancino.levelingup.presentation.initialization.InitScreen
import com.gcancino.levelingup.presentation.player.dashboard.DashboardScreen
import com.gcancino.levelingup.ui.components.ExpandableFloatingButton
import com.gcancino.levelingup.ui.theme.BackgroundColor
import kotlinx.coroutines.CoroutineScope
import com.gcancino.levelingup.ui.components.quests.QuestStartedScreen
import com.gcancino.levelingup.ui.components.topBars.DashboardTopBar
import com.gcancino.levelingup.ui.components.topBars.QuestStartedTopBar

@ExperimentalMaterial3Api
@Composable
fun Navigation() {
    val navController: NavHostController = rememberNavController()
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var expandedDropDown by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute == "dashboard") {
                DashboardTopBar(
                    expandDropDown = expandedDropDown,
                    appContainer = appContainer,
                    navController = navController
                )
            }
            if (currentRoute == "questStarted") {
                QuestStartedTopBar(
                    questTitle = "",
                    onBackClick = { navController.popBackStack() },
                    onSaveClick = { /*TODO*/ },
                    isSaving = false
                )
            }
        },
        floatingActionButton = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute == "dashboard") {
                ExpandableFloatingButton(
                    snackbarState = snackbarHostState,
                    bodyCompositionViewModel = hiltViewModel(),
                )

            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
                .background(BackgroundColor)
        ) {
            composable("initScreen") {
                InitScreen(
                    viewModel = hiltViewModel(),
                    onSignedIn = {
                        navController.navigate("dashboard") {
                            popUpTo("initScreen") {
                                inclusive = true
                            }
                        }
                    },
                    onSignInError = {
                        navController.navigate("signIn") {
                            popUpTo("initScreen") {
                                inclusive = true
                            }
                        }
                    }
                )
            }
            composable("signIn") {
                SignInScreen(
                    viewModel = hiltViewModel(),
                    onSignedIn = { TODO() },
                    onSignInError = { TODO() },
                    onGoToSignUp = {
                        navController.navigate("signUp") {
                            popUpTo("signIn") {
                                inclusive = true
                            }
                        }
                    }
                )
            }
            composable("signUp") {
                SignUpScreen(
                    viewModel = hiltViewModel(),
                    ctx = context
                )
            }
            composable("dashboard") {
                DashboardScreen(
                    viewModel = hiltViewModel(),
                    bodyCompositionBottomSheetViewModel = hiltViewModel()

                )
            }
            composable(
                route = "questStared/{questID}",
                arguments = listOf(
                    navArgument("questID") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val questID = backStackEntry.arguments?.getString("questID")

                QuestStartedScreen(
                    questID = questID ?: "",
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )


            }
            /*composable("improvements") {
                ImprovementScreen(
                    viewModel = appContainer.improvementViewModel
                )
            }
            composable("initialData") {
                InitialDataScreen(
                    viewModel = appContainer.initialDataViewModel,
                    navController = navController
                )
            }
            composable("forgotPassword") {
                /* TODO() */
            }
            */
        }
    }
}