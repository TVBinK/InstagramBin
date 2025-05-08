package com.baothanhbin.instagrambin.model

import androidx.compose.ui.graphics.painter.Painter

data class StoryHighlights(
    val image : Painter,
    val title : String,
    val isNewStory: Boolean = false
)
