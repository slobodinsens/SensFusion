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

    // Имена классов
    private val plateClassNames = listOf(
        "dot", "eight", "five", "four", "il",
        "nine", "one", "seven", "six", "three",
        "two", "zero"
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

        // Горизонтальная безопасная зона (30% от высоты экрана)
        val safeZoneHeight = height * 0.2f
        val top = (height - safeZoneHeight) / 2
        val bottom = top + safeZoneHeight
        val safeZone = RectF(
            horizontalMargin, // Отступ слева
            top,              // Верхняя граница
            width - horizontalMargin, // Отступ справа
            bottom            // Нижняя граница
        )

        // Создание тёмного слоя
        val saveLayer = canvas.saveLayer(0f, 0f, width, height, null)
        canvas.drawRect(0f, 0f, width, height, blurPaint)

        // Очистка безопасной зоны
        canvas.drawRect(safeZone, clearPaint)
        canvas.restoreToCount(saveLayer)

        // Отрисовка боксов
        for ((box, classId) in detectedBoxes) {
            val constrainedBox = constrainBoxToSafeZone(box, safeZone)

            // Подбираем цвет для класса
            val color = classColors[classId % classColors.size]
            boxPaint.color = color
            textPaint.color = color

            // Получаем имя класса
            val className = plateClassNames.getOrNull(classId) ?: "Unknown"

            // Рисуем бокс и текст
            canvas.drawRect(constrainedBox, boxPaint)
            canvas.drawText("Class: $className", constrainedBox.left, constrainedBox.top - 10, textPaint)
        }
    }

    /**
     * Масштабирует и ограничивает бокс в пределах безопасной зоны
     */
    private fun constrainBoxToSafeZone(box: RectF, safeZone: RectF): RectF {
        val safeWidth = safeZone.width()
        val safeHeight = safeZone.height()

        // Масштабирование бокса в процентах от безопасной зоны
        val left = safeZone.left + box.left * safeWidth
        val top = safeZone.top + box.top * safeHeight
        val right = safeZone.left + box.right * safeWidth
        val bottom = safeZone.top + box.bottom * safeHeight

        // Ограничение боксов в пределах безопасной зоны
        return RectF(
            left.coerceIn(safeZone.left, safeZone.right),
            top.coerceIn(safeZone.top, safeZone.bottom),
            right.coerceIn(safeZone.left, safeZone.right),
            bottom.coerceIn(safeZone.top, safeZone.bottom)
        )
    }
}
