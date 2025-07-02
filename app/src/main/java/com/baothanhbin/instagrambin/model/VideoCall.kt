package com.baothanhbin.instagrambin.model

data class VideoCall(
    val callId: String = "",
    val callerId: String = "",
    val receiverId: String = "",
    val status: CallStatus = CallStatus.IDLE,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val duration: Long = 0L,
    val isVideoEnabled: Boolean = true,
    val isAudioEnabled: Boolean = true
)

enum class CallStatus {
    IDLE,
    RINGING,
    CONNECTED,
    ENDED,
    MISSED,
    REJECTED
}

data class CallSignal(
    val signalType: SignalType,
    val callId: String,
    val fromUserId: String,
    val toUserId: String,
    val data: String = "", // SDP offer/answer or ICE candidate
    val timestamp: Long = System.currentTimeMillis()
)

enum class SignalType {
    CALL_REQUEST,
    CALL_ACCEPT,
    CALL_REJECT,
    CALL_END,
    SDP_OFFER,
    SDP_ANSWER,
    ICE_CANDIDATE
} 