import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.library")
    id("dagger.hilt.android.plugin")
    kotlin("android")
    id("kotlin-parcelize")
    kotlin("kapt")
}

android {
    compileSdk = 33

    defaultConfig {
        minSdk = 28
        targetSdk = 33
        version = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "$project.rootDir/tools/proguard-rules.pro"
            )
        }
    }

    packagingOptions { resources { excludes += "META-INF/DEPENDENCIES" } }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

kapt { correctErrorTypes = true }

dependencies {
    implementation(project(":core"))
    implementation(project(":core-android"))

    implementation(kotlin("stdlib", KotlinCompilerVersion.VERSION))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")

    implementation("org.mapstruct:mapstruct:1.4.1.Final")
    kapt("org.mapstruct:mapstruct-processor:1.4.1.Final")

    implementation("com.google.dagger:hilt-android:2.40")
    kapt("com.google.dagger:hilt-android-compiler:2.40")
    kapt("androidx.hilt:hilt-compiler:1.0.0")

    testImplementation("com.google.dagger:dagger:2.40")
    kaptTest("com.google.dagger:dagger-compiler:2.40")

    implementation("com.dropbox.mobile.store:store4:4.0.4-KT15")
    implementation("androidx.room:room-runtime:2.3.0")
    kapt("androidx.room:room-compiler:2.3.0")
    implementation("androidx.room:room-ktx:2.3.0")

    implementation("com.google.code.gson:gson:2.8.6")
    implementation("com.github.filosganga:geogson-jts:1.4.2")

    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")
    implementation("com.squareup.retrofit2:converter-gson:2.7.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.4.3")
}
