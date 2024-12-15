package com.example.sensfusion

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val blurPaint = Paint().apply {
        color = Color.BLACK
        alpha = 180 // Полупрозрачность размытого слоя
        isAntiAlias = true
    }

    private val rectPaint = Paint().apply {
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) // Удаление области прямоугольника
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
    private val detectedBoxes = mutableListOf<Pair<RectF, Int>>() // Список боксов и их классов

    fun setBoxes(newBoxes: List<Pair<RectF, Int>>) {
        detectedBoxes.clear()
        detectedBoxes.addAll(newBoxes)
        invalidate() // Перерисовать представление
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Отступы (например, 10% от ширины и высоты)
        val horizontalPadding = width * 0.05f
        val verticalPadding = height * 0.05f

        // Вычисляем координаты прямоугольника с учётом отступов
        val sectorHeight = (height - 2 * verticalPadding) / 3 // Высота прямоугольника
        val top = (height - sectorHeight) / 2 // Центрируем по вертикали
        val bottom = top + sectorHeight
        val left = horizontalPadding
        val right = width - horizontalPadding

        // Создаём размытие для всего экрана
        val saveLayer = canvas.saveLayer(0f, 0f, width, height, null)
        canvas.drawRect(0f, 0f, width, height, blurPaint)

        // Убираем область прямоугольника
        path.reset()
        path.addRect(left, top, right, bottom, Path.Direction.CW)
        canvas.drawPath(path, rectPaint)

        // Рисуем боксы внутри прямоугольника
        for ((box, classId) in detectedBoxes) {
            val scaledBox = RectF(
                left + box.left * (right - left),
                top + box.top * (bottom - top),
                left + box.right * (right - left),
                top + box.bottom * (bottom - top)
            )
            // Рисуем рамку
            canvas.drawRect(scaledBox, boxPaint)

            // Добавляем текст класса над боксом
            val label = "Class: $classId"
            canvas.drawText(label, scaledBox.left, scaledBox.top - 10, textPaint)
        }

        // Восстанавливаем слой
        canvas.restoreToCount(saveLayer)
    }
}
