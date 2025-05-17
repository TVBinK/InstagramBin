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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.baothanhbin.instagrambin.viewmodel.SearchViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun SearchScreen(
    onUserClick: (String) -> Unit, // userId
    viewModel: SearchViewModel = viewModel()
) {
    var query by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    var showAllUsers by remember { mutableStateOf(false) }

    LaunchedEffect(showAllUsers) {
        if (showAllUsers) {
            viewModel.loadAllUsers()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Thanh tìm kiếm và nút hiển thị tất cả
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        showAllUsers = false
                        if (it.isNotBlank()) viewModel.searchUserByUsername(it)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    placeholder = {
                        Text(
                            "Tìm kiếm ...",
                            color = Color(0xFF999999),
                            fontSize = 16.sp,
                            maxLines = 1
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3797EF),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = Color(0xFFF8F9FA),
                        unfocusedContainerColor = Color(0xFFF8F9FA)
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Button(
                    onClick = {
                        showAllUsers = !showAllUsers
                        if (showAllUsers) {
                            query = ""
                            viewModel.loadAllUsers()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showAllUsers) Color(0xFF666666) else Color(0xFF3797EF)
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .shadow(2.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = if (showAllUsers) "Tìm kiếm" else "Tất cả",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (query.isBlank() && !showAllUsers) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .padding(bottom = 16.dp),
                        tint = Color(0xFFCCCCCC)
                    )
                    Text(
                        "Nhập username để tìm kiếm",
                        color = Color(0xFF999999),
                        fontSize = 16.sp
                    )
                }
            }
        } else if (uiState.isLoading) {
            // Skeleton loading
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(5) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .shadow(2.dp, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
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
                    }
                }
            }
        } else if (uiState.searchResults.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .padding(bottom = 16.dp),
                        tint = Color(0xFFCCCCCC)
                    )
                    Text(
                        "Không tìm thấy người dùng",
                        color = Color(0xFF999999),
                        fontSize = 16.sp
                    )
                }
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

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .scale(scale)
                            .shadow(2.dp, RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(bounded = true, color = Color(0xFF666666))
                            ) {
                                isPressed = true
                                onUserClick(user.uid)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = user.profileImageUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color(0xFFE0E0E0), CircleShape)
                                    .background(Color(0xFFF8F9FA)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    user.username,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF333333),
                                    fontSize = 15.sp
                                )
                                Text(
                                    user.fullName,
                                    color = Color(0xFF666666),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
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