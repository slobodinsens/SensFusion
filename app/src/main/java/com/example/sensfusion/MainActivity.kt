@file:Suppress("DEPRECATION")

package com.example.sensfusion

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация виджетов
        val settingsWidget: CardView = findViewById(R.id.settingsWidget)
        val stolenCarWidget: CardView = findViewById(R.id.stolenCarWidget)
        val carNumberWidget: CardView = findViewById(R.id.carNumberWidget)

        // Загрузка анимации
        val widgetClickAnimation = AnimationUtils.loadAnimation(this, R.anim.widget_click_animation)

        // Метод для вибрации
        fun vibrate() {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                // Вибрация длительностью 50 мс
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }

        // Обработчики нажатий
        settingsWidget.setOnClickListener {
            it.startAnimation(widgetClickAnimation) // Анимация кнопки
            vibrate() // Вибрация
            val intent = Intent(this, EmailPassword::class.java)
            startActivity(intent)
        }

        stolenCarWidget.setOnClickListener {
            it.startAnimation(widgetClickAnimation) // Анимация кнопки
            vibrate() // Вибрация
            val intent = Intent(this, StolenCars::class.java)
            startActivity(intent)
        }

        carNumberWidget.setOnClickListener {
            it.startAnimation(widgetClickAnimation) // Анимация кнопки
            vibrate() // Вибрация
            val intent = Intent(this, PhotoNumber::class.java)
            startActivity(intent)
        }
    }
}
