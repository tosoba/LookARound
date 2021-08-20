import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.library")
    id("dagger.hilt.android.plugin")
    kotlin("android")
    id("kotlin-parcelize")
    kotlin("kapt")
}

android {
    compileSdk = 30

    defaultConfig {
        minSdk = 21
        targetSdk = 30
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions { jvmTarget = "1.8" }

    buildFeatures { viewBinding = true }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core-android"))
    implementation(project(":core-android-model"))
    implementation(project(":ui-main"))

    implementation(kotlin("stdlib", KotlinCompilerVersion.VERSION))
    implementation("androidx.appcompat:appcompat:1.3.0-rc01")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("com.google.android.material:material:1.3.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.kirich1409.viewbindingpropertydelegate:vbpd-noreflection:1.4.1")

    implementation("androidx.core:core-ktx:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.3.1")

    implementation("com.google.dagger:hilt-android:2.35")
    kapt("com.google.dagger:hilt-android-compiler:2.35")
    kapt("androidx.hilt:hilt-compiler:1.0.0")
    implementation("androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03")

    implementation("org.permissionsdispatcher:permissionsdispatcher:4.8.0")
    kapt("org.permissionsdispatcher:permissionsdispatcher-processor:4.8.0")

    implementation("androidx.camera:camera-camera2:1.1.0-alpha04")
    implementation("androidx.camera:camera-lifecycle:1.1.0-alpha04")
    implementation("androidx.camera:camera-view:1.0.0-alpha24")

    implementation("com.google.android.gms:play-services-location:18.0.0")
    implementation("com.github.jintin:FancyLocationProvider:2.0.0")

    implementation("com.jakewharton.timber:timber:4.7.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}
