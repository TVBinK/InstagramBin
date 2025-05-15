package com.baothanhbin.instagrambin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    navController: NavController? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    // Load data only once when the screen is first created
    LaunchedEffect(Unit) {
        viewModel.loadDataIfNeeded()
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            swipeRefreshState.isRefreshing = false
        }
    }

    Scaffold(
        topBar = { TopBar(currentRoute = "home") }
    ) { innerPadding ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.refreshPosts() },
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
            if (uiState.isLoading) {
                // Skeleton cho posts
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = 72.dp
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFFFFF))
                ) {
                    items(3) { PostSkeleton() }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = 72.dp
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFFFFF))
                ) {
                    // Stories là item đầu tiên
                    item {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
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
                                                model = user.profileImageUrl,
                                                contentDescription = "Your story",
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        Text("Tin của bạn", fontSize = 12.sp, maxLines = 1)
                                    }
                                }
                            }
                            items(uiState.friends) { friend ->
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
                                            model = friend.profileImageUrl,
                                            contentDescription = friend.username,
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Text(friend.username, fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                    // Các post
                    items(uiState.posts) { post ->
                        PostItem(
                            post = post,
                            navController = navController,
                            onLikeClick = { viewModel.toggleLike(post) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostItem(
    post: Post,
    navController: NavController?,
    onLikeClick: (Post) -> Unit,
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val liked = currentUserId != null && post.likes.containsKey(currentUserId)
    var showHeart by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .background(Color.White)
    ) {
        // Đường thẳng ngang ngăn cách giữa các post
        Divider(
            color = Color.Gray.copy(alpha = 0.5f),
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        //Post
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
                AsyncImage(
                    model = ImageRequest
                        .Builder(LocalContext.current)
                        .data(post.user.profileImageUrl)
                        .crossfade(400)
                        .build(),
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(30.dp),
                    contentScale = ContentScale.Crop,
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = post.user.username,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { 
                    navController?.navigate("profile/${post.user.uid}")
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = post.timeAgo,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        // Post image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            navController?.navigate("post_detail/${post.postId}")
                        },
                        onDoubleTap = {
                            showHeart = true
                        }
                    )
                }
        ) {
            AsyncImage(
                model = post.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Heart animation
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

            LaunchedEffect(showHeart) {
                if (showHeart) {
                    delay(1000)
                    showHeart = false
                }
            }
        }

        // Post actions and info
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
                Icon(
                    painter = painterResource(id = if (liked) R.drawable.heart else R.drawable.heart_outline),
                    contentDescription = "Like",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onLikeClick(post) },
                    tint = if (liked) Color.Red else Color.Black
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_comment),
                    contentDescription = "Comment",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { navController?.navigate("post_detail/${post.postId}") },
                    tint = Color.Black
                )
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

        // Likes and comments count
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
                Text(
                    text = "Xem tất cả ${post.commentsCount} bình luận",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier
                        .clickable { navController?.navigate("post_detail/${post.postId}") }
                        .padding(vertical = 4.dp)
                )
            }
        }

        // Post caption
        Column(
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = "${post.user.username} ${post.caption}",
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

@Composable
fun PostSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .background(Color.White)
    ) {
        Divider(
            color = Color.Gray.copy(alpha = 0.5f),
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(33.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3E2E2))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(16.dp)
                    .background(Color(0xFFE3E2E2), shape = MaterialTheme.shapes.small)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFFE3E2E2))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(16.dp)
                .background(Color(0xFFE3E2E2), shape = MaterialTheme.shapes.small)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(16.dp)
                .background(Color(0xFFE3E2E2), shape = MaterialTheme.shapes.small)
        )
    }
}

