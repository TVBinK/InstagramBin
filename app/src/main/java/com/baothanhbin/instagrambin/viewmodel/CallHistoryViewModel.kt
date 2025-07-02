package com.baothanhbin.instagrambin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baothanhbin.instagrambin.model.CallHistory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CallHistoryViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
    private val _callHistories = MutableStateFlow<List<CallHistory>>(emptyList())
    val callHistories: StateFlow<List<CallHistory>> = _callHistories.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private var callHistoryListener: ValueEventListener? = null
    
    fun loadCallHistory(userId: String) {
        currentUserId?.let { currentId ->
            _isLoading.value = true
            
            // Remove previous listener
            callHistoryListener?.let { listener ->
                database.getReference("call_history").removeEventListener(listener)
            }
            
            // Create conversation ID (sorted to ensure consistency)
            val conversationId = if (currentId < userId) "${currentId}_${userId}" else "${userId}_${currentId}"
            
            callHistoryListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val histories = mutableListOf<CallHistory>()
                    
                    snapshot.children.forEach { callSnapshot ->
                        callSnapshot.getValue(CallHistory::class.java)?.let { callHistory ->
                            histories.add(callHistory)
                        }
                    }
                    
                    // Sort by timestamp (newest first)
                    _callHistories.value = histories.sortedByDescending { it.timestamp }
                    _isLoading.value = false
                }
                
                override fun onCancelled(error: DatabaseError) {
                    _isLoading.value = false
                }
            }
            
            database.getReference("call_history")
                .child(conversationId)
                .addValueEventListener(callHistoryListener!!)
        }
    }
    
    fun saveCallStart(toUserId: String, callType: String = "video"): String {
        currentUserId?.let { currentId ->
            val conversationId = if (currentId < toUserId) "${currentId}_${toUserId}" else "${toUserId}_${currentId}"
            val callId = database.getReference("call_history").child(conversationId).push().key ?: return ""
            
            val callHistory = CallHistory(
                id = callId,
                fromUserId = currentId,
                toUserId = toUserId,
                callType = callType,
                status = "calling",
                startTime = System.currentTimeMillis(),
                timestamp = System.currentTimeMillis()
            )
            
            database.getReference("call_history")
                .child(conversationId)
                .child(callId)
                .setValue(callHistory)
            
            return callId
        }
        return ""
    }
    
    fun updateCallStatus(callId: String, otherUserId: String, status: String, endTime: Long = 0L) {
        currentUserId?.let { currentId ->
            val conversationId = if (currentId < otherUserId) "${currentId}_${otherUserId}" else "${otherUserId}_${currentId}"
            
            val updates = mutableMapOf<String, Any>(
                "status" to status
            )
            
            if (endTime > 0) {
                updates["endTime"] = endTime
                
                // Calculate duration if we have the call history
                database.getReference("call_history")
                    .child(conversationId)
                    .child(callId)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        snapshot.getValue(CallHistory::class.java)?.let { callHistory ->
                            val duration = endTime - callHistory.startTime
                            if (duration > 0) {
                                database.getReference("call_history")
                                    .child(conversationId)
                                    .child(callId)
                                    .child("duration")
                                    .setValue(duration)
                            }
                        }
                    }
            }
            
            database.getReference("call_history")
                .child(conversationId)
                .child(callId)
                .updateChildren(updates)
        }
    }
    
    fun markCallAsAnswered(callId: String, otherUserId: String) {
        updateCallStatus(callId, otherUserId, "answered")
    }
    
    fun markCallAsRejected(callId: String, otherUserId: String) {
        updateCallStatus(callId, otherUserId, "rejected")
    }
    
    fun markCallAsMissed(callId: String, otherUserId: String) {
        updateCallStatus(callId, otherUserId, "missed")
    }
    
    fun endCall(callId: String, otherUserId: String) {
        updateCallStatus(callId, otherUserId, "ended", System.currentTimeMillis())
    }
    
    override fun onCleared() {
        super.onCleared()
        callHistoryListener?.let { listener ->
            database.getReference("call_history").removeEventListener(listener)
        }
    }
} 