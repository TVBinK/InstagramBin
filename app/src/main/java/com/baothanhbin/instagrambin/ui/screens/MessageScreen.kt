package com.baothanhbin.instagrambin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage

@Composable
fun MessageScreen(
    username: String = "yukiscape",
    onBack: () -> Unit = {},
    onAdd: () -> Unit = {},
    onCamera: () -> Unit = {},
    onStoryClick: (String) -> Unit = {},
    onMessageClick: (String) -> Unit = {}
) {
    val stories = listOf(
        Story("Lea", "", true),
        Story("User 1", "", false),
        Story("User 2", "", false),
        Story("User 3", "", false)
    )
    val messages = listOf(
        Message("User Protector", "Sent just now", "", false),
        Message("Aryan Dhanuka", "Aww  5m ago", "", true),
        Message("User Protector", "Sent just now", "", false),
        Message("Aryan Dhanuka", "Aww  5m ago", "", true),
        Message("User Protector", "Sent just now", "", false)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.clickable { onBack() })
            Text(username, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Camera", modifier = Modifier.size(24.dp).clickable { onCamera() })
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Filled.Add, contentDescription = "Add", modifier = Modifier.size(24.dp).clickable { onAdd() })
            }
        }

        // Search bar
        OutlinedTextField(
            value = "",
            onValueChange = {},
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("Search") },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(50.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFF2F2F2),
                focusedContainerColor = Color(0xFFF2F2F2),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Stories
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(stories) { story ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        AsyncImage(
                            model = story.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (story.isAdd) Color.White else Color(0xFF2B2726)),
                            contentScale = ContentScale.Crop
                        )
                        if (story.isAdd) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                                    .align(Alignment.BottomEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = story.name,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Messages
        Text(
            "Messages",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            items(messages) { msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMessageClick(msg.name) }
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = msg.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (msg.isHighlight) Color.White else Color(0xFFFF8A80)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(msg.name, fontWeight = if (msg.isHighlight) FontWeight.Bold else FontWeight.Normal)
                        Text(msg.lastMessage, color = Color.Gray, fontSize = 13.sp)
                    }
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Camera", modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

// Data classes for mock
data class Story(val name: String, val avatarUrl: String, val isAdd: Boolean)
data class Message(val name: String, val lastMessage: String, val avatarUrl: String, val isHighlight: Boolean)

@Preview(showBackground = true)
@Composable
fun MessageScreenPreview() {
    MessageScreen()
} 