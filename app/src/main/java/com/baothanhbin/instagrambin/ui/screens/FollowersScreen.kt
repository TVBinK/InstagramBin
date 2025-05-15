package com.baothanhbin.instagrambin.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.baothanhbin.instagrambin.R
import com.baothanhbin.instagrambin.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun FollowersScreen(
    followers: List<User>,
    onBackClick: () -> Unit,
    onUserClick: (String) -> Unit,
    onFollowClick: (String) -> Unit,
    onUnfollowClick: (String) -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val database = FirebaseDatabase.getInstance().reference
    var followingStates by remember { mutableStateOf(mapOf<String, Boolean>()) }

    LaunchedEffect(followers) {
        val states = mutableMapOf<String, Boolean>()
        followers.forEach { user ->
            if (currentUser?.uid != null) {
                database.child("users").child(currentUser.uid).child("following")
                    .child(user.uid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        states[user.uid] = snapshot.exists()
                        followingStates = states
                    }
            }
        }
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
                    modifier = Modifier.clickable { onBackClick() }
                )
                Text(
                    text = "Người theo dõi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                // Empty space to center the title
                Spacer(modifier = Modifier.width(24.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(followers) { user ->
                FollowerItem(
                    user = user,
                    onUserClick = { onUserClick(user.uid) },
                    isFollowing = followingStates[user.uid] ?: false,
                    onFollowClick = { onFollowClick(user.uid) },
                    onUnfollowClick = { onUnfollowClick(user.uid) }
                )
            }
        }
    }
}

@Composable
private fun FollowerItem(
    user: User,
    onUserClick: () -> Unit,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUserClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.profileImageUrl,
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentScale = ContentScale.Crop
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = user.username,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = user.fullName,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        
        Button(
            onClick = { if (isFollowing) onUnfollowClick() else onFollowClick() },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFollowing) Color.Gray else Color(0xFF3797EF)
            ),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = if (isFollowing) "Đang theo dõi" else "Theo dõi",
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FollowersScreenPreview() {
    val dummyFollowers = List(10) {
        User(
            uid = "user$it",
            username = "user$it",
            fullName = "User $it",
            profileImageUrl = "https://example.com/profile.jpg"
        )
    }

    FollowersScreen(
        followers = dummyFollowers,
        onBackClick = {},
        onUserClick = {},
        onFollowClick = {},
        onUnfollowClick = {}
    )
}