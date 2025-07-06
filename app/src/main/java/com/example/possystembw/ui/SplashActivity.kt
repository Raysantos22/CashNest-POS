package com.example.possystembw.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.example.possystembw.DeviceUtils
import com.example.possystembw.R

class SplashActivity : AppCompatActivity() {
    private lateinit var videoView: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set orientation based on device type BEFORE setContentView
        DeviceUtils.setOrientationBasedOnDevice(this)

        setContentView(R.layout.activity_splash)

        // Hide the action bar if it exists
        supportActionBar?.hide()

        // Initialize VideoView
        videoView = findViewById(R.id.splashVideo)

        // Set video path (video file should be in res/raw folder)
        val videoPath = "android.resource://" + packageName + "/" + R.raw.white1
        val uri = Uri.parse(videoPath)
        videoView.setVideoURI(uri)

        // Start playing the video
        videoView.start()

        // Loop the video when it completes
        videoView.setOnCompletionListener { mediaPlayer ->
            videoView.start()
        }

        // Navigate to LoginActivity after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToLogin()
        }, 3000)

        // Optional: Skip video on touch
        videoView.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        if (!isFinishing) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        videoView.stopPlayback()
        super.onDestroy()
    }
}