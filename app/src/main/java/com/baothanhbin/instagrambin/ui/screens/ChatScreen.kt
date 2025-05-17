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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(user.username)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messageState.messages) { message ->
                    MessageItem(
                        message = message,
                        isCurrentUser = message.senderId == currentUserId,
                        navController = navController
                    )
                }
            }

            // Emoji picker
            if (showEmojiPicker) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
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
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clickable {
                                        messageText += emojis[index]
                                    },
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
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
                    // Emoji button
                    IconButton(
                        onClick = { showEmojiPicker = !showEmojiPicker },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.EmojiEmotions,
                            contentDescription = "Emoji",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Image button
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Image,
                            contentDescription = "Image",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
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
                        onClick = {
                            if (messageText.isNotBlank()) {
                                messageViewModel.sendMessage(user.uid, messageText)
                                messageText = ""
                                keyboardController?.hide()
                                showEmojiPicker = false
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(
                            Icons.Rounded.Send,
                            contentDescription = "Send",
                            tint = if (messageText.isNotBlank()) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
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
    navController: NavController? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                        bottomEnd = if (isCurrentUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isCurrentUser) MaterialTheme.colorScheme.primary
                    else Color.LightGray.copy(alpha = 0.2f)
                )
                .padding(12.dp),
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            if (isImageUrl(message.content)) {
                // Hiển thị ảnh
                AsyncImage(
                    model = message.content,
                    contentDescription = "Message image",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            navController?.navigate("image_view/${Uri.encode(message.content)}")
                        },
                    contentScale = ContentScale.Crop
                )
            } else {
                // Hiển thị text
                Text(
                    text = message.content,
                    color = if (isCurrentUser) Color.White else Color.Black,
                    textAlign = if (isCurrentUser) TextAlign.End else TextAlign.Start
                )
            }
            Text(
                text = formatMessageTime(message.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
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