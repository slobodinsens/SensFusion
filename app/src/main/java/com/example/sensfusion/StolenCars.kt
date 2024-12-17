@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package com.example.sensfusion

import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
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
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class StolenCars : AppCompatActivity() {

    private lateinit var tflite: Interpreter
    private lateinit var overlayView: OverlayView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var inputImageBuffer: TensorImage
    private lateinit var outputBuffer: TensorBuffer
    private lateinit var modelFile: MappedByteBuffer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stolen_cars)

        val previewView = findViewById<PreviewView>(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)

        try {
            modelFile = loadModelFile("dig_yolov8s_17_float16.tflite")
            tflite = Interpreter(modelFile)
            initBuffers()
            Log.d("TFLite", "Model and buffers initialized successfully.")
        } catch (e: Exception) {
            Log.e("TFLite", "Error loading model: ${e.message}")
        }

        setupCamera(previewView)
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
        val bitmap = imageProxy.toBitmap()
        bitmap?.let {
            val tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(it)

            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(640, 640, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0.0f, 255.0f)) // Normalize pixel values to [0, 1]
                .build()

            inputImageBuffer = imageProcessor.process(tensorImage)
            runInference()
        } ?: run {
            Log.e("CameraX", "Failed to convert ImageProxy to Bitmap.")
        }
        imageProxy.close()
    }

    private fun initBuffers() {
        val inputShape = tflite.getInputTensor(0).shape() // Assuming single input
        val outputShape = tflite.getOutputTensor(0).shape() // Assuming single output
        inputImageBuffer = TensorImage(DataType.FLOAT32)
        outputBuffer = TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32)
    }

    private fun runInference() {
        try {
            tflite.run(inputImageBuffer.buffer, outputBuffer.buffer.rewind())
            val detections = parseOutput(outputBuffer)
            runOnUiThread {
                overlayView.setBoxes(detections)
            }
        } catch (e: Exception) {
            Log.e("Inference", "Error during inference: ${e.message}")
        }
    }

    private fun parseOutput(outputBuffer: TensorBuffer): List<Pair<RectF, Int>> {
        val detections = mutableListOf<Pair<RectF, Int>>()
        val outputArray = outputBuffer.floatArray

        val numDetections = 8400
        val numAttributes = 16

        for (i in 0 until numDetections) {
            val offset = i * numAttributes
            val xCenter = outputArray[offset]
            val yCenter = outputArray[offset + 1]
            val width = outputArray[offset + 2]
            val height = outputArray[offset + 3]
            val confidence = outputArray[offset + 4]
            val classProbabilities = outputArray.sliceArray(offset + 5 until offset + numAttributes)
            val maxProbability = classProbabilities.maxOrNull() ?: 0f
            val classId = classProbabilities.toList().indexOf(maxProbability)

            if (confidence > 0.2 && maxProbability > 0.2) {
                val x1 = xCenter - width / 2
                val y1 = yCenter - height / 2
                val x2 = xCenter + width / 2
                val y2 = yCenter + height / 2
                detections.add(RectF(x1, y1, x2, y2) to classId)
            }
        }
        return detections
    }

    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val assetFileDescriptor: AssetFileDescriptor = assets.openFd(modelPath)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
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
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 90, out)
        val byteArray = out.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
}
