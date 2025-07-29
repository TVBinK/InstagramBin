package com.baothanhbin.instagrambin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.baothanhbin.instagrambin.R
import com.baothanhbin.instagrambin.model.Post
import com.baothanhbin.instagrambin.viewmodel.HomeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import androidx.navigation.NavController
import com.baothanhbin.instagrambin.ui.screen.TopBar
import com.baothanhbin.instagrambin.viewmodel.HomeUiState
import com.google.firebase.auth.FirebaseAuth
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.baothanhbin.instagrambin.navigation.Screen
import androidx.compose.foundation.lazy.rememberLazyListState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel, // ViewModel cung cấp dữ liệu
    navController: NavController? = null, // NavController để điều hướng
) {
    // Thu thập trạng thái giao diện và trạng thái làm mới từ ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing) // Trạng thái cho SwipeRefresh

    // Tự động tải dữ liệu khi màn hình được tạo lần đầu
    LaunchedEffect(Unit) {
        viewModel.loadDataIfNeeded()
    }

    // Đồng bộ trạng thái làm mới với SwipeRefresh
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            swipeRefreshState.isRefreshing = false
        }
    }

    // Sử dụng Scaffold để tạo bố cục với thanh trên cùng
    Scaffold(
        topBar = { TopBar(currentRoute = "home") } // Thanh trên cùng của màn hình
    ) { innerPadding ->
        // Hỗ trợ làm mới giao diện bằng cách kéo xuống
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.refreshPosts() }, // Gọi hàm làm mới bài đăng
            indicator = { state, trigger ->
                SwipeRefreshIndicator(
                    state = state,
                    refreshTriggerDistance = trigger,
                    backgroundColor = Color.White,
                    contentColor = Color(0xffff6f00),
                    modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
                )
            }
        ) {
            // Nếu đang tải dữ liệu, hiển thị giao diện skeleton
            if (uiState.isLoading) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = 72.dp
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFFFFF))
                ) {
                    items(3) { PostSkeleton() } // Hiển thị 3 skeleton placeholder
                }
            } else {
                // Hiển thị danh sách bài đăng và stories
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = 72.dp
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFFFFF)),
                    // Tối ưu performance
                    state = rememberLazyListState()
                ) {
                    // Hiển thị stories (tin của bạn và bạn bè)
                    item {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            // Tối ưu performance cho stories
                            state = rememberLazyListState()
                        ) {
                            // Hiển thị story của người dùng hiện tại
                            uiState.currentUser?.let { user ->
                                item {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .border(
                                                    1.dp, Brush.horizontalGradient(
                                                        listOf(
                                                            Color(0xffff6f00),
                                                            Color(0xffffeb35),
                                                            Color(0xffff6f00),
                                                            Color(0xffff2b99),
                                                            Color(0xffff2bd1),
                                                            Color(0xffff2bd1),
                                                        )
                                                    ), CircleShape
                                                )
                                                .size(82.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(user.profileImageUrl)
                                                    .crossfade(200)
                                                    .size(160) // Tối ưu size cho story
                                                    .build(),
                                                contentDescription = "Your story",
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop,
                                                placeholder = painterResource(id = R.drawable.profile_pic),
                                                error = painterResource(id = R.drawable.profile_pic)
                                            )
                                        }
                                        Text("Tin của bạn", fontSize = 12.sp, maxLines = 1)
                                    }
                                }
                            }
                            // Hiển thị stories của bạn bè
                            items(
                                items = uiState.friends,
                                key = { friend -> friend.uid } // Tối ưu re-compose
                            ) { friend ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .border(
                                                1.dp, Brush.horizontalGradient(
                                                    listOf(
                                                        Color(0xffff6f00),
                                                        Color(0xffffeb35),
                                                        Color(0xffff6f00),
                                                        Color(0xffff2b99),
                                                        Color(0xffff2bd1),
                                                        Color(0xffff2bd1),
                                                    )
                                                ), CircleShape
                                            )
                                            .size(82.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(friend.profileImageUrl)
                                                .crossfade(200)
                                                .size(160) // Tối ưu size cho story
                                                .build(),
                                            contentDescription = friend.username,
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop,
                                            placeholder = painterResource(id = R.drawable.profile_pic),
                                            error = painterResource(id = R.drawable.profile_pic)
                                        )
                                    }
                                    Text(friend.username, fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                    // Hiển thị danh sách bài đăng
                    items(
                        items = uiState.posts,
                        key = { post -> post.postId } // Tối ưu re-compose
                    ) { post ->
                        val onLikeClick = remember(post.postId) {
                            { _: Post -> viewModel.toggleLike(post) }
                        }
                        PostItem(
                            post = post,
                            navController = navController,
                            onLikeClick = onLikeClick
                        )
                    }
                }
            }
        }
    }
}

// Composable hiển thị chi tiết một bài đăng
@Composable
fun PostItem(
    post: Post, // Dữ liệu bài đăng
    navController: NavController?, // NavController để điều hướng
    onLikeClick: (Post) -> Unit, // Callback khi nhấn nút thích
) {
    // Kiểm tra xem người dùng hiện tại có thích bài đăng này không
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid }
    val liked by remember(post.likes, currentUserId) {
        derivedStateOf {
            currentUserId != null && post.likes.containsKey(currentUserId)
        }
    }
    // Trạng thái để hiển thị hiệu ứng trái tim khi nhấn đúp
    var showHeart by remember { mutableStateOf(false) }

    // Bố cục chính của bài đăng
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .background(Color.White)
    ) {
        // Đường phân cách giữa các bài đăng
        Divider(
            color = Color.Gray.copy(alpha = 0.5f),
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        // Phần tiêu đề bài đăng (avatar, tên người dùng, thời gian)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .border(
                        1.dp, Brush.horizontalGradient(
                            listOf(
                                Color(0xffff6f00),
                                Color(0xffffeb35),
                                Color(0xffff6f00),
                                Color(0xffff2b99),
                                Color(0xffff2bd1),
                                Color(0xffff2bd1),
                            )
                        ), CircleShape
                    )
                    .size(33.dp),
                contentAlignment = Alignment.Center
            ) {
                // Hiển thị ảnh đại diện của người dùng
                AsyncImage(
                    model = ImageRequest
                        .Builder(LocalContext.current)
                        .data(post.user.profileImageUrl)
                        .crossfade(300) // Giảm từ 400 xuống 300ms
                        .size(60) // Tối ưu size cho avatar
                        .build(),
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(30.dp),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    placeholder = painterResource(id = R.drawable.profile_pic),
                    error = painterResource(id = R.drawable.profile_pic)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Tên người dùng, có thể nhấn để điều hướng đến hồ sơ
            val onUsernameClick = remember(post.user.uid) {
                { navController?.navigate("profile/${post.user.uid}") }
            }
            Text(
                text = post.user.fullName,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple() // Hiệu ứng ripple khi nhấn
                ) {
                    onUsernameClick()
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            // Thời gian đăng bài
            Text(
                text = post.timeAgo,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        // Phần ảnh bài đăng
        val onImageClick = remember(post.postId) {
            { navController?.navigate(Screen.PostDetail.createRoute(post.postId)) }
        }
        val onImageDoubleClick = remember {
            { showHeart = true }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            // Nhấn một lần để xem chi tiết bài đăng
                            onImageClick()
                        },
                        onDoubleTap = {
                            // Nhấn đúp để hiển thị hiệu ứng trái tim
                            onImageDoubleClick()
                        }
                    )
                }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(post.imageUrl)
                    .crossfade(200) // Giảm crossfade cho ảnh chính
                    .size(800) // Tối ưu size cho ảnh bài đăng
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            // Hiệu ứng trái tim khi nhấn đúp
            if (showHeart) {
                Icon(
                    painter = painterResource(id = R.drawable.heart),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.Center),
                    tint = Color.Red.copy(alpha = 0.5f)
                )
            }

            // Tự động ẩn hiệu ứng trái tim sau 1 giây
            LaunchedEffect(showHeart) {
                if (showHeart) {
                    delay(1000)
                    showHeart = false
                }
            }
        }

        // Các nút tương tác (thích, bình luận, chia sẻ)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Nút thích
                Icon(
                    painter = painterResource(id = if (liked) R.drawable.heart else R.drawable.heart_outline),
                    contentDescription = "Like",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onLikeClick(post) },
                    tint = if (liked) Color.Red else Color.Black
                )
                // Nút bình luận
                Icon(
                    painter = painterResource(id = R.drawable.ic_comment),
                    contentDescription = "Comment",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { navController?.navigate(Screen.PostDetail.createRoute(post.postId)) },
                    tint = Color.Black
                )
                // Nút chia sẻ (chưa triển khai logic)
                Icon(
                    painter = painterResource(id = R.drawable.ic_send),
                    contentDescription = "Share",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { /* onShareClick() */ },
                    tint = Color.Black
                )
            }
        }

        // Số lượt thích và bình luận
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "${post.likesCount} lượt thích",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            if (post.commentsCount > 0) {
                // Liên kết để xem tất cả bình luận
                Text(
                    text = "Xem tất cả ${post.commentsCount} bình luận",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier
                        .clickable { navController?.navigate(Screen.PostDetail.createRoute(post.postId)) }
                        .padding(vertical = 4.dp)
                )
            }
        }

        // Chú thích bài đăng
        Column(
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = "${post.user.fullName} ${post.caption}",
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

// Composable hiển thị skeleton placeholder khi dữ liệu đang tải
@Composable
fun PostSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .background(Color.White)
    ) {
        // Đường phân cách
        Divider(
            color = Color.Gray.copy(alpha = 0.5f),
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        // Phần tiêu đề placeholder
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder cho avatar
            Box(
                modifier = Modifier
                    .size(33.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3E2E2))
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Placeholder cho tên người dùng
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(16.dp)
                    .background(Color(0xFFE3E2E2), shape = MaterialTheme.shapes.small)
            )
        }
        // Placeholder cho ảnh bài đăng
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFFE3E2E2))
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Placeholder cho số lượt thích
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(16.dp)
                .background(Color(0xFFE3E2E2), shape = MaterialTheme.shapes.small)
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Placeholder cho chú thích
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(16.dp)
                .background(Color(0xFFE3E2E2), shape = MaterialTheme.shapes.small)
        )
    }
}