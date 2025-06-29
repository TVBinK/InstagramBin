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
import coil.request.ImageRequest
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
import com.google.firebase.auth.FirebaseAuth

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
    val homeViewModel: com.baothanhbin.instagrambin.viewmodel.HomeViewModel = viewModel()
    val homeUiState by homeViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.loadDataIfNeeded()
    }

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
                title = { Text("Chi tiết bài viết") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            if (postState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (postState.currentPost != null) {
                val post = postState.currentPost!!
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
                                onLikeClick = { postViewModel.toggleLike(post) },
                                onCommentClick = { /* Handle comment */ },
                                onShareClick = { /* Handle share */ }
                            )
                        }

                        // Hiển thị số lượng bình luận
                        item {
                            Text(
                                text = "${uiState.comments.size} bình luận",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        // Lọc unique comments theo commentId
                        val uniqueComments = uiState.comments.distinctBy { it.commentId }
                        val parentComments = uniqueComments.filter { it.replyToCommentId == null }
                        items(parentComments) { parent ->
                            CommentItem(
                                comment = parent,
                                onReply = { replyComment ->
                                    viewModel.setReplyTo(replyComment.commentId, replyComment.user.username)
                                },
                                isReply = false,
                                repliedUsername = null
                            )
                            // Hiển thị các reply của comment cha này
                            val replies = uniqueComments.filter { it.replyToCommentId == parent.commentId }
                            replies.forEach { reply ->
                                CommentItem(
                                    comment = reply,
                                    onReply = { replyComment ->
                                        viewModel.setReplyTo(replyComment.commentId, replyComment.user.username)
                                    },
                                    isReply = true,
                                    repliedUsername = uniqueComments.find { it.commentId == reply.replyToCommentId }?.user?.username
                                )
                            }
                        }
                    }

                    // Comment input section
                    Column(
                        modifier = Modifier
                            .padding(bottom = 70.dp, start = 16.dp, end = 16.dp)
                            .fillMaxWidth()
                    ) {
                        if (uiState.replyingToCommentId != null && uiState.replyingToUsername != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 2.dp)
                            ) {
                                Text(
                                    text = "Đang trả lời @${uiState.replyingToUsername}",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.clearReply() }) {
                                    Text("Hủy", color = Color.Red)
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
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
                            val currentUser = homeUiState.currentUser
                            
                            if (currentUser?.profileImageUrl.isNullOrEmpty()) {
                                Image(
                                    painter = painterResource(id = R.drawable.profile_pic),
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.LightGray),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(currentUser?.profileImageUrl)
                                        .crossfade(300)
                                        .size(64) // Load smaller size
                                        .build(),
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.LightGray),
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(id = R.drawable.profile_pic),
                                    placeholder = painterResource(id = R.drawable.profile_pic),
                                    onLoading = { /* Có thể thêm loading indicator nếu cần */ },
                                    onSuccess = { /* Ảnh load thành công */ },
                                    onError = { /* Xử lý lỗi nếu cần */ }
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                modifier = Modifier.weight(1f).background(
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(24.dp)
                                ),
                                placeholder = { Text("Viết bình luận...") },
                                maxLines = 3,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
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
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val liked = currentUserId != null && post.likes.containsKey(currentUserId)

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
                    text = post.user.fullName,
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
                    painter = painterResource(id = if (liked) R.drawable.heart else R.drawable.heart_outline),
                    contentDescription = "Likes",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onLikeClick() },
                    tint = if (liked) Color.Red else Color.Black
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
fun CommentItem(
    comment: Comment,
    onReply: (Comment) -> Unit = {},
    isReply: Boolean = false,
    repliedUsername: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isReply) 40.dp else 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
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
                    text = comment.user.fullName,
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
            style = MaterialTheme.typography.bodyMedium
        )
        if (isReply && repliedUsername != null) {
            Text(
                text = "Trả lời @$repliedUsername",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 0.dp, top = 2.dp)
            )
        }
        // Nút trả lời
        TextButton(
            onClick = { onReply(comment) },
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text("Trả lời", fontSize = 13.sp)
        }
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