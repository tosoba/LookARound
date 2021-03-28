plugins {
    id("com.android.library")
    id("dagger.hilt.android.plugin")
    kotlin("android")
    id("kotlin-parcelize")
    kotlin("kapt")
}

android {
    compileSdk = 30
    buildToolsVersion = "30.0.3"

    defaultConfig {
        minSdk = 21
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions { jvmTarget = "1.8" }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core-android"))
    implementation(project(":core-android-model"))
    implementation(project(":ui-main"))

    implementation(kotlin("stdlib", org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION))
    implementation("androidx.appcompat:appcompat:1.3.0-beta01")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("com.google.android.material:material:1.3.0")
    implementation("dev.chrisbanes.accompanist:accompanist-insets:0.6.0")
    implementation("dev.chrisbanes.accompanist:accompanist-coil:0.6.0")

    implementation("androidx.core:core-ktx:1.3.2")
    implementation("androidx.fragment:fragment-ktx:1.3.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.0")

    implementation("com.mapzen.tangram:tangram:0.13.0")

    implementation("com.google.dagger:hilt-android:2.31.2-alpha")
    kapt("com.google.dagger:hilt-android-compiler:2.31.2-alpha")
    kapt("androidx.hilt:hilt-compiler:1.0.0-alpha03")
    implementation("androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03")

    implementation("androidx.compose.runtime:runtime:1.0.0-beta01")
    implementation("androidx.compose.compiler:compiler:1.0.0-beta01")
    implementation("androidx.compose.ui:ui:1.0.0-beta01")
    implementation("androidx.compose.ui:ui-tooling:1.0.0-beta01")
    implementation("androidx.compose.foundation:foundation:1.0.0-beta01")
    implementation("androidx.compose.material:material:1.0.0-beta01")
    implementation("androidx.compose.material:material-icons-core:1.0.0-beta01")
    implementation("androidx.compose.material:material-icons-extended:1.0.0-beta01")

    testImplementation("junit:junit:4.13.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}
