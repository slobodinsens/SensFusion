package com.example.sensfusion

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.sqrt
import kotlin.random.Random

class NeuronsBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val neurons = mutableListOf<Neuron>()
    private val random = Random.Default

    private val neuronPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val connectionPaint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }

    init {
        generateNeurons(20) // Уменьшено количество нейронов
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Рисуем соединения между близкими нейронами
        neurons.forEach { neuron1 ->
            neurons.forEach { neuron2 ->
                if (neuron1 != neuron2 && neuron1.isCloseTo(neuron2, 200f)) {
                    canvas.drawLine(
                        neuron1.x,
                        neuron1.y,
                        neuron2.x,
                        neuron2.y,
                        connectionPaint
                    )
                }
            }
        }

        // Рисуем нейроны
        neurons.forEach { neuron ->
            canvas.drawCircle(neuron.x, neuron.y, 9f, neuronPaint)
            neuron.updatePosition(width, height)
        }

        // Обновляем анимацию
        postInvalidateOnAnimation()
    }

    private fun generateNeurons(count: Int) {
        val width = context.resources.displayMetrics.widthPixels
        val height = context.resources.displayMetrics.heightPixels

        repeat(count) {
            neurons.add(
                Neuron(
                    x = random.nextFloat() * width,
                    y = random.nextFloat() * height,
                    dx = random.nextFloat() * 2 - 1,
                    dy = random.nextFloat() * 2 - 1
                )
            )
        }
    }

    private data class Neuron(var x: Float, var y: Float, var dx: Float, var dy: Float) {
        fun updatePosition(screenWidth: Int, screenHeight: Int) {
            x += dx
            y += dy

            // Отражение от краёв экрана
            if (x <= 0 || x >= screenWidth) dx = -dx
            if (y <= 0 || y >= screenHeight) dy = -dy
        }

        fun isCloseTo(other: Neuron, distance: Float): Boolean {
            val dx = x - other.x
            val dy = y - other.y
            return sqrt(dx * dx + dy * dy) <= distance
        }
    }
}
