package com.gcancino.levelingup.core

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDeepLink
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.gcancino.levelingup.presentation.auth.signIn.SignInScreen
import com.gcancino.levelingup.presentation.initialization.InitScreen
import com.gcancino.levelingup.presentation.onboarding.OnboardingScreen
import com.gcancino.levelingup.presentation.player.dailyTasks.EveningFlowScreen
import com.gcancino.levelingup.presentation.player.dailyTasks.MorningFlowScreen
import com.gcancino.levelingup.presentation.player.dailyTasks.TasksScreen
import com.gcancino.levelingup.presentation.player.dailyTasks.WeeklyFlowScreen
import com.gcancino.levelingup.presentation.player.dashboard.DashboardScreen
import com.gcancino.levelingup.presentation.player.identity.IdentitySetupScreen
import com.gcancino.levelingup.presentation.player.identity.IdentityWallScreen
import com.gcancino.levelingup.presentation.player.identity.StandardsScreen
import com.gcancino.levelingup.presentation.player.profile.ProfileScreen
import com.gcancino.levelingup.presentation.player.session.SessionPlayerScreen
import com.gcancino.levelingup.ui.theme.BackgroundColor
import com.gcancino.levelingup.ui.components.quests.QuestStartedScreen
import java.time.LocalDate
import java.time.ZoneId

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun Navigation() {
    val navController: NavHostController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "initScreen",
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        composable("initScreen") {
            InitScreen(
                viewModel = hiltViewModel(),
                onSignedIn = {
                    navController.navigate("dashboard") {
                        popUpTo("initScreen") { inclusive = true }
                    }
                },
                onSignInError = {
                    navController.navigate("signIn") {
                        popUpTo("initScreen") { inclusive = true }
                    }
                },
                onNeedsOnboarding = {
                    navController.navigate("onboarding") {
                        popUpTo("initScreen") { inclusive = true }
                    }
                }
            )
        }

        composable("signIn") {
            SignInScreen(
                viewModel = hiltViewModel(),
                onSignedIn = {
                    navController.navigate("dashboard") {
                        popUpTo("signIn") { inclusive = true }
                    }
                },
                onSignInError = { /* TODO */ },
                onGoToSignUp = {
                    navController.navigate("signUp") {
                        popUpTo("initScreen") { inclusive = true }
                    }
                }
            )
        }

        /**
         * Onboarding screen
         */
        composable("onboarding") {
            OnboardingScreen(
                viewModel    = hiltViewModel(),
                onCompleted  = {
                    navController.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onDismiss = {
                    navController.navigate("dashboard")
                }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                viewModel = hiltViewModel(),
                bodyCompositionBottomSheetViewModel = hiltViewModel(),
                notificationViewModel = hiltViewModel(),
                tasksViewModel = hiltViewModel(),
                activeQuestViewModel = hiltViewModel(),
                identityViewModel = hiltViewModel(),
                onStartSession = { date: LocalDate ->
                    val timestamp = date
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    navController.navigate("sessionPlayer/$timestamp")
                },
                onViewStandards = { navController.navigate("standards") },
                onSetupIdentity = { navController.navigate("identitySetup") },
                onProfileClick = { navController.navigate("profile") },
                onStartMorningFlow = { navController.navigate("morningFlow") },
                onStartEveningFlow = { navController.navigate("eveningFlow") }
            )
        }

        composable("profile") {
            ProfileScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToIdentityWall = { navController.navigate("identity/wall") },
                onSignOut = {
                    navController.navigate("signIn") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("questStarted") {
            QuestStartedScreen(
                viewModel      = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("standards") {
            StandardsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("identity/wall") {
            IdentityWallScreen(
                viewModel      = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("identitySetup") {
            IdentitySetupScreen(
                viewModel   = hiltViewModel(),
                onCompleted = { navController.popBackStack() }
            )
        }

        composable(
            route     = "sessionPlayer/{date}",
            arguments = listOf(navArgument("date") { type = NavType.LongType })
        ) {
            SessionPlayerScreen(
                viewModel      = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "morningFlow",
            deepLinks = listOf(navDeepLink { uriPattern = "levelingup://morning_flow" })
        ) {
            MorningFlowScreen(onCompleted = { navController.popBackStack() })
        }
        composable(
            route = "eveningFlow",
            deepLinks = listOf(navDeepLink { uriPattern = "levelingup://evening_flow" })
        ) {
            EveningFlowScreen(onCompleted = { navController.popBackStack() })
        }
        composable(
            route = "weeklyFlow",
            deepLinks = listOf(navDeepLink { uriPattern = "levelingup://weekly_reset" })
        ) {
            WeeklyFlowScreen(onCompleted = { navController.popBackStack() })
        }
        composable("tasks") {
            TasksScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
