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
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import com.baothanhbin.instagrambin.ui.screen.BottomBar
import com.baothanhbin.instagrambin.ui.screen.TopBar
import com.baothanhbin.instagrambin.ui.screens.HomeScreen
import com.baothanhbin.instagrambin.viewmodel.EditProfileViewModel
import com.baothanhbin.instagrambin.viewmodel.EditProfileViewModelFactory
import com.baothanhbin.instagrambin.viewmodel.PostsSectionViewModel
import com.baothanhbin.instagrambin.ui.screens.PostDetailScreen
import com.baothanhbin.instagrambin.ui.screens.MessageScreen
import com.baothanhbin.instagrambin.viewmodel.HomeViewModel
import com.baothanhbin.instagrambin.ui.screens.UserProfileScreen

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
                    val homeViewModel: HomeViewModel = viewModel()

                    // Listen to current route
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    // Routes that should hide navigation bars
                    val hideBarsRoutes = listOf("add", "post_screen/{imageUris}", "login", "signup", "splash")

                    Scaffold(
                        topBar = {
                            if (currentRoute !in hideBarsRoutes) {
                                TopBar(
                                    currentRoute = currentRoute,
                                    onLogout = {
                                        authViewModel.signOut()
                                        navController.navigate("login") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    },
                                    navController = navController
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
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    navController = navController
                                )
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
                                val editProfileViewModel: EditProfileViewModel = viewModel(
                                    factory = EditProfileViewModelFactory(LocalContext.current.applicationContext as Application)
                                )
                                EditProfileScreen(
                                    viewModel = editProfileViewModel,
                                    onCancel = { navController.popBackStack() },
                                    onDone = { navController.popBackStack() }
                                )
                            }

                            composable("profile/{userId}") { backStackEntry ->
                                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                                val userProfileViewModel: com.baothanhbin.instagrambin.viewmodel.UserProfileViewModel = viewModel()
                                LaunchedEffect(userId) {
                                    userProfileViewModel.setUserId(userId)
                                }
                                com.baothanhbin.instagrambin.ui.screens.UserProfileScreen(
                                    viewModel = userProfileViewModel,
                                    navController = navController,
                                    onBackClick = { navController.popBackStack() },
                                    onFollowClick = { userProfileViewModel.toggleFollow() },
                                    onUnfollowClick = { userProfileViewModel.toggleFollow() },
                                    onMessageClick = { /* TODO: handle message click */ },
                                    onFollowersClick = { navController.navigate("followers/$userId") },
                                    onFollowingClick = { navController.navigate("following/$userId") }
                                )
                            }

                            composable("followers/{userId}") { backStackEntry ->
                                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                                val followersViewModel: com.baothanhbin.instagrambin.viewmodel.FollowersViewModel = viewModel()
                                LaunchedEffect(userId) {
                                    followersViewModel.loadFollowers(userId)
                                }
                                val state by followersViewModel.uiState.collectAsState()
                                com.baothanhbin.instagrambin.ui.screens.FollowersScreen(
                                    followers = state.followers,
                                    onBackClick = { navController.popBackStack() },
                                    onUserClick = { followerId ->
                                        navController.navigate("profile/$followerId")
                                    },
                                    onFollowClick = { followerId ->
                                        followersViewModel.followUser(followerId)
                                    },
                                    onUnfollowClick = { followerId ->
                                        followersViewModel.unfollowUser(followerId)
                                    }
                                )
                            }

                            composable("following/{userId}") { backStackEntry ->
                                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                                val followingViewModel: com.baothanhbin.instagrambin.viewmodel.FollowingViewModel = viewModel()
                                LaunchedEffect(userId) {
                                    followingViewModel.loadFollowing(userId)
                                }
                                val state by followingViewModel.uiState.collectAsState()
                                com.baothanhbin.instagrambin.ui.screens.FollowingScreen(
                                    following = state.following,
                                    onBackClick = { navController.popBackStack() },
                                    onUserClick = { followingId ->
                                        navController.navigate("profile/$followingId")
                                    }
                                )
                            }

                            composable("post_detail/{postId}") { backStackEntry ->
                                val postId = backStackEntry.arguments?.getString("postId") ?: ""
                                PostDetailScreen(
                                    navController = navController,
                                    postId = postId
                                )
                            }

                            composable("message") { MessageScreen() }

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
fun MainScreen(
    authViewModel: AuthViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
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
            HomeScreen(viewModel = homeViewModel)
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