plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    id("kotlin-parcelize")
}

android {
    namespace = "com.gcancino.levelingup"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gcancino.levelingup"
        minSdk = 26
        targetSdk = 36
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

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.worker)

    // Material
    //implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.alpha)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.material.icons.extended)

    // Google
    implementation(libs.google.barcode.scanner)
    implementation(libs.google.auth)
    implementation(libs.google.gson)
    implementation(libs.google.permissions)

    // Firebase
    implementation(platform(libs.google.firebaseBom))
    implementation(libs.google.firebase.auth)
    implementation(libs.google.firebase.firestore)
    implementation(libs.google.firebase.storage)
    implementation(libs.google.firebase.messaging)
    implementation(libs.google.firebase.ai)

    // Coroutines
    implementation(libs.kotlin.coroutines)
    // Navigation
    implementation(libs.navigation.compose)
    // SplashScreen
    implementation(libs.splashScreen)
    // DataStore
    implementation(libs.storage.datastore)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    //kapt(libs.room.compiler)
    //annotationProcessor(libs.room.processor)

    // Camera
    implementation(libs.camerax.core)
    //implementation(libs.camerax.compose)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camera.view)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.compose)
    implementation(libs.hilt.worker)
    ksp(libs.hilt.compiler)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter)
    implementation(libs.coil.image)
    // One Signal
    implementation(libs.oneSignal)
    // Glance
    implementation(libs.glance.appwidget)
    implementation(libs.glance.appwidget.material)
    // Kotlin Serialization
    implementation(libs.kotlin.serialization)
    // Kotlin Reflect
    implementation(libs.kotlin.reflect)
    implementation(libs.sqlCipher)
    // Vosk Speech Recognition
    implementation(libs.vosk)
    // ReOwn
    implementation(platform(libs.reownBom))
    implementation(libs.reown.core)
    implementation(libs.reown.appKit)
    implementation(libs.kitizonwoseCal)
    implementation(libs.vico.compose)
    implementation(libs.okHttp)
    implementation(libs.guava)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    //implementation("com.composables:core:1.37.0")

}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}