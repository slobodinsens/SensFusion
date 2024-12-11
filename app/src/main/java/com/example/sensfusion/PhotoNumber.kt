@file:Suppress("DEPRECATION")

package com.example.sensfusion

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException

class PhotoNumber : AppCompatActivity() {

    private val GALLERY_REQUEST_CODE = 101
    private val CAMERA_REQUEST_CODE = 102
    private val serverUrl = "http://10.0.0.43:5000/process"
    private var selectedImageUri: Uri? = null
    private lateinit var selectedImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.photo_number)

        // Инициализация компонентов
        selectedImageView = findViewById(R.id.selectedImageView)
        val sendPhotoButton: Button = findViewById(R.id.send_photo)
        val openGalleryButton: Button = findViewById(R.id.openCameraButton3)
        val openCameraButton: Button = findViewById(R.id.openCameraButton)

        // Обработчик для выбора фото из галереи
        openGalleryButton.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            if (galleryIntent.resolveActivity(packageManager) != null) {
                startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
            } else {
                Toast.makeText(this, "No gallery app found.", Toast.LENGTH_SHORT).show()
            }
        }

        // Обработчик для захвата фото с камеры
        openCameraButton.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (cameraIntent.resolveActivity(packageManager) != null) {
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
            } else {
                Toast.makeText(this, "No camera app found.", Toast.LENGTH_SHORT).show()
            }
        }

        // Обработчик для отправки фото на сервер
        sendPhotoButton.setOnClickListener {
            if (selectedImageUri != null) {
                sendImageToServer(selectedImageUri!!) { response ->
                    runOnUiThread {
                        Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please select an image first.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {
                    selectedImageUri = data?.data
                    if (selectedImageUri != null) {
                        selectedImageView.setImageURI(selectedImageUri)
                    }
                }
                CAMERA_REQUEST_CODE -> {
                    val photoBitmap: Bitmap? = data?.extras?.get("data") as Bitmap?
                    if (photoBitmap != null) {
                        // Отображение фото в ImageView
                        selectedImageView.setImageBitmap(photoBitmap)

                        // Сохранение изображения для отправки на сервер
                        selectedImageUri = getImageUri(photoBitmap)
                    }
                }
            }
        }
    }

    private fun getImageUri(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "CapturedImage", null)
        return Uri.parse(path)
    }

    private fun sendImageToServer(imageUri: Uri, callback: (String) -> Unit) {
        val url = "$serverUrl/api/upload-image"

        val inputStream = contentResolver.openInputStream(imageUri)
        val imageData = inputStream?.readBytes() ?: return callback("Failed to read image")

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image",
                "uploaded_image.jpg",
                imageData.toRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback("Failed to upload image")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        callback("Image uploaded successfully: ${response.body?.string()}")
                    } else {
                        callback("Error uploading image: ${response.code}")
                    }
                }
            }
        })
    }
}
