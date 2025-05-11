package com.baothanhbin.instagrambin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.baothanhbin.instagrambin.R
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baothanhbin.instagrambin.viewmodel.SearchViewModel
import com.baothanhbin.instagrambin.model.UserProfile


@Composable
fun SearchScreen(
    onUserClick: (String) -> Unit, // userId
    viewModel: SearchViewModel = viewModel()
) {
    var query by remember { mutableStateOf("") }
    val results by viewModel.searchResults.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                if (it.isNotBlank()) viewModel.searchUserByUsername(it)
            },
            label = { Text("Tìm kiếm username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        results.forEach { user ->
            Text(
                text = user.username,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUserClick(user.uid) }
                    .padding(8.dp),
                style = MaterialTheme.typography.bodyLarge
            )
            Divider()
        }
    }
}
//Preview
@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    SearchScreen(onUserClick = {})
}