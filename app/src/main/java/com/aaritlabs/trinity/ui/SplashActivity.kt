package com.aaritlabs.trinity.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.OvershootInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.aaritlabs.trinity.R
import com.aaritlabs.trinity.audio.SoundManager
import com.aaritlabs.trinity.data.GamePreferences
import com.aaritlabs.trinity.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var prefs: GamePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = GamePreferences(this)
        prefs.applySavedTheme()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Safe Area View Insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.root.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }

        // Preload sound manager
        val soundManager = SoundManager.getInstance(this)

        // Splash Animations
        binding.ivSplashLogo.scaleX = 0.5f
        binding.ivSplashLogo.scaleY = 0.5f
        binding.ivSplashLogo.alpha = 0f

        binding.tvSplashTitle.alpha = 0f
        binding.tvSplashTitle.translationY = 60f

        binding.tvSplashTagline.alpha = 0f

        // 1. Logo pop & bounce
        binding.ivSplashLogo.animate()
            .alpha(1f)
            .scaleX(1.12f)
            .scaleY(1.12f)
            .setDuration(700)
            .setInterpolator(OvershootInterpolator(2.0f))
            .withEndAction {
                binding.ivSplashLogo.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(400)
                    .start()
            }
            .start()

        // 2. Title slide up & fade
        binding.tvSplashTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(250)
            .start()

        // 3. Tagline fade in
        binding.tvSplashTagline.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(500)
            .start()

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.anim_fade_in, R.anim.anim_fade_out)
            finish()
        }, 1600)
    }
}
