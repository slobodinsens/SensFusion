@file:Suppress("DEPRECATION")

package com.example.sensfusion

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private var isFlashOn = false
    private var isSosActive = false
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private val handler = Handler(Looper.getMainLooper())
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private val SOS_PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d(TAG, "FCM Token: $token")
            Toast.makeText(baseContext, "FCM Token: $token", Toast.LENGTH_SHORT).show()
        }

        // Initialize widgets
        val settingsWidget: CardView = findViewById(R.id.settingsWidget)
        val stolenCarWidget: CardView = findViewById(R.id.stolenCarWidget)
        val carNumberWidget: CardView = findViewById(R.id.carNumberWidget)
        val barrierWidget: CardView = findViewById(R.id.barrierWidget)
        val sosWidget: CardView = findViewById(R.id.sosWidget)
        val flashWidget: CardView = findViewById(R.id.flashWidget)

        // Load animation
        val widgetClickAnimation = AnimationUtils.loadAnimation(this, R.anim.widget_click_animation)

        // Initialize CameraManager
        try {
            cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            if (cameraManager.cameraIdList.isNotEmpty()) {
                cameraId = cameraManager.cameraIdList[0]
            } else {
                Toast.makeText(this, "No camera available", Toast.LENGTH_SHORT).show()
                flashWidget.isEnabled = false
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to initialize camera", Toast.LENGTH_SHORT).show()
            flashWidget.isEnabled = false
        }

        // Vibration method
        fun vibrate() {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
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
            checkSosPermissions()
        }

        flashWidget.setOnClickListener {
            it.startAnimation(widgetClickAnimation)
            vibrate()
            checkCameraPermissions()
        }
    }

    private fun checkCameraPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            showFlashModeDialog()
        }
    }

    private fun checkSosPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), SOS_PERMISSION_REQUEST_CODE)
        } else {
            showSosConfirmationDialog()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showFlashModeDialog()
                } else {
                    Toast.makeText(this, "Camera permission is required for flashlight", Toast.LENGTH_SHORT).show()
                }
            }
            SOS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showSosConfirmationDialog()
                } else {
                    Toast.makeText(this, "Phone permission is required for SOS calls", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun toggleFlashlight() {
        try {
            if (!::cameraId.isInitialized) {
                Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show()
                return
            }
            isFlashOn = !isFlashOn
            cameraManager.setTorchMode(cameraId, isFlashOn)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to toggle flashlight: ${e.message}", Toast.LENGTH_SHORT).show()
            isFlashOn = false
        }
    }

    private fun startSosFlashlight() {
        if (isSosActive) return
        if (!::cameraId.isInitialized) {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show()
            return
        }
        
        isSosActive = true
        val sosPattern = listOf(
            Pair(3, 2), // 3 short flashes at 2 Hz (0.5 sec)
            Pair(3, 1), // 3 long flashes at 1 Hz (1 sec)
            Pair(3, 2)  // 3 short flashes at 2 Hz (0.5 sec)
        )

        var sequenceIndex = 0
        var flashCount = 0

        handler.post(object : Runnable {
            override fun run() {
                if (!isSosActive) return

                if (sequenceIndex >= sosPattern.size) {
                    sequenceIndex = 0
                }

                val (count, frequency) = sosPattern[sequenceIndex]
                val duration = (1000L / frequency) / 2

                if (flashCount < count * 2) {
                    val isFlashOn = flashCount % 2 == 0
                    try {
                        cameraManager.setTorchMode(cameraId, isFlashOn)
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Failed to control flashlight: ${e.message}", Toast.LENGTH_SHORT).show()
                        stopSosFlashlight()
                        return
                    }
                    flashCount++
                    handler.postDelayed(this, duration)
                } else {
                    flashCount = 0
                    sequenceIndex++
                    handler.post(this)
                }
            }
        })
    }

    private fun stopSosFlashlight() {
        isSosActive = false
        try {
            if (::cameraId.isInitialized) {
                cameraManager.setTorchMode(cameraId, false)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to stop flashlight: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFlashModeDialog() {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Flashlight Mode")
            .setMessage("Choose the flashlight mode:")
            .setPositiveButton(if (isFlashOn) "Turn Off" else "Normal") { _, _ ->
                toggleFlashlight()
            }
            .setNegativeButton(if (isSosActive) "Stop SOS" else "SOS") { _, _ ->
                if (isSosActive) {
                    stopSosFlashlight()
                } else {
                    startSosFlashlight()
                }
            }
            .setNeutralButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }

    private fun showSosConfirmationDialog() {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Emergency Call")
            .setMessage("Are you sure you want to call SOS?")
            .setPositiveButton("Confirm") { _, _ ->
                val callIntent = Intent(Intent.ACTION_CALL)
                callIntent.data = Uri.parse("tel:999")
                try {
                    startActivity(callIntent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to initiate call: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSosFlashlight()
        handler.removeCallbacksAndMessages(null)
    }
}
