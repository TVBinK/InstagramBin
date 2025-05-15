package com.baothanhbin.instagrambin.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.baothanhbin.instagrambin.viewmodel.SearchViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baothanhbin.instagrambin.model.UserProfile

@Composable
fun SearchScreen(
    onUserClick: (String) -> Unit, // userId
    viewModel: SearchViewModel = viewModel()
) {
    var query by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Thanh tìm kiếm
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                if (it.isNotBlank()) viewModel.searchUserByUsername(it)
            },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF666666)) },
            placeholder = { Text("Tìm kiếm username...", color = Color(0xFF999999)) },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF666666),
                unfocusedBorderColor = Color(0xFFCCCCCC),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (query.isBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Nhập username để tìm kiếm", color = Color(0xFF999999))
            }
        } else if (uiState.isLoading) {
            // Skeleton loading
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(5) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0E0E0))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(16.dp)
                                    .background(Color(0xFFE0E0E0), shape = MaterialTheme.shapes.small)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(12.dp)
                                    .background(Color(0xFFE0E0E0), shape = MaterialTheme.shapes.small)
                            )
                        }
                    }
                    Divider(color = Color(0xFFE0E0E0))
                }
            }
        } else if (uiState.searchResults.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Không tìm thấy người dùng", color = Color(0xFF999999))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(uiState.searchResults) { user ->
                    var isPressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.95f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "Scale Animation"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(bounded = true, color = Color(0xFF666666))
                            ) {
                                isPressed = true
                                onUserClick(user.uid)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = user.profileImageUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0E0E0)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.username, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                            Text(user.fullName, color = Color(0xFF666666), fontSize = 13.sp)
                        }
                    }
                    Divider(color = Color(0xFFE0E0E0))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    SearchScreen(onUserClick = {})
}