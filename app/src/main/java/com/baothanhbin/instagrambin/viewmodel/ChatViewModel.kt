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
    val error: String? = null
)

class MessageViewModel(application: Application) : AndroidViewModel(application) {
    private val messageRepository = MessageRepository(application)
    private val _uiState = MutableStateFlow(MessageUiState())
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow()

    private var messagesListener: ValueEventListener? = null
    private var currentChatId: String? = null

    fun loadMessages(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Remove previous listener if exists
                removeMessagesListener()
                
                // Set up real-time listener
                currentChatId = userId
                val currentUser = FirebaseAuth.getInstance().currentUser ?: throw Exception("User not logged in")
                val chatRef = FirebaseDatabase.getInstance().getReference("chats")
                    .child(currentUser.uid)
                    .child(userId)

                messagesListener = chatRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val messages = snapshot.children.mapNotNull { 
                            it.getValue(Message::class.java) 
                        }.sortedBy { it.timestamp }
                        
                        _uiState.value = _uiState.value.copy(
                            messages = messages,
                            isLoading = false
                        )
                        
                        // Mark messages as read
                        viewModelScope.launch {
                            try {
                                messageRepository.markMessagesAsRead(userId)
                            } catch (e: Exception) {
                                _uiState.value = _uiState.value.copy(
                                    error = "Failed to mark messages as read: ${e.message}"
                                )
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to load messages: ${error.message}",
                            isLoading = false
                        )
                    }
                })
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load messages: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun sendMessage(receiverId: String, content: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                messageRepository.sendMessage(receiverId, content)
                // No need to update UI state here as the real-time listener will handle it
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to send message: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun sendImage(receiverId: String, imageUri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                messageRepository.sendImage(receiverId, imageUri)
                // No need to update UI state here as the real-time listener will handle it
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to send image: ${e.message}",
                    isLoading = false
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

    override fun onCleared() {
        super.onCleared()
        removeMessagesListener()
    }
} 