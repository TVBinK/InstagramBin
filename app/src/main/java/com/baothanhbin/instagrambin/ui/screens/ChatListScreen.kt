package com.baothanhbin.instagrambin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import coil.compose.AsyncImage
import com.baothanhbin.instagrambin.model.User
import com.baothanhbin.instagrambin.model.Message
import com.baothanhbin.instagrambin.ui.components.MessageItem
import com.baothanhbin.instagrambin.viewmodel.ChatListViewModel
import com.baothanhbin.instagrambin.viewmodel.MessageViewModel
import com.baothanhbin.instagrambin.viewmodel.SearchViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalContext
import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.compose.animation.core.*
import com.baothanhbin.instagrambin.R
import com.baothanhbin.instagrambin.ui.theme.InstagramUiComposeTheme
import com.baothanhbin.instagrambin.viewmodel.ChatPreview

@Composable
fun ShimmerEffect() {
    val transition = rememberInfiniteTransition()
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    val shimmerColors = listOf(
        Color(0xFF1976D2).copy(alpha = 0.6f),
        Color(0xFF1976D2).copy(alpha = 0.2f),
        Color(0xFF1976D2).copy(alpha = 0.6f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset(x = translateAnim.value, y = translateAnim.value)
    )

    ShimmerItem(brush = brush)
}

@Composable
fun ShimmerItem(brush: Brush) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar shimmer with gradient border
        Box(
            modifier = Modifier
                .size(48.dp)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFDE0046),
                            Color(0xFFF7A34B),
                            Color(0xFFDE0046)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(brush)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Name shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Message shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Time shimmer
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )
    }
    Divider(
        modifier = Modifier.padding(start = 72.dp, end = 0.dp),
        color = Color(0xFFE0E0E0)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onBackClick: () -> Unit,
    onChatClick: (User) -> Unit,
    onNewMessageClick: () -> Unit,
    searchViewModel: SearchViewModel = viewModel()
) {
    val chatListViewModel: ChatListViewModel = viewModel()
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    val chatListState by chatListViewModel.uiState.collectAsState()
    val searchState by searchViewModel.uiState.collectAsState()
    val username = FirebaseAuth.getInstance().currentUser?.displayName ?: "username"

    LaunchedEffect(Unit) {
        searchQuery = ""
        chatListViewModel.loadChats()
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            isSearching = true
            delay(300)
            searchViewModel.searchUserByUsername(searchQuery)
            isSearching = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
            IconButton(onClick = onNewMessageClick) {
                Icon(Icons.Default.Add, contentDescription = "New Message", tint = Color.Black)
            }
        }
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search", color = Color.Gray) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(20.dp)),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color(0xFFF0F0F0),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent
            ),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Chat list
        when {
            chatListState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            isSearching -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            searchQuery.isNotBlank() -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchState.searchResults) { user ->
                        ChatListItemInstagram(
                            user = user,
                            lastMessage = null,
                            lastMessageTime = null,
                            onChatClick = { onChatClick(user) }
                        )
                    }
                }
            }
            chatListState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = chatListState.error ?: "An error occurred")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(chatListState.chats) { chatPreview ->
                        ChatListItemInstagram(
                            user = chatPreview.user,
                            lastMessage = chatPreview.lastMessage,
                            lastMessageTime = timeAgo(chatPreview.lastMessageTime),
                            onChatClick = { onChatClick(chatPreview.user) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItemInstagram(
    user: User,
    lastMessage: String?,
    lastMessageTime: String?,
    onChatClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onChatClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with gradient border
        Box(
            modifier = Modifier
                .size(48.dp)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFDE0046), // Instagram pink
                            Color(0xFFF7A34B), // Instagram orange
                            Color(0xFFDE0046)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = user.profileImageUrl,
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = user.fullName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black
            )
            Text(
                text = lastMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = lastMessageTime ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
    Divider(
        modifier = Modifier.padding(start = 72.dp, end = 0.dp),
        color = Color(0xFFE0E0E0)
    )
}

fun timeAgo(time: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - time
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        time == 0L -> ""
        seconds < 60 -> "now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        else -> "${days}d"
    }
}
