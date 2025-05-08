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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.baothanhbin.instagrambin.R

data class BottomBarItem(val icon: Int, val route: String)

@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    currentRoute: String? = null
) {
    if (currentRoute == "profile" || currentRoute == "home") {
        TopAppBar(
            title = {
                if (currentRoute == "profile") {
                    Text(
                        text = "nyuht",
                        modifier = Modifier.padding(start = 0.dp, bottom = 0.dp),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
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
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            modifier = Modifier.size(30.dp),
                            tint = Color.Black
                        )
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
                            modifier = Modifier.size(23.dp),
                            painter = painterResource(id = R.drawable.ic_send),
                            contentDescription = null
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun BottomBar(
    navController: NavController? = null,
    onNavigate: (String) -> Unit = {},
    currentRoute: String? = null
) {
    val bottomBarItems = listOf(
        BottomBarItem(R.drawable.ic_home, "home"),
        BottomBarItem(R.drawable.ic_search, "search"),
        BottomBarItem(R.drawable.ic_add, "add"),
        BottomBarItem(R.drawable.ic_profile, "profile")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
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
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
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
@Preview(showBackground = true, name = "Profile TopBar Preview")
@Composable
fun ProfileTopBarPreview() {
    TopBar(currentRoute = "profile")
}