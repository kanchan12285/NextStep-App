package com.example.nextstep.ui

import com.example.nextstep.audio.AudioManager

object QuizSoundHelper {

    /**
     * Play correct answer sound (respects sound settings)
     */
    fun playCorrectAnswerSound() {
        try {
            if (AudioManager.instance().isSfxEnabled) {
                AudioManager.instance().playCorrectSound()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Play wrong answer sound (respects sound settings)
     */
    fun playWrongAnswerSound() {
        try {
            if (AudioManager.instance().isSfxEnabled) {
                AudioManager.instance().playWrongSound()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Handle quiz answer with automatic sound
     */
    fun handleQuizAnswer(isCorrect: Boolean, onAnswerProcessed: (Boolean) -> Unit) {
        if (isCorrect) {
            playCorrectAnswerSound()
        } else {
            playWrongAnswerSound()
        }

        // Execute callback after sound
        onAnswerProcessed(isCorrect)
    }
}
