package com.baothanhbin.instagrambin.repository

import com.baothanhbin.instagrambin.model.Post
import com.baothanhbin.instagrambin.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class PostRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    suspend fun getPosts(): List<Post> {
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")
        val userRef = database.getReference("users").child(currentUser.uid)
        val userSnapshot = userRef.get().await()
        val user = userSnapshot.getValue(User::class.java)
        
        // Get posts from current user and users they follow
        val followingIds = user?.following?.keys?.toList() ?: emptyList()
        val userIds = followingIds + currentUser.uid
        
        val posts = mutableListOf<Post>()
        for (userId in userIds) {
            val userPostsSnapshot = database.getReference("posts")
                .orderByChild("userId")
                .equalTo(userId)
                .get()
                .await()
            
            val userPosts = userPostsSnapshot.children.mapNotNull { 
                it.getValue(Post::class.java) 
            }
            posts.addAll(userPosts)
        }

        // Sort posts by timestamp
        val sortedPosts = posts.sortedByDescending { it.timestamp }

        // Load user data for each post and add timeAgo
        return sortedPosts.map { post ->
            val postUserSnapshot = database.getReference("users")
                .child(post.userId)
                .get()
                .await()
            val postUser = postUserSnapshot.getValue(User::class.java)
            post.copy(
                user = postUser ?: User(),
                timeAgo = getTimeAgo(post.timestamp)
            )
        }
    }

    suspend fun toggleLike(postId: String, userId: String): Post {
        val postRef = database.getReference("posts").child(postId)
        val postSnapshot = postRef.get().await()
        val post = postSnapshot.getValue(Post::class.java) ?: throw Exception("Post not found")
        
        val isLiked = post.likes.containsKey(userId)
        val updatedLikes = post.likes.toMutableMap().apply {
            if (isLiked) remove(userId) else put(userId, true)
        }
        val newLikesCount = if (isLiked) post.likesCount - 1 else post.likesCount + 1
        
        postRef.child("likes").setValue(updatedLikes).await()
        postRef.child("likesCount").setValue(newLikesCount).await()
        
        // Fetch user and recalculate timeAgo
        val postUserSnapshot = database.getReference("users")
            .child(post.userId)
            .get()
            .await()
        val postUser = postUserSnapshot.getValue(User::class.java)
        return post.copy(
            likes = updatedLikes,
            likesCount = newLikesCount,
            user = postUser ?: User(),
            timeAgo = getTimeAgo(post.timestamp)
        )
    }

    private fun getTimeAgo(timestamp: Long): String {
        val currentTime = System.currentTimeMillis()
        val diffInSeconds = (currentTime - timestamp) / 1000

        return when {
            diffInSeconds < 60 -> "Vừa xong"
            diffInSeconds < 3600 -> "${diffInSeconds / 60} phút trước"
            diffInSeconds < 86400 -> "${diffInSeconds / 3600} giờ trước"
            diffInSeconds < 2592000 -> "${diffInSeconds / 86400} ngày trước"
            diffInSeconds < 31536000 -> "${diffInSeconds / 2592000} tháng trước"
            else -> "${diffInSeconds / 31536000} năm trước"
        }
    }
} 