package com.example.possystembw.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.example.possystembw.R

class SplashActivity : AppCompatActivity() {
    private lateinit var videoView: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        // Navigate to LoginActivity after 5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToLogin()
        }, 3000) // 5 seconds

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

//class SplashActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_splash)
//
//        // Hide the action bar
//        supportActionBar?.hide()
//
//        // Get the ImageView
//        val splashImage: ImageView = findViewById(R.id.splashImage)
//
//        // Load and start the animation
//        val animation = AnimationUtils.loadAnimation(this, R.anim.splash_animation)
//        splashImage.startAnimation(animation)
//
//        // Navigate to LoginActivity after delay
//        Handler(Looper.getMainLooper()).postDelayed({
//            startActivity(Intent(this, LoginActivity::class.java))
//            finish()
//        }, 3000) // 3 seconds
//    }
//}




//<?xml version="1.0" encoding="utf-8"?>
//<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
//xmlns:app="http://schemas.android.com/apk/res-auto"
//android:layout_width="match_parent"
//android:layout_height="match_parent"
//android:background="@color/black">
//
//<VideoView
//android:id="@+id/splashVideo"
//android:layout_width="match_parent"
//android:layout_height="match_parent"
//app:layout_constraintBottom_toBottomOf="parent"
//app:layout_constraintEnd_toEndOf="parent"
//app:layout_constraintStart_toStartOf="parent"
//app:layout_constraintTop_toTopOf="parent" />
//
//</androidx.constraintlayout.widget.ConstraintLayout>


//
//<?xml version="1.0" encoding="utf-8"?>
//<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
//xmlns:app="http://schemas.android.com/apk/res-auto"
//android:layout_width="match_parent"
//android:layout_height="match_parent"
//android:background="@color/white">
//
//<ImageView
//android:id="@+id/splashImage"
//android:layout_width="200dp"
//android:layout_height="200dp"
//android:src="@mipmap/intro"
//app:layout_constraintBottom_toBottomOf="parent"
//app:layout_constraintEnd_toEndOf="parent"
//app:layout_constraintStart_toStartOf="parent"
//app:layout_constraintTop_toTopOf="parent" />
//
//<TextView
//android:layout_width="wrap_content"
//android:layout_height="wrap_content"
//android:text="ECPOS"
//android:textSize="24sp"
//android:textStyle="bold"
//android:textColor="@color/primary"
//android:layout_marginTop="16dp"
//app:layout_constraintTop_toBottomOf="@id/splashImage"
//app:layout_constraintStart_toStartOf="parent"
//app:layout_constraintEnd_toEndOf="parent" />
//
//</androidx.constraintlayout.widget.ConstraintLayout>