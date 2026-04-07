package com.gcancino.levelingup.presentation.player.identity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gcancino.levelingup.domain.models.identity.IdentityStandard
import com.gcancino.levelingup.domain.models.identity.Role
import com.gcancino.levelingup.presentation.onboarding.OnboardingIdentityStep
import com.gcancino.levelingup.ui.theme.SystemColors

@ExperimentalMaterial3Api
@Composable
fun IdentitySetupScreen(
    viewModel: IdentityViewModel = hiltViewModel(),
    onCompleted: () -> Unit
) {
    var statement by remember { mutableStateOf("") }
    var roles     by remember { mutableStateOf<List<Role>>(emptyList()) }
    var standards by remember { mutableStateOf<List<IdentityStandard>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("My Identity", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SystemColors.BackgroundColor
                )
            )
        },
        containerColor = SystemColors.BackgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            OnboardingIdentityStep(
                initialStatement = statement,
                initialRoles     = roles,
                initialStandards = standards,
                onNext           = { stmt, r, s ->
                    viewModel.saveIdentityProfile(stmt, r, s)
                    onCompleted()
                },
                onBack = { /* no back from standalone setup */ }
            )
        }
    }
}