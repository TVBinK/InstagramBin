package com.baothanhbin.instagrambin.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.baothanhbin.instagrambin.R
import com.baothanhbin.instagrambin.model.Post
import com.baothanhbin.instagrambin.model.User
import com.baothanhbin.instagrambin.ui.theme.InstagramUiComposeTheme
import kotlinx.coroutines.delay



@Composable
fun HomeScreen(
    stories: List<User> = listOf(),
    posts: List<Post> = listOf()
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Stories(stories = stories)
        }
        items(posts) { post ->
            var liked by remember {
                mutableStateOf(false)
            }
            LaunchedEffect(liked) {
                if (liked) {
                    delay(2000)
                    liked = false
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 1.dp, horizontal = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .border(
                                1.dp, Brush.horizontalGradient(
                                    listOf(
                                        Color(0xffff6f00),
                                        Color(0xffffeb35),
                                        Color(0xffff6f00),
                                        Color(0xffff2b99),
                                        Color(0xffff2bd1),
                                        Color(0xffff2bd1),
                                    )
                                ), CircleShape
                            )
                            .size(33.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest
                                .Builder(context)
                                .data(post.user.profile)
                                .crossfade(400)
                                .build(),
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(30.dp),
                            contentScale = ContentScale.Crop,
                            contentDescription = null
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = post.user.name)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        liked = true
                    })
                },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest
                        .Builder(context)
                        .data(post.post)
                        .crossfade(400)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )
                AnimatedVisibility(
                    visible = liked,
                    enter = scaleIn(
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                    exit = scaleOut()
                ) {
                    Image(
                        modifier = Modifier.size(100.dp),
                        painter = painterResource(id = R.drawable.heart),
                        contentDescription = null
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val iconsModifier = Modifier.size(20.dp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        modifier = iconsModifier,
                        painter = painterResource(id = R.drawable.ic_heart),
                        contentDescription = null
                    )
                    Icon(
                        modifier = iconsModifier,
                        painter = painterResource(id = R.drawable.ic_comment),
                        contentDescription = null
                    )
                    Icon(
                        modifier = iconsModifier,
                        painter = painterResource(id = R.drawable.ic_send),
                        contentDescription = null
                    )
                }
                Icon(
                    modifier = iconsModifier,
                    painter = painterResource(id = R.drawable.ic_save),
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(text = "${post.likesCount} likes", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = post.description, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "View all ${post.commentsCount} comments",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun Stories(stories: List<User>) {
    val context = LocalContext.current
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        itemsIndexed(stories) { index, story ->
            if (index == 0) {
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .border(
                            2.dp, Brush.horizontalGradient(
                                listOf(
                                    Color(0xffff6f00),
                                    Color(0xffffeb35),
                                    Color(0xffff6f00),
                                    Color(0xffff2b99),
                                    Color(0xffff2bd1),
                                    Color(0xffff2bd1),
                                )
                            ), CircleShape
                        )
                        .size(70.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest
                            .Builder(context)
                            .data(story.profile)
                            .crossfade(400)
                            .build(),
                        modifier = Modifier
                            .clip(CircleShape)
                            .size(60.dp),
                        contentScale = ContentScale.Crop,
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = story.name, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
        }
    }
}
@Composable
@Preview(showBackground = true)
fun HomeScreenPreview() {
    InstagramUiComposeTheme {
        val stories = listOf(
            User(
                profile = "https://this-person-does-not-exist.com/img/avatar-gen11945081a5b36eebba0679f61dfbd225.jpg",
                name = "melika"
            ),
        )
        val posts = listOf(
            Post(
                user = stories[0],
                post = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTbOgDkM2In7l5CVeeJjwcdVRd9tdQ4r5wwzPkLOz9CP-Uxnx-rdtxHvXJWbIz6qUBjFHI&usqp=CAU",
                description = "As you consider all the possible ways to improve yourself and the world, you notice John Travolta seems fairly unhappy.",
                likesCount = 1000,
                commentsCount = 200
            ),
            Post(
                user = stories[1],
                post = "https://www.wwf.org.uk/sites/default/files/styles/hero_s/public/2017-01/Ashley%20cooper%20forest.jpg?h=6f8e8448&itok=o0tpKRWJ",
                description = "As you consider all the possible ways to improve yourself and the world, you notice John Travolta seems fairly unhappy.",
                likesCount = 500,
                commentsCount = 50
            ),
        )
        HomeScreen(stories = stories, posts = posts)
    }
}
