package com.example.sensfusion

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sensfusion.R

class PhotoNumber : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_number)

        // Инициализация компонентов
        val editTextNumber: EditText = findViewById(R.id.editTextNumber)
        val sendButton: Button = findViewById(R.id.sendButton)
        val openCameraButton: Button = findViewById(R.id.openCameraButton)

        // Обработчик нажатия кнопки Send
        sendButton.setOnClickListener {
            val enteredText = editTextNumber.text.toString()
            if (enteredText.length == 8) {
                // Логика отправки текста
                Toast.makeText(this, "Text sent: $enteredText", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter exactly 8 characters.", Toast.LENGTH_SHORT).show()
            }
        }

        // Обработчик нажатия кнопки для открытия камеры
        openCameraButton.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (cameraIntent.resolveActivity(packageManager) != null) {
                startActivity(cameraIntent)
            } else {
                Toast.makeText(this, "No camera app found.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
