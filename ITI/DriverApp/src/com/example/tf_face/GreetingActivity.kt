package com.example.tf_face

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.tf_face.home.HomeFragment
import com.example.tf_face.ac.AcFragment
import com.example.tf_face.appgrid.AppGridFragment
import com.example.tf_face.notification.NotificationsFragment
import com.example.tf_face.seat.SeatFragment
import com.example.tf_face.R
import kotlin.random.Random

class GreetingActivity : AppCompatActivity() {

    private lateinit var profileIcon: ImageView
    private lateinit var username: TextView
    private lateinit var navHome: ImageView
    private lateinit var navAc: ImageView
    private lateinit var navSeat: ImageView
    private lateinit var navAppGrid: ImageView
    private lateinit var navNotifications: ImageView
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.d("GreetingActivity", "onCreate called, savedInstanceState=$savedInstanceState")
    try {
        // Enable full-screen mode
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_main)

        // Initialize views with null checks
        profileIcon = findViewById(R.id.profile_icon) ?: run {
            Log.e("GreetingActivity", "profile_icon is null")
            finish()
            return
        }
        username = findViewById(R.id.username) ?: run {
            Log.e("GreetingActivity", "username is null")
            finish()
            return
        }
        navHome = findViewById(R.id.nav_home) ?: run {
            Log.e("GreetingActivity", "nav_home is null")
            finish()
            return
        }
        navAc = findViewById(R.id.nav_ac) ?: run {
            Log.e("GreetingActivity", "nav_ac is null")
            finish()
            return
        }
        navSeat = findViewById(R.id.nav_seat) ?: run {
            Log.e("GreetingActivity", "nav_seat is null")
            finish()
            return
        }
        navAppGrid = findViewById(R.id.nav_app_grid) ?: run {
            Log.e("GreetingActivity", "nav_app_grid is null")
            finish()
            return
        }
        navNotifications = findViewById(R.id.nav_notifications) ?: run {
            Log.e("GreetingActivity", "nav_notifications is null")
            finish()
            return
        }

        // Set click listeners
        navHome.setOnClickListener {
            Log.d("GreetingActivity", "NavHome clicked")
            updateNavigationSelection(navHome)
            loadHomeFragment(
                intent.getStringExtra("user_name") ?: "Guest User",
                intent.getIntExtra("user_age", 0),
                intent.getStringExtra("user_gender") ?: "unknown",
                intent.getFloatExtra("user_weight", 70f)
            )
        }
        navAc.setOnClickListener {
            Log.d("GreetingActivity", "NavAc clicked")
            updateNavigationSelection(navAc)
            loadFragment(AcFragment())
        }
        navSeat.setOnClickListener {
            Log.d("GreetingActivity", "NavSeat clicked")
            updateNavigationSelection(navSeat)
            loadFragment(SeatFragment())
        }
        navAppGrid.setOnClickListener {
            Log.d("GreetingActivity", "NavAppGrid clicked")
            updateNavigationSelection(navAppGrid)
            loadFragment(AppGridFragment())
        }
        navNotifications.setOnClickListener {
            Log.d("GreetingActivity", "NavNotifications clicked")
            updateNavigationSelection(navNotifications)
            loadFragment(NotificationsFragment())
        }

        // Load initial HomeFragment with user data
        loadHomeFragment(
            intent.getStringExtra("user_name") ?: "Guest User",
            intent.getIntExtra("user_age", 0),
            intent.getStringExtra("user_gender") ?: "unknown",
            intent.getFloatExtra("user_weight", 70f)
        )
    } catch (e: Exception) {
        Log.e("GreetingActivity", "Failed to initialize activity", e)
        finish()
    }
}

    private fun loadHomeFragment(userName: String, userAge: Int, userGender: String, userWeight: Float) {
        try {
            val homeFragment = HomeFragment().apply {
                arguments = Bundle().apply {
                    putString("user_name", userName)
                    putInt("user_age", userAge)
                    putString("user_gender", userGender)
                    putFloat("user_weight", userWeight)
                }
            }
            loadFragment(homeFragment)
        } catch (e: Exception) {
            Log.e("GreetingActivity", "Failed to load HomeFragment", e)
        }
    }

    private fun updateNavigationSelection(selectedView: ImageView) {
        try {
            // Reset all icons to default color
            navHome.setColorFilter(resources.getColor(R.color.default_nav_color))
            navAc.setColorFilter(resources.getColor(R.color.default_nav_color))
            navSeat.setColorFilter(resources.getColor(R.color.default_nav_color))
            navAppGrid.setColorFilter(resources.getColor(R.color.default_nav_color))
            navNotifications.setColorFilter(resources.getColor(R.color.default_nav_color))

            // Highlight the selected icon
            selectedView.setColorFilter(resources.getColor(R.color.selected_nav_color))
        } catch (e: Exception) {
            Log.e("GreetingActivity", "Failed to update navigation selection", e)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    private fun loadFragment(fragment: Fragment) {
        try {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.fade_in,
                    R.anim.fade_out,
                    R.anim.fade_in,
                    R.anim.fade_out
                )
                .replace(R.id.fragment_container, fragment)
                .commit()
        } catch (e: Exception) {
            Log.e("GreetingActivity", "Failed to load fragment", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            coroutineScope.cancel()
        } catch (e: Exception) {
            Log.e("GreetingActivity", "Failed to cancel coroutineScope", e)
        }
    }
}