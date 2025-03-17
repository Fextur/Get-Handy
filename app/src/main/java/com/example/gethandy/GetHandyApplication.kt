package com.example.gethandy

import android.app.Application
import android.util.Log
import com.example.gethandy.data.local.AppDatabase

const val TAG = "GetHandyApplication"

class GetHandyApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()

        // Initialize Cloudinary here if needed
        initializeCloudinary()
    }

    private fun initializeCloudinary() {
        try {
            val config: HashMap<String, String> = hashMapOf(
                "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                "api_key" to BuildConfig.CLOUDINARY_API_KEY,
                "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
            )
            com.cloudinary.android.MediaManager.init(this, config)
        } catch (e: Exception) {
            Log.e("GetHandyApplication", "Error initializing Cloudinary")
        }
    }
}