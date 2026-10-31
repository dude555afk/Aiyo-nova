plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jlleitschuh.gradle.ktlint")
    id("androidx.room")
}

android {
    namespace = "com.beradeep.aiyo.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        targetSdk = 36
        consumerProguardFiles("consumer-rules.pro")
    }

    room {
        schemaDirectory("$projectDir/data/schemas")
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
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.mmkv)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.openai.client.bom))
    implementation(libs.openai.client)
    implementation(libs.ktor.client.okhttp)
}