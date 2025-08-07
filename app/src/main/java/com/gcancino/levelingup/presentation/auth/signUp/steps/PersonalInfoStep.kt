package com.gcancino.levelingup.presentation.auth.signUp.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.player.Genders
import com.gcancino.levelingup.ui.components.OutlinedDateField
import com.gcancino.levelingup.presentation.auth.signUp.SignUpViewModel

@Composable
fun PersonalInfoStep(
    signUpViewModel: SignUpViewModel,
    viewModel: PersonalInfoViewModel
) {
    val genders = Genders.entries
    val selectedGender by viewModel.selectedGender.collectAsState()
    val state by viewModel.saveState.collectAsState()

    val buttonOnClick: () -> Unit
    val buttonContent: @Composable RowScope.() -> Unit

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
                    color = Color.White, // Example if button container is primary
                    modifier = Modifier.size(2.dp)
                )
            }
        }
        else -> {
            buttonOnClick = { viewModel.savePersonalInfo() }
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
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Personal Information",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Providing accurate personal details help us tailor your experience and ensure accurate recommendations.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = { viewModel.onNameChange(it) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Name"
                    )
                },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                //isError = viewModel.nameError != null,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedDateField(
                value = viewModel.birthdate,
                onValueChange = { viewModel.onBirthdateChange(it) },
                error = null,
                label = "Your birthday"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                genders.forEach { gender ->
                    Button(
                        onClick = { viewModel.selectGender(gender.name) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedGender == gender.name) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (selectedGender == gender.name) {
                                Color.White
                            } else { Color.White }
                        )

                    ) {
                        Text(text = when(gender.name) {
                            "FEMALE" -> "Female"
                            "MALE" -> "Male"
                            else -> null
                        }.toString())
                    }
                }
            }
        }

        if (viewModel.isNameValid() && viewModel.isBirthdateValid() && viewModel.isGenderValid()) {
            Button(
                onClick = buttonOnClick,
                modifier = Modifier.align(Alignment.BottomEnd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                enabled = state !is Resource.Loading
            ) {
                buttonContent()
            }
        }
    }
}
