package com.example.nextstep.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.core.content.edit
import com.example.nextstep.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioManager private constructor(private val appContext: Context) {

    companion object {
        @Volatile
        private var INSTANCE: AudioManager? = null

        fun init(context: Context): AudioManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun instance(): AudioManager {
            return requireNotNull(INSTANCE) {
                "AudioManager not initialized. Call AudioManager.init(context) first."
            }
        }

        private const val PREFS_NAME = "audio_settings"
        private const val KEY_MUSIC_ENABLED = "music_enabled"
        private const val KEY_SFX_ENABLED = "sound_effects_enabled"
        private const val KEY_MUSIC_VOLUME = "music_volume"
        private const val KEY_SFX_VOLUME = "sfx_volume"
    }

    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val soundPool: SoundPool
    private var themeMusic: MediaPlayer? = null

    // Sound effect IDs
    private var clickSoundId: Int = 0
    private var correctSoundId: Int = 0
    private var wrongSoundId: Int = 0

    // Settings
    var isMusicEnabled: Boolean = prefs.getBoolean(KEY_MUSIC_ENABLED, true)
        private set

    var isSfxEnabled: Boolean = prefs.getBoolean(KEY_SFX_ENABLED, true)
        private set

    private var musicVolume: Float = prefs.getFloat(KEY_MUSIC_VOLUME, 0.7f)
    private var sfxVolume: Float = prefs.getFloat(KEY_SFX_VOLUME, 1.0f)

    // Loading state
    private var isInitialized = false

    init {
        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()

        loadSoundEffects()
    }

    private fun loadSoundEffects() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                clickSoundId = soundPool.load(appContext, R.raw.button_click, 1)
                correctSoundId = soundPool.load(appContext, R.raw.correct_answer, 1)
                wrongSoundId = soundPool.load(appContext, R.raw.wrong_answer, 1)

                kotlinx.coroutines.delay(500)

                withContext(Dispatchers.Main) {
                    isInitialized = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Music Controls
    fun startThemeMusic() {
        if (!isMusicEnabled) return

        try {
            if (themeMusic?.isPlaying == true) return

            if (themeMusic == null) {
                themeMusic = MediaPlayer.create(appContext, R.raw.app_theme)?.apply {
                    isLooping = true
                    setVolume(musicVolume, musicVolume)
                    setOnErrorListener { _, _, _ ->
                        stopThemeMusic()
                        false
                    }
                }
            }

            themeMusic?.start()
        } catch (e: Exception) {
            e.printStackTrace()
            stopThemeMusic()
        }
    }

    fun stopThemeMusic() {
        try {
            themeMusic?.apply {
                if (isPlaying) stop()
                release()
            }
            themeMusic = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pauseThemeMusic() {
        try {
            if (themeMusic?.isPlaying == true) {
                themeMusic?.pause()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resumeThemeMusic() {
        if (!isMusicEnabled) return

        try {
            if (themeMusic != null && !themeMusic!!.isPlaying) {
                themeMusic?.start()
            } else {
                startThemeMusic()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Sound Effects
    fun playClickSound() {
        if (!isSfxEnabled || !isInitialized || clickSoundId == 0) return

        try {
            soundPool.play(clickSoundId, sfxVolume, sfxVolume, 1, 0, 1f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playCorrectSound() {
        if (!isSfxEnabled || !isInitialized || correctSoundId == 0) return

        try {
            soundPool.play(correctSoundId, sfxVolume, sfxVolume, 1, 0, 1f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playWrongSound() {
        if (!isSfxEnabled || !isInitialized || wrongSoundId == 0) return

        try {
            soundPool.play(wrongSoundId, sfxVolume, sfxVolume, 1, 0, 1f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Settings Management
    fun setMusicEnabled(enabled: Boolean) {
        isMusicEnabled = enabled
        prefs.edit { putBoolean(KEY_MUSIC_ENABLED, enabled) }

        if (enabled) {
            startThemeMusic()
        } else {
            stopThemeMusic()
        }
    }

    fun setSfxEnabled(enabled: Boolean) {
        isSfxEnabled = enabled
        prefs.edit { putBoolean(KEY_SFX_ENABLED, enabled) }
    }

    fun setMusicVolume(volume: Float) {
        musicVolume = volume.coerceIn(0f, 1f)
        prefs.edit { putFloat(KEY_MUSIC_VOLUME, musicVolume) }

        try {
            themeMusic?.setVolume(musicVolume, musicVolume)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setSfxVolume(volume: Float) {
        sfxVolume = volume.coerceIn(0f, 1f)
        prefs.edit { putFloat(KEY_SFX_VOLUME, sfxVolume) }
    }

    // Lifecycle Management
    fun onAppStart() {
        if (isMusicEnabled) {
            startThemeMusic()
        }
    }

    fun onAppStop() {
        pauseThemeMusic()
    }

    fun onAppDestroy() {
        stopThemeMusic()

        try {
            soundPool.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Getters
    fun getMusicVolume() = musicVolume
    fun getSfxVolume() = sfxVolume
    fun isAudioInitialized() = isInitialized
}
