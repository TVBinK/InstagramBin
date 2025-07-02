package com.baothanhbin.instagrambin.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class WebRTCService private constructor(private val context: Context) {
    private val TAG = "WebRTCService"
    
    private val database = FirebaseDatabase.getInstance()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val isDisposed = AtomicBoolean(false)
    private val isResetting = AtomicBoolean(false)
    private var signalingListener: com.google.firebase.database.ChildEventListener? = null
    private var incomingCallListener: com.google.firebase.database.ValueEventListener? = null
    
    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()
    
    private val _isIncomingCall = MutableStateFlow(false)
    val isIncomingCall: StateFlow<Boolean> = _isIncomingCall.asStateFlow()
    
    // Callback để thông báo về ViewModel khi nhận end signal
    var onCallEndReceived: (() -> Unit)? = null
    // Callback để thông báo về ViewModel khi nhận offer signal
    var onIncomingCallReceived: ((callId: String, fromUserId: String, toUserId: String) -> Unit)? = null
    
    private var currentCallId: String? = null
    private var currentUserId: String? = null
    private var remoteUserId: String? = null
    
    // WebRTC fields
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var eglBase: EglBase? = null
    private var peerConnection: PeerConnection? = null
    private var remoteVideoTrack: VideoTrack? = null
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )
    
    // Synchronization object for WebRTC operations
    private val webRTCLock = Object()
    
    private val _remoteVideoTrackFlow = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrackFlow: StateFlow<VideoTrack?> = _remoteVideoTrackFlow.asStateFlow()
    
    private val _localVideoTrackFlow = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrackFlow: StateFlow<VideoTrack?> = _localVideoTrackFlow.asStateFlow()
    
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    
    enum class CallState {
        IDLE,
        CALLING,
        RINGING,
        CONNECTED,
        ENDED,
        ERROR
    }
    
    companion object {
        @Volatile
        private var INSTANCE: WebRTCService? = null
        
        fun getInstance(context: Context): WebRTCService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebRTCService(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        fun destroyInstance() {
            INSTANCE?.dispose()
            INSTANCE = null
        }
    }
    
    init {
        initializeWebRTC(context)
    }
    
    private fun initializeWebRTC(context: Context) {
        if (isDisposed.get()) return
        
        synchronized(webRTCLock) {
            try {
                // Khởi tạo EglBase sử dụng helper
                eglBase = WebRTCHelper.createEglBase()
                if (eglBase == null) {
                    Log.e(TAG, "Failed to create EGL base")
                    setCallState(CallState.ERROR)
                    return
                }
                
                // Khởi tạo PeerConnectionFactory sử dụng helper
                peerConnectionFactory = WebRTCHelper.initializePeerConnectionFactory(context, eglBase)
                if (peerConnectionFactory == null) {
                    Log.e(TAG, "Failed to initialize PeerConnectionFactory")
                    setCallState(CallState.ERROR)
                    return
                }
                
                // Tạo local video track
                createLocalVideoTrack(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing WebRTC", e)
                setCallState(CallState.ERROR)
            }
        }
    }
    
    private fun createLocalVideoTrack(context: Context) {
        if (isDisposed.get()) {
            android.util.Log.w("WebRTCService", "Cannot create local video track - service is disposed")
            return
        }
        
        synchronized(webRTCLock) {
            android.util.Log.d("WebRTCService", "Creating local video track... current callState: ${callState.value}")
            android.util.Log.d("WebRTCService", "Current WebRTC components - eglBase: ${eglBase != null}, factory: ${peerConnectionFactory != null}")
            
            // Clean up existing track if any
            if (localVideoTrack != null) {
                android.util.Log.d("WebRTCService", "Disposing existing local video track: ${localVideoTrack?.id()}")
                try {
                    localVideoTrack?.dispose()
                } catch (e: Exception) {
                    android.util.Log.w("WebRTCService", "Error disposing existing local video track", e)
                }
                localVideoTrack = null
                _localVideoTrackFlow.value = null // Clear flow
                android.util.Log.d("WebRTCService", "Existing local video track disposed successfully")
            }
            
            // Clean up existing video source
            if (localVideoSource != null) {
                android.util.Log.d("WebRTCService", "Disposing existing local video source")
                try {
                    localVideoSource?.dispose()
                } catch (e: Exception) {
                    android.util.Log.w("WebRTCService", "Error disposing existing local video source", e)
                }
                localVideoSource = null
            }
            
            // Clean up existing capturer
            if (videoCapturer != null) {
                android.util.Log.d("WebRTCService", "Stopping and disposing existing video capturer")
                try {
                    WebRTCHelper.stopVideoCapture(videoCapturer)
                    WebRTCHelper.disposeVideoCapturer(videoCapturer)
                } catch (e: Exception) {
                    android.util.Log.w("WebRTCService", "Error disposing existing video capturer", e)
                }
                videoCapturer = null
            }
            
            try {
                // Tạo camera capturer sử dụng helper
                videoCapturer = WebRTCHelper.createCameraCapturer(context)
                if (videoCapturer == null) {
                    android.util.Log.e("WebRTCService", "Cannot create video capturer")
                    return
                }
                android.util.Log.d("WebRTCService", "Video capturer created successfully")
                
                // Tạo video source sử dụng helper
                localVideoSource = WebRTCHelper.createVideoSource(peerConnectionFactory)
                if (localVideoSource == null) {
                    android.util.Log.e("WebRTCService", "Cannot create video source")
                    return
                }
                android.util.Log.d("WebRTCService", "Video source created successfully")
                
                // Khởi tạo video capturer sử dụng helper
                if (!WebRTCHelper.initializeVideoCapturer(videoCapturer, eglBase, context, localVideoSource)) {
                    android.util.Log.e("WebRTCService", "Failed to initialize video capturer")
                    return
                }
                android.util.Log.d("WebRTCService", "Video capturer initialized")

                // Bắt đầu capture sử dụng helper
                if (!WebRTCHelper.startVideoCapture(videoCapturer)) {
                    android.util.Log.e("WebRTCService", "Failed to start video capture")
                } else {
                    android.util.Log.d("WebRTCService", "Video capturer started successfully")
                }
                
                // Tạo video track sử dụng helper
                localVideoTrack = WebRTCHelper.createVideoTrack(peerConnectionFactory, localVideoSource)
                if (localVideoTrack != null) {
                    android.util.Log.d("WebRTCService", "Local video track created successfully: id=${localVideoTrack?.id()}, enabled=${localVideoTrack?.enabled()}")
                    // Update flow để UI cập nhật ngay lập tức
                    _localVideoTrackFlow.value = localVideoTrack
                    android.util.Log.d("WebRTCService", "Local video track flow updated successfully")
                    ensureLocalTrackAdded()
                } else {
                    android.util.Log.e("WebRTCService", "Cannot create local video track")
                    _localVideoTrackFlow.value = null
                    android.util.Log.e("WebRTCService", "Local video track flow cleared due to creation failure")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating local video track", e)
            }
        }
    }
    
    private fun createCameraCapturer(context: Context): VideoCapturer? {
        return WebRTCHelper.createCameraCapturer(context)
    }
    
    fun getLocalVideoTrack(): VideoTrack? {
        return synchronized(webRTCLock) {
            try {
                if (isDisposed.get()) {
                    Log.w(TAG, "Service is disposed, cannot get local video track")
                    return@synchronized null
                }
                localVideoTrack
            } catch (e: Exception) {
                Log.e(TAG, "Error getting local video track", e)
                null
            }
        }
    }
    
    fun getEglBase(): EglBase? {
        return synchronized(webRTCLock) {
            try {
                if (isDisposed.get()) {
                    Log.w(TAG, "Service is disposed, cannot get EGL base")
                    return@synchronized null
                }
                if (eglBase != null && WebRTCHelper.isEglContextValid(eglBase)) {
                    eglBase
                } else {
                    Log.w(TAG, "EGL base is null or context is invalid")
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error getting EGL base", e)
                null
            }
        }
    }
    
    fun isEglContextValid(): Boolean {
        return synchronized(webRTCLock) {
            try {
                if (isDisposed.get()) {
                    Log.w(TAG, "Service is disposed, EGL context is invalid")
                    return@synchronized false
                }
                WebRTCHelper.isEglContextValid(eglBase)
            } catch (e: Exception) {
                Log.w(TAG, "Error checking EGL context validity", e)
                false
            }
        }
    }
    
    fun startCall(userId: String, remoteUserId: String) {
        if (isDisposed.get() || isResetting.get()) {
            Log.w(TAG, "Cannot start call - service is disposed or resetting")
            return
        }
        
        // IMPROVED FIX: Clear previous call info if any, but preserve local video track
        if (currentCallId != null) {
            // Chỉ clear call info, không dispose local video track
            currentCallId = null
            currentUserId = null
            this.remoteUserId = null
            // Clear remote video track chỉ
            remoteVideoTrack = null
            _remoteVideoTrackFlow.value = null
        }
        
        this.currentUserId = userId
        this.remoteUserId = remoteUserId
        currentCallId = generateCallId()
        
        // Ensure WebRTC is initialized
        if (eglBase == null || peerConnectionFactory == null) {
            initializeWebRTC(context)
        }
        
        // Chỉ tạo local video track mới nếu chưa có hoặc bị dispose
        if (localVideoTrack == null) {
            createLocalVideoTrack(context)
            
            // Wait for track creation with retries
            var retries = 0
            while (localVideoTrack == null && retries < 10) {
                Thread.sleep(100) // Wait 100ms
                retries++
            }
        }
        
        if (localVideoTrack != null) {
            // Ensure flow is updated
            _localVideoTrackFlow.value = localVideoTrack
        }
        
        createPeerConnection()
        ensureLocalTrackAdded()
        
        // Tạo offer SDP
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        sendOfferSdp(desc)
                    }
                    override fun onSetFailure(p0: String?) {}
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, desc)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, MediaConstraints())
        
        // Gửi call offer và bắt đầu listen signaling
        sendCallOffer(currentCallId!!, userId, remoteUserId)
        listenSignaling()
        
        // Set state to CALLING để UI hiển thị outgoing call
        setCallState(CallState.CALLING)
    }
    
    fun acceptCall(callId: String, userId: String, remoteUserId: String) {
        android.util.Log.d("WebRTCService", "BEFORE acceptCall - currentCallId: $currentCallId")
        this.currentCallId = callId
        this.currentUserId = userId
        this.remoteUserId = remoteUserId
        
        android.util.Log.d("WebRTCService", "Accepting call - callId: $callId, callee: $userId, caller: $remoteUserId")
        
        // Ensure WebRTC is initialized before creating track
        if (eglBase == null || peerConnectionFactory == null) {
            android.util.Log.d("WebRTCService", "WebRTC not initialized for accept call, initializing now")
            initializeWebRTC(context)
        }
        
        // IMPROVED FIX: Reuse existing local video track or create new one only if needed
        if (localVideoTrack == null) {
            android.util.Log.d("WebRTCService", "Creating new local video track for callee on accept")
            createLocalVideoTrack(context)
            
            // Wait for track creation with retries (similar to startCall)
            var retries = 0
            while (localVideoTrack == null && retries < 10) {
                android.util.Log.d("WebRTCService", "Waiting for callee local video track creation, retry: $retries")
                Thread.sleep(100) // Wait 100ms
                retries++
            }
        } else {
            android.util.Log.d("WebRTCService", "Reusing existing local video track for callee: ${localVideoTrack?.id()}")
        }
        
        if (localVideoTrack != null) {
            android.util.Log.d("WebRTCService", "Callee local video track ready: id=${localVideoTrack?.id()}, enabled=${localVideoTrack?.enabled()}")
            // Ensure flow is updated
            _localVideoTrackFlow.value = localVideoTrack
        } else {
            android.util.Log.e("WebRTCService", "Failed to create/get callee local video track")
        }
        
        // Add camera track vào PeerConnection đã có
        ensureLocalTrackAdded()
        
        // Bây giờ tạo answer SDP (remote description đã được set trước đó)
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(answerDesc: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        android.util.Log.d("WebRTCService", "Answer SDP created and set successfully")
                        sendAnswerSdp(answerDesc)
                        // Gửi answer signal
                        currentUserId?.let { userId ->
                            remoteUserId?.let { remoteUserId ->
                                sendCallAnswer(currentCallId!!, userId, remoteUserId)
                            }
                        }
                    }
                    override fun onSetFailure(p0: String?) {
                        android.util.Log.e("WebRTCService", "Failed to set local answer SDP: $p0")
                    }
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, answerDesc)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {
                android.util.Log.e("WebRTCService", "Failed to create answer SDP: $p0")
            }
            override fun onSetFailure(p0: String?) {}
        }, MediaConstraints())
        
        // Set state to CONNECTED để vào màn hình cuộc gọi
        setCallState(CallState.CONNECTED)
    }
    
    fun rejectCall(callId: String, userId: String, remoteUserId: String) {
        sendCallReject(callId, userId, remoteUserId)
        setCallState(CallState.ENDED)
    }
    
    fun endCallAsync() {
        CoroutineScope(Dispatchers.IO).launch {
            endCall()
        }
    }
    
    fun endCall() {
        android.util.Log.d("WebRTCService", "endCall() called - callId: $currentCallId, userId: $currentUserId, remoteUserId: $remoteUserId")
        setCallState(CallState.ENDED)
        
        if (currentCallId == null || currentUserId == null || remoteUserId == null) {
            android.util.Log.w("WebRTCService", "Cannot send end signal - missing call info: callId=$currentCallId, userId=$currentUserId, remoteUserId=$remoteUserId")
        } else {
            android.util.Log.d("WebRTCService", "Sending end call signal from $currentUserId to $remoteUserId for call $currentCallId")
            sendCallEnd(currentCallId!!, currentUserId!!, remoteUserId!!)
        }
        
        // Delay reset để đảm bảo end signal được gửi và nhận, nhưng giữ service có thể tái sử dụng
        CoroutineScope(Dispatchers.IO).launch {
            delay(1500) // 1.5 second delay để đảm bảo signal được xử lý
            android.util.Log.d("WebRTCService", "Resetting after delay to allow reuse")
            reset()
        }
    }
    
    fun disposeAsync() {
        CoroutineScope(Dispatchers.IO).launch {
            dispose()
        }
    }
    
    private fun generateCallId(): String {
        return UUID.randomUUID().toString()
    }
    
    private fun sendCallOffer(callId: String, fromUserId: String, toUserId: String) {
        // Tạo node gốc để lưu thông tin call cơ bản
        val callData = mapOf(
            "callId" to callId,
            "fromUserId" to fromUserId,
            "toUserId" to toUserId,
            "type" to "offer",
            "timestamp" to System.currentTimeMillis()
        )
        database.getReference("calls").child(callId).setValue(callData)
        
        // Gửi offer signal vào signaling
        val offerSignal = mapOf(
            "type" to "offer",
            "fromUserId" to fromUserId,
            "toUserId" to toUserId,
            "timestamp" to System.currentTimeMillis()
        )
        database.getReference("calls").child(callId).child("signaling").push().setValue(offerSignal)
            .addOnSuccessListener {
                Log.d(TAG, "Call offer signal sent successfully")
                // Sau khi gửi offer, gửi FCM type 'call' tới toUserId
                sendCallNotificationToCallee(fromUserId, toUserId)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send call offer signal", e)
                setCallState(CallState.ERROR)
            }
    }
    
    private fun sendCallNotificationToCallee(fromUserId: String, toUserId: String) {
        // Lấy username của fromUserId từ database
        database.getReference("users").child(fromUserId).get().addOnSuccessListener { snapshot ->
            val username = snapshot.child("username").getValue(String::class.java) ?: "Người gọi"
            // Lấy FCM token của toUserId
            database.getReference("users").child(toUserId).child("fcmToken").get().addOnSuccessListener { tokenSnap ->
                val fcmToken = tokenSnap.getValue(String::class.java)
                if (!fcmToken.isNullOrBlank()) {
                    // Gửi FCM type 'call'
                    val notification = mapOf(
                        "to" to fcmToken,
                        "data" to mapOf(
                            "type" to "call",
                            "callerId" to fromUserId,
                            "callerName" to username
                        )
                    )
                    // Gửi qua FCM HTTP API (gọi NotificationService hoặc tự gửi HTTP request)
                    NotificationService.sendRawFCMNotification(context, notification)
                }
            }
        }
    }
    
    private fun sendCallAnswer(callId: String, fromUserId: String, toUserId: String) {
        // Update root node type
        database.getReference("calls").child(callId).child("type").setValue("answer")
        
        val answerSignal = mapOf(
            "type" to "answer",
            "fromUserId" to fromUserId,
            "toUserId" to toUserId,
            "timestamp" to System.currentTimeMillis()
        )
        database.getReference("calls").child(callId).child("signaling").push().setValue(answerSignal)
            .addOnSuccessListener {
                Log.d(TAG, "Call answer signal sent successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send call answer signal", e)
            }
    }
    
    private fun sendCallReject(callId: String, fromUserId: String, toUserId: String) {
        // Update root node type
        database.getReference("calls").child(callId).child("type").setValue("reject")
        
        val rejectSignal = mapOf(
            "type" to "reject",
            "fromUserId" to fromUserId,
            "toUserId" to toUserId,
            "timestamp" to System.currentTimeMillis()
        )
        database.getReference("calls").child(callId).child("signaling").push().setValue(rejectSignal)
            .addOnSuccessListener {
                Log.d(TAG, "Call reject signal sent successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send call reject signal", e)
            }
    }
    
    private fun sendCallEnd(callId: String, fromUserId: String, toUserId: String) {
        android.util.Log.d("WebRTCService", "sendCallEnd: callId=$callId, from=$fromUserId, to=$toUserId")
        
        // Update root node type first
        database.getReference("calls").child(callId).child("type").setValue("end")
            .addOnSuccessListener {
                android.util.Log.d("WebRTCService", "Call root node type updated to 'end' successfully")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("WebRTCService", "Failed to update call root node type to 'end'", e)
            }
        
        val endSignal = mapOf(
            "type" to "end",
            "fromUserId" to fromUserId,
            "toUserId" to toUserId,
            "timestamp" to System.currentTimeMillis()
        )
        database.getReference("calls").child(callId).child("signaling").push().setValue(endSignal)
            .addOnSuccessListener {
                Log.d("WebRTCService", "Call end signal sent successfully to Firebase")
            }
            .addOnFailureListener { e ->
                Log.e("WebRTCService", "Failed to send call end signal", e)
            }
    }
    
    private fun createPeerConnection() {
        if (isDisposed.get() || peerConnection != null) return
        
        synchronized(webRTCLock) {
            try {
                // DEBUG: Log local video track state before creating PeerConnection
                android.util.Log.d("WebRTCService", "BEFORE createPeerConnection - localVideoTrack: ${localVideoTrack?.id()}, flow: ${_localVideoTrackFlow.value?.id()}")
                
                val observer = object : PeerConnection.Observer {
                    override fun onIceCandidate(candidate: IceCandidate) {
                        if (!isDisposed.get()) {
                            sendIceCandidate(candidate)
                        }
                    }
                    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
                    override fun onAddStream(p0: MediaStream?) {}
                    override fun onDataChannel(p0: DataChannel?) {}
                    override fun onIceConnectionReceivingChange(p0: Boolean) {}
                    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
                    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
                    override fun onRemoveStream(p0: MediaStream?) {}
                    override fun onRenegotiationNeeded() {}
                    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
                    override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                        if (isDisposed.get()) return
                        try {
                            val track = receiver?.track()
                            android.util.Log.d("WebRTCService", "onAddTrack called - track type: ${track?.kind()}, id: ${track?.id()}")
                            
                            if (track is VideoTrack) {
                                android.util.Log.d("WebRTCService", "Remote VideoTrack received - id: ${track.id()}, enabled: ${track.enabled()}")
                                synchronized(webRTCLock) {
                                    if (!isDisposed.get()) {
                                        // Clear previous remote video track if any
                                        val previousTrack = remoteVideoTrack
                                        if (previousTrack != null && previousTrack != track) {
                                            android.util.Log.d("WebRTCService", "Replacing previous remote video track: ${previousTrack.id()}")
                                        }
                                        
                                        remoteVideoTrack = track
                                        _remoteVideoTrackFlow.value = track
                                        android.util.Log.d("WebRTCService", "Remote video track updated successfully in flow")
                                    }
                                }
                            } else {
                                android.util.Log.d("WebRTCService", "onAddTrack: not a VideoTrack, type=${track?.kind()} id=${track?.id()}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in onAddTrack", e)
                        }
                    }
                }
                
                // Tạo PeerConnection sử dụng helper
                peerConnection = WebRTCHelper.createPeerConnection(peerConnectionFactory, iceServers, observer)
                if (peerConnection == null) {
                    Log.e(TAG, "Failed to create PeerConnection")
                    setCallState(CallState.ERROR)
                    return
                }
                
                // DEBUG: Log local video track state after creating PeerConnection
                android.util.Log.d("WebRTCService", "AFTER createPeerConnection - localVideoTrack: ${localVideoTrack?.id()}, flow: ${_localVideoTrackFlow.value?.id()}")
                
                // IMPORTANT FIX: Ensure local video track flow is preserved after PeerConnection creation
                if (localVideoTrack != null && _localVideoTrackFlow.value == null) {
                    android.util.Log.w("WebRTCService", "Local video track flow was cleared during PeerConnection creation, restoring it")
                    _localVideoTrackFlow.value = localVideoTrack
                }
                
                // Không tự động add track ở đây, để ensureLocalTrackAdded() xử lý
            } catch (e: Exception) {
                Log.e(TAG, "Error creating PeerConnection", e)
                setCallState(CallState.ERROR)
            }
        }
    }
    
    private fun ensureLocalTrackAdded() {
        synchronized(webRTCLock) {
            if (isDisposed.get()) {
                Log.w(TAG, "Service is disposed, cannot ensure local track added")
                return
            }
            if (peerConnection != null) {
                // Add video track như cũ
                if (localVideoTrack != null) {
                    val senders = peerConnection?.senders ?: emptyList()
                    val alreadyAdded = senders.any { it.track() == localVideoTrack }
                    if (!alreadyAdded) {
                        peerConnection?.addTrack(localVideoTrack)
                    }
                }
                // Add audio track nếu chưa có
                if (localAudioTrack == null) {
                    createLocalAudioTrack()
                }
                if (localAudioTrack != null) {
                    val senders = peerConnection?.senders ?: emptyList()
                    val alreadyAdded = senders.any { it.track() == localAudioTrack }
                    if (!alreadyAdded) {
                        peerConnection?.addTrack(localAudioTrack)
                    }
                }
            } else {
                android.util.Log.w("WebRTCService", "Cannot ensure local track added - peerConnection: ${peerConnection != null}, localVideoTrack: ${localVideoTrack != null}")
            }
        }
    }
    
    private fun sendOfferSdp(desc: SessionDescription) {
        val offer = mapOf(
            "type" to "offerSdp",
            "sdp" to desc.description,
            "fromUserId" to (currentUserId ?: ""),
            "toUserId" to (remoteUserId ?: "")
        )
        database.getReference("calls").child(currentCallId!!).child("signaling").push().setValue(offer)
    }
    
    private fun sendAnswerSdp(desc: SessionDescription) {
        val answer = mapOf(
            "type" to "answerSdp",
            "sdp" to desc.description,
            "fromUserId" to (currentUserId ?: ""),
            "toUserId" to (remoteUserId ?: "")
        )
        database.getReference("calls").child(currentCallId!!).child("signaling").push().setValue(answer)
    }
    
    private fun sendIceCandidate(candidate: IceCandidate) {
        val cand = mapOf(
            "type" to "candidate",
            "sdpMid" to candidate.sdpMid,
            "sdpMLineIndex" to candidate.sdpMLineIndex,
            "candidate" to candidate.sdp,
            "fromUserId" to (currentUserId ?: ""),
            "toUserId" to (remoteUserId ?: "")
        )
        database.getReference("calls").child(currentCallId!!).child("signaling").push().setValue(cand)
    }
    
    private fun listenSignaling() {
        if (isDisposed.get() || currentCallId == null) return
        
        // Remove previous listener if exists
        signalingListener?.let { listener ->
            try {
                database.getReference("calls").child(currentCallId!!).child("signaling").removeEventListener(listener)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing previous signaling listener", e)
            }
        }
        
        val ref = database.getReference("calls").child(currentCallId!!).child("signaling")
        signalingListener = object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                if (isDisposed.get()) return
                
                try {
                    val map = snapshot.value as? Map<*, *> ?: return
                    val signalType = map["type"] as? String
                    android.util.Log.d("WebRTCService", "Signaling received: type=$signalType, currentCallId=$currentCallId, currentUserId=$currentUserId, remoteUserId=$remoteUserId")
                    
                    when (signalType) {
                        "offer" -> {
                            val fromUserId = map["fromUserId"] as? String
                            val toUserId = map["toUserId"] as? String
                            android.util.Log.d("WebRTCService", "Offer signal received - fromUserId: $fromUserId, toUserId: $toUserId, currentUserId: $currentUserId, callState: ${callState.value}")
                            
                            // Chỉ xử lý offer nếu tôi là người nhận (toUserId) và người gửi không phải tôi
                            if (toUserId == currentUserId && fromUserId != currentUserId && fromUserId != null && toUserId != null) {
                                android.util.Log.d("WebRTCService", "Processing offer signal - I am the receiver")
                                // Thông báo về ViewModel về incoming call
                                if (currentCallId != null) {
                                    onIncomingCallReceived?.invoke(currentCallId!!, fromUserId, toUserId)
                                }
                                
                                // Không tự động setup camera/PeerConnection, chỉ set state để UI hiển thị incoming call
                                if (callState.value == CallState.IDLE) {
                                    android.util.Log.d("WebRTCService", "Setting RINGING state for incoming call UI")
                                    setCallState(CallState.RINGING)
                                }
                            } else {
                                android.util.Log.d("WebRTCService", "Ignoring offer signal - not meant for me (toUserId=$toUserId, currentUserId=$currentUserId, fromUserId=$fromUserId)")
                            }
                        }
                        "answer" -> {
                            val fromUserId = map["fromUserId"] as? String
                            val toUserId = map["toUserId"] as? String
                            android.util.Log.d("WebRTCService", "Answer signal received - fromUserId: $fromUserId, toUserId: $toUserId, currentUserId: $currentUserId, callState: ${callState.value}")
                            
                            // Chỉ xử lý answer nếu tôi là người nhận (toUserId) và người gửi không phải tôi
                            if (toUserId == currentUserId && fromUserId != currentUserId && fromUserId != null) {
                                android.util.Log.d("WebRTCService", "Processing answer signal - setting CONNECTED")
                                
                                // DEBUG: Log local video track state before setting CONNECTED
                                android.util.Log.d("WebRTCService", "BEFORE setCallState CONNECTED - localVideoTrack: ${localVideoTrack?.id()}, flow: ${_localVideoTrackFlow.value?.id()}")
                                
                                // Bên người gọi nhận được answer, set CONNECTED
                                if (callState.value == CallState.CALLING) {
                                    setCallState(CallState.CONNECTED)
                                    
                                    // DEBUG: Log local video track state after setting CONNECTED
                                    android.util.Log.d("WebRTCService", "AFTER setCallState CONNECTED - localVideoTrack: ${localVideoTrack?.id()}, flow: ${_localVideoTrackFlow.value?.id()}")
                                    
                                    // IMPORTANT FIX: Ensure local video track flow is preserved after CONNECTED
                                    if (localVideoTrack != null && _localVideoTrackFlow.value == null) {
                                        android.util.Log.w("WebRTCService", "Local video track flow was cleared during CONNECTED transition, restoring it")
                                        _localVideoTrackFlow.value = localVideoTrack
                                    }
                                }
                            } else {
                                android.util.Log.d("WebRTCService", "Ignoring answer signal - not meant for me (toUserId=$toUserId, currentUserId=$currentUserId, fromUserId=$fromUserId)")
                            }
                        }
                        "reject" -> {
                            android.util.Log.d("WebRTCService", "Reject signal received")
                            setCallState(CallState.ENDED)
                        }
                        "end" -> {
                            val fromUserId = map["fromUserId"] as? String
                            val toUserId = map["toUserId"] as? String
                            android.util.Log.d("WebRTCService", "End signal received - fromUserId: $fromUserId, toUserId: $toUserId, currentUserId: $currentUserId")
                            
                            // Xử lý end signal nếu tôi là người nhận (toUserId) và người gửi không phải tôi
                            if (toUserId == currentUserId && fromUserId != currentUserId && fromUserId != null) {
                                android.util.Log.d("WebRTCService", "Processing end signal - invoking onCallEndReceived callback")
                                setCallState(CallState.ENDED)
                                // Thông báo về ViewModel để reset UI và popBackStack
                                onCallEndReceived?.invoke()
                                android.util.Log.d("WebRTCService", "onCallEndReceived callback invoked")
                                
                                // Delay reset để đảm bảo không có race condition
                                CoroutineScope(Dispatchers.IO).launch {
                                    delay(500) // 0.5 second delay
                                    android.util.Log.d("WebRTCService", "Resetting after receiving end signal")
                                    reset()
                                }
                            } else {
                                android.util.Log.d("WebRTCService", "Ignoring end signal - not meant for me (toUserId=$toUserId, currentUserId=$currentUserId, fromUserId=$fromUserId)")
                            }
                        }
                        "offerSdp" -> {
                            val fromUserId = map["fromUserId"] as? String
                            val toUserId = map["toUserId"] as? String
                            android.util.Log.d("WebRTCService", "offerSdp received - fromUserId: $fromUserId, toUserId: $toUserId, currentUserId: $currentUserId, callState: ${callState.value}, peerConnection: ${peerConnection != null}")
                            
                            // Chỉ xử lý offerSdp nếu tôi là người nhận và người gửi không phải tôi
                            if (toUserId == currentUserId && fromUserId != currentUserId && fromUserId != null) {
                                val sdp = map["sdp"] as? String ?: return
                                val desc = SessionDescription(SessionDescription.Type.OFFER, sdp)
                                
                                android.util.Log.d("WebRTCService", "Processing offerSdp - setting remote description")
                                // Chỉ set remote description, không tạo answer cho đến khi user accept
                                peerConnection?.setRemoteDescription(object : SdpObserver {
                                    override fun onSetSuccess() {
                                        android.util.Log.d("WebRTCService", "Remote offer SDP set successfully, waiting for user to accept call")
                                        // Không tự động tạo answer - đợi acceptCall()
                                    }
                                    override fun onSetFailure(p0: String?) {
                                        android.util.Log.e("WebRTCService", "Failed to set remote offer SDP: $p0")
                                    }
                                    override fun onCreateSuccess(p0: SessionDescription?) {}
                                    override fun onCreateFailure(p0: String?) {}
                                }, desc)
                            } else {
                                android.util.Log.d("WebRTCService", "Ignoring offerSdp - not meant for me")
                            }
                        }
                        "answerSdp" -> {
                            val fromUserId = map["fromUserId"] as? String
                            val toUserId = map["toUserId"] as? String
                            android.util.Log.d("WebRTCService", "answerSdp received - fromUserId: $fromUserId, toUserId: $toUserId, currentUserId: $currentUserId, callState: ${callState.value}")
                            
                            // Chỉ xử lý answerSdp nếu tôi là người nhận và người gửi không phải tôi
                            if (toUserId == currentUserId && fromUserId != currentUserId && fromUserId != null) {
                                val sdp = map["sdp"] as? String ?: return
                                val desc = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                                android.util.Log.d("WebRTCService", "Processing answerSdp - setting remote description")
                                peerConnection?.setRemoteDescription(object : SdpObserver {
                                    override fun onSetSuccess() {
                                        if (isDisposed.get()) return
                                        android.util.Log.d("WebRTCService", "Answer SDP set successfully")
                                    }
                                    override fun onSetFailure(p0: String?) {}
                                    override fun onCreateSuccess(p0: SessionDescription?) {}
                                    override fun onCreateFailure(p0: String?) {}
                                }, desc)
                            } else {
                                android.util.Log.d("WebRTCService", "Ignoring answerSdp - not meant for me")
                            }
                        }
                        "candidate" -> {
                            if (isDisposed.get()) return
                            val fromUserId = map["fromUserId"] as? String
                            val toUserId = map["toUserId"] as? String
                            android.util.Log.d("WebRTCService", "ICE candidate received - fromUserId: $fromUserId, toUserId: $toUserId, currentUserId: $currentUserId")
                            
                            // Chỉ xử lý ICE candidate nếu tôi là người nhận và người gửi không phải tôi
                            if (toUserId == currentUserId && fromUserId != currentUserId && fromUserId != null) {
                                val sdpMid = map["sdpMid"] as? String ?: return
                                val sdpMLineIndex = (map["sdpMLineIndex"] as? Long)?.toInt() ?: return
                                val candidate = map["candidate"] as? String ?: return
                                android.util.Log.d("WebRTCService", "Processing ICE candidate")
                                peerConnection?.addIceCandidate(
                                    IceCandidate(sdpMid, sdpMLineIndex, candidate)
                                )
                            } else {
                                android.util.Log.d("WebRTCService", "Ignoring ICE candidate - not meant for me")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing signaling message", e)
                }
            }
            override fun onChildChanged(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {}
            override fun onChildMoved(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e(TAG, "Signaling listener cancelled", error.toException())
            }
        }
        
        ref.addChildEventListener(signalingListener!!)
    }
    
    fun getRemoteVideoTrack(): VideoTrack? {
        return synchronized(webRTCLock) {
            try {
                if (isDisposed.get()) {
                    Log.w(TAG, "Service is disposed, cannot get remote video track")
                    return@synchronized null
                }
                remoteVideoTrack
            } catch (e: Exception) {
                Log.e(TAG, "Error getting remote video track", e)
                null
            }
        }
    }
    
    private fun cleanupCurrentCallSync() {
        // Synchronous cleanup chỉ resources của cuộc gọi hiện tại
        try {
            // Remove Firebase listeners for current call (sync)
            signalingListener?.let { listener ->
                try {
                    currentCallId?.let { callId ->
                        database.getReference("calls").child(callId).child("signaling").removeEventListener(listener)
                        android.util.Log.d("WebRTCService", "Removed signaling listener for call: $callId")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing signaling listener", e)
                }
                signalingListener = null
            }
            
            // Cleanup chỉ PeerConnection, giữ lại factory và EGL base
            synchronized(webRTCLock) {
                try {
                    // Close peer connection sử dụng helper
                    WebRTCHelper.closePeerConnection(peerConnection)
                    peerConnection = null
                    
                    // Clear remote video track 
                    remoteVideoTrack = null
                    _remoteVideoTrackFlow.value = null
                    
                    // IMPORTANT FIX: Chỉ dispose local video track khi cuộc gọi thực sự ended, không phải khi switching state
                    // Giữ local video track để tái sử dụng cho cuộc gọi tiếp theo hoặc trong cùng session
                    val currentState = _callState.value
                    if (currentState == CallState.ENDED || currentState == CallState.ERROR) {
                        // Chỉ dispose khi call thực sự ended
                        if (localVideoTrack != null) {
                            android.util.Log.d("WebRTCService", "Call ended - disposing local video track: ${localVideoTrack?.id()}")
                            try {
                                localVideoTrack?.dispose()
                            } catch (e: Exception) {
                                android.util.Log.w("WebRTCService", "Error disposing local video track during end cleanup", e)
                            }
                            localVideoTrack = null
                            // CRITICAL FIX: NEVER clear local video track flow, let it be handled by reset() or dispose()
                            android.util.Log.d("WebRTCService", "Local video track disposed but flow preservation handled by reset()")
                        }
                        
                        // Dispose local video source and capturer chỉ khi ended
                        if (localVideoSource != null) {
                            try {
                                localVideoSource?.dispose()
                            } catch (e: Exception) {
                                android.util.Log.w("WebRTCService", "Error disposing local video source during end cleanup", e)
                            }
                            localVideoSource = null
                        }
                        
                        if (videoCapturer != null) {
                            try {
                                WebRTCHelper.stopVideoCapture(videoCapturer)
                                WebRTCHelper.disposeVideoCapturer(videoCapturer)
                            } catch (e: Exception) {
                                android.util.Log.w("WebRTCService", "Error disposing video capturer during end cleanup", e)
                            }
                            videoCapturer = null
                        }
                    } else {
                        // Nếu không phải ended, giữ local video track và chỉ log
                        android.util.Log.d("WebRTCService", "Preserving local video track during call state transition: ${currentState}, track=${localVideoTrack?.id()}")
                    }
                    
                    // Clear call info
                    val oldCallId = currentCallId
                    currentCallId = null
                    currentUserId = null
                    remoteUserId = null
                    
                    android.util.Log.d("WebRTCService", "Current call cleanup completed synchronously for call: $oldCallId")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error during current call cleanup", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during synchronous current call cleanup", e)
        }
    }

    private fun cleanupCurrentCall() {
        // Async cleanup chỉ resources của cuộc gọi hiện tại, giữ lại WebRTC components để tái sử dụng
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // DEBUG: Log local video track state before cleanup
                android.util.Log.d("WebRTCService", "BEFORE cleanupCurrentCall - localVideoTrack: ${localVideoTrack?.id()}, flow: ${_localVideoTrackFlow.value?.id()}")
                
                // Remove Firebase listeners for current call
                withContext(Dispatchers.Main) {
                    signalingListener?.let { listener ->
                        try {
                            currentCallId?.let { callId ->
                                database.getReference("calls").child(callId).child("signaling").removeEventListener(listener)
                                android.util.Log.d("WebRTCService", "Removed signaling listener for call: $callId")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error removing signaling listener", e)
                        }
                        signalingListener = null
                    }
                }
                
                // Cleanup chỉ PeerConnection, giữ lại factory và EGL base
                synchronized(webRTCLock) {
                    try {
                        // Close peer connection sử dụng helper
                        WebRTCHelper.closePeerConnection(peerConnection)
                        peerConnection = null
                        
                        // Clear remote video track
                        remoteVideoTrack = null
                        _remoteVideoTrackFlow.value = null
                        
                        // IMPORTANT FIX: Preserve local video track and components for reuse
                        // Chỉ log không dispose, để local video có thể được tái sử dụng
                        android.util.Log.d("WebRTCService", "Preserving local video track for reuse: ${localVideoTrack?.id()}")
                        
                        // CRITICAL FIX: Ensure local video track flow is never cleared during cleanup
                        if (localVideoTrack != null) {
                            android.util.Log.d("WebRTCService", "Ensuring local video track flow is preserved during cleanup")
                            _localVideoTrackFlow.value = localVideoTrack
                        }
                        
                        // Clean up audio track
                        if (localAudioTrack != null) {
                            try { localAudioTrack?.dispose() } catch (_: Exception) {}
                            localAudioTrack = null
                        }
                        if (localAudioSource != null) {
                            try { localAudioSource?.dispose() } catch (_: Exception) {}
                            localAudioSource = null
                        }
                        
                        // Clear call info
                        currentCallId = null
                        currentUserId = null
                        remoteUserId = null
                        
                        android.util.Log.d("WebRTCService", "Current call cleanup completed - service ready for reuse")
                        
                        // DEBUG: Log local video track state after cleanup
                        android.util.Log.d("WebRTCService", "AFTER cleanupCurrentCall - localVideoTrack: ${localVideoTrack?.id()}, flow: ${_localVideoTrackFlow.value?.id()}")
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during current call cleanup", e)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during current call cleanup", e)
            }
        }
    }

    private fun cleanup() {
        // Full cleanup - dispose toàn bộ service
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Remove Firebase listeners first (có thể chạy trên main thread)
                withContext(Dispatchers.Main) {
                    signalingListener?.let { listener ->
                        try {
                            currentCallId?.let { callId ->
                                database.getReference("calls").child(callId).child("signaling").removeEventListener(listener)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error removing signaling listener", e)
                        }
                        signalingListener = null
                    }
                    
                    incomingCallListener?.let { listener ->
                        try {
                            database.getReference("calls").removeEventListener(listener)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error removing incoming call listener", e)
                        }
                        incomingCallListener = null
                    }
                }
                
                // Cleanup WebRTC resources trên background thread
                synchronized(webRTCLock) {
                    try {
                        // Stop video capturer sử dụng helper
                        WebRTCHelper.stopVideoCapture(videoCapturer)
                        
                        // Dispose video capturer sử dụng helper
                        WebRTCHelper.disposeVideoCapturer(videoCapturer)
                        
                        // Close peer connection sử dụng helper
                        WebRTCHelper.closePeerConnection(peerConnection)
                        
                        // Dispose peer connection factory sử dụng helper
                        WebRTCHelper.disposePeerConnectionFactory(peerConnectionFactory)
                        
                        // Release EGL base sử dụng helper
                        WebRTCHelper.releaseEglBase(eglBase)
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during WebRTC cleanup", e)
                    } finally {
                        // Clear all references
                        localVideoTrack = null
                        localVideoSource = null
                        videoCapturer = null
                        peerConnectionFactory = null
                        eglBase = null
                        peerConnection = null
                        remoteVideoTrack = null
                        currentCallId = null
                        currentUserId = null
                        remoteUserId = null
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }
    
    fun reset() {
        if (isResetting.get()) return
        
        try {
            isResetting.set(true)
            
            val currentState = _callState.value
            
            // Reset call state first
            setCallState(CallState.IDLE)
            
            // IMPORTANT FIX: Clear remote video track flow but preserve local video if not ended
            _remoteVideoTrackFlow.value = null
            
            // CRITICAL FIX: NEVER clear local video track flow during reset, always preserve it
            // Local video should persist across all call states for smooth user experience
            if (localVideoTrack != null) {
                // Always ensure local video track flow is maintained
                _localVideoTrackFlow.value = localVideoTrack
            }
            
            // Cleanup current call resources but don't dispose the service
            cleanupCurrentCall()
            
            // Ensure service is not marked as disposed
            isDisposed.set(false)
            
            // Delay trước khi khởi tạo lại để tránh race condition
            mainHandler.postDelayed({
                try {
                    // Reinitialize WebRTC if needed
                    if (eglBase == null || peerConnectionFactory == null) {
                        android.util.Log.d("WebRTCService", "Reinitializing WebRTC components")
                        initializeWebRTC(context)
                    }
                    
                    // Nếu không có local video track, tạo mới
                    if (localVideoTrack == null) {
                        android.util.Log.d("WebRTCService", "Creating new local video track after reset")
                        createLocalVideoTrack(context)
                    } else {
                        android.util.Log.d("WebRTCService", "Local video track preserved after reset: ${localVideoTrack?.id()}")
                        // Đảm bảo flow được update với track hiện tại
                        _localVideoTrackFlow.value = localVideoTrack
                    }
                    
                    // Ensure remote video flow is clear for new calls
                    _remoteVideoTrackFlow.value = null
                    
                    android.util.Log.d("WebRTCService", "WebRTCService reset successfully - ready for new calls")
                } catch (e: Exception) {
                    android.util.Log.e("WebRTCService", "Error reinitializing WebRTC after reset", e)
                } finally {
                    isResetting.set(false)
                }
            }, 200) // 200ms delay
            
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting WebRTCService", e)
            isResetting.set(false)
        }
    }
    
    fun dispose() {
        cleanup()
        isDisposed.set(true)
    }
    
    fun setCallStateRinging() {
        android.util.Log.d("WebRTCService", "setCallStateRinging() called from ViewModel")
        setCallState(CallState.RINGING)
    }
    
    fun setIncomingCall(callId: String, userId: String, remoteUserId: String) {
        android.util.Log.d("WebRTCService", "BEFORE setIncomingCall - currentCallId: $currentCallId")
        android.util.Log.d("WebRTCService", "setIncomingCall: callId=$callId, userId=$userId, remoteUserId=$remoteUserId")
        this.currentCallId = callId
        this.currentUserId = userId
        this.remoteUserId = remoteUserId
        android.util.Log.d("WebRTCService", "AFTER setIncomingCall - currentCallId: $currentCallId")
        
        // Ensure WebRTC is initialized
        if (eglBase == null || peerConnectionFactory == null) {
            android.util.Log.d("WebRTCService", "WebRTC not initialized for incoming call, initializing now")
            initializeWebRTC(context)
        }
        
        // Tạo PeerConnection ngay để có thể nhận offerSdp, nhưng chưa tạo camera
        android.util.Log.d("WebRTCService", "Creating PeerConnection for incoming call to handle offerSdp")
        createPeerConnection()
        
        // Lắng nghe signaling cho incoming call
        listenSignaling()
    }
    
    private fun setCallState(state: CallState) {
        val previousState = _callState.value
        if (previousState != state) {
            _callState.value = state
            
            // CRITICAL FIX: Ensure local video track flow is never lost during state transitions
            if (localVideoTrack != null && _localVideoTrackFlow.value == null) {
                _localVideoTrackFlow.value = localVideoTrack
            }
        }
    }
    
    fun isDisposed(): Boolean {
        return isDisposed.get()
    }
    
    fun getCurrentCallId(): String? {
        return currentCallId
    }
    
    fun switchCamera(): Boolean {
        return synchronized(webRTCLock) {
            try {
                if (isDisposed.get()) {
                    Log.w(TAG, "Cannot switch camera - service is disposed")
                    return@synchronized false
                }
                
                if (videoCapturer == null) {
                    Log.w(TAG, "Cannot switch camera - video capturer is null")
                    return@synchronized false
                }
                
                val result = WebRTCHelper.switchCamera(videoCapturer)
                if (result) {
                    Log.d(TAG, "Camera switched successfully")
                } else {
                    Log.e(TAG, "Failed to switch camera")
                }
                result
            } catch (e: Exception) {
                Log.e(TAG, "Error switching camera", e)
                false
            }
        }
    }
    
    fun getLocalAudioTrack(): AudioTrack? {
        return synchronized(webRTCLock) {
            try {
                if (isDisposed.get()) return@synchronized null
                localAudioTrack
            } catch (e: Exception) {
                null
            }
        }
    }
    
    private fun createLocalAudioTrack() {
        if (isDisposed.get()) return
        synchronized(webRTCLock) {
            // Clean up existing audio track
            if (localAudioTrack != null) {
                try { localAudioTrack?.dispose() } catch (_: Exception) {}
                localAudioTrack = null
            }
            if (localAudioSource != null) {
                try { localAudioSource?.dispose() } catch (_: Exception) {}
                localAudioSource = null
            }
            try {
                if (peerConnectionFactory != null) {
                    localAudioSource = peerConnectionFactory!!.createAudioSource(MediaConstraints())
                    localAudioTrack = peerConnectionFactory!!.createAudioTrack("ARDAMSa0", localAudioSource)
                    localAudioTrack?.setEnabled(true)
                }
            } catch (_: Exception) {}
        }
    }
    
    fun setMicEnabled(enabled: Boolean) {
        synchronized(webRTCLock) {
            localAudioTrack?.setEnabled(enabled)
        }
    }
    
    fun isMicEnabled(): Boolean {
        return synchronized(webRTCLock) {
            localAudioTrack?.enabled() ?: false
        }
    }
    
    // Hàm bật/tắt camera (video)
    fun setVideoEnabled(enabled: Boolean) {
        synchronized(webRTCLock) {
            localVideoTrack?.setEnabled(enabled)
        }
    }
    fun isVideoEnabled(): Boolean {
        return synchronized(webRTCLock) {
            localVideoTrack?.enabled() ?: false
        }
    }
} 