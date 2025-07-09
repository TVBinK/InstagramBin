package com.baothanhbin.instagrambin.service

import android.content.Context
import android.util.Log
import org.webrtc.*

/**
 * Helper class to safely handle WebRTC operations and prevent crashes
 */
object WebRTCHelper {
    private const val TAG = "WebRTCHelper"
    
    /**
     * Safely create EglBase with error handling
     */
    fun createEglBase(): EglBase? {
        return try {
            val eglBase = EglBase.create()
            Log.d(TAG, "EGL base created successfully")
            eglBase
        } catch (e: Exception) {
            Log.e(TAG, "Error creating EGL base", e)
            null
        }
    }
    
    /**
     * Safely check if EGL context is valid
     */
    fun isEglContextValid(eglBase: EglBase?): Boolean {
        return try {
            eglBase != null && eglBase.eglBaseContext != null
        } catch (e: Exception) {
            Log.w(TAG, "EGL context check failed", e)
            false
        }
    }
    
    /**
     * Safely initialize PeerConnectionFactory
     */
    fun initializePeerConnectionFactory(context: Context, eglBase: EglBase?): PeerConnectionFactory? {
        return try {
            if (eglBase == null || !isEglContextValid(eglBase)) {
                Log.e(TAG, "EGL base is null or invalid, cannot initialize PeerConnectionFactory")
                return null
            }
            
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .createInitializationOptions()
            )
            Log.d(TAG, "PeerConnectionFactory initialized")
            
            val options = PeerConnectionFactory.Options()
            val factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(DefaultVideoEncoderFactory(
                    eglBase.eglBaseContext, true, true
                ))
                .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
                .createPeerConnectionFactory()
            
            Log.d(TAG, "PeerConnectionFactory created successfully")
            factory
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing PeerConnectionFactory", e)
            null
        }
    }
    
    /**
     * Safely create camera capturer with specified camera type
     */
    fun createCameraCapturer(context: Context, useFrontCamera: Boolean = true): VideoCapturer? {
        return try {
            Log.d(TAG, "Creating camera capturer... useFrontCamera: $useFrontCamera")
            val enumerator = Camera2Enumerator(context)
            val deviceNames = enumerator.deviceNames
            Log.d(TAG, "Available cameras: ${deviceNames.size}")
            
            // Try preferred camera first
            for (deviceName in deviceNames) {
                Log.d(TAG, "Camera: $deviceName, front-facing: ${enumerator.isFrontFacing(deviceName)}")
                if (enumerator.isFrontFacing(deviceName) == useFrontCamera) {
                    val capturer = enumerator.createCapturer(deviceName, null)
                    if (capturer != null) {
                        Log.d(TAG, "${if (useFrontCamera) "Front" else "Back"} camera capturer created: $deviceName")
                        return capturer
                    }
                }
            }
            
            // Try fallback camera if preferred camera not available
            for (deviceName in deviceNames) {
                if (enumerator.isFrontFacing(deviceName) != useFrontCamera) {
                    val capturer = enumerator.createCapturer(deviceName, null)
                    if (capturer != null) {
                        Log.d(TAG, "${if (!useFrontCamera) "Front" else "Back"} camera capturer created as fallback: $deviceName")
                        return capturer
                    }
                }
            }
            
            Log.e(TAG, "No camera capturer could be created")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error creating camera capturer", e)
            null
        }
    }
    
    /**
     * Switch camera capturer (for CameraVideoCapturer)
     */
    fun switchCamera(capturer: VideoCapturer?): Boolean {
        return try {
            if (capturer is CameraVideoCapturer) {
                capturer.switchCamera(null)
                Log.d(TAG, "Camera switched successfully")
                true
            } else {
                Log.e(TAG, "Cannot switch camera - capturer is not CameraVideoCapturer")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error switching camera", e)
            false
        }
    }
    
    /**
     * Safely initialize video capturer
     */
    fun initializeVideoCapturer(
        capturer: VideoCapturer?,
        eglBase: EglBase?,
        context: Context,
        videoSource: VideoSource?
    ): Boolean {
        return try {
            if (capturer == null || eglBase == null || videoSource == null) {
                Log.e(TAG, "Cannot initialize video capturer - missing required components")
                return false
            }
            
            if (!isEglContextValid(eglBase)) {
                Log.e(TAG, "EGL context is invalid, cannot initialize capturer")
                return false
            }
            
            capturer.initialize(
                SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext),
                context,
                videoSource.capturerObserver
            )
            Log.d(TAG, "Video capturer initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing video capturer", e)
            false
        }
    }
    
    /**
     * Safely start video capture
     */
    fun startVideoCapture(capturer: VideoCapturer?, width: Int = 640, height: Int = 480, fps: Int = 30): Boolean {
        return try {
            if (capturer == null) {
                Log.e(TAG, "Cannot start video capture - capturer is null")
                return false
            }
            
            capturer.startCapture(width, height, fps)
            Log.d(TAG, "Video capturer started successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting video capture", e)
            false
        }
    }
    
    /**
     * Safely stop video capture
     */
    fun stopVideoCapture(capturer: VideoCapturer?): Boolean {
        return try {
            if (capturer == null) {
                Log.w(TAG, "Cannot stop video capture - capturer is null")
                return false
            }
            
            capturer.stopCapture()
            Log.d(TAG, "Video capturer stopped successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping video capture", e)
            false
        }
    }
    
    /**
     * Safely dispose video capturer
     */
    fun disposeVideoCapturer(capturer: VideoCapturer?): Boolean {
        return try {
            if (capturer == null) {
                Log.w(TAG, "Cannot dispose video capturer - capturer is null")
                return false
            }
            
            capturer.dispose()
            Log.d(TAG, "Video capturer disposed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing video capturer", e)
            false
        }
    }
    
    /**
     * Safely create video source
     */
    fun createVideoSource(factory: PeerConnectionFactory?): VideoSource? {
        return try {
            if (factory == null) {
                Log.e(TAG, "Cannot create video source - factory is null")
                return null
            }
            
            val videoSource = factory.createVideoSource(false)
            Log.d(TAG, "Video source created successfully")
            videoSource
        } catch (e: Exception) {
            Log.e(TAG, "Error creating video source", e)
            null
        }
    }
    
    /**
     * Safely create video track
     */
    fun createVideoTrack(factory: PeerConnectionFactory?, videoSource: VideoSource?, trackId: String = "ARDAMSv0"): VideoTrack? {
        return try {
            if (factory == null || videoSource == null) {
                Log.e(TAG, "Cannot create video track - factory or video source is null")
                return null
            }
            
            val videoTrack = factory.createVideoTrack(trackId, videoSource)
            Log.d(TAG, "Video track created successfully")
            videoTrack
        } catch (e: Exception) {
            Log.e(TAG, "Error creating video track", e)
            null
        }
    }
    
    /**
     * Safely create PeerConnection
     */
    fun createPeerConnection(
        factory: PeerConnectionFactory?,
        iceServers: List<PeerConnection.IceServer>,
        observer: PeerConnection.Observer
    ): PeerConnection? {
        return try {
            if (factory == null) {
                Log.e(TAG, "Cannot create PeerConnection - factory is null")
                return null
            }
            //Tạo "cầu nối" với cấu hình
            val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
            val peerConnection = factory.createPeerConnection(rtcConfig, observer)

            peerConnection
        } catch (e: Exception) {
            Log.e(TAG, "Error creating PeerConnection", e)
            null
        }
    }
    
    /**
     * Safely add track to PeerConnection
     */
    fun addTrackToPeerConnection(peerConnection: PeerConnection?, track: VideoTrack?): Boolean {
        return try {
            if (peerConnection == null || track == null) {
                Log.e(TAG, "Cannot add track - PeerConnection or track is null")
                return false
            }
            
            val result = peerConnection.addTrack(track)
            Log.d(TAG, "Track added to PeerConnection successfully")
            result != null // Return true if RtpSender is not null, false otherwise
        } catch (e: Exception) {
            Log.e(TAG, "Error adding track to PeerConnection", e)
            false
        }
    }
    
    /**
     * Safely close PeerConnection
     */
    fun closePeerConnection(peerConnection: PeerConnection?): Boolean {
        return try {
            if (peerConnection == null) {
                Log.w(TAG, "Cannot close PeerConnection - it is null")
                return false
            }
            
            peerConnection.close()
            Log.d(TAG, "PeerConnection closed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error closing PeerConnection", e)
            false
        }
    }
    
    /**
     * Safely dispose PeerConnectionFactory
     */
    fun disposePeerConnectionFactory(factory: PeerConnectionFactory?): Boolean {
        return try {
            if (factory == null) {
                Log.w(TAG, "Cannot dispose PeerConnectionFactory - it is null")
                return false
            }
            
            factory.dispose()
            Log.d(TAG, "PeerConnectionFactory disposed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing PeerConnectionFactory", e)
            false
        }
    }
    
    /**
     * Safely release EglBase
     */
    fun releaseEglBase(eglBase: EglBase?): Boolean {
        return try {
            if (eglBase == null) {
                Log.w(TAG, "Cannot release EglBase - it is null")
                return false
            }
            
            eglBase.release()
            Log.d(TAG, "EglBase released successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing EglBase", e)
            false
        }
    }
} 