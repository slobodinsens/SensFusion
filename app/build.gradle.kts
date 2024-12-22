plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.sensfusion"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.sensfusion"
        minSdk = 26
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets") // Правильная настройка assets
        }
    }
}

dependencies {
    // Базовые библиотеки Android
    implementation(libs.androidx.appcompat.v161)
    implementation(libs.androidx.core.ktx.v1120)
    implementation(libs.material)

    // ConstraintLayout для макетов
    implementation(libs.androidx.constraintlayout)

    // Анимации
    implementation(libs.androidx.interpolator)

    // Жизненный цикл (если будете использовать ViewModel или LiveData)
    implementation(libs.androidx.lifecycle.runtime.ktx.v261)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // RecyclerView (если планируете списки)
    implementation(libs.androidx.recyclerview)

    // Firebase Authentication (если Email и пароль планируете использовать с Firebase)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.androidx.core)
    implementation(libs.androidx.core)
    implementation(libs.litert.metadata)
    implementation(libs.androidx.monitor)
    implementation(libs.androidx.junit.ktx)
    implementation(libs.androidx.core)
    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.ui)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.activity.compose)
    debugImplementation(libs.ui.tooling)
    implementation(libs.androidx.runtime.livedata)

    // Дополнительные библиотеки Compose
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout.v214)
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.google.firebase:firebase-bom:33.7.0")
    implementation(libs.firebase.messaging)

    // TensorFlow Lite and ONNX Runtime
    implementation(libs.tensorflow.lite.support)
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.15.1")
    implementation ("androidx.constraintlayout:constraintlayout:2.2.0")
}
