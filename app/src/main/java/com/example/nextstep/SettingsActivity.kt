package com.example.nextstep

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager as SysAudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.example.nextstep.ui.setOnClickWithSound

class SettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var audioManager: com.example.nextstep.audio.AudioManager? = null

    private val TAG = "SettingsActivity"
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var ivBackArrow: ImageView
    private lateinit var scrollView: ScrollView
    private lateinit var profileSection: LinearLayout
    private lateinit var etName: EditText
    private lateinit var etAge: EditText
    private lateinit var imgCurrentAvatar: ImageView
    private lateinit var btnChangeAvatar: Button
    private lateinit var btnSaveProfile: Button
    private lateinit var notificationSection: LinearLayout
    private lateinit var switchNotifications: Switch
    private lateinit var appThemeSoundSection: LinearLayout
    private lateinit var switchAppMusic: Switch
    private lateinit var switchAppSoundEffects: Switch
    private lateinit var seekBarMusicVolume: SeekBar
    private lateinit var seekBarSfxVolume: SeekBar
    private lateinit var privacySecuritySection: LinearLayout
    private lateinit var btnViewPrivacyPolicy: Button
    private lateinit var switchDataSharing: Switch
    private lateinit var btnDeleteAccount: Button
    private lateinit var helpFaqSection: LinearLayout
    private lateinit var btnHelpAndFaq: Button
    private lateinit var btnReportBug: Button
    private lateinit var btnContactUs: Button
    private lateinit var aboutAppSection: LinearLayout
    private lateinit var btnAboutApp: Button
    private lateinit var resetProgressSection: LinearLayout
    private lateinit var btnResetMyProgress: Button

    private var isProcessing = false
    private var updatingUi = false

    private val audioPrefs by lazy { getSharedPreferences("audio_settings", Context.MODE_PRIVATE) }

    // Optional: request/abandon focus when music is toggled for better UX if your custom AudioManager doesn’t already handle it
    private val sysAudio by lazy { getSystemService(Context.AUDIO_SERVICE) as SysAudioManager }
    private val focusAttrs by lazy {
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }
    private val focusRequest by lazy {
        if (Build.VERSION.SDK_INT >= 26) {
            AudioFocusRequest.Builder(SysAudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(focusAttrs)
                .setOnAudioFocusChangeListener { change ->
                    Log.d("AudioManager", "Audio focus change: $change") // Tag to help Logcat filtering
                }
                .build()
        } else null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        try {
            Log.d(TAG, "Starting SettingsActivity initialization...")
            initializeComponents()
            initUI()
            loadAudioFromManagerOrPrefs()
            applyUiStateFromAudio()
            setupListeners()
            Log.d(TAG, "SettingsActivity initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            showErrorAndFinish("Failed to initialize settings: ${e.message}")
        }
    }



    private fun initializeComponents() {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        try {
            audioManager = com.example.nextstep.audio.AudioManager.instance()
            Log.d(TAG, "AudioManager connected successfully")
        } catch (e: Exception) {
            Log.w(TAG, "AudioManager not available: ${e.message}")
            audioManager = null
        }
    }

    private fun initUI() {
        ivBackArrow = findViewById(R.id.ivBackArrow)
        scrollView = findViewById(R.id.scrollView)
        profileSection = findViewById(R.id.profile_section)
        etName = findViewById(R.id.etName)
        etAge = findViewById(R.id.etAge)
        imgCurrentAvatar = findViewById(R.id.imgCurrentAvatar)
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        notificationSection = findViewById(R.id.notification_section)
        switchNotifications = findViewById(R.id.switchNotifications)
        appThemeSoundSection = findViewById(R.id.app_theme_sound_section)
        switchAppMusic = findViewById(R.id.switchAppMusic)
        switchAppSoundEffects = findViewById(R.id.switchAppSoundEffects)
        seekBarMusicVolume = findViewById(R.id.seekBarMusicVolume)
        seekBarSfxVolume = findViewById(R.id.seekBarSfxVolume)
        privacySecuritySection = findViewById(R.id.privacy_security_section)
        btnViewPrivacyPolicy = findViewById(R.id.btnViewPrivacyPolicy)
        switchDataSharing = findViewById(R.id.switchDataSharing)
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount)
        helpFaqSection = findViewById(R.id.help_faq_section)
        btnHelpAndFaq = findViewById(R.id.btnHelpAndFaq)
        btnReportBug = findViewById(R.id.btnReportBug)
        btnContactUs = findViewById(R.id.btnContactUs)
        aboutAppSection = findViewById(R.id.about_app_section)
        btnAboutApp = findViewById(R.id.btnAboutApp)
        resetProgressSection = findViewById(R.id.reset_progress_section)
        btnResetMyProgress = findViewById(R.id.btnResetMyProgress)
    }

    private fun loadAudioFromManagerOrPrefs() {
        val am = audioManager
        if (am != null) {
            // Pull authoritative state from custom audio manager
            musicEnabled = am.isMusicEnabled
            sfxEnabled = am.isSfxEnabled
            musicVolume = am.getMusicVolume()
            sfxVolume = am.getSfxVolume()
            Log.d(TAG, "Loaded audio from AudioManager: music=$musicEnabled sfx=$sfxEnabled mv=$musicVolume sv=$sfxVolume")
        } else {
            // Fallback to persisted prefs
            musicEnabled = audioPrefs.getBoolean("music_enabled", true)
            sfxEnabled = audioPrefs.getBoolean("sound_effects_enabled", true)
            musicVolume = audioPrefs.getFloat("music_volume", 0.7f)
            sfxVolume = audioPrefs.getFloat("sfx_volume", 1.0f)
            Log.d(TAG, "Loaded audio from prefs: music=$musicEnabled sfx=$sfxEnabled mv=$musicVolume sv=$sfxVolume")
        }
    }

    private var musicEnabled: Boolean = true
    private var sfxEnabled: Boolean = true
    private var musicVolume: Float = 0.7f
    private var sfxVolume: Float = 1.0f

    private fun applyUiStateFromAudio() {
        updatingUi = true
        switchAppMusic.isChecked = musicEnabled
        switchAppSoundEffects.isChecked = sfxEnabled
        seekBarMusicVolume.progress = (musicVolume.coerceIn(0f, 1f) * 100f).toInt()
        seekBarSfxVolume.progress = (sfxVolume.coerceIn(0f, 1f) * 100f).toInt()
        seekBarMusicVolume.isEnabled = musicEnabled
        seekBarSfxVolume.isEnabled = sfxEnabled
        updatingUi = false
    }

    private fun persistAudioPrefs() {
        audioPrefs.edit()
            .putBoolean("music_enabled", musicEnabled)
            .putBoolean("sound_effects_enabled", sfxEnabled)
            .putFloat("music_volume", musicVolume)
            .putFloat("sfx_volume", sfxVolume)
            .apply()
        Log.d(TAG, "Persisted audio prefs: music=$musicEnabled sfx=$sfxEnabled mv=$musicVolume sv=$sfxVolume")
    }

    private fun applyToAudioManager() {
        audioManager?.let { am ->
            try {
                am.setMusicEnabled(musicEnabled)
                am.setSfxEnabled(sfxEnabled)
                am.setMusicVolume(musicVolume)
                am.setSfxVolume(sfxVolume)
                Log.d("AudioManager", "Applied to AudioManager: music=$musicEnabled sfx=$sfxEnabled mv=$musicVolume sv=$sfxVolume")
                if (musicEnabled) requestAudioFocusIfNeeded() else abandonAudioFocusIfNeeded()
            } catch (e: Exception) {
                Log.e(TAG, "Error applying to AudioManager: ${e.message}", e)
            }
        }
    }

    private fun requestAudioFocusIfNeeded() {
        // Only if your custom AudioManager does NOT already request focus internally
        try {
            if (Build.VERSION.SDK_INT >= 26 && focusRequest != null) {
                val result = sysAudio.requestAudioFocus(focusRequest!!)
                Log.d("AudioManager", "requestAudioFocus result=$result")
            } else {
                @Suppress("DEPRECATION")
                val result = sysAudio.requestAudioFocus(
                    { change -> Log.d("AudioManager", "Audio focus change (legacy): $change") },
                    SysAudioManager.STREAM_MUSIC,
                    SysAudioManager.AUDIOFOCUS_GAIN
                )
                Log.d("AudioManager", "requestAudioFocus (legacy) result=$result")
            }
        } catch (e: Exception) {
            Log.e(TAG, "requestAudioFocus error: ${e.message}", e)
        }
    }

    private fun abandonAudioFocusIfNeeded() {
        try {
            if (Build.VERSION.SDK_INT >= 26 && focusRequest != null) {
                val result = sysAudio.abandonAudioFocusRequest(focusRequest!!)
                Log.d("AudioManager", "abandonAudioFocus result=$result")
            } else {
                @Suppress("DEPRECATION")
                val result = sysAudio.abandonAudioFocus(null)
                Log.d("AudioManager", "abandonAudioFocus (legacy) result=$result")
            }
        } catch (e: Exception) {
            Log.e(TAG, "abandonAudioFocus error: ${e.message}", e)
        }
    }

    private fun setupListeners() {
        ivBackArrow.setOnClickWithSound {
            if (!isProcessing) {
                onBackPressedDispatcher.onBackPressed()
            }
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isProcessing || updatingUi) return@setOnCheckedChangeListener
            debounceAction {
                try {
                    saveNotificationPreference(isChecked)
                    Toast.makeText(this, "Notifications: ${if (isChecked) "On" else "Off"}", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Error toggling notifications: ${e.message}", e)
                }
            }
        }

        switchAppMusic.setOnCheckedChangeListener { _, isChecked ->
            if (isProcessing || updatingUi) return@setOnCheckedChangeListener
            debounceAction {
                musicEnabled = isChecked
                seekBarMusicVolume.isEnabled = isChecked
                applyToAudioManager()
                persistAudioPrefs()
                Toast.makeText(this, "Music: ${if (isChecked) "On" else "Off"}", Toast.LENGTH_SHORT).show()
            }
        }

        switchAppSoundEffects.setOnCheckedChangeListener { _, isChecked ->
            if (isProcessing || updatingUi) return@setOnCheckedChangeListener
            debounceAction {
                sfxEnabled = isChecked
                seekBarSfxVolume.isEnabled = isChecked
                applyToAudioManager()
                persistAudioPrefs()
                if (isChecked) audioManager?.playClickSound()
                Toast.makeText(this, "Sound Effects: ${if (isChecked) "On" else "Off"}", Toast.LENGTH_SHORT).show()
            }
        }

        seekBarMusicVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !isProcessing && !updatingUi) {
                    musicVolume = progress.toFloat() / 100f
                    audioManager?.setMusicVolume(musicVolume)
                    Log.d("AudioManager", "Music volume -> $musicVolume")
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { persistAudioPrefs() }
        })

        seekBarSfxVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !isProcessing && !updatingUi) {
                    sfxVolume = progress.toFloat() / 100f
                    audioManager?.setSfxVolume(sfxVolume)
                    Log.d("AudioManager", "SFX volume -> $sfxVolume")
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                audioManager?.playClickSound()
                persistAudioPrefs()
            }
        })

        btnSaveProfile.setOnClickWithSound { saveProfileSettings() }
        btnChangeAvatar.setOnClickWithSound { showAvatarPicker() }
        btnDeleteAccount.setOnClickWithSound { showDeleteAccountConfirmation() }
        btnHelpAndFaq.setOnClickWithSound {
            try { startActivity(Intent(this, HelpFaqActivity::class.java)) }
            catch (e: Exception) {
                Log.e(TAG, "Error starting HelpFaqActivity: ${e.message}", e)
                Toast.makeText(this, "Help & FAQ not available", Toast.LENGTH_SHORT).show()
            }
        }
        btnReportBug.setOnClickWithSound { startActivity(Intent(this, ReportIssuesActivity::class.java)) }
        btnContactUs.setOnClickWithSound {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@nextstepgaming.com")
                putExtra(Intent.EXTRA_SUBJECT, "NextStep Gaming - Support Request")
                putExtra(Intent.EXTRA_TEXT, "Hello NextStep Team,\n\nI need help with:\n\n")
            }
            startActivity(Intent.createChooser(emailIntent, "Send Email"))
        }
        btnAboutApp.setOnClickWithSound { startActivity(Intent(this, AboutAppActivity::class.java)) }
        btnViewPrivacyPolicy.setOnClickWithSound { startActivity(Intent(this, PrivacyPolicyActivity::class.java)) }
        btnResetMyProgress.setOnClickWithSound { showResetProgressConfirmation() }
    }

    private fun loadUserSettings() {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    try {
                        if (document.exists()) {
                            etName.setText(document.getString("fullName") ?: "")
                            etAge.setText(document.getLong("age")?.toString() ?: "")
                            val avatarResName = document.getString("avatar") ?: "avatar_1"
                            val avatarResId = resources.getIdentifier(avatarResName, "drawable", packageName)
                            if (avatarResId != 0) {
                                imgCurrentAvatar.setImageResource(avatarResId)
                            } else {
                                imgCurrentAvatar.setImageResource(R.drawable.avatar_1)
                            }
                            switchNotifications.isChecked = document.getBoolean("notification_enabled") ?: true
                            switchDataSharing.isChecked = document.getBoolean("privacy_dataSharing") ?: false
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing user data: ${e.message}", e)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error loading user settings: ${e.message}", e)
                    Toast.makeText(this, "Failed to load some settings.", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadUserSettings: ${e.message}", e)
        }
    }

    private fun debounceAction(action: () -> Unit) {
        if (isProcessing) return
        isProcessing = true
        handler.postDelayed({
            try { action() } finally { isProcessing = false }
        }, 300)
    }

    private fun saveProfileSettings() {
        if (isProcessing) return
        debounceAction {
            try {
                val userId = auth.currentUser?.uid ?: return@debounceAction
                val name = etName.text.toString().trim()
                val age = etAge.text.toString().trim().toIntOrNull()

                val updates = mutableMapOf<String, Any?>()
                updates["fullName"] = name
                updates["age"] = age

                db.collection("users").document(userId).set(updates, SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profile settings saved!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error saving profile settings: ${e.message}", e)
                        Toast.makeText(this, "Failed to save profile settings", Toast.LENGTH_LONG).show()
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in saveProfileSettings: ${e.message}", e)
                Toast.makeText(this, "Error saving profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveNotificationPreference(isChecked: Boolean) {
        if (isProcessing) return
        debounceAction {
            try {
                val userId = auth.currentUser?.uid ?: return@debounceAction
                val updates = mapOf("notification_enabled" to isChecked)

                db.collection("users").document(userId).set(updates, SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(this, "Notification preference saved!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error saving notification preference: ${e.message}", e)
                        Toast.makeText(this, "Failed to save notification preference", Toast.LENGTH_LONG).show()
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in saveNotificationPreference: ${e.message}", e)
                Toast.makeText(this, "Error saving notifications", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveDataSharingPreference(isChecked: Boolean) {
        try {
            val userId = auth.currentUser?.uid ?: return

            db.collection("users").document(userId).update("privacy_dataSharing", isChecked)
                .addOnSuccessListener {
                    Toast.makeText(this, "Data sharing preference saved!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving data sharing preference: ${e.message}", e)
                    Toast.makeText(this, "Failed to update data sharing preference", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in saveDataSharingPreference: ${e.message}", e)
        }
    }

    private fun showAvatarPicker() {
        if (isProcessing) return
        try {
            val avatarDrawables = listOf(
                R.drawable.avatar_1,
                R.drawable.avatar_2,
                R.drawable.avatar_3,
                R.drawable.avatar_4,
                R.drawable.avatar_5
            )

            val avatarDialogView = layoutInflater.inflate(R.layout.dialog_avatar_picker, null)
            val avatarContainer = avatarDialogView.findViewById<LinearLayout>(R.id.avatarContainer)

            val dialog = AlertDialog.Builder(this)
                .setTitle("Choose Your Avatar")
                .setView(avatarDialogView)
                .setNegativeButton("Cancel", null)
                .create()

            avatarContainer.removeAllViews()

            for (drawableRes in avatarDrawables) {
                val imageView = ImageView(this).apply {
                    setImageResource(drawableRes)
                    layoutParams = LinearLayout.LayoutParams(120.dpToPx(), 120.dpToPx()).apply {
                        setMargins(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
                    }
                    background = getDrawable(R.drawable.avatar_selector_background)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setOnClickListener {
                        imgCurrentAvatar.setImageResource(drawableRes)
                        saveAvatarResource(drawableRes)
                        dialog.dismiss()
                    }
                }
                avatarContainer.addView(imageView)
            }

            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing avatar picker: ${e.message}", e)
            Toast.makeText(this, "Error opening avatar picker", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAvatarResource(drawableRes: Int) {
        try {
            val userId = auth.currentUser?.uid ?: return
            val resourceName = resources.getResourceEntryName(drawableRes)
            db.collection("users").document(userId).update("avatar", resourceName)
                .addOnSuccessListener {
                    Toast.makeText(this, "Avatar updated!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error updating avatar: ${e.message}", e)
                    Toast.makeText(this, "Failed to update avatar", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in saveAvatarResource: ${e.message}", e)
        }
    }

    private fun showResetProgressConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Reset Progress")
            .setMessage("Are you sure you want to reset all your progress? This action cannot be undone.")
            .setPositiveButton("Reset") { _, _ -> resetUserProgress() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetUserProgress() {
        try {
            val userId = auth.currentUser?.uid ?: return
            val progressUpdates = mapOf(
                "totalScore" to 0,
                "gamesPlayed" to 0,
                "achievements" to emptyList<String>(),
                "level" to 1,
                "experience" to 0
            )
            db.collection("users").document(userId).set(progressUpdates, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Progress reset successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error resetting progress: ${e.message}", e)
                    Toast.makeText(this, "Failed to reset progress", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in resetUserProgress: ${e.message}", e)
            Toast.makeText(this, "Error resetting progress", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently lost.")
            .setPositiveButton("Delete") { _, _ -> deleteUserAccount() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUserAccount() {
        try {
            val user = auth.currentUser
            val userId = user?.uid ?: return
            db.collection("users").document(userId).delete()
                .addOnSuccessListener {
                    user.delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error deleting auth account: ${e.message}", e)
                            Toast.makeText(this, "Failed to delete account", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error deleting user data: ${e.message}", e)
                    Toast.makeText(this, "Failed to delete account data", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in deleteUserAccount: ${e.message}", e)
            Toast.makeText(this, "Error deleting account", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showErrorAndFinish(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e(TAG, message)
        finish()
    }

    private fun Int.dpToPx(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
