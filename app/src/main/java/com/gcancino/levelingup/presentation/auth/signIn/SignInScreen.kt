package com.gcancino.levelingup.presentation.auth.signIn

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gcancino.levelingup.R
import com.gcancino.levelingup.core.Resource

@Composable
fun SignInScreen(
    viewModel: SignInViewModel = hiltViewModel(),
    onSignedIn: () -> Unit,
    onSignInError: () -> Unit,
    onGoToSignUp: () -> Unit,
) {
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is Resource.Success -> {
                onSignedIn()
            }
            is Resource.Error -> {
                onSignInError()
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Image(
            painter = painterResource(id = R.drawable.levelingup),
            contentDescription = "Logo",
            modifier = Modifier.width(200.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Google SignIn Button
        OutlinedButton(
            onClick = { viewModel.signInWithGoogle() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
                disabledContainerColor = Color.Unspecified,
                disabledContentColor = Color.Unspecified
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.google_icon),
                contentDescription = "Google Logo",
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Sign In with Google")
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Divider OR
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f)
                    .padding(end = 8.dp)
            )
            Text(text = "OR")
            HorizontalDivider(
                modifier = Modifier.weight(1f)
                    .padding(start = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Email TextField
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
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

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
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = {
                TODO()
            }) {
                Text(text = "Forgot Password?")
            }
        }

        // SignIn Button
        Button(
            onClick = { viewModel.signIn() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonColors(
                containerColor = Color(0xFF850000),
                contentColor = Color.White,
                disabledContainerColor = Color.Unspecified,
                disabledContentColor = Color.Unspecified
            )
        ) {
            when(authState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                else -> Text("Sign In")
            }
        }

        // Account Creation
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(text = "Don't have an account?")
            TextButton(onClick = {
                onGoToSignUp()
            }) {
                Text(text = "Sign Up")
            }
        }
    }
}