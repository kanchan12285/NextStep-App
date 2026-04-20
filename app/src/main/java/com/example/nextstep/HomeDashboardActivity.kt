package com.example.nextstep

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.nextstep.ui.setOnClickWithSound
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.nextstep.audio.AudioManager

class HomeDashboardActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var ivHamburgerMenu: ImageView
    private lateinit var ivSettingsIcon: ImageView
    private lateinit var llUserProfileHeader: LinearLayout
    private lateinit var imgUserAvatar: ImageView
    private lateinit var tvWelcomeMessage: TextView
    private lateinit var tvProfileSummary: TextView

    private lateinit var btnFindYourself: Button
    private lateinit var btnMiniGames: Button
    private lateinit var btnPlayWithFriend: Button
    private lateinit var btnPlayOnline: Button
    private lateinit var btnKnowMeCard: Button
    private lateinit var btnCareForYou: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Let the layout extend into the status bar area
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = Color.TRANSPARENT

        setContentView(R.layout.activity_home_dashboard)

        // Optional: hide default action bar title (custom toolbar used)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Initialize Firebase and views
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        ivHamburgerMenu = findViewById(R.id.ivHamburgerMenu)
        ivSettingsIcon = findViewById(R.id.ivSettingsIcon)
        llUserProfileHeader = findViewById(R.id.llUserProfileHeader)
        imgUserAvatar = findViewById(R.id.imgUserAvatar)
        tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage)
        tvProfileSummary = findViewById(R.id.tvProfileSummary)

        btnFindYourself = findViewById(R.id.btnFindYourself)
        btnMiniGames = findViewById(R.id.btnMiniGames)
        btnPlayWithFriend = findViewById(R.id.btnPlayWithFriend)
        btnPlayOnline = findViewById(R.id.btnPlayOnline)
        btnKnowMeCard = findViewById(R.id.btnKnowMeCard)
        btnCareForYou = findViewById(R.id.btnCareForYou)

        navigationView.setNavigationItemSelectedListener(this)

        ivHamburgerMenu.setOnClickWithSound {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        ivSettingsIcon.setOnClickWithSound {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        loadUserData()
        setupButtonListeners()
    }



    private fun loadUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val username = document.getString("fullName")
                        val avatarName = document.getString("avatar") ?: "avatar_1"
                        val avatarRes = when (avatarName) {
                            "avatar_1" -> R.drawable.avatar_1
                            "avatar_2" -> R.drawable.avatar_2
                            "avatar_3" -> R.drawable.avatar_3
                            "avatar_4" -> R.drawable.avatar_4
                            "avatar_5" -> R.drawable.avatar_5
                            else -> R.drawable.avatar_1
                        }
                        imgUserAvatar.setImageResource(avatarRes)
                        tvWelcomeMessage.text = "Hello, ${username ?: "Explorer"}!"
                        tvProfileSummary.text = "Ready for your next step?"
                    } else {
                        tvWelcomeMessage.text = "Hello, Explorer!"
                        imgUserAvatar.setImageResource(R.drawable.avatar_1)
                        tvProfileSummary.text = "Ready for your next step?"
                    }
                }
                .addOnFailureListener {
                    tvWelcomeMessage.text = "Hello, Explorer!"
                    imgUserAvatar.setImageResource(R.drawable.avatar_1)
                    tvProfileSummary.text = "Ready for your next step?"
                }
        }
    }

    private fun setupButtonListeners() {
        llUserProfileHeader.setOnClickWithSound {
            startActivity(Intent(this, SettingsActivity::class.java).apply {
                putExtra("scrollTo", "profile")
            })
        }
        btnFindYourself.setOnClickWithSound {
            startActivity(Intent(this, GameActivity::class.java))
        }
        btnMiniGames.setOnClickWithSound {
            startActivity(Intent(this, MiniGamesListActivity::class.java))
        }
        btnPlayWithFriend.setOnClickWithSound {
            val intent = Intent(this, MultiplayerActivity::class.java)
            intent.putExtra("MODE", "FRIENDS")
            startActivity(intent)
            Toast.makeText(this, "🤝 Create room codes to play with friends!", Toast.LENGTH_SHORT).show()
        }
        btnPlayOnline.setOnClickWithSound {
            val intent = Intent(this, PlayOnlineActivity::class.java)
            intent.putExtra("MODE", "ONLINE")
            startActivity(intent)
            Toast.makeText(this, "🌍 Finding random opponents online!", Toast.LENGTH_SHORT).show()
        }
        btnKnowMeCard.setOnClickWithSound {
            startActivity(Intent(this, PersonalityCardActivity::class.java))
            Toast.makeText(this, "📋 Generating your personality card...", Toast.LENGTH_SHORT).show()
        }
        btnCareForYou.setOnClickWithSound {
            startActivity(Intent(this, CAREActivity::class.java))
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout.closeDrawer(GravityCompat.START)
        when (item.itemId) {
            R.id.nav_home -> { /* Already home */ }
            R.id.nav_find_yourself -> startActivity(Intent(this, GameActivity::class.java))
            R.id.nav_mini_games -> startActivity(Intent(this, MiniGamesListActivity::class.java))
            R.id.nav_my_journey -> startActivity(Intent(this, MyProgressActivity::class.java))
            R.id.nav_profile -> startActivity(Intent(this, SettingsActivity::class.java).apply { putExtra("scrollTo", "profile") })
            R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.nav_report_issues -> startActivity(Intent(this, ReportIssuesActivity::class.java))
            R.id.nav_help_faq -> startActivity(Intent(this, HelpFaqActivity::class.java))
            R.id.nav_reset_progress -> startActivity(Intent(this, SettingsActivity::class.java).apply { putExtra("scrollTo", "reset_progress") })
            else -> Toast.makeText(this, "Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
