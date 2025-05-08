package com.baothanhbin.instagrambin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baothanhbin.instagrambin.R
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SignUpScreen(
    onSignUpClick: () -> Unit = {},
    onFacebookSignUpClick: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

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
            trailingIcon = {
                // val icon = if (passwordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                // IconButton(onClick = { passwordVisible = !passwordVisible }) {
                //   Icon(painter = painterResource(id = icon), contentDescription = null)
                //}
            },
            modifier = Modifier
                .fillMaxWidth()
        )

        // Sign up button
        Button(
            onClick = onSignUpClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF52A4E2))
        ) {
            Text("Đăng ký", color = Color.White, fontWeight = FontWeight.Bold)
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
                text = "Have an account?",
                color = Color.Gray,
                modifier = Modifier.padding(top = 15.dp)
            )
            TextButton(onClick = onLoginClick) {
                Text("Log in.", color = Color(0xFF3797EF))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    SignUpScreen()
} 