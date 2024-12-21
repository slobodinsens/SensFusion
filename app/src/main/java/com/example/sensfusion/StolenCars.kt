package com.example.sensfusion

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.YuvImage
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.nio.FloatBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class StolenCars : AppCompatActivity() {

    private lateinit var ortEnv: OrtEnvironment
    private lateinit var ortSession: OrtSession
    private lateinit var overlayView: OverlayView
    private lateinit var cameraExecutor: ExecutorService
    private var isProcessingFrame = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stolen_cars)

        val previewView = findViewById<PreviewView>(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)

        try {
            initOnnxRuntime()
            Log.d("ONNX", "Model initialized successfully.")
        } catch (e: Exception) {
            Log.e("ONNX", "Error loading model: ${e.message}")
        }

        setupCamera(previewView)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun initOnnxRuntime() {
        ortEnv = OrtEnvironment.getEnvironment()
        val modelData = assets.open("dig_yolov8s_17.onnx").use { it.readBytes() }
        ortSession = ortEnv.createSession(modelData)
    }

    private fun setupCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA

                val preview = androidx.camera.core.Preview.Builder().build().also {
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
        if (isProcessingFrame) {
            imageProxy.close()
            return
        }
        isProcessingFrame = true

        try {
            val bitmap = imageProxy.toBitmap()
            if (bitmap != null) {
                Log.d("ProcessImage", "Original bitmap dimensions: ${bitmap.width}x${bitmap.height}")
                val resizedBitmap = resizeAndPadToTarget(bitmap, 640, 640)
                Log.d("ProcessImage", "Bitmap after resize dimensions: ${resizedBitmap.width}x${resizedBitmap.height}")
                val rotatedBitmap = rotateBitmap(resizedBitmap, 90)
                Log.d("ProcessImage", "Bitmap after rotate dimensions: ${rotatedBitmap.width}x${rotatedBitmap.height}")

                val inputArray = preprocessImage(rotatedBitmap)
                val detections = runInference(inputArray)
                runOnUiThread {
                    overlayView.setBoxes(detections)
                }
            } else {
                Log.e("CameraX", "Failed to convert imageProxy to Bitmap.")
            }
        } catch (e: Exception) {
            Log.e("Processing", "Error processing image: ${e.message}")
        } finally {
            imageProxy.close()
            isProcessingFrame = false
        }
    }

    private fun preprocessImage(bitmap: Bitmap): FloatArray {
        val floatArray = FloatArray(3 * 640 * 640)
        var index = 0
        for (y in 0 until 640) {
            for (x in 0 until 640) {
                val pixel = bitmap.getPixel(x, y)
                floatArray[index++] = (pixel shr 16 and 0xFF) / 255f // R
                floatArray[index++] = (pixel shr 8 and 0xFF) / 255f  // G
                floatArray[index++] = (pixel and 0xFF) / 255f        // B
            }
        }
        return floatArray
    }

    private fun runInference(inputArray: FloatArray): List<Pair<RectF, Int>> {
        return try {
            val inputShape = longArrayOf(1, 3, 640, 640)
            val inputName = ortSession.inputNames.iterator().next()

            val inputTensor = OnnxTensor.createTensor(ortEnv, FloatBuffer.wrap(inputArray), inputShape)
            val results = ortSession.run(mapOf(inputName to inputTensor))
            val output = results[0].value as Array<FloatArray>
            parseOutput(output)
        } catch (e: Exception) {
            Log.e("ONNX", "Error during inference: ${e.message}")
            emptyList()
        }
    }

    private fun parseOutput(outputArray: Array<FloatArray>): List<Pair<RectF, Int>> {
        val detections = mutableListOf<Pair<RectF, Int>>()
        val numDetections = outputArray.size
        val numAttributes = 85
        val imageWidth = 640f
        val imageHeight = 640f

        for (i in 0 until numDetections) {
            val offset = outputArray[i]
            val xCenter = offset[0] * imageWidth
            val yCenter = offset[1] * imageHeight
            val width = offset[2] * imageWidth
            val height = offset[3] * imageHeight
            val confidence = offset[4]

            val classProbabilities = offset.sliceArray(5 until numAttributes)
            val maxProbability = classProbabilities.maxOrNull() ?: 0f
            val classId = classProbabilities.toList().indexOf(maxProbability)

            if (confidence > 0.3 && maxProbability > 0.3) {
                val x1 = xCenter - width / 2
                val y1 = yCenter - height / 2
                val x2 = xCenter + width / 2
                val y2 = yCenter + height / 2
                detections.add(RectF(x1, y1, x2, y2) to classId)
            }
        }
        return detections
    }

    private fun resizeAndPadToTarget(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val aspectRatio = bitmap.width.toFloat() / bitmap.height
        val scaledWidth: Int
        val scaledHeight: Int

        if (aspectRatio > 1) {
            scaledWidth = targetWidth
            scaledHeight = (targetWidth / aspectRatio).toInt()
        } else {
            scaledHeight = targetHeight
            scaledWidth = (targetHeight * aspectRatio).toInt()
        }

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        val paddedBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(paddedBitmap)
        val grayPaint = Paint().apply { color = Color.GRAY }
        canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), grayPaint)
        val left = (targetWidth - scaledWidth) / 2f
        val top = (targetHeight - scaledHeight) / 2f
        canvas.drawBitmap(scaledBitmap, left, top, null)

        return paddedBitmap
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat()) // Положительное значение degrees — поворот по часовой стрелке
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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
}
