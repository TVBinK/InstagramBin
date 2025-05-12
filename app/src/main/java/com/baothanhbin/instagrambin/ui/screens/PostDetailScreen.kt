package com.baothanhbin.instagrambin.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.baothanhbin.instagrambin.R
import com.baothanhbin.instagrambin.model.Post
import com.baothanhbin.instagrambin.ui.screen.TopBar
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baothanhbin.instagrambin.viewmodel.PostsSectionViewModel
import kotlinx.coroutines.delay

@Composable
fun PostDetailScreen(
    postId: String,
    navController: NavController,
    postsViewModel: PostsSectionViewModel = viewModel()
) {
    val uiState by postsViewModel.uiState.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val post = uiState.posts.find { it.postId == postId }
    val liked = currentUserId != null && post?.likes?.containsKey(currentUserId) == true
    var showHeart by remember { mutableStateOf(false) }

    LaunchedEffect(showHeart) {
        if (showHeart) {
            delay(1000)
            showHeart = false
        }
    }

    if (uiState.error != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = uiState.error ?: "Không tìm thấy bài viết",
                color = Color.Red
            )
        }
        return
    }

    if (post == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

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
                    modifier = Modifier.clickable { navController.navigateUp() }
                )
                Text(
                    text = "Bài viết",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                // Empty space to center the title
                Spacer(modifier = Modifier.width(24.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        .border(1.dp, Color.LightGray, CircleShape)
                        .size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = post.user.profileImageUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
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
            ) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

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
                    contentDescription = "Like",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            postsViewModel.toggleLike(post)
                            showHeart = true
                        },
                    tint = if (liked) Color.Red else Color.Black
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_comment),
                    contentDescription = "Comment",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_send),
                    contentDescription = "Share",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Post likes count
            Text(
                text = "${post.likesCount} lượt thích",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            // Post caption
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = post.user.username,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = post.caption)
            }

            // Post timestamp
            Text(
                text = "2 giờ trước", // TODO: Format timestamp
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
} 