plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.sf_new"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.sf_new"
        minSdk = 26
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
}

dependencies {
    // Базовые библиотеки Android
    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation( "androidx.core:core-ktx:1.12.0")
    implementation ("com.google.android.material:material:1.9.0")

    // ConstraintLayout для макетов
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Анимации
    implementation ("androidx.interpolator:interpolator:1.0.0")

    // Жизненный цикл (если будете использовать ViewModel или LiveData)
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")

    // RecyclerView (если планируете списки)
    implementation ("androidx.recyclerview:recyclerview:1.3.1")

    // Firebase Authentication (если Email и пароль планируете использовать с Firebase)
    implementation ("com.google.firebase:firebase-auth-ktx:22.1.0")
}
