package com.baothanhbin.instagrambin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baothanhbin.instagrambin.model.Message
import com.baothanhbin.instagrambin.model.User
import com.baothanhbin.instagrambin.viewmodel.MessageViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.tooling.preview.Preview
import com.baothanhbin.instagrambin.ui.theme.InstagramUiComposeTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Send
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.NavController
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import com.baothanhbin.instagrambin.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    user: User,
    onBackClick: () -> Unit,
    messageViewModel: MessageViewModel = viewModel(),
    navController: NavController? = null
) {
    var messageText by remember { mutableStateOf("") }
    val messageState by messageViewModel.uiState.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    var showEmojiPicker by remember { mutableStateOf(false) }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            messageViewModel.sendImage(user.uid, it)
        }
    }

    LaunchedEffect(user.uid) {
        messageViewModel.loadMessages(user.uid)
    }

    // Track previous message count to detect new messages
    var previousMessageCount by remember { mutableStateOf(0) }
    
    LaunchedEffect(messageState.messages.size) {
        if (messageState.messages.size > previousMessageCount) {
            // Only scroll if new messages were added
            listState.animateScrollToItem(messageState.messages.lastIndex)
            previousMessageCount = messageState.messages.size
        }
    }

    // Swipe refresh state
    val swipeRefreshState = rememberSwipeRefreshState(
        isRefreshing = messageState.isLoading
    )

    // Load more messages when scrolling to top
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (listState.firstVisibleItemIndex <= 5 && messageState.hasMoreMessages && !messageState.isLoadingMore) {
            messageViewModel.loadMoreMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar
                        if (!user.profileImageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = user.profileImageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color.LightGray, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(user.username, style = MaterialTheme.typography.titleMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(onClick = { messageViewModel.refreshMessages() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F7F7))
                .padding(paddingValues)
        ) {
            // Messages list with swipe refresh
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { messageViewModel.refreshMessages() },
                modifier = Modifier.weight(1f)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Loading more indicator at top
                    if (messageState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp) }
                        }
                    }

                    // Messages
                    items(messageState.messages) { message ->
                        MessageItem(
                            message = message,
                            isCurrentUser = message.senderId == currentUserId,
                            showAvatar = message.senderId != currentUserId,
                            avatarUrl = if (message.senderId != currentUserId) user.profileImageUrl else null,
                            navController = navController
                        )
                    }

                    // Loading indicator at bottom for initial load
                    if (messageState.isLoading && messageState.messages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                        }
                    }

                    // Error message
                    if (messageState.error != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = messageState.error!!,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { messageViewModel.clearError() }) { Text("Dismiss") }
                                }
                            }
                        }
                    }
                }
            }

            // Emoji picker
            if (showEmojiPicker) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(8),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        items(emojis.size) { index ->
                            Text(
                                text = emojis[index],
                                modifier = Modifier.padding(4.dp).clickable { messageText += emojis[index] },
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }

            // Input bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showEmojiPicker = !showEmojiPicker },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Rounded.EmojiEmotions, contentDescription = "Emoji", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = "Image", tint = MaterialTheme.colorScheme.primary)
                    }
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        placeholder = { Text("Type a message...") },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 5,
                        shape = RoundedCornerShape(24.dp)
                    )
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                messageViewModel.sendMessage(user.uid, messageText)
                                messageText = ""
                                keyboardController?.hide()
                                showEmojiPicker = false
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(
                            Icons.Rounded.Send,
                            contentDescription = "Send",
                            tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Chat Screen")
@Composable
private fun ChatScreenPreview() {
    InstagramUiComposeTheme {
        val sampleUser = User(
            uid = "preview_user_id",
            email = "preview@example.com",
            username = "preview_user",
            fullName = "Preview User",
            profileImageUrl = "https://example.com/profile.jpg",
            bio = "This is a preview user",
            followers = mapOf(),
            following = mapOf()
        )

        val sampleMessages = listOf(
            Message(
                messageId = "1",
                senderId = "current_user",
                receiverId = "preview_user_id",
                content = "Hello! How are you?",
                timestamp = System.currentTimeMillis() - 3600000 // 1 hour ago
            ),
            Message(
                messageId = "2",
                senderId = "preview_user_id",
                receiverId = "current_user",
                content = "Hi! I'm good, thanks for asking!",
                timestamp = System.currentTimeMillis() - 3500000 // 58 minutes ago
            ),
            Message(
                messageId = "3",
                senderId = "current_user",
                receiverId = "preview_user_id",
                content = "What are you up to?",
                timestamp = System.currentTimeMillis() - 3400000 // 56 minutes ago
            )
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sampleMessages) { message ->
                    MessageItem(
                        message = message,
                        isCurrentUser = message.senderId == "current_user"
                    )
                }
            }

            // Message input
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Image button
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Image",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    TextField(
                        value = "",
                        onValueChange = { },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        placeholder = { Text("Type a message...") },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 5
                    )

                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(40.dp),
                        enabled = false
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }
        }
    }
}

private fun formatMessageTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun FullScreenImageView(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var lastPanX by remember { mutableStateOf(0f) }
    var lastPanY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }

        // Image with zoom and pan support
        AsyncImage(
            model = imageUrl,
            contentDescription = "Full screen image",
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        
                        if (scale > 1f) {
                            // Calculate new offset
                            val newOffsetX = offsetX + pan.x
                            val newOffsetY = offsetY + pan.y
                            
                            // Apply constraints to keep image within bounds
                            val maxOffset = (scale - 1f) * size.width / 2
                            offsetX = newOffsetX.coerceIn(-maxOffset, maxOffset)
                            offsetY = newOffsetY.coerceIn(-maxOffset, maxOffset)
                        } else {
                            // Reset position when scale is 1 or less
                            offsetX = 0f
                            offsetY = 0f
                        }
                        
                        lastPanX = pan.x
                        lastPanY = pan.y
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
                .align(Alignment.Center),
            contentScale = ContentScale.Fit
        )
    }
}

fun isImageUrl(url: String): Boolean {
    val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp")
    return (url.startsWith("http") && imageExtensions.any { url.contains(it, ignoreCase = true) }) ||
            url.contains("cloudinary", ignoreCase = true)
}

@Composable
fun MessageItem(
    message: Message,
    isCurrentUser: Boolean,
    showAvatar: Boolean = false,
    avatarUrl: String? = null,
    navController: NavController? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isCurrentUser && showAvatar) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.LightGray, CircleShape)
                        .align(Alignment.Bottom),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(4.dp))
            } else {
                // Default avatar
                Image(
                    painter = painterResource(id = R.drawable.ic_profile),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.LightGray, CircleShape)
                        .align(Alignment.Bottom)
                )
                Spacer(Modifier.width(4.dp))
            }
        }
        Column(
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = if (isCurrentUser) Color(0xFF8A56AC) else Color.White,
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomEnd = if (isCurrentUser) 4.dp else 18.dp,
                            bottomStart = if (isCurrentUser) 18.dp else 4.dp
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = if (isCurrentUser) Color(0xFF8A56AC) else Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomEnd = if (isCurrentUser) 4.dp else 18.dp,
                            bottomStart = if (isCurrentUser) 18.dp else 4.dp
                        )
                    )
                    .padding(10.dp)
                    .widthIn(max = 260.dp)
            ) {
                if (isImageUrl(message.content)) {
                    AsyncImage(
                        model = message.content,
                        contentDescription = "Message image",
                        modifier = Modifier
                            .sizeIn(maxWidth = 200.dp, maxHeight = 200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { navController?.navigate("image_view/${Uri.encode(message.content)}") },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = message.content,
                        color = if (isCurrentUser) Color.White else Color.Black,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            Text(
                text = formatMessageTime(message.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 2.dp, start = 6.dp, end = 6.dp)
            )
        }
        if (isCurrentUser) {
            Spacer(Modifier.width(32.dp))
        }
    }
}

@Composable
fun FullImageScreen(imageUrl: String, onBack: () -> Unit) {
    FullScreenImageView(
        imageUrl = imageUrl,
        onDismiss = onBack
    )
}

// List of emojis
private val emojis = listOf(
    "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
    "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰",
    "😘", "😗", "😙", "😚", "😋", "😛", "😝", "😜",
    "🤪", "🤨", "🧐", "🤓", "😎", "🤩", "🥳", "😏",
    "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣",
    "😖", "😫", "😩", "🥺", "😢", "😭", "😤", "😠",
    "😡", "🤬", "🤯", "😳", "🥵", "🥶", "😱", "😨",
    "😰", "😥", "😓", "🤗", "🤔", "🤭", "🤫", "🤥",
    "😶", "😐", "😑", "😬", "🙄", "😯", "😦", "😧",
    "😮", "😲", "🥱", "😴", "🤤", "😪", "😵", "🤐",
    "🥴", "🤢", "🤮", "🤧", "😷", "🤒", "🤕", "🤑",
    "🤠", "💩", "👻", "👽", "🤖", "😺", "😸", "😹",
    "😻", "😼", "😽", "🙀", "😿", "😾", "🙈", "🙉",
    "🙊", "👶", "👧", "🧒", "👦", "👩", "🧑", "👨",
    "👵", "🧓", "👴", "👮", "🕵️", "👷", "👸", "🤴",
    "👳", "👲", "🧕", "🤵", "👰", "🤰", "🤱", "👼",
    "🎅", "🤶", "🧙", "🧚", "🧛", "🧜", "🧝", "🧞",
    "🧟", "🧌", "💆", "💇", "🚶", "🧍", "🧎", "🏃",
    "💃", "🕺", "🕴️", "👯", "🧖", "🧗", "🤺", "🤾",
    "🏌️", "🏇", "🧘", "🏄", "🏊", "🤽", "🏋️", "🚴",
    "🚵", "🤸", "⛹️", "🤹", "🤼", "🤽", "🤾", "🤺"
) 