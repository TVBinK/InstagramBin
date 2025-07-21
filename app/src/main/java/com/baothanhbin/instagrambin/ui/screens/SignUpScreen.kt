package com.baothanhbin.instagrambin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baothanhbin.instagrambin.viewmodel.AuthViewModel
import com.baothanhbin.instagrambin.viewmodel.AuthState

@Composable
fun SignUpScreen(
    onSignUpClick: () -> Unit = {},
    onFacebookSignUpClick: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val authState by authViewModel.authState.collectAsState()

    if (authState is AuthState.Loading) {
        LoadingScreen()
        return
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                onSignUpClick()
            }
            is AuthState.Error -> {
                errorMessage = (authState as AuthState.Error).message
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Instagram logo
        Text(
            text = "Instagram",
            style = TextStyle(
                fontFamily = FontFamily.Cursive,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Error message
        errorMessage?.let {
            Text(
                text = it,
                color = Color.Red,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Email") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        // Full Name
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            placeholder = { Text("Tên") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        // Username
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            placeholder = { Text("Tài khoản") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Mật khẩu") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
        )

        // Sign up button
        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank() && username.isNotBlank() && fullName.isNotBlank()) {
                    authViewModel.signUp(email, password, username, fullName)
                } else {
                    errorMessage = "Vui lòng điền đầy đủ thông tin"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF52A4E2)),
            enabled = authState !is AuthState.Loading
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Text("Đăng ký", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Login link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 50.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Bạn đã có tài khoản?",
                color = Color.Gray,
                modifier = Modifier.padding(top = 13.dp)
            )
            TextButton(onClick = onLoginClick) {
                Text("Đăng nhập.", color = Color(0xFF3797EF))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    SignUpScreen()
} 