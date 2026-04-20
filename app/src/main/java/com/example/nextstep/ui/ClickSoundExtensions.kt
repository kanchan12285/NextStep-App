package com.example.nextstep.ui

import android.view.View
import com.example.nextstep.audio.AudioManager

/**
 * Extension function to add click sound to any View (most common usage)
 * Usage: button.setOnClickWithSound { /* your click action */ }
 */
fun View.setOnClickWithSound(action: () -> Unit) {
    setOnClickListener {
        try {
            AudioManager.instance().playClickSound()
            action()
        } catch (e: Exception) {
            // Fallback - just execute action without sound if AudioManager fails
            action()
        }
    }
}

/**
 * Extension function when you need access to the view parameter
 * Usage: button.setOnClickWithSoundAndView { view -> /* your click action */ }
 */
fun View.setOnClickWithSoundAndView(action: (View) -> Unit) {
    setOnClickListener { view ->
        try {
            AudioManager.instance().playClickSound()
            action(view)
        } catch (e: Exception) {
            // Fallback - just execute action without sound if AudioManager fails
            action(view)
        }
    }
}

/**
 * Extension function for buttons that need different sound effects
 * Usage: button.setOnClickWithCustomSound(SoundType.CORRECT) { /* action */ }
 */
enum class SoundType {
    CLICK, CORRECT, WRONG
}

fun View.setOnClickWithCustomSound(soundType: SoundType, action: () -> Unit) {
    setOnClickListener {
        try {
            when (soundType) {
                SoundType.CLICK -> AudioManager.instance().playClickSound()
                SoundType.CORRECT -> AudioManager.instance().playCorrectSound()
                SoundType.WRONG -> AudioManager.instance().playWrongSound()
            }
            action()
        } catch (e: Exception) {
            // Fallback - just execute action without sound if AudioManager fails
            action()
        }
    }
}

/**
 * Extension to play sound without click (for manual sound triggers)
 */
fun playSound(soundType: SoundType) {
    try {
        when (soundType) {
            SoundType.CLICK -> AudioManager.instance().playClickSound()
            SoundType.CORRECT -> AudioManager.instance().playCorrectSound()
            SoundType.WRONG -> AudioManager.instance().playWrongSound()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
