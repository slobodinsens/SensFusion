package com.example.sensfusion


import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.example.sensfusion.R



class SplashActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Настройка VideoView для воспроизведения MP4
        val videoView = findViewById<VideoView>(R.id.videoView)
        val videoUri = Uri.parse("android.resource://${packageName}/${R.raw.splash_video}") // Замените "splash_video" на имя вашего файла MP4
        videoView.setVideoURI(videoUri)

        // Запускаем видео
        videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.isLooping = true // Включить зацикливание, если нужно
        }
        videoView.start()

        // Переход к MainActivity после 3 секунд
        videoView.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3000) // 3000ms = 3 секунды
    }
}
