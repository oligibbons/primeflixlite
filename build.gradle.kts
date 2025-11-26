// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
}

dependencies {

    // 1. Room Database (The Memory)
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version") // Extensions for Kotlin
    ksp("androidx.room:room-compiler:$room_version") // The Code Generator

    // 2. Retrofit (The Networking)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // JSON parser
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // 3. Coil (Image Loading - Strict Memory Control)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // 4. Media3 (The Video Player)
    val media3_version = "1.3.0" // Stable version
    implementation("androidx.media3:media3-exoplayer:$media3_version")
    implementation("androidx.media3:media3-ui:$media3_version")
    implementation("androidx.media3:media3-common:$media3_version")
}