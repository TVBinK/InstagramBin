package com.baothanhbin.instagrambin.navigation

import android.annotation.SuppressLint
import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.baothanhbin.instagrambin.ui.screens.*
import com.baothanhbin.instagrambin.viewmodel.*

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home")
    object Search : Screen("search")
    object Add : Screen("add")
    object PostScreen : Screen("post_screen/{imageUris}") {
        fun createRoute(imageUris: String) = "post_screen/$imageUris"
    }
    object Profile : Screen("profile")
    object SignUp : Screen("signup")
    object EditProfile : Screen("edit_profile")
    object UserProfile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    object Followers : Screen("followers/{userId}") {
        fun createRoute(userId: String) = "followers/$userId"
    }
    object Following : Screen("following/{userId}") {
        fun createRoute(userId: String) = "following/$userId"
    }
    object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: String) = "post_detail/$postId"
    }
    object Message : Screen("message")
    object Chat : Screen("chat/{userId}") {
        fun createRoute(userId: String) = "chat/$userId"
    }
    object ImageView : Screen("image_view/{imageUrl}") {
        fun createRoute(imageUrl: String) = "image_view/$imageUrl"
    }
    object VideoCall : Screen("video_call_screen/{fromUserId}") {
        fun createRoute(fromUserId: String) = "video_call_screen/$fromUserId"
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    videoCallViewModel: VideoCallViewModel,
    openVideoCallState: MutableState<Triple<String, String, String?>?>,
    isProcessingVideoCallNotification: MutableState<Boolean>,
    paddings: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    // Điều hướng dựa vào trạng thái đăng nhập
                    val isLoggedIn = authViewModel.authState.value is AuthState.Success
                    if (isLoggedIn) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginClick = { navController.navigate(Screen.Home.route) },
                onFacebookLoginClick = { navController.navigate(Screen.Home.route) },
                onSignUpClick = { navController.navigate(Screen.SignUp.route) },
                onForgotPasswordClick = { navController.navigate("forgot_password") },
                authViewModel = authViewModel
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                navController = navController
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(onUserClick = { userId ->
                navController.navigate(Screen.UserProfile.createRoute(userId))
            })
        }
        composable(Screen.Add.route) {
            AddScreen(paddingValues = paddings, navController = navController)
        }
        composable(Screen.PostScreen.route) { backStackEntry ->
            val imageUrisString = backStackEntry.arguments?.getString("imageUris")
            val imageUris = imageUrisString?.split(",")?.mapNotNull { uriString ->
                if (uriString.isNotBlank()) Uri.parse(Uri.decode(uriString)) else null
            } ?: emptyList()
            PostScreen(
                navController = navController,
                imageUris = imageUris
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(paddingValues = paddings, navController = navController)
        }
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onSignUpClick = { navController.navigate(Screen.Home.route) },
                onFacebookSignUpClick = { navController.navigate(Screen.Home.route) },
                onLoginClick = { navController.popBackStack() },
                authViewModel = authViewModel
            )
        }
        composable(Screen.EditProfile.route) {
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
            val userProfileViewModel: UserProfileViewModel = viewModel()
            LaunchedEffect(userId) { userProfileViewModel.setUserId(userId) }
            UserProfileScreen(
                viewModel = userProfileViewModel,
                navController = navController,
                onBackClick = { navController.popBackStack() },
                onFollowClick = { userProfileViewModel.toggleFollow() },
                onUnfollowClick = { userProfileViewModel.toggleFollow() },
                onMessageClick = { navController.navigate(Screen.Chat.createRoute(userId)) },
                onFollowersClick = { navController.navigate(Screen.Followers.createRoute(userId)) },
                onFollowingClick = { navController.navigate(Screen.Following.createRoute(userId)) }
            )
        }
        composable("followers/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val followersViewModel: FollowersViewModel = viewModel()
            LaunchedEffect(userId) { followersViewModel.loadFollowers(userId) }
            val state by followersViewModel.uiState.collectAsState()
            FollowersScreen(
                followers = state.followers,
                onBackClick = { navController.popBackStack() },
                onUserClick = { followerId -> navController.navigate(Screen.UserProfile.createRoute(followerId)) },
                onFollowClick = { followerId -> followersViewModel.followUser(followerId) },
                onUnfollowClick = { followerId -> followersViewModel.unfollowUser(followerId) }
            )
        }
        composable("following/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val followingViewModel: FollowingViewModel = viewModel()
            LaunchedEffect(userId) { followingViewModel.loadFollowing(userId) }
            val state by followingViewModel.uiState.collectAsState()
            FollowingScreen(
                following = state.following,
                onBackClick = { navController.popBackStack() },
                onUserClick = { followingId -> navController.navigate(Screen.UserProfile.createRoute(followingId)) }
            )
        }
        composable("post_detail/{postId}") { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            PostDetailScreen(
                navController = navController,
                postId = postId
            )
        }
        composable(Screen.Message.route) {
            ChatListScreen(
                onBackClick = { navController.popBackStack() },
                onChatClick = { user -> navController.navigate(Screen.Chat.createRoute(user.uid)) },
                onNewMessageClick = {
                    navController.navigate(Screen.Search.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable("chat/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val userProfileViewModel: UserProfileViewModel = viewModel()
            val uiState by userProfileViewModel.uiState.collectAsState()
            LaunchedEffect(userId) { userProfileViewModel.setUserId(userId) }
            uiState.user?.let { userData ->
                ChatScreen(
                    user = userData,
                    onBackClick = { navController.popBackStack() },
                    navController = navController,
                    videoCallViewModel = videoCallViewModel
                )
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
        }
        composable(Screen.ImageView.route) { backStackEntry ->
            val imageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
            val previousEntry = navController.previousBackStackEntry
            val previousRoute = previousEntry?.destination?.route
            FullImageScreen(
                imageUrl = Uri.decode(imageUrl),
                onBack = {
                    if (previousRoute?.startsWith("chat/") == true) {
                        previousRoute?.let { route -> navController.popBackStack(route, false) } ?: navController.popBackStack()
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }
        composable(Screen.VideoCall.route) { backStackEntry ->
            val fromUserId = backStackEntry.arguments?.getString("fromUserId") ?: ""
            val incomingCall = videoCallViewModel.incomingCall.value
            val callUser = incomingCall?.fromUser
            if (callUser != null) {
                VideoCallScreen(
                    user = callUser,
                    webRTCService = videoCallViewModel.getWebRTCService(),
                    onEndCall = {
                        videoCallViewModel.endCall()
                        navController.popBackStack()
                    },
                    videoCallViewModel = videoCallViewModel
                )
            }
        }
    }
} 