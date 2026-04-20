package com.example.nextstep

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

class NextStepApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.d("NextStepApp", "Initializing app with audio system...")

        try {
            // Initialize AudioManager with error handling
            com.example.nextstep.audio.AudioManager.init(this)
            Log.d("NextStepApp", "AudioManager initialized successfully")

            // Register lifecycle observer
            ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
            Log.d("NextStepApp", "Lifecycle observer registered")

        } catch (e: Exception) {
            Log.e("NextStepApp", "Error initializing audio system: ${e.message}", e)
            // Don't crash - continue without audio if needed
        }
    }

    /**
     * Separate lifecycle observer to handle app lifecycle
     */
    private class AppLifecycleObserver : DefaultLifecycleObserver {

        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            try {
                com.example.nextstep.audio.AudioManager.instance().onAppStart()
                Log.d("NextStepApp", "Background music started")
            } catch (e: Exception) {
                Log.e("NextStepApp", "Error starting background music: ${e.message}")
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            try {
                com.example.nextstep.audio.AudioManager.instance().onAppStop()
                Log.d("NextStepApp", "Background music paused")
            } catch (e: Exception) {
                Log.e("NextStepApp", "Error pausing background music: ${e.message}")
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            try {
                com.example.nextstep.audio.AudioManager.instance().onAppDestroy()
                Log.d("NextStepApp", "Audio system destroyed")
            } catch (e: Exception) {
                Log.e("NextStepApp", "Error destroying audio system: ${e.message}")
            }
        }
    }
}
