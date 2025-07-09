package com.baothanhbin.instagrambin.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baothanhbin.instagrambin.model.User
import com.baothanhbin.instagrambin.service.WebRTCService
import com.baothanhbin.instagrambin.service.PermissionService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoCallViewModel(private val context: Context) : ViewModel() {
    private val webRTCService = WebRTCService.getInstance(context)
    private val permissionService = PermissionService(context)
    private val database = FirebaseDatabase.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private var incomingCallListener: com.google.firebase.database.ValueEventListener? = null
    
    private val _isInCall = MutableStateFlow(false)
    val isInCall: StateFlow<Boolean> = _isInCall.asStateFlow()
    
    private val _currentCallUser = MutableStateFlow<User?>(null)
    val currentCallUser: StateFlow<User?> = _currentCallUser.asStateFlow()
    
    private val _incomingCall = MutableStateFlow<IncomingCall?>(null)
    val incomingCall: StateFlow<IncomingCall?> = _incomingCall.asStateFlow()
    
    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()
    
    private val _shouldPopBack = MutableStateFlow(false)
    val shouldPopBack: StateFlow<Boolean> = _shouldPopBack.asStateFlow()
    
    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()
    
    private val _isVideoMuted = MutableStateFlow(false)
    val isVideoMuted: StateFlow<Boolean> = _isVideoMuted.asStateFlow()
    
    data class IncomingCall(
        val callId: String,
        val fromUserId: String,
        val fromUser: User
    )
    
    data class PermissionState(
        val hasCameraPermission: Boolean = false,
        val hasMicrophonePermission: Boolean = false,
        val hasAllPermissions: Boolean = false
    )
    
    init {
        // Check initial permissions
        checkPermissions()
        
        // Lắng nghe cuộc gọi mới từ node calls gốc
        listenForNewCalls()
        
        // Monitor WebRTC service state để reinit listener nếu cần
        viewModelScope.launch {
            webRTCService.callState.collect { state ->
                if (state == WebRTCService.CallState.IDLE && !_isInCall.value && incomingCallListener == null) {
                    listenForNewCalls()
                }
                if ((state == WebRTCService.CallState.IDLE || state == WebRTCService.CallState.ENDED) && _isInCall.value) {
                    endCallUiFirst()
                }
            }
        }
        
        // Đăng ký callback để nhận end signal từ WebRTCService
        webRTCService.onCallEndReceived = {
            _incomingCall.value = null
            _isInCall.value = false
            _currentCallUser.value = null
            _shouldPopBack.value = true
        }
        
        // Đăng ký callback để nhận incoming call từ WebRTCService
        webRTCService.onIncomingCallReceived = { callId, fromUserId, toUserId ->
            currentUserId?.let { userId ->
                if (toUserId == userId && _incomingCall.value == null) {
                    // Lấy thông tin user để hiển thị UI
                    database.getReference("users").child(fromUserId)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                val user = snapshot.getValue(User::class.java)
                                user?.let {
                                    _incomingCall.value = IncomingCall(
                                        callId = callId,
                                        fromUserId = fromUserId,
                                        fromUser = it
                                    )
                                    // Set thông tin call cho WebRTCService để lắng nghe signaling
                                    webRTCService.setIncomingCall(callId, userId, fromUserId)
                                }
                            }
                        }
                }
            }
        }

    }
    
    fun checkPermissions() {
        val hasCamera = permissionService.hasCameraPermission()
        val hasMicrophone = permissionService.hasMicrophonePermission()
        val hasAll = permissionService.hasAllVideoCallPermissions()
        
        _permissionState.value = PermissionState(
            hasCameraPermission = hasCamera,
            hasMicrophonePermission = hasMicrophone,
            hasAllPermissions = hasAll
        )
    }
    
    fun startCall(user: User) {
        if (!permissionService.hasAllVideoCallPermissions()) {
            return
        }
        
        currentUserId?.let { userId ->
            _currentCallUser.value = user
            _isInCall.value = true
            webRTCService.startCall(userId, user.uid)
        }
    }
    
    fun acceptCall(callId: String, fromUserId: String) {
        if (!permissionService.hasAllVideoCallPermissions()) {
            return
        }
        
        currentUserId?.let { userId ->
            // Lấy thông tin người gọi từ Firebase để hiển thị UI
            database.getReference("users").child(fromUserId)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(User::class.java)
                        user?.let {
                            _currentCallUser.value = it
                            _isInCall.value = true
                            webRTCService.acceptCall(callId, userId, fromUserId)
                            _incomingCall.value = null
                        }
                    }
                }
        }
    }
    
    fun rejectCall(callId: String, fromUserId: String) {
        currentUserId?.let { userId ->
            webRTCService.rejectCall(callId, userId, fromUserId)
            _incomingCall.value = null
        }
    }
    
    fun endCall() {
        // Reset UI state first
        _isInCall.value = false
        _currentCallUser.value = null
        _incomingCall.value = null
        _shouldPopBack.value = true
        // Then call webRTCService.endCall() to send signal and cleanup
        webRTCService.endCall()
    }
    
    fun endCallUiFirst() {
        _isInCall.value = false
        _currentCallUser.value = null
        _incomingCall.value = null
        // Không cần gọi endCall() hay reset() - đã được handle bởi end signal
    }
    
    fun getWebRTCService(): WebRTCService = webRTCService
    
    fun getRequiredPermissions(): Array<String> = permissionService.getRequiredPermissions()
    
    fun toggleMic() {
        val newState = !_isMicMuted.value
        _isMicMuted.value = newState
        webRTCService.setMicEnabled(!newState)
    }
    
    fun toggleVideo() {
        val newState = !_isVideoMuted.value
        _isVideoMuted.value = newState
        webRTCService.setVideoEnabled(!newState)
    }
    
    private fun listenForNewCalls() {
        currentUserId?.let { userId ->
            
            // Hủy listener cũ nếu có
            incomingCallListener?.let { listener ->
                try {
                    database.getReference("calls").removeEventListener(listener)
                } catch (e: Exception) {

                }
            }
            // Tạo listener mới
            incomingCallListener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    // Duyệt qua tất cả các cuộc gọi đến người dùng hiện tại
                    for (childSnapshot in snapshot.children) {
                        val callData = childSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                        val callId = childSnapshot.key
                        val fromUserId = callData?.get("fromUserId") as? String
                        val toUserId = callData?.get("toUserId") as? String
                        val type = callData?.get("type") as? String
                        
                        // Kiểm tra xem có phải cuộc gọi đến mình không
                        if (callId != null && fromUserId != null && toUserId == userId && type == "offer" && _incomingCall.value == null) {
                            // Set thông tin call cho WebRTCService để lắng nghe signaling
                            webRTCService.setIncomingCall(callId, userId, fromUserId)
                        }
                    }
                }
                
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {

                }
            }
            
            database.getReference("calls")
                .orderByChild("toUserId")
                .equalTo(userId)
                .addValueEventListener(incomingCallListener!!)
        }
    }
    
    fun resetPopBackFlag() {
        _shouldPopBack.value = false
    }
    
    override fun onCleared() {
        super.onCleared()
        
        // Cleanup Firebase listeners
        incomingCallListener?.let { listener ->
            try {
                database.getReference("calls").removeEventListener(listener)
            } catch (e: Exception) {
                // Silent catch
            }
            incomingCallListener = null
        }
        
        // Clear callbacks
        webRTCService.onCallEndReceived = null
        webRTCService.onIncomingCallReceived = null
    }
    
    fun setIncomingCallFromNotification(callId: String, fromUserId: String, callerName: String?) {
        // Tạo User giả với thông tin tối thiểu
        val user = User(
            uid = fromUserId,
            fullName = callerName ?: "Người gọi"
        )
        _incomingCall.value = IncomingCall(
            callId = callId,
            fromUserId = fromUserId,
            fromUser = user
        )
    }
}