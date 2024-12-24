package com.example.sensfusion

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class CarouselActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carousel)

        val carouselView: CarouselView = findViewById(R.id.carouselView)

        // Запуск анимации
        carouselView.startAnimation(2f)

        // Переход после задержки
        carouselView.postDelayed({
            val intent = Intent(this, NextActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000) // Переход через 3 секунды
    }
}

class NextActivity {

}
