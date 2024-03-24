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
        consumerProguardFiles("consumer-rules.pro")
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

    buildFeatures { compose = true }

    composeOptions { kotlinCompilerExtensionVersion = "1.2.0-beta02" }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions { jvmTarget = "1.8" }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core-android"))
    implementation(project(":viewpager-bottom-sheet-behavior"))

    implementation(kotlin("stdlib", KotlinCompilerVersion.VERSION))

    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.0")

    implementation("com.google.android.material:material:1.6.0")
    implementation("androidx.palette:palette:1.0.0")

    implementation("androidx.compose.runtime:runtime:1.2.0-beta02")
    implementation("androidx.compose.compiler:compiler:1.2.0-beta02")
    implementation("androidx.compose.ui:ui:1.2.0-beta02")
    implementation("androidx.compose.ui:ui-tooling:1.2.0-beta02")
    implementation("androidx.compose.foundation:foundation:1.2.0-beta02")
    implementation("androidx.compose.material:material:1.2.0-beta02")
    implementation("androidx.compose.material:material-icons-core:1.2.0-beta02")
    implementation("androidx.compose.material:material-icons-extended:1.2.0-beta02")

    implementation("com.google.dagger:hilt-android:2.40")
    kapt("com.google.dagger:hilt-android-compiler:2.40")
    kapt("androidx.hilt:hilt-compiler:1.0.0")
    implementation("androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03")

    implementation("com.jakewharton.timber:timber:4.7.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
