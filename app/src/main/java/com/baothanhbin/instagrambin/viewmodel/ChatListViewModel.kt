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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

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
    companion object {
        private var cachedChats: List<ChatPreview> = emptyList()
        private var lastUpdateTime: Long = 0
        private const val CACHE_DURATION = 5 * 60 * 1000 // 5 phút
    }

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val messageRepository = MessageRepository(application)
    private var chatListener: ValueEventListener? = null
    private var messageListeners = mutableMapOf<String, ValueEventListener>()

    init {
        // Clear cache khi khởi tạo để đảm bảo data mới
        clearCache()
        
        // Kiểm tra cache trước khi setup listener
        if (cachedChats.isNotEmpty() && System.currentTimeMillis() - lastUpdateTime < CACHE_DURATION) {
            _uiState.value = _uiState.value.copy(chats = cachedChats)
        }
        setupChatListener()
    }

    private fun clearCache() {
        // Clear static cache
        cachedChats = emptyList()
        lastUpdateTime = 0
        
        // Clear message repository cache
        messageRepository.clearUserCache()
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

                        // Chỉ load dữ liệu nếu có chat mới hoặc chưa có dữ liệu
                        if (hasNewChats || _uiState.value.chats.isEmpty()) {
                            _uiState.value = _uiState.value.copy(isLoading = true)
                            
                            // Load song song thông tin user và tin nhắn cho các chat mới
                            val newChatUsers = chatUsers - currentChatUserIds
                            val newChatPreviews = newChatUsers.map { userId ->
                                async {
                                    val userSnapshot = database.getReference("users")
                                        .child(userId)
                                        .get()
                                        .await()
                                    val user = userSnapshot.getValue(User::class.java)
                                    val messages = messageRepository.getMessages(userId)
                                    val lastMsg = messages.lastOrNull()
                                    ChatPreview(
                                        user = user ?: User(),
                                        lastMessage = lastMsg?.content ?: "",
                                        lastMessageTime = lastMsg?.timestamp ?: 0L
                                    )
                                }
                            }.awaitAll()

                            // Cập nhật danh sách chat, loại trùng theo user.uid
                            val allChats = (_uiState.value.chats + newChatPreviews)
                                .groupBy { it.user.uid }
                                .map { it.value.last() } // lấy phần tử mới nhất cho mỗi user

                            // Cập nhật cache
                            cachedChats = allChats
                            lastUpdateTime = System.currentTimeMillis()

                            _uiState.value = _uiState.value.copy(
                                chats = allChats,
                                isLoading = false
                            )

                            // Thiết lập listener cho tin nhắn mới
                            setupMessageListeners(newChatUsers)
                        }
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

    private fun setupMessageListeners(userIds: Set<String>) {
        val currentUser = auth.currentUser ?: return
        
        userIds.forEach { userId ->
            if (messageListeners[userId] == null) {
                val messageListener = database.getReference("chats")
                    .child(currentUser.uid)
                    .child(userId)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            viewModelScope.launch {
                                try {
                                    val messages = snapshot.children.mapNotNull { 
                                        it.getValue(Message::class.java) 
                                    }.sortedBy { it.timestamp }
                                    
                                    val lastMsg = messages.lastOrNull()
                                    if (lastMsg != null) {
                                        // Cập nhật tin nhắn mới nhất cho chat
                                        val updatedChats = _uiState.value.chats.map { chat ->
                                            if (chat.user.uid == userId) {
                                                chat.copy(
                                                    lastMessage = lastMsg.content,
                                                    lastMessageTime = lastMsg.timestamp
                                                )
                                            } else chat
                                        }
                                        _uiState.value = _uiState.value.copy(chats = updatedChats)
                                    }
                                } catch (e: Exception) {
                                    // Xử lý lỗi nếu cần
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Xử lý lỗi nếu cần
                        }
                    })
                messageListeners[userId] = messageListener
            }
        }
    }

    fun loadChats(forceRefresh: Boolean = false) {
        // Kiểm tra cache trước khi load lại
        if (!forceRefresh && cachedChats.isNotEmpty() && System.currentTimeMillis() - lastUpdateTime < CACHE_DURATION) {
            _uiState.value = _uiState.value.copy(chats = cachedChats)
            return
        }

        // Load lại nếu chưa có dữ liệu hoặc khi force refresh
        if (_uiState.value.chats.isEmpty() || forceRefresh) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            setupChatListener()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Xóa chat listener
        chatListener?.let { listener ->
            val currentUser = auth.currentUser ?: return
            database.getReference("chats")
                .child(currentUser.uid)
                .removeEventListener(listener)
        }
        
        // Xóa message listeners
        val currentUser = auth.currentUser ?: return
        messageListeners.forEach { (userId, listener) ->
            database.getReference("chats")
                .child(currentUser.uid)
                .child(userId)
                .removeEventListener(listener)
        }
        messageListeners.clear()
    }
} 