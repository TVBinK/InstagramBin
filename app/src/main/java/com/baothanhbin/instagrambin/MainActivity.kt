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

// Lớp chính của ứng dụng, kế thừa ComponentActivity để sử dụng Jetpack Compose
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Áp dụng theme tùy chỉnh của ứng dụng
            InstagramUiComposeTheme {
                // Quản lý màu sắc thanh hệ thống (status bar, navigation bar)
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = !isSystemInDarkTheme() // Sử dụng biểu tượng sáng nếu không ở chế độ tối

                // Thiết lập màu thanh hệ thống trong SideEffect để đảm bảo cập nhật khi theme thay đổi
                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent, // Thanh hệ thống trong suốt
                        darkIcons = useDarkIcons // Biểu tượng sáng/tối tùy theo theme
                    )
                }

                // Đặt hướng bố cục (trái sang phải) và mật độ hiển thị
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Ltr,
                    LocalDensity provides LocalDensity.current
                ) {
                    // Tạo NavController để quản lý điều hướng
                    val navController = rememberNavController()
                    // Khởi tạo AuthViewModel để quản lý trạng thái xác thực
                    val authViewModel: AuthViewModel = viewModel()
                    // Thu thập trạng thái xác thực
                    val authState by authViewModel.authState.collectAsState()
                    // Khởi tạo HomeViewModel để quản lý dữ liệu màn hình chính
                    val homeViewModel: HomeViewModel = viewModel()

                    // Lắng nghe tuyến đường hiện tại để xác định màn hình đang hiển thị
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    // Danh sách các tuyến đường không hiển thị thanh trên và thanh dưới
                    val hideBarsRoutes = listOf(
                        "add",
                        "post_screen/{imageUris}",
                        "login",
                        "signup",
                        "splash",
                        "message",
                        "chat/{userId}"
                    )

                    // Sử dụng Scaffold để tạo bố cục với thanh trên và thanh dưới
                    Scaffold(
                        topBar = {
                            // Chỉ hiển thị TopBar nếu không phải tuyến đường trong hideBarsRoutes
                            if (currentRoute !in hideBarsRoutes) {
                                TopBar(
                                    currentRoute = currentRoute,
                                    onLogout = {
                                        // Đăng xuất và điều hướng về màn hình đăng nhập
                                        authViewModel.signOut()
                                        navController.navigate("login") {
                                            popUpTo("home") { inclusive = true } // Xóa màn hình home khỏi stack
                                        }
                                    },
                                    navController = navController
                                )
                            }
                        },
                        bottomBar = {
                            // Chỉ hiển thị BottomBar nếu không phải tuyến đường trong hideBarsRoutes
                            if (currentRoute !in hideBarsRoutes) {
                                BottomBar(
                                    navController = navController,
                                    currentRoute = currentRoute
                                )
                            }
                        }
                    ) { paddings ->
                        // NavHost quản lý các màn hình và điều hướng
                        NavHost(
                            navController = navController,
                            startDestination = "splash" // Màn hình khởi đầu là splash
                        ) {
                            // Màn hình splash
                            composable("splash") {
                                SplashScreen(
                                    onSplashFinished = {
                                        // Khi splash hoàn tất, điều hướng dựa trên trạng thái xác thực
                                        when (authState) {
                                            is AuthState.Success -> navController.navigate("home") {
                                                popUpTo("splash") { inclusive = true } // Xóa màn hình splash
                                            }
                                            else -> navController.navigate("login") {
                                                popUpTo("splash") { inclusive = true } // Xóa màn hình splash
                                            }
                                        }
                                    }
                                )
                            }

                            // Màn hình đăng nhập
                            composable("login") {
                                if (authState is AuthState.Loading) {
                                    LoadingScreen() // Hiển thị màn hình tải khi đang xác thực
                                } else {
                                    LoginScreen(
                                        onLoginClick = { navController.navigate("home") }, // Điều hướng về home khi đăng nhập thành công
                                        onFacebookLoginClick = { navController.navigate("home") }, // Đăng nhập bằng Facebook
                                        onSignUpClick = { navController.navigate("signup") }, // Chuyển sang màn hình đăng ký
                                        onForgotPasswordClick = { navController.navigate("forgot_password") }, // Chuyển sang màn hình quên mật khẩu
                                        authViewModel = authViewModel
                                    )
                                }
                            }

                            // Màn hình chính
                            composable("home") {
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    navController = navController
                                )
                            }

                            // Màn hình tìm kiếm
                            composable("search") {
                                SearchScreen(onUserClick = { userId ->
                                    navController.navigate("profile/$userId") // Điều hướng đến hồ sơ người dùng
                                })
                            }

                            // Màn hình thêm bài đăng
                            composable("add") {
                                AddScreen(paddingValues = paddings, navController = navController)
                            }

                            // Màn hình đăng bài với danh sách URI hình ảnh
                            composable("post_screen/{imageUris}") { backStackEntry ->
                                val imageUrisString = backStackEntry.arguments?.getString("imageUris")
                                // Chuyển đổi chuỗi URI thành danh sách Uri
                                val imageUris = imageUrisString?.split(",")?.mapNotNull {
                                    if (it.isNotBlank()) android.net.Uri.parse(Uri.decode(it)) else null
                                } ?: emptyList()
                                PostScreen(
                                    navController = navController,
                                    imageUris = imageUris
                                )
                            }

                            // Màn hình hồ sơ người dùng hiện tại
                            composable("profile") {
                                ProfileScreen(paddingValues = paddings, navController = navController)
                            }

                            // Màn hình đăng ký
                            composable("signup") {
                                SignUpScreen(
                                    onSignUpClick = { navController.navigate("home") }, // Điều hướng về home khi đăng ký thành công
                                    onFacebookSignUpClick = { navController.navigate("home") }, // Đăng ký bằng Facebook
                                    onLoginClick = { navController.popBackStack() }, // Quay lại màn hình đăng nhập
                                    authViewModel = authViewModel
                                )
                            }

                            // Màn hình chỉnh sửa hồ sơ
                            composable("edit_profile") {
                                val editProfileViewModel: EditProfileViewModel = viewModel(
                                    factory = EditProfileViewModelFactory(LocalContext.current.applicationContext as Application)
                                )
                                EditProfileScreen(
                                    viewModel = editProfileViewModel,
                                    onCancel = { navController.popBackStack() }, // Hủy và quay lại
                                    onDone = { navController.popBackStack() } // Hoàn tất và quay lại
                                )
                            }

                            // Màn hình hồ sơ của người dùng khác
                            composable("profile/{userId}") { backStackEntry ->
                                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                                val userProfileViewModel: com.baothanhbin.instagrambin.viewmodel.UserProfileViewModel = viewModel()
                                // Tải dữ liệu hồ sơ dựa trên userId
                                LaunchedEffect(userId) {
                                    userProfileViewModel.setUserId(userId)
                                }
                                com.baothanhbin.instagrambin.ui.screens.UserProfileScreen(
                                    viewModel = userProfileViewModel,
                                    navController = navController,
                                    onBackClick = { navController.popBackStack() },
                                    onFollowClick = { userProfileViewModel.toggleFollow() }, // Theo dõi
                                    onUnfollowClick = { userProfileViewModel.toggleFollow() }, // Bỏ theo dõi
                                    onMessageClick = {
                                        navController.navigate("chat/$userId") // Mở màn hình chat
                                    },
                                    onFollowersClick = { navController.navigate("followers/$userId") }, // Xem danh sách người theo dõi
                                    onFollowingClick = { navController.navigate("following/$userId") } // Xem danh sách đang theo dõi
                                )
                            }

                            // Màn hình danh sách người theo dõi
                            composable("followers/{userId}") { backStackEntry ->
                                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                                val followersViewModel: com.baothanhbin.instagrambin.viewmodel.FollowersViewModel = viewModel()
                                // Tải danh sách người theo dõi
                                LaunchedEffect(userId) {
                                    followersViewModel.loadFollowers(userId)
                                }
                                val state by followersViewModel.uiState.collectAsState()
                                com.baothanhbin.instagrambin.ui.screens.FollowersScreen(
                                    followers = state.followers,
                                    onBackClick = { navController.popBackStack() },
                                    onUserClick = { followerId ->
                                        navController.navigate("profile/$followerId") // Xem hồ sơ người theo dõi
                                    },
                                    onFollowClick = { followerId ->
                                        followersViewModel.followUser(followerId) // Theo dõi
                                    },
                                    onUnfollowClick = { followerId ->
                                        followersViewModel.unfollowUser(followerId) // Bỏ theo dõi
                                    }
                                )
                            }

                            // Màn hình danh sách người đang theo dõi
                            composable("following/{userId}") { backStackEntry ->
                                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                                val followingViewModel: com.baothanhbin.instagrambin.viewmodel.FollowingViewModel = viewModel()
                                // Tải danh sách người đang theo dõi
                                LaunchedEffect(userId) {
                                    followingViewModel.loadFollowing(userId)
                                }
                                val state by followingViewModel.uiState.collectAsState()
                                com.baothanhbin.instagrambin.ui.screens.FollowingScreen(
                                    following = state.following,
                                    onBackClick = { navController.popBackStack() },
                                    onUserClick = { followingId ->
                                        navController.navigate("profile/$followingId") // Xem hồ sơ người đang theo dõi
                                    }
                                )
                            }

                            // Màn hình chi tiết bài đăng
                            composable("post_detail/{postId}") { backStackEntry ->
                                val postId = backStackEntry.arguments?.getString("postId") ?: ""
                                PostDetailScreen(
                                    navController = navController,
                                    postId = postId
                                )
                            }

                            // Màn hình danh sách tin nhắn
                            composable("message") {
                                ChatListScreen(


                                    onBackClick = { navController.popBackStack() }, // Quay lại
                                    onChatClick = { user ->
                                        navController.navigate("chat/${user.uid}") // Mở màn hình chat với người dùng
                                    },
                                    onNewMessageClick = {
                                        navController.navigate("search") {
                                            // Điều hướng đến màn hình tìm kiếm để chọn người nhắn tin
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true // Tránh tạo nhiều instance
                                            restoreState = true // Khôi phục trạng thái
                                        }
                                    }
                                )
                            }

                            // Màn hình chat với một người dùng cụ thể
                            composable("chat/{userId}") { backStackEntry ->
                                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                                val userProfileViewModel: com.baothanhbin.instagrambin.viewmodel.UserProfileViewModel = viewModel()
                                val uiState by userProfileViewModel.uiState.collectAsState()

                                // Tải dữ liệu người dùng dựa trên userId
                                LaunchedEffect(userId) {
                                    userProfileViewModel.setUserId(userId)
                                }

                                // Nếu dữ liệu người dùng đã tải, hiển thị màn hình chat
                                uiState.user?.let { userData ->
                                    ChatScreen(
                                        user = userData,
                                        onBackClick = { navController.popBackStack() },
                                        navController = navController
                                    )
                                } ?: run {
                                    // Nếu chưa tải xong, hiển thị vòng tròn tải
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }

                            // Màn hình xem hình ảnh toàn màn hình
                            composable("image_view/{imageUrl}") { backStackEntry ->
                                val imageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
                                val previousEntry = navController.previousBackStackEntry
                                val previousRoute = previousEntry?.destination?.route
                                val userId = previousEntry?.arguments?.getString("userId")
                                FullImageScreen(
                                    imageUrl = Uri.decode(imageUrl),
                                    onBack = {
                                        // Điều hướng quay lại dựa trên tuyến đường trước đó
                                        if (previousRoute?.startsWith("chat/") == true) {
                                            navController.popBackStack(previousRoute, false)
                                        } else {
                                            navController.popBackStack()
                                        }
                                    }
                                )
                            }
                        }
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
        modifier = Modifier.fillMaxSize(), // Chiếm toàn bộ màn hình
        contentAlignment = Alignment.Center // Căn giữa nội dung
    ) {
        CircularProgressIndicator() // Hiển thị vòng tròn tải
    }
}

// Composable chính để quản lý trạng thái xác thực và hiển thị màn hình phù hợp
@Composable
fun MainScreen(
    authViewModel: AuthViewModel = viewModel(), // ViewModel quản lý xác thực
    homeViewModel: HomeViewModel = viewModel() // ViewModel cho màn hình chính
) {
    val authState by authViewModel.authState.collectAsState() // Thu thập trạng thái xác thực

    // Xử lý hiển thị dựa trên trạng thái xác thực
    when (authState) {
        is AuthState.Initial -> {
            // Trạng thái khởi tạo: hiển thị màn hình đăng nhập
            LoginScreen(
                onLoginClick = { /* handled by navigation */ },
                onFacebookLoginClick = { /* handled by navigation */ },
                onSignUpClick = { /* handled by navigation */ },
                onForgotPasswordClick = { /* handled by navigation */ },
                authViewModel = authViewModel
            )
        }
        is AuthState.Loading -> {
            // Trạng thái đang tải: hiển thị màn hình tải
            LoadingScreen()
        }
        is AuthState.Success -> {
            // Trạng thái xác thực thành công: hiển thị màn hình chính
            HomeScreen(viewModel = homeViewModel)
        }
        is AuthState.Error -> {
            // Trạng thái lỗi: hiển thị màn hình đăng nhập
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