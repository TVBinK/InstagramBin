package com.baothanhbin.instagrambin

import android.app.Application
import android.content.Context
import com.baothanhbin.instagrambin.service.WebRTCService
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import coil.disk.DiskCache

class InstagramBinApplication : Application(), ImageLoaderFactory {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase Database persistence before any other usage
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        
        // Alternative way using Firebase KTX
        // Firebase.database.setPersistenceEnabled(true)
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        // Cleanup WebRTCService when app is terminated
        WebRTCService.destroyInstance()
    }

    fun getNewWebRTCService(context: Context): WebRTCService {
        CoroutineScope(Dispatchers.IO).launch {
            WebRTCService.destroyInstance()
        }
        return WebRTCService.getInstance(context)
    }
    
    // Tối ưu Coil ImageLoader cho performance
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 25% của available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // 2% của disk space
                    .build()
            }
            .respectCacheHeaders(false) // Luôn cache để tăng performance
            .crossfade(true) // Enable crossfade mặc định
            .crossfade(200) // Crossfade duration mặc định
            .build()
    }
} 