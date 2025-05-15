package com.baothanhbin.instagrambin.repository

import com.baothanhbin.instagrambin.model.User
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val database = FirebaseDatabase.getInstance()

    suspend fun getUser(userId: String): User {
        val userSnapshot = database.getReference("users").child(userId).get().await()
        return userSnapshot.getValue(User::class.java) ?: throw Exception("User not found")
    }

    suspend fun getFriends(userId: String): List<User> {
        val userSnapshot = database.getReference("users").child(userId).get().await()
        val user = userSnapshot.getValue(User::class.java) ?: throw Exception("User not found")
        
        val followingIds = user.following?.keys?.toList() ?: emptyList()
        val friends = mutableListOf<User>()
        
        for (friendId in followingIds) {
            val friendSnapshot = database.getReference("users").child(friendId).get().await()
            friendSnapshot.getValue(User::class.java)?.let { friends.add(it) }
        }
        
        return friends
    }
} 