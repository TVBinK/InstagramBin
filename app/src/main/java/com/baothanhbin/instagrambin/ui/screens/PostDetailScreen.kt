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
import android.app.Application
import com.baothanhbin.instagrambin.viewmodel.CommentViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    navController: NavController,
    postId: String,
    viewModel: CommentViewModel = viewModel(
        factory = CommentViewModelFactory(LocalContext.current.applicationContext as Application)
    )
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
                                navController = navController,
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
                        val commentMap = uniqueComments.associateBy { it.commentId }
                        item {
                            RecursiveCommentList(
                                comments = uniqueComments,
                                commentMap = commentMap,
                                navController = navController,
                                parentId = null,
                                onReply = { replyComment ->
                                    viewModel.setReplyTo(replyComment.commentId, replyComment.user.username)
                                }
                            )
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
    navController: NavController,
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
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        if (post.userId == currentUserId) {
                            navController.navigate("profile")
                        } else {
                            navController.navigate("user_profile/${post.userId}")
                        }
                    },
                    color = MaterialTheme.colorScheme.primary
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
    navController: NavController,
    onReply: (Comment) -> Unit = {},
    depth: Int = 0,
    repliedUsername: String? = null,
    timestampText: String = "",
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val startPadding = 16.dp + (depth.dp * 14)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding, end = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = comment.user.profileImageUrl,
            contentDescription = "Profile",
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.user.fullName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            if (comment.userId == currentUserId) {
                                navController.navigate("profile")
                            } else {
                                navController.navigate("user_profile/${comment.userId}")
                            }
                        }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = timestampText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            if (depth > 0 && repliedUsername != null) {
                Text(
                    text = "Trả lời @$repliedUsername",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.Start)
                )
            }
            TextButton(
                onClick = { onReply(comment) },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text("Trả lời", fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun RecursiveCommentList(
    comments: List<Comment>,
    commentMap: Map<String, Comment>,
    navController: NavController,
    parentId: String? = null,
    depth: Int = 0,
    onReply: (Comment) -> Unit
) {
    val children = comments.filter { it.replyToCommentId == parentId }
    var showAllReplies by remember(parentId) { mutableStateOf(false) }
    val childrenToShow = if (parentId != null && !showAllReplies && children.size > 1) children.take(1) else children
    childrenToShow.forEach { comment ->
        val repliedUsername = comment.replyToCommentId?.let { commentMap[it]?.user?.username }
        val timestampText = formatTimestamp(comment.timestamp)
        val replies = comments.filter { it.replyToCommentId == comment.commentId }
        val hiddenRepliesCount = if (!showAllReplies && replies.size > 1) replies.size - 1 else 0
        Column(
            modifier = Modifier
                .padding(
                    top = if (depth == 0) 6.dp else 0.dp,
                    bottom = if (depth == 0) 6.dp else 0.dp
                )
        ) {
            CommentItem(
                comment = comment,
                navController = navController,
                onReply = onReply,
                depth = depth,
                repliedUsername = repliedUsername,
                timestampText = timestampText
            )
            // Đệ quy cho reply của comment này
            RecursiveCommentList(
                comments = comments,
                commentMap = commentMap,
                navController = navController,
                parentId = comment.commentId,
                depth = depth + 1,
                onReply = onReply
            )
            if (parentId != null && hiddenRepliesCount > 0) {
                Text(
                    text = "Xem $hiddenRepliesCount câu trả lời khác",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = ((depth + 1) * 14).dp + 48.dp, top = 2.dp)
                        .clickable { showAllReplies = true }
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}