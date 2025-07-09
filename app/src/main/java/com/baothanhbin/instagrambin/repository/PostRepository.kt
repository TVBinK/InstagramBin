package com.baothanhbin.instagrambin.repository

import android.app.Application
import com.baothanhbin.instagrambin.model.Post
import com.baothanhbin.instagrambin.model.User
import com.baothanhbin.instagrambin.service.NotificationService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class PostRepository(private val application: Application) {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val notificationService = NotificationService(application)

    suspend fun getPosts(): List<Post> {
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")
        //Lấy thông tin người dùng hiện tại
        val userRef = database.getReference("users").child(currentUser.uid)
        val userSnapshot = userRef.get().await()
        val user = userSnapshot.getValue(User::class.java)
        
        // Lấy danh sách người dùng đang follow
        val followingIds = user?.following?.keys?.toList() ?: emptyList()
        val userIds = followingIds + currentUser.uid //bao gồm cả người dùng hiện tại

        // Lấy bài đăng từ tất cả người dùng trong danh sách userIds
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

        // Sắp xếp bài đăng theo thời gian giảm dần
        val sortedPosts = posts.sortedByDescending { it.timestamp }

        // Lấy thông tin người dùng cho mỗi bài đăng và tính toán thời gian đã đăng
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
        // Kiểm tra xem người dùng đã thích bài đăng hay chưa
        val isLiked = post.likes.containsKey(userId)
        // Cập nhật trạng thái thích/unlike bài đăng
        val updatedLikes = post.likes.toMutableMap().apply {
            if (isLiked) remove(userId) else put(userId, true)
        }
        // Cập nhật số lượng likes
        val newLikesCount = if (isLiked) post.likesCount - 1 else post.likesCount + 1
        // Cập nhật bài đăng trong cơ sở dữ liệu
        postRef.child("likes").setValue(updatedLikes).await()
        postRef.child("likesCount").setValue(newLikesCount).await()
        
        // Gửi notification nếu like (không gửi khi unlike)
        if (!isLiked) {
            notificationService.sendLikeNotification(post, userId)
        }
        
        // Cập nhật thông tin người dùng của bài đăng
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
    //ham tinh toán thời gian đã đăng
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