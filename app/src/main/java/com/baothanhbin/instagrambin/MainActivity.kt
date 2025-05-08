package com.baothanhbin.instagrambin

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.baothanhbin.instagrambin.ui.theme.InstagramUiComposeTheme
import com.baothanhbin.instagrambin.model.User
import com.baothanhbin.instagrambin.model.Post
import com.baothanhbin.instagrambin.ui.screen.BottomBar
import com.baothanhbin.instagrambin.ui.screen.HomeScreen
import com.baothanhbin.instagrambin.ui.screen.TopBar
import com.baothanhbin.instagrambin.screen.ProfileScreen
import com.baothanhbin.instagrambin.screen.SearchScreen
import com.baothanhbin.instagrambin.screen.AddScreen
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.baothanhbin.instagrambin.screen.PostScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstagramUiComposeTheme {
                val systemUiController = rememberSystemUiController()
                SideEffect {
                    systemUiController.setStatusBarColor(
                        color = Color.White,
                        darkIcons = true
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    // Lắng nghe sự thay đổi của route hiện tại
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    val hideBarsRoutes = listOf("add", "post_screen/{imageUris}")
                    Scaffold(
                        topBar = {
                            if (currentRoute !in hideBarsRoutes && currentRoute != "search") {
                                TopBar(currentRoute = currentRoute)
                            }
                        },
                        bottomBar = {
                            if (currentRoute !in hideBarsRoutes) {
                                BottomBar(
                                    navController = navController,
                                    currentRoute = currentRoute
                                )
                            }
                        }
                    ) { paddings ->
                        NavHost(
                            navController = navController,
                            startDestination = "home"
                        ) {
                            composable("home") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(paddings)
                                ) {
                                    val stories = listOf(
                                        User(
                                            profile = "https://this-person-does-not-exist.com/img/avatar-gen4f59c3a3f9487457d506860bb75e3247.jpg",
                                            name = "sara"
                                        ),
                                        User(
                                            profile = "https://this-person-does-not-exist.com/img/avatar-gen116a503718103ee85aa48038cc85d079.jpg",
                                            name = "marta"
                                        ),
                                        User(
                                            profile = "https://this-person-does-not-exist.com/img/avatar-gen11670e8d5aee2f5eacc036906962f823.jpg",
                                            name = "niki"
                                        ),
                                        User(
                                            profile = "https://this-person-does-not-exist.com/img/avatar-gena157df185052c72cc24fec571a77cdf0.jpg",
                                            name = "john"
                                        ),
                                    )
                                    val posts = listOf(
                                        Post(
                                            user = stories[0],
                                            post = "https://www.wwf.org.uk/sites/default/files/styles/hero_s/public/2017-01/Ashley%20cooper%20forest.jpg?h=6f8e8448&itok=o0tpKRWJ",
                                            description = "As you consider all the possible ways to improve yourself and the world, you notice John Travolta seems fairly unhappy.",
                                            likesCount = (100..10000).random(),
                                            commentsCount = (100..10000).random(),
                                        ),
                                        Post(
                                            user = stories[1],
                                            post = "https://www.iucn.org/sites/default/files/styles/what_we_do_large/public/images-themes/biodiversity-shutterstock_1477256246.jpg.webp?itok=4i9JdtFu",
                                            description = "As you consider all the possible ways to improve yourself and the world, you notice John Travolta seems fairly unhappy.",
                                            likesCount = (100..10000).random(),
                                            commentsCount = (100..10000).random(),
                                        ),
                                        Post(
                                            user = stories[2],
                                            post = "https://www.naturebasedsolutionsinitiative.org/wp-content/uploads/2022/11/chuttersnap-MpxAiNDevjU-unsplash-1-aspect-ratio-1024-768.jpg",
                                            description = "As you consider all the possible ways to improve yourself and the world, you notice John Travolta seems fairly unhappy.",
                                            likesCount = (100..10000).random(),
                                            commentsCount = (100..10000).random(),
                                        ),
                                        Post(
                                            user = stories[3],
                                            post = "https://cdn-blob.austria.info/cms-uploads-prod/default/0002/92/thumb_191674_default_teaser.jpeg?cachebuster=1682774670",
                                            description = "As you consider all the possible ways to improve yourself and the world, you notice John Travolta seems fairly unhappy.",
                                            likesCount = (100..10000).random(),
                                            commentsCount = (100..10000).random(),
                                        ),
                                    )
                                    HomeScreen(stories = stories, posts = posts)
                                }
                            }
                            composable("search") {
                                SearchScreen(paddingValues = paddings)
                            }
                            composable("add") {
                                AddScreen(paddingValues = paddings, navController = navController)
                            }
                            composable("post_screen/{imageUris}") { backStackEntry ->
                                val imageUrisString = backStackEntry.arguments?.getString("imageUris")
                                val imageUris = imageUrisString?.split(",")?.mapNotNull {
                                    if (it.isNotBlank()) android.net.Uri.parse(Uri.decode(it)) else null
                                } ?: emptyList()
                                PostScreen(
                                    navController = navController,
                                    imageUris = imageUris
                                )
                            }
                            composable("profile") {
                                ProfileScreen(paddingValues = paddings)
                            }
                        }
                    }
                }
            }
        }
    }
}