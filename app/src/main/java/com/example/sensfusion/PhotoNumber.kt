@file:Suppress("DEPRECATION")

package com.example.sensfusion

import android.Manifest
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class PhotoNumber : AppCompatActivity() {

    // Server URL - Replace with your computer's local IP address
    private val serverUrl = "https://192.168.1.108:5000"  // For Android Emulator
    // If using a physical device, use your computer's local IP address like:
    // private val serverUrl = "http://192.168.1.xxx:8443"  // Replace xxx with your actual IP
    private val sendTextUrl = "$serverUrl/api/send-text"
    private val uploadImageUrl = "$serverUrl/api/upload-image"

    // Request Codes
    private val CAMERA_REQUEST_CODE = 102
    private val GALLERY_REQUEST_CODE = 103
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001

    private var photoUri: Uri? = null
    private lateinit var selectedImageView: ImageView
    private lateinit var inputEditText: EditText
    private var fcmToken: String? = null
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.photo_number)

        // Initialize UI components
        selectedImageView = findViewById(R.id.selectedImageView)
        inputEditText = findViewById(R.id.editTextNumber)
        val openCameraButton: Button = findViewById(R.id.openCameraButton)
        val openGalleryButton: Button = findViewById(R.id.openCameraButton3)
        val sendPhotoButton: Button = findViewById(R.id.send_photo)
        val sendButton: Button = findViewById(R.id.sendButton)

        // Initialize progress dialog
        progressDialog = ProgressDialog(this).apply {
            setMessage("Uploading...")
            setCancelable(false)
        }

        // Request notification permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            }
        }

        // Get FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM Token", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            fcmToken = task.result
            Log.d("FCM Token", "Token: $fcmToken")
        }

        // Open camera
        openCameraButton.setOnClickListener {
            val uri = createImageUri()
            if (uri != null) {
                photoUri = uri
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                }
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
            } else {
                showSnackbar(it, "Failed to create image file.")
            }
        }

        // Open gallery
        openGalleryButton.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
        }

        // Send text to server
        sendButton.setOnClickListener { view ->
            val text = inputEditText.text.toString().trim()
            if (text.isNotBlank()) {
                val uniqueToken = UUID.randomUUID().toString()
                showProgress()
                sendTextToServer(text, uniqueToken, view)
            } else {
                showSnackbar(view, "Enter text to send.")
            }
        }

        // Send photo to server
        sendPhotoButton.setOnClickListener { view ->
            if (photoUri != null) {
                val uniqueToken = UUID.randomUUID().toString()
                showProgress()
                sendImageToServer(photoUri!!, uniqueToken, view)
            } else {
                showSnackbar(view, "No photo to send.")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    photoUri?.let { uri ->
                        val bitmap = contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it)
                        }
                        selectedImageView.setImageBitmap(bitmap)
                    } ?: showSnackbar(selectedImageView, "Failed to capture image.")
                }
                GALLERY_REQUEST_CODE -> {
                    photoUri = data?.data
                    photoUri?.let { uri ->
                        val bitmap = contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it)
                        }
                        selectedImageView.setImageBitmap(bitmap)
                    } ?: showSnackbar(selectedImageView, "No image selected.")
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

    private fun getUnsafeOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    private fun sendTextToServer(text: String, token: String, view: View) {
        Log.d("Network", "Attempting to send text to: $sendTextUrl")
        
        val requestBody = FormBody.Builder()
            .add("text", text)
            .add("fcm_token", token)
            .build()

        val request = Request.Builder()
            .url(sendTextUrl)
            .post(requestBody)
            .build()

        val client = getUnsafeOkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Network", "Failed to send text", e)
                runOnUiThread {
                    hideProgress()
                    val errorMessage = when {
                        e.message?.contains("Failed to connect") == true -> 
                            "Cannot connect to server. Please check if the server is running and the IP address is correct."
                        e.message?.contains("timeout") == true -> 
                            "Connection timed out. Please check your network connection."
                        else -> "Failed to send text: ${e.localizedMessage}"
                    }
                    showSnackbar(view, errorMessage)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("Network", "Response code: ${response.code}")
                Log.d("Network", "Response headers: ${response.headers}")
                
                runOnUiThread { hideProgress() }
                response.use {
                    val responseBody = response.body?.string()
                    Log.d("Network", "Response body: $responseBody")
                    
                    when (response.code) {
                        404 -> {
                            runOnUiThread { 
                                showSnackbar(view, "Server endpoint not found. Please check the server URL and API endpoints.")
                            }
                        }
                        500 -> {
                            runOnUiThread { 
                                showSnackbar(view, "Server error. Please try again later.")
                            }
                        }
                        else -> {
                            if (response.isSuccessful && responseBody != null) {
                                val jsonResponse = JSONObject(responseBody)
                                val message = jsonResponse.optString("message", "No response message.")
                                runOnUiThread { showSnackbar(view, message) }
                            } else {
                                runOnUiThread { 
                                    showSnackbar(view, "Error sending text: ${response.code} - ${response.message}")
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    private fun sendImageToServer(imageUri: Uri, token: String, view: View) {
        Log.d("Network", "Attempting to send image to: $uploadImageUrl")
        
        val inputStream = contentResolver.openInputStream(imageUri)
        val imageData = inputStream?.readBytes() ?: return runOnUiThread {
            hideProgress()
            showSnackbar(view, "Failed to read image.")
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", "uploaded_image.jpg", imageData.toRequestBody("image/jpeg".toMediaTypeOrNull()))
            .addFormDataPart("fcm_token", token)
            .build()

        val request = Request.Builder()
            .url(uploadImageUrl)
            .post(requestBody)
            .build()

        val client = getUnsafeOkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Network", "Failed to upload image", e)
                runOnUiThread {
                    hideProgress()
                    val errorMessage = when {
                        e.message?.contains("Failed to connect") == true -> 
                            "Cannot connect to server. Please check if the server is running and the IP address is correct."
                        e.message?.contains("timeout") == true -> 
                            "Connection timed out. Please check your network connection."
                        else -> "Failed to upload image: ${e.localizedMessage}"
                    }
                    showSnackbar(view, errorMessage)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("Network", "Response code: ${response.code}")
                Log.d("Network", "Response headers: ${response.headers}")
                
                runOnUiThread { hideProgress() }
                response.use {
                    val responseBody = response.body?.string()
                    Log.d("Network", "Response body: $responseBody")
                    
                    when (response.code) {
                        404 -> {
                            runOnUiThread { 
                                showSnackbar(view, "Server endpoint not found. Please check the server URL and API endpoints.")
                            }
                        }
                        500 -> {
                            runOnUiThread { 
                                showSnackbar(view, "Server error. Please try again later.")
                            }
                        }
                        else -> {
                            if (response.isSuccessful && responseBody != null) {
                                val jsonResponse = JSONObject(responseBody)
                                val message = jsonResponse.optString("message", "No response message.")
                                runOnUiThread { showSnackbar(view, message) }
                            } else {
                                runOnUiThread { 
                                    showSnackbar(view, "Error uploading image: ${response.code} - ${response.message}")
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    private fun getTimeStamp(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return timeStamp.format(Date())
    }

    private fun showSnackbar(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showProgress() {
        progressDialog.show()
    }

    private fun hideProgress() {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }
}
