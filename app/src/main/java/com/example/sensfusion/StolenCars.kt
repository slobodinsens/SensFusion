package com.example.sensfusion

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class StolenCars : AppCompatActivity() {

    private lateinit var imageCapture: ImageCapture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stolen_cars)

        val previewView = findViewById<PreviewView>(R.id.previewView)
        val captureButton = findViewById<Button>(R.id.captureButton)

        // Инициализируем CameraX
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Выбираем заднюю камеру
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Настройка предварительного просмотра
                val preview = androidx.camera.core.Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // Настройка для захвата изображений
                imageCapture = ImageCapture.Builder()
                    .setTargetRotation(previewView.display.rotation)
                    .build()

                // Привязываем камеру к жизненному циклу
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            } catch (e: Exception) {
                Log.e("CameraX", "Ошибка запуска камеры", e)
                Toast.makeText(this, "Не удалось открыть камеру", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))

        // Обработчик кнопки фиксации изображения
        captureButton.setOnClickListener {
            takePhoto()
        }
    }

    private fun takePhoto() {
        val photoFile = File(
            externalMediaDirs.firstOrNull(),
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Toast.makeText(this@StolenCars, "Фото сохранено: ${photoFile.absolutePath}", Toast.LENGTH_SHORT).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", "Ошибка сохранения фото: ${exception.message}", exception)
                    Toast.makeText(this@StolenCars, "Ошибка сохранения фото", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
