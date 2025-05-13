package com.baothanhbin.instagrambin.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.tooling.preview.Preview
import com.baothanhbin.instagrambin.model.User
import com.baothanhbin.instagrambin.ui.theme.InstagramUiComposeTheme
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import android.widget.Toast
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.baothanhbin.instagrambin.viewmodel.CommentViewModel
import com.baothanhbin.instagrambin.viewmodel.PostsSectionViewModel
import com.baothanhbin.instagrambin.model.Comment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    navController: NavController,
    postId: String,
    viewModel: CommentViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var commentText by remember { mutableStateOf("") }
    val postViewModel: PostsSectionViewModel = viewModel()
    val postState by postViewModel.uiState.collectAsState()

    LaunchedEffect(postId) {
        viewModel.loadComments(postId)
        postViewModel.loadPostById(postId)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comments") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (postState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val post = postState.currentPost
                if (post != null) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Content area with LazyColumn
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            item {
                                PostDetailContent(
                                    post = post,
                                    onLikeClick = { /* Handle like */ },
                                    onCommentClick = { /* Handle comment */ },
                                    onShareClick = { /* Handle share */ }
                                )
                            }

                            items(uiState.comments) { comment ->
                                CommentItem(comment = comment)
                            }
                        }

                        // Comment input section
                        Row(
                            modifier = Modifier
                                .padding(bottom = 70.dp, start = 16.dp, end = 16.dp)
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(24.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar user hiện tại
                            val homeViewModel: com.baothanhbin.instagrambin.viewmodel.HomeViewModel = viewModel()
                            val homeUiState by homeViewModel.uiState.collectAsState()
                            val currentUser = homeUiState.currentUser
                            AsyncImage(
                                model = currentUser?.profileImageUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = Color.Transparent,
                                        shape = RoundedCornerShape(24.dp)
                                    ),
                                placeholder = { Text("Viết bình luận...") },
                                maxLines = 3,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Send
                                ),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (commentText.isNotBlank()) {
                                            viewModel.addComment(postId, commentText)
                                            commentText = ""
                                            keyboardController?.hide()
                                        }
                                    }
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                )
                            )
                            IconButton(
                                onClick = {
                                    if (commentText.isNotBlank()) {
                                        viewModel.addComment(postId, commentText)
                                        commentText = ""
                                        keyboardController?.hide()
                                    }
                                },
                                enabled = commentText.isNotBlank()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Không tìm thấy bài viết",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun PostDetailContent(
    post: Post,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AsyncImage(
                    model = post.user.profileImageUrl,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = post.user.username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = formatTimestamp(post.timestamp),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        // Post Image
        AsyncImage(
            model = post.imageUrl,
            contentDescription = "Post image",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = post.caption,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = if (post.likes.isNotEmpty()) R.drawable.heart else R.drawable.heart_outline),
                    contentDescription = "Likes",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = post.likesCount.toString())
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_comment),
                    contentDescription = "Comments",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = post.commentsCount.toString())
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AsyncImage(
                    model = comment.user.profileImageUrl,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = comment.user.username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = formatTimestamp(comment.timestamp),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = comment.content,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 40.dp)
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Preview(showBackground = true)
@Composable
private fun PostDetailScreenPreview() {
    val mockUser = User(
        uid = "1",
        username = "baothanhbin",
        profileImageUrl = "https://randomuser.me/api/portraits/men/1.jpg"
    )
    
    val mockPost = Post(
        postId = "p1",
        userId = "1",
        imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb",
        caption = "Đây là một bài viết mẫu với caption dài để test hiển thị. Có thể có nhiều dòng và nhiều nội dung khác nhau.",
        timestamp = System.currentTimeMillis(),
        likesCount = 123,
        commentsCount = 45,
        user = mockUser,
        likes = mapOf("1" to true)
    )

    InstagramUiComposeTheme {
        Surface {
            PostDetailScreen(
                navController = rememberNavController(),
                postId = "p1"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PostDetailContentPreview() {
    val mockUser = User(
        uid = "1",
        username = "baothanhbin",
        profileImageUrl = "https://randomuser.me/api/portraits/men/1.jpg"
    )
    
    val mockPost = Post(
        postId = "p1",
        userId = "1",
        imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb",
        caption = "Đây là một bài viết mẫu với caption dài để test hiển thị. Có thể có nhiều dòng và nhiều nội dung khác nhau.",
        timestamp = System.currentTimeMillis(),
        likesCount = 123,
        commentsCount = 45,
        user = mockUser,
        likes = mapOf("1" to true)
    )

    InstagramUiComposeTheme {
        Surface {
            PostDetailContent(
                post = mockPost,
                onLikeClick = {},
                onCommentClick = {},
                onShareClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CommentItemPreview() {
    val mockComment = Comment(
        commentId = "c1",
        postId = "p1",
        userId = "2",
        content = "Bài viết rất đẹp! Cảm ơn bạn đã chia sẻ những khoảnh khắc tuyệt vời này.",
        timestamp = System.currentTimeMillis() - 3600000,
        user = User(
            uid = "2",
            username = "user1",
            profileImageUrl = "@drawable/ic_launcher_foreground"
        )
    )

    InstagramUiComposeTheme {
        Surface {
            CommentItem(comment = mockComment)
        }
    }
} 