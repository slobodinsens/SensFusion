package com.example.sensfusion

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CarouselRenderer : GLSurfaceView.Renderer {

    private val rotationMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private var angle = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Устанавливаем цвет фона
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        Matrix.setIdentityM(modelMatrix, 0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // Устанавливаем viewport
        GLES20.glViewport(0, 0, width, height)

        // Настраиваем проекционную матрицу
        val ratio: Float = width.toFloat() / height
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)

        // Настраиваем видовую матрицу
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -5f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Очищаем экран
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Применяем вращение
        Matrix.setRotateM(rotationMatrix, 0, angle, 0f, 1f, 0f)
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, rotationMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        // Рисуем (для примера, нарисуем прямоугольник)
        drawRectangle()
    }

    private fun drawRectangle() {
        val vertices = floatArrayOf(
            -0.5f, -0.5f, 0f, // нижний левый
            0.5f, -0.5f, 0f,  // нижний правый
            -0.5f, 0.5f, 0f,  // верхний левый
            0.5f, 0.5f, 0f    // верхний правый
        )

        // Задайте буфер вершин
        val vertexBuffer = FloatBuffer.wrap(vertices)
        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(0)

        // Устанавливаем цвет
        GLES20.glUniform4f(0, 1f, 1f, 1f, 1f)

        // Рисуем треугольники
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertices.size / 3)
    }

    fun setRotationSpeed(speed: Float) {
        angle += speed
    }
}
