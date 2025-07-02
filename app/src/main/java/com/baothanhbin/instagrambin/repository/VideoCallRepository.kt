package com.baothanhbin.instagrambin.repository

import com.baothanhbin.instagrambin.model.CallSignal
import com.baothanhbin.instagrambin.model.SignalType
import com.baothanhbin.instagrambin.model.VideoCall
import com.baothanhbin.instagrambin.model.CallStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class VideoCallRepository {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid
    
    // Video calls collection
    private val videoCallsRef = database.getReference("video_calls")
    
    // Signaling collection for WebRTC
    private val signalingRef = database.getReference("signaling")
    
    fun createCall(callerId: String, receiverId: String): String {
        val callId = UUID.randomUUID().toString()
        val call = VideoCall(
            callId = callId,
            callerId = callerId,
            receiverId = receiverId,
            status = CallStatus.RINGING,
            startTime = System.currentTimeMillis()
        )
        
        videoCallsRef.child(callId).setValue(call)
        return callId
    }
    
    fun updateCallStatus(callId: String, status: CallStatus) {
        val updates = HashMap<String, Any>()
        updates["status"] = status.name
        
        when (status) {
            CallStatus.CONNECTED -> {
                updates["startTime"] = System.currentTimeMillis()
            }
            CallStatus.ENDED -> {
                updates["endTime"] = System.currentTimeMillis()
                // Calculate duration
                videoCallsRef.child(callId).get().addOnSuccessListener { snapshot ->
                    val call = snapshot.getValue(VideoCall::class.java)
                    call?.let {
                        val duration = System.currentTimeMillis() - it.startTime
                        updates["duration"] = duration
                        videoCallsRef.child(callId).updateChildren(updates)
                    }
                }
                return
            }
            else -> {}
        }
        
        videoCallsRef.child(callId).updateChildren(updates)
    }
    
    fun getCall(callId: String): Flow<VideoCall?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val call = snapshot.getValue(VideoCall::class.java)
                trySend(call)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        videoCallsRef.child(callId).addValueEventListener(listener)
        awaitClose { videoCallsRef.child(callId).removeEventListener(listener) }
    }
    
    fun getUserCalls(userId: String): Flow<List<VideoCall>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val calls = mutableListOf<VideoCall>()
                for (child in snapshot.children) {
                    val call = child.getValue(VideoCall::class.java)
                    if (call != null && (call.callerId == userId || call.receiverId == userId)) {
                        calls.add(call)
                    }
                }
                trySend(calls.sortedByDescending { it.startTime })
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        videoCallsRef.addValueEventListener(listener)
        awaitClose { videoCallsRef.removeEventListener(listener) }
    }
    
    // Signaling methods for WebRTC
    fun sendSignal(signal: CallSignal) {
        val signalRef = signalingRef.child(signal.callId).push()
        signalRef.setValue(signal)
    }
    
    fun listenForSignals(callId: String, userId: String): Flow<CallSignal> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val signal = child.getValue(CallSignal::class.java)
                    if (signal != null && signal.toUserId == userId) {
                        trySend(signal)
                        // Remove the signal after processing
                        child.ref.removeValue()
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        signalingRef.child(callId).addValueEventListener(listener)
        awaitClose { signalingRef.child(callId).removeEventListener(listener) }
    }
    
    // Send call request
    fun sendCallRequest(callId: String, fromUserId: String, toUserId: String) {
        val signal = CallSignal(
            signalType = SignalType.CALL_REQUEST,
            callId = callId,
            fromUserId = fromUserId,
            toUserId = toUserId
        )
        sendSignal(signal)
    }
    
    // Send call accept
    fun sendCallAccept(callId: String, fromUserId: String, toUserId: String) {
        val signal = CallSignal(
            signalType = SignalType.CALL_ACCEPT,
            callId = callId,
            fromUserId = fromUserId,
            toUserId = toUserId
        )
        sendSignal(signal)
    }
    
    // Send call reject
    fun sendCallReject(callId: String, fromUserId: String, toUserId: String) {
        val signal = CallSignal(
            signalType = SignalType.CALL_REJECT,
            callId = callId,
            fromUserId = fromUserId,
            toUserId = toUserId
        )
        sendSignal(signal)
    }
    
    // Send call end
    fun sendCallEnd(callId: String, fromUserId: String, toUserId: String) {
        val signal = CallSignal(
            signalType = SignalType.CALL_END,
            callId = callId,
            fromUserId = fromUserId,
            toUserId = toUserId
        )
        sendSignal(signal)
    }
    
    // Send SDP offer
    fun sendSdpOffer(callId: String, fromUserId: String, toUserId: String, sdp: String) {
        val signal = CallSignal(
            signalType = SignalType.SDP_OFFER,
            callId = callId,
            fromUserId = fromUserId,
            toUserId = toUserId,
            data = sdp
        )
        sendSignal(signal)
    }
    
    // Send SDP answer
    fun sendSdpAnswer(callId: String, fromUserId: String, toUserId: String, sdp: String) {
        val signal = CallSignal(
            signalType = SignalType.SDP_ANSWER,
            callId = callId,
            fromUserId = fromUserId,
            toUserId = toUserId,
            data = sdp
        )
        sendSignal(signal)
    }
    
    // Send ICE candidate
    fun sendIceCandidate(callId: String, fromUserId: String, toUserId: String, candidate: String) {
        val signal = CallSignal(
            signalType = SignalType.ICE_CANDIDATE,
            callId = callId,
            fromUserId = fromUserId,
            toUserId = toUserId,
            data = candidate
        )
        sendSignal(signal)
    }
    
    suspend fun deleteCall(callId: String) {
        videoCallsRef.child(callId).removeValue().await()
        signalingRef.child(callId).removeValue().await()
    }
} 