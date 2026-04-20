package com.example.nextstep.ui

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.AppCompatButton
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.nextstep.audio.AudioManager

object GlobalSoundManager {

    /**
     * Apply click sounds to ALL buttons in an activity automatically
     * Call this in every activity's onCreate() after setContentView()
     */
    fun applyGlobalButtonSounds(activity: Activity) {
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        applyClickSoundsRecursively(rootView)
    }

    /**
     * Recursively find and apply click sounds to all button types
     */
    private fun applyClickSoundsRecursively(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)

            when (child) {
                // Handle all button types
                is MaterialButton, is AppCompatButton, is Button -> {
                    applyClickSoundToView(child)
                }
                is FloatingActionButton -> {
                    applyClickSoundToView(child)
                }
                // Handle clickable views (ImageViews, TextViews, etc.)
                is View -> {
                    if (child.isClickable && child.hasOnClickListeners()) {
                        applyClickSoundToView(child)
                    }
                }
            }

            // Recursively check child ViewGroups
            if (child is ViewGroup) {
                applyClickSoundsRecursively(child)
            }
        }
    }

    /**
     * Apply click sound to individual view while preserving existing click listener
     */
    private fun applyClickSoundToView(view: View) {
        val existingListener = view.getOnClickListener()

        view.setOnClickListener { v ->
            // Play click sound first (respects sound settings automatically)
            try {
                if (AudioManager.instance().isSfxEnabled) {
                    AudioManager.instance().playClickSound()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Execute original click listener
            existingListener?.onClick(v)
        }
    }

    /**
     * Helper extension to check if view has click listeners
     */
    private fun View.getOnClickListener(): View.OnClickListener? {
        return try {
            val field = View::class.java.getDeclaredField("mListenerInfo")
            field.isAccessible = true
            val listenerInfo = field.get(this)

            if (listenerInfo != null) {
                val clickField = listenerInfo.javaClass.getDeclaredField("mOnClickListener")
                clickField.isAccessible = true
                clickField.get(listenerInfo) as? View.OnClickListener
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
