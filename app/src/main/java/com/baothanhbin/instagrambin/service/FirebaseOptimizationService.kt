package com.baothanhbin.instagrambin.service

import android.app.Application
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import com.baothanhbin.instagrambin.model.User
import com.baothanhbin.instagrambin.model.Message
import kotlinx.coroutines.tasks.await

class FirebaseOptimizationService(private val application: Application) {
    
    private val database: FirebaseDatabase = Firebase.database
    private val userCache = ConcurrentHashMap<String, User>()
    private val messageCache = ConcurrentHashMap<String, List<Message>>()
    
    // Cache TTL (Time To Live) - 5 minutes
    private val CACHE_TTL = 5 * 60 * 1000L
    private val cacheTimestamps = ConcurrentHashMap<String, Long>()
    
    companion object {
        private const val MAX_CACHE_SIZE = 100
        private const val MAX_MESSAGE_CACHE_SIZE = 50
    }
    
    init {
        // Firebase persistence is already enabled in InstagramBinApplication
        // No need to call setPersistenceEnabled() here
    }
    
    // Optimized user loading with caching
    suspend fun getUser(userId: String): User? {
        // Check cache first
        val cachedUser = userCache[userId]
        val cacheTime = cacheTimestamps[userId]
        
        if (cachedUser != null && cacheTime != null && 
            System.currentTimeMillis() - cacheTime < CACHE_TTL) {
            return cachedUser
        }
        
        // Load from Firebase
        return try {
            val snapshot = database.getReference("users").child(userId).get().await()
            val user = snapshot.getValue(User::class.java)
            
            if (user != null) {
                // Update cache
                userCache[userId] = user
                cacheTimestamps[userId] = System.currentTimeMillis()
                
                // Clean cache if too large
                if (userCache.size > MAX_CACHE_SIZE) {
                    cleanCache()
                }
            }
            
            user
        } catch (e: Exception) {
            // Return cached user if available, even if expired
            cachedUser
        }
    }
    
    // Batch load users
    suspend fun getUsers(userIds: List<String>): Map<String, User> {
        val result = mutableMapOf<String, User>()
        val usersToLoad = mutableListOf<String>()
        
        // Check cache first
        for (userId in userIds) {
            val cachedUser = userCache[userId]
            val cacheTime = cacheTimestamps[userId]
            
            if (cachedUser != null && cacheTime != null && 
                System.currentTimeMillis() - cacheTime < CACHE_TTL) {
                result[userId] = cachedUser
            } else {
                usersToLoad.add(userId)
            }
        }
        
        // Load uncached users in parallel
        if (usersToLoad.isNotEmpty()) {
            val userSnapshots = usersToLoad.map { userId ->
                database.getReference("users").child(userId).get().await()
            }
            
            usersToLoad.forEachIndexed { index, userId ->
                val user = userSnapshots[index].getValue(User::class.java)
                if (user != null) {
                    result[userId] = user
                    userCache[userId] = user
                    cacheTimestamps[userId] = System.currentTimeMillis()
                }
            }
        }
        
        return result
    }
    
    // Optimized message loading with pagination
    suspend fun getMessages(
        currentUserId: String, 
        otherUserId: String, 
        limit: Int = 20,
        lastMessageTimestamp: Long? = null
    ): List<Message> {
        val cacheKey = "${currentUserId}_${otherUserId}"
        
        // Check cache for initial load
        if (lastMessageTimestamp == null) {
            val cachedMessages = messageCache[cacheKey]
            val cacheTime = cacheTimestamps[cacheKey]
            
            if (cachedMessages != null && cacheTime != null && 
                System.currentTimeMillis() - cacheTime < CACHE_TTL) {
                return cachedMessages.takeLast(limit)
            }
        }
        
        // Load from Firebase with optimized query
        val chatRef = database.getReference("chats")
            .child(currentUserId)
            .child(otherUserId)
        
        val query = if (lastMessageTimestamp != null) {
            chatRef.orderByChild("timestamp")
                .endBefore(lastMessageTimestamp.toDouble())
                .limitToFirst(limit)
        } else {
            chatRef.orderByChild("timestamp")
                .limitToFirst(limit)
        }
        
        return try {
            val snapshot = query.get().await()
            val messages = snapshot.children.mapNotNull { childSnapshot -> 
                childSnapshot.getValue(Message::class.java) 
            }.sortedBy { message -> message.timestamp }
            
            // For pagination, we want the most recent messages, so we need to get more and take the last ones
            val allMessages = if (lastMessageTimestamp == null) {
                // For initial load, get more messages to ensure we have enough recent ones
                val fullSnapshot = chatRef.orderByChild("timestamp").get().await()
                fullSnapshot.children.mapNotNull { childSnapshot -> 
                    childSnapshot.getValue(Message::class.java) 
                }.sortedBy { message -> message.timestamp }
            } else {
                messages
            }
            
            val resultMessages = if (lastMessageTimestamp == null) {
                allMessages.takeLast(limit)
            } else {
                allMessages
            }
            
            // Cache initial load
            if (lastMessageTimestamp == null && resultMessages.isNotEmpty()) {
                messageCache[cacheKey] = allMessages
                cacheTimestamps[cacheKey] = System.currentTimeMillis()
                
                // Clean message cache if too large
                if (messageCache.size > MAX_MESSAGE_CACHE_SIZE) {
                    cleanMessageCache()
                }
            }
            
            resultMessages
        } catch (e: Exception) {
            // Return cached messages if available
            messageCache[cacheKey]?.takeLast(limit) ?: emptyList()
        }
    }
    
    // Preload user data for better performance
    suspend fun preloadUsers(userIds: List<String>) {
        val uncachedUsers = userIds.filter { userId ->
            val cacheTime = cacheTimestamps[userId]
            cacheTime == null || System.currentTimeMillis() - cacheTime >= CACHE_TTL
        }
        
        if (uncachedUsers.isNotEmpty()) {
            getUsers(uncachedUsers)
        }
    }
    
    // Clear cache
    fun clearCache() {
        userCache.clear()
        messageCache.clear()
        cacheTimestamps.clear()
    }
    
    // Force clear all cache and data
    fun forceClearAllData() {
        clearCache()
        
        // Clear Firebase Database cache
        try {
            FirebaseDatabase.getInstance().purgeOutstandingWrites()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Clean old cache entries
    private fun cleanCache() {
        val currentTime = System.currentTimeMillis()
        val expiredKeys = cacheTimestamps.entries
            .filter { currentTime - it.value >= CACHE_TTL }
            .map { it.key }
        
        expiredKeys.forEach { key ->
            userCache.remove(key)
            cacheTimestamps.remove(key)
        }
    }
    
    private fun cleanMessageCache() {
        val currentTime = System.currentTimeMillis()
        val expiredKeys = cacheTimestamps.entries
            .filter { it.key.contains("_") && currentTime - it.value >= CACHE_TTL }
            .map { it.key }
        
        expiredKeys.forEach { key ->
            messageCache.remove(key)
            cacheTimestamps.remove(key)
        }
    }
    
    // Get cache statistics
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "userCacheSize" to userCache.size,
            "messageCacheSize" to messageCache.size,
            "totalCacheEntries" to cacheTimestamps.size
        )
    }
} 