package com.baothanhbin.instagrambin

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.baothanhbin.instagrambin.ui.theme.InstagramUiComposeTheme
import com.baothanhbin.instagrambin.ui.screen.BottomBar
import com.baothanhbin.instagrambin.ui.screens.HomeScreen
import com.baothanhbin.instagrambin.ui.screen.TopBar
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.baothanhbin.instagrambin.viewmodel.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.baothanhbin.instagrambin.viewmodel.AuthState
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import com.baothanhbin.instagrambin.ui.screens.AddScreen
import com.baothanhbin.instagrambin.ui.screens.EditProfileScreen
import com.baothanhbin.instagrambin.ui.screens.LoginScreen
import com.baothanhbin.instagrambin.ui.screens.PostScreen
import com.baothanhbin.instagrambin.ui.screens.ProfileScreen
import com.baothanhbin.instagrambin.ui.screens.SearchScreen
import com.baothanhbin.instagrambin.ui.screens.SignUpScreen
import com.baothanhbin.instagrambin.ui.screens.SplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstagramUiComposeTheme {
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = !isSystemInDarkTheme()

                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent,
                        darkIcons = useDarkIcons
                    )
                }

                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Ltr,
                    LocalDensity provides LocalDensity.current
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = viewModel()
                    val authState by authViewModel.authState.collectAsState()

                    // Listen to current route
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    // Routes that should hide navigation bars
                    val hideBarsRoutes = listOf("add", "post_screen/{imageUris}", "login", "signup", "splash")

                    Scaffold(
                        topBar = {
                            if (currentRoute !in hideBarsRoutes && currentRoute != "search") {
                                TopBar(
                                    currentRoute = currentRoute,
                                    onLogout = {
                                        authViewModel.signOut()
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        },
                        bottomBar = {
                            if (currentRoute !in hideBarsRoutes) {
                                BottomBar(
                                    navController = navController,
                                    currentRoute = currentRoute
                                )
                            }
                        }
                    ) { paddings ->
                        NavHost(
                            navController = navController,
                            startDestination = "splash"
                        ) {
                            composable("splash") {
                                SplashScreen(
                                    onSplashFinished = {
                                        when (authState) {
                                            is AuthState.Success -> navController.navigate("home") {
                                                popUpTo("splash") { inclusive = true }
                                            }
                                            else -> navController.navigate("login") {
                                                popUpTo("splash") { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            }

                            composable("login") {
                                if (authState is AuthState.Loading) {
                                    LoadingScreen()
                                } else {
                                    LoginScreen(
                                        onLoginClick = { navController.navigate("home") },
                                        onFacebookLoginClick = { navController.navigate("home") },
                                        onSignUpClick = { navController.navigate("signup") },
                                        onForgotPasswordClick = { navController.navigate("forgot_password") },
                                        authViewModel = authViewModel
                                    )
                                }
                            }

                            composable("home") {
                                HomeScreen()
                            }

                            composable("search") {
                                SearchScreen(onUserClick = { userId ->
                                    navController.navigate("profile/$userId")
                                })
                            }

                            composable("add") {
                                AddScreen(paddingValues = paddings, navController = navController)
                            }

                            composable("post_screen/{imageUris}") { backStackEntry ->
                                val imageUrisString = backStackEntry.arguments?.getString("imageUris")
                                val imageUris = imageUrisString?.split(",")?.mapNotNull {
                                    if (it.isNotBlank()) android.net.Uri.parse(Uri.decode(it)) else null
                                } ?: emptyList()
                                PostScreen(
                                    navController = navController,
                                    imageUris = imageUris
                                )
                            }

                            composable("profile") {
                                ProfileScreen(paddingValues = paddings, navController = navController)
                            }

                            composable("signup") {
                                SignUpScreen(
                                    onSignUpClick = { navController.navigate("home") },
                                    onFacebookSignUpClick = { navController.navigate("home") },
                                    onLoginClick = { navController.popBackStack() },
                                    authViewModel = authViewModel
                                )
                            }

                            composable("edit_profile") {
                                val editProfileViewModel: com.baothanhbin.instagrambin.viewmodel.EditProfileViewModel = viewModel()
                                EditProfileScreen(
                                    viewModel = editProfileViewModel,
                                    onCancel = { navController.popBackStack() },
                                    onDone = { navController.popBackStack() }
                                )
                            }

                            composable("profile/{userId}") { backStackEntry ->
                                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                                com.baothanhbin.instagrambin.ui.screens.UserProfileScreen(
                                    userId = userId,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun MainScreen(authViewModel: AuthViewModel = viewModel()) {
    val authState by authViewModel.authState.collectAsState()

    when (authState) {
        is AuthState.Initial -> {
            LoginScreen(
                onLoginClick = { /* handled by navigation */ },
                onFacebookLoginClick = { /* handled by navigation */ },
                onSignUpClick = { /* handled by navigation */ },
                onForgotPasswordClick = { /* handled by navigation */ },
                authViewModel = authViewModel
            )
        }
        is AuthState.Loading -> {
            LoadingScreen()
        }
        is AuthState.Success -> {
            HomeScreen()
        }
        is AuthState.Error -> {
            LoginScreen(
                onLoginClick = { /* handled by navigation */ },
                onFacebookLoginClick = { /* handled by navigation */ },
                onSignUpClick = { /* handled by navigation */ },
                onForgotPasswordClick = { /* handled by navigation */ },
                authViewModel = authViewModel
            )
        }
    }
}