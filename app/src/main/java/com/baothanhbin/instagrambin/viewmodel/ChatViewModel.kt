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
                    val allMessages = moreMessages + currentState.messages
                    val newLastTimestamp = moreMessages.first().timestamp
                    
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
                            val updatedMessages = currentState.messages + newMessages
                            val newLastTimestamp = newMessages.last().timestamp
                            
                            _uiState.value = currentState.copy(
                                messages = updatedMessages,
                                lastMessageTimestamp = newLastTimestamp
                            )
                            
                            // Mark new messages as read
                            messageRepository.markMessagesAsRead(userId)
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

    // Refresh messages
    fun refreshMessages() {
        currentChatId?.let { userId ->
            isInitialLoad = true
            loadMessages(userId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        removeMessagesListener()
        messageRepository.clearUserCache()
    }
} 