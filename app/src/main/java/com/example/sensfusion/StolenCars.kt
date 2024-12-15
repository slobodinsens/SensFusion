@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package com.example.sensfusion

import android.content.res.AssetFileDescriptor
import android.graphics.*
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class StolenCars : AppCompatActivity() {

    private lateinit var tflite: Interpreter
    private lateinit var overlayView: OverlayView
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stolen_cars)

        val previewView = findViewById<PreviewView>(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)

        // Load the model
        try {
            tflite = loadModelFile("yolo11m_car_plate_ocr_float32.tflite")
            Log.d("TFLite", "Model loaded successfully.")
        } catch (e: IOException) {
            Log.e("TFLite", "Error loading model: ${e.message}")
        }

        // Setup camera
        setupCamera(previewView)

        // Initialize the executor service
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun setupCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            processImage(imageProxy)
                        }
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

            } catch (e: Exception) {
                Log.e("CameraX", "Error initializing camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        bitmap?.let {
            Log.d("ProcessImage", "Image converted to Bitmap. Width: ${it.width}, Height: ${it.height}")
            saveBitmap(it, "input_image.jpg")
            runInference(it)
        } ?: run {
            Log.e("CameraX", "Failed to convert ImageProxy to Bitmap.")
        }
        imageProxy.close()
    }

    private fun ImageProxy.toBitmap(): Bitmap? {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, out)
        val byteArray = out.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    private fun saveBitmap(bitmap: Bitmap, fileName: String) {
        try {
            val file = File(getExternalFilesDir(null), fileName)
            val outStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
            outStream.flush()
            outStream.close()
            Log.d("SaveBitmap", "Bitmap saved to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("SaveBitmap", "Error saving Bitmap: ${e.message}")
        }
    }

    private fun loadModelFile(modelPath: String): Interpreter {
        val fileDescriptor: AssetFileDescriptor = assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return Interpreter(fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength))
    }

    private fun preprocessBitmap(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val inputSize = 640
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val input = Array(1) { Array(inputSize) { Array(inputSize) { FloatArray(3) } } }

        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val pixel = scaledBitmap.getPixel(x, y)
                input[0][y][x][0] = (pixel shr 16 and 0xFF) / 255.0f
                input[0][y][x][1] = (pixel shr 8 and 0xFF) / 255.0f
                input[0][y][x][2] = (pixel and 0xFF) / 255.0f
            }
        }

        Log.d("PreprocessBitmap", "Bitmap preprocessed for model input.")
        return input
    }

    private fun runInference(bitmap: Bitmap) {
        val input = preprocessBitmap(bitmap)
        val output = Array(1) { Array(42) { FloatArray(8400) } }

        tflite.run(input, output)

        Log.d("Inference", "Raw output shape: [${output.size}, ${output[0].size}, ${output[0][0].size}]")

        val detectedBoxes = parseOutput(output, bitmap.width, bitmap.height)

        if (detectedBoxes.isEmpty()) {
            Log.d("Inference", "No objects detected.")
        } else {
            Log.d("Inference", "Detected objects: ${detectedBoxes.size}")
        }

        runOnUiThread {
            overlayView.setBoxes(detectedBoxes)
        }
    }

    private fun parseOutput(output: Array<Array<FloatArray>>, imageWidth: Int, imageHeight: Int): List<Pair<RectF, Int>> {
        val boxesWithClasses = mutableListOf<Pair<RectF, Int>>()

        for (i in output[0][0].indices) {
            val xCenter = output[0][0][i]
            val yCenter = output[0][1][i]
            val width = output[0][2][i]
            val height = output[0][3][i]
            val confidence = output[0][4][i]

            val classProbabilities = output[0].sliceArray(5 until output[0].size).map { it[i] }
            val maxProbability = classProbabilities.maxOrNull() ?: 0f
            val classId = classProbabilities.indexOf(maxProbability)

            if (confidence > 0.2 && maxProbability > 0.2) {
                val x1 = (xCenter - width / 2) * imageWidth
                val y1 = (yCenter - height / 2) * imageHeight
                val x2 = (xCenter + width / 2) * imageWidth
                val y2 = (yCenter + height / 2) * imageHeight
                boxesWithClasses.add(RectF(x1, y1, x2, y2) to classId)
            }
        }

        boxesWithClasses.forEach { (box, classId) ->
            Log.d("Detection", "Box: $box, Class ID: $classId")
        }

        return boxesWithClasses
    }
}
