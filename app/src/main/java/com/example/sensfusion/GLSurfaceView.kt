package com.example.sensfusion

import android.content.Context
import android.opengl.GLSurfaceView

class CarouselView(context: Context) : GLSurfaceView(context) {
    private val renderer: CarouselRenderer

    init {
        setEGLContextClientVersion(2) // Используем OpenGL ES 2.0
        renderer = CarouselRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun startAnimation(speed: Float) {
        renderer.setRotationSpeed(speed)
    }
}
