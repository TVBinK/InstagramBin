package com.baothanhbin.instagrambin


import android.annotation.SuppressLint
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
import com.baothanhbin.instagrambin.ui.screens.ChatScreen
import com.baothanhbin.instagrambin.ui.screens.HomeScreen
import com.baothanhbin.instagrambin.viewmodel.EditProfileViewModel
import com.baothanhbin.instagrambin.viewmodel.EditProfileViewModelFactory
import com.baothanhbin.instagrambin.viewmodel.PostsSectionViewModel
import com.baothanhbin.instagrambin.ui.screens.PostDetailScreen
import com.baothanhbin.instagrambin.viewmodel.HomeViewModel
import com.baothanhbin.instagrambin.ui.screens.UserProfileScreen
import com.baothanhbin.instagrambin.ui.screens.ChatListScreen
import com.baothanhbin.instagrambin.ui.screens.FullImageScreen
import com.baothanhbin.instagrambin.viewmodel.AuthViewModelFactory
import com.baothanhbin.instagrambin.service.FCMService
import com.baothanhbin.instagrambin.viewmodel.HomeViewModelFactory

import android.os.Build
import androidx.annotation.RequiresApi
import com.baothanhbin.instagrambin.viewmodel.VideoCallViewModel
import com.baothanhbin.instagrambin.navigation.AppNavigation
import com.baothanhbin.instagrambin.navigation.Screen


class MainActivity : ComponentActivity() {
    //Test git nhe (next commit)
    // Đổi sang Triple để lưu callId, callerId, callerName
    private val openVideoCallState = mutableStateOf<Triple<String, String, String?>?>(null)
    // Flag để nhớ đang xử lý video call từ notification
    private val isProcessingVideoCallNotification = mutableStateOf(false)

    @SuppressLint("StateFlowValueCalledInComposition")
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val openVideoCall = intent?.getBooleanExtra("openVideoCall", false) == true
        val callerId = intent?.getStringExtra("caller_id")
        val callerName = intent?.getStringExtra("caller_name")
        val callId = intent?.getStringExtra("call_id")
        if (openVideoCall && !callerId.isNullOrBlank() && !callId.isNullOrBlank()) {
            openVideoCallState.value = Triple(callId, callerId, callerName)
            isProcessingVideoCallNotification.value = true
        }

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
                    val context = LocalContext.current
                    val videoCallViewModel: VideoCallViewModel = viewModel { VideoCallViewModel(context) }
                    val authViewModel: AuthViewModel = viewModel(
                        factory = AuthViewModelFactory(LocalContext.current.applicationContext as Application)
                    )
                    val homeViewModel: HomeViewModel = viewModel(
                        factory = HomeViewModelFactory(LocalContext.current.applicationContext as Application)
                    )

                    // Lắng nghe route hiện tại để xác định có hiển thị top/bottom bar không
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val hideBarsRoutes = listOf(
                        Screen.Add.route,
                        Screen.PostScreen.route,
                        Screen.Login.route,
                        Screen.SignUp.route,
                        Screen.Splash.route,
                        Screen.Message.route,
                        "chat/{userId}"
                    )
                    val videoCallRoutePrefix = "video_call_screen"

                    Scaffold(
                        topBar = {
                            if (currentRoute !in hideBarsRoutes && !(currentRoute?.startsWith(videoCallRoutePrefix) == true)) {
                                TopBar(
                                    currentRoute = currentRoute,
                                    onLogout = {
                                        authViewModel.signOut()
                                        homeViewModel.clearCache()
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(Screen.Home.route) { inclusive = true }
                                        }
                                    },
                                    navController = navController
                                )
                            }
                        },
                        bottomBar = {
                            if (currentRoute !in hideBarsRoutes && !(currentRoute?.startsWith(videoCallRoutePrefix) == true)) {
                                BottomBar(
                                    navController = navController,
                                    currentRoute = currentRoute
                                )
                            }
                        }
                    ) { paddings ->
                        AppNavigation(
                            navController = navController,
                            authViewModel = authViewModel,
                            homeViewModel = homeViewModel,
                            videoCallViewModel = videoCallViewModel,
                            openVideoCallState = openVideoCallState,
                            isProcessingVideoCallNotification = isProcessingVideoCallNotification,
                            paddings = paddings
                        )
                    }
                }
            }
        }
    }
}

// Composable hiển thị màn hình tải với vòng tròn tiến trình
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
