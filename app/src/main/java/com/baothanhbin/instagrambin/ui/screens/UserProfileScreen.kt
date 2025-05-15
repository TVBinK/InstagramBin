package com.baothanhbin.instagrambin.ui.screens

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.baothanhbin.instagrambin.R
import com.baothanhbin.instagrambin.model.Post
import com.baothanhbin.instagrambin.model.User
import com.baothanhbin.instagrambin.viewmodel.UserProfileUiState
import com.baothanhbin.instagrambin.viewmodel.UserProfileViewModel
import kotlinx.coroutines.launch
import androidx.navigation.NavController

@Composable
fun UserProfileScreenContent(
    state: UserProfileUiState,
    onBackClick: () -> Unit,
    onPostClick: (Post) -> Unit,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
    onMessageClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit
) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.clickable { onBackClick() }
                )
                Text(
                    text = state.user?.username ?: "",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More",
                    modifier = Modifier.clickable { /* TODO: Show options menu */ }
                )
            }
        },
        containerColor = Color.White
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
        ) {
            // Profile Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Image
                        AsyncImage(
                            model = state.user?.profileImageUrl,
                            contentDescription = "Profile Image",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray),
                            contentScale = ContentScale.Crop
                        )

                        // Stats
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onFollowersClick() }
                            ) {
                                Text(
                                    text = state.user?.followersCount?.toString() ?: "0",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Người theo dõi",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onFollowingClick() }
                            ) {
                                Text(
                                    text = state.user?.followingCount?.toString() ?: "0",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Đang theo dõi",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Bio
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = state.user?.fullName ?: "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = state.user?.bio ?: "",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.isFollowing) {
                            Button(
                                onClick = onUnfollowClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEFEFEF)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Bỏ theo dõi",
                                    color = Color.Black,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            Button(
                                onClick = onFollowClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF3797EF)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Theo dõi",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Button(
                            onClick = onMessageClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEFEFEF)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Nhắn tin",
                                color = Color.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Posts Grid
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Grid Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, Color.LightGray)
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GridOn,
                            contentDescription = "Grid",
                            tint = Color.Black
                        )
                    }

                    // Posts Grid
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.posts) { post ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .padding(1.dp)
                                        .clickable { onPostClick(post) }
                                ) {
                                    AsyncImage(
                                        model = post.imageUrl,
                                        contentDescription = "Post",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserProfileScreen(
    viewModel: UserProfileViewModel,
    navController: NavController,
    onBackClick: () -> Unit,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
    onMessageClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    UserProfileScreenContent(
        state = state,
        onBackClick = onBackClick,
        onPostClick = { post -> 
            navController.navigate("post_detail/${post.postId}")
        },
        onFollowClick = onFollowClick,
        onUnfollowClick = onUnfollowClick,
        onMessageClick = onMessageClick,
        onFollowersClick = onFollowersClick,
        onFollowingClick = onFollowingClick
    )
}

@Preview(showBackground = true)
@Composable
fun UserProfileScreenPreview() {
    val mockUser = User(
        uid = "1",
        email = "mock@email.com",
        username = "mockuser",
        fullName = "Mock User",
        profileImageUrl = "",
        bio = "This is a mock bio.",
        followers = mapOf(
            "follower1" to true,
            "follower2" to true,
            "follower3" to true
        ),
        following = mapOf(
            "following1" to true,
            "following2" to true
        ),
        gender = "Other"
    )
    val mockPosts = List(6) { i ->
        Post(
            postId = "$i",
            userId = "1",
            imageUrl = "",
            caption = "Caption $i",
            timestamp = System.currentTimeMillis() - i * 100000,
            likesCount = i * 10,
            commentsCount = i * 2
        )
    }
    val mockState = com.baothanhbin.instagrambin.viewmodel.UserProfileUiState(
        user = mockUser,
        posts = mockPosts,
        isLoading = false,
        error = null,
        isFollowing = false
    )
    UserProfileScreenContent(
        state = mockState,
        onBackClick = {},
        onPostClick = {},
        onFollowClick = {},
        onUnfollowClick = {},
        onMessageClick = {},
        onFollowersClick = {},
        onFollowingClick = {}
    )
}