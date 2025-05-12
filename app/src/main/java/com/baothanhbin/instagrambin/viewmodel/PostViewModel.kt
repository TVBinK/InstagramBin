package com.baothanhbin.instagrambin.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baothanhbin.instagrambin.model.Post
import com.baothanhbin.instagrambin.service.CloudinaryService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class PostUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PostUiState())
    val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val cloudinaryService = CloudinaryService(application)

    fun uploadPost(imageUris: List<Uri>, caption: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null, isSuccess = false) }
                
                val currentUser = auth.currentUser ?: throw Exception("User not logged in")
                val postId = UUID.randomUUID().toString()
                
                // Upload images to Cloudinary
                val imageUrls = mutableListOf<String>()
                for (uri in imageUris) {
                    val imageUrl = cloudinaryService.uploadImage(uri)
                    imageUrls.add(imageUrl)
                }

                // Create post object
                val post = Post(
                    postId = postId,
                    userId = currentUser.uid,
                    imageUrl = imageUrls.first(), // For now, we'll use the first image as the main image
                    caption = caption,
                    timestamp = System.currentTimeMillis()
                )

                // Save post to Firebase Database
                database.getReference("posts")
                    .child(postId)
                    .setValue(post)
                    .await()

                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
} 