package com.baothanhbin.instagrambin.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baothanhbin.instagrambin.model.User
import com.baothanhbin.instagrambin.repository.MessageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.app.Application
import com.baothanhbin.instagrambin.model.Message
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay

// New data class for chat preview
data class ChatPreview(
    val user: User,
    val lastMessage: String,
    val lastMessageTime: Long
)

data class ChatListUiState(
    val chats: List<ChatPreview> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ChatListViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val messageRepository = MessageRepository(application)
    private var chatListener: ValueEventListener? = null

    init {
        setupChatListener()
    }

    private fun setupChatListener() {
        val currentUser = auth.currentUser ?: return
        val chatsRef = database.getReference("chats").child(currentUser.uid)

        chatListener = chatsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch {
                    try {
                        val chatUsers = mutableSetOf<String>()
                        snapshot.children.forEach { chatSnapshot ->
                            chatSnapshot.key?.let { chatUsers.add(it) }
                        }

                        // Kiểm tra xem có thay đổi về danh sách chat không
                        val currentChatUserIds = _uiState.value.chats.map { it.user.uid }.toSet()
                        val hasNewChats = chatUsers != currentChatUserIds

                        // Nếu không có chat mới và đã có dữ liệu, chỉ cập nhật tin nhắn mới nhất
                        if (!hasNewChats && _uiState.value.chats.isNotEmpty()) {
                            val updatedChats = _uiState.value.chats.map { chatPreview ->
                                val messages = messageRepository.getMessages(chatPreview.user.uid)
                                val lastMsg = messages.lastOrNull()
                                chatPreview.copy(
                                    lastMessage = lastMsg?.content ?: "",
                                    lastMessageTime = lastMsg?.timestamp ?: 0L
                                )
                            }
                            _uiState.value = _uiState.value.copy(chats = updatedChats)
                            return@launch
                        }

                        // Nếu có chat mới hoặc chưa có dữ liệu, load lại toàn bộ
                        _uiState.value = _uiState.value.copy(isLoading = true)
                        delay(500)

                        val chatPreviews = mutableListOf<ChatPreview>()
                        for (userId in chatUsers) {
                            val userSnapshot = database.getReference("users")
                                .child(userId)
                                .get()
                                .await()
                            val user = userSnapshot.getValue(User::class.java) ?: continue
                            val messages = messageRepository.getMessages(userId)
                            val lastMsg = messages.lastOrNull()
                            chatPreviews.add(
                                ChatPreview(
                                    user = user,
                                    lastMessage = lastMsg?.content ?: "",
                                    lastMessageTime = lastMsg?.timestamp ?: 0L
                                )
                            )
                        }

                        _uiState.value = _uiState.value.copy(
                            chats = chatPreviews,
                            isLoading = false
                        )
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            error = e.message,
                            isLoading = false
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load chats: ${error.message}",
                    isLoading = false
                )
            }
        })
    }

    fun loadChats() {
        // Chỉ load lại nếu chưa có dữ liệu
        if (_uiState.value.chats.isEmpty()) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            setupChatListener()
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatListener?.let { listener ->
            val currentUser = auth.currentUser ?: return
            database.getReference("chats")
                .child(currentUser.uid)
                .removeEventListener(listener)
        }
    }
} 