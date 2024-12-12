package com.example.sensfusion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Проверка разрешения для уведомлений (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        // Инициализация кнопок
        val settingsButton: Button = findViewById(R.id.settingsButton)
        val stolenCarButton: Button = findViewById(R.id.stolenCarButton)
        val carNumberButton: Button = findViewById(R.id.carNumberButton)

        // Загрузка анимации
        val buttonClickAnimation = AnimationUtils.loadAnimation(this, R.anim.button_click_animation)

        // Обработчик нажатия на кнопку Settings
        settingsButton.setOnClickListener {
            it.startAnimation(buttonClickAnimation)
            // Переход в EmailPassword Activity
            val intent = Intent(this, EmailPassword::class.java)
            startActivity(intent)
        }

        // Обработчик нажатия на кнопку Stolen Car
        stolenCarButton.setOnClickListener {
            it.startAnimation(buttonClickAnimation)
            val intent = Intent(this, StolenCars::class.java)
            startActivity(intent)
        }

        // Обработчик нажатия на кнопку Car Number
        carNumberButton.setOnClickListener {
            it.startAnimation(buttonClickAnimation) // Анимация кнопки

            // Переход на другую активность (CarNumberActivity)
            val intent = Intent(this, PhotoNumber::class.java)
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notifications permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
