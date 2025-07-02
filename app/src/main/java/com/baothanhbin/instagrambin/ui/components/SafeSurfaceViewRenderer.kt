package com.baothanhbin.instagrambin.ui.components

import android.content.Context
import android.util.Log
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

/**
 * Safe wrapper for SurfaceViewRenderer to prevent crashes
 */
class SafeSurfaceViewRenderer(context: Context) : SurfaceViewRenderer(context) {
    private val TAG = "SafeSurfaceViewRenderer"
    private var isInitialized = false
    private var isReleased = false
    private val lock = Object()
    private var sinkAdded = false
    
    /**
     * Safely initialize the SurfaceViewRenderer
     */
    fun safeInit(eglBase: EglBase?, rendererEvents: RendererCommon.RendererEvents? = null): Boolean {
        return synchronized(lock) {
            try {
                if (isReleased) {
                    Log.w(TAG, "Cannot initialize - already released")
                    return@synchronized false
                }
                
                if (isInitialized) {
                    Log.w(TAG, "Already initialized")
                    return@synchronized true
                }
                
                if (eglBase == null) {
                    Log.e(TAG, "EGL base is null")
                    return@synchronized false
                }
                
                // Check if EGL context is valid
                if (eglBase.eglBaseContext == null) {
                    Log.e(TAG, "EGL context is null")
                    return@synchronized false
                }
                
                init(eglBase.eglBaseContext, rendererEvents)
                isInitialized = true
                Log.d(TAG, "SurfaceViewRenderer initialized successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing SurfaceViewRenderer", e)
                false
            }
        }
    }
    
    /**
     * Safely add video sink
     */
    fun safeAddSink(videoTrack: VideoTrack?): Boolean = synchronized(lock) {
        try {
            if (isReleased || !isInitialized || videoTrack == null) return@synchronized false
            videoTrack.addSink(this)
            sinkAdded = true
            Log.d(TAG, "Video sink added successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding video sink", e)
            false
        }
    }
    
    /**
     * Safely remove video sink
     */
    fun safeRemoveSink(videoTrack: VideoTrack?): Boolean = synchronized(lock) {
        try {
            if (isReleased || !isInitialized || videoTrack == null || !sinkAdded) return@synchronized false
            
            // Kiểm tra thêm null pointer
            if (videoTrack.id() == null) {
                Log.w(TAG, "VideoTrack ID is null, skipping removeSink")
                sinkAdded = false
                return@synchronized true
            }
            
            try {
                videoTrack.removeSink(this)
                Log.d(TAG, "Video sink removed successfully")
            } catch (nativeCrash: Throwable) {
                Log.e(TAG, "Native crash when removing video sink, ignore to prevent app crash", nativeCrash)
                // swallow native crash
            }
            sinkAdded = false
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error removing video sink", e)
            sinkAdded = false
            false
        }
    }
    
    /**
     * Safely release the SurfaceViewRenderer
     */
    fun safeRelease(): Boolean = synchronized(lock) {
        try {
            if (isReleased) return@synchronized true
            if (isInitialized) {
                sinkAdded = false
                release()
                Log.d(TAG, "SurfaceViewRenderer released successfully")
            } else {
                Log.w(TAG, "Not initialized, skipping release")
            }
            isReleased = true
            isInitialized = false
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing SurfaceViewRenderer", e)
            false
        }
    }
    
    /**
     * Check if the SurfaceViewRenderer is initialized
     */
    fun isInitialized(): Boolean = synchronized(lock) { isInitialized && !isReleased }
    
    /**
     * Check if the SurfaceViewRenderer is released
     */
    fun isReleased(): Boolean = synchronized(lock) { isReleased }
} 