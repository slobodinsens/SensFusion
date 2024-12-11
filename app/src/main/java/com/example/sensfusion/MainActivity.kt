package com.example.sensfusion


import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.sensfusion.R



class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            // Логика для Stolen Car (замените этот комментарий вашим кодом)
        }

        // Обработчик нажатия на кнопку Car Number
        carNumberButton.setOnClickListener {
            it.startAnimation(buttonClickAnimation)
            // Логика для Car Number (замените этот комментарий вашим кодом)
        }
    }
}
