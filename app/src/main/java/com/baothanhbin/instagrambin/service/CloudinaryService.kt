package com.baothanhbin.instagrambin.service

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class CloudinaryService(private val context: Context) {
    companion object {
        private var isInitialized = false

        private fun initialize(context: Context) {
            if (!isInitialized) {
                val config = mapOf(
                    "cloud_name" to "dhhmc1htn",
                    "api_key" to "221486764667326",
                    "api_secret" to "1z53snGNF6nyhBW2efFeoiTauLk",
                    "secure" to true
                )
                MediaManager.init(context, config)
                isInitialized = true
            }
        }
    }

    init {
        initialize(context)
    }

    private fun isCloudinaryUrl(uri: Uri): Boolean {
        return uri.toString().contains("res.cloudinary.com")
    }

    private fun getFileFromUri(uri: Uri): File {
        // If it's a Cloudinary URL, return null
        if (isCloudinaryUrl(uri)) {
            throw IllegalArgumentException("Cannot process Cloudinary URL as local file")
        }

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Could not open input stream for URI: $uri")
            
        val fileName = "temp_${System.currentTimeMillis()}.jpg"
        val file = File(context.cacheDir, fileName)
        
        inputStream.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        
        return file
    }

    suspend fun uploadImage(imageUri: Uri): String = suspendCancellableCoroutine { continuation ->
        try {
            // If it's already a Cloudinary URL, return it directly
            if (isCloudinaryUrl(imageUri)) {
                continuation.resume(imageUri.toString())
                return@suspendCancellableCoroutine
            }

            // Convert Uri to File
            val file = getFileFromUri(imageUri)
            
            MediaManager.get()
                .upload(file.absolutePath)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d("Cloudinary", "Upload started")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        Log.d("Cloudinary", "Upload progress: $bytes/$totalBytes")
                    }

                    override fun onSuccess(requestId: String, resultData: Map<Any?, Any?>) {
                        val url = resultData["secure_url"] as? String
                        if (url != null) {
                            // Clean up temporary file
                            file.delete()
                            continuation.resume(url)
                        } else {
                            file.delete()
                            continuation.resumeWithException(Exception("Failed to get image URL"))
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        file.delete()
                        Log.e("Cloudinary", "Upload error: ${error.description}")
                        continuation.resumeWithException(Exception(error.description))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        file.delete()
                        Log.e("Cloudinary", "Upload rescheduled: ${error.description}")
                        continuation.resumeWithException(Exception(error.description))
                    }
                })
                .dispatch()
        } catch (e: Exception) {
            Log.e("Cloudinary", "Error in upload: ${e.message}", e)
            continuation.resumeWithException(e)
        }
    }
} 