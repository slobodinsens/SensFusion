@file:Suppress("DEPRECATION")

package com.example.sensfusion

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.io.Serializable
import kotlin.math.cos
import kotlin.math.sin

class RadarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paintBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.FILL
    }

    private val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val paintSweep = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(100, 0, 255, 0)
        style = Paint.Style.FILL
    }

    private val paintDevice = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private var sweepAngle = 0f
    private val devices = mutableListOf<Device>()

    private val handler = android.os.Handler()
    private val sweepRunnable = object : Runnable {
        override fun run() {
            updateSweepAngle()
            handler.postDelayed(this, 50)
        }
    }

    data class Device(val name: String, val x: Float, val y: Float) : Serializable

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = width / 3f

        canvas.drawCircle(cx, cy, radius, paintBackground)
        canvas.drawCircle(cx, cy, radius, paintLine)
        canvas.drawCircle(cx, cy, radius / 2, paintLine)
        canvas.drawCircle(cx, cy, radius / 4, paintLine)

        val sweepRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(sweepRect, sweepAngle, 30f, true, paintSweep)

        devices.forEach { device ->
            canvas.drawCircle(device.x, device.y, 10f, paintDevice)
        }
    }

    fun updateDevices(rssiList: List<Serializable>) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = width / 3f

        devices.clear()
        rssiList.forEach { item ->
            if (item is Device) {
                // Calculate the position based on the RSSI
                val distance = radius * (1 - (item.x.toFloat() / -100f)) // Adjust 'x' to represent RSSI
                val angle = Math.random() * 2 * Math.PI // Randomly distribute across the circle

                val x = (cx + distance * cos(angle)).toFloat()
                val y = (cy + distance * sin(angle)).toFloat()
                devices.add(Device(item.name, x, y))
            }
        }

        invalidate()
    }

    fun updateSweepAngle() {
        sweepAngle += 5f
        if (sweepAngle >= 360) sweepAngle = 0f
        invalidate()
    }

    fun startSweepAnimation() {
        handler.post(sweepRunnable)
    }

    fun stopSweepAnimation() {
        handler.removeCallbacks(sweepRunnable)
    }
}
