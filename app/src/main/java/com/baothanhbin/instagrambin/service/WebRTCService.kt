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
    var onIncomingCallReceived: ((callId: String, fromUserId: String, toUserId: String) -> Unit)? =
        null

    private var currentCallId: String? = null
    private var currentUserId: String? = null
    private var remoteUserId: String? = null

    // WebRTC components
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var eglBase: EglBase? = null
    private var peerConnection: PeerConnection? = null
    private var remoteVideoTrack: VideoTrack? = null

    // Danh sách ICE servers, sử dụng STUN server của Google
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    // Lock để đồng bộ hóa các thao tác WebRTC
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

                // Khởi tạo PeerConnectionFactory
                peerConnectionFactory =
                    WebRTCHelper.initializePeerConnectionFactory(context, eglBase)
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
            return
        }

        synchronized(webRTCLock) {

            // xoa bỏ các track và nguồn video cũ nếu có
            if (localVideoTrack != null) {

                try {
                    localVideoTrack?.dispose()
                } catch (e: Exception) {

                }
                localVideoTrack = null
                _localVideoTrackFlow.value = null // Clear flow

            }

            // xoa bỏ các nguồn video cũ nếu có
            if (localVideoSource != null) {

                try {
                    localVideoSource?.dispose()
                } catch (e: Exception) {

                }
                localVideoSource = null
            }

            // xoa bỏ video capturer cũ nếu có
            if (videoCapturer != null) {

                try {
                    WebRTCHelper.stopVideoCapture(videoCapturer)
                    WebRTCHelper.disposeVideoCapturer(videoCapturer)
                } catch (e: Exception) {

                }
                videoCapturer = null
            }

            try {
                // Tạo camera capturer
                videoCapturer = WebRTCHelper.createCameraCapturer(context)
                if (videoCapturer == null) {
                    android.util.Log.e("WebRTCService", "Cannot create video capturer")
                    return
                }

                // Tạo video source sử dụng helper
                localVideoSource = WebRTCHelper.createVideoSource(peerConnectionFactory)
                if (localVideoSource == null) {

                    return
                }


                // Khởi tạo video capturer sử dụng helper
                if (!WebRTCHelper.initializeVideoCapturer(
                        videoCapturer,
                        eglBase,
                        context,
                        localVideoSource
                    )
                ) {
                    return
                }

                // Bắt đầu capture sử dụng helper
                if (!WebRTCHelper.startVideoCapture(videoCapturer)) {
                    android.util.Log.e("WebRTCService", "Failed to start video capture")
                } else {
                    android.util.Log.d("WebRTCService", "Video capturer started successfully")
                }

                // Tạo video track sử dụng helper
                localVideoTrack =
                    WebRTCHelper.createVideoTrack(peerConnectionFactory, localVideoSource)
                if (localVideoTrack != null) {
                    android.util.Log.d(
                        "WebRTCService",
                        "Local video track created successfully: id=${localVideoTrack?.id()}, enabled=${localVideoTrack?.enabled()}"
                    )
                    // Update flow để UI cập nhật ngay lập tức
                    _localVideoTrackFlow.value = localVideoTrack
                    android.util.Log.d(
                        "WebRTCService",
                        "Local video track flow updated successfully"
                    )
                    ensureLocalTrackAdded()
                } else {
                    android.util.Log.e("WebRTCService", "Cannot create local video track")
                    _localVideoTrackFlow.value = null
                    android.util.Log.e(
                        "WebRTCService",
                        "Local video track flow cleared due to creation failure"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating local video track", e)
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
        // Kiểm tra trạng thái service trước khi bắt đầu cuộc gọi
        if (isDisposed.get() || isResetting.get()) {
            Log.w(TAG, "Cannot start call - service is disposed or resetting")
            return
        }

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

        //Chac chắn rằng WebRTC đã được khởi tạo trước khi bắt đầu cuộc gọi
        if (eglBase == null || peerConnectionFactory == null) {
            initializeWebRTC(context)
        }

        // Chỉ tạo local video track mới nếu chưa có hoặc bị dispose
        if (localVideoTrack == null) {
            createLocalVideoTrack(context)

            // Chờ track được tạo với retries (giống như startCall)
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
                // Set local description và gửi offer SDP
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
        this.currentCallId = callId
        this.currentUserId = userId
        this.remoteUserId = remoteUserId

        // Chắc chắn rằng WebRTC đã được khởi tạo trước khi chấp nhận cuộc gọi
        if (eglBase == null || peerConnectionFactory == null) {
            initializeWebRTC(context)
        }

        // Nếu peerConnection chưa được tạo, tạo mới
        if (localVideoTrack == null) {

            createLocalVideoTrack(context)

            // Chờ track được tạo với retries
            var retries = 0
            while (localVideoTrack == null && retries < 10) {
                Thread.sleep(100) // Wait 100ms
                retries++
            }
        } else {

        }

        if (localVideoTrack != null) {

            // chắc chắn rằng local video track đã được cập nhật vào flow
            _localVideoTrackFlow.value = localVideoTrack
        } else {

        }

        // Add camera track vào PeerConnection đã có
        ensureLocalTrackAdded()

        // Khi nhận được offerSdp từ Firebase (trong listenSignaling)
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(answerDesc: SessionDescription) {
                // Set local description và gửi answer SDP
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
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

    fun endCall() {
        android.util.Log.d(
            "WebRTCService",
            "endCall() called - callId: $currentCallId, userId: $currentUserId, remoteUserId: $remoteUserId"
        )
        setCallState(CallState.ENDED)

        if (currentCallId == null || currentUserId == null || remoteUserId == null) {
            android.util.Log.w(
                "WebRTCService",
                "Cannot send end signal - missing call info: callId=$currentCallId, userId=$currentUserId, remoteUserId=$remoteUserId"
            )
        } else {
            android.util.Log.d(
                "WebRTCService",
                "Sending end call signal from $currentUserId to $remoteUserId for call $currentCallId"
            )
            sendCallEnd(currentCallId!!, currentUserId!!, remoteUserId!!)
        }

        // Delay reset để đảm bảo end signal được gửi và nhận, nhưng giữ service có thể tái sử dụng
        CoroutineScope(Dispatchers.IO).launch {
            delay(1500) // 1.5 second delay để đảm bảo signal được xử lý
            android.util.Log.d("WebRTCService", "Resetting after delay to allow reuse")
            reset()
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
            database.getReference("users").child(toUserId).child("fcmToken").get()
                .addOnSuccessListener { tokenSnap ->
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
        database.getReference("calls").child(callId).child("signaling").push()
            .setValue(answerSignal)
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
        database.getReference("calls").child(callId).child("signaling").push()
            .setValue(rejectSignal)
            .addOnSuccessListener {
                Log.d(TAG, "Call reject signal sent successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send call reject signal", e)
            }
    }

    private fun sendCallEnd(callId: String, fromUserId: String, toUserId: String) {
        android.util.Log.d(
            "WebRTCService",
            "sendCallEnd: callId=$callId, from=$fromUserId, to=$toUserId"
        )

        // Update root node type first
        database.getReference("calls").child(callId).child("type").setValue("end")
            .addOnSuccessListener {
                android.util.Log.d(
                    "WebRTCService",
                    "Call root node type updated to 'end' successfully"
                )
            }
            .addOnFailureListener { e ->
                android.util.Log.e(
                    "WebRTCService",
                    "Failed to update call root node type to 'end'",
                    e
                )
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
                    override fun onAddTrack(
                        receiver: RtpReceiver?,
                        streams: Array<out MediaStream>?
                    ) {
                        if (isDisposed.get()) return
                        try {
                            val track = receiver?.track()

                            if (track is VideoTrack) {
                                synchronized(webRTCLock) {
                                    if (!isDisposed.get()) {
                                        // xoa bỏ track cũ nếu có
                                        val previousTrack = remoteVideoTrack
                                        if (previousTrack != null && previousTrack != track) {
                                            android.util.Log.d(
                                                "WebRTCService",
                                                "Replacing previous remote video track: ${previousTrack.id()}"
                                            )
                                        }
                                        // Cập nhật remote video track
                                        remoteVideoTrack = track
                                        _remoteVideoTrackFlow.value = track

                                    }
                                }
                            } else {

                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in onAddTrack", e)
                        }
                    }
                }

                // Tạo PeerConnection sử dụng helper
                peerConnection =
                    WebRTCHelper.createPeerConnection(peerConnectionFactory, iceServers, observer)
                if (peerConnection == null) {
                    Log.e(TAG, "Failed to create PeerConnection")
                    setCallState(CallState.ERROR)
                    return
                }


                // Nếu localVideoTrack đã được tạo, thêm vào PeerConnection
                if (localVideoTrack != null && _localVideoTrackFlow.value == null) {
                    android.util.Log.w(
                        "WebRTCService",
                        "Local video track flow was cleared during PeerConnection creation, restoring it"
                    )
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
                    } else {

                    }
                } else {

                }
            } else {
                android.util.Log.w(
                    "WebRTCService",
                    "Cannot ensure local track added - peerConnection: ${peerConnection != null}, localVideoTrack: ${localVideoTrack != null}"
                )
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
        database.getReference("calls").child(currentCallId!!).child("signaling").push()
            .setValue(offer)
    }

    private fun sendAnswerSdp(desc: SessionDescription) {
        val answer = mapOf(
            "type" to "answerSdp",
            "sdp" to desc.description,
            "fromUserId" to (currentUserId ?: ""),
            "toUserId" to (remoteUserId ?: "")
        )
        database.getReference("calls").child(currentCallId!!).child("signaling").push()
            .setValue(answer)
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
        database.getReference("calls").child(currentCallId!!).child("signaling").push()
            .setValue(cand)
    }

    private fun listenSignaling() {
        if (isDisposed.get() || currentCallId == null) return

        // Remove previous listener if exists
        signalingListener?.let { listener ->
            try {
                database.getReference("calls").child(currentCallId!!).child("signaling")
                    .removeEventListener(listener)
            } catch (e: Exception) {

            }
        }

        val ref = database.getReference("calls").child(currentCallId!!).child("signaling")
        signalingListener = object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(
                snapshot: com.google.firebase.database.DataSnapshot,
                previousChildName: String?
            ) {
                if (isDisposed.get()) return

                try {
                    val map = snapshot.value as? Map<*, *> ?: return
                    val signalType = map["type"] as? String
                    when (signalType) {
                        "offer" -> {
                            val fromUserId = map["fromUserId"] as? String
                            val toUserId = map["toUserId"] as? String

                            // Chỉ xử lý offer nếu tôi là người nhận (toUserId) và người gửi không phải tôi
                            if (toUserId == currentUserId && fromUserId != currentUserId && fromUserId != null && toUserId != null) {
                                // Thông báo về ViewModel về incoming call
                                if (currentCallId != null) {
                                    onIncomingCallReceived?.invoke(
                                        currentCallId!!,
                                        fromUserId,
                                        toUserId
                                    )
                                }

                                // Không tự động setup camera/PeerConnection, chỉ set state để UI hiển thị incoming call
                                if (callState.value == CallState.IDLE) {

                                    setCallState(CallState.RINGING)
                                }
                            } else {

                            }
                        }

                        "answer" -> {
                            val fromUserId = map["fromUserId"] as? String
                            val toUserId = map["toUserId"] as? String

                            // Chỉ xử lý answer nếu tôi là người nhận (toUserId) và người gửi không phải tôi
                            if (toUserId == currentUserId && fromUserId != currentUserId && fromUserId != null) {
                                // Bên người gọi nhận được answer, set CONNECTED
                                if (callState.value == CallState.CALLING) {
                                    setCallState(CallState.CONNECTED)
                                    // Đảm bảo rằng peerConnection đã được tạo
                                    if (localVideoTrack != null && _localVideoTrackFlow.value == null) {
                                        _localVideoTrackFlow.value = localVideoTrack
                                    }
                                }
                            } else {

                            }
                        }

                        "reject" -> {

                            setCallState(CallState.ENDED)
                        }

                        "end" -> {
                            val fromUserId = map["fromUserId"] as? String
                            val toUserId = map["toUserId"] as? String

                            // Xử lý end signal nếu tôi là người nhận (toUserId) và người gửi không phải tôi
                            if (toUserId == currentUserId && fromUserId != currentUserId && fromUserId != null) {
                                setCallState(CallState.ENDED)
                                // Thông báo về ViewModel để reset UI và popBackStack
                                onCallEndReceived?.invoke()

                                // Delay reset để đảm bảo không có race condition
                                CoroutineScope(Dispatchers.IO).launch {
                                    delay(500) // 0.5 second delay
                                    reset()
                                }
                            } else {
                            }
                        }

                        "offerSdp" -> {
                            val fromUserId = map["fromUserId"] as? String // Người gửi offer SDP
                            val toUserId = map["toUserId"] as? String


                            // Chỉ xử lý offerSdp nếu tôi là người nhận và người gửi không phải tôi
                            if (toUserId == currentUserId && fromUserId != currentUserId && fromUserId != null) {
                                val sdp = map["sdp"] as? String ?: return
                                val desc = SessionDescription(SessionDescription.Type.OFFER, sdp)

                                // Chỉ set remote description, không tạo answer cho đến khi user accept
                                peerConnection?.setRemoteDescription(object : SdpObserver {
                                    override fun onSetSuccess() {
                                        // Không tự động tạo answer - đợi acceptCall()
                                    }

                                    override fun onSetFailure(p0: String?) {

                                    }

                                    override fun onCreateSuccess(p0: SessionDescription?) {}
                                    override fun onCreateFailure(p0: String?) {}
                                }, desc)
                            } else {

                            }
                        }

                        "answerSdp" -> {
                            val fromUserId = map["fromUserId"] as? String
                            val toUserId = map["toUserId"] as? String

                            // Chỉ xử lý answerSdp nếu tôi là người nhận và người gửi không phải tôi
                            if (toUserId == currentUserId && fromUserId != currentUserId && fromUserId != null) {
                                val sdp = map["sdp"] as? String ?: return
                                val desc = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                                peerConnection?.setRemoteDescription(object : SdpObserver {
                                    override fun onSetSuccess() {
                                        if (isDisposed.get()) return
                                    }

                                    override fun onSetFailure(p0: String?) {}
                                    override fun onCreateSuccess(p0: SessionDescription?) {}
                                    override fun onCreateFailure(p0: String?) {}
                                }, desc)
                            } else {
                            }
                        }

                        "candidate" -> {
                            if (isDisposed.get()) return
                            val fromUserId = map["fromUserId"] as? String
                            val toUserId = map["toUserId"] as? String

                            // Chỉ xử lý ICE candidate nếu tôi là người nhận và người gửi không phải tôi
                            if (toUserId == currentUserId && fromUserId != currentUserId && fromUserId != null) {
                                val sdpMid = map["sdpMid"] as? String ?: return
                                val sdpMLineIndex =
                                    (map["sdpMLineIndex"] as? Long)?.toInt() ?: return
                                // Kiểm tra candidate có tồn tại không
                                val candidate = map["candidate"] as? String ?: return
                                // thêm vào peerConnection để thử kết nối với địa chỉ đó
                                peerConnection?.addIceCandidate(
                                    IceCandidate(sdpMid, sdpMLineIndex, candidate)
                                )
                            } else {

                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing signaling message", e)
                }
            }

            override fun onChildChanged(
                snapshot: com.google.firebase.database.DataSnapshot,
                previousChildName: String?
            ) {
            }

            override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {}
            override fun onChildMoved(
                snapshot: com.google.firebase.database.DataSnapshot,
                previousChildName: String?
            ) {
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e(TAG, "Signaling listener cancelled", error.toException())
            }
        }

        ref.addChildEventListener(signalingListener!!)
    }

    private fun cleanupCurrentCall() {
        // Async cleanup chỉ resources của cuộc gọi hiện tại, giữ lại WebRTC components để tái sử dụng
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // DEBUG: Log local video track state before cleanup
                android.util.Log.d(
                    "WebRTCService",
                    "BEFORE cleanupCurrentCall - localVideoTrack: ${localVideoTrack?.id()}, flow: ${_localVideoTrackFlow.value?.id()}"
                )

                // Remove Firebase listeners for current call
                withContext(Dispatchers.Main) {
                    signalingListener?.let { listener ->
                        try {
                            currentCallId?.let { callId ->
                                database.getReference("calls").child(callId).child("signaling")
                                    .removeEventListener(listener)
                                android.util.Log.d(
                                    "WebRTCService",
                                    "Removed signaling listener for call: $callId"
                                )
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
                        android.util.Log.d(
                            "WebRTCService",
                            "Preserving local video track for reuse: ${localVideoTrack?.id()}"
                        )

                        // CRITICAL FIX: Ensure local video track flow is never cleared during cleanup
                        if (localVideoTrack != null) {
                            android.util.Log.d(
                                "WebRTCService",
                                "Ensuring local video track flow is preserved during cleanup"
                            )
                            _localVideoTrackFlow.value = localVideoTrack
                        }

                        // Clean up audio track
                        if (localAudioTrack != null) {
                            try {
                                localAudioTrack?.dispose()
                            } catch (_: Exception) {
                            }
                            localAudioTrack = null
                        }
                        if (localAudioSource != null) {
                            try {
                                localAudioSource?.dispose()
                            } catch (_: Exception) {
                            }
                            localAudioSource = null
                        }

                        // Clear call info
                        currentCallId = null
                        currentUserId = null
                        remoteUserId = null


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
                                database.getReference("calls").child(callId).child("signaling")
                                    .removeEventListener(listener)
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
                        initializeWebRTC(context)
                    }

                    // Nếu không có local video track, tạo mới
                    if (localVideoTrack == null) {
                        createLocalVideoTrack(context)
                    } else {

                        // Đảm bảo flow được update với track hiện tại
                        _localVideoTrackFlow.value = localVideoTrack
                    }

                    // Ensure remote video flow is clear for new calls
                    _remoteVideoTrackFlow.value = null

                } catch (e: Exception) {

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

    fun setIncomingCall(callId: String, userId: String, remoteUserId: String) {


        this.currentCallId = callId
        this.currentUserId = userId
        this.remoteUserId = remoteUserId


        // Ensure WebRTC is initialized
        if (eglBase == null || peerConnectionFactory == null) {

            initializeWebRTC(context)
        }

        // Tạo local video track nếu chưa có
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

    private fun createLocalAudioTrack() {
        if (isDisposed.get()) return
        synchronized(webRTCLock) {
            // Clean up existing audio track
            if (localAudioTrack != null) {
                try {
                    localAudioTrack?.dispose()
                } catch (_: Exception) {
                }
                localAudioTrack = null
            }
            if (localAudioSource != null) {
                try {
                    localAudioSource?.dispose()
                } catch (_: Exception) {
                }
                localAudioSource = null
            }
            try {
                if (peerConnectionFactory != null) {
                    localAudioSource = peerConnectionFactory!!.createAudioSource(MediaConstraints())
                    localAudioTrack =
                        peerConnectionFactory!!.createAudioTrack("ARDAMSa0", localAudioSource)
                    localAudioTrack?.setEnabled(true)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun setMicEnabled(enabled: Boolean) {
        synchronized(webRTCLock) {
            localAudioTrack?.setEnabled(enabled)
        }
    }

    // Hàm bật/tắt camera (video)
    fun setVideoEnabled(enabled: Boolean) {
        synchronized(webRTCLock) {
            localVideoTrack?.setEnabled(enabled)
        }
    }
} 