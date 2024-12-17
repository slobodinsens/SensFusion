package com.example.sensfusion

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val blurPaint = Paint().apply {
        color = Color.BLACK
        alpha = 180
        isAntiAlias = true
    }

    private val rectPaint = Paint().apply {
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        isAntiAlias = true
    }

    private val boxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 40f
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val path = Path()
    private var detectedBoxes = listOf<Pair<RectF, Int>>() // Обновляем список безопасно

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * Параллельная обработка новых боксов
     */
    fun setBoxes(newBoxes: List<Pair<RectF, Int>>) {
        executor.submit {
            // Подготовка данных для боксов
            val processedBoxes = newBoxes.map { (box, classId) ->
                // Дополнительные преобразования данных (если нужно)
                box to classId
            }

            // Обновляем данные на UI-потоке
            post {
                detectedBoxes = processedBoxes
                invalidate() // Запрос на перерисовку
            }
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val horizontalPadding = width * 0.05f
        val verticalPadding = height * 0.05f

        val sectorHeight = (height - 2 * verticalPadding) / 3
        val top = (height - sectorHeight) / 2
        val bottom = top + sectorHeight
        val left = horizontalPadding
        val right = width - horizontalPadding

        val saveLayer = canvas.saveLayer(0f, 0f, width, height, null)
        canvas.drawRect(0f, 0f, width, height, blurPaint)

        path.reset()
        path.addRect(left, top, right, bottom, Path.Direction.CW)
        canvas.drawPath(path, rectPaint)

        // Рисуем боксы и текст
        for ((box, classId) in detectedBoxes) {
            val scaledBox = RectF(
                left + box.left * (right - left),
                top + box.top * (bottom - top),
                left + box.right * (right - left),
                top + box.bottom * (bottom - top)
            )
            canvas.drawRect(scaledBox, boxPaint)
            canvas.drawText("Class: $classId", scaledBox.left, scaledBox.top - 10, textPaint)
        }

        canvas.restoreToCount(saveLayer)
    }
}
