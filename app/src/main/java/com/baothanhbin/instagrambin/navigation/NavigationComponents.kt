package com.baothanhbin.instagrambin.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.navigation.NavController
import com.baothanhbin.instagrambin.R
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baothanhbin.instagrambin.viewmodel.HomeViewModel
import androidx.compose.runtime.collectAsState

data class BottomBarItem(val icon: Int, val route: String)

@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    currentRoute: String? = null,
    onLogout: () -> Unit = {},
    navController: NavController? = null
) {
    val showMenu = remember { mutableStateOf(false) }

    if (currentRoute == "profile" || currentRoute == "home" || currentRoute?.startsWith("user_profile/") == true) {
        TopAppBar(
            title = {
                if (currentRoute == "profile") {
                } else if (currentRoute == "home") {
                    Icon(
                        modifier = Modifier
                            .height(40.dp)
                            .padding(start = 14.dp, bottom = 6.dp),
                        painter = painterResource(id = R.drawable.instagram),
                        contentDescription = "instagram"
                    )
                }
            },
            actions = {
                if (currentRoute == "profile") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(end = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            modifier = Modifier.size(30.dp),
                            tint = Color.Black
                        )
                        Box {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                modifier = Modifier
                                    .size(30.dp)
                                    .clickable { showMenu.value = !showMenu.value },
                                tint = Color.Black
                            )
                            DropdownMenu(
                                expanded = showMenu.value,
                                onDismissRequest = { showMenu.value = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Đăng xuất") },
                                    onClick = {
                                        showMenu.value = false
                                        onLogout()
                                    }
                                )
                            }
                        }
                    }
                } else if (currentRoute == "home"){
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(end = 14.dp)
                    ) {
                        Icon(
                            modifier = Modifier.size(23.dp),
                            painter = painterResource(id = R.drawable.ic_heart),
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Icon(
                            modifier = Modifier
                                .size(23.dp)
                                .clickable { navController?.navigate("message") },
                            painter = painterResource(id = R.drawable.ic_send),
                            contentDescription = null
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.White
            ),
        )
    }
}

@Composable
fun BottomBar(
    navController: NavController? = null,
    onNavigate: (String) -> Unit = {},
    currentRoute: String? = null
) {
    val homeViewModel: HomeViewModel = viewModel()
    val uiState by homeViewModel.uiState.collectAsState()
    val currentUser = uiState.currentUser
    val bottomBarItems = listOf(
        BottomBarItem(R.drawable.ic_home, "home"),
        BottomBarItem(R.drawable.ic_search, "search"),
        BottomBarItem(R.drawable.ic_add, "add"),
        BottomBarItem(R.drawable.ic_profile, "profile")
    )
    if (currentRoute?.startsWith("user_profile/") != true) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 10.dp)
                .background(Color.White),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            bottomBarItems.forEach { item ->
                val isSelected = currentRoute == item.route
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.LightGray else Color.Transparent)
                        .clickable {
                            if (currentRoute != item.route) {
                                if (navController != null) {
                                    navController.navigate(item.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } else {
                                    onNavigate(item.route)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (item.route == "profile" && currentUser?.profileImageUrl?.isNotEmpty() == true) {
                        AsyncImage(
                            model = currentUser.profileImageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(if (isSelected) 26.dp else 22.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            modifier = Modifier
                                .size(if (isSelected) 26.dp else 22.dp),
                            painter = painterResource(id = item.icon),
                            contentDescription = null,
                            tint = if (isSelected) Color.Black else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Profile TopBar Preview")
@Composable
fun ProfileTopBarPreview() {
    TopBar(currentRoute = "profile")
}