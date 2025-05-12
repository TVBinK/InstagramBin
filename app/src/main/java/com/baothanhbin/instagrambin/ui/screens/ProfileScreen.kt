package com.baothanhbin.instagrambin.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baothanhbin.instagrambin.R
import com.baothanhbin.instagrambin.model.StoryHighlights
import com.baothanhbin.instagrambin.model.TabRowIcons
import com.baothanhbin.instagrambin.ui.screen.TopBar
import com.baothanhbin.instagrambin.ui.theme.InstagramUiComposeTheme
import com.baothanhbin.instagrambin.viewmodel.ProfileViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.baothanhbin.instagrambin.viewmodel.PostsSectionViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(paddingValues: PaddingValues = PaddingValues(0.dp), navController: androidx.navigation.NavController? = null) {
    val viewModel: ProfileViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    InstagramUiComposeTheme {
        Scaffold(
            topBar = {
                TopBar(currentRoute = "profile")
            }
        ) { contentPadding ->
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = uiState.error!!, color = Color.Red)
                    }
                }
                uiState.user != null -> {
                    MainContent(
                        modifier = Modifier.padding(contentPadding),
                        onEditProfileClick = {
                            navController?.navigate("edit_profile")
                        },
                        onFollowersClick = {
                            navController?.navigate("followers/${uiState.user!!.uid}")
                        },
                        onFollowingClick = {
                            navController?.navigate("following/${uiState.user!!.uid}")
                        },
                        user = uiState.user!!,
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun MainContent(
    modifier: Modifier,
    onEditProfileClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    user: com.baothanhbin.instagrambin.model.User,
    navController: NavController?
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize()) {
        ProfileSection(
            onEditProfileClick = onEditProfileClick,
            onFollowersClick = onFollowersClick,
            onFollowingClick = onFollowingClick,
            user = user
        )
        Spacer(modifier = Modifier.height(20.dp))
        PostsTabView(
            selectedTabIndex = selectedTabIndex,
            onTabSelected = { index ->
                selectedTabIndex = index
            }
        )
        when (selectedTabIndex) {
            0 -> PostsSection(navController = navController)
        }
    }
}

@Composable
fun PostsSection(
    viewModel: PostsSectionViewModel = viewModel(),
    navController: NavController? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = uiState.error ?: "Error loading posts")
            }
        }
        else -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.scale(1.01f)
            ) {
                items(uiState.posts.size) { index ->
                    val post = uiState.posts[index]
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = "Post",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .border(width = 1.dp, color = Color.White)
                            .clickable {
                                navController?.navigate("post_detail/${post.postId}")
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun PostsTabView(
    modifier: Modifier = Modifier,
    selectedTabIndex: Int,
    onTabSelected: (selectedIndex: Int) -> Unit
) {
    val tabIcons = listOf(
        TabRowIcons(R.drawable.ic_grid),
        TabRowIcons(R.drawable.instagram_reels_icon),
        TabRowIcons(R.drawable.instagram_tag_icon)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabIcons.forEachIndexed { index, tabRowIcons ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable {
                        onTabSelected(index)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = tabRowIcons.icon),
                    contentDescription = "Tab ${index + 1}",
                    modifier = Modifier.size(20.dp),
                    tint = if (selectedTabIndex == index) Color.Black else Color.Gray
                )
                if (selectedTabIndex == index) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .align(Alignment.BottomCenter)
                            .background(Color.Black)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileSection(
    modifier: Modifier = Modifier,
    onEditProfileClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    user: com.baothanhbin.instagrambin.model.User
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            if (user.profileImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = user.profileImageUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.LightGray, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.profile_pic),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.LightGray, CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            FollowStatusBar(
                modifier = Modifier.weight(8f),
                followers = user.followersCount,
                following = user.followingCount,
                onFollowersClick = onFollowersClick,
                onFollowingClick = onFollowingClick
            )
        }
        BioSection(
            name = user.fullName,
            username = user.username,
            description = user.bio,
        )
        ButtonsSection(modifier = modifier.fillMaxWidth(), onEditProfileClick = onEditProfileClick)
        Spacer(modifier = Modifier.height(20.dp))
        HighlightSection(
            highlight = listOf(
                StoryHighlights(
                    image = painterResource(id = R.drawable.img_plus),
                    title = "Mới",
                    isNewStory = true
                ),
                StoryHighlights(
                    image = painterResource(id = R.drawable.insta_highlight_discorde),
                    title = "Discord"
                ),
                StoryHighlights(
                    image = painterResource(id = R.drawable.insta_highlight_youtube),
                    title = "Youtube"
                ),
            )
        )
    }
}

@Composable
fun ImageBuilder(image: Painter, modifier: Modifier) {
    Image(
        painter = image,
        contentDescription = "Highlight",
        modifier = modifier
            .size(70.dp)
            .clip(CircleShape)
            .border(width = 2.dp, color = Color.LightGray, shape = CircleShape)
            .padding(5.dp)
    )
}

@Composable
fun HighlightSection(highlight: List<StoryHighlights>, modifier: Modifier = Modifier) {
    LazyRow(modifier.padding(horizontal = 20.dp)) {
        items(highlight.size) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(end = 15.dp)
            ) {
                if (highlight[it].isNewStory) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F5F5))
                            .border(1.dp, Color.LightGray, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Thêm câu chuyện mới",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    ImageBuilder(
                        image = highlight[it].image
                            ?: painterResource(id = R.drawable.ic_launcher_foreground),
                        modifier = Modifier.size(70.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = highlight[it].title, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ButtonsSection(modifier: Modifier, onEditProfileClick: () -> Unit) {
    val width = 400.dp
    val height = 30.dp
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        SampleButton(
            modifier = Modifier
                .width(width)
                .height(height)
                .clickable { onEditProfileClick() },
            text = "Chỉnh sửa trang cá nhân",
        )
    }
}

@Composable
fun SampleButton(
    modifier: Modifier = Modifier, text: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFFF1F1F1))
    ) {
        if (text != null) Text(
            text = text, fontWeight = FontWeight.SemiBold, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BioSection(
    name: String,
    username: String,
    description: String,
) {
    val letterSpacing = 0.5.sp
    val lineHeight = 20.sp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = name,
            fontWeight = FontWeight.Bold,
            letterSpacing = letterSpacing,
            lineHeight = lineHeight
        )
        Text(
            text = "@${username}",
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
            letterSpacing = letterSpacing,
            lineHeight = lineHeight
        )
        Text(
            text = description,
            fontWeight = FontWeight.Medium,
            letterSpacing = letterSpacing,
            lineHeight = lineHeight
        )
    }
}

@Composable
fun FollowStatusBar(
    modifier: Modifier = Modifier,
    followers: Int = 0,
    following: Int = 0,
    onFollowersClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {}
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(onClick = onFollowersClick)
        ) {
            Text(
                text = "0",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(text = "Bài viết", fontSize = 12.sp)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(onClick = onFollowersClick)
        ) {
            Text(
                text = followers.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(text = "Người theo dõi", fontSize = 12.sp)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(onClick = onFollowingClick)
        ) {
            Text(
                text = following.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(text = "Đang theo dõi", fontSize = 12.sp)
        }
    }
}