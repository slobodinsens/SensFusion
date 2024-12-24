@file:Suppress("DEPRECATION")

package com.example.sensfusion

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class StolenCars : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stolen_cars)

        // Инициализация WebView
        webView = findViewById(R.id.webView)

        // Настройка WebView
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true // Включаем JavaScript
        webSettings.domStorageEnabled = true // Включаем поддержку DOM Storage

        // Открытие ссылок внутри WebView
        webView.webViewClient = WebViewClient()

        // Загрузка сайта
        webView.loadUrl("https://www.gov.il/apps/police/stolencar/")
    }

    override fun onBackPressed() {
        // Если возможно, возвращаемся на предыдущую страницу
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
