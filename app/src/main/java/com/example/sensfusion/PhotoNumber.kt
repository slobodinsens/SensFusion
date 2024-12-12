@file:Suppress("DEPRECATION")

package com.example.sensfusion

import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class PhotoNumber : AppCompatActivity() {

    private val CAMERA_REQUEST_CODE = 102
    private val GALLERY_REQUEST_CODE = 103
    private val serverUrl = "http://10.0.0.43:5000/process"
    private var photoUri: Uri? = null
    private lateinit var selectedImageView: ImageView
    private lateinit var inputEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.photo_number)

        selectedImageView = findViewById(R.id.selectedImageView)
        inputEditText = findViewById(R.id.editTextNumber)
        val openCameraButton: Button = findViewById(R.id.openCameraButton)
        val openGalleryButton: Button = findViewById(R.id.openCameraButton3)
        val sendPhotoButton: Button = findViewById(R.id.send_photo)
        val sendButton: Button = findViewById(R.id.sendButton)

        // Обработчик для захвата фото с камеры
        openCameraButton.setOnClickListener {
            val uri = createImageUri()
            if (uri != null) {
                photoUri = uri
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                }
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
            } else {
                Toast.makeText(this, "Failed to create image file.", Toast.LENGTH_SHORT).show()
            }
        }

        // Обработчик для открытия галереи
        openGalleryButton.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
        }

        // Обработчик для отправки текста на сервер
        sendButton.setOnClickListener {
            val text = inputEditText.text.toString().trim()
            if (text.isNotBlank()) {
                sendTextToServer(text) { response ->
                    runOnUiThread {
                        Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Введите текст для отправки", Toast.LENGTH_SHORT).show()
            }
        }

        // Обработчик для отправки фото на сервер
        sendPhotoButton.setOnClickListener {
            photoUri?.let { uri ->
                sendImageToServer(uri) { response ->
                    runOnUiThread {
                        Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
                    }
                }
            } ?: Toast.makeText(this, "No photo to send.", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    photoUri?.let { uri ->
                        val bitmap = contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it)
                        }
                        selectedImageView.setImageBitmap(bitmap)
                    } ?: Toast.makeText(this, "Failed to capture image.", Toast.LENGTH_SHORT).show()
                }
                GALLERY_REQUEST_CODE -> {
                    photoUri = data?.data
                    photoUri?.let { uri ->
                        val bitmap = contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it)
                        }
                        selectedImageView.setImageBitmap(bitmap)
                    } ?: Toast.makeText(this, "No image selected.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createImageUri(): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_${getTimeStamp()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SensFusion")
        }
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
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

    private fun getTimeStamp(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return timeStamp.format(Date())
    }
}
