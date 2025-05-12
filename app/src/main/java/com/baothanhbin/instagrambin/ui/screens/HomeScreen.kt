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
import com.baothanhbin.instagrambin.model.User
import com.baothanhbin.instagrambin.ui.theme.InstagramUiComposeTheme
import com.baothanhbin.instagrambin.viewmodel.HomeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import androidx.navigation.NavController
import com.baothanhbin.instagrambin.ui.screen.TopBar
import com.baothanhbin.instagrambin.viewmodel.HomeUiState
import com.google.firebase.auth.FirebaseAuth

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    navController: NavController? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopBar(currentRoute = "home") }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = 72.dp
            ),
            modifier = Modifier.fillMaxSize()
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
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(Color.LightGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = user.profileImageUrl,
                                        contentDescription = "Your story",
                                        modifier = Modifier
                                            .fillMaxSize()
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
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = friend.profileImageUrl,
                                    contentDescription = friend.username,
                                    modifier = Modifier
                                        .fillMaxSize()
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
                    onLikeClick = { viewModel.toggleLike(post) },
                    onCommentClick = { /* TODO: mở giao diện bình luận */ }
                )
            }
        }
    }
}

@Composable
fun PostItem(
    post: Post,
    navController: NavController?,
    onLikeClick: (Post) -> Unit,
    onCommentClick: (Post) -> Unit
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
        // Post header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
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
                fontWeight = FontWeight.Bold
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

        // Post actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(
                    id = if (liked) R.drawable.heart else R.drawable.heart_outline
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { 
                        onLikeClick(post)
                        showHeart = true
                    },
                tint = if (liked) Color.Red else Color.Black
            )
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_comment),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onCommentClick(post) }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_send),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }

        // Post caption and likes
        Column(
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = "${post.likesCount} lượt thích",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "${post.user.username} ${post.caption}",
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "2 giờ trước",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val mockUser = User(
        uid = "1",
        username = "baothanhbin",
        profileImageUrl = "https://randomuser.me/api/portraits/men/1.jpg"
    )
    val mockFriends = listOf(
        User(uid = "2", username = "friend1", profileImageUrl = "https://randomuser.me/api/portraits/women/2.jpg"),
        User(uid = "3", username = "friend2", profileImageUrl = "https://randomuser.me/api/portraits/men/3.jpg"),
        User(uid = "4", username = "friend3", profileImageUrl = "https://randomuser.me/api/portraits/women/4.jpg")
    )
    val mockPosts = listOf(
        Post(
            postId = "p1",
            userId = "2",
            imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb",
            caption = "Caption 1",
            user = mockFriends[0],
            likesCount = 10
        ),
        Post(
            postId = "p2",
            userId = "3",
            imageUrl = "https://images.unsplash.com/photo-1519125323398-675f0ddb6308",
            caption = "Caption 2",
            user = mockFriends[1],
            likesCount = 5
        )
    )
    val mockState = HomeUiState(
        posts = mockPosts,
        friends = mockFriends,
        currentUser = mockUser
    )
    InstagramUiComposeTheme {
        HomeScreenPreviewContent(mockState)
    }
}

@Composable
private fun HomeScreenPreviewContent(mockState: HomeUiState) {
    // Không dùng viewModel, truyền trực tiếp state vào UI
    LazyColumn(
        contentPadding = PaddingValues(bottom = 72.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                mockState.currentUser?.let { user ->
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = user.profileImageUrl,
                                    contentDescription = "Your story",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Text("Tin của bạn", fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
                items(mockState.friends) { friend ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = friend.profileImageUrl,
                                contentDescription = friend.username,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Text(friend.username, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
        items(mockState.posts) { post ->
            PostItem(
                post = post,
                navController = null,
                onLikeClick = { /* TODO: implement toggleLike */ },
                onCommentClick = { /* TODO: implement comment click */ }
            )
        }
    }
}

