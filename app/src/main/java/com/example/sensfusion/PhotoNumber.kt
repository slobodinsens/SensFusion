@file:Suppress("DEPRECATION")

package com.example.sensfusion

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class PhotoNumber : AppCompatActivity() {

    private val GALLERY_REQUEST_CODE = 101
    private val serverUrl = "http://10.0.0.43:5000/process" // Укажите IP-адрес сервера

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.photo_number)

        // Инициализация компонентов
        val editTextNumber: EditText = findViewById(R.id.editTextNumber)
        val sendButton: Button = findViewById(R.id.sendButton)
        val openGalleryButton: Button = findViewById(R.id.openCameraButton)

        // Обработчик нажатия кнопки Send
        sendButton.setOnClickListener {
            val enteredText = editTextNumber.text.toString()
            if (enteredText.length == 8) {
                sendTextToServer(enteredText) { response ->
                    runOnUiThread {
                        Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
                    }
                }
            }
              else {
                Toast.makeText(this, "Please enter exactly 8 characters.", Toast.LENGTH_SHORT).show()
            }
        }

        // Обработчик нажатия кнопки для открытия галереи
        openGalleryButton.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            if (galleryIntent.resolveActivity(packageManager) != null) {
                startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
            } else {
                Toast.makeText(this, "No gallery app found.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            if (selectedImageUri != null) {
                sendImageToServer(selectedImageUri) { response ->
                    runOnUiThread {
                        Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "No image selected.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendTextToServer(text: String, callback: (String) -> Unit) {
        val url = "$serverUrl/api/send-text"
        val requestBody = FormBody.Builder()
            .add("text", text)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback("Failed to send text")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        callback("Text sent successfully: ${response.body?.string()}")
                    } else {
                        callback("Error sending text: ${response.code}")
                    }
                }
            }
        })
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
