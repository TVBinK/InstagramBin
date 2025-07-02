package com.baothanhbin.instagrambin.service

import android.app.Application
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class DataClearService(private val application: Application) {
    
    private val firebaseOptimizationService = FirebaseOptimizationService(application)
    
    /**
     * Clear tất cả cache và data khi đăng xuất
     */
    suspend fun clearAllData() {
        try {
            // Clear Firebase Optimization cache
            firebaseOptimizationService.forceClearAllData()
            
            // Clear Firebase Auth cache (nếu có)
            val auth = FirebaseAuth.getInstance()
            auth.signOut()
            
            // Clear local storage cache (nếu có)
            clearLocalCache()
            
        } catch (e: Exception) {
            // Log error but don't throw
            e.printStackTrace()
        }
    }
    
    /**
     * Clear local cache
     */
    private fun clearLocalCache() {
        try {
            // Clear application cache
            application.cacheDir.deleteRecursively()
            
            // Clear external cache if exists
            application.externalCacheDir?.deleteRecursively()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Clear specific user data
     */
    suspend fun clearUserData(userId: String) {
        try {
            // Clear user-specific cache
            firebaseOptimizationService.clearCache()
            
            // Clear Firebase Database cache for specific user
            FirebaseDatabase.getInstance().purgeOutstandingWrites()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Force refresh all data
     */
    suspend fun forceRefreshAllData() {
        try {
            // Clear all cache
            firebaseOptimizationService.clearCache()
            
            // Purge Firebase writes
            FirebaseDatabase.getInstance().purgeOutstandingWrites()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 