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

    private val textPaint = Paint().apply {
        textSize = 50f
        style = Paint.Style.FILL
        color = Color.WHITE
        isAntiAlias = true
    }

    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private var detectedDigits = mutableListOf<String>() // Список фиксированных распознанных символов
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    // Словарь для соответствия классов и символов
    private val plateClassNames = mapOf(
        "dot" to ".", "eight" to "8", "five" to "5", "four" to "4", "il" to "il",
        "nine" to "9", "one" to "1", "seven" to "7", "six" to "6", "three" to "3",
        "two" to "2", "zero" to "0"
    )

    /**
     * Обновляет список распознанных символов и фиксирует их, если они ещё не добавлены
     */
    fun setBoxes(newBoxes: List<Pair<RectF, Int>>) {
        executor.submit {
            post {
                for ((_, classId) in newBoxes) {
                    // Преобразование classId в строку
                    val className = plateClassNames.keys.elementAtOrNull(classId)
                    val digit = plateClassNames[className]
                    if (digit != null && !detectedDigits.contains(digit)) {
                        detectedDigits.add(digit)
                    }
                }
                invalidate() // Перерисовка экрана
            }
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Определяем безопасную зону
        val horizontalMargin = width * 0.05f
        val safeZoneHeight = height * 0.2f
        val top = (height - safeZoneHeight) / 2
        val bottom = top + safeZoneHeight
        val safeZone = RectF(
            horizontalMargin, // Отступ слева
            top,              // Верхняя граница
            width - horizontalMargin, // Отступ справа
            bottom            // Нижняя граница
        )

        // Затемнение экрана за исключением безопасной зоны
        val saveLayer = canvas.saveLayer(0f, 0f, width, height, null)
        canvas.drawRect(0f, 0f, width, height, blurPaint)
        canvas.drawRect(safeZone, clearPaint)
        canvas.restoreToCount(saveLayer)

        // Отображение зафиксированных символов над безопасной зоной
        var textX = horizontalMargin // Начальная позиция текста слева
        val textY = top - 20 // Текст чуть выше безопасной зоны

        for (digit in detectedDigits) {
            canvas.drawText(digit, textX, textY, textPaint)
            textX += 50 // Сдвиг текста вправо
        }
    }
}
