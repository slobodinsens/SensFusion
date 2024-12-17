package com.example.sensfusion

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
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
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        textSize = 35f
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private var detectedBoxes = listOf<Pair<RectF, Int>>()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    // Список цветов для классов
    private val classColors = listOf(
        Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
        Color.CYAN, Color.MAGENTA, Color.LTGRAY, Color.WHITE
    )

    private val boxShrinkFactor = 0.9f // Коэффициент уменьшения боксов

    /**
     * Обновляет список распознанных боксов и перерисовывает
     */
    fun setBoxes(newBoxes: List<Pair<RectF, Int>>) {
        executor.submit {
            post {
                detectedBoxes = newBoxes
                invalidate()
            }
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Отступы 5% от ширины экрана
        val horizontalMargin = width * 0.05f

        // Создание горизонтального прямоугольника
        val rectHeight = height * 0.2f // Высота прямоугольника (20% от высоты экрана)
        val top = (height - rectHeight) / 2
        val bottom = top + rectHeight
        val clearRect = RectF(
            horizontalMargin, // Отступ слева
            top,              // Верхняя граница
            width - horizontalMargin, // Отступ справа
            bottom            // Нижняя граница
        )

        // Создание темного слоя
        val saveLayer = canvas.saveLayer(0f, 0f, width, height, null)
        canvas.drawRect(0f, 0f, width, height, blurPaint)

        // Очистка прямоугольника
        canvas.drawRect(clearRect, clearPaint)
        canvas.restoreToCount(saveLayer)

        // Отрисовка боксов
        for ((box, classId) in detectedBoxes) {
            val scaledBox = scaleBoxToRect(box, clearRect)
            val shrunkBox = shrinkBox(scaledBox, boxShrinkFactor)

            // Подбираем цвет для класса
            val color = classColors[classId % classColors.size]
            boxPaint.color = color
            textPaint.color = color

            // Рисуем бокс и текст
            canvas.drawRect(shrunkBox, boxPaint)
            canvas.drawText("Class: $classId", shrunkBox.left, shrunkBox.top - 10, textPaint)
        }
    }

    /**
     * Масштабирует распознанный бокс в пределах чистого прямоугольника
     */
    private fun scaleBoxToRect(box: RectF, clearRect: RectF): RectF {
        val rectWidth = clearRect.width()
        val rectHeight = clearRect.height()

        return RectF(
            clearRect.left + box.left * rectWidth,
            clearRect.top + box.top * rectHeight,
            clearRect.left + box.right * rectWidth,
            clearRect.top + box.bottom * rectHeight
        )
    }

    /**
     * Уменьшает размер бокса с учетом коэффициента
     */
    private fun shrinkBox(box: RectF, shrinkFactor: Float): RectF {
        val width = box.width()
        val height = box.height()
        val deltaX = (width * (1 - shrinkFactor)) / 2
        val deltaY = (height * (1 - shrinkFactor)) / 2

        return RectF(
            box.left + deltaX,
            box.top + deltaY,
            box.right - deltaX,
            box.bottom - deltaY
        )
    }
}
