package com.baothanhbin.instagrambin

import android.app.Application
import android.content.Context
import com.baothanhbin.instagrambin.service.WebRTCService
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InstagramBinApplication : Application() {
    
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
} 