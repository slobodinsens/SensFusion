@file:Suppress("DEPRECATION")

package com.example.sensfusion

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var isFlashOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize widgets
        val settingsWidget: CardView = findViewById(R.id.settingsWidget)
        val stolenCarWidget: CardView = findViewById(R.id.stolenCarWidget)
        val carNumberWidget: CardView = findViewById(R.id.carNumberWidget)
        val barrierWidget: CardView = findViewById(R.id.barrierWidget)
        val sosWidget: CardView = findViewById(R.id.sosWidget)
        val flashWidget: CardView = findViewById(R.id.flashWidget)

        // Load animation
        val widgetClickAnimation = AnimationUtils.loadAnimation(this, R.anim.widget_click_animation)

        // Vibration method
        fun vibrate() {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }

        // Flashlight toggle method
        fun toggleFlashlight() {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            try {
                val cameraId = cameraManager.cameraIdList[0]
                isFlashOn = !isFlashOn
                cameraManager.setTorchMode(cameraId, isFlashOn)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Set onClick listeners for the first row
        settingsWidget.setOnClickListener {
            it.startAnimation(widgetClickAnimation)
            vibrate()
            val intent = Intent(this, EmailPassword::class.java)
            startActivity(intent)
        }

        stolenCarWidget.setOnClickListener {
            it.startAnimation(widgetClickAnimation)
            vibrate()
            val intent = Intent(this, StolenCars::class.java)
            startActivity(intent)
        }

        carNumberWidget.setOnClickListener {
            it.startAnimation(widgetClickAnimation)
            vibrate()
            val intent = Intent(this, PhotoNumber::class.java)
            startActivity(intent)
        }

        // Set onClick listeners for the second row
        barrierWidget.setOnClickListener {
            it.startAnimation(widgetClickAnimation)
            vibrate()
            val intent = Intent(this, BarrierActivity::class.java)
            startActivity(intent)
        }

        sosWidget.setOnClickListener {
            it.startAnimation(widgetClickAnimation)
            vibrate()
            // Check call permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 1)
            } else {
                // Start phone call
                val callIntent = Intent(Intent.ACTION_CALL)
                callIntent.data = Uri.parse("tel:999")
                startActivity(callIntent)
            }
        }

        flashWidget.setOnClickListener {
            it.startAnimation(widgetClickAnimation)
            vibrate()
            toggleFlashlight()
        }
    }
}
