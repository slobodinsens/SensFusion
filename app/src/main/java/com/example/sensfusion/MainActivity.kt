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
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var isFlashOn = false
    private var isSosActive = false
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private val handler = Handler(Looper.getMainLooper())

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

        // Initialize CameraManager
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList[0] // Use the first camera

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
            // Show confirmation dialog
            showSosConfirmationDialog()
        }

        flashWidget.setOnClickListener {
            it.startAnimation(widgetClickAnimation)
            vibrate()
            // Show flash mode selection dialog
            showFlashModeDialog()
        }
    }

    private fun toggleFlashlight() {
        try {
            isFlashOn = !isFlashOn
            cameraManager.setTorchMode(cameraId, isFlashOn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startSosFlashlight() {
        if (isSosActive) return // Если SOS уже активен, ничего не делать
        isSosActive = true

        val sosPattern = listOf(
            Pair(3, 2), // 3 короткие вспышки с частотой 2 Гц (0.5 сек)
            Pair(3, 1), // 3 длинные вспышки с частотой 1 Гц (1 сек)
            Pair(3, 2)  // 3 короткие вспышки с частотой 2 Гц (0.5 сек)
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
                        e.printStackTrace()
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
            cameraManager.setTorchMode(cameraId, false) // Выключаем фонарь
        } catch (e: Exception) {
            e.printStackTrace()
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
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 1)
                } else {
                    val callIntent = Intent(Intent.ACTION_CALL)
                    callIntent.data = Uri.parse("tel:999")
                    startActivity(callIntent)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }
}
