package com.baothanhbin.instagrambin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAbsoluteAlignment
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
fun LoginScreen(
    onLoginClick: () -> Unit = {},
    onFacebookLoginClick: () -> Unit = {},
    onSignUpClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        //cách top 100dp

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
            placeholder = { Text("Mật Khẩu") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
        )

        // Forgot password
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            TextButton(onClick = onForgotPasswordClick) {
                Text("Quên mật khẩu?", color = Color(0xFF3797EF))
            }
        }

        // Login button
        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF52A4E2))
        ) {
            Text("Đăng Nhập", color = Color.White, fontWeight = FontWeight.Bold)
        }

        // Or divider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(modifier = Modifier.weight(1f))
            Text("  HOẶC  ", color = Color.Gray)
            Divider(modifier = Modifier.weight(1f))
        }

        // Facebook login
        TextButton(onClick = onFacebookLoginClick) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = R.drawable.ic_facebook), // Thay bằng icon Facebook của bạn
                contentDescription = null,
                tint = Color(0xFF1877F2)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Đăng nhập bằng Facebook", color = Color(0xFF1877F2))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Sign up
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 50.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            // Don't have an account? căn giữa
            Text(
                text = "Chưa có tài khoản?",
                color = Color.Gray,
                modifier = Modifier
                    .padding(top = 14.dp)
            )
            TextButton(onClick = onSignUpClick) {
                Text("Đăng ký.", color = Color(0xFF3797EF))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen()
} 