package com.baothanhbin.instagrambin.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baothanhbin.instagrambin.model.Message
import com.baothanhbin.instagrambin.repository.MessageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MessageUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val hasMoreMessages: Boolean = true,
    val lastMessageTimestamp: Long? = null
)

class MessageViewModel(application: Application) : AndroidViewModel(application) {
    private val messageRepository = MessageRepository(application)
    private val _uiState = MutableStateFlow(MessageUiState())
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow()

    private var messagesListener: ValueEventListener? = null
    private var currentChatId: String? = null
    private var isInitialLoad = true
    
    companion object {
        private const val MAX_MESSAGES_IN_MEMORY = 200 // Limit messages in memory for performance
    }

    fun loadMessages(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Remove previous listener if exists
                removeMessagesListener()
                
                // Preload user info for better performance
                messageRepository.preloadUserInfo(userId)
                
                // Load initial messages with pagination
                val initialMessages = messageRepository.getMessages(userId, MessageRepository.MESSAGES_PER_PAGE)
                android.util.Log.d("MessageViewModel", "Initial load: loaded ${initialMessages.size} messages for user $userId")
                
                val lastTimestamp = if (initialMessages.isNotEmpty()) {
                    initialMessages.first().timestamp
                } else null
                
                _uiState.value = _uiState.value.copy(
                    messages = initialMessages,
                    isLoading = false,
                    hasMoreMessages = initialMessages.size >= MessageRepository.MESSAGES_PER_PAGE,
                    lastMessageTimestamp = lastTimestamp
                )
                
                // Set up real-time listener for new messages only
                setupRealtimeListener(userId)
                
                // Mark messages as read
                messageRepository.markMessagesAsRead(userId)
                
                isInitialLoad = false
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load messages: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    // Load more messages (pagination)
    fun loadMoreMessages() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasMoreMessages || currentState.lastMessageTimestamp == null) {
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = currentState.copy(isLoadingMore = true)
                
                // Get current chat user ID
                val chatUserId = currentChatId ?: return@launch
                
                val moreMessages = messageRepository.getMessages(
                    chatUserId,
                    MessageRepository.MESSAGES_PER_PAGE,
                    currentState.lastMessageTimestamp
                )
                
                if (moreMessages.isNotEmpty()) {
                    // Get existing message IDs to prevent duplicates
                    val existingMessageIds = currentState.messages.map { it.messageId }.toSet()
                    
                    // Filter out duplicate messages
                    val uniqueMoreMessages = moreMessages.filter { message ->
                        !existingMessageIds.contains(message.messageId)
                    }
                    
                    if (uniqueMoreMessages.isNotEmpty()) {
                        var allMessages = uniqueMoreMessages + currentState.messages
                        
                        // Trim messages if too many in memory for performance
                        if (allMessages.size > MAX_MESSAGES_IN_MEMORY) {
                            allMessages = allMessages.takeLast(MAX_MESSAGES_IN_MEMORY)
                            android.util.Log.d("MessageViewModel", "Trimmed pagination messages to ${MAX_MESSAGES_IN_MEMORY} for performance")
                        }
                        
                        val newLastTimestamp = uniqueMoreMessages.minOf { it.timestamp }
                        
                        _uiState.value = currentState.copy(
                            messages = allMessages,
                            isLoadingMore = false,
                            hasMoreMessages = moreMessages.size >= MessageRepository.MESSAGES_PER_PAGE,
                            lastMessageTimestamp = newLastTimestamp
                        )
                    } else {
                        _uiState.value = currentState.copy(
                            isLoadingMore = false,
                            hasMoreMessages = false
                        )
                    }
                } else {
                    _uiState.value = currentState.copy(
                        isLoadingMore = false,
                        hasMoreMessages = false
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    error = "Failed to load more messages: ${e.message}",
                    isLoadingMore = false
                )
            }
        }
    }

    private fun setupRealtimeListener(userId: String) {
        currentChatId = userId
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        
        val chatRef = FirebaseDatabase.getInstance().getReference("chats")
            .child(currentUser.uid)
            .child(userId)

        messagesListener = chatRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isInitialLoad) return // Skip during initial load
                
                viewModelScope.launch {
                    try {
                        val currentState = _uiState.value
                        val latestTimestamp = currentState.lastMessageTimestamp ?: 0L
                        
                        // Only load new messages since last timestamp
                        val newMessages = messageRepository.getLatestMessages(userId, latestTimestamp)
                        
                        if (newMessages.isNotEmpty()) {
                            android.util.Log.d("MessageViewModel", "Realtime: received ${newMessages.size} new messages")
                            
                            // Get existing message IDs to prevent duplicates
                            val existingMessageIds = currentState.messages.map { it.messageId }.toSet()
                            
                            // Filter out duplicate messages
                            val uniqueNewMessages = newMessages.filter { newMessage ->
                                !existingMessageIds.contains(newMessage.messageId)
                            }
                            
                            android.util.Log.d("MessageViewModel", "Realtime: ${uniqueNewMessages.size} unique messages after filtering duplicates")
                            
                            if (uniqueNewMessages.isNotEmpty()) {
                                var updatedMessages = currentState.messages + uniqueNewMessages
                                
                                // Trim messages if too many in memory for performance
                                if (updatedMessages.size > MAX_MESSAGES_IN_MEMORY) {
                                    updatedMessages = updatedMessages.takeLast(MAX_MESSAGES_IN_MEMORY)
                                    android.util.Log.d("MessageViewModel", "Trimmed messages to ${MAX_MESSAGES_IN_MEMORY} for performance")
                                }
                                
                                val newLastTimestamp = uniqueNewMessages.maxOf { it.timestamp }
                                
                                android.util.Log.d("MessageViewModel", "Realtime: updating UI with ${updatedMessages.size} total messages")
                                
                                _uiState.value = currentState.copy(
                                    messages = updatedMessages,
                                    lastMessageTimestamp = newLastTimestamp
                                )
                                
                                // Mark new messages as read
                                messageRepository.markMessagesAsRead(userId)
                            }
                        } else {
                            android.util.Log.d("MessageViewModel", "Realtime: no new messages received")
                        }
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to update messages: ${e.message}"
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to listen for messages: ${error.message}"
                )
            }
        })
    }

    fun sendMessage(receiverId: String, content: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(error = null)
                messageRepository.sendMessage(receiverId, content)
                // Real-time listener will handle UI update
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to send message: ${e.message}"
                )
            }
        }
    }

    fun sendImage(receiverId: String, imageUri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(error = null)
                messageRepository.sendImage(receiverId, imageUri)
                // Real-time listener will handle UI update
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to send image: ${e.message}"
                )
            }
        }
    }

    private fun removeMessagesListener() {
        currentChatId?.let { chatId ->
            val currentUser = FirebaseAuth.getInstance().currentUser ?: return
            val chatRef = FirebaseDatabase.getInstance().getReference("chats")
                .child(currentUser.uid)
                .child(chatId)
            
            messagesListener?.let { listener ->
                chatRef.removeEventListener(listener)
                messagesListener = null
            }
        }
    }

    // Clear error state
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    // Utility function to remove duplicates from message list
    private fun removeDuplicateMessages(messages: List<Message>): List<Message> {
        return messages.distinctBy { it.messageId }
    }
    
    // Clean up any existing duplicate messages in current state
    fun cleanupDuplicates() {
        val currentState = _uiState.value
        val cleanedMessages = removeDuplicateMessages(currentState.messages)
        
        if (cleanedMessages.size != currentState.messages.size) {
            android.util.Log.d("MessageViewModel", "Cleaned up ${currentState.messages.size - cleanedMessages.size} duplicate messages")
            _uiState.value = currentState.copy(messages = cleanedMessages)
        }
    }

    // Refresh messages
    fun refreshMessages() {
        currentChatId?.let { userId ->
            android.util.Log.d("MessageViewModel", "Refreshing messages for user: $userId")
            // Reset state completely before reload
            _uiState.value = MessageUiState()
            isInitialLoad = true
            // Remove current listener to prevent conflicts
            removeMessagesListener()
            // Clear repository cache to ensure fresh data
            messageRepository.clearUserCache()
            loadMessages(userId)
        }
    }
    
    // Force cleanup duplicates and trim messages (can be called from UI)
    fun forceCleanup() {
        val currentState = _uiState.value
        var cleanedMessages = removeDuplicateMessages(currentState.messages)
        
        // Also trim if too many messages
        if (cleanedMessages.size > MAX_MESSAGES_IN_MEMORY) {
            cleanedMessages = cleanedMessages.takeLast(MAX_MESSAGES_IN_MEMORY)
        }
        
        if (cleanedMessages.size != currentState.messages.size) {
            android.util.Log.d("MessageViewModel", "Force cleanup: ${currentState.messages.size} -> ${cleanedMessages.size} messages")
            _uiState.value = currentState.copy(messages = cleanedMessages)
        }
    }

    override fun onCleared() {
        super.onCleared()
        removeMessagesListener()
        messageRepository.clearUserCache()
    }
} 