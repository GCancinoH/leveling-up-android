package com.gcancino.levelingup.presentation.auth.signUp.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.gcancino.levelingup.R
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.presentation.auth.signUp.SignUpViewModel

@Composable
fun AccountStep(
    viewModel: AccountStepViewModel,
    signUpViewModel: SignUpViewModel
) {
    val accountState by viewModel.signUpState.collectAsState()

    val buttonOnClick: () -> Unit
    val buttonContent: @Composable RowScope.() -> Unit

    when (accountState) {
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
            buttonOnClick = { viewModel.createAccount() }
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
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily(Font(R.font.leveling_up))
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Providing accurate personal details help us tailor your experience and ensure accurate recommendations.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily(Font(R.font.leveling_up))
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = viewModel.email,
                onValueChange = { viewModel.onEmailChange(it) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = "Email"
                    )
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                isError = viewModel.emailError != null,
                supportingText = {
                    if (viewModel.emailError != null) {
                        Text(
                            text = viewModel.emailError!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Password TextField
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = viewModel.password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Password"
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { viewModel.onPasswordVisibilityChange() }
                    ) {
                        Icon(
                            imageVector = if (viewModel.isPasswordVisible)
                                Icons.Filled.Visibility
                            else
                                Icons.Filled.VisibilityOff,
                            contentDescription = "Password Visibility"
                        )
                    }
                },
                visualTransformation = if (viewModel.isPasswordVisible)
                    VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                isError = viewModel.passwordError != null,
                supportingText = {
                    if (viewModel.passwordError != null) {
                        Text(
                            text = viewModel.passwordError!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (viewModel.isValidEmail() && viewModel.isValidPassword()) {
            Button(
                onClick = buttonOnClick,
                modifier = Modifier.align(Alignment.BottomEnd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                enabled = accountState !is Resource.Loading
            ) {
                buttonContent()
            }
        }
    }
}