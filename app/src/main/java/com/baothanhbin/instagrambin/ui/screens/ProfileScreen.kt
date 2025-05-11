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

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    val mockUser = com.baothanhbin.instagrambin.model.User(
        uid = "preview",
        email = "preview@example.com",
        username = "preview_user",
        fullName = "Preview User",
        bio = "This is a preview bio",
        followers = 100,
        following = 50
    )
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        TopBar(currentRoute = "profile")
    }, content = { contentPadding ->
        MainContent(
            modifier = Modifier.padding(contentPadding),
            onEditProfileClick = {},
            user = mockUser
        )
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(paddingValues: PaddingValues = PaddingValues(0.dp), navController: androidx.navigation.NavController? = null) {
    val viewModel: ProfileViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val authViewModel: com.baothanhbin.instagrambin.viewmodel.AuthViewModel = viewModel()
    var showMenu by remember { mutableStateOf(false) }

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
                        user = uiState.user!!
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
    user: com.baothanhbin.instagrambin.model.User
) {
    var selectedTabIndex by remember {
        mutableIntStateOf(0)
    }
    Column(modifier.fillMaxSize()) {
        ProfileSection(
            onEditProfileClick = onEditProfileClick,
            user = user
        )
        Spacer(modifier = Modifier.height(20.dp))
        PostsTabView(onTabSelected = { index ->
            selectedTabIndex = index
        })
        when (selectedTabIndex) {
            0 -> PostsSection()
        }
    }
}

@Composable
fun PostsSection() {
    val posts = listOf(
        painterResource(id = R.drawable.animated_topbar),
        painterResource(id = R.drawable.animation),
        painterResource(id = R.drawable.scrollable_column)
    )
    LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.scale(1.01f), content = {
        items(posts.size) {
            Image(
                painter = posts[it],
                contentDescription = "",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .aspectRatio(1f)
                    .border(
                        width = 1.dp, color = Color.White
                    )
            )
        }
    })
}

@Composable
fun PostsTabView(
    modifier: Modifier = Modifier, onTabSelected: (selectedIndex: Int) -> Unit
) {
    var selectedTabIndex by remember {
        mutableIntStateOf(0)
    }
    val tabIcons = listOf(
        TabRowIcons(R.drawable.ic_grid),
        TabRowIcons(R.drawable.instagram_reels_icon),
        TabRowIcons(R.drawable.instagram_tag_icon)
    )
    TabRow(
        selectedTabIndex = selectedTabIndex, modifier = modifier
    ) {
        tabIcons.forEachIndexed { index, tabRowIcons ->
            Tab(selected = index == selectedTabIndex, onClick = {
                selectedTabIndex = index
                onTabSelected(index)
            }, icon = {
                Icon(
                    painter = painterResource(id = tabRowIcons.icon),
                    contentDescription = "",
                    modifier = Modifier.size(20.dp),
                    tint = if (selectedTabIndex == index) Color.Black else Color.Gray
                )
            }, selectedContentColor = Color.Black, unselectedContentColor = Color.Gray
            )
        }
    }
}

@Composable
fun ProfileSection(
    modifier: Modifier = Modifier,
    onEditProfileClick: () -> Unit,
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
                followers = user.followers,
                following = user.following
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
        contentDescription = "",
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
    following: Int = 0
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "0",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(text = "Bài viết", fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = followers.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(text = "Người theo dõi", fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = following.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(text = "Đang theo dõi", fontSize = 12.sp)
        }
    }
}